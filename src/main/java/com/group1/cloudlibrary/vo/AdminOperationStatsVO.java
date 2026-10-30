package com.group1.cloudlibrary.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员操作统计（管理员个人主页「管理操作记录」区域，仅最高管理员可见）
 */
@Data
public class AdminOperationStatsVO {

    /** 管理员ID */
    private Long adminId;

    /** 处理举报条数 */
    private long reportCount;

    /** 删除评论条数 */
    private long commentDeleteCount;

    /** 编辑图书本数 */
    private long bookEditCount;

    /** 操作总数（三条统计之和） */
    private long total;

    /** 最近的操作明细（最多 10 条，按时间倒序） */
    private List<Item> recent = new ArrayList<>();

    /** 单条操作明细 */
    @Data
    public static class Item {

        /** 操作类型：1删除评论/2处理举报/3编辑图书 */
        private Integer type;

        /** 操作类型文案 */
        private String typeText;

        /** 操作对象ID */
        private Long targetId;

        /** 操作说明 */
        private String detail;

        /** 操作时间 */
        private LocalDateTime createdAt;
    }
}
