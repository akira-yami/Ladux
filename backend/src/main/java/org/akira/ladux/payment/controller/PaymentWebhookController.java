package org.akira.ladux.payment.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.payment.dto.PaymentWebhookResult;
import org.akira.ladux.payment.service.PaymentWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public VNPay webhook; request authenticity is checked by the service HMAC validation. */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @RequestMapping(value = "/vnpay-webhook", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> handleVNPayWebhook(@RequestParam Map<String, String> params) {
        PaymentWebhookResult result = paymentWebhookService.processVNPayWebhook(params);
        return ResponseEntity.ok(String.format(
                "{\"RspCode\":\"%s\",\"Message\":\"%s\"}",
                result.vnpResponseCode(), result.message()));
    }
}
