package org.akira.ladux.payment.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akira.ladux.payment.dto.PaymentWebhookResult;
import org.akira.ladux.payment.service.PaymentWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/** Public VNPay webhook; request authenticity is checked by the service HMAC validation. */
@RestController
@Slf4j
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @RequestMapping(value = "/vnpay-webhook", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> handleVNPayWebhook(@RequestParam MultiValueMap<String, String> params) {
        PaymentWebhookResult result;
        try {
            result = params.entrySet().stream().anyMatch(e -> e.getKey().startsWith("vnp_") && e.getValue().size() != 1)
                    ? PaymentWebhookResult.retryable()
                    : paymentWebhookService.processVNPayWebhook(params.toSingleValueMap());
        } catch (RuntimeException ex) {
            log.warn("[VNPAY] IPN settlement failed ({})", ex.getClass().getSimpleName());
            result = PaymentWebhookResult.retryable();
        }
        return ResponseEntity.ok(Map.of("RspCode", result.vnpResponseCode(), "Message", result.message()));
    }
}
