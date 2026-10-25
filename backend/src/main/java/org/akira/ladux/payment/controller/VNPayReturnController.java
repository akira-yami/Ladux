package org.akira.ladux.payment.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.service.impl.VNPayQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class VNPayReturnController {
    private final VNPayQueryService service;

    @PostMapping("/vnpay/verify-return")
    public PaymentCallbackResponse verifyReturn(@AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> params) {
        return service.verifyReturn(principal.getId(), params);
    }

    @PostMapping("/my/txn-ref/{ref}/reconcile")
    public PaymentCallbackResponse reconcile(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String ref) {
        return service.reconcile(principal.getId(), ref);
    }
}
