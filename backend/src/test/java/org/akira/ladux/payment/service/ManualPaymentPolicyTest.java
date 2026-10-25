package org.akira.ladux.payment.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.OrderLifecycleService;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.dto.request.PaymentCallbackRequest;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.service.impl.PaymentServiceImpl;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManualPaymentPolicyTest {
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OrderLifecycleService lifecycle = mock(OrderLifecycleService.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl(payments, orders, users, lifecycle,
            mock(VNPayPaymentUrlService.class), new VNPayProperties(), mock(RefundWorkflowService.class));

    private Payment payment(PaymentProvider provider, OrderStatus status) {
        Order order = Order.builder().id(12).user(User.builder().id(2).build()).status(status).build();
        Payment payment = Payment.builder().id(3).order(order).provider(provider).amount(new BigDecimal("100000"))
                .status(PaymentStatus.PENDING).build();
        when(payments.findOrderIdByPaymentId(3)).thenReturn(Optional.of(12));
        when(orders.findWithItemsByIdForUpdate(12)).thenReturn(Optional.of(order));
        when(payments.findByIdForUpdate(3)).thenReturn(Optional.of(payment));
        return payment;
    }

    @Test void cannotForgeVNPaySuccessOrChangeItsProvider() {
        Payment payment = payment(PaymentProvider.VNPAY, OrderStatus.PENDING);
        assertThrows(BusinessRuleException.class, () -> service.updatePayment(3,
                new PaymentCallbackRequest(12, PaymentProvider.VNPAY, "receipt", PaymentStatus.SUCCESS)));
        assertThrows(BusinessRuleException.class, () -> service.updatePayment(3,
                new PaymentCallbackRequest(12, PaymentProvider.COD, "receipt", PaymentStatus.SUCCESS)));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(lifecycle);
    }

    @Test void cannotBypassRefundWorkflow() {
        payment(PaymentProvider.COD, OrderStatus.SHIPPED);
        assertThrows(BusinessRuleException.class, () -> service.updatePayment(3,
                new PaymentCallbackRequest(12, PaymentProvider.COD, "receipt", PaymentStatus.REFUNDED)));
    }

    @Test void cashCollectionNeedsReceiptAndShippedOrder() {
        payment(PaymentProvider.COD, OrderStatus.SHIPPED);
        assertThrows(BusinessRuleException.class, () -> service.updatePayment(3,
                new PaymentCallbackRequest(12, PaymentProvider.COD, "  ", PaymentStatus.SUCCESS)));
        payment(PaymentProvider.COD, OrderStatus.PENDING);
        assertThrows(BusinessRuleException.class, () -> service.updatePayment(3,
                new PaymentCallbackRequest(12, PaymentProvider.COD, "receipt", PaymentStatus.SUCCESS)));
    }

    @Test void collectsCODWithoutMovingShipmentBackToConfirmed() {
        Payment payment = payment(PaymentProvider.COD, OrderStatus.SHIPPED);
        User admin = User.builder().id(1).username("admin").build();
        when(users.findById(1)).thenReturn(Optional.of(admin));
        var principal = new UserPrincipal(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            service.updatePayment(3, new PaymentCallbackRequest(12, PaymentProvider.COD, "  COD-RECEIPT-42  ", PaymentStatus.SUCCESS));
            assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
            assertEquals("COD-RECEIPT-42", payment.getTransactionNo());
            assertEquals(OrderStatus.SHIPPED, payment.getOrder().getStatus());
            assertEquals(admin, payment.getOrder().getHistories().getFirst().getUser());
            verifyNoInteractions(lifecycle);
        } finally { SecurityContextHolder.clearContext(); }
    }
}
