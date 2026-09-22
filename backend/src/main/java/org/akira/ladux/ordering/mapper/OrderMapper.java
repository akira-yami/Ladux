package org.akira.ladux.ordering.mapper;

import java.util.List;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.payment.enums.PaymentProvider;

public final class OrderMapper {
    private OrderMapper() {
    }

    public static OrderResponse fromEntity(Order order) {
        if (order == null) {
            return null;
        }
        PaymentProvider provider = resolvePaymentProvider(order);
        return new OrderResponse(
                order.getId(),
                order.getUser() == null ? null : order.getUser().getId(),
                order.getCoupon() == null ? null : order.getCoupon().getCode(),
                order.getSubTotal(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getStatus(),
                ShippingAddressMapper.fromEntity(order.getShippingAddress()),
                order.getTrackingNumber(),
                order.getCarrierName(),
                order.getShippingFee(),
                order.getCreatedAt(),
                order.getPaymentExpiresAt(),
                order.getItems().stream()
                        .map(OrderItemMapper::fromEntity)
                        .toList(),
                provider,
                order.getReturnReason()
        );
    }

    public static OrderResponse summaryFromEntity(Order order) {
        if (order == null) {
            return null;
        }
        PaymentProvider provider = resolvePaymentProvider(order);
        return new OrderResponse(
                order.getId(),
                order.getUser() == null ? null : order.getUser().getId(),
                order.getCoupon() == null ? null : order.getCoupon().getCode(),
                order.getSubTotal(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getStatus(),
                ShippingAddressMapper.fromEntity(order.getShippingAddress()),
                order.getTrackingNumber(),
                order.getCarrierName(),
                order.getShippingFee(),
                order.getCreatedAt(),
                order.getPaymentExpiresAt(),
                List.of(),
                provider,
                order.getReturnReason()
        );
    }

    private static PaymentProvider resolvePaymentProvider(Order order) {
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            // prefer the latest payment's provider
            return order.getPayments().get(order.getPayments().size() - 1).getProvider();
        }
        return null;
    }
}
