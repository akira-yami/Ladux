package org.akira.ladux.identity.service;

import jakarta.servlet.http.HttpServletRequest;
import org.akira.ladux.identity.dto.request.MfaVerifyRequest;
import org.akira.ladux.identity.entity.User;

public interface MfaVerificationService {

    VerifiedMfaLogin verify(MfaVerifyRequest request, boolean adminSession, HttpServletRequest servletRequest);

    record VerifiedMfaLogin(User user, boolean adminSession) {
    }
}
