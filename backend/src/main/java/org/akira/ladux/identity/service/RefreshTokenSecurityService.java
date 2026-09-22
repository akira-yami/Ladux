package org.akira.ladux.identity.service;

public interface RefreshTokenSecurityService {
    void handleReuse(String familyId, Integer userId);
}
