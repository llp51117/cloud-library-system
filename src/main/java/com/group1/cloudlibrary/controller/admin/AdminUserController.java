package com.group1.cloudlibrary.controller.admin;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.AdminTitleDTO;
import com.group1.cloudlibrary.dto.BanUserDTO;
import com.group1.cloudlibrary.service.AdminGuard;
import com.group1.cloudlibrary.service.UserAdminService;
import com.group1.cloudlibrary.service.UserService;
import com.group1.cloudlibrary.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（管理端）
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final UserAdminService userAdminService;
    private final AdminGuard adminGuard;

    /** 用户列表（关键字搜索；排序：最高管理员 → 管理员 → 普通用户） */
    @GetMapping
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword) {
        return Result.success(userService.adminPage(page, size, keyword));
    }

    /** 禁用用户（填写禁用天数与原因） */
    @PutMapping("/{id}/ban")
    public Result<Void> ban(@PathVariable Long id, @Valid @RequestBody BanUserDTO dto) {
        userService.banUser(id, dto);
        return Result.success("已禁用该用户", null);
    }

    /** 启用用户（恢复正常状态） */
    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        userService.enableUser(id);
        return Result.success("已恢复该用户", null);
    }

    /** 注销用户（超管直接执行，普通管理员需先提交操作申请） */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        userService.cancelUser(id);
        return Result.success("已注销该用户", null);
    }

    /** 该用户未归还（借阅中 + 逾期）的图书数量：设为管理员前弹窗判断用 */
    @GetMapping("/{id}/unreturned-count")
    public Result<Long> unreturnedCount(@PathVariable Long id) {
        return Result.success(userAdminService.unreturnedCount(id));
    }

    /**
     * 设为管理员（只有最高管理员能操作）。
     * 没有未归还图书 → 直接成为正式管理员；还有未归还图书 → 先设为待激活管理员（10 天内还清即可转正）。
     */
    @PutMapping("/{id}/set-admin")
    public Result<Boolean> setAdmin(@PathVariable Long id) {
        boolean pending = userAdminService.setAdmin(id);
        return Result.success(pending
                ? "该用户还有未归还图书，已设为待激活管理员，10 天内归还全部图书即可转正"
                : "已设为正式管理员", pending);
    }

    /** 取消管理员权限（只有最高管理员能操作），恢复为普通用户 */
    @PutMapping("/{id}/cancel-admin")
    public Result<Void> cancelAdmin(@PathVariable Long id) {
        userAdminService.cancelAdmin(id);
        return Result.success("已取消管理员权限，该用户恢复为普通用户", null);
    }

    /**
     * 设置管理员封号（只有最高管理员能操作），如「图书管理专员」；
     * title 传空表示清除封号
     */
    @PutMapping("/{id}/admin-title")
    public Result<Void> setAdminTitle(@PathVariable Long id, @Valid @RequestBody AdminTitleDTO dto) {
        userAdminService.setAdminTitle(id, dto.getTitle());
        boolean cleared = !StringUtils.hasText(dto.getTitle());
        return Result.success(cleared ? "已清除该管理员的封号" : "已设置管理员封号：" + dto.getTitle().trim(), null);
    }

    /** 彻底删除用户及其全部关联数据（超管直接执行，普通管理员需先提交操作申请） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminGuard.requireSuperAdminOrApply("删除用户", "delete_user");
        userAdminService.deleteUserCompletely(id);
        return Result.success("已删除该用户及其关联数据", null);
    }
}
