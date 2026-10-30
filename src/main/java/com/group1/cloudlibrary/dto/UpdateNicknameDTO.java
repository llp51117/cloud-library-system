package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改昵称请求
 */
@Data
public class UpdateNicknameDTO {

    /** 昵称，允许为空（留空表示恢复为邮箱前缀），最长 50 个字符 */
    @Size(max = 50, message = "昵称不能超过 50 个字符")
    private String nickname;
}
