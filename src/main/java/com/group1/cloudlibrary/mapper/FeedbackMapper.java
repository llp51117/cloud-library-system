package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.group1.cloudlibrary.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 反馈 Mapper
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    String BASE_COLUMNS = " f.*, u.email AS user_email, u.nickname AS user_nickname, u.avatar AS user_avatar ";

    /**
     * 分页查询反馈（管理端）
     */
    @Select("<script>" +
            "SELECT" + BASE_COLUMNS + " FROM feedback f LEFT JOIN `user` u ON f.user_id = u.id " +
            "<where>" +
            "  <if test='status != null'> AND f.status = #{status} </if>" +
            "</where>" +
            " ORDER BY f.created_at DESC" +
            "</script>")
    IPage<Feedback> selectFeedbackPage(IPage<Feedback> page, @Param("status") Integer status);

    /**
     * 查询某用户的反馈列表（用户端）
     */
    @Select("SELECT" + BASE_COLUMNS + " FROM feedback f LEFT JOIN `user` u ON f.user_id = u.id " +
            "WHERE f.user_id = #{userId} ORDER BY f.created_at DESC")
    java.util.List<Feedback> selectMyFeedbacks(@Param("userId") Long userId);
}
