package org.akira.ladux.ordering.dto.response;

import java.io.Serializable;

/**
 * API representation of an order's immutable shipping-address snapshot.
 * Keeping the JPA embeddable out of the response also makes Redis cache
 * serialization independent from the persistence model.
 */
public record ShippingAddressResponse(
        String receiverName,
        String phone,
        String street,
        String ward,
        String district,
        String city
) implements Serializable {
}
