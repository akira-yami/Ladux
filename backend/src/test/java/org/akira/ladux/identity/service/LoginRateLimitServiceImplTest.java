package org.akira.ladux.identity.service;

import org.akira.ladux.identity.service.impl.LoginRateLimitServiceImpl;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoginRateLimitServiceImplTest {

    @Test
    void adminLoginUsesAccountAndIpAccountScopesWithNormalizedUsername() {
        DistributedRateLimitService distributed = mock(DistributedRateLimitService.class);
        LoginRateLimitService service = service(distributed);

        service.checkLoginAccountAndIp(true, "203.0.113.10", "  ADMIN.User  ");

        verify(distributed).check("admin-login-account", "admin.user", 5, 15,
                "Ban da thu dang nhap qua nhieu, hay thu lai sau");
        verify(distributed).check("admin-login-ip-account", "203.0.113.10|admin.user", 3, 10,
                "Ban da thu dang nhap qua nhieu, hay thu lai sau");
    }

    private LoginRateLimitService service(DistributedRateLimitService distributed) {
        return new LoginRateLimitServiceImpl(distributed, 5, 15, 3, 10);
    }
}
