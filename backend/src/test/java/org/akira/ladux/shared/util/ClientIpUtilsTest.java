package org.akira.ladux.shared.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpUtilsTest {
    @Test
    void ignoresAttackerControlledForwardedHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("X-Forwarded-For", "8.8.8.8");
        request.addHeader("Proxy-Client-IP", "1.1.1.1");
        request.addHeader("Forwarded", "for=203.0.113.44");
        assertEquals("192.0.2.10", ClientIpUtils.getClientIp(request));
    }
}
