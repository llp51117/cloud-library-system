package com.group1.cloudlibrary.schedule;

import com.group1.cloudlibrary.mapper.VerificationCodeMapper;
import com.group1.cloudlibrary.service.BorrowService;
import com.group1.cloudlibrary.service.ReservationService;
import com.group1.cloudlibrary.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：
 * <ol>
 *   <li>自动催还：每天 09:00 扫描即将逾期 / 已逾期的借阅记录，发送提醒通知</li>
 *   <li>预约超时：每 10 分钟检查“可借阅”状态超时的预约，自动顺延给下一位</li>
 *   <li>验证码清理：每天 02:00 清理已过期的验证码</li>
 *   <li>待激活管理员超期检查：每天 03:00 检查 pending_admin=1 的用户是否超过截止时间</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTask {

    private final BorrowService borrowService;
    private final ReservationService reservationService;
    private final VerificationCodeMapper verificationCodeMapper;
    private final UserAdminService userAdminService;

    /**
     * 自动催还 + 逾期标记（每天 09:00）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void autoRemind() {
        try {
            int count = borrowService.autoRemind(3);
            log.info("【定时任务】自动催还完成，共发送 {} 条提醒", count);
        } catch (Exception e) {
            log.error("【定时任务】自动催还失败", e);
        }
    }

    /**
     * 预约超时顺延（每 10 分钟）
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void reservationTimeout() {
        try {
            int count = reservationService.handleTimeout();
            if (count > 0) {
                log.info("【定时任务】预约超时顺延完成，共处理 {} 条", count);
            }
        } catch (Exception e) {
            log.error("【定时任务】预约超时处理失败", e);
        }
    }

    /**
     * 待激活管理员超期检查（每天 03:00）：
     * 10 天内没还清全部图书 → 转换失败，恢复为普通用户，并通知本人与最高管理员
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void checkPendingAdmin() {
        try {
            int count = userAdminService.scanExpiredPendingAdmins();
            if (count > 0) {
                log.info("【定时任务】待激活管理员超期检查完成，共 {} 人转换失败", count);
            }
        } catch (Exception e) {
            log.error("【定时任务】待激活管理员超期检查失败", e);
        }
    }

    /**
     * 验证码清理（每天 02:00）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanVerificationCode() {
        try {
            verificationCodeMapper.cleanExpired();
            log.info("【定时任务】过期验证码清理完成");
        } catch (Exception e) {
            log.error("【定时任务】验证码清理失败", e);
        }
    }
}
