package org.akira.ladux.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.config.EmailOtpProperties;
import org.akira.ladux.identity.enums.EmailVerificationPurpose;
import org.akira.ladux.identity.service.EmailOtpSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GmailEmailOtpSender
        implements EmailOtpSender {

    private final JavaMailSender mailSender;
    private final EmailOtpProperties otpProperties;

    @Value("${app.email.from}")
    private String from;

    @Override
    public void sendOtp(
            String recipient,
            String otp,
            EmailVerificationPurpose purpose
    ) {
        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(resolveSubject(purpose));
        message.setText(resolveContent(otp, purpose));

        mailSender.send(message);
    }

    private String resolveSubject(
            EmailVerificationPurpose purpose
    ) {
        return switch (purpose) {
            case EMAIL_UPDATE ->
                    "Mã xác minh email LADUX";
            case PASSWORD_CHANGE ->
                    "Mã xác minh đổi mật khẩu LADUX";
        };
    }

    private String resolveContent(
            String otp,
            EmailVerificationPurpose purpose
    ) {
        String action = switch (purpose) {
            case EMAIL_UPDATE ->
                    "thêm hoặc cập nhật email";
            case PASSWORD_CHANGE ->
                    "đổi mật khẩu";
        };

        return """
                Xin chào,

                Bạn đang thực hiện yêu cầu %s trên LADUX.

                Mã xác thực của bạn là:

                %s

                Mã có hiệu lực trong %d phút.
                Không chia sẻ mã này cho bất kỳ ai.

                Nếu bạn không thực hiện yêu cầu này,
                hãy bỏ qua email.

                LADUX
                """.formatted(
                        action,
                        otp,
                        Math.max(
                                1,
                                (otpProperties.getExpiresInSeconds() + 59) / 60
                        )
                );
    }
}
