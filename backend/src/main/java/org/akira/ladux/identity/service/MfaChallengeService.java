package org.akira.ladux.identity.service;

import java.util.Optional;

public interface MfaChallengeService {

    MfaChallenge create(Integer userId, boolean adminSession);

    Optional<MfaChallenge> findActive(String challengeId);

    MfaChallengeAttempt complete(String challengeId, boolean verified);

    record MfaChallenge(String id, Integer userId, boolean adminSession) {
    }

    enum MfaChallengeAttempt {
        SUCCESS,
        FAILED,
        INVALID
    }
}
