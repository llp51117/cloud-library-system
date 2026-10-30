package com.group1.cloudlibrary.common;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = Result.FAIL;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException of(String message) {
        return new BusinessException(message);
    }
}
