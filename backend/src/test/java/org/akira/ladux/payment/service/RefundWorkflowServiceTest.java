package org.akira.ladux.payment.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.entity.RefundAttempt;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.repository.RefundAttemptRepository;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RefundWorkflowServiceTest {
    @Test
    void rejectsRefundGreaterThanOriginalPaymentBeforeAnyGatewayCall() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        RefundAttemptRepository attempts = mock(RefundAttemptRepository.class);
        UserRepository users = mock(UserRepository.class);
        RefundWorkflowService service = new RefundWorkflowService(orders, payments, attempts, users);
        Order order = Order.builder().id(12).status(OrderStatus.RETURNED)
                .finalAmount(new BigDecimal("100000")).build();
        when(orders.findWithItemsByIdForUpdate(12)).thenReturn(Optional.of(order));
        when(attempts.findByOrderId(12)).thenReturn(Optional.empty());
        when(payments.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(12, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(Payment.builder().id(3).amount(new BigDecimal("100000"))
                        .provider(PaymentProvider.VNPAY).build()));
        assertThrows(BusinessRuleException.class, () ->
                service.prepare(12, new BigDecimal("200000"), "test", 1));
        verify(attempts, never()).saveAndFlush(any());
        verifyNoInteractions(users);
    }

    @Test
    void refusesSecondRefundEvenIfOrderStillReturned() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        RefundAttemptRepository attempts = mock(RefundAttemptRepository.class);
        RefundWorkflowService service = new RefundWorkflowService(orders, payments, attempts, mock(UserRepository.class));
        when(orders.findWithItemsByIdForUpdate(12)).thenReturn(Optional.of(
                Order.builder().id(12).status(OrderStatus.RETURNED).build()));
        when(attempts.findByOrderId(12)).thenReturn(Optional.of(RefundAttempt.builder()
                .status(RefundAttempt.Status.REVIEW_REQUIRED).build()));
        assertThrows(BusinessRuleException.class, () -> service.prepare(12, null, "retry", 1));
        verifyNoInteractions(payments);
    }
}
