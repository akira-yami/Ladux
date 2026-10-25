package org.akira.ladux.shared.config;

import java.net.URI;
import java.util.Base64;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Fail startup before accepting traffic when production safety settings are invalid. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionConfig implements InitializingBean {
    private final Environment environment;
    public ProductionConfig(Environment environment) { this.environment = environment; }

    @Override
    public void afterPropertiesSet() {
        for (String profile : environment.getActiveProfiles())
            if (profile.equals("dev") || profile.equals("test")) fail("Do not combine prod with dev/test profiles");
        String secret = required("app.jwt.secret");
        try {
            if (Base64.getDecoder().decode(secret).length < 32) fail("JWT secret must contain at least 32 random bytes");
        } catch (IllegalArgumentException ex) { fail("Production JWT secret must be Base64 encoded"); }
        for (String key : new String[]{"app.auth.cookie.secure", "app.auth.device-cookie.secure", "app.captcha.enabled"})
            if (!environment.getProperty(key, Boolean.class, false)) fail(key + " must be true in production");
        required("app.captcha.secret");
        required("app.captcha.expected-hostname");
        for (String key : new String[]{"app.oauth2.success-redirect", "app.oauth2.failure-redirect",
                "payment.vnpay.pay-url", "payment.vnpay.return-url", "payment.vnpay.query-url", "payment.vnpay.refund-url"}) {
            try {
                URI uri = URI.create(required(key));
                if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                        || uri.getHost().equals("localhost") || uri.getHost().endsWith("example.com")) fail(key + " must be a real HTTPS URL");
            } catch (IllegalArgumentException ex) { fail(key + " must be a valid HTTPS URL"); }
        }
        required("payment.vnpay.tmn-code");
        required("payment.vnpay.hash-secret");
        if (new java.math.BigDecimal(environment.getProperty("app.order.shipping-fee", "30000")).signum() < 0)
            fail("Shipping fee must not be negative");
    }

    private String required(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank() || value.startsWith("REPLACE_") || value.contains("${")) fail("Missing production setting: " + key);
        return value;
    }
    private void fail(String message) { throw new IllegalStateException(message); }
}
