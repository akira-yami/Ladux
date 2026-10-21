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
        when(repo.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(orderRepository.findWithItemsByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    }

    // Admin chi sua provider tren don da bi huy (job het han tu huy): phai luu duoc,
    // khong duoc nem loi "da bi huy" va khong duoc cham vao trang thai don.
    @Test
    void updatePayment_changesProviderOnCancelledOrder_whenStatusNotChanged() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        Payment payment = pendingPayment(order, PaymentProvider.VNPAY);
        stubAdminUpdateLookup(payment, order);

        PaymentCallbackResponse response = paymentService.updatePayment(
                payment.getId(),
                new PaymentCallbackRequest(order.getId(), PaymentProvider.MOMO, null, PaymentStatus.PENDING));

        assertEquals(PaymentProvider.MOMO, response.provider());
        assertEquals(PaymentStatus.PENDING, response.status());
        verify(orderLifecycleService, never()).confirmAfterSuccessfulPayment(any());
        verify(orderLifecycleService, never()).cancelOrder(any(), any());
    }

    // Doi trang thai thanh toan tren don da huy thi van phai chan.
    @Test
    void updatePayment_rejectsStatusChangeOnCancelledOrder() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        Payment payment = pendingPayment(order, PaymentProvider.VNPAY);
        stubAdminUpdateLookup(payment, order);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                paymentService.updatePayment(
                        payment.getId(),
                        new PaymentCallbackRequest(order.getId(), PaymentProvider.VNPAY, null, PaymentStatus.SUCCESS)));

        assertTrue(ex.getMessage().contains("da bi huy"));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(orderLifecycleService, never()).confirmAfterSuccessfulPayment(any());
    }

    // Regression: don da qua han 15 phut nhung chua bi job huy, admin xac nhan SUCCESS
    // phai thanh cong thay vi tu huy don roi bao loi nhu luong khach.
    @Test
    void updatePayment_adminConfirmsSuccessAfterPaymentWindowExpired() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        order.setPaymentExpiresAt(Instant.now().minusSeconds(3600));
        Payment payment = pendingPayment(order, PaymentProvider.VNPAY);
        stubAdminUpdateLookup(payment, order);

        PaymentCallbackResponse response = paymentService.updatePayment(
                payment.getId(),
                new PaymentCallbackRequest(order.getId(), PaymentProvider.VNPAY, null, PaymentStatus.SUCCESS));

        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertNotNull(response.transactionNo());
        verify(orderLifecycleService).confirmAfterSuccessfulPayment(order);
        verify(orderLifecycleService, never()).cancelOrder(any(), any());
    }

    // Don da giao thi khong cho sua payment nua, ke ca chi sua provider.
    @Test
    void updatePayment_rejectsShippedOrder() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);
        Payment payment = pendingPayment(order, PaymentProvider.COD);
        stubAdminUpdateLookup(payment, order);

        assertThrows(BusinessRuleException.class, () ->
                paymentService.updatePayment(
                        payment.getId(),
                        new PaymentCallbackRequest(order.getId(), PaymentProvider.MOMO, null, PaymentStatus.PENDING)));
    }

    // MoMo: khach thanh toan xong la SUCCESS ngay, don duoc xac nhan, khong cho admin duyet.
    @Test
    void createPayment_momoSucceedsImmediately() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        Payment payment = pendingPayment(order, PaymentProvider.MOMO);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(repo.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())).thenReturn(Optional.of(payment));
        when(repo.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentCallbackResponse response = paymentService.createPayment(
                7, new PaymentCreateRequest(order.getId(), PaymentProvider.MOMO), "127.0.0.1");

        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertNotNull(response.transactionNo());
        verify(orderLifecycleService).confirmAfterSuccessfulPayment(order);
    }

    // COD: tao payment xong van PENDING de cho admin duyet.
    @Test
    void createPayment_codStaysPendingForAdminApproval() {
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
