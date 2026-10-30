package com.group1.cloudlibrary.controller.admin;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.service.StatisticsService;
import com.group1.cloudlibrary.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据概览接口（管理端）
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    /** 关键数据卡片：总藏书 / 总用户 / 当前借阅中 / 逾期图书 */
    @GetMapping("/overview")
    public Result<StatisticsVO> overview() {
        return Result.success(statisticsService.overview());
    }

    /** 热门图书排行（柱状图，前 N 名） */
    @GetMapping("/hot-books")
    public Result<List<Map<String, Object>>> hotBooks(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(statisticsService.hotBooks(limit));
    }

    /** 借阅趋势（折线图，近 N 天） */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(statisticsService.borrowTrend(days));
    }

    /** 用户活跃排行 */
    @GetMapping("/active-users")
    public Result<List<Map<String, Object>>> activeUsers(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(statisticsService.activeUsers(limit));
    }

    /** AI 分析报告 */
    @GetMapping("/ai-report")
    public Result<String> aiReport(@RequestParam(defaultValue = "7") int days) {
        return Result.success(statisticsService.aiReport(days));
    }
}
