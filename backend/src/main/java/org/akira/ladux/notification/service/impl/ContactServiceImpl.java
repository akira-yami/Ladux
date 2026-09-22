package org.akira.ladux.notification.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.notification.dto.request.ContactRequest;
import org.akira.ladux.notification.service.ContactService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.contact.receiver-email}")
    private String receiverEmail;

    @Override
    public void sendContactMessage(ContactRequest request) {
        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setFrom(senderEmail);
        mail.setTo(receiverEmail);
        mail.setSubject("[LADUX] Liên hệ mới từ " + request.fullName());

        mail.setText("""
                Bạn vừa nhận được một yêu cầu liên hệ mới từ website LADUX.

                Họ và tên: %s
                Email / Số điện thoại: %s

                Nội dung:
                %s
                """.formatted(
                request.fullName(),
                request.contact(),
                request.message()
        ));

        try {
            mailSender.send(mail);
        } catch (MailException exception) {
            throw new BusinessRuleException(
                    "Không thể gửi yêu cầu liên hệ. Vui lòng thử lại sau"
            );
        }
    }
}