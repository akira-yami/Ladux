package org.akira.ladux.identity.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import org.akira.ladux.customer.enums.CustomerLevel;

// Danh cho Admin
public record UserResponse(
        Integer id,
        String email,
        String username,
        String fullName,
        String phone,
        String avatar,
        Long loyaltyPoints,
        CustomerLevel level,
        BigDecimal totalSpent,
        boolean isActive,
        List<String> roles) implements Serializable {
}
