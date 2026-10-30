package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送验证码请求
 */
@Data
public class SendCodeDTO {

    @NotBlank(message = "请输入邮箱地址")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 类型：0注册/1找回密码 */
    private Integer type = 1;
}
