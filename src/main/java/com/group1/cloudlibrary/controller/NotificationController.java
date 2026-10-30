package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.UnreadCountDTO;
import com.group1.cloudlibrary.entity.Announcement;
import com.group1.cloudlibrary.entity.Notification;
import com.group1.cloudlibrary.service.AnnouncementService;
import com.group1.cloudlibrary.service.NotificationService;
import com.group1.cloudlibrary.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 消息通知接口（用户端）：系统公告 + 个人通知
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AnnouncementService announcementService;

    /** 系统公告列表（所有用户可见） */
    @GetMapping("/announcements")
    public Result<List<Announcement>> announcements() {
        return Result.success(announcementService.listActive(UserContext.requireUserId()));
    }

    /** 我的个人通知列表（types 支持多选筛选，如「预约相关」传 types=2,3） */
    @GetMapping
    public Result<PageResult<Notification>> my(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) List<Integer> types) {
        return Result.success(notificationService.myNotifications(UserContext.requireUserId(), page, size, types));
    }

    /** 我的通知各类型数量（供前端筛选下拉显示每个类型的条数） */
    @GetMapping("/type-counts")
    public Result<Map<Integer, Long>> typeCounts() {
        return Result.success(notificationService.typeCounts(UserContext.requireUserId()));
    }

    /** 未读数量：分别给出「我的通知」「系统公告」的数量以及合计 */
    @GetMapping("/unread-count")
    public Result<UnreadCountDTO> unreadCount() {
        Long userId = UserContext.requireUserId();
        UnreadCountDTO dto = new UnreadCountDTO();
        long notification = notificationService.unreadCount(userId);
        long announcement = announcementService.unreadCount(userId);
        dto.setNotification(notification);
        dto.setAnnouncement(announcement);
        dto.setTotal(notification + announcement);
        return Result.success(dto);
    }

    /** 系统公告全部标记已读 */
    @PutMapping("/announcements/read-all")
    public Result<Void> markAllAnnouncementRead() {
        announcementService.markAllRead(UserContext.requireUserId());
        return Result.success();
    }

    /** 单条系统公告标记已读 */
    @PutMapping("/announcements/{id}/read")
    public Result<Void> markAnnouncementRead(@PathVariable Long id) {
        announcementService.markRead(UserContext.requireUserId(), id);
        return Result.success();
    }

    /** 单条标记已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, UserContext.requireUserId());
        return Result.success();
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(UserContext.requireUserId());
        return Result.success("已全部标记为已读", null);
    }
}
