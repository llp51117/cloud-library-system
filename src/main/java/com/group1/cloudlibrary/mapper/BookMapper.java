package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 图书 Mapper
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 借阅次数 +1
     */
    @Update("UPDATE book SET borrow_count = borrow_count + 1 WHERE id = #{bookId}")
    int increaseBorrowCount(@Param("bookId") Long bookId);

    /**
     * 收藏人数 +1 / -1（不会小于 0）
     */
    @Update("UPDATE book SET favorite_count = GREATEST(favorite_count + #{delta}, 0) WHERE id = #{bookId}")
    int updateFavoriteCount(@Param("bookId") Long bookId, @Param("delta") int delta);

    /**
     * 搜索次数 +1
     */
    @Update("UPDATE book SET search_count = search_count + 1 WHERE id = #{bookId}")
    int increaseSearchCount(@Param("bookId") Long bookId);

    /**
     * 库存 -1（借阅成功时扣减）：只有库存 &gt; 0 才扣，返回受影响行数（0 = 没库存了）。
     *
     * <p>用「条件更新」而不是先查再改，是为了并发下也不会超卖：
     * 两个读者同时借最后一本时，只有一个能扣到库存，另一个拿到的行数是 0。</p>
     */
    @Update("UPDATE book SET stock = stock - 1 WHERE id = #{bookId} AND stock > 0")
    int decreaseStockIfAvailable(@Param("bookId") Long bookId);

    /**
     * 库存 +1（归还 / 管理员收回时回补）
     */
    @Update("UPDATE book SET stock = stock + 1 WHERE id = #{bookId}")
    int increaseStock(@Param("bookId") Long bookId);

    /**
     * 热门图书排行（按借阅次数，取前 N 名）
     */
    @Select("SELECT id AS bookId, title AS bookTitle, author AS bookAuthor, platform, borrow_count AS value " +
            "FROM book ORDER BY borrow_count DESC, id ASC LIMIT #{limit}")
    List<Map<String, Object>> selectHotBooks(@Param("limit") int limit);

    /**
     * 收藏排行（按图书收藏人数，取前 N 名）
     */
    @Select("SELECT id AS bookId, title AS bookTitle, author AS bookAuthor, platform, " +
            " cover_url AS coverUrl, favorite_count AS value " +
            "FROM book WHERE title IS NOT NULL AND title <> '' " +
            "ORDER BY favorite_count DESC, id ASC LIMIT #{limit}")
    List<Map<String, Object>> selectFavoriteRank(@Param("limit") int limit);

    /**
     * 搜索排行（按图书搜索次数，取前 N 名）
     */
    @Select("SELECT id AS bookId, title AS bookTitle, author AS bookAuthor, platform, " +
            " cover_url AS coverUrl, search_count AS value " +
            "FROM book WHERE title IS NOT NULL AND title <> '' " +
            "ORDER BY search_count DESC, id ASC LIMIT #{limit}")
    List<Map<String, Object>> selectSearchRank(@Param("limit") int limit);

    /**
     * 按维度聚合的排行榜（按作者 / 按平台）
     *
     * @param group  聚合列，仅允许 author / platform（由 Service 白名单传入，不接受外部字符串）
     * @param metric 统计列，仅允许 search_count / favorite_count
     */
    @Select("SELECT ${group} AS name, COUNT(*) AS bookCount, SUM(${metric}) AS value " +
            "FROM book WHERE ${group} IS NOT NULL AND ${group} <> '' " +
            "GROUP BY ${group} ORDER BY value DESC, name ASC LIMIT #{limit}")
    List<Map<String, Object>> selectRankByGroup(@Param("group") String group,
                                                @Param("metric") String metric,
                                                @Param("limit") int limit);

    /**
     * 关键字语义兜底检索（Milvus 未部署时使用）
     */
    @Select("<script>" +
            "SELECT * FROM book " +
            "<where>" +
            "  <foreach collection='keywords' item='kw' separator=' OR ' open='(' close=')'>" +
            "    title LIKE CONCAT('%', #{kw}, '%') OR author LIKE CONCAT('%', #{kw}, '%') " +
            "    OR platform LIKE CONCAT('%', #{kw}, '%') OR description LIKE CONCAT('%', #{kw}, '%')" +
            "  </foreach>" +
            "</where>" +
            " ORDER BY favorite_count DESC LIMIT #{limit}" +
            "</script>")
    List<Book> searchByKeywords(@Param("keywords") List<String> keywords, @Param("limit") int limit);

    /**
     * 基于简介（description）的相似推荐：书名 / 作者没命中时，用核心词去简介里再查一次
     */
    @Select("<script>" +
            "SELECT * FROM book " +
            "<where>" +
            "  <foreach collection='keywords' item='kw' separator=' OR ' open='(' close=')'>" +
            "    description LIKE CONCAT('%', #{kw}, '%')" +
            "  </foreach>" +
            "</where>" +
            " ORDER BY favorite_count DESC, id ASC LIMIT #{limit}" +
            "</script>")
    List<Book> searchByDescription(@Param("keywords") List<String> keywords, @Param("limit") int limit);

    /**
     * 受欢迎的图书（用于用户没说具体书名 / 什么都没查到时给大模型做兜底推荐）
     */
    @Select("SELECT * FROM book ORDER BY favorite_count DESC, id ASC LIMIT #{limit}")
    List<Book> selectPopularBooks(@Param("limit") int limit);

    /**
     * 关键字模糊查询的命中总数（与 searchByKeywords 条件一致，用于「有多少本」类问题）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM book " +
            "<where>" +
            "  <foreach collection='keywords' item='kw' separator=' OR ' open='(' close=')'>" +
            "    title LIKE CONCAT('%', #{kw}, '%') OR author LIKE CONCAT('%', #{kw}, '%') " +
            "    OR platform LIKE CONCAT('%', #{kw}, '%') OR description LIKE CONCAT('%', #{kw}, '%')" +
            "  </foreach>" +
            "</where>" +
            "</script>")
    long countByKeywords(@Param("keywords") List<String> keywords);
}
