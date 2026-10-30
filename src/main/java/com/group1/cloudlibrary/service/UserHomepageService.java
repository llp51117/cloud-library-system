package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.entity.Borrow;
import com.group1.cloudlibrary.entity.Favorite;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.mapper.BookMapper;
import com.group1.cloudlibrary.mapper.BorrowMapper;
import com.group1.cloudlibrary.mapper.FavoriteMapper;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.util.NicknameUtil;
import com.group1.cloudlibrary.vo.UserHomepageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户个人主页服务（评论区点击头像 / 昵称进入）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserHomepageService {

    private static final int BORROW_ING = 0;
    private static final int BORROW_OVERDUE = 2;

    private final UserMapper userMapper;
    private final BorrowMapper borrowMapper;
    private final FavoriteMapper favoriteMapper;
    private final BookMapper bookMapper;

    /**
     * 个人主页数据：头像 / 昵称 / 注册时间 / 账号状态 / 身份标签 / 封号 / 正在阅读 / 已收藏
     *
     * <p>正式管理员（role=1 且 pending_admin=0）不能借书，主页不展示「正在阅读 / 已收藏」；
     * 最高管理员主页则只有基本信息（前端按身份渲染不同版式）。</p>
     */
    public UserHomepageVO homepage(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        UserHomepageVO vo = new UserHomepageVO();
        vo.setId(user.getId());
        vo.setNickname(displayName(user));
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole() == null ? 0 : user.getRole());
        vo.setIsSuperAdmin(user.getIsSuperAdmin() == null ? 0 : user.getIsSuperAdmin());
        vo.setPendingAdmin(user.getPendingAdmin() == null ? 0 : user.getPendingAdmin());
        vo.setStatus(user.getStatus() == null ? 1 : user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setIdentity(identityOf(user));
        vo.setAdminTitle(user.getAdminTitle());
        // 正式管理员不能借书，也不展示收藏：借阅 / 收藏区域直接不返回数据
        if (isFormalAdmin(user)) {
            vo.setReading(new ArrayList<>());
            vo.setFavorites(new ArrayList<>());
        } else {
            vo.setReading(loadReading(userId));
            vo.setFavorites(loadFavorites(userId));
        }
        return vo;
    }

    /** 是否正式管理员（role=1 且 pending_admin=0，即已进入管理端的管理员） */
    public static boolean isFormalAdmin(User user) {
        return user != null
                && user.getRole() != null && user.getRole() == 1
                && (user.getPendingAdmin() == null || user.getPendingAdmin() == 0);
    }

    /** 身份标签文本 */
    public static String identityOf(User user) {
        if (user.getIsSuperAdmin() != null && user.getIsSuperAdmin() == 1) {
            return "最高管理员";
        }
        if (user.getRole() != null && user.getRole() == 1) {
            return user.getPendingAdmin() != null && user.getPendingAdmin() == 1 ? "待激活管理员" : "管理员";
        }
        return "普通用户";
    }

    /** 正在阅读：借阅中 + 逾期未还 */
    private List<UserHomepageVO.HomepageBookVO> loadReading(Long userId) {
        List<Borrow> borrows = borrowMapper.selectList(Wrappers.<Borrow>lambdaQuery()
                .eq(Borrow::getUserId, userId)
                .in(Borrow::getStatus, BORROW_ING, BORROW_OVERDUE)
                .orderByDesc(Borrow::getBorrowDate));
        List<UserHomepageVO.HomepageBookVO> list = new ArrayList<>();
        if (borrows.isEmpty()) {
            return list;
        }
        Map<Long, Book> bookMap = loadBooks(borrows.stream().map(Borrow::getBookId).toList());
        for (Borrow borrow : borrows) {
            Book book = bookMap.get(borrow.getBookId());
            if (book == null) {
                continue;
            }
            UserHomepageVO.HomepageBookVO item = toItem(book);
            item.setBorrowStatus(borrow.getStatus());
            item.setDueDate(borrow.getDueDate());
            list.add(item);
        }
        return list;
    }

    /** 已收藏 */
    private List<UserHomepageVO.HomepageBookVO> loadFavorites(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreatedAt));
        List<UserHomepageVO.HomepageBookVO> list = new ArrayList<>();
        if (favorites.isEmpty()) {
            return list;
        }
        Map<Long, Book> bookMap = loadBooks(favorites.stream().map(Favorite::getBookId).toList());
        for (Favorite favorite : favorites) {
            Book book = bookMap.get(favorite.getBookId());
            if (book == null) {
                continue;
            }
            UserHomepageVO.HomepageBookVO item = toItem(book);
            item.setFavoriteTime(favorite.getCreatedAt());
            list.add(item);
        }
        return list;
    }

    private Map<Long, Book> loadBooks(List<Long> bookIds) {
        Map<Long, Book> map = new HashMap<>();
        if (bookIds == null || bookIds.isEmpty()) {
            return map;
        }
        for (Book book : bookMapper.selectBatchIds(bookIds)) {
            map.put(book.getId(), book);
        }
        return map;
    }

    private UserHomepageVO.HomepageBookVO toItem(Book book) {
        UserHomepageVO.HomepageBookVO item = new UserHomepageVO.HomepageBookVO();
        item.setId(book.getId());
        item.setTitle(book.getTitle());
        item.setAuthor(book.getAuthor());
        item.setCoverUrl(book.getCoverUrl());
        item.setCoverCardCrop(book.getCoverCardCrop());
        return item;
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : NicknameUtil.fromEmail(user.getEmail());
    }
}
