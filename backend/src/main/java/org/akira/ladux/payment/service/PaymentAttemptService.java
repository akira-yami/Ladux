package org.akira.ladux.payment.service;

import java.math.BigDecimal;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.enums.PaymentProvider;

public interface PaymentAttemptService {
    void initializePayment(Order order, PaymentProvider provider, BigDecimal amount);

    PaymentCallbackResponse retryPayment(int userId, int orderId, String clientIp);
}

