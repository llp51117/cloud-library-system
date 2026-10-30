package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.BookIdDTO;
import com.group1.cloudlibrary.entity.Borrow;
import com.group1.cloudlibrary.service.BorrowService;
import com.group1.cloudlibrary.util.UserContext;
import com.group1.cloudlibrary.vo.MyBookCountsVO;
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

/**
 * 借阅接口（用户端）
 */
@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    /** 提交借阅申请（无人借阅则直接借阅成功，否则自动进入预约队列） */
    @PostMapping
    public Result<String> borrow(@Valid @RequestBody BookIdDTO dto) {
        String message = borrowService.borrow(UserContext.requireUserId(), dto.getBookId());
        return Result.success(message, message);
    }

    /** 我的借阅记录 */
    @GetMapping("/my")
    public Result<PageResult<Borrow>> my(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) Integer status) {
        return Result.success(borrowService.myBorrows(UserContext.requireUserId(), page, size, status));
    }

    /** 「我的书籍」四个子 Tab 的数量统计（当前借阅 / 借阅历史 / 我的收藏 / 我的预约） */
    @GetMapping("/counts")
    public Result<MyBookCountsVO> counts() {
        return Result.success(borrowService.myBookCounts(UserContext.requireUserId()));
    }

    /** 归还图书（提前归还） */
    @PutMapping("/{id}/return")
    public Result<Void> returnBook(@PathVariable Long id) {
        borrowService.returnBook(UserContext.requireUserId(), id);
        return Result.success("归还成功", null);
    }

    /** 续借（延长 15 天） */
    @PutMapping("/{id}/renew")
    public Result<String> renew(@PathVariable Long id) {
        String message = borrowService.renew(UserContext.requireUserId(), id);
        return Result.success(message, message);
    }
}
