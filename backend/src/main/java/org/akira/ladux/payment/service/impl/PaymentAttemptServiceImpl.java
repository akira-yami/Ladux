package org.akira.ladux.payment.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.service.PaymentAttemptService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Quan ly vong doi payment attempt gan voi don hang.
// initializePayment reserves the configured payment window; COD has no timeout.
// retryPayment resumes PENDING or creates a new attempt after FAILED within that window.
@Service
@RequiredArgsConstructor
public class PaymentAttemptServiceImpl implements PaymentAttemptService {
    private final org.akira.ladux.payment.service.PaymentService paymentService;
    private final org.akira.ladux.payment.config.VNPayProperties vnPayProperties;

    @Override
    public void initializePayment(Order order, PaymentProvider provider, BigDecimal amount) {
        if (provider != PaymentProvider.COD && provider != PaymentProvider.VNPAY)
            throw new org.akira.ladux.shared.exception.BusinessRuleException("Phương thức thanh toán chưa được hỗ trợ");
        if (provider == PaymentProvider.VNPAY && (amount == null || amount.signum() <= 0))
            throw new org.akira.ladux.shared.exception.BusinessRuleException("VNPay yêu cầu số tiền thanh toán lớn hơn 0");
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
        // Resume an existing pending URL, or create one new attempt after a confirmed failure.
        return paymentService.createPayment(userId,
                new org.akira.ladux.payment.dto.request.PaymentCreateRequest(orderId, PaymentProvider.VNPAY), clientIp);
    }

    private Instant paymentExpiresAt(PaymentProvider provider) {
        return provider == PaymentProvider.COD ? null : Instant.now().plusSeconds(Math.max(1, vnPayProperties.getExpireMinutes()) * 60L);
    }
}

