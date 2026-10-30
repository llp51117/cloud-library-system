package com.group1.cloudlibrary.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结果封装
 */
@Data
public class PageResult<T> implements Serializable {

    /** 总条数 */
    private Long total;
    /** 当前页数据 */
    private List<T> list;
    /** 当前页码 */
    private Long page;
    /** 每页条数 */
    private Long size;
    /** 总页数 */
    private Long pages;

    public PageResult() {
        this.total = 0L;
        this.list = Collections.emptyList();
        this.page = 1L;
        this.size = 10L;
        this.pages = 0L;
    }

    public PageResult(Long total, List<T> list, Long page, Long size) {
        this.total = total;
        this.list = list;
        this.page = page;
        this.size = size;
        this.pages = (size == null || size == 0) ? 0L : (total + size - 1) / size;
    }

    /**
     * MyBatis-Plus 分页对象转换
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize());
    }

    /**
     * MyBatis-Plus 分页对象转换并映射元素类型
     */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> mapper) {
        List<T> records = page.getRecords().stream().map(mapper).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), records, page.getCurrent(), page.getSize());
    }
}
