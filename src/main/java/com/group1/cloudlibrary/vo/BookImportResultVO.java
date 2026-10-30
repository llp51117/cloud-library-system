package com.group1.cloudlibrary.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入图书的结果
 * <p>按「书名 + 作者」查重：数据库里已存在的、以及同一批数据里重复的，都会跳过并列出书名。</p>
 */
@Data
public class BookImportResultVO {

    /** 实际导入成功的数量 */
    private int imported;

    /** 书名或作者为空，视为无效数据直接跳过 */
    private int invalid;

    /** 数据库中已存在且没有空字段可补，原样跳过不导入 */
    private List<String> duplicated = new ArrayList<>();

    /** 数据库中已存在并补齐了空字段的记录：《书名》- 作者（补充：出版社、价格） */
    private List<String> updated = new ArrayList<>();

    /** 同一批数据内部重复（只导第一条），已跳过 */
    private List<String> repeatedInBatch = new ArrayList<>();

    /** 补齐空字段的记录条数 */
    public int getUpdatedCount() {
        return updated.size();
    }

    /** 跳过的重复数据总条数 */
    public int getDuplicatedCount() {
        return duplicated.size() + repeatedInBatch.size();
    }
}
