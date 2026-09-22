package org.akira.ladux.identity.enums;

public enum EmailVerificationStatus {
    PENDING,
    VERIFIED,
    CONSUMED,
    EXPIRED,
    INVALIDATED,
    SEND_FAILED
}