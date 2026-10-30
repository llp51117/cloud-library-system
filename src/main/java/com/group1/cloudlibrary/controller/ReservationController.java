package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.BookIdDTO;
import com.group1.cloudlibrary.entity.Reservation;
import com.group1.cloudlibrary.service.ReservationService;
import com.group1.cloudlibrary.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 预约接口（用户端）
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** 提交预约（图书被借出时进入排队队列） */
    @PostMapping
    public Result<Reservation> reserve(@Valid @RequestBody BookIdDTO dto) {
        return Result.success("预约成功，请留意通知", reservationService.reserve(UserContext.requireUserId(), dto.getBookId()));
    }

    /** 我的预约列表 */
    @GetMapping("/my")
    public Result<List<Reservation>> my() {
        return Result.success(reservationService.myReservations(UserContext.requireUserId()));
    }

    /** 取消预约 */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(UserContext.requireUserId(), id);
        return Result.success("已取消预约", null);
    }

    /** 去借阅（可借阅状态，需在 24 小时内完成） */
    @PutMapping("/{id}/borrow")
    public Result<String> borrow(@PathVariable Long id) {
        String message = reservationService.borrowByReservation(UserContext.requireUserId(), id);
        return Result.success(message, message);
    }
}
