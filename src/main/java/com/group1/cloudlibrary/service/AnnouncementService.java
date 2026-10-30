package com.group1.cloudlibrary.service;

import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.dto.AnnouncementDTO;
import com.group1.cloudlibrary.entity.Announcement;
import com.group1.cloudlibrary.mapper.AnnouncementMapper;
import com.group1.cloudlibrary.mapper.AnnouncementReadMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final AnnouncementReadMapper announcementReadMapper;

    /**
     * 用户端：查询有效公告列表
     */
    public List<Announcement> listActive(Long userId) {
        return announcementMapper.selectActiveList(userId);
    }

    /**
     * 系统公告未读数量：有效公告总数 - 本人已读数量（每次实时查库，不做缓存）
     */
    public long unreadCount(Long userId) {
        long total = announcementReadMapper.countActiveAnnouncements();
        long read = announcementReadMapper.countReadByUser(userId);
        return Math.max(total - read, 0);
    }

    /**
     * 系统公告全部标记已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        announcementReadMapper.markAllRead(userId);
    }

    /**
     * 单条公告标记已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long announcementId) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null || announcement.getStatus() == null || announcement.getStatus() != 1) {
            return;
        }
        announcementReadMapper.markRead(userId, announcementId);
    }

    /**
     * 管理端：查询全部公告（含已删除）
     */
    public List<Announcement> listAll() {
        return announcementMapper.selectAllWithAdmin();
    }

    /**
     * 公告详情
     */
    public Announcement detail(Long id) {
        Announcement announcement = announcementMapper.selectDetailById(id);
        if (announcement == null) {
            throw BusinessException.of("公告不存在");
        }
        return announcement;
    }

    /**
     * 发布公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Long publish(AnnouncementDTO dto, Long adminId) {
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setAdminId(adminId);
        announcement.setStatus(1);
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.insert(announcement);
        log.info("管理员 {} 发布公告：{}", adminId, dto.getTitle());
        return announcement.getId();
    }

    /**
     * 编辑公告
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(AnnouncementDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.of("缺少公告ID");
        }
        Announcement announcement = announcementMapper.selectById(dto.getId());
        if (announcement == null) {
            throw BusinessException.of("公告不存在");
        }
        if (StringUtils.hasText(dto.getTitle())) {
            announcement.setTitle(dto.getTitle());
        }
        if (StringUtils.hasText(dto.getContent())) {
            announcement.setContent(dto.getContent());
        }
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);
    }

    /**
     * 删除公告（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.of("公告不存在");
        }
        announcement.setStatus(0);
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);
    }
}
