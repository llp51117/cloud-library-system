package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.BookIdDTO;
import com.group1.cloudlibrary.entity.Favorite;
import com.group1.cloudlibrary.service.FavoriteService;
import com.group1.cloudlibrary.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收藏接口（用户端）
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** 收藏 / 取消收藏（切换） */
    @PostMapping
    public Result<Boolean> toggle(@Valid @RequestBody BookIdDTO dto) {
        boolean favorited = favoriteService.toggle(UserContext.requireUserId(), dto.getBookId());
        return Result.success(favorited ? "收藏成功" : "已取消收藏", favorited);
    }

    /** 我的收藏列表 */
    @GetMapping("/my")
    public Result<List<Favorite>> my() {
        return Result.success(favoriteService.myFavorites(UserContext.requireUserId()));
    }
}
