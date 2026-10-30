package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.SearchLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 搜索记录 Mapper
 */
@Mapper
public interface SearchLogMapper extends BaseMapper<SearchLog> {

    /**
     * 个人高频搜索词：按当前用户搜索次数降序取前 limit 个关键词。
     * 次数相同时，最近搜索过的、更早入库的排前面。
     */
    @Select("SELECT keyword FROM search_log WHERE user_id = #{userId} " +
            "GROUP BY keyword ORDER BY COUNT(*) DESC, MAX(created_at) DESC, MIN(id) ASC LIMIT #{limit}")
    List<String> selectUserTopKeywords(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 全平台高频搜索词（含未登录用户的搜索）。
     */
    @Select("SELECT keyword FROM search_log " +
            "GROUP BY keyword ORDER BY COUNT(*) DESC, MAX(created_at) DESC, MIN(id) ASC LIMIT #{limit}")
    List<String> selectGlobalTopKeywords(@Param("limit") int limit);
}
