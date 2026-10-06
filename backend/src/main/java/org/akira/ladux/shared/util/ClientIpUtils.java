package org.akira.ladux.shared.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Never read client-controlled forwarding headers here. In production Spring's
 * ForwardedHeaderFilter may wrap the request, but only Caddy may supply those
 * headers on the internal Docker network. Local requests use socket remoteAddr.
 */
public final class ClientIpUtils {

    private ClientIpUtils() {}

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "127.0.0.1" : remoteAddr;
    }
}
