package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.RefreshToken;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.service.impl.AuthenticationTokenServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationTokenServiceImplTest {

    @Test
    void issuesAccessAndOpaqueRefreshTokenTogether() {
        JwtService jwtService = mock(JwtService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        User user = User.builder().id(9).username("customer").build();
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.create(user)).thenReturn(RefreshToken.builder().tokenHash("opaque-refresh").user(user).build());

        AuthenticationTokenService.IssuedAuthenticationTokens result =
                new AuthenticationTokenServiceImpl(jwtService, refreshTokenService).issueAuthenticationTokens(user);

        assertEquals("access-token", result.accessToken());
        assertEquals("opaque-refresh", result.refreshToken());
        verify(jwtService).generateAccessToken(user);
        verify(refreshTokenService).create(user);
    }
}
