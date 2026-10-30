package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.dto.FeedbackDTO;
import com.group1.cloudlibrary.entity.Feedback;
import com.group1.cloudlibrary.mapper.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 意见反馈服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final int STATUS_PENDING = 0;
    /** 已回复 */
    private static final int STATUS_REPLIED = 1;

    private static final String[] TYPE_NAMES = {"功能建议", "举报用户", "内容错误", "系统BUG", "其他"};

    private final FeedbackMapper feedbackMapper;
    private final NotificationService notificationService;

    /**
     * 提交反馈
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, FeedbackDTO dto) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(dto.getType());
        feedback.setTitle(dto.getTitle());
        feedback.setContent(dto.getContent());
        feedback.setStatus(STATUS_PENDING);
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(feedback);
        return feedback.getId();
    }

    /**
     * 我的反馈列表
     */
    public List<Feedback> myFeedbacks(Long userId) {
        return feedbackMapper.selectMyFeedbacks(userId);
    }

    /**
     * 管理端：反馈分页查询
     *
     * @param status 0待回复 / 1已回复 / null 全部
     */
    public PageResult<Feedback> adminPage(long page, long size, Integer status) {
        IPage<Feedback> result = feedbackMapper.selectFeedbackPage(new Page<>(page, size), status);
        return PageResult.of(result);
    }

    /**
     * 管理端：待回复反馈数量（顶栏「用户管理」Tab 的红点）
     */
    public long pendingCount() {
        Long count = feedbackMapper.selectCount(
                Wrappers.<Feedback>lambdaQuery().eq(Feedback::getStatus, STATUS_PENDING));
        return count == null ? 0 : count;
    }

    /**
     * 管理端：回复反馈，并给用户发送通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long feedbackId, String reply) {
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw BusinessException.of("反馈不存在");
        }
        feedback.setReply(reply);
        feedback.setStatus(STATUS_REPLIED);
        feedback.setReplyTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);

        String typeName = feedback.getType() != null && feedback.getType() < TYPE_NAMES.length
                ? TYPE_NAMES[feedback.getType()] : "反馈";
        notificationService.send(feedback.getUserId(), NotificationService.TYPE_FEEDBACK_REPLY,
                "您的反馈已回复",
                "您提交的【" + typeName + "】《" + feedback.getTitle() + "》已收到管理员回复：" + reply);
    }
}
