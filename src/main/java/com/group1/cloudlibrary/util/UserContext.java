package com.group1.cloudlibrary.util;

import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.common.Result;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL = new ThreadLocal<>();

    public static void set(Long userId, Integer role, String email) {
        USER_ID.set(userId);
        ROLE.set(role);
        EMAIL.set(email);
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
        EMAIL.remove();
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Integer getRole() {
        return ROLE.get();
    }

    public static String getEmail() {
        return EMAIL.get();
    }

    public static boolean isAdmin() {
        Integer role = ROLE.get();
        return role != null && role == 1;
    }

    /**
     * 获取当前用户ID，未登录则抛异常
     */
    public static Long requireUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new BusinessException(Result.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }
}
