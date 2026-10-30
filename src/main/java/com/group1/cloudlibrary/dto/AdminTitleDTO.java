package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设置管理员封号请求（管理端，只有最高管理员能操作）
 */
@Data
public class AdminTitleDTO {

    /** 封号，如「图书管理专员」；传空表示清除封号 */
    @Size(max = 50, message = "封号不能超过 50 个字符")
    private String title;
}
