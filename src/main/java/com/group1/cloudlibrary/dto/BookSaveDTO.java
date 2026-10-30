package com.group1.cloudlibrary.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 图书新增 / 编辑请求（管理端）
 */
@Data
public class BookSaveDTO {

    private Long id;

    @NotBlank(message = "请输入书名")
    private String title;

    @NotBlank(message = "请输入作者")
    private String author;

    private String publisher;

    /** 平台（起点/晋江/番茄等） */
    private String platform;

    private BigDecimal price;

    private LocalDate publishDate;

    private String coverUrl;

    /** 封面在首页卡片里的裁剪参数（JSON 字符串，详见 Book#coverCardCrop） */
    private String coverCardCrop;

    /** 封面在详情页里的裁剪参数（JSON 字符串，详见 Book#coverDetailCrop） */
    private String coverDetailCrop;
    private String description;

    /** 库存默认值：新增图书 / Excel 未填库存列时用它 */
    public static final int DEFAULT_STOCK = 10;

    /** 是否完结：0连载中/1已完结 */
    private Integer isFinished = 0;

    /**
     * 库存数量（可借余量）。
     *
     * <p>新增时默认 10；批量导入没填该列时同样默认 10（见 ExcelService）。</p>
     */
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock = DEFAULT_STOCK;
}
