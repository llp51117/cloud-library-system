package com.group1.cloudlibrary.vo;

import lombok.Data;

/**
 * 登录成功返回
 */
@Data
public class LoginVO {

    /** JWT 令牌 */
    private String token;

    /** 用户信息 */
    private UserVO userInfo;

    public LoginVO(String token, UserVO userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
}
