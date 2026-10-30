package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 全部标记已读
     */
    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);

    /**
     * 按类型分组统计某用户的通知数量（type, cnt）
     */
    @Select("SELECT type, COUNT(*) AS cnt FROM notification WHERE user_id = #{userId} GROUP BY type")
    List<Map<String, Object>> countGroupByType(@Param("userId") Long userId);
}
