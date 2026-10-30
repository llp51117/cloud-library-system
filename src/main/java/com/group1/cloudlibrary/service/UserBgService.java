package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.vo.ThemeAnalyzeVO;
import com.group1.cloudlibrary.vo.UserBgVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 「光影幻境」（主题标识 magic-bg）的背景图服务
 * <p>
 * 只服务第 12 套特殊内置主题：上传背景图 + 自动取色 + 存 URL 与配色。
 * 原来 11 套主题（含深浅模式）的逻辑一行不动，仍然全部走 UserService.updateTheme。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBgService {

    /** 取色失败时的兜底配色（蓝紫色系，浅色卡上可读性达标） */
    private static final String DEFAULT_PRIMARY = "#5B6BFF";
    private static final String DEFAULT_DARK = "#232347";
    private static final String DEFAULT_LIGHT = "#C9CFFF";

    private final UserMapper userMapper;
    private final FileService fileService;
    private final UserThemeService userThemeService;

    /**
     * 上传背景图：自动取色 → 存图 → 写入 user.bg_url / user.bg_palette。
     * 配色是「从图片主要颜色里随机挑 3 个」，前端可以「重新分析」换一套再确认。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserBgVO upload(Long userId, MultipartFile file) {
        String primary = DEFAULT_PRIMARY;
        String dark = DEFAULT_DARK;
        String light = DEFAULT_LIGHT;
        List<String> colors = List.of();
        // 先取色再存文件：取色要读输入流
        try {
            ThemeAnalyzeVO analyze = userThemeService.analyze(file);
            if (analyze != null) {
                if (analyze.getPrimaryColor() != null) {
                    primary = analyze.getPrimaryColor();
                }
                if (analyze.getDarkColor() != null) {
                    dark = analyze.getDarkColor();
                }
                if (analyze.getLightColor() != null) {
                    light = analyze.getLightColor();
                }
                colors = analyze.getColors() == null ? List.of() : analyze.getColors();
            }
        } catch (Exception e) {
            // 取色失败不影响上传：用兜底配色，用户还能在弹窗里手动换
            log.warn("背景图取色失败，使用默认配色: {}", e.getMessage());
        }

        String url = fileService.uploadImage(file, "bg");
        User user = requireUser(userId);
        user.setBgUrl(url);
        user.setBgPalette(join(primary, dark, light));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        UserBgVO vo = new UserBgVO();
        vo.setUrl(url);
        vo.setPrimaryColor(primary);
        vo.setDarkColor(dark);
        vo.setLightColor(light);
        vo.setColors(colors);
        return vo;
    }

    /** 保存用户确认后的配色（主色 / 深色 / 浅色），返回存库的 palette 字符串 */
    @Transactional(rollbackFor = Exception.class)
    public String updatePalette(Long userId, String primary, String dark, String light) {
        User user = requireUser(userId);
        String palette = join(primary, dark, light);
        user.setBgPalette(palette);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return palette;
    }

    /** 清除背景图：回落到「白底 + 黑色星星」默认背景（主题标识仍是 magic-bg） */
    @Transactional(rollbackFor = Exception.class)
    public void clear(Long userId) {
        User user = requireUser(userId);
        userMapper.update(
                null,
                Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, userId)
                        .set(User::getBgUrl, "")
                        .set(User::getBgPalette, "")
                        .set(User::getUpdatedAt, LocalDateTime.now())
        );
    }

    private static String join(String primary, String dark, String light) {
        return String.join(",", norm(primary, DEFAULT_PRIMARY), norm(dark, DEFAULT_DARK), norm(light, DEFAULT_LIGHT));
    }

    private static String norm(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim().toUpperCase();
    }

    /** 取当前用户，不存在就抛业务异常 */
    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        return user;
    }
}
