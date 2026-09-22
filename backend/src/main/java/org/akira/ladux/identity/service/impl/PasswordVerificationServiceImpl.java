package org.akira.ladux.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.repository.EmailVerificationRepository;
import org.akira.ladux.identity.repository.PhoneVerificationRepository;
import org.akira.ladux.identity.service.EmailVerificationService;
import org.akira.ladux.identity.service.PasswordVerificationService;
import org.akira.ladux.identity.service.PhoneVerificationService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordVerificationServiceImpl
        implements PasswordVerificationService {

    private final PhoneVerificationRepository
            phoneVerificationRepository;

    private final EmailVerificationRepository
            emailVerificationRepository;

    private final PhoneVerificationService
            phoneVerificationService;

    private final EmailVerificationService
            emailVerificationService;

    @Override
    @Transactional
    public void consume(
            Integer customerId,
            String verificationId
    ) {
        boolean phoneVerificationExists =
                phoneVerificationRepository
                        .existsByVerificationIdAndCustomerId(
                                verificationId,
                                customerId
                        );

        if (phoneVerificationExists) {
            phoneVerificationService
                    .consumePasswordChangeVerification(
                            customerId,
                            verificationId
                    );

            return;
        }

        boolean emailVerificationExists =
                emailVerificationRepository
                        .existsByVerificationIdAndCustomerId(
                                verificationId,
                                customerId
                        );

        if (emailVerificationExists) {
            emailVerificationService
                    .consumePasswordChangeVerification(
                            customerId,
                            verificationId
                    );

            return;
        }

        throw new BusinessRuleException(
                "Phiên xác thực đổi mật khẩu không hợp lệ"
        );
    }
}