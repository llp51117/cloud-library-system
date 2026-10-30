package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户自定义主题表 user_theme
 * <p>用户上传图片 → 系统分析配色 → 命名后存到这里，每个用户只看到自己的主题。</p>
 */
@Data
@TableName("user_theme")
public class UserTheme implements Serializable {

    /** 主题ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（归属：只属于创建它的用户） */
    private Long userId;

    /** 用户命名的主题名，如「我的夏日」 */
    private String name;

    /** 主色，如 #00D4FF */
    private String primaryColor;

    /** 深色（主色加深），如 #00908F */
    private String darkColor;

    /** 浅色（主色变浅），如 #7FE9FF */
    private String lightColor;

    /**
     * 整套配色：逗号分隔的 #RRGGBB，顺序固定为
     * 主色, 深色, 浅色, 按钮, 边框, Tab选中, 文字高亮, 卡片边框（可再多几个）
     */
    private String palette;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
