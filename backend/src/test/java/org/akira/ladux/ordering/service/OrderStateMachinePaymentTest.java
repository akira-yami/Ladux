package org.akira.ladux.ordering.service;

import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.notification.repository.NotificationRepository;
import org.akira.ladux.ordering.dto.request.OrderStatusUpdateRequest;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.impl.OrderStateMachineImpl;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderStateMachinePaymentTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final OrderStateMachineImpl service = new OrderStateMachineImpl(orders, mock(OrderLifecycleService.class),
            events, mock(UserRepository.class), mock(NotificationRepository.class));

    private Order order(OrderStatus status, PaymentProvider provider) {
        Order order = Order.builder().id(42).user(User.builder().id(2).build()).status(status).build();
        order.getPayments().add(Payment.builder().order(order).provider(provider).status(PaymentStatus.PENDING).build());
        when(orders.findWithItemsByIdForUpdate(42)).thenReturn(Optional.of(order));
        return order;
    }

    @Test void refusesManualConfirmationOfUnpaidVNPay() {
        Order order = order(OrderStatus.PENDING, PaymentProvider.VNPAY);
        assertThrows(BusinessRuleException.class, () -> service.updateOrderStatus(42,
                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null)));
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test void refusesShippingAnUnpaidOnlineOrder() {
        order(OrderStatus.CONFIRMED, PaymentProvider.VNPAY);
        assertThrows(BusinessRuleException.class, () -> service.updateOrderStatus(42,
                new OrderStatusUpdateRequest(OrderStatus.SHIPPED, null)));
    }

    @Test void requiresCollectedCashBeforeCODDelivery() {
        Order order = order(OrderStatus.SHIPPED, PaymentProvider.COD);
        assertThrows(BusinessRuleException.class, () -> service.updateOrderStatus(42,
                new OrderStatusUpdateRequest(OrderStatus.DELIVERED, null)));
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        verifyNoInteractions(events);
    }

    @Test void rejectingReturnDoesNotAwardLoyaltyAgain() {
        Order order = order(OrderStatus.RETURN_REQUESTED, PaymentProvider.COD);
        service.updateOrderStatus(42, new OrderStatusUpdateRequest(OrderStatus.DELIVERED, null));
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verifyNoInteractions(events);
    }
}
