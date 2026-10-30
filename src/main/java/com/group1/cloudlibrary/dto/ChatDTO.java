package com.group1.cloudlibrary.dto;

import lombok.Data;

/**
 * 智能问答请求
 */
@Data
public class ChatDTO {

    /**
     * 用户问题。
     * <p>允许为空：空消息、纯标点、「哈哈」「？」这类没有实际问题的输入，由服务端按闲聊场景自然回应。</p>
     */
    private String question;
}
