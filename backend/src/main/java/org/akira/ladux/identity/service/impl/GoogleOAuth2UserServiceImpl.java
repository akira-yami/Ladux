package org.akira.ladux.identity.service.impl;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.AuthProvider;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.repository.RoleRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.GoogleOAuth2UserService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2UserServiceImpl implements GoogleOAuth2UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User loginOrRegister(
            String googleSubject,
            String email,
            boolean emailVerified,
            String fullName,
            String pictureUrl
    ) {
        String normalizedSubject = normalizeRequired(
                googleSubject,
                "Google không cung cấp định danh tài khoản"
        );
        String normalizedEmail = normalizeRequired(
                email,
                "Google không cung cấp email"
        ).toLowerCase(Locale.ROOT);

        if (!emailVerified) {
            throw new BusinessRuleException("Google chưa xác minh email");
        }

        User userBySubject = userRepository
                .findByGoogleSubject(normalizedSubject)
                .orElse(null);
        if (userBySubject != null) {
            validateActive(userBySubject);
            updateGoogleProfile(userBySubject, normalizedEmail, fullName, pictureUrl);
            return userBySubject;
        }

        User userByEmail = userRepository
                .findByCustomerEmail(normalizedEmail)
                .orElse(null);
        if (userByEmail != null) {
            validateActive(userByEmail);
            if (userByEmail.getGoogleSubject() != null
                    && !normalizedSubject.equals(userByEmail.getGoogleSubject())) {
                throw new BusinessRuleException("Email đã được liên kết với một tài khoản Google khác");
            }

            userByEmail.setGoogleSubject(normalizedSubject);
            updateGoogleProfile(userByEmail, normalizedEmail, fullName, pictureUrl);
            return userByEmail;
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER);
        if (customerRole == null) {
            throw new ResourceNotFoundException("Không tìm thấy vai trò CUSTOMER trong hệ thống");
        }

        User newUser = User.builder()
                .username(generateUsername(normalizedEmail))
                // Giữ cột password NOT NULL nhưng không tạo một credential có thể đoán được.
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .googleSubject(normalizedSubject)
                .isActive(true)
                .authProvider(AuthProvider.GOOGLE)
                .roles(Set.of(customerRole))
                .build();

        Customer customer = Customer.builder()
                .user(newUser)
                .fullName(resolveFullName(fullName, normalizedEmail))
                .email(normalizedEmail)
                .emailVerifiedAt(Instant.now())
                .avatarUrl(normalizeOptional(pictureUrl))
                .build();
        newUser.setCustomer(customer);
        return userRepository.save(newUser);
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(errorMessage);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String resolveFullName(String fullName, String email) {
        String normalizedName = normalizeOptional(fullName);
        return normalizedName == null ? email : normalizedName;
    }

    private void updateGoogleProfile(
            User user,
            String email,
            String fullName,
            String pictureUrl
    ) {
        Customer customer = user.getCustomer();
        if (customer == null) {
            customer = Customer.builder()
                    .user(user)
                    .fullName(resolveFullName(fullName, email))
                    .email(email)
                    .emailVerifiedAt(Instant.now())
                    .avatarUrl(normalizeOptional(pictureUrl))
                    .build();
            user.setCustomer(customer);
            return;
        }

        customer.setEmail(email);
        customer.setEmailVerifiedAt(Instant.now());
        String normalizedName = normalizeOptional(fullName);
        if (customer.getFullName() == null && normalizedName != null) {
            customer.setFullName(normalizedName);
        }
        String normalizedPicture = normalizeOptional(pictureUrl);
        if (normalizedPicture != null) {
            String currentAvatar = customer.getAvatarUrl();
            if (currentAvatar == null || currentAvatar.isBlank()) {
                customer.setAvatarUrl(normalizedPicture);
            }
        }
    }

    private void validateActive(User user) {
        if (!user.isActive()) {
            throw new BusinessRuleException("Tài khoản đã bị vô hiệu hóa");
        }
    }

    private String generateUsername(String email) {
        String prefix = email.substring(0, email.indexOf('@'))
                .replaceAll("[^a-zA-Z0-9_]", "");

        if (prefix.length() < 4) {
            prefix = "google_" + prefix;
        }

        String candidate = prefix;
        while (userRepository.existsByUsername(candidate)) {
            candidate = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        }
        return candidate;
    }
}
