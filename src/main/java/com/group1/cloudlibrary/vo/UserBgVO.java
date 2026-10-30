package com.group1.cloudlibrary.vo;

import lombok.Data;

import java.util.List;

/**
 * 「光影幻境」主题的背景图上传结果（POST /api/user/bg）
 * <p>
 * 上传后服务端顺手用「图片取色」逻辑算出一套 主色 / 深色 / 浅色，
 * 前端把它展示出来给用户确认（可以重新分析换一套），确认后再按这套配色应用主题。
 * </p>
 */
@Data
public class UserBgVO {

    /** 背景图 URL（存 user.bg_url） */
    private String url;

    /** 背景图取出来的主色 */
    private String primaryColor;

    /** 背景图取出来的深色 */
    private String darkColor;

    /** 背景图取出来的浅色 */
    private String lightColor;

    /** 图片里的全部主要颜色（5~8 个），给用户点选当主色用 */
    private List<String> colors;
}
