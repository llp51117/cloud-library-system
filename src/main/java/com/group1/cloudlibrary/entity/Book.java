package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书表 book
 */
@Data
@TableName("book")
public class Book implements Serializable {

    /** 图书ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 平台（起点/晋江/番茄等） */
    private String platform;

    /** 价格 */
    private BigDecimal price;

    /** 出版时间 */
    private LocalDate publishDate;

    /** 封面图URL */
    private String coverUrl;

    /**
     * 封面在「首页卡片」里的裁剪参数（JSON 字符串）
     *
     * <p>格式：{"x":0,"y":0,"width":0,"height":0,"naturalWidth":0,"naturalHeight":0,"zoom":1}，
     * 前四个值是以<b>原图像素</b>为单位的裁剪区域左上角与宽高。为空表示不裁剪，按原图样式展示。</p>
     */
    private String coverCardCrop;

    /**
     * 封面在「图书详情页」里的裁剪参数（JSON 字符串，格式同 {@link #coverCardCrop}）
     */
    private String coverDetailCrop;

    /** 简介 */
    private String description;

    /** 是否完结：0连载中/1已完结 */
    private Integer isFinished;

    /**
     * 库存数量（可借余量）。
     *
     * <p>借阅成功 -1，归还成功 +1；为 0 时不能再借，只能预约。</p>
     */
    private Integer stock;

    /** 借阅次数 */
    private Integer borrowCount;

    /** 收藏人数 */
    private Integer favoriteCount;

    /** 搜索次数 */
    private Integer searchCount;

    /** 评论管理置顶：0否/1是 */
    private Integer commentPinned;

    /** 录入时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // ================= 以下为展示用非数据库字段 =================

    /** 当前是否可借阅（有库存） */
    @TableField(exist = false)
    private Boolean available;

    /** 当前用户是否已借阅该书且尚未归还（借阅中 / 逾期都算） */
    @TableField(exist = false)
    private Boolean borrowedByMe;

    /** 当前用户该书的未归还借阅记录ID（前端「去归还」直接用它调归还接口） */
    @TableField(exist = false)
    private Long myBorrowId;

    /** 当前用户该书的借阅是否已逾期（逾期时前端「去归还」标红） */
    @TableField(exist = false)
    private Boolean myBorrowOverdue;

    /** 当前用户是否已收藏 */
    @TableField(exist = false)
    private Boolean favorited;

    /** 收藏热度星级（1~5） */
    @TableField(exist = false)
    private Integer hotStars;

    /** 是否为「最近借阅优先」推荐的图书（猜你喜欢排序标记） */
    @TableField(exist = false)
    private Boolean recentBorrowed;

    /** 语义相似度得分（语义搜索时返回） */
    @TableField(exist = false)
    private Double score;
}
