package com.group1.cloudlibrary.dto;

import lombok.Data;

/**
 * 审批操作申请（最高管理员）
 */
@Data
public class ApplicationReplyDTO {

    /** 审批回复 / 拒绝理由 */
    private String reply;
}
