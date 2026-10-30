package com.group1.cloudlibrary.controller.admin;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.ReplyFeedbackDTO;
import com.group1.cloudlibrary.entity.Feedback;
import com.group1.cloudlibrary.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户反馈管理接口（管理端）
 */
@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    /** 反馈列表（按状态筛选：0待回复 / 1已回复 / 不传为全部） */
    @GetMapping
    public Result<PageResult<Feedback>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(feedbackService.adminPage(page, size, status));
    }

    /** 待回复反馈数量（管理端「用户管理」Tab 红点） */
    @GetMapping("/pending-count")
    public Result<Long> pendingCount() {
        return Result.success(feedbackService.pendingCount());
    }

    /** 回复反馈 */
    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody ReplyFeedbackDTO dto) {
        feedbackService.reply(id, dto.getReply());
        return Result.success("回复成功，已通知用户", null);
    }
}
