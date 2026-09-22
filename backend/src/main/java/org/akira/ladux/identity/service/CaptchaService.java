package org.akira.ladux.identity.service;

/** Verifies a provider-issued CAPTCHA token on the server; browser claims are never trusted. */
public interface CaptchaService {

    void verify(String captchaToken, String clientIp);
}
