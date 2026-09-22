package org.akira.ladux.identity.service;

import java.time.Instant;
import java.util.Optional;
import org.akira.ladux.identity.entity.RefreshToken;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.RefreshTokenRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.impl.RefreshTokenServiceImpl;
import org.akira.ladux.shared.exception.UnauthenticatedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenServiceImpl(repository, mock(UserRepository.class), mock(RefreshTokenSecurityService.class));
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 604_800_000L);
    }

    @Test
    void createKeepsHashInManagedEntityAndReturnsRawTokenSeparately() {
        User user = User.builder().id(7).username("customer").build();
        ArgumentCaptor<RefreshToken> persistedCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        when(repository.save(persistedCaptor.capture())).thenAnswer(invocation -> {
            RefreshToken persisted = invocation.getArgument(0);
            persisted.setId(99L);
            return persisted;
        });

        RefreshToken issued = service.create(user);
        RefreshToken persisted = persistedCaptor.getValue();

        assertEquals(44, persisted.getToken().length());
        assertEquals(64, issued.getToken().length());
        assertNotEquals(persisted.getToken(), issued.getToken());
        assertEquals(99L, issued.getId());
        assertFalse(persisted == issued);
    }

    @Test
    void verifyAndRotateAcceptsLegacyRawTokenAndReissuesHashedToken() {
        String legacyRaw = "legacy-token-stored-by-old-version";
        User user = User.builder().id(8).username("legacy").build();
        RefreshToken current = RefreshToken.builder()
                .tokenHash(legacyRaw)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(600))
                .revoked(false)
                .build();

        when(repository.findByToken(anyString())).thenAnswer(invocation ->
                legacyRaw.equals(invocation.getArgument(0)) ? Optional.of(current) : Optional.empty()
        );
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken rotated = service.verifyAndRotate(legacyRaw);

        assertTrue(current.isRevoked());
        assertEquals(64, rotated.getToken().length());
    }

    @Test
    void verifyAndRotateRejectsMissingTokenAsUnauthenticated() {
        assertThrows(UnauthenticatedException.class, () -> service.verifyAndRotate(null));
    }
}
