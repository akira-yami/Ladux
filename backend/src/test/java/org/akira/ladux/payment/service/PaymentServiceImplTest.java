package org.akira.ladux.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.OrderLifecycleService;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.dto.request.PaymentCallbackRequest;
import org.akira.ladux.payment.dto.request.PaymentCreateRequest;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.service.impl.PaymentServiceImpl;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository repo;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @Mock
    private VNPayPaymentUrlService vnPayPaymentUrlService;

    @Mock
    private VNPayProperties vnPayProperties;

    @Mock
    private RefundWorkflowService refundWorkflowService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order orderWithStatus(OrderStatus status) {
        return Order.builder()
                .id(101)
                .user(User.builder().id(7).build())
                .status(status)
                .finalAmount(new BigDecimal("200000.00"))
                .build();
    }

    private Payment pendingPayment(Order order, PaymentProvider provider) {
        return Payment.builder()
                .id(50)
                .order(order)
                .provider(provider)
                .amount(new BigDecimal("200000.00"))
                .status(PaymentStatus.PENDING)
                .build();
    }

    private void stubAdminUpdateLookup(Payment payment, Order order) {
        when(repo.findOrderIdByPaymentId(payment.getId())).thenReturn(Optional.of(order.getId()));
        when(repo.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(orderRepository.findWithItemsByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    }

    // The provider is part of the financial audit trail, including cancelled orders.
    @Test
    void updatePayment_rejectsProviderChangeOnCancelledOrder() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        Payment payment = pendingPayment(order, PaymentProvider.VNPAY);
        stubAdminUpdateLookup(payment, order);

        assertThrows(BusinessRuleException.class, () -> paymentService.updatePayment(
                payment.getId(),
                new PaymentCallbackRequest(order.getId(), PaymentProvider.MOMO, null, PaymentStatus.PENDING)));

        assertEquals(PaymentProvider.VNPAY, payment.getProvider());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(orderLifecycleService, never()).confirmAfterSuccessfulPayment(any());
        verify(orderLifecycleService, never()).cancelOrder(any(), any());
    }

    // Doi trang thai thanh toan tren don da huy thi van phai chan.
    @Test
    void updatePayment_rejectsStatusChangeOnCancelledOrder() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        Payment payment = pendingPayment(order, PaymentProvider.VNPAY);
        stubAdminUpdateLookup(payment, order);

        assertThrows(BusinessRuleException.class, () ->
                paymentService.updatePayment(
                        payment.getId(),
                        new PaymentCallbackRequest(order.getId(), PaymentProvider.VNPAY, null, PaymentStatus.SUCCESS)));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(orderLifecycleService, never()).confirmAfterSuccessfulPayment(any());
    }

    // An expired VNPay attempt still needs verified gateway evidence, not admin success.
    @Test
    void updatePayment_rejectsManualSuccessAfterPaymentWindowExpired() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        order.setPaymentExpiresAt(Instant.now().minusSeconds(3600));
        Payment payment = pendingPayment(order, PaymentProvider.VNPAY);
        stubAdminUpdateLookup(payment, order);

        assertThrows(BusinessRuleException.class, () -> paymentService.updatePayment(
                payment.getId(),
                new PaymentCallbackRequest(order.getId(), PaymentProvider.VNPAY, null, PaymentStatus.SUCCESS)));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getTransactionNo());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(orderLifecycleService, never()).confirmAfterSuccessfulPayment(any());
        verify(orderLifecycleService, never()).cancelOrder(any(), any());
    }

    // A shipped COD payment may only record a receipt, never change providers.
    @Test
    void updatePayment_rejectsProviderChangeOnShippedOrder() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);
        Payment payment = pendingPayment(order, PaymentProvider.COD);
        stubAdminUpdateLookup(payment, order);

        assertThrows(BusinessRuleException.class, () ->
                paymentService.updatePayment(
                        payment.getId(),
                        new PaymentCallbackRequest(order.getId(), PaymentProvider.MOMO, null, PaymentStatus.PENDING)));
    }

    // No MoMo gateway exists yet, so creating a request cannot prove money arrived.
    @Test
    void createPayment_rejectsUnsupportedMomoWithoutDatabaseMutation() {
        Order order = orderWithStatus(OrderStatus.PENDING);

        assertThrows(BusinessRuleException.class, () -> paymentService.createPayment(
                7, new PaymentCreateRequest(order.getId(), PaymentProvider.MOMO), "127.0.0.1"));

        verifyNoInteractions(repo, orderRepository, orderLifecycleService);
    }

    // COD remains unpaid until a receipt is recorded during shipping.
    @Test
    void createPayment_codStaysPendingUntilReceipt() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        Payment payment = pendingPayment(order, PaymentProvider.COD);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(repo.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())).thenReturn(Optional.of(payment));

        PaymentCallbackResponse response = paymentService.createPayment(
                7, new PaymentCreateRequest(order.getId(), PaymentProvider.COD), "127.0.0.1");

        assertEquals(PaymentStatus.PENDING, response.status());
        verify(orderLifecycleService, never()).confirmAfterSuccessfulPayment(any());
    }
}
