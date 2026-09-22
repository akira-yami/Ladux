package org.akira.ladux.payment.service;

import java.util.Map;
import org.akira.ladux.payment.dto.PaymentWebhookResult;

/**
 * Service chuyen xu ly webhook tu Payment Gateway (IPN/Callback).
 * Tach rieng khoi PaymentService CRUD de tap trung security, idempotency va state machine.
 */
public interface PaymentWebhookService {

    /**
     * Xu ly IPN/Callback tu VNPay.
     *
     * @param params toan bo query params Gateway gui sang (bao gom vnp_SecureHash)
     * @return ket qua xu ly de Controller map HTTP response
     */
    PaymentWebhookResult processVNPayWebhook(Map<String, String> params);
}