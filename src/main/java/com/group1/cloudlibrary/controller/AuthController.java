package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.LoginDTO;
import com.group1.cloudlibrary.dto.RegisterDTO;
import com.group1.cloudlibrary.dto.ResetPasswordDTO;
import com.group1.cloudlibrary.dto.SendCodeDTO;
import com.group1.cloudlibrary.service.AuthService;
import com.group1.cloudlibrary.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 用户登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    /** 用户注册 */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success("注册成功", null);
    }

    /** 发送邮箱验证码（type：0注册 / 1找回密码） */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        authService.sendCode(dto.getEmail(), dto.getType());
        return Result.success("验证码已发送，请查收邮件", null);
    }

    /** 重置密码（忘记密码） */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.success("密码修改成功，请重新登录", null);
    }

    /** 退出登录（前端清除本地令牌即可） */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("已安全退出", null);
    }
}
