package org.akira.ladux.identity.service;

import org.akira.ladux.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = "app.mfa.challenge.ttl-seconds=1")
class MfaChallengeServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MfaChallengeService mfaChallengeService;

    @Test
    void successfulChallengeIsSingleUse() {
        MfaChallengeService.MfaChallenge challenge = mfaChallengeService.create(101, true);

        assertTrue(mfaChallengeService.findActive(challenge.id()).isPresent());
        assertEquals(MfaChallengeService.MfaChallengeAttempt.SUCCESS,
                mfaChallengeService.complete(challenge.id(), true));
        assertTrue(mfaChallengeService.findActive(challenge.id()).isEmpty());
        assertEquals(MfaChallengeService.MfaChallengeAttempt.INVALID,
                mfaChallengeService.complete(challenge.id(), true));
    }

    @Test
    void failedAttemptsInvalidateChallengeAtConfiguredLimit() {
        MfaChallengeService.MfaChallenge challenge = mfaChallengeService.create(102, true);
        for (int attempt = 1; attempt < 5; attempt++) {
            assertEquals(MfaChallengeService.MfaChallengeAttempt.FAILED,
                    mfaChallengeService.complete(challenge.id(), false));
        }
        assertEquals(MfaChallengeService.MfaChallengeAttempt.INVALID,
                mfaChallengeService.complete(challenge.id(), false));
    }

    @Test
    void expiredChallengeIsInvalid() throws InterruptedException {
        MfaChallengeService.MfaChallenge challenge = mfaChallengeService.create(103, true);
        Thread.sleep(1200);

        assertTrue(mfaChallengeService.findActive(challenge.id()).isEmpty());
        assertEquals(MfaChallengeService.MfaChallengeAttempt.INVALID,
                mfaChallengeService.complete(challenge.id(), true));
    }
}
