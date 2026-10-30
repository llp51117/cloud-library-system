package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏表 favorite
 */
@Data
@TableName("favorite")
public class Favorite implements Serializable {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 图书ID */
    private Long bookId;

    /** 收藏时间 */
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

    /**
     * 当前用户是否已借阅这本书（借阅中 0 / 逾期未还 2 都算）。
     * 用于收藏列表把「去借阅」按钮置灰显示「已借阅」。
     */
    @TableField(exist = false)
    private Boolean borrowing;
}
