package org.akira.ladux.payment.service;

import java.math.BigDecimal;
import org.akira.ladux.AbstractIntegrationTest;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.entity.ShippingAddress;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.repository.RefundAttemptRepository;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class RefundRecoveryIntegrationTest extends AbstractIntegrationTest {
    @Autowired RefundWorkflowService refunds;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired RefundAttemptRepository attempts;
    @Autowired UserRepository users;

    private Order order(OrderStatus status) {
        return orders.saveAndFlush(Order.builder().user(users.findById(2).orElseThrow()).status(status)
                .subTotal(new BigDecimal("100000")).finalAmount(new BigDecimal("100000")).shippingFee(BigDecimal.ZERO)
                .shippingAddress(ShippingAddress.builder().receiverName("Refund audit").phone("0912345678")
                        .street("Test").ward("Test").district("Test").city("Test").build()).build());
    }
    private Payment paid(Order order, boolean review) {
        Payment payment = payments.saveAndFlush(Payment.builder().order(order).provider(PaymentProvider.COD)
                .amount(order.getFinalAmount()).status(PaymentStatus.SUCCESS).requiresReview(review).build());
        order.getPayments().add(payment);
        return payment;
    }

    @Test void ambiguousRefundRequiresEvidenceAndConfirmationIsIdempotent() {
        Order order = order(OrderStatus.SHIPPED);
        Payment original = paid(order, false);
        Payment duplicate = paid(order, true);
        var prepared = refunds.prepare(order.getId(), duplicate.getId(), null, "Duplicate receipt", 1);
        refunds.markForReview(prepared.requestId(), "unknown");
        long id = attempts.findByPaymentId(duplicate.getId()).orElseThrow().getId();
        assertThrows(BusinessRuleException.class, () -> refunds.confirmReconciled(id, " ", 1));
        refunds.confirmReconciled(id, "BANK-STATEMENT-42", 1);
        refunds.confirmReconciled(id, "BANK-STATEMENT-42", 1);
        assertEquals(PaymentStatus.REFUNDED, duplicate.getStatus());
        assertEquals(PaymentStatus.SUCCESS, original.getStatus());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals(1, order.getHistories().size());
    }

    @Test void returnedOrderCanRefundBothReceiptsExactlyOnce() {
        Order order = order(OrderStatus.RETURNED);
        Payment original = paid(order, false);
        Payment duplicate = paid(order, true);
        var first = refunds.prepare(order.getId(), duplicate.getId(), null, "Duplicate receipt", 1);
        refunds.complete(first.requestId());
        assertEquals(OrderStatus.RETURNED, order.getStatus());
        var second = refunds.prepare(order.getId(), original.getId(), null, "Returned goods", 1);
        refunds.complete(second.requestId());
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        assertEquals(PaymentStatus.REFUNDED, original.getStatus());
        assertEquals(PaymentStatus.REFUNDED, duplicate.getStatus());
        assertNotEquals(first.requestId(), second.requestId());
        assertThrows(BusinessRuleException.class, () -> refunds.prepare(order.getId(), original.getId(), null, "Retry", 1));
    }
}
