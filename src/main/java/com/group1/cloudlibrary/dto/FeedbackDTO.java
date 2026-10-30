package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交意见反馈请求
 */
@Data
public class FeedbackDTO {

    /** 类型：0功能建议/1举报用户/2内容错误/3系统BUG/4其他 */
    @NotNull(message = "请选择反馈类型")
    private Integer type;

    @NotBlank(message = "请输入标题")
    private String title;

    @NotBlank(message = "请输入反馈内容")
    private String content;
}
