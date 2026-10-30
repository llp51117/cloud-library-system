package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.dto.BookCommentStats;
import com.group1.cloudlibrary.dto.CommentDTO;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.entity.Comment;
import com.group1.cloudlibrary.mapper.BookMapper;
import com.group1.cloudlibrary.mapper.CommentMapper;
import com.group1.cloudlibrary.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论服务：发表、点赞、@管理员、管理端审核
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    /** 评论状态：0正常/1系统删除/2管理员删除/3管理员恢复 */
    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_SYSTEM_DELETED = 1;
    public static final int STATUS_ADMIN_DELETED = 2;
    public static final int STATUS_RESTORED = 3;

    /** 敏感词库（系统自动过滤） */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "广告", "代购", "赌博", "诈骗", "色情", "暴力", "刷单", "加微信", "私聊", "违禁",
            "傻逼", "垃圾");

    private final CommentMapper commentMapper;
    private final BookMapper bookMapper;
    private final AdminOperationService adminOperationService;

    /**
     * 用户端：某本图书的评论列表（顶级评论分页 + 楼中楼子回复挂载、当前用户点赞/点踩状态）
     */
    public PageResult<Comment> listByBook(Long bookId, long page, long size) {
        IPage<Comment> result = commentMapper.selectCommentPage(new Page<>(page, size), bookId, null, null, true);
        fillUserState(result.getRecords());
        fillReplies(result.getRecords());
        return PageResult.of(result);
    }

    /**
     * 发表评论（顶级评论或楼中楼回复，含敏感词自动过滤）
     */
    @Transactional(rollbackFor = Exception.class)
    public Comment add(Long userId, CommentDTO dto) {
        if (bookMapper.selectById(dto.getBookId()) == null) {
            throw BusinessException.of("图书不存在");
        }
        // 楼中楼回复：父评论必须存在、属于同一本书，且本身是顶级评论（只支持一层）
        if (dto.getParentId() != null) {
            Comment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null || !parent.getBookId().equals(dto.getBookId())) {
                throw BusinessException.of("要回复的评论不存在");
            }
            if (parent.getParentId() != null) {
                throw BusinessException.of("回复只支持一层，不能回复子回复");
            }
        }
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setBookId(dto.getBookId());
        comment.setContent(dto.getContent());
        comment.setLikeCount(0);
        comment.setDislikeCount(0);
        comment.setParentId(dto.getParentId());
        comment.setIsReported(0);
        comment.setCreatedAt(LocalDateTime.now());

        if (containsSensitiveWord(dto.getContent())) {
            comment.setStatus(STATUS_SYSTEM_DELETED);
            log.info("评论包含敏感词，已自动删除：bookId={}", dto.getBookId());
        } else {
            comment.setStatus(STATUS_NORMAL);
        }
        commentMapper.insert(comment);
        return comment;
    }

    /**
     * 点赞评论（切换式：未赞 → 点赞 +1；已赞 → 取消点赞 -1；一个用户对同一条评论只能赞一次）
     *
     * @return likeCount-最新点赞数 / liked-当前是否已点赞
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.of("评论不存在");
        }
        Long userId = UserContext.requireUserId();
        boolean liked;
        if (commentMapper.countLike(commentId, userId) > 0) {
            commentMapper.deleteLike(commentId, userId);
            commentMapper.decreaseLike(commentId);
            liked = false;
        } else {
            commentMapper.insertLike(userId, commentId);
            commentMapper.increaseLike(commentId);
            liked = true;
        }
        Comment updated = commentMapper.selectById(commentId);
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("likeCount", updated.getLikeCount() == null ? 0 : updated.getLikeCount());
        return data;
    }

    /**
     * 踩评论（切换式：未踩 → 踩 +1；已踩 → 取消踩 -1；一个用户对同一条评论只能踩一次）
     * 与点赞互相独立，可以同时点赞和点踩
     *
     * @return disliked-当前是否已踩 / dislikeCount-最新踩数
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleDislike(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.of("评论不存在");
        }
        Long userId = UserContext.requireUserId();
        boolean disliked;
        if (commentMapper.countDislike(commentId, userId) > 0) {
            commentMapper.deleteDislike(commentId, userId);
            commentMapper.decreaseDislike(commentId);
            disliked = false;
        } else {
            commentMapper.insertDislike(userId, commentId);
            commentMapper.increaseDislike(commentId);
            disliked = true;
        }
        Comment updated = commentMapper.selectById(commentId);
        Map<String, Object> data = new HashMap<>();
        data.put("disliked", disliked);
        data.put("dislikeCount", updated.getDislikeCount() == null ? 0 : updated.getDislikeCount());
        return data;
    }

    /**
     * @管理员 举报评论（同一用户对同一条评论重复 @ 只算一次，不同用户举报次数累加）
     *
     * @return reported-是否已举报 / reportCount-该评论被举报的总次数
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> report(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.of("评论不存在");
        }
        Long userId = UserContext.requireUserId();
        int inserted = commentMapper.insertReport(userId, commentId);
        if (comment.getIsReported() == null || comment.getIsReported() == 0) {
            comment.setIsReported(1);
            commentMapper.updateById(comment);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("reported", true);
        data.put("alreadyReported", inserted == 0);
        data.put("reportCount", commentMapper.countReport(commentId));
        return data;
    }

    /**
     * 管理端：分页查询评论
     *
     * @param tab all-全部正常 / system-系统已删 / reported-@管理员
     */
    public PageResult<Comment> adminPage(String tab, long page, long size) {
        String type = tab == null ? "all" : tab;
        IPage<Comment> result = switch (type) {
            case "system" -> commentMapper.selectCommentPage(new Page<>(page, size), null,
                    List.of(STATUS_SYSTEM_DELETED), null, null);
            // @管理员列表：只看还需要处理的（正常/已恢复），管理员已删除的不再列出
            case "reported" -> commentMapper.selectCommentPage(new Page<>(page, size), null,
                    List.of(STATUS_NORMAL, STATUS_RESTORED), 1, null);
            default -> commentMapper.selectCommentPage(new Page<>(page, size), null,
                    List.of(STATUS_NORMAL, STATUS_RESTORED), null, null);
        };
        result.getRecords().forEach(this::fillExtra);
        return PageResult.of(result);
    }

    /**
     * 管理端：删除评论
     *
     * <p>同时写入管理员操作记录：删除评论 1 条；如果这条评论是被 @管理员 标记的，
     * 再记 1 条「处理举报」。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminDelete(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.of("评论不存在");
        }
        boolean reported = isReported(comment);
        comment.setStatus(STATUS_ADMIN_DELETED);
        commentMapper.updateById(comment);

        String brief = AdminOperationService.abbreviate(comment.getContent());
        adminOperationService.record(AdminOperationService.TYPE_COMMENT_DELETE, commentId,
                "删除评论：" + brief);
        if (reported) {
            adminOperationService.record(AdminOperationService.TYPE_REPORT, commentId,
                    "处理举报：删除被标记的评论「" + brief + "」");
        }
    }

    /**
     * 管理端：恢复评论（系统已删 → 管理员恢复）
     *
     * <p>如果恢复的是被 @管理员 标记的评论，记 1 条「处理举报」。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminRestore(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.of("评论不存在");
        }
        boolean reported = isReported(comment);
        comment.setStatus(STATUS_RESTORED);
        comment.setIsReported(0);
        commentMapper.updateById(comment);

        if (reported) {
            adminOperationService.record(AdminOperationService.TYPE_REPORT, commentId,
                    "处理举报：恢复被标记的评论「" + AdminOperationService.abbreviate(comment.getContent()) + "」");
        }
    }

    /** 该评论是否被 @管理员 标记过（用于「处理举报」统计） */
    private boolean isReported(Comment comment) {
        return comment.getIsReported() != null && comment.getIsReported() == 1;
    }

    /**
     * 填充展示字段：删除原因
     */
    private void fillExtra(Comment comment) {
        if (comment.getStatus() != null && comment.getStatus() == STATUS_SYSTEM_DELETED) {
            comment.setDeleteReason("包含敏感词，系统自动删除");
        }
    }

    /**
     * 填充当前用户的点赞 / 点踩状态（liked / disliked 字段）
     */
    private void fillUserState(List<Comment> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Long userId = UserContext.getUserId();
        if (userId == null) {
            records.forEach(c -> {
                c.setLiked(false);
                c.setDisliked(false);
            });
            return;
        }
        List<Long> ids = records.stream().map(Comment::getId).toList();
        Set<Long> likedIds = new HashSet<>(commentMapper.selectLikedIds(userId, ids));
        Set<Long> dislikedIds = new HashSet<>(commentMapper.selectDislikedIds(userId, ids));
        records.forEach(c -> {
            c.setLiked(likedIds.contains(c.getId()));
            c.setDisliked(dislikedIds.contains(c.getId()));
        });
    }

    /**
     * 为分页里的顶级评论挂载楼中楼子回复（只支持一层，子回复不再嵌套）
     */
    private void fillReplies(List<Comment> parents) {
        if (parents == null || parents.isEmpty()) {
            return;
        }
        parents.forEach(p -> p.setReplies(new ArrayList<>()));
        List<Long> parentIds = parents.stream().map(Comment::getId).toList();
        List<Comment> replies = commentMapper.selectReplies(parentIds);
        if (replies == null || replies.isEmpty()) {
            return;
        }
        fillUserState(replies);
        Map<Long, List<Comment>> grouped = replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));
        parents.forEach(p -> p.setReplies(grouped.getOrDefault(p.getId(), new ArrayList<>())));
    }

    /**
     * 管理端：某本图书的全部评论（含被删除的、被举报的）
     */
    public PageResult<Comment> adminListByBook(Long bookId, long page, long size) {
        IPage<Comment> result = commentMapper.selectCommentPage(new Page<>(page, size), bookId, null, null, null);
        result.getRecords().forEach(this::fillExtra);
        return PageResult.of(result);
    }

    /**
     * 管理端：按图书聚合的评论统计列表（置顶优先 → 被举报多优先 → 评论数多优先）
     */
    public PageResult<BookCommentStats> adminBookStats(long page, long size) {
        IPage<BookCommentStats> result = commentMapper.selectBookCommentStats(new Page<>(page, size));
        return PageResult.of(result);
    }

    /**
     * 管理端：置顶 / 取消置顶某本书的评论管理入口
     */
    @Transactional(rollbackFor = Exception.class)
    public int adminTogglePin(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw BusinessException.of("图书不存在");
        }
        int pinned = book.getCommentPinned() != null && book.getCommentPinned() == 1 ? 0 : 1;
        book.setCommentPinned(pinned);
        bookMapper.updateById(book);
        return pinned;
    }

    private boolean containsSensitiveWord(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String lower = content.toLowerCase();
        return SENSITIVE_WORDS.stream().anyMatch(lower::contains);
    }
}
