package org.akira.ladux.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.repository.RefreshTokenRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.RefreshTokenSecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenSecurityServiceImpl implements RefreshTokenSecurityService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReuse(String familyId, Integer userId) {
        if (familyId != null && !familyId.isBlank()) {
            refreshTokenRepository.findAll().stream()
                    .filter(token -> familyId.equals(token.getFamilyId()))
                    .forEach(token -> token.setRevoked(true));
        }
        userRepository.incrementTokenVersion(userId);
    }
}
