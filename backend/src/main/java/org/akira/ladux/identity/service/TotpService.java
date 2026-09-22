package org.akira.ladux.identity.service;

public interface TotpService {

    boolean verifyCode(String base32Secret, String code);

    boolean isValidSecret(String base32Secret);
}
