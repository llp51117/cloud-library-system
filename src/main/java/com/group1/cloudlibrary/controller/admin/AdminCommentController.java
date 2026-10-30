package com.group1.cloudlibrary.controller.admin;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.BookCommentStats;
import com.group1.cloudlibrary.entity.Comment;
import com.group1.cloudlibrary.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论管理接口（管理端）
 */
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    /**
     * 评论列表
     *
     * @param tab all-全部评论 / system-系统已删 / reported-@管理员
     */
    @GetMapping
    public Result<PageResult<Comment>> list(@RequestParam(defaultValue = "all") String tab,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.success(commentService.adminPage(tab, page, size));
    }

    /** 删除评论 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.adminDelete(id);
        return Result.success("评论已删除", null);
    }

    /**
     * 按图书聚合的评论统计列表（置顶优先 → 被举报多优先 → 评论数多优先）
     */
    @GetMapping("/books")
    public Result<PageResult<BookCommentStats>> bookStats(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size) {
        return Result.success(commentService.adminBookStats(page, size));
    }

    /** 某本图书的全部评论（含被删除的、被举报的） */
    @GetMapping("/books/{bookId}/comments")
    public Result<PageResult<Comment>> bookComments(@PathVariable Long bookId,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return Result.success(commentService.adminListByBook(bookId, page, size));
    }

    /** 置顶 / 取消置顶某本书（返回最新置顶状态：0否/1是） */
    @PutMapping("/books/{bookId}/pin")
    public Result<Integer> togglePin(@PathVariable Long bookId) {
        return Result.success(commentService.adminTogglePin(bookId));
    }

    /** 恢复评论（系统已删 → 恢复） */
    @PutMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        commentService.adminRestore(id);
        return Result.success("评论已恢复", null);
    }
}
