package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发表评论请求
 */
@Data
public class CommentDTO {

    @NotNull(message = "缺少图书ID")
    private Long bookId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** 父评论ID（楼中楼回复，只支持一层；顶级评论不传） */
    private Long parentId;
}
