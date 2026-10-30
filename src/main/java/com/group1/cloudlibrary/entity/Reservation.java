package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约记录表 reservation
 */
@Data
@TableName("reservation")
public class Reservation implements Serializable {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 图书ID */
    private Long bookId;

    /** 预约时间 */
    private LocalDateTime reservationTime;

    /** 变为可借阅的时间 */
    private LocalDateTime availableTime;

    /** 状态：0等待借阅/1可借阅/2已借阅/3已取消/4超时取消 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    // ================= 展示用非数据库字段 =================

    /** 书名 */
    @TableField(exist = false)
    private String bookTitle;

    /** 作者 */
    @TableField(exist = false)
    private String bookAuthor;

    /** 平台 */
    @TableField(exist = false)
    private String bookPlatform;

    /** 封面 */
    @TableField(exist = false)
    private String bookCover;

    /** 排队位次 */
    @TableField(exist = false)
    private Integer queuePosition;

    /** 剩余保留时间（小时），可借阅状态下有效 */
    @TableField(exist = false)
    private Long remainHours;
}
