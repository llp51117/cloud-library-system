package com.group1.cloudlibrary.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果
 * 格式：{"code": 200, "message": "success", "data": {...}}
 */
@Data
public class Result<T> implements Serializable {

    /** 成功 */
    public static final int SUCCESS = 200;
    /** 参数错误 */
    public static final int BAD_REQUEST = 400;
    /** 未登录 / 令牌无效 */
    public static final int UNAUTHORIZED = 401;
    /** 无权限 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 业务失败 */
    public static final int FAIL = 500;

    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(SUCCESS, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS, message, data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(FAIL, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(UNAUTHORIZED, message, null);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(FORBIDDEN, message, null);
    }
}
