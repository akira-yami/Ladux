package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.SecurityEventType;

public interface SecurityEventService {

    void record(User user, SecurityEventType eventType, String ipAddress, String userAgent, boolean success);
}
