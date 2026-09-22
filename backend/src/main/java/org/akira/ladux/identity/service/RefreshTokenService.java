package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.RefreshToken;
import org.akira.ladux.identity.entity.User;

public interface RefreshTokenService {

    RefreshToken create(User user);

    RefreshToken createRefreshToken(User user, String familyId, Long parentId);

    RefreshToken verifyAndRotate(String rawToken);

    void revoke(String rawToken);

    void revokeSessionAndBump(String rawToken);

    void revokeAllRefreshTokens(Integer userId);
}
