package org.akira.ladux.identity.service;

import java.util.UUID;
import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.customer.repository.CustomerRepository;
import org.akira.ladux.identity.dto.request.RegisterRequest;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class UserRegistrationIntegrationTest extends AbstractIntegrationTest {
    @Autowired UserService service;
    @Autowired UserRepository users;
    @Autowired CustomerRepository customers;
    @Autowired CartRepository carts;

    @Test void registrationCreatesUserCustomerAndCartWithOneGeneratedIdentity() {
        String username = "signup-" + UUID.randomUUID();
        var response = service.savedUser(new RegisterRequest("New customer", username, "FixturePassword123!"));
        users.flush();
        var user = users.findByUsername(username).orElseThrow();
        var customer = customers.findByUserId(user.getId()).orElseThrow();
        assertEquals(response.id(), user.getId());
        assertEquals(user.getId(), customer.getId());
        assertEquals("New customer", customer.getFullName());
        assertNotNull(carts.findByUserId(user.getId()));
    }
}
