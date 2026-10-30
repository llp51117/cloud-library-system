package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.service.BookService;
import com.group1.cloudlibrary.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 图书浏览接口（用户端）
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * 图书列表（关键词搜索 + 完结状态筛选，按收藏热度排序）
     *
     * @param keyword    书名 / 作者 / 平台模糊匹配，也可以是「连载中」「已完结」
     * @param isFinished 完结状态筛选：0-连载中，1-已完结，不传表示全部
     */
    @GetMapping
    public Result<PageResult<Book>> list(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer isFinished) {
        return Result.success(bookService.pageBooks(page, size, keyword, isFinished, UserContext.getUserId()));
    }

    /** 语义搜索（自然语言描述） */
    @GetMapping("/semantic")
    public Result<List<Book>> semantic(@RequestParam String query,
                                       @RequestParam(defaultValue = "10") int limit) {
        return Result.success(bookService.semanticSearch(query, limit, UserContext.getUserId()));
    }

    /**
     * 猜你喜欢
     *
     * <p>limit 传 0 或不传表示返回全部图书；当前用户最近借阅过的图书会排在最前面。</p>
     */
    @GetMapping("/guess-you-like")
    public Result<List<Book>> guessYouLike(@RequestParam(defaultValue = "0") int limit) {
        return Result.success(bookService.guessYouLike(UserContext.getUserId(), limit));
    }

    /**
     * 首页热门搜索关键词
     *
     * <p>优先取当前用户的个人高频搜索词，再用全平台高频词去重补足。</p>
     */
    @GetMapping("/hot-keywords")
    public Result<Map<String, List<String>>> hotKeywords() {
        return Result.success(bookService.hotKeywords(UserContext.getUserId()));
    }

    /** 记录一次有效搜索（供前端在确认搜索时调用） */
    @PostMapping("/search-log")
    public Result<Void> recordSearch(@RequestParam String keyword) {
        bookService.recordSearch(keyword, UserContext.getUserId());
        return Result.success();
    }

    /** 排行榜：hot-热门 / favorite-收藏 / active-用户活跃；dim：book-按书籍 / author-按作者 / platform-按平台 */
    @GetMapping("/ranks")
    public Result<List<Map<String, Object>>> ranks(@RequestParam(defaultValue = "hot") String type,
                                                   @RequestParam(defaultValue = "book") String dim,
                                                   @RequestParam(defaultValue = "10") int limit) {
        return Result.success(bookService.ranks(type, dim, limit));
    }

    /** 图书详情 */
    @GetMapping("/{id}")
    public Result<Book> detail(@PathVariable Long id) {
        return Result.success(bookService.detail(id, UserContext.getUserId()));
    }
}
