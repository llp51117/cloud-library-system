package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.group1.cloudlibrary.dto.BookCommentStats;
import com.group1.cloudlibrary.entity.Comment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评论 Mapper
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    String BASE_COLUMNS = " c.*, u.email AS user_email, u.nickname AS user_nickname, u.avatar AS user_avatar, " +
            " u.role AS user_role, u.is_super_admin AS user_super_admin, u.pending_admin AS user_pending_admin, " +
            " u.admin_title AS user_admin_title, " +
            " (SELECT COUNT(*) FROM comment_report cr WHERE cr.comment_id = c.id) AS report_count, " +
            " bk.title AS book_title ";

    String BASE_JOIN = " FROM comment c LEFT JOIN `user` u ON c.user_id = u.id " +
            " LEFT JOIN book bk ON c.book_id = bk.id ";

    /**
     * 分页查询评论
     *
     * @param bookId     图书ID（可选）
     * @param statusList 状态筛选（空表示不限制）
     * @param reported   是否只看 @管理员 标记（1 表示只看）
     * @param onlyRoot   true 表示只查顶级评论（用户端列表，楼中楼回复单独挂载）
     */
    @Select("<script>" +
            "SELECT" + BASE_COLUMNS + BASE_JOIN +
            "<where>" +
            "  <if test='bookId != null'> AND c.book_id = #{bookId} </if>" +
            "  <if test='reported != null'> AND c.is_reported = #{reported} </if>" +
            "  <if test='onlyRoot != null and onlyRoot'> AND c.parent_id IS NULL </if>" +
            "  <if test='statusList != null and statusList.size() > 0'>" +
            "    AND c.status IN " +
            "    <foreach collection='statusList' item='st' open='(' separator=',' close=')'>#{st}</foreach>" +
            "  </if>" +
            "</where>" +
            " ORDER BY c.created_at DESC" +
            "</script>")
    IPage<Comment> selectCommentPage(IPage<Comment> page,
                                     @Param("bookId") Long bookId,
                                     @Param("statusList") List<Integer> statusList,
                                     @Param("reported") Integer reported,
                                     @Param("onlyRoot") Boolean onlyRoot);

    /**
     * 查询给定父评论的子回复（楼中楼，只支持一层），按时间正序展示
     */
    @Select("<script>" +
            "SELECT" + BASE_COLUMNS + BASE_JOIN +
            " WHERE c.parent_id IN " +
            "<foreach collection='ids' item='pid' open='(' separator=',' close=')'>#{pid}</foreach>" +
            " ORDER BY c.created_at ASC" +
            "</script>")
    List<Comment> selectReplies(@Param("ids") List<Long> ids);

    /**
     * 点赞 +1
     */
    @Update("UPDATE comment SET like_count = like_count + 1 WHERE id = #{id}")
    int increaseLike(@Param("id") Long id);

    /**
     * 点赞 -1（不会小于 0）
     */
    @Update("UPDATE comment SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decreaseLike(@Param("id") Long id);

    /**
     * 当前用户是否已点赞该评论
     */
    @Select("SELECT COUNT(*) FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int countLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 记录点赞（唯一索引去重，重复插入返回 0）
     */
    @Insert("INSERT IGNORE INTO comment_like (user_id, comment_id) VALUES (#{userId}, #{commentId})")
    int insertLike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    /**
     * 取消点赞（删除记录）
     */
    @Delete("DELETE FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 查询当前用户在给定评论集合中已点赞的评论ID
     */
    @Select("<script>" +
            "SELECT comment_id FROM comment_like WHERE user_id = #{userId} AND comment_id IN " +
            "<foreach collection='ids' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>" +
            "</script>")
    List<Long> selectLikedIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    /**
     * 踩 +1
     */
    @Update("UPDATE comment SET dislike_count = dislike_count + 1 WHERE id = #{id}")
    int increaseDislike(@Param("id") Long id);

    /**
     * 踩 -1（不会小于 0）
     */
    @Update("UPDATE comment SET dislike_count = GREATEST(dislike_count - 1, 0) WHERE id = #{id}")
    int decreaseDislike(@Param("id") Long id);

    /**
     * 当前用户是否已踩该评论
     */
    @Select("SELECT COUNT(*) FROM comment_dislike WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int countDislike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 记录踩（唯一索引去重，重复插入返回 0）
     */
    @Insert("INSERT IGNORE INTO comment_dislike (user_id, comment_id) VALUES (#{userId}, #{commentId})")
    int insertDislike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    /**
     * 取消踩（删除记录）
     */
    @Delete("DELETE FROM comment_dislike WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteDislike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 查询当前用户在给定评论集合中已踩的评论ID
     */
    @Select("<script>" +
            "SELECT comment_id FROM comment_dislike WHERE user_id = #{userId} AND comment_id IN " +
            "<foreach collection='ids' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>" +
            "</script>")
    List<Long> selectDislikedIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    /**
     * 删除指定图书下所有评论的点赞记录（删评论前必须先清掉，否则外键约束会拦住）
     */
    @Delete("<script>" +
            "DELETE cl FROM comment_like cl JOIN comment c ON cl.comment_id = c.id " +
            "WHERE c.book_id IN " +
            "<foreach collection='bookIds' item='bid' open='(' separator=',' close=')'>#{bid}</foreach>" +
            "</script>")
    int deleteLikeByBookIds(@Param("bookIds") List<Long> bookIds);

    /**
     * 删除指定图书下所有评论的举报记录（删评论前必须先清掉，否则外键约束会拦住）
     */
    @Delete("<script>" +
            "DELETE cr FROM comment_report cr JOIN comment c ON cr.comment_id = c.id " +
            "WHERE c.book_id IN " +
            "<foreach collection='bookIds' item='bid' open='(' separator=',' close=')'>#{bid}</foreach>" +
            "</script>")
    int deleteReportByBookIds(@Param("bookIds") List<Long> bookIds);

    /**
     * 删除指定图书下所有评论的踩记录（删评论前必须先清掉，否则外键约束会拦住）
     */
    @Delete("<script>" +
            "DELETE cd FROM comment_dislike cd JOIN comment c ON cd.comment_id = c.id " +
            "WHERE c.book_id IN " +
            "<foreach collection='bookIds' item='bid' open='(' separator=',' close=')'>#{bid}</foreach>" +
            "</script>")
    int deleteDislikeByBookIds(@Param("bookIds") List<Long> bookIds);

    /**
     * 该用户自己发表的评论ID（顶级评论 + 他发的回复）
     */
    @Select("SELECT id FROM comment WHERE user_id = #{userId}")
    List<Long> selectCommentIdsByUser(@Param("userId") Long userId);

    /**
     * 别人回复该用户评论产生的回复ID（楼中楼只有一层）
     */
    @Select("SELECT id FROM comment WHERE parent_id IN (SELECT id FROM comment WHERE user_id = #{userId})")
    List<Long> selectReplyIdsToUser(@Param("userId") Long userId);

    /**
     * 删除点赞记录：既包括「该用户自己点的赞」，也包括「别人点赞该用户评论/回复」的记录。
     * 删评论前必须先清掉，否则外键约束会拦住。
     */
    @Delete("<script>" +
            "DELETE FROM comment_like WHERE user_id = #{userId} " +
            "<if test='commentIds != null and commentIds.size() > 0'>" +
            " OR comment_id IN " +
            "<foreach collection='commentIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>" +
            "</if>" +
            "</script>")
    int deleteLikeByUser(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    /**
     * 删除举报记录：既包括「该用户自己提交的举报」，也包括「别人举报该用户评论/回复」的记录
     */
    @Delete("<script>" +
            "DELETE FROM comment_report WHERE user_id = #{userId} " +
            "<if test='commentIds != null and commentIds.size() > 0'>" +
            " OR comment_id IN " +
            "<foreach collection='commentIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>" +
            "</if>" +
            "</script>")
    int deleteReportByUser(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    /**
     * 删除踩记录：既包括「该用户自己点的踩」，也包括「别人踩该用户评论/回复」的记录
     */
    @Delete("<script>" +
            "DELETE FROM comment_dislike WHERE user_id = #{userId} " +
            "<if test='commentIds != null and commentIds.size() > 0'>" +
            " OR comment_id IN " +
            "<foreach collection='commentIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>" +
            "</if>" +
            "</script>")
    int deleteDislikeByUser(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    /**
     * 删除别人对该用户评论的回复（楼中楼，parent_id 无外键但同样要清理）
     */
    @Delete("DELETE r FROM comment r JOIN comment p ON r.parent_id = p.id WHERE p.user_id = #{userId}")
    int deleteRepliesToUserComments(@Param("userId") Long userId);

    /**
     * 删除该用户发表的全部评论
     */
    @Delete("DELETE FROM comment WHERE user_id = #{userId}")
    int deleteByUser(@Param("userId") Long userId);

    /**
     * 记录举报（唯一索引去重：同一用户重复 @ 同一条评论只算一次，返回 0）
     */
    @Insert("INSERT IGNORE INTO comment_report (user_id, comment_id) VALUES (#{userId}, #{commentId})")
    int insertReport(@Param("userId") Long userId, @Param("commentId") Long commentId);

    /**
     * 某条评论被举报的次数（所有用户汇总，重复举报已由唯一索引去重）
     */
    @Select("SELECT COUNT(*) FROM comment_report WHERE comment_id = #{commentId}")
    int countReport(@Param("commentId") Long commentId);

    /**
     * 管理端：按图书聚合的评论统计（评论数 + 被举报数）
     * 排序：置顶优先 → 被举报多优先 → 评论数多优先
     */
    @Select("SELECT bk.id, bk.title, bk.author, bk.cover_url AS coverUrl, bk.comment_pinned AS commentPinned, " +
            " (SELECT COUNT(*) FROM comment c WHERE c.book_id = bk.id) AS commentCount, " +
            " (SELECT COALESCE(SUM(c3.like_count), 0) FROM comment c3 WHERE c3.book_id = bk.id) AS likeCount, " +
            " (SELECT COALESCE(SUM(c4.dislike_count), 0) FROM comment c4 WHERE c4.book_id = bk.id) AS dislikeCount, " +
            " (SELECT COUNT(*) FROM comment c2 WHERE c2.book_id = bk.id AND c2.is_reported = 1) AS reportCount " +
            "FROM book bk " +
            "ORDER BY bk.comment_pinned DESC, reportCount DESC, commentCount DESC, bk.id ASC")
    IPage<BookCommentStats> selectBookCommentStats(IPage<BookCommentStats> page);
}
