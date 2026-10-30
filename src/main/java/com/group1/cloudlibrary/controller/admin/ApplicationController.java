package com.group1.cloudlibrary.controller.admin;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.ApplicationDTO;
import com.group1.cloudlibrary.dto.ApplicationReplyDTO;
import com.group1.cloudlibrary.entity.Application;
import com.group1.cloudlibrary.service.ApplicationService;
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
 * 操作申请接口（管理端）
 *
 * <p>普通管理员提交申请；最高管理员在「通知公告 → 操作申请」里审批。</p>
 */
@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /** 提交操作申请（普通管理员） */
    @PostMapping
    public Result<Long> apply(@Valid @RequestBody ApplicationDTO dto) {
        return Result.success("申请已提交，等待最高管理员审批", applicationService.apply(dto));
    }

    /** 操作申请列表（超管看全部；mine=true 只看自己提交的） */
    @GetMapping
    public Result<PageResult<Application>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "false") boolean mine) {
        return Result.success(applicationService.page(page, size, status, mine));
    }

    /** 待审批申请数量 */
    @GetMapping("/pending-count")
    public Result<Long> pendingCount() {
        return Result.success(applicationService.pendingCount());
    }

    /** 同意申请：系统代为执行对应操作 */
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) ApplicationReplyDTO dto) {
        applicationService.approve(id, dto == null ? null : dto.getReply());
        return Result.success("已同意，操作已执行", null);
    }

    /** 拒绝申请：不执行操作，可填写拒绝理由 */
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) ApplicationReplyDTO dto) {
        applicationService.reject(id, dto == null ? null : dto.getReply());
        return Result.success("已拒绝该申请", null);
    }
}
