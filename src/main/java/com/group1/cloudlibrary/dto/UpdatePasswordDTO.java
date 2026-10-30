package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class UpdatePasswordDTO {

    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @NotBlank(message = "请设置新密码")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String newPassword;

    @NotBlank(message = "请再次确认新密码")
    private String confirmPassword;
}
