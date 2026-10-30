package com.group1.cloudlibrary.dto;

import lombok.Data;

/**
 * 封面裁剪参数：分别保存「首页卡片（横向）」和「图书详情页（竖向）」两套
 *
 * <p>值为 JSON 字符串（见 Book 实体的 coverCardCrop / coverDetailCrop 说明），
 * 传空字符串表示清空该模式的裁剪，回到「不裁剪、按原图展示」。</p>
 */
@Data
public class CoverCropDTO {

    /** 首页卡片的裁剪参数 */
    private String coverCardCrop;

    /** 图书详情页的裁剪参数 */
    private String coverDetailCrop;
}
