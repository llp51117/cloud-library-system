package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginDTO {

    @NotBlank(message = "请输入邮箱地址")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "请输入密码")
    private String password;
}
