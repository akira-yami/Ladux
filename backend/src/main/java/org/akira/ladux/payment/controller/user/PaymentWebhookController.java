package org.akira.ladux.payment.controller.user;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.payment.dto.PaymentWebhookResult;
import org.akira.ladux.payment.service.PaymentWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public webhook endpoint — KHONG yeu cau cookie/JWT auth.
 * Bao mat duoc dam bao bang HMAC signature validation o tang Service (khong phai auth cookie).
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    /**
     * VNPay IPN (Instant Payment Notification).
     * Ho tro ca GET va POST vi Gateway co the gui theo mot trong hai phuong thuc tuy cau hinh.
     */
    @RequestMapping(value = "/vnpay-webhook", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public ResponseEntity<String> handleVNPayWebhook(@RequestParam Map<String, String> params) {
        PaymentWebhookResult result = paymentWebhookService.processVNPayWebhook(params);
        return toVNPayResponse(result);
    }

    /**
     * Map ket qua service sang HTTP status + body JSON chuan VNPay.
     *
     * - PROCESSED / IDEMPOTENT -> 200 OK (Gateway dung retry khi nhan 200)
     * - INVALID_SIGNATURE      -> 403 Forbidden (fake request)
     * - AMOUNT_MISMATCH        -> 400 Bad Request (du lieu bat thuong, can dieu tra)
     * - ORDER_NOT_FOUND        -> 404 Not Found
     */
    private ResponseEntity<String> toVNPayResponse(PaymentWebhookResult result) {
        String body = String.format(
                "{\"RspCode\":\"%s\",\"Message\":\"%s\"}",
                result.vnpResponseCode(),
                result.message()
        );

        return ResponseEntity.ok(body);
    }
}