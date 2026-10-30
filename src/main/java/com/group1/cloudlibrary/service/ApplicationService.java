package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.dto.ApplicationDTO;
import com.group1.cloudlibrary.entity.Application;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.mapper.ApplicationMapper;
import com.group1.cloudlibrary.mapper.BookMapper;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.util.NicknameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 操作申请服务。
 *
 * <p>普通管理员不能直接执行「删除用户 / 注销用户 / 批量删除图书」，
 * 需要在管理端提交操作申请；最高管理员（超管）在「通知公告 → 操作申请」里审批，
 * 同意后由系统代为执行，并把结果通知申请人。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    /** 删除用户（彻底删除） */
    public static final String TYPE_DELETE_USER = "delete_user";
    /** 批量删除图书 */
    public static final String TYPE_BATCH_DELETE_BOOK = "batch_delete_book";
    /** 注销用户 */
    public static final String TYPE_CANCEL_USER = "cancel_user";
    /** 其他 */
    public static final String TYPE_OTHER = "other";

    /** 状态：待审批 */
    private static final int STATUS_PENDING = 0;

    private static final Set<String> VALID_TYPES = Set.of(
            TYPE_DELETE_USER, TYPE_BATCH_DELETE_BOOK, TYPE_CANCEL_USER, TYPE_OTHER);

    private final ApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final AdminGuard adminGuard;
    private final UserService userService;
    private final UserAdminService userAdminService;
    private final BookService bookService;
    private final NotificationService notificationService;

    // ==========================================================
    // 提交申请（普通管理员）
    // ==========================================================

    /**
     * 提交操作申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long apply(ApplicationDTO dto) {
        User applicant = adminGuard.currentUser();
        if (applicant == null) {
            throw BusinessException.of("请先登录");
        }
        if (applicant.getRole() == null || applicant.getRole() != 1) {
            throw BusinessException.of("只有管理员才能提交操作申请");
        }
        if (adminGuard.isSuperAdmin(applicant)) {
            throw BusinessException.of("您是最高管理员，可以直接执行该操作，无需提交申请");
        }
        if (applicant.getPendingAdmin() != null && applicant.getPendingAdmin() == 1) {
            throw BusinessException.of("待激活管理员还不能提交操作申请，请先归还全部图书");
        }
        String type = dto.getType() == null ? "" : dto.getType().trim();
        if (!VALID_TYPES.contains(type)) {
            throw BusinessException.of("申请类型不合法");
        }
        String targetId = normalizeTargetId(dto.getTargetId());
        validateTarget(type, targetId);

        Application application = new Application();
        application.setApplicantId(applicant.getId());
        application.setType(type);
        // 没填目标（如「其他」类型）一律写 NULL，target_id 为空时不报错
        application.setTargetId(StringUtils.hasText(targetId) ? targetId : null);
        application.setContent(dto.getContent());
        application.setStatus(STATUS_PENDING);
        application.setCreatedAt(LocalDateTime.now());
        applicationMapper.insert(application);

        String applicantName = displayName(applicant);
        String summary = "【" + typeLabel(type) + "】目标：" + (StringUtils.hasText(targetId) ? targetId : "无");
        for (Long superId : adminGuard.superAdminIds()) {
            notificationService.send(superId, NotificationService.TYPE_APPLICATION_SUBMIT, "有新的操作申请待审批",
                    "管理员 " + applicantName + " 提交了" + summary + "，理由：" + dto.getContent()
                            + "。请前往「通知公告 → 操作申请」审批。");
        }
        log.info("提交操作申请：applicantId={}, type={}, targetId={}", applicant.getId(), type, targetId);
        return application.getId();
    }

    // ==========================================================
    // 审批（最高管理员）
    // ==========================================================

    /**
     * 同意申请：先执行对应操作，再把状态置为「已同意」，并通知申请人
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, String reply) {
        adminGuard.requireSuperAdmin("审批操作申请");
        Application application = requirePending(id);
        String executed;
        switch (application.getType()) {
            case TYPE_DELETE_USER -> {
                userAdminService.deleteUserCompletely(parseSingleTarget(application.getTargetId()));
                executed = "已删除该用户及其全部关联数据";
            }
            case TYPE_CANCEL_USER -> {
                userService.cancelUser(parseSingleTarget(application.getTargetId()));
                executed = "已注销该用户";
            }
            case TYPE_BATCH_DELETE_BOOK -> {
                List<Long> bookIds = parseTargetIds(application.getTargetId());
                bookService.batchDelete(bookIds);
                executed = "已删除 " + bookIds.size() + " 本图书及其关联数据";
            }
            default -> executed = "其他类型申请，无需系统执行，已记录审批结果";
        }
        String finalReply = StringUtils.hasText(reply) ? reply.trim() : "同意执行";
        finish(application, 1, finalReply);
        notificationService.send(application.getApplicantId(), NotificationService.TYPE_APPLICATION_RESULT, "操作申请已通过",
                "您提交的「" + typeLabel(application.getType()) + "」申请已通过，" + executed
                        + "。最高管理员回复：" + finalReply);
        log.info("操作申请已通过：id={}, type={}, targetId={}", id, application.getType(), application.getTargetId());
    }

    /**
     * 拒绝申请：不执行任何操作，状态置为「已拒绝」，并通知申请人
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reply) {
        adminGuard.requireSuperAdmin("审批操作申请");
        Application application = requirePending(id);
        String finalReply = StringUtils.hasText(reply) ? reply.trim() : "不予通过";
        finish(application, 2, finalReply);
        notificationService.send(application.getApplicantId(), NotificationService.TYPE_APPLICATION_RESULT, "操作申请被拒绝",
                "您提交的「" + typeLabel(application.getType()) + "」申请已被拒绝，操作未执行。拒绝理由：" + finalReply);
        log.info("操作申请被拒绝：id={}, type={}", id, application.getType());
    }

    // ==========================================================
    // 查询
    // ==========================================================

    /**
     * 操作申请分页列表（最高管理员在「通知公告 → 操作申请」里查看）
     *
     * @param status      状态筛选（null 表示全部）
     * @param onlyMine    是否只看自己提交的申请
     */
    public PageResult<Application> page(long page, long size, Integer status, boolean onlyMine) {
        User current = adminGuard.currentUser();
        Long applicantId = null;
        if (onlyMine) {
            if (current == null) {
                throw BusinessException.of("请先登录");
            }
            applicantId = current.getId();
        } else {
            adminGuard.requireSuperAdmin("查看全部操作申请");
        }
        IPage<Application> result = applicationMapper.selectApplicationPage(new Page<>(page, size), status, applicantId);
        fillTargetLabels(result.getRecords());
        return PageResult.of(result);
    }

    /** 待审批数量（超管首页角标用） */
    public long pendingCount() {
        Long count = applicationMapper.selectCount(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Application>lambdaQuery()
                        .eq(Application::getStatus, STATUS_PENDING));
        return count == null ? 0 : count;
    }

    // ==========================================================
    // 内部方法
    // ==========================================================

    private Application requirePending(Long id) {
        Application application = applicationMapper.selectById(id);
        if (application == null) {
            throw BusinessException.of("操作申请不存在");
        }
        if (application.getStatus() == null || application.getStatus() != STATUS_PENDING) {
            throw BusinessException.of("该申请已处理，不能重复审批");
        }
        return application;
    }

    private void finish(Application application, int status, String reply) {
        application.setStatus(status);
        application.setReply(reply);
        application.setReplyTime(LocalDateTime.now());
        applicationMapper.updateById(application);
    }

    /** delete_user / cancel_user 只能针对单个用户，batch_delete_book 支持多个ID */
    private void validateTarget(String type, String targetId) {
        switch (type) {
            case TYPE_DELETE_USER, TYPE_CANCEL_USER -> {
                Long userId = parseSingleTarget(targetId);
                User user = userMapper.selectById(userId);
                if (user == null) {
                    throw BusinessException.of("目标用户不存在（ID：" + userId + "）");
                }
                if (adminGuard.isSuperAdmin(user)) {
                    throw BusinessException.of("不能对最高管理员发起该申请");
                }
            }
            case TYPE_BATCH_DELETE_BOOK -> {
                List<Long> bookIds = parseTargetIds(targetId);
                Long exists = bookMapper.selectCount(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Book>lambdaQuery()
                        .in(Book::getId, bookIds));
                if (exists == null || exists == 0) {
                    throw BusinessException.of("目标图书不存在，请检查图书ID");
                }
            }
            default -> {
                // other 类型不校验目标
            }
        }
    }

    /**
     * 规范化操作目标ID：
     * 1. 没填 → 返回 null（不写库，「其他」类型允许不填目标）
     * 2. 填了但含非数字段 → 直接报错，避免把文字写进 target_id 触发
     *    MySQL 的 "Data truncated for column 'target_id'"
     */
    private String normalizeTargetId(String rawTargetId) {
        String text = StringUtils.hasText(rawTargetId) ? rawTargetId.trim() : "";
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (String part : text.split("[,，\\s]+")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            if (!part.trim().matches("\\d+")) {
                throw BusinessException.of("目标 ID 必须是数字：" + part.trim());
            }
        }
        return text;
    }

    /** 解析单个目标ID */
    private Long parseSingleTarget(String targetId) {
        List<Long> ids = parseTargetIds(targetId);
        if (ids.isEmpty()) {
            throw BusinessException.of("请填写操作目标ID");
        }
        return ids.get(0);
    }

    /** 解析目标ID列表（支持英文逗号 / 中文逗号 / 空格分隔） */
    private List<Long> parseTargetIds(String targetId) {
        List<Long> ids = new ArrayList<>();
        if (!StringUtils.hasText(targetId)) {
            return ids;
        }
        for (String part : targetId.split("[,，\\s]+")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                ids.add(Long.valueOf(part.trim()));
            } catch (NumberFormatException e) {
                throw BusinessException.of("目标 ID 必须是数字：" + part.trim());
            }
        }
        return ids;
    }

    /** 给申请记录补上「目标的可读名称」，方便超管审批时看清对象 */
    private void fillTargetLabels(List<Application> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> bookIds = new HashSet<>();
        for (Application application : list) {
            for (Long targetId : parseTargetIds(application.getTargetId())) {
                if (TYPE_BATCH_DELETE_BOOK.equals(application.getType())) {
                    bookIds.add(targetId);
                } else {
                    userIds.add(targetId);
                }
            }
        }
        Map<Long, String> userLabels = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                userLabels.put(user.getId(), displayName(user) + "（" + user.getEmail() + "）");
            }
        }
        Map<Long, String> bookLabels = new HashMap<>();
        if (!bookIds.isEmpty()) {
            for (Book book : bookMapper.selectBatchIds(bookIds)) {
                bookLabels.put(book.getId(), book.getTitle());
            }
        }
        for (Application application : list) {
            List<String> labels = new ArrayList<>();
            for (Long targetId : parseTargetIds(application.getTargetId())) {
                String label = TYPE_BATCH_DELETE_BOOK.equals(application.getType())
                        ? bookLabels.get(targetId) : userLabels.get(targetId);
                labels.add(label == null ? ("ID " + targetId) : label);
            }
            application.setTargetLabel(labels.isEmpty() ? null : String.join("、", labels));
        }
    }

    /** 申请类型的中文名 */
    public static String typeLabel(String type) {
        if (type == null) {
            return "其他";
        }
        return switch (type) {
            case TYPE_DELETE_USER -> "删除用户";
            case TYPE_CANCEL_USER -> "注销用户";
            case TYPE_BATCH_DELETE_BOOK -> "批量删除图书";
            default -> "其他";
        };
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : NicknameUtil.fromEmail(user.getEmail());
    }
}
