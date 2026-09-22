package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.User;

public interface LoginHistoryService {

    LoginHistoryResult recordSuccessfulLogin(User user, String ipAddress, String deviceIdHash, String userAgent, boolean mfaUsed);

    record LoginHistoryResult(boolean newIp, boolean newDevice) {
    }
}
