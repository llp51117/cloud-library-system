package com.group1.cloudlibrary.vo;

import com.group1.cloudlibrary.entity.UserTheme;
import com.group1.cloudlibrary.service.UserThemeService;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户自定义主题 VO
 * <p>key = custom-{id}，前端切换主题时把它当成普通主题标识用。</p>
 */
@Data
public class UserThemeVO {

    /** 主题ID */
    private Long id;

    /** 主题标识：custom-{id} */
    private String key;

    /** 用户命名的主题名 */
    private String name;

    /** 主色 */
    private String primaryColor;

    /** 深色 */
    private String darkColor;

    /** 浅色 */
    private String lightColor;

    /** 整套配色（主色/深色/浅色/按钮/边框/Tab选中/文字高亮/卡片边框 …） */
    private List<String> palette;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 自定义主题标识前缀 */
    public static final String KEY_PREFIX = "custom-";

    public static String keyOf(Long id) {
        return KEY_PREFIX + id;
    }

    public static UserThemeVO from(UserTheme entity) {
        if (entity == null) {
            return null;
        }
        UserThemeVO vo = new UserThemeVO();
        vo.setId(entity.getId());
        vo.setKey(keyOf(entity.getId()));
        vo.setName(entity.getName());
        vo.setPrimaryColor(entity.getPrimaryColor());
        vo.setDarkColor(entity.getDarkColor());
        vo.setLightColor(entity.getLightColor());
        vo.setPalette(UserThemeService.splitPalette(entity.getPalette()));
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
