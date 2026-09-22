package org.akira.ladux.identity.service;

import java.util.Optional;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.AuthProvider;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.repository.RoleRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.service.impl.GoogleOAuth2UserServiceImpl;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GoogleOAuth2UserServiceImplTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private GoogleOAuth2UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new GoogleOAuth2UserServiceImpl(
                userRepository,
                roleRepository,
                passwordEncoder
        );
    }

    @Test
    void loginOrRegister_createsGoogleUserWithHashedRandomCredential() {
        Role customerRole = Role.builder()
                .id(2)
                .name(RoleName.CUSTOMER)
                .build();

        when(userRepository.findByGoogleSubject("google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByCustomerEmail("alice@example.com"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.CUSTOMER))
                .thenReturn(customerRole);
        when(userRepository.existsByUsername("alice"))
                .thenReturn(false);
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-random-credential");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.loginOrRegister(
                " google-subject ",
                " Alice@Example.COM ",
                true,
                "Alice Nguyen",
                " https://example.com/alice.png "
        );

        assertEquals(AuthProvider.GOOGLE, result.getAuthProvider());
        assertEquals("google-subject", result.getGoogleSubject());
        assertEquals("alice", result.getUsername());
        assertEquals("encoded-random-credential", result.getPassword());
        assertTrue(result.getRoles().contains(customerRole));
        assertNotNull(result.getCustomer());
        assertEquals("alice@example.com", result.getCustomer().getEmail());
        assertEquals("Alice Nguyen", result.getCustomer().getFullName());
        assertEquals("https://example.com/alice.png", result.getCustomer().getAvatarUrl());
        assertNotNull(result.getCustomer().getEmailVerifiedAt());
        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(result);
    }

    @Test
    void loginOrRegister_linksVerifiedGoogleIdentityWithoutRemovingLocalLogin() {
        User localUser = User.builder()
                .id(42)
                .username("alice_local")
                .password("encoded-local-password")
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
        localUser.setCustomer(Customer.builder()
                .email("alice@example.com")
                .fullName("Alice")
                .build());

        when(userRepository.findByGoogleSubject("google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByCustomerEmail("alice@example.com"))
                .thenReturn(Optional.of(localUser));

        User result = service.loginOrRegister(
                "google-subject",
                "alice@example.com",
                true,
                "Alice Google",
                null
        );

        assertEquals(localUser, result);
        assertEquals("google-subject", result.getGoogleSubject());
        assertEquals(AuthProvider.LOCAL, result.getAuthProvider());
        assertEquals("encoded-local-password", result.getPassword());
        assertEquals("Alice", result.getCustomer().getFullName());
        assertNotNull(result.getCustomer().getEmailVerifiedAt());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginOrRegister_rejectsUnverifiedEmailBeforeRepositoryAccess() {
        assertThrows(
                BusinessRuleException.class,
                () -> service.loginOrRegister(
                        "google-subject",
                        "alice@example.com",
                        false,
                        "Alice",
                        null
                )
        );

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }
}
