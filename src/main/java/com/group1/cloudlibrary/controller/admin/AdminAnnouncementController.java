package com.group1.cloudlibrary.controller.admin;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.AnnouncementDTO;
import com.group1.cloudlibrary.entity.Announcement;
import com.group1.cloudlibrary.service.AnnouncementService;
import com.group1.cloudlibrary.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知公告接口（管理端）
 */
@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    /** 公告列表（按时间倒序，含已删除） */
    @GetMapping
    public Result<List<Announcement>> list() {
        return Result.success(announcementService.listAll());
    }

    /** 发布公告 */
    @PostMapping
    public Result<Long> publish(@Valid @RequestBody AnnouncementDTO dto) {
        Long id = announcementService.publish(dto, UserContext.requireUserId());
        return Result.success("公告发布成功", id);
    }

    /** 编辑公告 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AnnouncementDTO dto) {
        dto.setId(id);
        announcementService.update(dto);
        return Result.success("公告修改成功", null);
    }

    /** 删除公告 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success("公告已删除", null);
    }
}
