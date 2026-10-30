package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 验证登录密码请求（修改邮箱场景使用）
 */
@Data
public class PasswordVerifyDTO {

    /** 当前登录密码 */
    @NotBlank(message = "请输入登录密码")
    private String password;
}
