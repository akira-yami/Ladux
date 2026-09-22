package org.akira.ladux.identity.service;

import java.util.Optional;
import java.util.Set;
import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.entity.UserMfaMethod;
import org.akira.ladux.identity.enums.MfaType;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.repository.UserMfaMethodRepository;
import org.akira.ladux.identity.service.impl.MfaServiceImpl;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfaServiceImplTest {

    @Test
    void requiresMfaReturnsTrueForAdminAndStaff() {
        MfaServiceImpl service = new MfaServiceImpl(
                mock(UserMfaMethodRepository.class),
                mock(MfaSecretCipher.class),
                mock(TotpService.class),
                mock(Environment.class),
                ""
        );

        User admin = User.builder().roles(Set.of(Role.builder().name(RoleName.ADMIN).build())).build();
        User staff = User.builder().roles(Set.of(Role.builder().name(RoleName.STAFF).build())).build();
        User customer = User.builder().roles(Set.of(Role.builder().name(RoleName.CUSTOMER).build())).build();

        assertTrue(service.requiresMfa(admin));
        assertTrue(service.requiresMfa(staff));
        assertFalse(service.requiresMfa(customer));
    }

    @Test
    void verifyTotpWithDevFixedCodeSucceedsWithoutQueryingRepository() {
        UserMfaMethodRepository repo = mock(UserMfaMethodRepository.class);
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(false);

        MfaServiceImpl service = new MfaServiceImpl(
                repo,
                mock(MfaSecretCipher.class),
                mock(TotpService.class),
                env,
                "123456"
        );

        User admin = User.builder().id(1).roles(Set.of(Role.builder().name(RoleName.ADMIN).build())).build();

        boolean result = service.verifyTotp(admin, "123456");
        assertTrue(result);
        verify(repo, never()).findFirstByUserIdAndTypeAndEnabledTrueOrderByIdDesc(any(), any());
    }

    @Test
    void verifyTotpWithDevFixedCodeIsIgnoredOnProd() {
        UserMfaMethodRepository repo = mock(UserMfaMethodRepository.class);
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(true);
        when(repo.findFirstByUserIdAndTypeAndEnabledTrueOrderByIdDesc(1, MfaType.TOTP))
                .thenReturn(Optional.empty());

        MfaServiceImpl service = new MfaServiceImpl(
                repo,
                mock(MfaSecretCipher.class),
                mock(TotpService.class),
                env,
                "123456"
        );

        User admin = User.builder().id(1).roles(Set.of(Role.builder().name(RoleName.ADMIN).build())).build();

        assertThrows(BusinessRuleException.class, () -> service.verifyTotp(admin, "123456"));
        verify(repo).findFirstByUserIdAndTypeAndEnabledTrueOrderByIdDesc(1, MfaType.TOTP);
    }

    @Test
    void verifyTotpWithDifferentCodeQueriesRepositoryAndValidates() {
        UserMfaMethodRepository repo = mock(UserMfaMethodRepository.class);
        MfaSecretCipher cipher = mock(MfaSecretCipher.class);
        TotpService totpService = mock(TotpService.class);
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(Profiles.of("prod"))).thenReturn(false);

        User admin = User.builder().id(1).roles(Set.of(Role.builder().name(RoleName.ADMIN).build())).build();
        UserMfaMethod method = UserMfaMethod.builder().id(10L).secretEncrypted("encSecret").build();

        when(repo.findFirstByUserIdAndTypeAndEnabledTrueOrderByIdDesc(1, MfaType.TOTP))
                .thenReturn(Optional.of(method));
        when(cipher.decrypt("encSecret")).thenReturn("plainBase32");
        when(totpService.verifyCode("plainBase32", "654321")).thenReturn(true);

        MfaServiceImpl service = new MfaServiceImpl(repo, cipher, totpService, env, "123456");

        assertTrue(service.verifyTotp(admin, "654321"));
    }
}
