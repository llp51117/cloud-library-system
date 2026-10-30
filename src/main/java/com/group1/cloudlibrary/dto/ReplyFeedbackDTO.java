package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员回复反馈请求
 */
@Data
public class ReplyFeedbackDTO {

    @NotBlank(message = "请输入回复内容")
    private String reply;
}
