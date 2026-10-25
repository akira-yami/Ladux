package org.akira.ladux.identity.service;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.identity.entity.Role;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.enums.RoleName;
import org.akira.ladux.identity.repository.RoleRepository;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.entity.Cart;
import org.akira.ladux.ordering.repository.CartRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BootstrapAdminService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final CartRepository carts;
    private final PasswordEncoder encoder;

    @Transactional
    public void createInitialAdmin(String username, String password, String email) {
        // Lock one shared role so two new app instances cannot bootstrap two admins.
        Role adminRole = roles.findByNameForUpdate(RoleName.ADMIN);
        if (adminRole == null) throw new IllegalStateException("Missing ADMIN role; apply production migrations first");
        if (users.existsByRoles_Name(RoleName.ADMIN)) return;
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,60}") || password == null
                || password.length() < 16 || password.getBytes(StandardCharsets.UTF_8).length > 72
                || email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || password.startsWith("REPLACE_"))
            throw new IllegalStateException("Configure a valid bootstrap username/email and a 16–72 byte password");
        if (users.existsByUsername(username))
            throw new IllegalStateException("Bootstrap username already exists; existing accounts are never promoted automatically");
        User user = User.builder().username(username).password(encoder.encode(password)).roles(Set.of(adminRole)).build();
        user.setCustomer(Customer.builder().fullName("Ladux Administrator").email(email).build());
        users.saveAndFlush(user);
        carts.save(Cart.builder().user(user).build());
    }
}
