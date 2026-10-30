package com.group1.cloudlibrary.vo;

import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.util.EmailMaskUtil;
import com.group1.cloudlibrary.util.NicknameUtil;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户信息视图对象（不包含密码）
 */
@Data
public class UserVO {

    private Long id;
    private String email;
    /** 打码后的邮箱，如 1*********@qq.com，页面默认展示这个 */
    private String emailMasked;
    private String nickname;
    private String avatar;
    private Integer role;
    /** 是否最高管理员：0否/1是 */
    private Integer isSuperAdmin;
    /** 是否待激活管理员：0否/1是 */
    private Integer pendingAdmin;
    /** 待激活截止时间（待激活管理员专用） */
    private LocalDateTime pendingAdminDeadline;
    /** 管理员封号（如「图书管理专员」，由最高管理员设置） */
    private String adminTitle;
    /** 用户主题（皮肤）标识，如 aurora-white */
    private String theme;

    /** 「光影幻境」主题的背景图 URL（只有 theme = magic-bg 时有意义，为空 = 用默认星星背景） */
    private String bgUrl;

    /** 「光影幻境」的配色：主色,深色,浅色（逗号分隔），由背景图自动取色得到 */
    private String bgPalette;

    private Integer status;
    private String banReason;
    private Integer banDays;
    private LocalDateTime banStartTime;
    private LocalDateTime createdAt;

    /** 默认主题：极光圣殿白（新用户注册后就是这套浅色主题） */
    public static final String DEFAULT_THEME = "aurora-white";

    /** 已下线的内置主题：库里还有老数据时一律回退到默认主题 */
    private static final Set<String> REMOVED_THEMES = Set.of("supernova-yellow");

    /** 自定义主题标识前缀：custom-{id}（图片取色，不参加深浅模式） */
    public static final String CUSTOM_PREFIX = "custom-";

    /** 深浅模式：{主题色}-{dark|light}，例如 quantum-blue-light */
    public static final String MODE_DARK = "dark";
    public static final String MODE_LIGHT = "light";
    private static final Set<String> MODE_SUFFIXES = Set.of("-dark", "-light");

    /**
     * 内置主题色标识（12 个，与前端 utils/themes.js 一一对应）
     * 「超新星爆闪黄」已下线（和「熔核爆裂橙」太像），老数据回退到默认主题。
     * magic-bg = 第 12 套特殊内置主题「光影幻境」（背景图 + 向上滚动 + 鼠标光圈），
     * 它的「颜色」不固定，来自用户上传的背景图取色，存在 user.bg_palette。
     * 存库的标识 = 主题色 + 模式后缀，例如 neon-pink-dark / neon-pink-light。
     */
    public static final Set<String> BUILT_IN_THEMES = Set.of(
            "quantum-blue", "void-purple", "deep-cyan", "matrix-green", "core-orange", "gold-rhythm",
            "flame-red", "neon-pink", "ghost-gray", "aurora-white", "nebula-magenta", "magic-bg"
    );

    /** 永远只有浅色的内置主题：极光圣殿白（永远白色）、光影幻境 magic-bg（颜色取自背景图） */
    private static final Set<String> LIGHT_ONLY_THEMES = Set.of("aurora-white", "magic-bg");

    /** 老数据（标识没带模式后缀）默认用哪套模式：霓虹幻梦粉原来是浅色，其余深色 */
    private static final Set<String> DEFAULT_LIGHT_THEMES = Set.of("neon-pink");

    /** 拆出主题色：neon-pink-light → neon-pink（自定义主题原样返回） */
    public static String baseThemeOf(String theme) {
        String value = theme == null ? "" : theme.trim();
        for (String suffix : MODE_SUFFIXES) {
            if (value.endsWith(suffix)) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return value;
    }

    /** 拆出模式：neon-pink-light → light；没带后缀的按该主题的默认模式补（老数据） */
    public static String modeOf(String theme) {
        String value = theme == null ? "" : theme.trim();
        for (String suffix : MODE_SUFFIXES) {
            if (value.endsWith(suffix)) {
                return suffix.substring(1);
            }
        }
        return DEFAULT_LIGHT_THEMES.contains(value) ? MODE_LIGHT : MODE_DARK;
    }

    /** 该主题色默认用哪套模式（给没带后缀的老数据补后缀用） */
    public static String defaultModeOf(String base) {
        return DEFAULT_LIGHT_THEMES.contains(base) ? MODE_LIGHT : MODE_DARK;
    }

    /**
     * 规范化主题标识：
     * <p>空 / 已下线 → 默认主题；自定义主题（custom-{id}）原样保留；</p>
     * <p>内置主题补齐模式后缀（老数据 quantum-blue → quantum-blue-dark、
     * neon-pink → neon-pink-light），极光圣殿白永远不带后缀。</p>
     */
    public static String normalizeTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            return DEFAULT_THEME;
        }
        String value = theme.trim();
        if (REMOVED_THEMES.contains(value)) {
            return DEFAULT_THEME;
        }
        if (value.startsWith(CUSTOM_PREFIX)) {
            return value;
        }
        String base = baseThemeOf(value);
        if (!BUILT_IN_THEMES.contains(base)) {
            return DEFAULT_THEME;
        }
        // 极光圣殿白：永远白色，不带模式后缀
        if (LIGHT_ONLY_THEMES.contains(base)) {
            return base;
        }
        // 老数据没有后缀 → 补上默认模式；已经有后缀 → 原样保留
        if (base.equals(value)) {
            return base + "-" + defaultModeOf(base);
        }
        return value;
    }

    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setEmailMasked(EmailMaskUtil.mask(user.getEmail()));
        // 昵称没设置（历史数据）时，回退成邮箱 @ 前缀，保证各处展示都有名字
        vo.setNickname(NicknameUtil.orDefault(user.getNickname(), user.getEmail()));
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setIsSuperAdmin(user.getIsSuperAdmin() == null ? 0 : user.getIsSuperAdmin());
        vo.setPendingAdmin(user.getPendingAdmin() == null ? 0 : user.getPendingAdmin());
        vo.setPendingAdminDeadline(user.getPendingAdminDeadline());
        vo.setAdminTitle(user.getAdminTitle());
        vo.setTheme(normalizeTheme(user.getTheme()));
        vo.setBgUrl(user.getBgUrl());
        vo.setBgPalette(user.getBgPalette());
        vo.setStatus(user.getStatus());
        vo.setBanReason(user.getBanReason());
        vo.setBanDays(user.getBanDays());
        vo.setBanStartTime(user.getBanStartTime());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
