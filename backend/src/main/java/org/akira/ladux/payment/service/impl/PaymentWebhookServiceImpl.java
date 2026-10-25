package org.akira.ladux.payment.service.impl;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.dto.PaymentWebhookResult;
import org.akira.ladux.payment.service.PaymentWebhookService;
import org.akira.ladux.payment.util.VNPayUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {
    private final VNPayProperties vnPayProperties;
    private final VNPaySettlementService settlementService;

    @Override
    public PaymentWebhookResult processVNPayWebhook(Map<String, String> params) {
        if (!VNPayUtils.verify(params, vnPayProperties.getHashSecret())
                || !java.util.Objects.equals(vnPayProperties.getTmnCode(), params.get("vnp_TmnCode")))
            return PaymentWebhookResult.invalidSignature();
        // Commit settlement before acknowledging VNPay. DB errors must be retryable.
        return settlementService.settle(params);
    }
}
