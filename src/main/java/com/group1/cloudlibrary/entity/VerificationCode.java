package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 验证码表 verification_code
 */
@Data
@TableName("verification_code")
public class VerificationCode implements Serializable {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 邮箱 */
    private String email;

    /** 验证码 */
    private String code;

    /** 类型：0注册/1找回密码 */
    private Integer type;

    /** 过期时间（有效期 5 分钟） */
    private LocalDateTime expiredAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
