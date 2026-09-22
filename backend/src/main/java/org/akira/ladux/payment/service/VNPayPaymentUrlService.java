package org.akira.ladux.payment.service;

import org.akira.ladux.payment.entity.Payment;

public interface VNPayPaymentUrlService {

    String createPaymentUrl(
            Payment payment,
            String clientIp
    );
}