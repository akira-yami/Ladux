package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

    String generateAccessToken(User user);

    String extractUsername(String jwt);

    Integer extractTokenVersion(String jwt);

    boolean isTokenValid(String jwt, UserDetails userDetails);
}
