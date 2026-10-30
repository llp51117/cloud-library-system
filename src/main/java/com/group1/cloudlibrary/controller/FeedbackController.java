package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.FeedbackDTO;
import com.group1.cloudlibrary.entity.Feedback;
import com.group1.cloudlibrary.service.FeedbackService;
import com.group1.cloudlibrary.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 意见反馈接口（用户端）
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /** 提交意见反馈 */
    @PostMapping
    public Result<Long> submit(@Valid @RequestBody FeedbackDTO dto) {
        return Result.success("反馈提交成功，我们会尽快处理", feedbackService.submit(UserContext.requireUserId(), dto));
    }

    /** 我的反馈列表 */
    @GetMapping("/my")
    public Result<List<Feedback>> my() {
        return Result.success(feedbackService.myFeedbacks(UserContext.requireUserId()));
    }
}
