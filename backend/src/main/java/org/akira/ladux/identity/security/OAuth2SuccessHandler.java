package org.akira.ladux.identity.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.akira.ladux.identity.entity.RefreshToken;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.service.GoogleOAuth2UserService;
import org.akira.ladux.identity.service.MfaService;
import org.akira.ladux.identity.service.RefreshTokenCookieService;
import org.akira.ladux.identity.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final RefreshTokenCookieService refreshTokenCookieService;
    private final RefreshTokenService refreshTokenService;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final MfaService mfaService;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Value("${app.oauth2.success-redirect}")
    private String successRedirectUrl;

    public OAuth2SuccessHandler(
            RefreshTokenCookieService refreshTokenCookieService,
            RefreshTokenService refreshTokenService,
            GoogleOAuth2UserService googleOAuth2UserService,
            MfaService mfaService,
            OAuth2FailureHandler oAuth2FailureHandler
    ) {
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.refreshTokenService = refreshTokenService;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.mfaService = mfaService;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        try {
            OAuth2User principal = (OAuth2User) authentication.getPrincipal();

            String googleSubject = principal.getAttribute("sub");
            String email = principal.getAttribute("email");
            String fullName = principal.getAttribute("name");
            String picture = principal.getAttribute("picture");

            Object verifiedAttribute = principal.getAttribute("email_verified");

            boolean emailVerified = Boolean.TRUE.equals(verifiedAttribute)
                    || "true".equalsIgnoreCase(String.valueOf(verifiedAttribute));

            User user = googleOAuth2UserService.loginOrRegister(
                    googleSubject,
                    email,
                    emailVerified,
                    fullName,
                    picture
            );
            // The OAuth callback has no MFA challenge handoff. Fail closed for privileged accounts
            // instead of granting a refresh token that could bypass the required TOTP gate.
            if (mfaService.requiresMfa(user)) {
                throw new IllegalStateException("Privileged accounts must use the MFA login flow");
            }

            RefreshToken refreshToken = refreshTokenService.create(user);

            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    refreshTokenCookieService.createRefreshCookie(refreshToken.getToken()).toString()
            );

            clearAuthenticationAttributes(request);
            invalidateOAuthSession(request);
            getRedirectStrategy().sendRedirect(request, response, successRedirectUrl);
        } catch (RuntimeException exception) {
            clearAuthenticationAttributes(request);
            invalidateOAuthSession(request);
            oAuth2FailureHandler.onAuthenticationFailure(
                    request,
                    response,
                    new AuthenticationServiceException(
                            "Không thể hoàn tất đăng nhập Google",
                            exception
                    )
            );
        }
    }

    private void invalidateOAuthSession(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }
}
