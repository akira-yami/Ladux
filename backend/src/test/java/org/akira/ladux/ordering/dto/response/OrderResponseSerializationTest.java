package org.akira.ladux.ordering.dto.response;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderResponseSerializationTest {

    @Test
    void pagedOrderResponseCanBeSerializedForRedisCache() {
        ShippingAddressResponse address = new ShippingAddressResponse(
                "Nguyen Van A",
                "0900000000",
                "123 Test Street",
                "Ward 1",
                "District 1",
                "Ho Chi Minh City"
        );
        OrderResponse order = new OrderResponse(
                1,
                2,
                "GIAM10",
                new BigDecimal("1000000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("930000.00"),
                OrderStatus.PENDING,
                address,
                null,
                "VNPOST",
                new BigDecimal("30000.00"),
                Instant.parse("2026-08-04T00:00:00Z"),
                null,
                List.of(),
                PaymentProvider.COD,
                null
        );
        PageImpl<OrderResponse> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> {
            try (ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
                stream.writeObject(page);
            }
        });
        assertTrue(bytes.size() > 0);
    }
}
