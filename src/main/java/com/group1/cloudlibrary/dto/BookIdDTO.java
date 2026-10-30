package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 仅包含图书ID的通用请求体（借阅 / 预约 / 收藏）
 */
@Data
public class BookIdDTO {

    @NotNull(message = "缺少图书ID")
    private Long bookId;
}
