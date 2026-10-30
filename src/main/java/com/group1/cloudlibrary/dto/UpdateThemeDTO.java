package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改主题（皮肤）请求
 */
@Data
public class UpdateThemeDTO {

    /** 主题标识，如 quantum-blue（12 个主题之一） */
    @NotBlank(message = "请选择主题")
    private String theme;
}
