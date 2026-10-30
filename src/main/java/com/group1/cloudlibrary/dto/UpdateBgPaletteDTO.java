package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改「光影幻境」配色（PUT /api/user/bg/palette）
 * 三个颜色都来自背景图取色结果，前端确认后回传保存。
 */
@Data
public class UpdateBgPaletteDTO {

    private static final String HEX = "^#[0-9A-Fa-f]{6}$";

    @Pattern(regexp = HEX, message = "主色格式不正确")
    private String primaryColor;

    @Pattern(regexp = HEX, message = "深色格式不正确")
    private String darkColor;

    @Pattern(regexp = HEX, message = "浅色格式不正确")
    private String lightColor;
}
