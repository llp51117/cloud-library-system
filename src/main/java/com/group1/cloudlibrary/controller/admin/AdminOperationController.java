package com.group1.cloudlibrary.controller.admin;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.service.AdminGuard;
import com.group1.cloudlibrary.service.AdminOperationService;
import com.group1.cloudlibrary.vo.AdminOperationStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员操作记录接口（管理端）
 *
 * <p>管理员个人主页的「管理操作记录」区域使用，只对最高管理员开放。</p>
 */
@RestController
@RequestMapping("/api/admin/operations")
@RequiredArgsConstructor
public class AdminOperationController {

    private final AdminOperationService adminOperationService;
    private final AdminGuard adminGuard;

    /**
     * 查询某个管理员做过的操作：
     * 处理过多少条举报 / 删过多少条评论 / 编辑过多少本书，以及最近的操作明细
     */
    @GetMapping("/{adminId}")
    public Result<AdminOperationStatsVO> stats(@PathVariable Long adminId) {
        adminGuard.requireSuperAdmin("查看管理操作记录");
        return Result.success(adminOperationService.stats(adminId));
    }
}
