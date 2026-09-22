package org.akira.ladux.customer.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import org.akira.ladux.customer.enums.CustomerLevel;

public record CustomerResponse(
        Integer id,
        Integer userId,
        String email,
        String username,
        String fullName,
        String phone,
        String avatarUrl,
        Long loyaltyPoints,
        CustomerLevel level,
        BigDecimal totalSpent
) implements Serializable {
}
