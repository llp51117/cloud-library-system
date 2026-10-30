package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 禁用用户请求（管理端）
 */
@Data
public class BanUserDTO {

    @NotNull(message = "请填写禁用天数")
    @Min(value = 1, message = "禁用天数至少为 1 天")
    private Integer banDays;

    @NotBlank(message = "请填写禁用原因")
    private String banReason;
}
