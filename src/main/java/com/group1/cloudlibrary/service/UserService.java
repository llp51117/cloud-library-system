package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.dto.BanUserDTO;
import com.group1.cloudlibrary.dto.UpdateEmailDTO;
import com.group1.cloudlibrary.dto.UpdatePasswordDTO;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.entity.UserTheme;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.mapper.UserThemeMapper;
import com.group1.cloudlibrary.util.NicknameUtil;
import com.group1.cloudlibrary.util.PasswordUtil;
import com.group1.cloudlibrary.vo.UserThemeVO;
import com.group1.cloudlibrary.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户服务：个人信息管理 + 管理端用户管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** 验证码类型：2 = 修改邮箱（0注册 / 1找回密码 / 2修改邮箱） */
    private static final int CODE_TYPE_CHANGE_EMAIL = 2;

    /** 默认主题：极光圣殿白（新用户注册后就是这套浅色主题） */
    public static final String DEFAULT_THEME = UserVO.DEFAULT_THEME;

    /**
     * 系统内置主题色标识（11 个，与前端 utils/themes.js 一一对应）
     * 存库时统一带模式后缀：{主题色}-dark / {主题色}-light（极光圣殿白除外，永远 aurora-white）。
     * 「超新星爆闪黄」已下线（和「熔核爆裂橙」太像），老数据回退到默认主题。
     */
    private static final Set<String> THEMES = UserVO.BUILT_IN_THEMES;

    /** 已下线的主题：库里还有老数据时按默认主题处理 */
    public static final Set<String> REMOVED_THEMES = Set.of("supernova-yellow");

    private final UserMapper userMapper;
    private final UserThemeMapper userThemeMapper;
    private final AuthService authService;
    private final MailService mailService;
    private final AdminGuard adminGuard;

    // ==========================================================
    // 用户端
    // ==========================================================

    public UserVO profile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        return UserVO.from(user);
    }

    /**
     * 查询当前用户的主题（皮肤）
     */
    public String getTheme(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        return UserVO.normalizeTheme(user.getTheme());
    }

    /**
     * 修改当前用户的主题（皮肤），主题存数据库、跟账号绑定
     * <p>允许三类主题标识：内置主题的深色版 / 浅色版（如 quantum-blue-dark、neon-pink-light）、
     * 极光圣殿白（aurora-white，只有一套）+ 该用户自己的自定义主题（custom-{id}）。</p>
     * <p>老数据（只有主题色、没有模式后缀）进来时自动补上该主题的默认模式，
     * 用户原来的样子不变。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public String updateTheme(Long userId, String theme) {
        String value = theme == null ? "" : theme.trim();
        boolean custom = value.startsWith(UserThemeVO.KEY_PREFIX);
        if (custom) {
            String idText = value.substring(UserThemeVO.KEY_PREFIX.length());
            Long themeId;
            try {
                themeId = Long.valueOf(idText);
            } catch (NumberFormatException e) {
                throw BusinessException.of("主题不存在，请选择一个有效主题");
            }
            Long owned = userThemeMapper.selectCount(Wrappers.<UserTheme>lambdaQuery()
                    .eq(UserTheme::getId, themeId)
                    .eq(UserTheme::getUserId, userId));
            if (owned == null || owned == 0) {
                throw BusinessException.of("主题不存在，或不属于当前账号");
            }
        } else {
            // 内置主题：拆出「主题色 + 模式」，校验主题色合法后规范化（补 / 去模式后缀）
            String base = UserVO.baseThemeOf(value);
            if (!THEMES.contains(base)) {
                throw BusinessException.of("主题不存在，请选择一个有效主题");
            }
            value = UserVO.normalizeTheme(value);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        user.setTheme(value);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return value;
    }

    /**
     * 更新头像
     */
    @Transactional(rollbackFor = Exception.class)
    public String updateAvatar(Long userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        user.setAvatar(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return avatarUrl;
    }

    /**
     * 修改昵称：保存到数据库，刷新后仍是新昵称；留空则恢复成邮箱前缀
     */
    @Transactional(rollbackFor = Exception.class)
    public String updateNickname(Long userId, String nickname) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        String value = StringUtils.hasText(nickname) ? nickname.trim() : NicknameUtil.fromEmail(user.getEmail());
        if (value.length() > 50) {
            throw BusinessException.of("昵称不能超过 50 个字符");
        }
        if (!StringUtils.hasText(value)) {
            value = "读者";
        }
        user.setNickname(value);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return value;
    }

    /**
     * 修改邮箱第一步：验证登录密码，通过后才返回当前完整邮箱。
     * 页面默认只展示打码邮箱，避免登录即可看到完整邮箱。
     */
    public String verifyPasswordAndGetEmail(Long userId, String password) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw BusinessException.of("密码错误，无法查看完整邮箱");
        }
        return user.getEmail();
    }

    /**
     * 修改邮箱第二步：把验证码发送到「当前（旧）邮箱」。
     * 邮箱由后端按 userId 取，前端无法指定，避免把验证码发到任意邮箱。
     */
    public void sendEmailChangeCode(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        authService.sendCode(user.getEmail(), CODE_TYPE_CHANGE_EMAIL);
    }

    /**
     * 修改邮箱：校验旧邮箱收到的验证码后保存新邮箱，并给旧邮箱发变更通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateEmail(Long userId, UpdateEmailDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        if (dto.getEmail().equals(user.getEmail())) {
            throw BusinessException.of("新邮箱与当前邮箱相同");
        }
        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, dto.getEmail())
                .ne(User::getId, userId));
        if (exists != null && exists > 0) {
            throw BusinessException.of("该邮箱已被其他账号使用");
        }
        // 验证码发到旧邮箱，用旧邮箱校验（type=2 修改邮箱）
        authService.verifyCode(user.getEmail(), dto.getCode(), CODE_TYPE_CHANGE_EMAIL);
        authService.consumeCode(user.getEmail(), CODE_TYPE_CHANGE_EMAIL);

        String oldEmail = user.getEmail();
        user.setEmail(dto.getEmail());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        mailService.sendNotify(oldEmail, "邮箱修改通知", "您的账号邮箱已成功修改为：" + dto.getEmail()
                + "\n如非本人操作，请立即联系管理员。");
    }

    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        if (!PasswordUtil.matches(dto.getOldPassword(), user.getPassword())) {
            throw BusinessException.of("原密码错误");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw BusinessException.of("两次输入的新密码不一致");
        }
        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    // ==========================================================
    // 管理端
    // ==========================================================

    /**
     * 用户列表（按邮箱 / 昵称搜索）
     * <p>排序：最高管理员永远排第一 → 管理员（正式在前、待激活在后）→ 普通用户，
     * 同一身份内按注册时间倒序。</p>
     */
    public PageResult<UserVO> adminPage(long page, long size, String keyword) {
        IPage<User> result = userMapper.selectPage(new Page<>(page, size), Wrappers.<User>lambdaQuery()
                .nested(StringUtils.hasText(keyword), q -> q.like(User::getEmail, keyword)
                        .or().like(User::getNickname, keyword))
                .orderByDesc(User::getIsSuperAdmin)
                .orderByDesc(User::getRole)
                .orderByAsc(User::getPendingAdmin)
                .orderByDesc(User::getCreatedAt));
        return PageResult.of(result, UserVO::from);
    }

    /**
     * 禁用用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long userId, BanUserDTO dto) {
        User user = requireUser(userId);
        if (adminGuard.isSuperAdmin(user)) {
            throw BusinessException.of("不能禁用最高管理员账号");
        }
        if (user.getRole() != null && user.getRole() == 1) {
            throw BusinessException.of("不能禁用管理员账号");
        }
        user.setStatus(2);
        user.setBanDays(dto.getBanDays());
        user.setBanReason(dto.getBanReason());
        user.setBanStartTime(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        mailService.sendNotify(user.getEmail(), "账号禁用通知",
                "您的账号已被禁用 " + dto.getBanDays() + " 天，原因：" + dto.getBanReason()
                        + "\n禁用期间可正常登录，但无法借阅与评论，到期后自动恢复正常。");
        log.info("管理员禁用用户：userId={}, days={}", userId, dto.getBanDays());
    }

    /**
     * 启用用户（恢复正常状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        User user = requireUser(userId);
        user.setStatus(1);
        user.setBanDays(0);
        user.setBanReason(null);
        user.setBanStartTime(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        mailService.sendNotify(user.getEmail(), "账号恢复通知", "您的账号已恢复正常状态，现在可以正常使用全部功能。");
    }

    /**
     * 注销用户（无法登录）
     * <p>属于「需要申请」的高风险操作：最高管理员可直接执行，普通管理员必须提交操作申请。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelUser(Long userId) {
        adminGuard.requireSuperAdminOrApply("注销用户", "cancel_user");
        User user = requireUser(userId);
        if (adminGuard.isSuperAdmin(user)) {
            throw BusinessException.of("不能注销最高管理员账号");
        }
        if (user.getRole() != null && user.getRole() == 1) {
            throw BusinessException.of("不能注销管理员账号");
        }
        user.setStatus(0);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        mailService.sendNotify(user.getEmail(), "账号注销通知", "您的账号已被注销，将无法登录云图书管理系统。");
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        return user;
    }
}
