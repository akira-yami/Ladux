package org.akira.ladux.identity.service;

public interface PasswordVerificationService {

    void consume(
            Integer customerId,
            String verificationId
    );
}