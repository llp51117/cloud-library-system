package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.dto.BookSaveDTO;
import com.group1.cloudlibrary.dto.CoverCropDTO;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.entity.Borrow;
import com.group1.cloudlibrary.entity.Comment;
import com.group1.cloudlibrary.entity.Favorite;
import com.group1.cloudlibrary.entity.Reservation;
import com.group1.cloudlibrary.entity.SearchLog;
import com.group1.cloudlibrary.mapper.BookMapper;
import com.group1.cloudlibrary.mapper.BorrowMapper;
import com.group1.cloudlibrary.mapper.CommentMapper;
import com.group1.cloudlibrary.mapper.FavoriteMapper;
import com.group1.cloudlibrary.mapper.ReservationMapper;
import com.group1.cloudlibrary.mapper.SearchLogMapper;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.vo.BatchCoverResultVO;
import com.group1.cloudlibrary.vo.BookImportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图书服务：列表检索、语义搜索、详情、猜你喜欢、热门搜索、排行、管理端维护
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    /** 猜你喜欢：最近借阅过的图书优先展示的最大数量 */
    private static final int PRIORITY_LIMIT = 4;

    /** 热门搜索：取自当前用户的个人高频关键词个数 */
    private static final int PERSONAL_KEYWORD_LIMIT = 3;

    /** 热门搜索：最终展示的关键词总个数 */
    private static final int TOTAL_KEYWORD_LIMIT = 5;

    /** 热门搜索：全平台候选关键词个数（多取一些，用于去重后补足） */
    private static final int GLOBAL_KEYWORD_LIMIT = 10;

    /** 搜索计数：单次搜索最多累加多少本图书的搜索次数 */
    private static final int SEARCH_COUNT_LIMIT = 10;

    private final BookMapper bookMapper;
    private final BorrowMapper borrowMapper;
    private final FavoriteMapper favoriteMapper;
    private final ReservationMapper reservationMapper;
    private final CommentMapper commentMapper;
    private final SearchLogMapper searchLogMapper;
    private final UserMapper userMapper;
    private final FavoriteService favoriteService;
    private final BorrowService borrowService;
    private final AiService aiService;
    private final FileService fileService;
    private final AdminGuard adminGuard;
    private final AdminOperationService adminOperationService;

    // ==========================================================
    // 用户端
    // ==========================================================

    /**
     * 图书列表（关键词搜索 + 完结状态筛选 + 按收藏热度排序 + 分页）
     */
    public PageResult<Book> pageBooks(long page, long size, String keyword, Integer isFinished, Long userId) {
        String text = StringUtils.hasText(keyword) ? keyword.trim() : "";
        // 完结状态：优先取筛选框传来的值，其次识别「连载中 / 已完结」这类关键词
        Integer keywordStatus = parseFinishStatus(text);
        Integer status = isFinished != null ? isFinished : keywordStatus;

        LambdaQueryWrapper<Book> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(Book::getIsFinished, status);
        }
        // 关键词本身就是完结状态时，不再拿它去匹配文本字段
        if (!text.isEmpty() && keywordStatus == null) {
            // 四个文本字段任一命中即可（LIKE %关键词%）
            wrapper.and(w -> w.like(Book::getTitle, text)
                    .or().like(Book::getAuthor, text)
                    .or().like(Book::getPlatform, text)
                    .or().like(Book::getDescription, text));
        }
        wrapper.orderByDesc(Book::getFavoriteCount);

        // 注意：这里不再累加 search_count。
        // 首页已改为「输入即搜索」，若在此计数，用户每敲一个字都会命中并累加，
        // 会把「科」「科幻」「科幻小」等中间输入片段全部统计进去，污染热门排行。
        // 搜索次数统一由 recordSearch() 在用户确认搜索时写入。
        IPage<Book> result = bookMapper.selectPage(new Page<>(page, size), wrapper);
        fillDisplayFields(result.getRecords(), userId);
        return PageResult.of(result);
    }

    /**
     * 把「连载中 / 已完结」这类关键词翻译成完结状态值
     *
     * @return 0-连载中，1-已完结，null-不是完结状态关键词
     */
    private Integer parseFinishStatus(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String text = keyword.trim();
        if ("连载中".equals(text) || "未完结".equals(text)) {
            return 0;
        }
        if ("已完结".equals(text) || "完结".equals(text)) {
            return 1;
        }
        return null;
    }

    /**
     * 语义搜索：自然语言描述 → 语义相关图书
     */
    public List<Book> semanticSearch(String query, int limit, Long userId) {
        List<Book> books = aiService.semanticSearch(query, limit);
        fillDisplayFields(books, userId);
        return books;
    }

    /**
     * 图书详情
     */
    public Book detail(Long id, Long userId) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw BusinessException.of("图书不存在");
        }
        bookMapper.increaseSearchCount(id);
        book.setSearchCount((book.getSearchCount() == null ? 0 : book.getSearchCount()) + 1);
        fillDisplayFields(List.of(book), userId);
        return book;
    }

    /**
     * 猜你喜欢：展示全部图书
     *
     * <p>排序规则：当前用户最近借阅过的图书优先排在最前面（最多 4 本），
     * 其余图书（含从未借阅过的）按收藏热度降序排在后面。</p>
     *
     * @param limit 返回条数，小于等于 0 时返回全部图书
     */
    public List<Book> guessYouLike(Long userId, int limit) {
        // 全部图书，默认按收藏热度降序
        List<Book> all = bookMapper.selectList(Wrappers.<Book>lambdaQuery()
                .orderByDesc(Book::getFavoriteCount)
                .orderByAsc(Book::getId));

        // 当前用户最近借阅过的图书（按借阅时间倒序去重，最多 PRIORITY_LIMIT 本）
        LinkedHashSet<Long> recentBookIds = new LinkedHashSet<>();
        if (userId != null) {
            borrowMapper.selectList(Wrappers.<Borrow>lambdaQuery()
                            .eq(Borrow::getUserId, userId)
                            .orderByDesc(Borrow::getBorrowDate)
                            .orderByDesc(Borrow::getId))
                    .stream()
                    .map(Borrow::getBookId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(PRIORITY_LIMIT)
                    .forEach(recentBookIds::add);
        }

        if (!recentBookIds.isEmpty() && !all.isEmpty()) {
            Map<Long, Book> idToBook = all.stream()
                    .collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a, LinkedHashMap::new));
            List<Book> ordered = new ArrayList<>(all.size());
            // 1) 最近借阅过的图书排在最前面
            recentBookIds.stream().map(idToBook::get).filter(Objects::nonNull).forEach(ordered::add);
            // 2) 其余图书保持原有的收藏热度顺序排在后面
            all.stream().filter(b -> !recentBookIds.contains(b.getId())).forEach(ordered::add);
            all = ordered;
        }

        List<Book> result = (limit > 0 && all.size() > limit)
                ? new ArrayList<>(all.subList(0, limit))
                : all;
        fillDisplayFields(result, userId);
        // 标记「最近借阅优先」的图书，供前端展示标签
        result.forEach(book -> book.setRecentBorrowed(recentBookIds.contains(book.getId())));
        return result;
    }

    /**
     * 记录一次有效搜索
     *
     * <p>由前端在用户「确认搜索」（回车、点击热门词、或停止输入后）调用，
     * 避免「输入即搜索」过程中把「科」「科幻」这类中间输入片段也统计进去。</p>
     *
     * <p>同时累加命中图书的搜索次数，保证「热门排行」的数据来源不受影响。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordSearch(String keyword, Long userId) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        // 与 search_log.keyword VARCHAR(100) 长度对齐（声明为 final 以便在下方 lambda 中引用）
        final String kw = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
        SearchLog searchLog = new SearchLog();
        searchLog.setUserId(userId);
        searchLog.setKeyword(kw);
        searchLog.setCreatedAt(LocalDateTime.now());
        searchLogMapper.insert(searchLog);

        // 累加命中图书的搜索次数（用于热门排行）
        List<Long> hitBookIds = bookMapper.selectList(Wrappers.<Book>lambdaQuery()
                        .select(Book::getId)
                        .and(w -> w.like(Book::getTitle, kw)
                                .or().like(Book::getAuthor, kw)
                                .or().like(Book::getPlatform, kw))
                        .orderByDesc(Book::getFavoriteCount)
                        .last("LIMIT " + SEARCH_COUNT_LIMIT))
                .stream().map(Book::getId).collect(Collectors.toList());
        hitBookIds.forEach(bookMapper::increaseSearchCount);
    }

    /**
     * 首页「热门搜索」关键词
     *
     * <p>规则：</p>
     * <ul>
     *     <li>优先展示当前用户最常搜索的 3 个关键词；</li>
     *     <li>再用全平台搜索最多的关键词去重补足到 5 个；</li>
     *     <li>当前用户没有任何搜索记录时，5 个全部取全平台热门关键词。</li>
     * </ul>
     *
     * @return personal-个人高频词、global-全平台高频词、keywords-最终展示的关键词列表
     */
    public Map<String, List<String>> hotKeywords(Long userId) {
        List<String> personal = userId == null
                ? new ArrayList<>()
                : searchLogMapper.selectUserTopKeywords(userId, PERSONAL_KEYWORD_LIMIT);
        List<String> global = searchLogMapper.selectGlobalTopKeywords(userId == null ? TOTAL_KEYWORD_LIMIT : GLOBAL_KEYWORD_LIMIT);

        // LinkedHashSet 自动去重，同时保留「个人高频词在前」的顺序
        LinkedHashSet<String> merged = new LinkedHashSet<>(personal);
        for (String keyword : global) {
            if (merged.size() >= TOTAL_KEYWORD_LIMIT) {
                break;
            }
            merged.add(keyword);
        }

        Map<String, List<String>> data = new LinkedHashMap<>();
        data.put("personal", personal);
        data.put("global", global);
        data.put("keywords", new ArrayList<>(merged));
        return data;
    }

    /**
     * 三大排行榜
     *
     * @param type hot-热门（搜索次数）/ favorite-收藏人数 / active-用户活跃度
     * @param dim  统计维度：book-按书籍 / author-按作者 / platform-按平台（active 榜单忽略该参数）
     */
    public List<Map<String, Object>> ranks(String type, String dim, int limit) {
        String group = normalizeDim(dim);
        return switch (type == null ? "hot" : type) {
            case "favorite" -> switch (group) {
                case "author" -> bookMapper.selectRankByGroup("author", "favorite_count", limit);
                case "platform" -> bookMapper.selectRankByGroup("platform", "favorite_count", limit);
                default -> bookMapper.selectFavoriteRank(limit);
            };
            case "active" -> userActiveRank(limit);
            default -> switch (group) {
                case "author" -> bookMapper.selectRankByGroup("author", "search_count", limit);
                case "platform" -> bookMapper.selectRankByGroup("platform", "search_count", limit);
                default -> bookMapper.selectSearchRank(limit);
            };
        };
    }

    /**
     * 统计维度白名单，避免把外部字符串直接拼进 SQL
     */
    private String normalizeDim(String dim) {
        if ("author".equalsIgnoreCase(dim)) {
            return "author";
        }
        if ("platform".equalsIgnoreCase(dim)) {
            return "platform";
        }
        return "book";
    }

    /**
     * 用户活跃排行：先取活跃度最高的前 N 位用户，再按注册时间从早到晚排序，
     * 让折线图呈现高低起伏的对比（而不是按活跃度单调递减）
     */
    public List<Map<String, Object>> userActiveRank(int limit) {
        return userMapper.selectActiveUsersRank(limit);
    }

    // ==========================================================
    // 管理端
    // ==========================================================

    /**
     * 管理端图书分页查询（书名/作者/平台）
     */
    public PageResult<Book> adminPage(long page, long size, String keyword) {
        IPage<Book> result = bookMapper.selectPage(new Page<>(page, size), Wrappers.<Book>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(Book::getTitle, keyword)
                        .or().like(Book::getAuthor, keyword)
                        .or().like(Book::getPlatform, keyword))
                .orderByDesc(Book::getCreatedAt));
        return PageResult.of(result);
    }

    /**
     * 新增图书
     */
    @Transactional(rollbackFor = Exception.class)
    public Long save(BookSaveDTO dto) {
        Book book = toEntity(dto);
        book.setBorrowCount(0);
        book.setFavoriteCount(0);
        book.setSearchCount(0);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.insert(book);
        return book.getId();
    }

    /**
     * 编辑图书（同时写入管理员操作记录：编辑图书 1 本）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(BookSaveDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.of("缺少图书ID");
        }
        Book exist = bookMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of("图书不存在");
        }
        Book book = toEntity(dto);
        book.setId(dto.getId());
        // 封面被移除时，之前的裁剪参数也一并失效（置空＝不裁剪）
        if (!StringUtils.hasText(dto.getCoverUrl())) {
            book.setCoverCardCrop("");
            book.setCoverDetailCrop("");
        }
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);

        adminOperationService.record(AdminOperationService.TYPE_BOOK_EDIT, book.getId(),
                "编辑图书《" + (StringUtils.hasText(book.getTitle()) ? book.getTitle().trim() : exist.getTitle()) + "》");
    }

    /**
     * 单独保存封面裁剪参数（首页卡片 / 详情页两套，互不影响）
     *
     * @param dto cover_card_crop、cover_detail_crop，传空字符串表示清空裁剪
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCoverCrop(Long id, CoverCropDTO dto) {
        if (id == null) {
            throw BusinessException.of("缺少图书ID");
        }
        if (bookMapper.selectById(id) == null) {
            throw BusinessException.of("图书不存在");
        }
        bookMapper.update(null, Wrappers.<Book>lambdaUpdate()
                .eq(Book::getId, id)
                .set(Book::getCoverCardCrop, emptyToNull(dto.getCoverCardCrop()))
                .set(Book::getCoverDetailCrop, emptyToNull(dto.getCoverDetailCrop()))
                .set(Book::getUpdatedAt, LocalDateTime.now()));
    }

    /** 空字符串按 null 存（不裁剪） */
    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 删除图书（按顺序清理关联的评论点赞/举报/踩记录、评论、借阅、预约、收藏）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw BusinessException.of("图书不存在");
        }
        cleanBookRelations(List.of(id));
        bookMapper.deleteById(id);
    }

    /**
     * 批量删除
     * <p>属于「需要申请」的高风险操作：最高管理员可直接执行，普通管理员必须提交操作申请。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.of("请先勾选要删除的图书");
        }
        adminGuard.requireSuperAdminOrApply("批量删除图书", "batch_delete_book");
        cleanBookRelations(ids);
        return bookMapper.deleteBatchIds(ids);
    }

    /**
     * 批量导入（书名 + 作者查重，重复的不新增，只补空字段）
     *
     * <p>查重标准为「书名 + 作者」完全相同（比对时去掉空白、忽略大小写）：</p>
     * <ul>
     *   <li>数据库里已存在 → 不新增记录，只把这条记录里<b>本来为空</b>的字段（出版社 / 价格 / 简介 / 平台 / 完结状态）
     *       用 Excel 里的值补进去；库里已有值的字段一律保留，不覆盖。一个字段都没补上的才算 duplicated</li>
     *   <li>同一批数据里重复出现 → 只处理第一条，其余记入 repeatedInBatch</li>
     *   <li>书名或作者为空 → 无效数据，记入 invalid</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public BookImportResultVO batchImport(List<BookSaveDTO> list) {
        if (list == null || list.isEmpty()) {
            throw BusinessException.of("导入数据为空");
        }

        BookImportResultVO result = new BookImportResultVO();

        // 一次性取出已有图书的「书名+作者」建索引，避免每条数据都查一次库
        Map<String, Book> existMap = new LinkedHashMap<>();
        for (Book book : bookMapper.selectList(Wrappers.<Book>lambdaQuery()
                .select(Book::getId, Book::getTitle, Book::getAuthor, Book::getPublisher, Book::getPrice,
                        Book::getDescription, Book::getPlatform, Book::getIsFinished, Book::getStock))) {
            existMap.putIfAbsent(bookKey(book.getTitle(), book.getAuthor()), book);
        }

        // 本批次内已经处理过的组合，用于同一批 Excel 的重复数据去重
        Set<String> batchKeys = new HashSet<>();

        int imported = 0;
        int invalid = 0;
        for (BookSaveDTO dto : list) {
            if (!StringUtils.hasText(dto.getTitle()) || !StringUtils.hasText(dto.getAuthor())) {
                invalid++;
                continue;
            }
            String key = bookKey(dto.getTitle(), dto.getAuthor());
            if (!batchKeys.add(key)) {
                result.getRepeatedInBatch().add(bookLabel(dto.getTitle(), dto.getAuthor()));
                continue;
            }
            String label = bookLabel(dto.getTitle(), dto.getAuthor());

            Book existing = existMap.get(key);
            if (existing != null) {
                // 已存在：不新增，只把空的字段补上
                List<String> filled = fillEmptyFields(existing, dto);
                if (filled.isEmpty()) {
                    result.getDuplicated().add(label);
                    continue;
                }
                Book update = new Book();
                update.setId(existing.getId());
                update.setPublisher(existing.getPublisher());
                update.setPlatform(existing.getPlatform());
                update.setPrice(existing.getPrice());
                update.setDescription(existing.getDescription());
                update.setIsFinished(existing.getIsFinished());
                update.setStock(existing.getStock());
                update.setUpdatedAt(LocalDateTime.now());
                bookMapper.updateById(update);
                result.getUpdated().add(label + "（补充：" + String.join("、", filled) + "）");
                continue;
            }

            Book book = toEntity(dto);
            book.setBorrowCount(0);
            book.setFavoriteCount(0);
            book.setSearchCount(0);
            book.setCreatedAt(LocalDateTime.now());
            book.setUpdatedAt(LocalDateTime.now());
            bookMapper.insert(book);
            imported++;
        }
        result.setImported(imported);
        result.setInvalid(invalid);
        return result;
    }

    /**
     * 用 Excel 里的值补齐已有记录的空字段（publisher / price / description / platform / isFinished）
     *
     * <p>会同步修改内存里的 existing 对象，这样同一批数据里后面再出现同名行时，也能基于最新值继续判断。</p>
     *
     * @return 实际补上的字段中文名，一个都没补返回空列表
     */
    private List<String> fillEmptyFields(Book existing, BookSaveDTO dto) {
        List<String> filled = new ArrayList<>();
        if (!StringUtils.hasText(existing.getPublisher()) && StringUtils.hasText(dto.getPublisher())) {
            existing.setPublisher(dto.getPublisher().trim());
            filled.add("出版社");
        }
        if (!StringUtils.hasText(existing.getPlatform()) && StringUtils.hasText(dto.getPlatform())) {
            existing.setPlatform(dto.getPlatform().trim());
            filled.add("平台");
        }
        if (existing.getPrice() == null && dto.getPrice() != null) {
            existing.setPrice(dto.getPrice());
            filled.add("价格");
        }
        if (!StringUtils.hasText(existing.getDescription()) && StringUtils.hasText(dto.getDescription())) {
            existing.setDescription(dto.getDescription().trim());
            filled.add("简介");
        }
        // 完结状态在库里默认是 0，只有真正为 null 才算空
        if (existing.getIsFinished() == null && dto.getIsFinished() != null) {
            existing.setIsFinished(dto.getIsFinished());
            filled.add("完结状态");
        }
        // 库存：老数据（加字段前录入的书）可能是 null，导入时给补上
        if (existing.getStock() == null) {
            existing.setStock(dto.getStock() == null ? BookSaveDTO.DEFAULT_STOCK : Math.max(dto.getStock(), 0));
            filled.add("库存");
        }
        return filled;
    }

    /**
     * 批量上传封面：按「图片文件名（不含扩展名）= 书名」自动匹配并写入 coverUrl
     *
     * <p>比对时会去掉空格、忽略大小写，所以「三体 .jpg」「三体.jpg」都能匹配到《三体》；
     * 同名图书只更新先录入的那一本。单张图片的大小与格式限制沿用 {@link FileService#uploadImage}（10MB、jpg/png/gif/webp/bmp）。</p>
     *
     * @param files 多张图片
     * @return 匹配成功 / 未匹配 / 上传失败三个清单
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchCoverResultVO batchUploadCovers(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw BusinessException.of("请先选择要上传的图片");
        }

        BatchCoverResultVO result = new BatchCoverResultVO();
        result.setTotal(files.size());

        // 一次性取出书名建索引，避免每张图片都查一次库；同名图书保留先录入的那本
        Map<String, Book> titleIndex = new LinkedHashMap<>();
        for (Book book : bookMapper.selectList(Wrappers.<Book>lambdaQuery().select(Book::getId, Book::getTitle))) {
            String key = normalizeTitle(book.getTitle());
            if (!key.isEmpty()) {
                titleIndex.putIfAbsent(key, book);
            }
        }

        for (MultipartFile file : files) {
            String baseName = imageBaseName(file.getOriginalFilename());
            Book target = titleIndex.get(normalizeTitle(baseName));
            if (target == null) {
                result.getUnmatched().add(baseName);
                continue;
            }
            try {
                String url = fileService.uploadImage(file, "cover");
                Book update = new Book();
                update.setId(target.getId());
                update.setCoverUrl(url);
                update.setUpdatedAt(LocalDateTime.now());
                bookMapper.updateById(update);
                result.getMatched().add(target.getTitle());
            } catch (Exception e) {
                log.warn("批量上传封面失败，文件：{}，原因：{}", baseName, e.getMessage());
                result.getFailed().add(baseName + "（" + e.getMessage() + "）");
            }
        }
        return result;
    }

    /**
     * AI 辅助编目：自动生成图书摘要
     */
    public String aiSummary(String title, String author, String description) {
        if (!StringUtils.hasText(title)) {
            throw BusinessException.of("请输入书名后再生成摘要");
        }
        return aiService.generateBookSummary(title, author == null ? "佚名" : author, description);
    }

    // ==========================================================
    // 内部方法
    // ==========================================================

    /**
     * 填充展示字段：是否可借阅、是否已收藏、当前用户是否借阅中/逾期、收藏热度星级
     */
    public void fillDisplayFields(List<Book> books, Long userId) {
        if (books == null || books.isEmpty()) {
            return;
        }
        List<Long> bookIds = books.stream().map(Book::getId).collect(Collectors.toList());
        // 收藏状态
        Set<Long> favoritedIds = new LinkedHashSet<>();
        // 当前用户对这些书的「未归还」借阅记录（借阅中 0 / 逾期 2），用于前端显示「去归还」
        Map<Long, Borrow> myBorrowMap = new LinkedHashMap<>();
        if (userId != null) {
            List<Favorite> favorites = favoriteMapper.selectList(Wrappers.<Favorite>lambdaQuery()
                    .eq(Favorite::getUserId, userId)
                    .in(Favorite::getBookId, bookIds));
            favoritedIds = favorites.stream().map(Favorite::getBookId).collect(Collectors.toSet());

            List<Borrow> myBorrows = borrowMapper.selectList(Wrappers.<Borrow>lambdaQuery()
                    .eq(Borrow::getUserId, userId)
                    .in(Borrow::getBookId, bookIds)
                    .in(Borrow::getStatus, 0, 2));
            for (Borrow borrow : myBorrows) {
                // 同一本书理论上只有一条未归还记录，取最早的那条即可
                myBorrowMap.putIfAbsent(borrow.getBookId(), borrow);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (Book book : books) {
            book.setFavorited(favoritedIds.contains(book.getId()));
            /* 可借阅 = 还有库存（一本书可以有多本馆藏，借出几本不影响剩下的能不能借）；
               库存为 0 时前端显示「暂无库存」，只能预约。 */
            book.setAvailable(book.getStock() != null && book.getStock() > 0);
            // 当前用户是否已借且未归还（逾期也算），前端据此把按钮切成「去归还」
            Borrow mine = myBorrowMap.get(book.getId());
            book.setBorrowedByMe(mine != null);
            if (mine != null) {
                book.setMyBorrowId(mine.getId());
                // 已标记为逾期，或者到了应还时间还没还（定时任务还没刷新）都算逾期
                book.setMyBorrowOverdue(mine.getStatus() != null && mine.getStatus() == 2
                        || (mine.getDueDate() != null && mine.getDueDate().isBefore(now)));
            }
            book.setHotStars(calcHotStars(book.getFavoriteCount()));
        }
    }

    /**
     * 计算收藏热度星级（0~5）
     */
    private int calcHotStars(Integer favoriteCount) {
        int count = favoriteCount == null ? 0 : favoriteCount;
        if (count <= 0) {
            return 0;
        }
        if (count < 10) {
            return 1;
        }
        if (count < 50) {
            return 2;
        }
        if (count < 200) {
            return 3;
        }
        if (count < 500) {
            return 4;
        }
        return 5;
    }

    /**
     * 清理图书关联数据，避免外键约束导致删除失败。
     *
     * <p>必须按「外键依赖的末端 → 源头」的顺序删：
     * 评论的点赞 / 举报 / 踩记录 → 评论 → 借阅 → 预约 → 收藏。
     * 老库里 comment_like / comment_report / comment_dislike 的外键可能没有 ON DELETE CASCADE，
     * 所以这里显式先把这三张表里的记录删掉，再删评论，最后图书本身由调用方删除。</p>
     */
    private void cleanBookRelations(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return;
        }
        // 1. 评论的点赞记录（依赖 comment，必须最先删）
        commentMapper.deleteLikeByBookIds(bookIds);
        // 2. 评论的举报记录
        commentMapper.deleteReportByBookIds(bookIds);
        // 3. 评论的踩记录
        commentMapper.deleteDislikeByBookIds(bookIds);
        // 4. 评论
        commentMapper.delete(Wrappers.<Comment>lambdaQuery().in(Comment::getBookId, bookIds));
        // 5. 借阅记录
        borrowMapper.delete(Wrappers.<Borrow>lambdaQuery().in(Borrow::getBookId, bookIds));
        // 6. 预约记录
        reservationMapper.delete(Wrappers.<Reservation>lambdaQuery().in(Reservation::getBookId, bookIds));
        // 7. 收藏
        favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery().in(Favorite::getBookId, bookIds));
        // 8. 图书本身由调用方删除（deleteById / deleteBatchIds）
    }

    /**
     * 「书名 + 作者」查重键：去掉空白并忽略大小写，保证只有内容完全相同才算重复
     */
    private String bookKey(String title, String author) {
        return normalizeTitle(title) + "#" + normalizeTitle(author);
    }

    /**
     * 导入结果里展示用的图书标识：《书名》- 作者
     */
    private String bookLabel(String title, String author) {
        return "《" + (title == null ? "" : title.trim()) + "》- " + (author == null ? "" : author.trim());
    }

    /**
     * 书名归一化：去掉所有空白字符并转小写，用于图片名与书名的比对
     */
    private String normalizeTitle(String title) {
        return title == null ? "" : title.replaceAll("\\s+", "").toLowerCase();
    }

    /**
     * 取图片文件名（去掉可能带的路径与扩展名）：三体.jpg -> 三体
     */
    private String imageBaseName(String originalFilename) {
        String name = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int sep = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (sep >= 0) {
            name = name.substring(sep + 1);
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private Book toEntity(BookSaveDTO dto) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPublisher(dto.getPublisher());
        book.setPlatform(dto.getPlatform());
        book.setPrice(dto.getPrice());
        book.setPublishDate(dto.getPublishDate());
        book.setCoverUrl(dto.getCoverUrl());
        book.setCoverCardCrop(dto.getCoverCardCrop());
        book.setCoverDetailCrop(dto.getCoverDetailCrop());
        book.setDescription(dto.getDescription());
        book.setIsFinished(dto.getIsFinished() == null ? 0 : dto.getIsFinished());
        // 库存：没填就用默认值 10，且不允许负数
        int stock = dto.getStock() == null ? BookSaveDTO.DEFAULT_STOCK : dto.getStock();
        book.setStock(Math.max(stock, 0));
        return book;
    }
}
