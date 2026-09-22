package org.akira.ladux.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.akira.ladux.identity.security.EndpointRateLimitFilter;
import org.akira.ladux.identity.security.JwtFilter;
import org.akira.ladux.identity.security.OAuth2FailureHandler;
import org.akira.ladux.identity.security.OAuth2SuccessHandler;
import org.akira.ladux.shared.exception.ErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * REST API dung Bearer JWT va hoan toan stateless. OAuth2 dung mot filter chain
 * rieng de Spring luu authorization request tam thoi giua redirect va callback;
 * session nay khong duoc dung de xac thuc bat ky REST request nao.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private final JwtFilter jwtFilter;
    private final EndpointRateLimitFilter endpointRateLimitFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final String allowedOrigins;
    private final boolean productionOriginsOnly;

    public SecurityConfig(
            JwtFilter jwtFilter,
            EndpointRateLimitFilter endpointRateLimitFilter,
            OAuth2SuccessHandler oAuth2SuccessHandler,
            OAuth2FailureHandler oAuth2FailureHandler,
            @org.springframework.beans.factory.annotation.Value(
                    "${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}"
            ) String allowedOrigins,
            @org.springframework.beans.factory.annotation.Value("${app.cors.production-only:false}")
            boolean productionOriginsOnly
    ) {
        this.jwtFilter = jwtFilter;
        this.endpointRateLimitFilter = endpointRateLimitFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
        this.allowedOrigins = allowedOrigins;
        this.productionOriginsOnly = productionOriginsOnly;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** OAuth2 Authorization Code flow: chi session tam thoi cho state/authorization request. */
    @Bean
    @Order(1)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .build();
    }

    /** REST/resources chain: khong doc, tao hay luu SecurityContext trong HTTP session. */
    @Bean
    @Order(2)
    public SecurityFilterChain restSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error", "/api/v1/auth/**").permitAll()
                        .requestMatchers(
                                "/api/v1/admin/auth/login", "/api/v1/admin/auth/login/",
                                "/api/v1/admin/auth/mfa/verify", "/api/v1/admin/auth/mfa/verify/",
                                "/api/v1/admin/auth/refresh", "/api/v1/admin/auth/refresh/",
                                "/api/v1/admin/auth/logout", "/api/v1/admin/auth/logout/"
                        ).permitAll()
                        .requestMatchers("/api/v1/payments/vnpay-webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products", "/api/v1/products/**",
                                "/api/v1/brands", "/api/v1/brands/**",
                                "/api/v1/categories", "/api/v1/categories/**",
                                "/api/v1/reviews", "/api/v1/reviews/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/chatbot/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(this::writeUnauthorized)
                        .accessDeniedHandler(this::writeForbidden)
                );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(endpointRateLimitFilter, JwtFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        validateOrigins(origins);
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void validateOrigins(List<String> origins) {
        if (!productionOriginsOnly) {
            return;
        }
        if (origins.isEmpty() || origins.stream().anyMatch(origin -> origin.contains("*")
                || origin.contains("localhost")
                || origin.startsWith("http://"))) {
            throw new IllegalStateException(
                    "Production CORS origins must be explicit HTTPS origins and must not include localhost or wildcards"
            );
        }
    }

    private void writeUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        writeSecurityError(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Ban chua dang nhap hoac access token khong hop le"
        );
    }

    private void writeForbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        writeSecurityError(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "Ban khong co quyen thuc hien thao tac nay"
        );
    }

    private void writeSecurityError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OBJECT_MAPPER.writeValue(
                response.getOutputStream(),
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status)
                        .error(error)
                        .message(message)
                        .build()
        );
    }
}
