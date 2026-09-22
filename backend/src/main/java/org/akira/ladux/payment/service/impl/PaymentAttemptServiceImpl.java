package org.akira.ladux.payment.service.impl;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.mapper.PaymentCallbackMapper;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.service.PaymentAttemptService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Quan ly vong doi payment attempt gan voi don hang.
// initializePayment: goi tu createOrder — tao Payment PENDING + set paymentExpiresAt (15 phut, tru COD).
// retryPayment: chi khi lan thanh toan gan nhat FAILED — tao attempt PENDING moi.
@Service
@RequiredArgsConstructor
public class PaymentAttemptServiceImpl implements PaymentAttemptService {
    // Thoi gian cho thanh toan truoc khi job tu huy don PENDING.
    private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(15);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final org.akira.ladux.payment.service.VNPayPaymentUrlService vnPayPaymentUrlService;

    @Override
    public void initializePayment(Order order, PaymentProvider provider, BigDecimal amount) {
        // COD không có hạn thanh toán — khách trả khi nhận hàng.
        order.setPaymentExpiresAt(paymentExpiresAt(provider));
        Payment payment = Payment.builder()
                .order(order)
                .provider(provider)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();
        if (provider == PaymentProvider.VNPAY) {
            payment.setMerchantTxnRef("LDX" + System.currentTimeMillis() + java.util.UUID.randomUUID().toString().substring(0, 4));
        }
        order.getPayments().add(payment);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "payments", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true)
    })
    public PaymentCallbackResponse retryPayment(int userId, int orderId, String clientIp) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay order voi id = " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Don hang khong con o trang thai co the thanh toan lai");
        }

        if (order.getUser().getId() != userId) {
            throw new BusinessRuleException("Khong the thu lai thanh toan cho don hang cua nguoi khac");
        }

        Payment lastPayment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new BusinessRuleException("Don hang chua co lan thanh toan nao de thu lai"));

        if (lastPayment.getStatus() != PaymentStatus.FAILED) {
            throw new BusinessRuleException("Chi co the thanh toan lai khi lan thanh toan gan nhat FAILED");
        }

        Payment retry = Payment.builder()
                .order(order)
                .amount(order.getFinalAmount())
                .status(PaymentStatus.PENDING)
                .provider(lastPayment.getProvider())
                .build();

        retry = paymentRepository.save(retry);

        if (retry.getProvider() == PaymentProvider.VNPAY) {
            retry.setMerchantTxnRef(generateMerchantTxnRef(order, retry));
            retry.setPaymentUrl(vnPayPaymentUrlService.createPaymentUrl(retry, clientIp));
            retry = paymentRepository.save(retry);
        }

        return PaymentCallbackMapper.fromEntity(retry);
    }

    private String generateMerchantTxnRef(Order order, Payment payment) {
        return "LDX" + order.getId() + payment.getId() + System.currentTimeMillis();
    }

    private Instant paymentExpiresAt(PaymentProvider provider) {
        return provider == PaymentProvider.COD ? null : Instant.now().plus(PAYMENT_TIMEOUT);
    }
}

