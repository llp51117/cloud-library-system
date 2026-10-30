package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.entity.Announcement;
import com.group1.cloudlibrary.entity.Application;
import com.group1.cloudlibrary.entity.Borrow;
import com.group1.cloudlibrary.entity.Favorite;
import com.group1.cloudlibrary.entity.Feedback;
import com.group1.cloudlibrary.entity.Notification;
import com.group1.cloudlibrary.entity.Reservation;
import com.group1.cloudlibrary.entity.SearchLog;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.entity.VerificationCode;
import com.group1.cloudlibrary.mapper.AnnouncementMapper;
import com.group1.cloudlibrary.mapper.AnnouncementReadMapper;
import com.group1.cloudlibrary.mapper.ApplicationMapper;
import com.group1.cloudlibrary.mapper.BorrowMapper;
import com.group1.cloudlibrary.mapper.CommentMapper;
import com.group1.cloudlibrary.mapper.FavoriteMapper;
import com.group1.cloudlibrary.mapper.FeedbackMapper;
import com.group1.cloudlibrary.mapper.NotificationMapper;
import com.group1.cloudlibrary.mapper.ReservationMapper;
import com.group1.cloudlibrary.mapper.SearchLogMapper;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.mapper.VerificationCodeMapper;
import com.group1.cloudlibrary.util.NicknameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员权限服务：普通用户 → 待激活管理员 → 正式管理员的转换，以及账号彻底删除。
 *
 * <p>三种角色状态：</p>
 * <ul>
 *   <li>普通用户：role=0，pending_admin=0 —— 用户端，能借书</li>
 *   <li>待激活管理员：role=1，pending_admin=1 —— 用户端，不能借书，只能还书</li>
 *   <li>正式管理员：role=1，pending_admin=0 —— 管理端，不能借书</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    /** 待激活管理员的有效期：10 天 */
    public static final int PENDING_ADMIN_DAYS = 10;

    /** 管理员封号最大长度（与 user.admin_title 字段一致） */
    public static final int ADMIN_TITLE_MAX_LENGTH = 50;

    /** 借阅状态：借阅中 */
    private static final int BORROW_ING = 0;
    /** 借阅状态：已归还 */
    private static final int BORROW_RETURNED = 1;
    /** 借阅状态：逾期 */
    private static final int BORROW_OVERDUE = 2;

    private final UserMapper userMapper;
    private final BorrowMapper borrowMapper;
    private final CommentMapper commentMapper;
    private final FavoriteMapper favoriteMapper;
    private final ReservationMapper reservationMapper;
    private final FeedbackMapper feedbackMapper;
    private final NotificationMapper notificationMapper;
    private final SearchLogMapper searchLogMapper;
    private final AnnouncementMapper announcementMapper;
    private final AnnouncementReadMapper announcementReadMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final ApplicationMapper applicationMapper;
    private final NotificationService notificationService;
    private final AdminGuard adminGuard;

    /**
     * 该用户未归还的图书数量（借阅中 + 逾期）
     */
    public long unreturnedCount(Long userId) {
        Long count = borrowMapper.selectCount(Wrappers.<Borrow>lambdaQuery()
                .eq(Borrow::getUserId, userId)
                .in(Borrow::getStatus, BORROW_ING, BORROW_OVERDUE));
        return count == null ? 0 : count;
    }

    /**
     * 设为管理员（只有最高管理员能操作）。
     *
     * <p>没有未归还图书 → 直接设为正式管理员；还有未归还图书 → 设为待激活管理员，
     * 10 天内还清全部图书即可转正，否则自动恢复为普通用户。</p>
     *
     * @return true 表示设为「待激活管理员」，false 表示直接成为正式管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean setAdmin(Long userId) {
        adminGuard.requireSuperAdmin("设置管理员");
        User user = requireUser(userId);
        if (adminGuard.isSuperAdmin(user)) {
            throw BusinessException.of("该用户已经是最高管理员");
        }
        boolean alreadyAdmin = user.getRole() != null && user.getRole() == 1
                && (user.getPendingAdmin() == null || user.getPendingAdmin() == 0);
        if (alreadyAdmin) {
            throw BusinessException.of("该用户已经是管理员");
        }
        boolean pending = unreturnedCount(userId) > 0;
        applyAdminState(userId, 1, pending ? 1 : 0,
                pending ? LocalDateTime.now().plusDays(PENDING_ADMIN_DAYS) : null);
        if (pending) {
            notificationService.send(userId, NotificationService.TYPE_ADMIN_PENDING, "管理员权限待激活",
                    "您已被设为管理员，请在10天内归还所有图书，归还成功后即可行使管理员权限。");
            log.info("用户设为待激活管理员：userId={}, deadline={}", userId, LocalDateTime.now().plusDays(PENDING_ADMIN_DAYS));
        } else {
            notificationService.send(userId, NotificationService.TYPE_ADMIN_ACTIVATED, "管理员权限已开通",
                    "您已正式成为管理员，现在可以行使管理员权限了。");
            log.info("用户直接设为正式管理员：userId={}", userId);
        }
        return pending;
    }

    /**
     * 取消管理员权限（只有最高管理员能操作），恢复为普通用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelAdmin(Long userId) {
        adminGuard.requireSuperAdmin("取消管理员");
        User user = requireUser(userId);
        if (adminGuard.isSuperAdmin(user)) {
            throw BusinessException.of("不能取消最高管理员的权限");
        }
        applyAdminState(userId, 0, 0, null);
        notificationService.send(userId, NotificationService.TYPE_ADMIN_CANCELED, "管理员权限已取消",
                "您的管理员权限已被取消，已恢复为普通用户。");
        log.info("取消管理员权限：userId={}", userId);
    }

    /**
     * 设置 / 清除管理员封号（只有最高管理员能操作），如「图书管理专员」
     *
     * @param title 封号，留空表示清除
     */
    @Transactional(rollbackFor = Exception.class)
    public void setAdminTitle(Long userId, String title) {
        adminGuard.requireSuperAdmin("设置管理员封号");
        User user = requireUser(userId);
        if (adminGuard.isSuperAdmin(user)) {
            throw BusinessException.of("最高管理员不需要设置封号");
        }
        if (!isFormalAdmin(user)) {
            throw BusinessException.of("只能给正式管理员设置封号");
        }
        String value = StringUtils.hasText(title) ? title.trim() : null;
        if (value != null && value.length() > ADMIN_TITLE_MAX_LENGTH) {
            throw BusinessException.of("封号不能超过 " + ADMIN_TITLE_MAX_LENGTH + " 个字符");
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getAdminTitle, value)
                .set(User::getUpdatedAt, LocalDateTime.now()));
        log.info("设置管理员封号：userId={}, title={}", userId, value);
    }

    /**
     * 归还图书后调用：待激活管理员还清全部图书 → 自动转正为正式管理员，并通知本人
     */
    @Transactional(rollbackFor = Exception.class)
    public void promoteIfBooksReturned(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getPendingAdmin() == null || user.getPendingAdmin() != 1) {
            return;
        }
        if (unreturnedCount(userId) > 0) {
            return;
        }
        applyAdminState(userId, 1, 0, null);
        notificationService.send(userId, NotificationService.TYPE_ADMIN_ACTIVATED, "管理员权限已开通",
                "您已正式成为管理员，现在可以行使管理员权限了。");
        log.info("待激活管理员已还清图书，自动转正：userId={}", userId);
    }

    /**
     * 定时任务调用：超过截止时间仍未还清图书的待激活管理员 → 转换失败，恢复为普通用户，
     * 并分别通知本人与最高管理员。
     *
     * @return 本次转换失败的人数
     */
    @Transactional(rollbackFor = Exception.class)
    public int scanExpiredPendingAdmins() {
        List<User> expiredList = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getPendingAdmin, 1)
                .isNotNull(User::getPendingAdminDeadline)
                .lt(User::getPendingAdminDeadline, LocalDateTime.now()));
        if (expiredList.isEmpty()) {
            return 0;
        }
        List<Long> superAdminIds = adminGuard.superAdminIds();
        for (User user : expiredList) {
            applyAdminState(user.getId(), 0, 0, null);
            String name = displayName(user);
            notificationService.send(user.getId(), NotificationService.TYPE_ADMIN_FAILED, "管理员转换失败",
                    "您因10天内未归还全部图书，管理员转换失败，已恢复为普通用户。");
            for (Long superId : superAdminIds) {
                notificationService.send(superId, NotificationService.TYPE_ADMIN_FAILED_SUPER, "管理员转换失败提醒",
                        "您之前设置为管理员的用户 " + name + "，因10天内未归还全部图书，转换失败，已恢复为普通用户。");
            }
            log.info("待激活管理员超期，转换失败：userId={}, email={}", user.getId(), user.getEmail());
        }
        return expiredList.size();
    }

    /**
     * 彻底删除用户及其全部关联数据（外键依赖顺序：子表 → 父表 → 用户本身）。
     * 只有最高管理员本人，或已经过超管审批的操作申请，才会走到这里。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserCompletely(Long userId) {
        User user = requireUser(userId);
        if (adminGuard.isSuperAdmin(user)) {
            throw BusinessException.of("不能删除最高管理员账号");
        }
        // 1. 评论相关：先取「该用户发表的评论 + 别人对他评论的回复」，再按依赖顺序清理
        List<Long> commentIds = new ArrayList<>(commentMapper.selectCommentIdsByUser(userId));
        commentIds.addAll(commentMapper.selectReplyIdsToUser(userId));
        commentMapper.deleteLikeByUser(userId, commentIds);
        commentMapper.deleteReportByUser(userId, commentIds);
        commentMapper.deleteDislikeByUser(userId, commentIds);
        commentMapper.deleteRepliesToUserComments(userId);
        commentMapper.deleteByUser(userId);
        // 2. 借阅 / 预约 / 收藏
        borrowMapper.delete(Wrappers.<Borrow>lambdaQuery().eq(Borrow::getUserId, userId));
        reservationMapper.delete(Wrappers.<Reservation>lambdaQuery().eq(Reservation::getUserId, userId));
        favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery().eq(Favorite::getUserId, userId));
        // 3. 反馈 / 通知 / 搜索记录 / 公告已读
        feedbackMapper.delete(Wrappers.<Feedback>lambdaQuery().eq(Feedback::getUserId, userId));
        notificationMapper.delete(Wrappers.<Notification>lambdaQuery().eq(Notification::getUserId, userId));
        searchLogMapper.delete(Wrappers.<SearchLog>lambdaQuery().eq(SearchLog::getUserId, userId));
        announcementReadMapper.deleteByUser(userId);
        // 4. 该用户发布的公告 / 他提交过的操作申请 / 邮箱验证码
        announcementMapper.delete(Wrappers.<Announcement>lambdaQuery().eq(Announcement::getAdminId, userId));
        applicationMapper.delete(Wrappers.<Application>lambdaQuery().eq(Application::getApplicantId, userId));
        verificationCodeMapper.delete(Wrappers.<VerificationCode>lambdaQuery().eq(VerificationCode::getEmail, user.getEmail()));
        // 5. 最后删用户本身
        userMapper.deleteById(userId);
        log.info("删除用户及其全部关联数据：userId={}, email={}", userId, user.getEmail());
    }

    /** 是否正式管理员：role=1 且 pending_admin=0 */
    private boolean isFormalAdmin(User user) {
        return user.getRole() != null && user.getRole() == 1
                && (user.getPendingAdmin() == null || user.getPendingAdmin() == 0);
    }

    /**
     * 统一更新 role / pending_admin / pending_admin_deadline（null 也要写进库，所以用 UpdateWrapper）。
     * 身份发生变化时封号一并清空（待激活 / 普通用户都不该保留封号）。
     */
    private void applyAdminState(Long userId, int role, int pendingAdmin, LocalDateTime deadline) {
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getRole, role)
                .set(User::getPendingAdmin, pendingAdmin)
                .set(User::getPendingAdminDeadline, deadline)
                .set(User::getAdminTitle, null)
                .set(User::getUpdatedAt, LocalDateTime.now()));
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        return user;
    }

    /** 展示名：昵称优先，没昵称回退邮箱前缀 */
    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : NicknameUtil.fromEmail(user.getEmail());
    }
}
