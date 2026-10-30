package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收藏 Mapper
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 查询某用户收藏的图书（含图书信息）
     */
    @Select("SELECT f.*, bk.title AS book_title, bk.author AS book_author, bk.platform AS book_platform, " +
            "bk.cover_url AS book_cover, " +
            "CASE WHEN EXISTS (SELECT 1 FROM borrow b WHERE b.user_id = f.user_id " +
            "AND b.book_id = f.book_id AND b.status IN (0, 2)) THEN 1 ELSE 0 END AS borrowing " +
            "FROM favorite f LEFT JOIN book bk ON f.book_id = bk.id " +
            "WHERE f.user_id = #{userId} ORDER BY f.created_at DESC")
    List<Favorite> selectMyFavorites(@Param("userId") Long userId);
}
