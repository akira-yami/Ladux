package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.User;

public interface LoginAlertService {

    void sendNewLoginAlert(User user, String ipAddress, String userAgent, boolean newIp, boolean newDevice);
}
