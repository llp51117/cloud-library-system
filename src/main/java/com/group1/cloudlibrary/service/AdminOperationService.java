package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.group1.cloudlibrary.entity.AdminOperationLog;
import com.group1.cloudlibrary.mapper.AdminOperationLogMapper;
import com.group1.cloudlibrary.util.UserContext;
import com.group1.cloudlibrary.vo.AdminOperationStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员操作记录服务：记录管理员的关键操作，并统计「处理举报 / 删除评论 / 编辑图书」数量。
 *
 * <p>记录人是当前登录用户（{@link UserContext}）；没有登录上下文（例如定时任务、系统自动处理）时直接忽略。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOperationService {

    /** 操作类型：删除评论 */
    public static final int TYPE_COMMENT_DELETE = 1;
    /** 操作类型：处理举报（对被 @管理员 标记的评论做出处理） */
    public static final int TYPE_REPORT = 2;
    /** 操作类型：编辑图书 */
    public static final int TYPE_BOOK_EDIT = 3;

    /** 详情文案里保留的内容长度 */
    private static final int DETAIL_TEXT_LIMIT = 20;

    /** 最近操作明细条数 */
    private static final int RECENT_LIMIT = 10;

    private final AdminOperationLogMapper adminOperationLogMapper;

    /**
     * 记录当前登录管理员的一条操作
     *
     * @param type     操作类型，见 TYPE_XXX
     * @param targetId 操作对象ID（评论ID / 图书ID）
     * @param detail   操作说明
     */
    public void record(int type, Long targetId, String detail) {
        recordFor(UserContext.getUserId(), type, targetId, detail);
    }

    /**
     * 记录指定管理员的操作（adminId 为空时忽略）
     */
    public void recordFor(Long adminId, int type, Long targetId, String detail) {
        if (adminId == null) {
            return;
        }
        try {
            AdminOperationLog record = new AdminOperationLog();
            record.setAdminId(adminId);
            record.setType(type);
            record.setTargetId(targetId);
            record.setDetail(abbreviate(detail));
            record.setCreatedAt(LocalDateTime.now());
            adminOperationLogMapper.insert(record);
        } catch (Exception e) {
            // 操作记录属于附加功能：写入失败不能影响正常的业务操作
            log.warn("写入管理员操作记录失败：adminId={}, type={}, detail={}", adminId, type, detail, e);
        }
    }

    /**
     * 某管理员的操作统计 + 最近操作明细
     */
    public AdminOperationStatsVO stats(Long adminId) {
        AdminOperationStatsVO vo = new AdminOperationStatsVO();
        vo.setAdminId(adminId);
        if (adminId == null) {
            return vo;
        }
        vo.setReportCount(count(adminId, TYPE_REPORT));
        vo.setCommentDeleteCount(count(adminId, TYPE_COMMENT_DELETE));
        vo.setBookEditCount(count(adminId, TYPE_BOOK_EDIT));
        vo.setTotal(vo.getReportCount() + vo.getCommentDeleteCount() + vo.getBookEditCount());

        List<AdminOperationLog> recent = adminOperationLogMapper
                .selectPage(new Page<>(1, RECENT_LIMIT), Wrappers.<AdminOperationLog>lambdaQuery()
                        .eq(AdminOperationLog::getAdminId, adminId)
                        .orderByDesc(AdminOperationLog::getCreatedAt)
                        .orderByDesc(AdminOperationLog::getId))
                .getRecords();
        vo.setRecent(recent.stream().map(this::toItem).toList());
        return vo;
    }

    /** 操作类型文案 */
    public static String typeText(Integer type) {
        if (type == null) {
            return "其他操作";
        }
        return switch (type) {
            case TYPE_COMMENT_DELETE -> "删除评论";
            case TYPE_REPORT -> "处理举报";
            case TYPE_BOOK_EDIT -> "编辑图书";
            default -> "其他操作";
        };
    }

    /** 截断过长的说明文案（评论内容可能很长） */
    public static String abbreviate(String text) {
        if (text == null) {
            return null;
        }
        String value = text.replaceAll("\\s+", " ").trim();
        return value.length() <= DETAIL_TEXT_LIMIT ? value : value.substring(0, DETAIL_TEXT_LIMIT) + "…";
    }

    private long count(Long adminId, int type) {
        Long count = adminOperationLogMapper.countByType(adminId, type);
        return count == null ? 0 : count;
    }

    private AdminOperationStatsVO.Item toItem(AdminOperationLog record) {
        AdminOperationStatsVO.Item item = new AdminOperationStatsVO.Item();
        item.setType(record.getType());
        item.setTypeText(typeText(record.getType()));
        item.setTargetId(record.getTargetId());
        item.setDetail(record.getDetail());
        item.setCreatedAt(record.getCreatedAt());
        return item;
    }
}
