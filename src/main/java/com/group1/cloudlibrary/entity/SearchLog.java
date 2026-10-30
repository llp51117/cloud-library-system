package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搜索记录表 search_log
 * 用于首页「热门搜索」关键词统计：个人高频词 + 全平台高频词
 */
@Data
@TableName("search_log")
public class SearchLog implements Serializable {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 搜索用户ID（未登录为NULL） */
    private Long userId;

    /** 搜索关键词 */
    private String keyword;

    /** 搜索时间 */
    private LocalDateTime createdAt;
}
