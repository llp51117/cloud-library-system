package com.group1.cloudlibrary.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理端评论管理：按图书聚合的评论统计
 */
@Data
public class BookCommentStats implements Serializable {

    /** 图书ID */
    private Long id;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 封面图URL */
    private String coverUrl;

    /** 是否置顶：0否/1是 */
    private Integer commentPinned;

    /** 评论总数（含被删除的） */
    private Integer commentCount;

    /** 该书所有评论的总点赞数 */
    private Integer likeCount;

    /** 该书所有评论的总踩数 */
    private Integer dislikeCount;

    /** 被举报评论数 */
    private Integer reportCount;
}
