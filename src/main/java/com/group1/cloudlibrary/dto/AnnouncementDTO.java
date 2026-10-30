package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公告发布 / 编辑请求（管理端）
 */
@Data
public class AnnouncementDTO {

    private Long id;

    @NotBlank(message = "请输入公告标题")
    private String title;

    @NotBlank(message = "请输入公告内容")
    private String content;
}
