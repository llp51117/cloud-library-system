package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 反馈表 feedback
 */
@Data
@TableName("feedback")
public class Feedback implements Serializable {

    /** 反馈ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 类型：0功能建议/1举报用户/2内容错误/3系统BUG/4其他 */
    private Integer type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 状态：0待回复/1已回复 */
    private Integer status;

    /** 管理员回复内容 */
    private String reply;

    /** 回复时间 */
    private LocalDateTime replyTime;

    /** 提交时间 */
    private LocalDateTime createdAt;

    // ================= 展示用非数据库字段 =================

    /** 提交人邮箱 */
    @TableField(exist = false)
    private String userEmail;

    /** 提交人昵称 */
    @TableField(exist = false)
    private String userNickname;

    /** 提交人头像 */
    @TableField(exist = false)
    private String userAvatar;
}
