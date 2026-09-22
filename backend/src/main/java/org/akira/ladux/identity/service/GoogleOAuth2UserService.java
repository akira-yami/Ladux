package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.User;

public interface GoogleOAuth2UserService {

    User loginOrRegister(
            String googleSubject,
            String email,
            boolean emailVerified,
            String fullName,
            String pictureUrl
    );
}