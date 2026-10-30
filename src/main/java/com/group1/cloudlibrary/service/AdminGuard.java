package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 管理员身份守卫：统一判断「最高管理员 / 普通管理员 / 待激活管理员」，
 * 所有需要超管权限、或需要走「操作申请」的地方都从这里取结论，避免各处重复查库。
 */
@Component
@RequiredArgsConstructor
public class AdminGuard {

    private final UserMapper userMapper;

    /** 当前登录用户（未登录返回 null） */
    public User currentUser() {
        Long userId = UserContext.getUserId();
        return userId == null ? null : userMapper.selectById(userId);
    }

    /** 某个用户是否最高管理员 */
    public boolean isSuperAdmin(User user) {
        return user != null && user.getIsSuperAdmin() != null && user.getIsSuperAdmin() == 1;
    }

    /** 当前登录用户是否最高管理员 */
    public boolean isSuperAdmin() {
        return isSuperAdmin(currentUser());
    }

    /**
     * 校验当前登录用户是最高管理员（超管），否则抛业务异常
     *
     * @param action 动作描述，用于拼接提示语，如「设置管理员」
     */
    public User requireSuperAdmin(String action) {
        User user = currentUser();
        if (user == null) {
            throw BusinessException.of("请先登录");
        }
        if (!isSuperAdmin(user)) {
            throw BusinessException.of("只有最高管理员才能" + action);
        }
        return user;
    }

    /**
     * 校验「高风险操作」的执行权限：最高管理员可直接执行，
     * 普通管理员必须提交操作申请，由超管审批后执行。
     *
     * @param action    动作描述，如「批量删除图书」
     * @param applyType 对应的申请类型，如 batch_delete_book
     */
    public void requireSuperAdminOrApply(String action, String applyType) {
        User user = currentUser();
        if (user == null) {
            throw BusinessException.of("请先登录");
        }
        if (!isSuperAdmin(user)) {
            throw BusinessException.of("「" + action + "」需要提交操作申请，由最高管理员审批。"
                    + "请在管理端点击「操作申请」提交（申请类型：" + applyType + "）");
        }
    }

    /** 所有最高管理员的用户ID（用于给超管发通知） */
    public List<Long> superAdminIds() {
        return userMapper.selectList(Wrappers.<User>lambdaQuery()
                        .select(User::getId)
                        .eq(User::getIsSuperAdmin, 1))
                .stream()
                .map(User::getId)
                .toList();
    }
}
