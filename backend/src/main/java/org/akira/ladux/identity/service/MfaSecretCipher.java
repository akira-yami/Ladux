package org.akira.ladux.identity.service;

public interface MfaSecretCipher {

    String encrypt(String plainText);

    String decrypt(String encryptedText);
}
