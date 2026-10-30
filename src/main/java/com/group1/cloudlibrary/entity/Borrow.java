package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借阅记录表 borrow
 */
@Data
@TableName("borrow")
public class Borrow implements Serializable {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 图书ID */
    private Long bookId;

    /** 借阅时间 */
    private LocalDateTime borrowDate;

    /** 应还时间（借阅日期 + cloud-library.borrow.days 天，当前配置 10 天） */
    private LocalDateTime dueDate;

    /** 实际归还时间 */
    private LocalDateTime returnDate;

    /** 状态：0借阅中/1已归还/2逾期 */
    private Integer status;

    /** 归还方式：0用户自己归还/1管理员强制归还（仅 status=1 时有意义） */
    private Integer returnType;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 续借次数（扩展字段，用于限制续借） */
    private Integer renewCount;

    // ================= 展示用非数据库字段 =================

    /** 借阅人邮箱 */
    @TableField(exist = false)
    private String userEmail;

    /** 借阅人昵称 */
    @TableField(exist = false)
    private String userNickname;

    /** 借阅人头像 */
    @TableField(exist = false)
    private String userAvatar;

    /** 书名 */
    @TableField(exist = false)
    private String bookTitle;

    /** 作者 */
    @TableField(exist = false)
    private String bookAuthor;

    /** 出版社 */
    @TableField(exist = false)
    private String bookPublisher;

    /** 平台 */
    @TableField(exist = false)
    private String bookPlatform;

    /** 封面 */
    @TableField(exist = false)
    private String bookCover;
}
