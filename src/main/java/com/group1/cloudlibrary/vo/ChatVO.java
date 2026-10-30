package com.group1.cloudlibrary.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能问答返回
 */
@Data
public class ChatVO {

    /** AI 回答 */
    private String answer;

    /** 引用来源（如：根据《三体》图书信息…） */
    private List<String> references = new ArrayList<>();

    /** 是否来自真实大模型（false 表示降级为本地知识库回答） */
    private Boolean aiGenerated = true;
}
