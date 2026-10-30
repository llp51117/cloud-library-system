package com.group1.cloudlibrary.vo;

import lombok.Data;

/**
 * 管理端数据概览卡片
 */
@Data
public class StatisticsVO {

    /** 总藏书量 */
    private Long totalBooks;

    /** 总用户数 */
    private Long totalUsers;

    /** 当前借阅中数量 */
    private Long borrowingCount;

    /** 逾期图书数量 */
    private Long overdueCount;
}
