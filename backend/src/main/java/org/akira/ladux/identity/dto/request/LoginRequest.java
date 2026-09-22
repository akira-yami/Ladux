package org.akira.ladux.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username khong duoc de trong")
        String username,
        @NotBlank(message = "Password khong duoc de trong")
        String password,
        String captchaToken
) {

    /** Kept for callers compiled against the pre-CAPTCHA request shape. */
    public LoginRequest(String username, String password) {
        this(username, password, null);
    }
}
