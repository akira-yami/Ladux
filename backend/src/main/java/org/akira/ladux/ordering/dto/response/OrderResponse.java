package org.akira.ladux.ordering.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.payment.enums.PaymentProvider;

public record OrderResponse(
        Integer id,
        Integer userId,
        String couponCode,
        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        OrderStatus status,
        ShippingAddressResponse shippingAddress,
        String trackingNumber,
        String carrier,
        BigDecimal shippingFee,
        Instant createdAt,
        Instant paymentExpiresAt,
        List<OrderItemResponse> orderItems,
        PaymentProvider paymentProvider,
        String returnReason
) implements Serializable {
}
