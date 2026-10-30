package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作申请表 application
 *
 * <p>普通管理员不能直接执行「删除用户 / 注销用户 / 批量删除图书」这类高风险操作，
 * 需要在这里提交申请，由最高管理员（is_super_admin = 1）审批。</p>
 */
@Data
@TableName("application")
public class Application implements Serializable {

    /** 申请ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人ID（提交申请的普通管理员） */
    private Long applicantId;

    /** 申请类型：delete_user / cancel_user / batch_delete_book / other */
    private String type;

    /** 操作目标ID（批量操作时多个ID用英文逗号分隔） */
    private String targetId;

    /** 申请理由 */
    private String content;

    /** 状态：0待审批/1已同意/2已拒绝 */
    private Integer status;

    /** 最高管理员的回复 */
    private String reply;

    /** 申请时间 */
    private LocalDateTime createdAt;

    /** 审批时间 */
    private LocalDateTime replyTime;

    // ================= 展示用非数据库字段 =================

    /** 申请人邮箱 */
    @TableField(exist = false)
    private String applicantEmail;

    /** 申请人昵称 */
    @TableField(exist = false)
    private String applicantNickname;

    /** 申请人头像 */
    @TableField(exist = false)
    private String applicantAvatar;

    /** 目标的可读名称（图书名 / 用户邮箱），由服务层补全 */
    @TableField(exist = false)
    private String targetLabel;
}
