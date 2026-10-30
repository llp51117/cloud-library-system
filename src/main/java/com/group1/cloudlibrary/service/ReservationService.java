package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.config.CloudLibraryProperties;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.entity.Reservation;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.mapper.BookMapper;
import com.group1.cloudlibrary.mapper.ReservationMapper;
import com.group1.cloudlibrary.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约服务：预约排队、取消预约、去借阅、超时顺延
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final BorrowService borrowService;
    private final NotificationService notificationService;
    private final CloudLibraryProperties properties;

    /**
     * 提交预约（图书已被借出时进入排队队列）
     * <p>管理员（含待激活管理员）不能借书，因此也不能预约。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Reservation reserve(Long userId, Long bookId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getRole() != null && user.getRole() == 1) {
            throw BusinessException.of("管理员账号不能预约借阅图书");
        }
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw BusinessException.of("图书不存在");
        }
        // 有库存就能直接借，不需要预约；只有库存为 0（都被借走）时才进队列
        if (borrowService.hasStock(bookId)) {
            throw BusinessException.of("该图书当前有库存，可直接借阅，无需预约");
        }
        return borrowService.joinQueue(userId, bookId);
    }

    /**
     * 我的预约列表
     */
    public List<Reservation> myReservations(Long userId) {
        List<Reservation> list = reservationMapper.selectMyReservations(userId);
        int keepHours = properties.getReservation().getKeepHours();
        for (Reservation reservation : list) {
            if (reservation.getStatus() == 0) {
                reservation.setQueuePosition(queuePosition(reservation.getBookId(), userId));
            }
            if (reservation.getStatus() == 1 && reservation.getAvailableTime() != null) {
                long passed = Duration.between(reservation.getAvailableTime(), LocalDateTime.now()).toHours();
                reservation.setRemainHours(Math.max(0, keepHours - passed));
            }
        }
        return list;
    }

    /**
     * 取消预约
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null || !reservation.getUserId().equals(userId)) {
            throw BusinessException.of("预约记录不存在");
        }
        if (reservation.getStatus() == 2) {
            throw BusinessException.of("该预约已完成借阅，无法取消");
        }
        if (reservation.getStatus() == 3 || reservation.getStatus() == 4) {
            throw BusinessException.of("该预约已取消");
        }
        boolean wasReady = reservation.getStatus() == 1;
        reservation.setStatus(3);
        reservationMapper.updateById(reservation);

        // 若取消的是“可借阅”状态，则顺延给下一位预约者
        if (wasReady) {
            passToNext(reservation.getBookId());
        }
    }

    /**
     * 去借阅（可借阅状态下 24 小时内完成借阅）
     */
    @Transactional(rollbackFor = Exception.class)
    public String borrowByReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null || !reservation.getUserId().equals(userId)) {
            throw BusinessException.of("预约记录不存在");
        }
        if (reservation.getStatus() != 1) {
            throw BusinessException.of("当前预约状态不可借阅，请等待图书归还通知");
        }
        if (reservation.getAvailableTime() != null) {
            long hours = Duration.between(reservation.getAvailableTime(), LocalDateTime.now()).toHours();
            if (hours >= properties.getReservation().getKeepHours()) {
                throw BusinessException.of("预约已超时，请重新预约");
            }
        }
        borrowService.createBorrowRecord(userId, reservation.getBookId());
        reservation.setStatus(2);
        reservationMapper.updateById(reservation);
        return "借阅成功！请按时归还图书";
    }

    /**
     * 定时任务：处理超时未借阅的预约，自动顺延给下一位
     */
    @Transactional(rollbackFor = Exception.class)
    public int handleTimeout() {
        int keepHours = properties.getReservation().getKeepHours();
        List<Reservation> timeouts = reservationMapper.selectTimeoutReservations(keepHours);
        for (Reservation reservation : timeouts) {
            reservation.setStatus(4);
            reservationMapper.updateById(reservation);

            Book book = bookMapper.selectById(reservation.getBookId());
            String title = book == null ? "图书" : "《" + book.getTitle() + "》";
            notificationService.send(reservation.getUserId(), NotificationService.TYPE_RESERVATION_TIMEOUT,
                    "预约超时取消",
                    "您预约的" + title + "超过 " + keepHours + " 小时未借阅，预约已自动取消。");
            passToNext(reservation.getBookId());
        }
        if (!timeouts.isEmpty()) {
            log.info("处理超时预约 {} 条", timeouts.size());
        }
        return timeouts.size();
    }

    /**
     * 将优先借阅权顺延给下一位预约者
     */
    private void passToNext(Long bookId) {
        Reservation next = borrowService.findWaitingReservation(bookId);
        if (next == null) {
            return;
        }
        next.setStatus(1);
        next.setAvailableTime(LocalDateTime.now());
        reservationMapper.updateById(next);

        Book book = bookMapper.selectById(bookId);
        String title = book == null ? "图书" : "《" + book.getTitle() + "》";
        notificationService.send(next.getUserId(), NotificationService.TYPE_RESERVATION_READY,
                "预约图书可借阅啦",
                "您预约的" + title + "已可借阅，请在 " + properties.getReservation().getKeepHours()
                        + " 小时内点击“去借阅”完成借阅，超时将自动顺延给下一位读者。");
    }

    /**
     * 计算排队位次
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

    /**
     * 某用户对某图书是否存在有效预约
     */
    public boolean hasActiveReservation(Long userId, Long bookId) {
        Long count = reservationMapper.selectCount(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getBookId, bookId)
                .in(Reservation::getStatus, 0, 1));
        return count != null && count > 0;
    }
}
