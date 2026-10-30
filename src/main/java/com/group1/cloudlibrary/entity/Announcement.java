package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告表 announcement
 */
@Data
@TableName("announcement")
public class Announcement implements Serializable {

    /** 公告ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 发布管理员ID */
    private Long adminId;

    /** 状态：0已删除/1正常 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // ================= 展示用非数据库字段 =================

    /** 发布管理员邮箱（不再直接展示，仅作兜底数据保留） */
    @TableField(exist = false)
    private String adminEmail;

    /** 发布管理员昵称：普通管理员展示昵称，不展示邮箱 */
    @TableField(exist = false)
    private String adminNickname;

    /** 发布人是否最高管理员：1-是（前端展示「最高管理员」） */
    @TableField(exist = false)
    private Integer adminSuperAdmin;

    /** 发布人角色：0普通用户 / 1管理员 */
    @TableField(exist = false)
    private Integer adminRole;

    /** 当前用户是否已读（未读时前端显示小红点） */
    @TableField(exist = false)
    private Boolean read;
}
