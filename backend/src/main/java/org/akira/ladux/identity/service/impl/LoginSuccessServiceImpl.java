package org.akira.ladux.identity.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.service.AuthenticationTokenService;
import org.akira.ladux.identity.service.DeviceTokenService;
import org.akira.ladux.identity.service.LoginAlertService;
import org.akira.ladux.identity.service.LoginHistoryService;
import org.akira.ladux.identity.service.LoginSuccessService;
import org.akira.ladux.shared.util.ClientIpUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginSuccessServiceImpl implements LoginSuccessService {

    private final AuthenticationTokenService authenticationTokenService;
    private final DeviceTokenService deviceTokenService;
    private final LoginHistoryService loginHistoryService;
    private final LoginAlertService loginAlertService;

    @Override
    @Transactional
    public CompletedLogin complete(User user, HttpServletRequest request) {
        DeviceTokenService.DeviceIdentity device = deviceTokenService.resolve(request);
        String ipAddress = ClientIpUtils.getClientIp(request);
        String userAgent = request == null ? null : request.getHeader("User-Agent");
        AuthenticationTokenService.IssuedAuthenticationTokens tokens = authenticationTokenService.issueAuthenticationTokens(user);
        LoginHistoryService.LoginHistoryResult history = loginHistoryService.recordSuccessfulLogin(
                user, ipAddress, device.hash(), userAgent
        );
        if (history.newIp() || history.newDevice()) {
            loginAlertService.sendNewLoginAlert(user, ipAddress, userAgent, history.newIp(), history.newDevice());
        }
        return new CompletedLogin(
                tokens,
                device.tokenToSet() == null ? null : deviceTokenService.createCookie(device.tokenToSet())
        );
    }
}
