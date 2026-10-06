package org.akira.ladux.identity.service;

import org.akira.ladux.identity.repository.RefreshTokenRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.impl.RefreshTokenSecurityServiceImpl;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RefreshTokenSecurityServiceTest {
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenSecurityService service = new RefreshTokenSecurityServiceImpl(tokens, users);

    @Test
    void reuseRevokesOnlyTheAffectedFamilyAndInvalidatesAccessTokens() {
        service.handleReuse("family-a", 7);

        // Bulk UPDATE is scoped in SQL; a table scan must not be performed.
        verify(tokens).revokeFamily("family-a");
        verify(users).incrementTokenVersion(7);
    }

    @Test
    void missingFamilyStillInvalidatesAccessTokensWithoutRevokingOtherFamilies() {
        service.handleReuse(null, 7);
        verifyNoInteractions(tokens);
        verify(users).incrementTokenVersion(7);
    }
}
