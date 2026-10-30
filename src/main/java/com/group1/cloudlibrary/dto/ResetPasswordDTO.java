package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码 / 重置密码请求
 */
@Data
public class ResetPasswordDTO {

    @NotBlank(message = "请输入注册邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "请输入验证码")
    private String code;

    @NotBlank(message = "请设置新密码")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String newPassword;

    @NotBlank(message = "请再次确认新密码")
    private String confirmPassword;
}
