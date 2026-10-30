package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论表 comment
 */
@Data
@TableName("comment")
public class Comment implements Serializable {

    /** 评论ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 图书ID */
    private Long bookId;

    /** 评论内容 */
    private String content;

    /** 点赞数 */
    private Integer likeCount;

    /** 踩数 */
    private Integer dislikeCount;

    /** 父评论ID（楼中楼回复，只支持一层；顶级评论为 null） */
    private Long parentId;

    /** 状态：0正常/1系统删除/2管理员删除/3管理员恢复 */
    private Integer status;

    /** 是否被 @管理员 标记：0否/1是 */
    private Integer isReported;

    /** 评论时间 */
    private LocalDateTime createdAt;

    // ================= 展示用非数据库字段 =================

    /** 评论人邮箱 */
    @TableField(exist = false)
    private String userEmail;

    /** 评论人昵称（没有昵称时后台返回空，前端回退显示邮箱） */
    @TableField(exist = false)
    private String userNickname;

    /** 评论人头像 */
    @TableField(exist = false)
    private String userAvatar;

    /** 评论人角色：0普通用户/1管理员 */
    @TableField(exist = false)
    private Integer userRole;

    /** 评论人是否最高管理员（个人主页入口 / 头像标识用） */
    @TableField(exist = false)
    private Integer userSuperAdmin;

    /** 评论人是否待激活管理员（头像标识用） */
    @TableField(exist = false)
    private Integer userPendingAdmin;

    /** 评论人管理员封号（正式管理员才有值） */
    @TableField(exist = false)
    private String userAdminTitle;

    /** 书名 */
    @TableField(exist = false)
    private String bookTitle;

    /** 被举报次数（comment_report 表所有用户的汇总行数） */
    @TableField(exist = false)
    private Integer reportCount;

    /** 当前登录用户是否已点赞该评论 */
    @TableField(exist = false)
    private Boolean liked;

    /** 当前登录用户是否已踩该评论 */
    @TableField(exist = false)
    private Boolean disliked;

    /** 楼中楼子回复（只支持一层） */
    @TableField(exist = false)
    private List<Comment> replies;

    /** 删除原因描述（系统已删列表使用） */
    @TableField(exist = false)
    private String deleteReason;

    /** 举报原因（@管理员列表使用） */
    @TableField(exist = false)
    private String reportReason;
}
