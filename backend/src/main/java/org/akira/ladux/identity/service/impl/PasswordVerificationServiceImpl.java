package org.akira.ladux.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.service.EmailVerificationService;
import org.akira.ladux.identity.service.PasswordVerificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordVerificationServiceImpl implements PasswordVerificationService {

    private final EmailVerificationService emailVerificationService;

    @Override
    @Transactional
    public void consume(Integer customerId, String verificationId) {
        emailVerificationService.consumePasswordChangeVerification(customerId, verificationId);
    }
}
