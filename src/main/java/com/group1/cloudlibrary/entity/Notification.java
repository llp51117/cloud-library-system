package com.group1.cloudlibrary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知表 notification
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    /** 通知ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（NULL 表示系统公告，所有用户可见） */
    private Long userId;

    /** 类型：0系统公告/1借阅成功/2预约轮到/3预约超时/4还书成功/5逾期提醒/6反馈回复/7催还提醒 */
    private Integer type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 是否已读：0未读/1已读 */
    private Integer isRead;

    /** 发布时间 */
    private LocalDateTime createdAt;
}
