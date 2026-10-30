package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.mapper.BookMapper;
import com.group1.cloudlibrary.mapper.BorrowMapper;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务（管理端数据概览）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final BorrowMapper borrowMapper;
    private final AiService aiService;

    /**
     * 4 个关键指标：总藏书量、总用户数、当前借阅中、逾期图书
     */
    public StatisticsVO overview() {
        StatisticsVO vo = new StatisticsVO();
        Long totalBooks = bookMapper.selectCount(null);
        // 「总用户数」= 普通用户 + 待激活管理员，只排除正式管理员（role=1 且 pending_admin=0）
        // 写法与 UserMapper 里两个活跃排行保持一致，避免待激活管理员被漏掉
        Long totalUsers = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .apply("NOT (role = 1 AND COALESCE(pending_admin, 0) = 0)"));
        long borrowing = borrowMapper.countByStatus(0);
        long overdue = borrowMapper.countByStatus(2);
        vo.setTotalBooks(totalBooks == null ? 0 : totalBooks);
        vo.setTotalUsers(totalUsers == null ? 0 : totalUsers);
        vo.setBorrowingCount(borrowing + overdue);
        vo.setOverdueCount(overdue);
        return vo;
    }

    /**
     * 热门图书排行（前 N 名，柱状图）
     */
    public List<Map<String, Object>> hotBooks(int limit) {
        return bookMapper.selectHotBooks(limit);
    }

    /**
     * 借阅趋势（近 N 天，折线图，自动补齐无数据日期）
     */
    public List<Map<String, Object>> borrowTrend(int days) {
        List<Map<String, Object>> rows = borrowMapper.selectBorrowTrend(days);
        Map<String, Long> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            dataMap.put(String.valueOf(row.get("day")), Long.parseLong(String.valueOf(row.get("count"))));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", day.toString());
            item.put("count", dataMap.getOrDefault(day.toString(), 0L));
            result.add(item);
        }
        return result;
    }

    /**
     * 生成 AI 分析报告
     */
    public String aiReport(int days) {
        StatisticsVO statistics = overview();
        List<Map<String, Object>> hotBooks = hotBooks(5);
        List<Map<String, Object>> trend = borrowTrend(days);
        return aiService.generateAnalysisReport(statistics, hotBooks, trend);
    }

    /**
     * 用户活跃排行（前 N 名）
     */
    public List<Map<String, Object>> activeUsers(int limit) {
        return userMapper.selectActiveRank(limit);
    }
}
