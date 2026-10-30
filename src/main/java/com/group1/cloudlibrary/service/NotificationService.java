package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.entity.Notification;
import com.group1.cloudlibrary.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务：个人通知的生成、查询与已读标记
 * <p>通知类型：0系统公告/1借阅成功/2预约轮到/3预约超时/4还书成功/5逾期提醒/6反馈回复/7催还提醒/8管理员强制归还</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final int TYPE_ANNOUNCEMENT = 0;
    public static final int TYPE_BORROW_SUCCESS = 1;
    public static final int TYPE_RESERVATION_READY = 2;
    public static final int TYPE_RESERVATION_TIMEOUT = 3;
    public static final int TYPE_RETURN_SUCCESS = 4;
    public static final int TYPE_OVERDUE = 5;
    public static final int TYPE_FEEDBACK_REPLY = 6;
    public static final int TYPE_REMIND_RETURN = 7;
    /**
     * 管理员强制归还（区别于 TYPE_RETURN_SUCCESS：用户自己归还）
     * <p>注：「强制归还」功能已下线（已归还只由用户自己在用户端触发）；
     * 保留该常量仅用于兼容历史已生成的通知记录，业务逻辑不再产生该类型。</p>
     */
    public static final int TYPE_ADMIN_FORCE_RETURN = 8;
    /** 被设为待激活管理员（需在 10 天内归还全部图书） */
    public static final int TYPE_ADMIN_PENDING = 9;
    /** 正式成为管理员 */
    public static final int TYPE_ADMIN_ACTIVATED = 10;
    /** 管理员转换失败（10 天内未归还全部图书，恢复为普通用户）——发给当事人 */
    public static final int TYPE_ADMIN_FAILED = 11;
    /** 操作申请的审批结果——发给申请人 */
    public static final int TYPE_APPLICATION_RESULT = 12;
    /** 管理员转换失败——发给最高管理员（超管） */
    public static final int TYPE_ADMIN_FAILED_SUPER = 13;
    /** 取消管理员权限 */
    public static final int TYPE_ADMIN_CANCELED = 14;
    /** 有新的操作申请待审批——发给最高管理员 */
    public static final int TYPE_APPLICATION_SUBMIT = 15;

    private final NotificationMapper notificationMapper;

    /**
     * 发送一条个人通知
     */
    public void send(Long userId, int type, String title, String content) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        log.debug("发送通知：userId={}, type={}, title={}", userId, type, title);
    }

    /**
     * 查询我的通知（分页，可按多个类型筛选：如「预约相关」传 [2, 3]）
     */
    public PageResult<Notification> myNotifications(Long userId, long page, long size, List<Integer> types) {
        IPage<Notification> result = notificationMapper.selectPage(new Page<>(page, size),
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getUserId, userId)
                        .in(types != null && !types.isEmpty(), Notification::getType, types)
                        .orderByDesc(Notification::getCreatedAt));
        return PageResult.of(result);
    }

    /**
     * 我的通知各类型数量（type → 条数），供前端筛选下拉显示各类型条数
     */
    public Map<Integer, Long> typeCounts(Long userId) {
        Map<Integer, Long> counts = new HashMap<>();
        for (Map<String, Object> row : notificationMapper.countGroupByType(userId)) {
            counts.put(((Number) row.get("type")).intValue(), ((Number) row.get("cnt")).longValue());
        }
        return counts;
    }

    /**
     * 未读数量
     */
    public long unreadCount(Long userId) {
        Long count = notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return count == null ? 0 : count;
    }

    /**
     * 单条标记已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id, Long userId) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            return;
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }

    /**
     * 全部标记已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }
}
