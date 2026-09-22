package org.akira.ladux.identity.service;

import java.util.Optional;
import org.akira.ladux.identity.dto.request.MfaVerifyRequest;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.SecurityEventType;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.impl.MfaVerificationServiceImpl;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfaVerificationServiceImplTest {

    @Test
    void wrongTotpIsDeniedAndCannotIssueTokens() {
        MfaChallengeService challengeService = mock(MfaChallengeService.class);
        UserRepository users = mock(UserRepository.class);
        MfaService mfaService = mock(MfaService.class);
        SecurityEventService events = mock(SecurityEventService.class);
        User user = User.builder().id(5).username("admin").build();
        when(challengeService.findActive("challenge"))
                .thenReturn(Optional.of(new MfaChallengeService.MfaChallenge("challenge", 5, true)));
        when(users.findById(5)).thenReturn(Optional.of(user));
        when(mfaService.verifyTotp(user, "123456")).thenReturn(false);
        when(challengeService.complete("challenge", false)).thenReturn(MfaChallengeService.MfaChallengeAttempt.FAILED);

        MfaVerificationService service = new MfaVerificationServiceImpl(
                challengeService, mock(LoginRateLimitService.class), users, mfaService, events
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.verify(
                new MfaVerifyRequest("challenge", "123456"), true, new MockHttpServletRequest()
        ));
        assertEquals("Ma xac thuc MFA khong chinh xac", ex.getMessage());

        verify(events).record(eq(user), eq(SecurityEventType.MFA_FAILED), any(), any(), eq(false));
    }
}
