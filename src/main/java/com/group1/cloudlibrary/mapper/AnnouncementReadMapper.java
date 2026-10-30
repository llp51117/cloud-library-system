package com.group1.cloudlibrary.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 公告已读记录 Mapper
 * <p>公告面向全体用户，不能在其自身记录已读状态，因此未读数 = 有效公告总数 - 本人已读数</p>
 */
@Mapper
public interface AnnouncementReadMapper {

    /** 有效公告总数 */
    @Select("SELECT COUNT(*) FROM announcement WHERE status = 1")
    long countActiveAnnouncements();

    /** 当前用户已读过的（仍有效的）公告数 */
    @Select("SELECT COUNT(*) FROM announcement_read ar " +
            "JOIN announcement a ON a.id = ar.announcement_id AND a.status = 1 " +
            "WHERE ar.user_id = #{userId}")
    long countReadByUser(@Param("userId") Long userId);

    /**
     * 把当前全部有效公告标记为该用户已读。
     * 用 NOT EXISTS 做去重，重复调用不会插入重复记录。
     */
    @Insert("INSERT INTO announcement_read (user_id, announcement_id) " +
            "SELECT #{userId}, a.id FROM announcement a " +
            "WHERE a.status = 1 " +
            "AND NOT EXISTS (SELECT 1 FROM announcement_read r " +
            "WHERE r.user_id = #{userId} AND r.announcement_id = a.id)")
    int markAllRead(@Param("userId") Long userId);

    /** 单条公告标记已读：唯一索引保证重复点击不会产生重复记录 */
    @Insert("INSERT IGNORE INTO announcement_read (user_id, announcement_id) VALUES (#{userId}, #{announcementId})")
    int markRead(@Param("userId") Long userId, @Param("announcementId") Long announcementId);

    /** 删除某用户的公告已读记录（删除用户时清理外键） */
    @Delete("DELETE FROM announcement_read WHERE user_id = #{userId}")
    int deleteByUser(@Param("userId") Long userId);
}
