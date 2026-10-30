package com.group1.cloudlibrary.util;

/**
 * 邮箱脱敏工具类
 * 评论、点评等公开场景展示邮箱时隐藏主体部分，例如：
 * helloworld@example.com → hell*******@example.com
 */
public final class EmailMaskUtil {

    /** 邮箱名保留的前缀长度 */
    private static final int VISIBLE_PREFIX = 4;

    /** 中间打码的星号个数 */
    private static final int MASK_LENGTH = 7;

    private EmailMaskUtil() {
    }

    /**
     * 邮箱脱敏：保留邮箱名前 4 位 + 7 个星号 + 完整域名（含 @）。
     * 空邮箱、非法邮箱（缺少 @ 或 @ 在最前面）统一返回 ***。
     */
    public static String mask(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }

        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0) {
            return "***";
        }

        String name = value.substring(0, at);
        String domain = value.substring(at);
        String head = name.length() > VISIBLE_PREFIX ? name.substring(0, VISIBLE_PREFIX) : name;
        return head + "*".repeat(MASK_LENGTH) + domain;
    }
}
