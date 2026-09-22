package org.akira.ladux.customer.mapper;

import org.akira.ladux.customer.dto.response.CustomerResponse;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.identity.entity.User;

public final class CustomerMapper {
    private CustomerMapper() {
    }

    public static CustomerResponse fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        User user = customer.getUser();
        return new CustomerResponse(
                customer.getId(),
                user == null ? null : user.getId(),
                user == null ? null : customer.getEmail(),
                user == null ? null : user.getUsername(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getAvatarUrl(),
                customer.getLoyaltyPoints(),
                customer.getLevel(),
                customer.getTotalSpent()
        );
    }
}
