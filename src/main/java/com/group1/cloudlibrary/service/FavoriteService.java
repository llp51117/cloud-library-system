package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.entity.Favorite;
import com.group1.cloudlibrary.mapper.BookMapper;
import com.group1.cloudlibrary.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收藏服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final BookMapper bookMapper;

    /**
     * 切换收藏状态
     *
     * @return true 表示已收藏，false 表示已取消收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw BusinessException.of("图书不存在");
        }
        Favorite exist = favoriteMapper.selectOne(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBookId, bookId));
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            bookMapper.updateFavoriteCount(bookId, -1);
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setBookId(bookId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        bookMapper.updateFavoriteCount(bookId, 1);
        return true;
    }

    /**
     * 我的收藏列表
     */
    public List<Favorite> myFavorites(Long userId) {
        return favoriteMapper.selectMyFavorites(userId);
    }

    /**
     * 某用户是否收藏了某本书
     */
    public boolean isFavorited(Long userId, Long bookId) {
        if (userId == null) {
            return false;
        }
        Long count = favoriteMapper.selectCount(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBookId, bookId));
        return count != null && count > 0;
    }
}
