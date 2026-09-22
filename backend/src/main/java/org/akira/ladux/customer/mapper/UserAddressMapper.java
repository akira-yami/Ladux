package org.akira.ladux.customer.mapper;

import org.akira.ladux.customer.dto.response.UserAddressResponse;
import org.akira.ladux.customer.entity.UserAddress;

public final class UserAddressMapper {
    private UserAddressMapper() {
    }

    public static UserAddressResponse fromEntity(UserAddress address) {
        if (address == null) {
            return null;
        }
        return new UserAddressResponse(
                address.getId(),
                address.getUser() == null ? null : address.getUser().getId(),
                address.getReceiverName(),
                address.getPhone(),
                address.getStreet(),
                address.getWard(),
                address.getDistrict(),
                address.getCity(),
                address.isDefault()
        );
    }
}
