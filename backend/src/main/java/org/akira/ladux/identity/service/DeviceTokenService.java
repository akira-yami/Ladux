package org.akira.ladux.identity.service;

import jakarta.servlet.http.HttpServletRequest;

public interface DeviceTokenService {

    DeviceIdentity resolve(HttpServletRequest request);

    String createCookie(String token);

    record DeviceIdentity(String hash, String tokenToSet) {
    }
}
