package org.akira.ladux.identity.service.impl;

import java.time.Instant;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.service.LoginAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Mail transport failures are contained so a successful login never becomes a failed login. */
@Service
public class EmailLoginAlertService implements LoginAlertService {

    private static final Logger log = LoggerFactory.getLogger(EmailLoginAlertService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailLoginAlertService(JavaMailSender mailSender, @Value("${app.email.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendNewLoginAlert(User user, String ipAddress, String userAgent, boolean newIp, boolean newDevice) {
        try {
            String email = user == null || user.getCustomer() == null ? null : user.getCustomer().getEmail();
            if (email == null || email.isBlank()) {
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("Canh bao dang nhap moi LADUX");
            message.setText("""
                    Xin chao,

                    Tai khoan LADUX cua ban vua dang nhap luc: %s
                    Dia chi IP: %s
                    Thiet bi/trinh duyet: %s
                    Phat hien: %s

                    Neu day khong phai ban, hay doi mat khau ngay va lien he bo phan ho tro.
                    """.formatted(
                    Instant.now(),
                    ipAddress == null ? "khong xac dinh" : ipAddress,
                    userAgent == null || userAgent.isBlank() ? "khong xac dinh" : userAgent,
                    describe(newIp, newDevice)
            ));
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("Không thể gửi email cảnh báo đăng nhập cho userId={}", user.getId(), exception);
        }
    }

    private String describe(boolean newIp, boolean newDevice) {
        if (newIp && newDevice) {
            return "IP moi va thiet bi moi";
        }
        return newIp ? "IP moi" : "thiet bi moi";
    }
}
