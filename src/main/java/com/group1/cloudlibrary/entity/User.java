package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表 user
 */
@Data
@TableName("user")
public class User implements Serializable {

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 邮箱（登录账号） */
    private String email;

    /** 昵称（展示用，默认取邮箱 @ 前面的部分） */
    private String nickname;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 头像URL */
    private String avatar;

    /** 角色：0普通用户/1管理员 */
    private Integer role;

    /** 是否最高管理员：0否/1是（只有超管能设置/取消管理员、审批操作申请） */
    private Integer isSuperAdmin;

    /** 待激活管理员：0否/1是（已设为管理员但还有未归还图书，还完才能转正） */
    private Integer pendingAdmin;

    /** 待激活截止时间（10 天内未归还全部图书则转换失败，恢复为普通用户） */
    private LocalDateTime pendingAdminDeadline;

    /** 管理员封号（由最高管理员设置，如「图书管理专员」，普通用户为 NULL） */
    private String adminTitle;

    /** 用户主题（皮肤）标识，默认 aurora-white（极光圣殿白），与前端 utils/themes.js 一一对应 */
    private String theme;

    /**
     * 「光影幻境」（第 12 套特殊内置主题 magic-bg）的背景图 URL。
     * 只有主题 = magic-bg 时才有意义；为空表示用默认的「白底 + 黑色星星」背景。
     */
    private String bgUrl;

    /**
     * 「光影幻境」的配色：背景图取出来的 主色,深色,浅色（逗号分隔，如 #FF1744,#8E0022,#FF6B8A）。
     * 卡片 / 文字 / 按钮按它配色，换设备登录也能还原。
     */
    private String bgPalette;

    /** 状态：0注销/1正常/2禁用 */
    private Integer status;

    /** 禁用原因 */
    private String banReason;

    /** 禁用天数 */
    private Integer banDays;

    /** 禁用开始时间 */
    private LocalDateTime banStartTime;

    /** 注册时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
