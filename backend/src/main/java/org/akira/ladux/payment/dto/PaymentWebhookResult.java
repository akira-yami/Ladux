package org.akira.ladux.payment.dto;

/**
 * Ket qua xu ly webhook, dung de Controller map sang HTTP status + body VNPay.
 */
public record PaymentWebhookResult(Outcome outcome, String vnpResponseCode, String message) {

    public enum Outcome {
        /** Webhook hop le, da xu ly lan dau. */
        PROCESSED,
        /** Webhook trung (da SUCCESS hoac race condition unique constraint) — tra 200 de Gateway dung retry. */
        IDEMPOTENT,
        /** Chu ky HMAC khong hop le — tu choi ngay. */
        INVALID_SIGNATURE,
        /** So tien Gateway khong khop don hang — tu choi va canh bao van hanh. */
        AMOUNT_MISMATCH,
        /** Khong tim thay don hang / payment tuong ung. */
        ORDER_NOT_FOUND,
        RETRYABLE_ERROR
    }

    public static PaymentWebhookResult processed() {
        return new PaymentWebhookResult(Outcome.PROCESSED, "00", "Confirm Success");
    }

    public static PaymentWebhookResult idempotent() {
        return new PaymentWebhookResult(Outcome.IDEMPOTENT, "02", "Order already confirmed");
    }

    public static PaymentWebhookResult invalidSignature() {
        return new PaymentWebhookResult(Outcome.INVALID_SIGNATURE, "97", "Invalid Checksum");
    }

    public static PaymentWebhookResult amountMismatch(String detail) {
        return new PaymentWebhookResult(Outcome.AMOUNT_MISMATCH, "04", detail);
    }

    public static PaymentWebhookResult orderNotFound() {
        return new PaymentWebhookResult(Outcome.ORDER_NOT_FOUND, "01", "Order not found");
    }

    public static PaymentWebhookResult retryable() {
        return new PaymentWebhookResult(Outcome.RETRYABLE_ERROR, "99", "Unable to confirm payment");
    }
}
