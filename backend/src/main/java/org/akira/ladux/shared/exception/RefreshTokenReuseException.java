package org.akira.ladux.shared.exception;

public class RefreshTokenReuseException extends RuntimeException{
    public RefreshTokenReuseException(String message) {
        super(message);
    }
}
