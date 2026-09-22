package org.akira.ladux.customer.dto.response;

import java.io.Serializable;

public record UserAddressResponse(
        Integer id,
        Integer userId,
        String receiverName,
        String phone,
        String street,
        String ward,
        String district,
        String city,
        boolean isDefault
) implements Serializable {
}
