package org.akira.ladux.identity.service;

import java.util.List;
import org.akira.ladux.identity.entity.RefreshToken;
import org.akira.ladux.identity.repository.RefreshTokenRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.impl.RefreshTokenSecurityServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefreshTokenSecurityServiceTest {
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenSecurityService service = new RefreshTokenSecurityServiceImpl(tokens, users);

    @Test
    void reuseRevokesOnlyTheAffectedFamilyAndInvalidatesAccessTokens() {
        RefreshToken current = RefreshToken.builder().familyId("family-a").build();
        RefreshToken child = RefreshToken.builder().familyId("family-a").build();
        RefreshToken unrelated = RefreshToken.builder().familyId("family-b").build();
        when(tokens.findAll()).thenReturn(List.of(current, child, unrelated));

        service.handleReuse("family-a", 7);

        assertTrue(current.isRevoked());
        assertTrue(child.isRevoked());
        assertFalse(unrelated.isRevoked());
        verify(users).incrementTokenVersion(7);
    }

    @Test
    void missingFamilyStillInvalidatesAccessTokensWithoutRevokingOtherFamilies() {
        service.handleReuse(null, 7);
        verifyNoInteractions(tokens);
        verify(users).incrementTokenVersion(7);
    }
}
