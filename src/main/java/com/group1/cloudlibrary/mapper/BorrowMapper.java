package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.group1.cloudlibrary.entity.Borrow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 借阅记录 Mapper
 */
@Mapper
public interface BorrowMapper extends BaseMapper<Borrow> {

    String BASE_COLUMNS = " b.*, u.email AS user_email, u.nickname AS user_nickname, u.avatar AS user_avatar, " +
            " bk.title AS book_title, bk.author AS book_author, bk.platform AS book_platform, bk.cover_url AS book_cover ";

    String BASE_JOIN = " FROM borrow b LEFT JOIN `user` u ON b.user_id = u.id " +
            " LEFT JOIN book bk ON b.book_id = bk.id ";

    /**
     * 分页查询借阅记录（用户端：我的借阅 / 管理端：全部记录）
     *
     * @param searchType user-按用户邮箱 / book-按书名 / platform-按平台
     */
    @Select("<script>" +
            "SELECT" + BASE_COLUMNS + BASE_JOIN +
            "<where>" +
            "  <if test='userId != null'> AND b.user_id = #{userId} </if>" +
            "  <if test='status != null'> AND b.status = #{status} </if>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    <choose>" +
            "      <when test='searchType == \"user\"'> AND (u.email LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%')) </when>" +
            "      <when test='searchType == \"book\"'> AND bk.title LIKE CONCAT('%', #{keyword}, '%') </when>" +
            "      <when test='searchType == \"platform\"'> AND bk.platform LIKE CONCAT('%', #{keyword}, '%') </when>" +
            "      <otherwise> AND (bk.title LIKE CONCAT('%', #{keyword}, '%') OR bk.author LIKE CONCAT('%', #{keyword}, '%')) </otherwise>" +
            "    </choose>" +
            "  </if>" +
            "</where>" +
            " ORDER BY b.borrow_date DESC" +
            "</script>")
    IPage<Borrow> selectBorrowPage(IPage<Borrow> page,
                                   @Param("userId") Long userId,
                                   @Param("status") Integer status,
                                   @Param("searchType") String searchType,
                                   @Param("keyword") String keyword);

    /**
     * 查询用户当前借阅中的图书ID集合（用于判断图书是否被借出）
     */
    @Select("SELECT book_id FROM borrow WHERE status IN (0, 2) GROUP BY book_id")
    List<Long> selectBorrowedBookIds();

    /**
     * 近 N 天每日借阅量（借阅趋势折线图）
     */
    @Select("SELECT DATE(borrow_date) AS day, COUNT(*) AS count FROM borrow " +
            "WHERE borrow_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(borrow_date) ORDER BY day ASC")
    List<Map<String, Object>> selectBorrowTrend(@Param("days") int days);

    /**
     * 即将逾期 / 已逾期的借阅记录（自动催还使用）
     */
    @Select("SELECT b.*, u.email AS user_email, u.nickname AS user_nickname, bk.title AS book_title FROM borrow b " +
            "LEFT JOIN `user` u ON b.user_id = u.id LEFT JOIN book bk ON b.book_id = bk.id " +
            "WHERE b.status IN (0, 2) AND b.due_date <= DATE_ADD(NOW(), INTERVAL #{days} DAY)")
    List<Borrow> selectDueSoon(@Param("days") int days);

    /**
     * 统计指定状态的借阅数量
     */
    @Select("SELECT COUNT(*) FROM borrow WHERE status = #{status}")
    long countByStatus(@Param("status") int status);
}
