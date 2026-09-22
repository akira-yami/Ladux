package org.akira.ladux.identity.service;

import jakarta.servlet.http.HttpServletRequest;
import org.akira.ladux.identity.dto.request.LoginRequest;
import org.akira.ladux.identity.entity.User;

public interface LocalLoginService {

    PasswordLoginResult verifyPassword(LoginRequest request, boolean adminLogin, HttpServletRequest servletRequest);

    record PasswordLoginResult(User user, String challengeId) {
        public boolean mfaRequired() {
            return challengeId != null;
        }
    }
}
