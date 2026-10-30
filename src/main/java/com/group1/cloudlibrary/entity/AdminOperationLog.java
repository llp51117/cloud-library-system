package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员操作记录表 admin_operation_log
 *
 * <p>管理员在管理端做出的关键操作（删除评论 / 处理举报 / 编辑图书）都会写入一条记录，
 * 用于在管理员个人主页展示「管理操作记录」。</p>
 */
@Data
@TableName("admin_operation_log")
public class AdminOperationLog implements Serializable {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作管理员ID */
    private Long adminId;

    /** 操作类型：1删除评论/2处理举报/3编辑图书 */
    private Integer type;

    /** 操作对象ID（评论ID / 图书ID） */
    private Long targetId;

    /** 操作说明 */
    private String detail;

    /** 操作时间 */
    private LocalDateTime createdAt;
}
