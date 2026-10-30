package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.config.CloudLibraryProperties;
import com.group1.cloudlibrary.dto.LoginDTO;
import com.group1.cloudlibrary.dto.RegisterDTO;
import com.group1.cloudlibrary.dto.ResetPasswordDTO;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.entity.VerificationCode;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.mapper.VerificationCodeMapper;
import com.group1.cloudlibrary.util.JwtUtils;
import com.group1.cloudlibrary.util.NicknameUtil;
import com.group1.cloudlibrary.util.PasswordUtil;
import com.group1.cloudlibrary.util.RedisUtil;
import com.group1.cloudlibrary.vo.LoginVO;
import com.group1.cloudlibrary.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 用户认证服务：注册、登录、验证码、找回密码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 验证码有效期（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final String CODE_KEY_PREFIX = "code:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final VerificationCodeMapper codeMapper;
    private final MailService mailService;
    private final JwtUtils jwtUtils;
    private final RedisUtil redisUtil;
    private final CloudLibraryProperties properties;

    /**
     * 注册
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw BusinessException.of("两次输入的密码不一致");
        }
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getEmail, dto.getEmail()));
        if (count != null && count > 0) {
            throw BusinessException.of("邮箱已被注册，是否忘记密码？");
        }
        User user = new User();
        user.setEmail(dto.getEmail());
        // 注册时给个默认昵称：邮箱 @ 前面的部分
        user.setNickname(NicknameUtil.fromEmail(dto.getEmail()));
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        user.setBanDays(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        log.info("新用户注册成功: {}", dto.getEmail());
    }

    /**
     * 登录
     */
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, dto.getEmail()));
        if (user == null) {
            throw BusinessException.of("邮箱号错误");
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw BusinessException.of("密码错误");
        }
        if (user.getStatus() == 0) {
            throw BusinessException.of("您的账号已被注销，无法登录");
        }
        if (user.getStatus() == 2) {
            // 检查禁用是否到期，到期自动恢复正常
            if (isBanExpired(user)) {
                user.setStatus(1);
                user.setBanReason(null);
                user.setBanDays(0);
                user.setBanStartTime(null);
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
            } else {
                throw BusinessException.of("您的账号已被禁用，原因：" + user.getBanReason());
            }
        }
        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new LoginVO(token, UserVO.from(user));
    }

    /**
     * 发送验证码
     *
     * @param type 0注册/1找回密码/2修改邮箱
     */
    @Transactional(rollbackFor = Exception.class)
    public void sendCode(String email, Integer type) {
        int codeType = type == null ? 1 : type;
        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
        if (codeType == 0 && exists != null && exists > 0) {
            throw BusinessException.of("邮箱已被注册，是否忘记密码？");
        }
        // 找回密码 / 修改邮箱：邮箱不存在统一提示「邮箱号错误」，避免暴露哪些邮箱已注册
        if (codeType != 0 && (exists == null || exists == 0)) {
            throw BusinessException.of("邮箱号错误");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();

        // 清理旧验证码并写入新验证码
        codeMapper.deleteByEmailAndType(email, codeType);
        VerificationCode entity = new VerificationCode();
        entity.setEmail(email);
        entity.setCode(code);
        entity.setType(codeType);
        entity.setExpiredAt(now.plusMinutes(CODE_EXPIRE_MINUTES));
        entity.setCreatedAt(now);
        codeMapper.insert(entity);

        // Redis 缓存（Redis 不可用时自动降级为仅数据库校验）
        redisUtil.set(CODE_KEY_PREFIX + codeType + ":" + email, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        mailService.sendVerificationCode(email, code, codeType);
        log.info("验证码已生成: email={}, code={}", email, code);
    }

    /**
     * 重置密码（忘记密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw BusinessException.of("两次输入的新密码不一致");
        }
        verifyCode(dto.getEmail(), dto.getCode(), 1);
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, dto.getEmail()));
        if (user == null) {
            throw BusinessException.of("该邮箱尚未注册");
        }
        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        consumeCode(dto.getEmail(), 1);
        log.info("用户重置密码成功: {}", dto.getEmail());
    }

    /**
     * 校验验证码（先查 Redis，再查数据库）
     */
    public void verifyCode(String email, String code, int type) {
        String cacheKey = CODE_KEY_PREFIX + type + ":" + email;
        Object cached = redisUtil.get(cacheKey);
        if (cached != null && code.equals(String.valueOf(cached))) {
            return;
        }
        VerificationCode entity = codeMapper.selectOne(Wrappers.<VerificationCode>lambdaQuery()
                .eq(VerificationCode::getEmail, email)
                .eq(VerificationCode::getType, type)
                .orderByDesc(VerificationCode::getId)
                .last("LIMIT 1"));
        if (entity == null) {
            throw BusinessException.of("请先获取验证码");
        }
        if (entity.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.of("验证码已过期，请重新获取");
        }
        if (!entity.getCode().equals(code)) {
            throw BusinessException.of("验证码错误");
        }
    }

    /**
     * 验证码用掉后立即失效（Redis + 数据库一起清），防止同一个码被重复使用
     */
    public void consumeCode(String email, int type) {
        redisUtil.delete(CODE_KEY_PREFIX + type + ":" + email);
        codeMapper.deleteByEmailAndType(email, type);
    }

    /**
     * 判断禁用是否已到期
     */
    private boolean isBanExpired(User user) {
        if (user.getBanStartTime() == null || user.getBanDays() == null) {
            return true;
        }
        return user.getBanStartTime().plusDays(user.getBanDays()).isBefore(LocalDateTime.now());
    }
}
