package org.akira.ladux.identity.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.dto.request.MfaVerifyRequest;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.SecurityEventType;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.LoginRateLimitService;
import org.akira.ladux.identity.service.MfaChallengeService;
import org.akira.ladux.identity.service.MfaService;
import org.akira.ladux.identity.service.MfaVerificationService;
import org.akira.ladux.identity.service.SecurityEventService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.util.ClientIpUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MfaVerificationServiceImpl implements MfaVerificationService {

    private final MfaChallengeService mfaChallengeService;
    private final LoginRateLimitService loginRateLimitService;
    private final UserRepository userRepository;
    private final MfaService mfaService;
    private final SecurityEventService securityEventService;

    @Override
    public VerifiedMfaLogin verify(
            MfaVerifyRequest request,
            boolean adminSession,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = ClientIpUtils.getClientIp(servletRequest);
        String userAgent = servletRequest == null ? null : servletRequest.getHeader("User-Agent");
        MfaChallengeService.MfaChallenge challenge = mfaChallengeService.findActive(request.challengeId())
                .orElseThrow(() -> new BusinessRuleException("Phien xac thuc MFA khong hop le hoac da het han"));
        if (challenge.adminSession() != adminSession) {
            throw new BusinessRuleException("Phien xac thuc MFA khong hop le");
        }
        User user = userRepository.findById(challenge.userId())
                .orElseThrow(() -> new BusinessRuleException("Nguoi dung khong ton tai"));
        loginRateLimitService.checkMfaAccountAndChallenge(user.getUsername(), request.challengeId());

        boolean valid;
        String failureReason = null;
        try {
            valid = mfaService.verifyTotp(user, request.code());
            if (!valid) {
                failureReason = "Ma xac thuc MFA khong chinh xac";
            }
        } catch (BusinessRuleException exception) {
            valid = false;
            failureReason = exception.getMessage();
        }
        MfaChallengeService.MfaChallengeAttempt attempt = mfaChallengeService.complete(request.challengeId(), valid);
        if (!valid || attempt != MfaChallengeService.MfaChallengeAttempt.SUCCESS) {
            securityEventService.record(user, SecurityEventType.MFA_FAILED, ipAddress, userAgent, false);
            throw new BusinessRuleException(failureReason != null ? failureReason : "Ma xac thuc MFA khong chinh xac hoac da het han");
        }

        securityEventService.record(user, SecurityEventType.MFA_SUCCESS, ipAddress, userAgent, true);
        return new VerifiedMfaLogin(user, challenge.adminSession());
    }
}
