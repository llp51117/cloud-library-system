package com.group1.cloudlibrary.service;

import com.group1.cloudlibrary.config.CloudLibraryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务：发送验证码 / 通知邮件
 * <p>当 cloud-library.email.enabled=false 时，验证码只打印到控制台，方便本地开发。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final CloudLibraryProperties properties;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送验证码邮件
     *
     * @param to   收件人邮箱
     * @param code 验证码
     * @param type 类型：0注册/1找回密码/2修改邮箱
     */
    public void sendVerificationCode(String to, String code, int type) {
        String title;
        if (type == 0) {
            title = "注册验证码";
        } else if (type == 2) {
            title = "修改邮箱验证码";
        } else {
            title = "找回密码验证码";
        }
        String content = String.format("""
                尊敬的用户：

                您正在进行【%s】操作，验证码为：%s

                验证码 5 分钟内有效，请勿泄露给他人。
                如非本人操作，请忽略本邮件。

                —— 云图书管理系统
                """, title, code);
        send(to, "【云图书管理系统】" + title, content);
    }

    /**
     * 发送普通通知邮件
     */
    public void sendNotify(String to, String subject, String content) {
        send(to, "【云图书管理系统】" + subject, content);
    }

    private void send(String to, String subject, String content) {
        if (!Boolean.TRUE.equals(properties.getEmail().getEnabled())) {
            log.warn("【邮件未开启】收件人={} 主题={}\n{}", to, subject, content);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功，收件人={}", to);
        } catch (Exception e) {
            log.error("邮件发送失败，收件人={}，原因={}", to, e.getMessage());
        }
    }
}
