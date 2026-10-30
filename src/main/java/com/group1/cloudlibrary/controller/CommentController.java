package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.CommentDTO;
import com.group1.cloudlibrary.entity.Comment;
import com.group1.cloudlibrary.service.CommentService;
import com.group1.cloudlibrary.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 评论接口（用户端）
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 某本图书的评论列表 */
    @GetMapping
    public Result<PageResult<Comment>> list(@RequestParam Long bookId,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.success(commentService.listByBook(bookId, page, size));
    }

    /** 发表评论 */
    @PostMapping
    public Result<Comment> add(@Valid @RequestBody CommentDTO dto) {
        return Result.success("评论成功", commentService.add(UserContext.requireUserId(), dto));
    }

    /** 点赞/取消点赞评论（切换式，返回最新点赞数与当前用户点赞状态） */
    @PutMapping("/{id}/like")
    public Result<Map<String, Object>> like(@PathVariable Long id) {
        return Result.success(commentService.toggleLike(id));
    }

    /** 踩/取消踩评论（切换式，与点赞互相独立，返回最新踩数与当前用户点踩状态） */
    @PutMapping("/{id}/dislike")
    public Result<Map<String, Object>> dislike(@PathVariable Long id) {
        return Result.success(commentService.toggleDislike(id));
    }

    /** @管理员 举报评论（同一用户重复举报只计一次，返回举报总次数） */
    @PutMapping("/{id}/report")
    public Result<Map<String, Object>> report(@PathVariable Long id) {
        return Result.success("已通知管理员审核", commentService.report(id));
    }
}
