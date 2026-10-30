package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公告 Mapper
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    /**
     * 查询公告列表（含发布人邮箱）
     */
    @Select("SELECT a.*, u.email AS admin_email, u.nickname AS admin_nickname, " +
            "u.is_super_admin AS admin_super_admin, u.role AS admin_role, " +
            "CASE WHEN EXISTS (SELECT 1 FROM announcement_read r " +
            "WHERE r.announcement_id = a.id AND r.user_id = #{userId}) THEN 1 ELSE 0 END AS `read` " +
            "FROM announcement a LEFT JOIN `user` u ON a.admin_id = u.id " +
            "WHERE a.status = 1 ORDER BY a.created_at DESC")
    List<Announcement> selectActiveList(@Param("userId") Long userId);

    /**
     * 管理端：查询全部公告（包含已删除）
     */
    @Select("SELECT a.*, u.email AS admin_email, u.nickname AS admin_nickname, " +
            "u.is_super_admin AS admin_super_admin, u.role AS admin_role " +
            "FROM announcement a LEFT JOIN `user` u ON a.admin_id = u.id " +
            "ORDER BY a.created_at DESC")
    List<Announcement> selectAllWithAdmin();

    /**
     * 按 ID 查询详情
     */
    @Select("SELECT a.*, u.email AS admin_email, u.nickname AS admin_nickname, " +
            "u.is_super_admin AS admin_super_admin, u.role AS admin_role " +
            "FROM announcement a LEFT JOIN `user` u ON a.admin_id = u.id " +
            "WHERE a.id = #{id}")
    Announcement selectDetailById(@Param("id") Long id);
}
