package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 预约记录 Mapper
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    String BASE_COLUMNS = " r.*, bk.title AS book_title, bk.author AS book_author, " +
            " bk.platform AS book_platform, bk.cover_url AS book_cover ";

    /**
     * 查询某用户的预约列表
     */
    @Select("SELECT" + BASE_COLUMNS + " FROM reservation r LEFT JOIN book bk ON r.book_id = bk.id " +
            "WHERE r.user_id = #{userId} ORDER BY r.reservation_time DESC")
    List<Reservation> selectMyReservations(@Param("userId") Long userId);

    /**
     * 查询某本书的有效排队队列（等待借阅/可借阅）
     */
    @Select("SELECT" + BASE_COLUMNS + " FROM reservation r LEFT JOIN book bk ON r.book_id = bk.id " +
            "WHERE r.book_id = #{bookId} AND r.status IN (0, 1) ORDER BY r.reservation_time ASC")
    List<Reservation> selectQueue(@Param("bookId") Long bookId);

    /**
     * 查询所有已超时且状态为“可借阅”的预约（定时任务顺延使用）
     */
    @Select("SELECT * FROM reservation WHERE status = 1 AND available_time <= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)")
    List<Reservation> selectTimeoutReservations(@Param("hours") int hours);
}
