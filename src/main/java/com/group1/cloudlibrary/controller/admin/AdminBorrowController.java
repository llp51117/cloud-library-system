package com.group1.cloudlibrary.controller.admin;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.entity.Borrow;
import com.group1.cloudlibrary.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 借阅管理接口（管理端）
 */
@RestController
@RequestMapping("/api/admin/borrows")
@RequiredArgsConstructor
public class AdminBorrowController {

    private final BorrowService borrowService;

    /** 借阅记录查询（按用户/图书/平台搜索 + 状态筛选） */
    @GetMapping
    public Result<PageResult<Borrow>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "user") String searchType,
                                           @RequestParam(required = false) String keyword) {
        return Result.success(borrowService.adminPage(page, size, status, searchType, keyword));
    }

    /** 手动催还（单条） */
    @PutMapping("/{id}/remind")
    public Result<Void> remind(@PathVariable Long id) {
        borrowService.remind(id);
        return Result.success("催还提醒已发送", null);
    }

    /** 一键催还：给所有逾期 / 3 天内到期的借阅记录批量发送催还通知，返回发送条数 */
    @PutMapping("/remind-all")
    public Result<Integer> remindAll() {
        int count = borrowService.remindAll();
        return Result.success("已发送 " + count + " 条催还通知", count);
    }
}
