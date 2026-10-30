package com.group1.cloudlibrary.util;

import org.springframework.util.StringUtils;

/**
 * 昵称工具：昵称缺省时回退为邮箱 @ 前面的部分（1042795769@qq.com → 1042795769）
 */
public final class NicknameUtil {

    private NicknameUtil() {
    }

    /** 昵称有值就用昵称，否则用邮箱前缀；都没有时返回「读者」 */
    public static String orDefault(String nickname, String email) {
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        String prefix = fromEmail(email);
        return StringUtils.hasText(prefix) ? prefix : "读者";
    }

    /** 取邮箱 @ 前面的部分；不是邮箱格式时原样返回 */
    public static String fromEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        String value = email.trim();
        int at = value.indexOf('@');
        return at > 0 ? value.substring(0, at) : value;
    }
}
