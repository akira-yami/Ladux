package org.akira.ladux.identity.service.impl;

import java.util.Map;
import org.akira.ladux.identity.service.CaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Generic implementation for providers compatible with reCAPTCHA/hCaptcha's
 * server-side form endpoint. The CAPTCHA secret is read only from the runtime
 * environment-backed configuration and is never logged.
 */
@Service
public class ProviderCaptchaService implements CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(ProviderCaptchaService.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String secret;
    private final String expectedHostname;
    private final String expectedAction;

    public ProviderCaptchaService(
            RestClient.Builder restClientBuilder,
            @Value("${app.captcha.enabled:true}") boolean enabled,
            @Value("${app.captcha.verify-url:https://www.google.com/recaptcha/api/siteverify}") String verifyUrl,
            @Value("${app.captcha.secret:}") String secret,
            @Value("${app.captcha.expected-hostname:}") String expectedHostname,
            @Value("${app.captcha.expected-action:}") String expectedAction
    ) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restClient = restClientBuilder.baseUrl(verifyUrl).requestFactory(factory).build();
        this.enabled = enabled;
        this.secret = secret;
        this.expectedHostname = expectedHostname;
        this.expectedAction = expectedAction;
    }

    @Override
    public void verify(String captchaToken, String clientIp) {
        if (!enabled) {
            return;
        }
        if (captchaToken == null || captchaToken.isBlank()) {
            throw denied();
        }
        if (secret == null || secret.isBlank()) {
            log.error("CAPTCHA đang được bật nhưng chưa cấu hình server secret");
            throw new IllegalStateException("Cấu hình CAPTCHA server chưa hoàn tất");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", captchaToken);
        if (clientIp != null && !clientIp.isBlank()) {
            form.add("remoteip", clientIp);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (!isAccepted(response)) {
                throw denied();
            }
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (RestClientException exception) {
            // CAPTCHA outages must fail closed; do not let them downgrade the password gate.
            log.warn("Yêu cầu xác minh CAPTCHA thất bại: {}", exception.getClass().getSimpleName());
            throw denied();
        }
    }

    private boolean isAccepted(Map<String, Object> response) {
        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            return false;
        }
        if (!expectedHostname.isBlank() && !expectedHostname.equalsIgnoreCase(String.valueOf(response.get("hostname")))) {
            return false;
        }
        return expectedAction.isBlank() || expectedAction.equals(String.valueOf(response.get("action")));
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("CAPTCHA khong hop le");
    }
}
