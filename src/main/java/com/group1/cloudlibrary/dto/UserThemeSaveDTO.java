package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 保存自定义主题请求（POST /api/user/themes）
 */
@Data
public class UserThemeSaveDTO {

    /** 用户命名的主题名，如「我的夏日」 */
    @NotBlank(message = "请给这套颜色起个名字")
    @Size(max = 30, message = "主题名不能超过 30 个字符")
    private String name;

    /** 主色 #RRGGBB */
    @NotBlank(message = "缺少主色")
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "主色格式不正确")
    private String primaryColor;

    /** 深色 #RRGGBB */
    @NotBlank(message = "缺少深色")
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "深色格式不正确")
    private String darkColor;

    /** 浅色 #RRGGBB */
    @NotBlank(message = "缺少浅色")
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "浅色格式不正确")
    private String lightColor;

    /**
     * 整套配色（可选）：从图片提取出的主要颜色随机分配到各处后的结果，
     * 顺序固定为 主色, 深色, 浅色, 按钮, 边框, Tab选中, 文字高亮, 卡片边框（最多 12 个）
     */
    @Size(max = 12, message = "配色最多 12 个颜色")
    private List<String> palette;
}
