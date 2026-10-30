package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交操作申请（普通管理员 → 最高管理员）
 */
@Data
public class ApplicationDTO {

    /** 申请类型：delete_user / batch_delete_book / cancel_user / other */
    @NotBlank(message = "请选择申请类型")
    private String type;

    /** 操作目标ID（批量操作时多个ID用英文逗号分隔） */
    private String targetId;

    /** 申请理由 */
    @NotBlank(message = "请填写申请理由")
    private String content;
}
