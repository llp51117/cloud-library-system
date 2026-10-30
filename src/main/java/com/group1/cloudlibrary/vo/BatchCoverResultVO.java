package com.group1.cloudlibrary.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量上传封面的结果
 * <p>按「图片文件名 = 书名」匹配：匹配上的直接写入 coverUrl，匹配不到或上传失败的原样返回给前端展示。</p>
 */
@Data
public class BatchCoverResultVO {

    /** 本次上传的图片总数 */
    private int total;

    /** 成功匹配并更新封面的书名 */
    private List<String> matched = new ArrayList<>();

    /** 没匹配到图书的图片文件名（不含扩展名） */
    private List<String> unmatched = new ArrayList<>();

    /** 上传失败的图片（文件名 + 失败原因），例如超过 10MB、格式不支持 */
    private List<String> failed = new ArrayList<>();

    /** 便捷字段：成功条数 */
    public int getMatchedCount() {
        return matched.size();
    }
}
