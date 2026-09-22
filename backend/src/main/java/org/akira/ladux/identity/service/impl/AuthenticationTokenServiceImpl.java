package org.akira.ladux.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.entity.RefreshToken;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.service.AuthenticationTokenService;
import org.akira.ladux.identity.service.JwtService;
import org.akira.ladux.identity.service.RefreshTokenService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationTokenServiceImpl implements AuthenticationTokenService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public IssuedAuthenticationTokens issueAuthenticationTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user);
        return new IssuedAuthenticationTokens(accessToken, refreshToken.getToken());
    }
}
