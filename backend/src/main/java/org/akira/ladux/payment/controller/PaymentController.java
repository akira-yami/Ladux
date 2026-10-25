package org.akira.ladux.payment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.payment.dto.request.PaymentCallbackRequest;
import org.akira.ladux.payment.dto.request.PaymentCreateRequest;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.service.PaymentService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;
    private final org.akira.ladux.payment.service.RefundWorkflowService refundWorkflowService;

    @GetMapping("/api/v1/admin/payments/refunds/attention")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<org.akira.ladux.payment.dto.response.RefundAttemptResponse>> refundAttention(Pageable pageable) {
        return ResponseEntity.ok(refundWorkflowService.attention(pageable));
    }

    @PostMapping("/api/v1/admin/payments/refunds/{attemptId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> confirmRefund(@PathVariable long attemptId,
            @Valid @RequestBody org.akira.ladux.payment.dto.request.RefundResolutionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(refundWorkflowService.confirmReconciled(attemptId, request.reference(), principal.getId()));
    }

    @GetMapping("/api/v1/payments/my")
    public ResponseEntity<PageResponse<PaymentCallbackResponse>> getMyPayments(
            @AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return ResponseEntity.ok(service.getMyPayments(principal.getId(), pageable));
    }

    @GetMapping("/api/v1/payments/my/order/{orderId}")
    public ResponseEntity<PageResponse<PaymentCallbackResponse>> getMyPaymentsByOrderId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int orderId,
            Pageable pageable) {
        return ResponseEntity.ok(service.getMyPaymentsByOrderId(principal.getId(), orderId, pageable));
    }

    @GetMapping("/api/v1/payments/my/status/{status}")
    public ResponseEntity<PageResponse<PaymentCallbackResponse>> getMyPaymentsByStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable PaymentStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(service.getMyPaymentsByStatus(principal.getId(), status, pageable));
    }

    @PostMapping("/api/v1/payments")
    public ResponseEntity<PaymentCallbackResponse> createPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentCreateRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = org.akira.ladux.shared.util.ClientIpUtils.getClientIp(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createPayment(principal.getId(), request, clientIp));
    }

    @GetMapping("/api/v1/payments/my/txn-ref/{merchantTxnRef}")
    public ResponseEntity<PaymentCallbackResponse> getMyPaymentByMerchantTxnRef(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String merchantTxnRef) {
        return ResponseEntity.ok(service.getMyPaymentByMerchantTxnRef(principal.getId(), merchantTxnRef));
    }

    @GetMapping("/api/v1/admin/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentCallbackResponse>> getAllPayments(Pageable pageable,
            @RequestParam(required = false) Integer orderId, @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "false") boolean reviewOnly) {
        return ResponseEntity.ok(service.searchPayments(orderId, status, reviewOnly, pageable));
    }

    @GetMapping("/api/v1/admin/payments/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentCallbackResponse>> getPaymentsByOrderId(
            @PathVariable int orderId, Pageable pageable) {
        return ResponseEntity.ok(service.getPaymentsByOrderId(orderId, pageable));
    }

    @GetMapping("/api/v1/admin/payments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentCallbackResponse> getPaymentById(@PathVariable int id) {
        return ResponseEntity.ok(service.getPaymentById(id));
    }

    @GetMapping("/api/v1/admin/payments/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentCallbackResponse>> getPaymentsByStatus(
            @PathVariable PaymentStatus status, Pageable pageable) {
        return ResponseEntity.ok(service.getPaymentsByStatus(status, pageable));
    }

    @PutMapping("/api/v1/admin/payments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentCallbackResponse> updatePayment(
            @PathVariable int id, @Valid @RequestBody PaymentCallbackRequest request) {
        return ResponseEntity.ok(service.updatePayment(id, request));
    }

    @PostMapping("/api/v1/admin/payments/order/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> processRefund(
            @PathVariable int orderId,
            @Valid @RequestBody(required = false) org.akira.ladux.payment.dto.request.RefundRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {
        BigDecimal amount = body == null ? null : body.amount();
        String reason = body == null || body.reason() == null ? "Hoàn tiền bởi Admin" : body.reason();
        return ResponseEntity.ok(service.processRefund(orderId, body == null ? null : body.paymentId(), amount, reason, principal.getId()));
    }
}
