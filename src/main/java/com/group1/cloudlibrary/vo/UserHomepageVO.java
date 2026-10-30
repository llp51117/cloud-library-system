package com.group1.cloudlibrary.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户个人主页（评论区点头像 / 昵称进入）。
 * <p>按需求只暴露：头像、昵称、注册时间、账号状态、身份标签、正在阅读、已收藏；
 * 不含邮箱、用户ID 之外的隐私信息，也不含借阅历史明细。</p>
 */
@Data
public class UserHomepageVO {

    /** 用户ID（前端跳转用） */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 角色：0普通用户/1管理员 */
    private Integer role;

    /** 是否最高管理员：0否/1是 */
    private Integer isSuperAdmin;

    /** 是否待激活管理员：0否/1是 */
    private Integer pendingAdmin;

    /** 管理员封号（正式管理员才有值，如「图书管理专员」） */
    private String adminTitle;

    /** 账号状态：0注销/1正常/2禁用 */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime createdAt;

    /** 身份标签：最高管理员 / 管理员 / 待激活管理员 / 普通用户 */
    private String identity;

    /** 正在阅读（借阅中 + 逾期未还） */
    private List<HomepageBookVO> reading = new ArrayList<>();

    /** 已收藏 */
    private List<HomepageBookVO> favorites = new ArrayList<>();

    /** 主页展示的图书卡片（正在阅读 / 已收藏共用） */
    @Data
    public static class HomepageBookVO {
        /** 图书ID */
        private Long id;
        /** 书名 */
        private String title;
        /** 作者 */
        private String author;
        /** 封面 */
        private String coverUrl;
        /** 首页卡片裁剪参数（JSON 字符串） */
        private String coverCardCrop;
        /** 借阅状态：0借阅中/2逾期（仅「正在阅读」有值） */
        private Integer borrowStatus;
        /** 应还时间（仅「正在阅读」有值） */
        private LocalDateTime dueDate;
        /** 收藏时间（仅「已收藏」有值） */
        private LocalDateTime favoriteTime;
    }
}
