package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterDTO {

    /**
     * 注册邮箱格式约束：
     * <ul>
     *   <li>整体必须是 xxx@xxx.xxx；</li>
     *   <li>@ 前面只能由字母、数字、下划线、点、减号组成，长度 1~30；</li>
     *   <li>@ 后面必须是主流邮箱域名（大小写均可）。</li>
     * </ul>
     * 只用于注册：老账号（历史遗留的其他域名）登录 / 找回密码不受影响。
     */
    public static final String EMAIL_PATTERN =
            "(?i)^[a-z0-9._-]{1,30}@(qq|163|126|gmail|outlook|hotmail|foxmail|sina|sohu|yahoo|aliyun|icloud|live)\\.com$";

    @NotBlank(message = "请输入邮箱地址")
    @Email(message = "邮箱格式不正确")
    @Pattern(regexp = EMAIL_PATTERN, message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "请设置密码")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String password;

    @NotBlank(message = "请再次确认密码")
    private String confirmPassword;
}
