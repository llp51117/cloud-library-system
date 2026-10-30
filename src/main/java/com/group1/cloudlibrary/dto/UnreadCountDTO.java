package com.group1.cloudlibrary.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 未读数量：系统公告与个人通知分开统计，total 为两者之和
 */
@Data
public class UnreadCountDTO implements Serializable {

    /** 我的通知未读数量 */
    private long notification;

    /** 系统公告未读数量 */
    private long announcement;

    /** 合计未读数量（右上角红点显示这个值） */
    private long total;
}
