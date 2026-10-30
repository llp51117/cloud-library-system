package com.group1.cloudlibrary.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 「我的书籍」四个子 Tab 的数量统计（不受分页大小影响）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyBookCountsVO {

    /** 当前借阅中数量（借阅中 0 / 已逾期 2） */
    private Long currentCount;

    /** 借阅历史数量（已归还 1） */
    private Long historyCount;

    /** 我的收藏数量 */
    private Long favoriteCount;

    /** 我的预约数量 */
    private Long reservationCount;
}
