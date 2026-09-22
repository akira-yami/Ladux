package org.akira.ladux.identity.service;

import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.service.impl.LoginSuccessServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginSuccessServiceImplTest {

    @Test
    void familiarIpAndDeviceAreRecordedWithoutAlert() {
        AuthenticationTokenService tokens = mock(AuthenticationTokenService.class);
        DeviceTokenService devices = mock(DeviceTokenService.class);
        LoginHistoryService history = mock(LoginHistoryService.class);
        LoginAlertService alerts = mock(LoginAlertService.class);
        User user = User.builder().id(11).username("customer").build();
        when(devices.resolve(any())).thenReturn(new DeviceTokenService.DeviceIdentity("device-hash", null));
        when(tokens.issueAuthenticationTokens(user)).thenReturn(
                new AuthenticationTokenService.IssuedAuthenticationTokens("access", "refresh")
        );
        when(history.recordSuccessfulLogin(eq(user), any(), eq("device-hash"), any(), eq(false)))
                .thenReturn(new LoginHistoryService.LoginHistoryResult(false, false));

        new LoginSuccessServiceImpl(tokens, devices, history, alerts).complete(user, new MockHttpServletRequest(), false);

        verify(history).recordSuccessfulLogin(eq(user), any(), eq("device-hash"), any(), eq(false));
        verify(alerts, never()).sendNewLoginAlert(any(), any(), any(), any(Boolean.class), any(Boolean.class));
    }

    @Test
    void newIpOrDeviceTriggersNonBlockingAlert() {
        AuthenticationTokenService tokens = mock(AuthenticationTokenService.class);
        DeviceTokenService devices = mock(DeviceTokenService.class);
        LoginHistoryService history = mock(LoginHistoryService.class);
        LoginAlertService alerts = mock(LoginAlertService.class);
        User user = User.builder().id(12).username("customer").build();
        when(devices.resolve(any())).thenReturn(new DeviceTokenService.DeviceIdentity("new-device-hash", "raw-device-token"));
        when(devices.createCookie("raw-device-token")).thenReturn("DEVICE_TOKEN=raw-device-token; HttpOnly; Secure");
        when(tokens.issueAuthenticationTokens(user)).thenReturn(
                new AuthenticationTokenService.IssuedAuthenticationTokens("access", "refresh")
        );
        when(history.recordSuccessfulLogin(eq(user), any(), eq("new-device-hash"), any(), eq(true)))
                .thenReturn(new LoginHistoryService.LoginHistoryResult(true, true));

        LoginSuccessService.CompletedLogin result = new LoginSuccessServiceImpl(tokens, devices, history, alerts)
                .complete(user, new MockHttpServletRequest(), true);

        verify(alerts).sendNewLoginAlert(eq(user), any(), any(), eq(true), eq(true));
        verify(devices).createCookie("raw-device-token");
        org.junit.jupiter.api.Assertions.assertEquals("DEVICE_TOKEN=raw-device-token; HttpOnly; Secure", result.deviceCookieToSet());
    }
}
