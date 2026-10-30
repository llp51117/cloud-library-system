package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改邮箱请求（需邮件验证码）
 */
@Data
public class UpdateEmailDTO {

    @NotBlank(message = "请输入新邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "请输入验证码")
    private String code;
}
