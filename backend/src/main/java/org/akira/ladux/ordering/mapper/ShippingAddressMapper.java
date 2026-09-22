package org.akira.ladux.ordering.mapper;

import org.akira.ladux.ordering.dto.response.ShippingAddressResponse;
import org.akira.ladux.ordering.entity.ShippingAddress;

public final class ShippingAddressMapper {
    private ShippingAddressMapper() {
    }

    public static ShippingAddressResponse fromEntity(ShippingAddress address) {
        if (address == null) {
            return null;
        }
        return new ShippingAddressResponse(
                address.getReceiverName(),
                address.getPhone(),
                address.getStreet(),
                address.getWard(),
                address.getDistrict(),
                address.getCity()
        );
    }
}
