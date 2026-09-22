package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.User;

public interface MfaService {

    boolean requiresMfa(User user);

    boolean verifyTotp(User user, String code);

    void enableTotp(User user, String base32Secret);
}
