package org.akira.ladux.identity.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2FailureHandler
        extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.failure-redirect}")
    private String failureRedirectUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        String redirectUrl = UriComponentsBuilder
                .fromUriString(failureRedirectUrl)
                .queryParam("reason", "google_login_failed")
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(
                request,
                response,
                redirectUrl
        );
    }
}