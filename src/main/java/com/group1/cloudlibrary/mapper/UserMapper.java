package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 活跃排行（管理端数据概览）：按 借阅*3 + 评论*2 + 收藏 计算活跃度，取前 N 名。
     *
     * <p>只排除「正式管理员」（role=1 且 pending_admin=0）——他们属于运营账号，不占用榜单名额；
     * 待激活管理员（role=1 且 pending_admin=1）本质上仍是普通用户，借阅/评论/收藏照常计入榜单。</p>
     */
    @Select("SELECT u.id AS userId, u.email AS userEmail, u.nickname AS nickname, " +
            " u.nickname AS userNickname, u.avatar AS avatar, " +
            " (SELECT COUNT(*) FROM borrow b WHERE b.user_id = u.id) AS borrowCount, " +
            " (SELECT COUNT(*) FROM comment c WHERE c.user_id = u.id) AS commentCount, " +
            " (SELECT COUNT(*) FROM favorite f WHERE f.user_id = u.id) AS favoriteCount, " +
            " (SELECT COUNT(*) FROM borrow b WHERE b.user_id = u.id) * 3 " +
            "   + (SELECT COUNT(*) FROM comment c WHERE c.user_id = u.id) * 2 " +
            "   + (SELECT COUNT(*) FROM favorite f WHERE f.user_id = u.id) AS value " +
            "FROM `user` u WHERE u.status <> 0 " +
            // 只剔除正式管理员；待激活管理员仍算普通用户，保留在榜单里
            "  AND NOT (u.role = 1 AND COALESCE(u.pending_admin, 0) = 0) " +
            "ORDER BY value DESC, u.id ASC LIMIT #{limit}")
    List<Map<String, Object>> selectActiveRank(@Param("limit") int limit);

    /**
     * 用户活跃排行：先按活跃度（借阅*3 + 评论*2 + 收藏）取前 N 名真实用户，
     * 再按注册时间从早到晚排序——这样折线图会呈现高低起伏的对比，而不是单调递减。
     * 排除已注销（status = 0）的用户和正式管理员（role = 1 且 pending_admin = 0），
     * 用户不足 N 个时按实际数量返回。列表按活跃度降序、折线图按注册时间升序，两种排序由前端各自处理。
     *
     * <p>注意：待激活管理员（role=1 且 pending_admin=1）还在用户端活动，借阅/评论/收藏都算他的，
     * 所以这里<b>不能</b>简单地用 role &lt;&gt; 1 把整个 role=1 都排掉。</p>
     */
    @Select("SELECT t.* FROM ( " +
            "  SELECT u.id AS userId, u.email AS userEmail, u.nickname AS nickname, u.avatar AS avatar, u.created_at AS createdAt, " +
            "    (SELECT COUNT(*) FROM borrow b WHERE b.user_id = u.id) AS borrowCount, " +
            "    (SELECT COUNT(*) FROM comment c WHERE c.user_id = u.id) AS commentCount, " +
            "    (SELECT COUNT(*) FROM favorite f WHERE f.user_id = u.id) AS favoriteCount, " +
            "    (SELECT COUNT(*) FROM borrow b WHERE b.user_id = u.id) * 3 " +
            "      + (SELECT COUNT(*) FROM comment c WHERE c.user_id = u.id) * 2 " +
            "      + (SELECT COUNT(*) FROM favorite f WHERE f.user_id = u.id) AS value " +
            "  FROM `user` u WHERE u.status <> 0 " +
            // 只排除正式管理员（role=1 且 pending_admin=0）：运营账号不该占用活跃榜名额。
            // 待激活管理员（role=1 且 pending_admin=1）仍是普通用户，必须保留在榜内。
            "  AND NOT (u.role = 1 AND COALESCE(u.pending_admin, 0) = 0) " +
            "  ORDER BY value DESC, u.id ASC LIMIT #{limit} " +
            ") t ORDER BY t.createdAt ASC, t.userId ASC")
    List<Map<String, Object>> selectActiveUsersRank(@Param("limit") int limit);
}
