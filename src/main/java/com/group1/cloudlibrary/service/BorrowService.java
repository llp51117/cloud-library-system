package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.config.CloudLibraryProperties;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.entity.Borrow;
import com.group1.cloudlibrary.entity.Favorite;
import com.group1.cloudlibrary.entity.Reservation;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.mapper.BookMapper;
import com.group1.cloudlibrary.mapper.BorrowMapper;
import com.group1.cloudlibrary.mapper.FavoriteMapper;
import com.group1.cloudlibrary.mapper.ReservationMapper;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.vo.MyBookCountsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 借阅服务：借阅申请、归还、续借、预约排队、催还
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 一键催还的统计范围：应还时间在 3 天内（含已逾期）的借阅记录 */
    private static final int REMIND_BEFORE_DAYS = 3;

    private final BorrowMapper borrowMapper;
    private final BookMapper bookMapper;
    private final FavoriteMapper favoriteMapper;
    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final UserAdminService userAdminService;
    private final CloudLibraryProperties properties;

    // ==========================================================
    // 用户端
    // ==========================================================

    /**
     * 提交借阅申请
     * <p>无人借阅 → 直接借阅成功；已被借出或已被他人预约到号 → 自动进入预约队列</p>
     *
     * @return 提示信息
     */
    @Transactional(rollbackFor = Exception.class)
    public String borrow(Long userId, Long bookId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 2) {
            throw BusinessException.of("您的账号已被禁用，暂时无法借阅图书");
        }
        requireBorrowable(user);
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw BusinessException.of("图书不存在");
        }

        // 是否已借阅且未归还
        Long borrowing = borrowMapper.selectCount(Wrappers.<Borrow>lambdaQuery()
                .eq(Borrow::getUserId, userId)
                .eq(Borrow::getBookId, bookId)
                .in(Borrow::getStatus, 0, 2));
        if (borrowing != null && borrowing > 0) {
            throw BusinessException.of("您已借阅该图书且尚未归还");
        }

        // 是否存在“可借阅”状态的预约（拥有优先借阅权）
        Reservation ready = findReadyReservation(bookId);
        // 库存：这才是“还能不能借”的唯一依据（一本书可以有多本馆藏）
        int stock = stockOf(book);

        // 只剩最后一本、且已经轮到别人借阅时，才需要让别人先借（保护已到号读者的优先权）
        if (ready != null && !ready.getUserId().equals(userId) && stock <= 1) {
            Reservation queue = joinQueue(userId, bookId);
            return "该图书已被其他读者预约，已为您加入预约队列，当前排在第 " + queue.getQueuePosition() + " 位";
        }
        if (stock <= 0) {
            throw BusinessException.of("该图书暂无库存，请预约");
        }

        // 有库存 → 可以借阅
        Borrow borrow = createBorrowRecord(userId, bookId);
        // 若本人正处于可借阅预约状态，标记为已借阅
        if (ready != null && ready.getUserId().equals(userId)) {
            ready.setStatus(2);
            reservationMapper.updateById(ready);
        }
        return "借阅成功！请在 " + DATE_FORMATTER.format(borrow.getDueDate()) + " 前归还";
    }

    /**
     * 加入预约排队队列
     */
    @Transactional(rollbackFor = Exception.class)
    public Reservation joinQueue(Long userId, Long bookId) {
        Long exists = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getBookId, bookId)
                .in(Reservation::getStatus, 0, 1));
        if (exists != null && exists > 0) {
            throw BusinessException.of("您已预约该图书，请勿重复预约");
        }
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setBookId(bookId);
        reservation.setReservationTime(LocalDateTime.now());
        reservation.setStatus(0);
        reservation.setCreatedAt(LocalDateTime.now());
        reservationMapper.insert(reservation);

        Book book = bookMapper.selectById(bookId);
        String bookTitle = book == null ? "该图书" : "《" + book.getTitle() + "》";
        notificationService.send(userId, NotificationService.TYPE_RESERVATION_READY,
                "预约成功", "您已成功预约 " + bookTitle + "，图书归还后系统会第一时间通知您。");

        reservation.setQueuePosition(queuePosition(bookId, userId));
        return reservation;
    }

    /**
     * 创建借阅记录（供预约“去借阅”复用）
     */
    @Transactional(rollbackFor = Exception.class)
    public Borrow createBorrowRecord(Long userId, Long bookId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        requireBorrowable(user);
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw BusinessException.of("图书不存在");
        }
        if (stockOf(book) <= 0) {
            throw BusinessException.of("该图书暂无库存，请预约");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.plusDays(properties.getBorrow().getDays());

        Borrow borrow = new Borrow();
        borrow.setUserId(userId);
        borrow.setBookId(bookId);
        borrow.setBorrowDate(now);
        borrow.setDueDate(due);
        borrow.setStatus(0);
        borrow.setRenewCount(0);
        borrow.setCreatedAt(now);
        borrowMapper.insert(borrow);

        // 借阅成功：库存 -1（条件更新，并发下不会超卖），借阅次数 +1
        if (bookMapper.decreaseStockIfAvailable(bookId) <= 0) {
            throw BusinessException.of("该图书暂无库存，请预约");
        }
        bookMapper.increaseBorrowCount(bookId);
        notificationService.send(userId, NotificationService.TYPE_BORROW_SUCCESS,
                "借阅成功",
                "您已成功借阅《" + book.getTitle() + "》，应归还时间为 "
                        + DATE_FORMATTER.format(due) + "，请按时归还。");
        return borrow;
    }

    /**
     * 我的借阅记录（分页）
     */
    public PageResult<Borrow> myBorrows(Long userId, long page, long size, Integer status) {
        refreshOverdue();
        IPage<Borrow> result = borrowMapper.selectBorrowPage(new Page<>(page, size), userId, status, null, null);
        return PageResult.of(result);
    }

    /**
     * 「我的书籍」四个子 Tab 的数量：全部用 COUNT 实时统计，不受分页大小影响。
     * 借阅状态：0借阅中 / 1已归还 / 2已逾期
     */
    public MyBookCountsVO myBookCounts(Long userId) {
        refreshOverdue();
        long current = borrowMapper.selectCount(Wrappers.<Borrow>lambdaQuery()
                .eq(Borrow::getUserId, userId)
                .in(Borrow::getStatus, 0, 2));
        long history = borrowMapper.selectCount(Wrappers.<Borrow>lambdaQuery()
                .eq(Borrow::getUserId, userId)
                .eq(Borrow::getStatus, 1));
        long favorite = favoriteMapper.selectCount(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId));
        // 与「我的预约」列表保持一致：所有状态的预约都算，包括已取消、已借阅的
        long reservation = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getUserId, userId));
        return new MyBookCountsVO(current, history, favorite, reservation);
    }

    /**
     * 归还图书（提前归还）
     */
    @Transactional(rollbackFor = Exception.class)
    public void returnBook(Long userId, Long borrowId) {
        Borrow borrow = borrowMapper.selectById(borrowId);
        if (borrow == null || !borrow.getUserId().equals(userId)) {
            throw BusinessException.of("借阅记录不存在");
        }
        if (borrow.getStatus() == 1) {
            throw BusinessException.of("该图书已归还");
        }
        doReturn(borrow);
    }

    /**
     * 续借（延长 15 天，需在应还日前 3 天内且无排队）
     */
    @Transactional(rollbackFor = Exception.class)
    public String renew(Long userId, Long borrowId) {
        Borrow borrow = borrowMapper.selectById(borrowId);
        if (borrow == null || !borrow.getUserId().equals(userId)) {
            throw BusinessException.of("借阅记录不存在");
        }
        if (borrow.getStatus() == 1) {
            throw BusinessException.of("该图书已归还，无需续借");
        }
        int limit = properties.getBorrow().getRenewLimit();
        if (borrow.getRenewCount() != null && borrow.getRenewCount() >= limit) {
            throw BusinessException.of("每本图书最多续借 " + limit + " 次");
        }
        LocalDateTime now = LocalDateTime.now();
        long daysToDue = java.time.Duration.between(now, borrow.getDueDate()).toDays();
        if (daysToDue > properties.getBorrow().getRenewBeforeDays()) {
            throw BusinessException.of("距离应还日期还有 " + daysToDue + " 天，请在应还日前 "
                    + properties.getBorrow().getRenewBeforeDays() + " 天内再续借");
        }
        Long queueCount = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getBookId, borrow.getBookId())
                .eq(Reservation::getStatus, 0));
        if (queueCount != null && queueCount > 0) {
            throw BusinessException.of("该图书已有读者排队预约，无法续借");
        }

        LocalDateTime newDue = borrow.getDueDate().plusDays(properties.getBorrow().getRenewDays());
        borrow.setDueDate(newDue);
        borrow.setRenewCount((borrow.getRenewCount() == null ? 0 : borrow.getRenewCount()) + 1);
        borrow.setStatus(0);
        borrowMapper.updateById(borrow);

        Book book = bookMapper.selectById(borrow.getBookId());
        notificationService.send(userId, NotificationService.TYPE_BORROW_SUCCESS,
                "续借成功",
                "《" + (book == null ? "" : book.getTitle()) + "》续借成功，新的应还时间为 "
                        + DATE_FORMATTER.format(newDue) + "。");
        return "续借成功，新的应还时间为 " + DATE_FORMATTER.format(newDue);
    }

    // ==========================================================
    // 管理端
    // ==========================================================

    /**
     * 管理端：借阅记录分页查询
     *
     * @param searchType user-按用户 / book-按图书 / platform-按平台
     */
    public PageResult<Borrow> adminPage(long page, long size, Integer status, String searchType, String keyword) {
        refreshOverdue();
        IPage<Borrow> result = borrowMapper.selectBorrowPage(new Page<>(page, size), null, status, searchType, keyword);
        return PageResult.of(result);
    }

    /**
     * 管理端：手动催还
     */
    @Transactional(rollbackFor = Exception.class)
    public void remind(Long borrowId) {
        Borrow borrow = borrowMapper.selectById(borrowId);
        if (borrow == null) {
            throw BusinessException.of("借阅记录不存在");
        }
        if (borrow.getStatus() == 1) {
            throw BusinessException.of("该记录已归还，无需催还");
        }
        Book book = bookMapper.selectById(borrow.getBookId());
        notificationService.send(borrow.getUserId(), NotificationService.TYPE_REMIND_RETURN,
                "图书催还提醒",
                "管理员提醒您：《" + (book == null ? "" : book.getTitle()) + "》应于 "
                        + DATE_FORMATTER.format(borrow.getDueDate()) + " 前归还，请及时归还。");
    }

    /**
     * 管理端：一键催还
     * <p>给所有「已逾期」或「即将逾期（3 天内）」的借阅记录批量发送催还通知，返回发送条数。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public int remindAll() {
        refreshOverdue();
        List<Borrow> list = borrowMapper.selectDueSoon(REMIND_BEFORE_DAYS);
        int count = 0;
        for (Borrow borrow : list) {
            String title = borrow.getBookTitle() == null ? "图书" : ("《" + borrow.getBookTitle() + "》");
            boolean overdue = borrow.getStatus() != null && borrow.getStatus() == 2;
            notificationService.send(borrow.getUserId(),
                    overdue ? NotificationService.TYPE_OVERDUE : NotificationService.TYPE_REMIND_RETURN,
                    overdue ? "图书逾期催还提醒" : "图书即将到期提醒",
                    "管理员提醒您：您借阅的" + title + "应归还时间为 " + DATE_FORMATTER.format(borrow.getDueDate())
                            + (overdue ? "，已逾期，请尽快归还。" : "，即将到期，请及时归还或办理续借。"));
            count++;
        }
        log.info("管理员一键催还完成，共发送 {} 条通知", count);
        return count;
    }

    /**
     * 定时任务：自动催还（即将逾期 / 已逾期）
     */
    @Transactional(rollbackFor = Exception.class)
    public int autoRemind(int beforeDays) {
        refreshOverdue();
        List<Borrow> list = borrowMapper.selectDueSoon(beforeDays);
        int count = 0;
        for (Borrow borrow : list) {
            String title = borrow.getBookTitle() == null ? "图书" : ("《" + borrow.getBookTitle() + "》");
            boolean overdue = borrow.getStatus() != null && borrow.getStatus() == 2;
            notificationService.send(borrow.getUserId(),
                    overdue ? NotificationService.TYPE_OVERDUE : NotificationService.TYPE_REMIND_RETURN,
                    overdue ? "图书逾期提醒" : "图书即将到期提醒",
                    "您借阅的" + title + "应归还时间为 " + DATE_FORMATTER.format(borrow.getDueDate())
                            + (overdue ? "，已逾期，请尽快归还。" : "，请及时归还或办理续借。"));
            count++;
        }
        return count;
    }

    // ==========================================================
    // 内部方法
    // ==========================================================

    /**
     * 执行归还：更新记录 + 通知 + 唤醒下一位预约者
     * <p>「强制归还」已下线：已归还状态只由用户在用户端自己归还时触发。</p>
     */
    private void doReturn(Borrow borrow) {
        borrow.setStatus(1);
        borrow.setReturnDate(LocalDateTime.now());
        // 归还方式固定为 0（用户自己归还）；1 只存在于历史数据里
        borrow.setReturnType(0);
        borrowMapper.updateById(borrow);

        // 归还成功：库存 +1（不做上限限制，只要归还就回补一本）
        bookMapper.increaseStock(borrow.getBookId());

        Book book = bookMapper.selectById(borrow.getBookId());
        String title = book == null ? "图书" : "《" + book.getTitle() + "》";
        notificationService.send(borrow.getUserId(), NotificationService.TYPE_RETURN_SUCCESS,
                "归还成功", "您已成功归还" + title + "，感谢使用云图书管理系统。");

        // 唤醒排队中的下一位预约者
        Reservation next = findWaitingReservation(borrow.getBookId());
        if (next != null) {
            next.setStatus(1);
            next.setAvailableTime(LocalDateTime.now());
            reservationMapper.updateById(next);
            int keepHours = properties.getReservation().getKeepHours();
            notificationService.send(next.getUserId(), NotificationService.TYPE_RESERVATION_READY,
                    "预约图书可借阅啦",
                    "您预约的" + title + "已可借阅，请在 " + keepHours + " 小时内点击“去借阅”完成借阅，超时将自动顺延给下一位读者。");
        }
        // 待激活管理员：还清全部图书后自动转正为正式管理员
        userAdminService.promoteIfBooksReturned(borrow.getUserId());
        log.info("图书归还完成：borrowId={}", borrow.getId());
    }

    /**
     * 借阅资格校验：正式管理员与待激活管理员都不能借书（待激活管理员只能还书）
     */
    private void requireBorrowable(User user) {
        if (user.getRole() == null || user.getRole() != 1) {
            return;
        }
        if (user.getPendingAdmin() != null && user.getPendingAdmin() == 1) {
            throw BusinessException.of("您已被设为管理员，需在 10 天内归还全部图书才能行使管理员权限，暂时不能借阅");
        }
        throw BusinessException.of("管理员账号不能借阅图书，请前往管理端操作");
    }

    /**
     * 刷新逾期状态：把超过应还时间且仍为“借阅中”的记录标记为“逾期”
     */
    public int refreshOverdue() {
        return borrowMapper.update(null, Wrappers.<Borrow>lambdaUpdate()
                .set(Borrow::getStatus, 2)
                .eq(Borrow::getStatus, 0)
                .lt(Borrow::getDueDate, LocalDateTime.now()));
    }

    /**
     * 图书的库存余量（null 视为 0，绝不会出现负数）
     */
    private int stockOf(Book book) {
        if (book == null || book.getStock() == null) {
            return 0;
        }
        return Math.max(book.getStock(), 0);
    }

    /**
     * 这本书还有没有库存（> 0 就能借）
     */
    public boolean hasStock(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        return book != null && stockOf(book) > 0;
    }

    /**
     * 图书是否被借出（存在借阅中/逾期记录）
     */
    public boolean isBookBorrowed(Long bookId) {
        Long count = borrowMapper.selectCount(Wrappers.<Borrow>lambdaQuery()
                .eq(Borrow::getBookId, bookId)
                .in(Borrow::getStatus, 0, 2));
        return count != null && count > 0;
    }

    /**
     * 查询某图书“可借阅”状态的预约（队列第一位）
     */
    public Reservation findReadyReservation(Long bookId) {
        return reservationMapper.selectOne(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getBookId, bookId)
                .eq(Reservation::getStatus, 1)
                .orderByAsc(Reservation::getReservationTime)
                .last("LIMIT 1"));
    }

    /**
     * 查询某图书排队中的第一位预约者
     */
    public Reservation findWaitingReservation(Long bookId) {
        return reservationMapper.selectOne(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getBookId, bookId)
                .eq(Reservation::getStatus, 0)
                .orderByAsc(Reservation::getReservationTime)
                .last("LIMIT 1"));
    }

    /**
     * 计算某用户在某图书预约队列中的位次
     */
    private int queuePosition(Long bookId, Long userId) {
        List<Reservation> queue = reservationMapper.selectQueue(bookId);
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getUserId().equals(userId)) {
                return i + 1;
            }
        }
        return queue.size();
    }
}
