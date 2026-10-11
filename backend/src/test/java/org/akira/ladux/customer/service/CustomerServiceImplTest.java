package org.akira.ladux.customer.service;

import java.util.List;
import java.util.Optional;
import org.akira.ladux.customer.dto.request.PhoneUpdateRequest;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.customer.service.impl.CustomerServiceImpl;
import org.akira.ladux.identity.dto.request.UpdateInformationPersonal;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.identity.util.PhoneNumberUtils;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerServiceImplTest {

    private static final int USER_ID = 42;
    private static final String ORIGINAL_PHONE = "+84912345678";

    private CustomerRepository repository;
    private CustomerServiceImpl service;
    private Customer customer;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        service = new CustomerServiceImpl(repository, new PhoneNumberUtils());

        User user = User.builder()
                .id(USER_ID)
                .username("profile_user")
                .password("encoded-password")
                .isActive(true)
                .build();
        customer = Customer.builder()
                .id(USER_ID)
                .user(user)
                .fullName("Tên ban đầu")
                .email("old@example.com")
                .phone(ORIGINAL_PHONE)
                .build();
        user.setCustomer(customer);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(user),
                        null,
                        List.of()
                )
        );
        when(repository.findByUserId(USER_ID))
                .thenReturn(Optional.of(customer));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateInformationPersonal_changesOnlyFullName() {
        UserResponse response = service.updateInformationPersonal(
                new UpdateInformationPersonal("Tên mới")
        );

        assertEquals("Tên mới", response.fullName());
        assertEquals("old@example.com", response.email());
        assertEquals(ORIGINAL_PHONE, response.phone());
        assertEquals("old@example.com", customer.getEmail());
        assertEquals(ORIGINAL_PHONE, customer.getPhone());
    }

    @Test
    void updatePhone_normalizesContactNumberAndPreservesEmail() {
        UserResponse response = service.updatePhone(new PhoneUpdateRequest("0988123456"));

        assertEquals("+84988123456", response.phone());
        assertEquals("+84988123456", customer.getPhone());
        assertEquals("old@example.com", response.email());
    }

    @Test
    void updatePhone_rejectsNumberUsedByAnotherCustomer() {
        when(repository.existsByPhoneAndIdNot("+84988123456", USER_ID)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> service.updatePhone(new PhoneUpdateRequest("0988123456")));
        assertEquals(ORIGINAL_PHONE, customer.getPhone());
    }

    @Test
    void updatePhone_rejectsInvalidContactNumber() {
        assertThrows(BusinessRuleException.class,
                () -> service.updatePhone(new PhoneUpdateRequest("invalid")));
        assertEquals(ORIGINAL_PHONE, customer.getPhone());
    }
}
