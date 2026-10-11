package org.akira.ladux.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.entity.LoginHistory;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.SecurityEventType;
import org.akira.ladux.identity.repository.LoginHistoryRepository;
import org.akira.ladux.identity.service.LoginHistoryService;
import org.akira.ladux.identity.service.SecurityEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final SecurityEventService securityEventService;

    @Override
    @Transactional
    public LoginHistoryResult recordSuccessfulLogin(
            User user,
            String ipAddress,
            String deviceIdHash,
            String userAgent
    ) {
        boolean newIp = !loginHistoryRepository.existsByUserIdAndIpAddress(user.getId(), ipAddress);
        boolean newDevice = !loginHistoryRepository.existsByUserIdAndDeviceIdHash(user.getId(), deviceIdHash);
        loginHistoryRepository.save(LoginHistory.builder()
                .user(user)
                .ipAddress(trim(ipAddress, 64))
                .deviceIdHash(deviceIdHash)
                .userAgent(trim(userAgent, 500))
                .newIp(newIp)
                .newDevice(newDevice)
                .build());
        securityEventService.record(user, SecurityEventType.LOGIN_SUCCESS, ipAddress, userAgent, true);
        if (newIp) {
            securityEventService.record(user, SecurityEventType.NEW_IP_LOGIN, ipAddress, userAgent, true);
        }
        if (newDevice) {
            securityEventService.record(user, SecurityEventType.NEW_DEVICE_LOGIN, ipAddress, userAgent, true);
        }
        return new LoginHistoryResult(newIp, newDevice);
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.substring(0, Math.min(value.length(), maxLength));
    }
}
