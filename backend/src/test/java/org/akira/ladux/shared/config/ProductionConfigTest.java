package org.akira.ladux.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigTest {
    private MockEnvironment valid() {
        MockEnvironment env = new MockEnvironment().withProperty("app.jwt.secret", java.util.Base64.getEncoder().encodeToString(new byte[32]))
                .withProperty("app.auth.cookie.secure", "true").withProperty("app.auth.device-cookie.secure", "true")
                .withProperty("app.captcha.enabled", "true").withProperty("app.captcha.secret", "fixture-secret")
                .withProperty("app.captcha.expected-hostname", "ladux.vn").withProperty("payment.vnpay.tmn-code", "merchant")
                .withProperty("payment.vnpay.hash-secret", "fixture-hmac");
        for (String key : new String[]{"app.oauth2.success-redirect", "app.oauth2.failure-redirect", "payment.vnpay.pay-url",
                "payment.vnpay.return-url", "payment.vnpay.query-url", "payment.vnpay.refund-url"})
            env.setProperty(key, "https://ladux.vn/fixture");
        env.setActiveProfiles("prod");
        return env;
    }

    @Test void acceptsExplicitSecureConfiguration() { assertDoesNotThrow(() -> new ProductionConfig(valid()).afterPropertiesSet()); }
    @Test void rejectsWeakSecret() { assertThrows(IllegalStateException.class, () -> new ProductionConfig(valid().withProperty("app.jwt.secret", "weak")).afterPropertiesSet()); }
    @Test void rejectsInsecureCookies() { assertThrows(IllegalStateException.class, () -> new ProductionConfig(valid().withProperty("app.auth.cookie.secure", "false")).afterPropertiesSet()); }
    @Test void rejectsPlaceholderURLs() { assertThrows(IllegalStateException.class, () -> new ProductionConfig(valid().withProperty("payment.vnpay.query-url", "https://example.com/api")).afterPropertiesSet()); }
    @Test void rejectsMixedProfiles() {
        var env = valid(); env.setActiveProfiles("prod", "dev");
        assertThrows(IllegalStateException.class, () -> new ProductionConfig(env).afterPropertiesSet());
    }
}
