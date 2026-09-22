package org.akira.ladux.identity.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.customer.enums.CustomerLevel;
import org.akira.ladux.identity.dto.response.UserResponse;
import org.akira.ladux.identity.entity.User;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        List<String> roleNames = user.getRoles() == null ? List.of()
                : user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList();

        Customer customer = user.getCustomer();
        String fullName = customer == null ? null : customer.getFullName();
        String phone = customer == null ? null : customer.getPhone();
        String avatar = customer == null ? null : customer.getAvatarUrl();
        Long loyaltyPoints = customer == null ? 0L : customer.getLoyaltyPoints();
        CustomerLevel level = customer == null ? CustomerLevel.BROWSER : customer.getLevel();
        BigDecimal totalSpent = customer == null ? BigDecimal.ZERO : customer.getTotalSpent();

        return new UserResponse(
                user.getId(),
                customer == null ? null : customer.getEmail(),
                user.getUsername(),
                fullName,
                phone,
                avatar,
                loyaltyPoints,
                level,
                totalSpent,
                user.isActive(),
                roleNames);
    }
}
