package com.group1.cloudlibrary.vo;

import lombok.Data;

import java.util.List;

/**
 * 图片配色分析结果（POST /api/user/themes/analyze）
 * <p>
 * 先把图片里的主要颜色（5~8 个）都提取出来，再随机挑 3 个当「主色 / 深色 / 浅色」。
 * 只返回配色建议，不落库；用户命名后调用保存接口才写入 user_theme。
 * </p>
 */
@Data
public class ThemeAnalyzeVO {

    /** 图片里提取出的主要颜色（5~8 个，按出现频率从高到低），如粉 / 蓝 / 棕 / 紫 / 白 / 黄 */
    private List<String> colors;

    /** 本次随机分配的主色 */
    private String primaryColor;

    /** 本次随机分配的深色 */
    private String darkColor;

    /** 本次随机分配的浅色 */
    private String lightColor;

    /** 根据主色给的默认名字，用户可改 */
    private String suggestName;
}
