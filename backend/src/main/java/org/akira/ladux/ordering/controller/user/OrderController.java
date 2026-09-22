package org.akira.ladux.ordering.controller.user;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.ordering.dto.request.OrderRequest;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.service.OrderService;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// API don hang cho khach hang (CUSTOMER).
// userId luon lay tu UserPrincipal — khong tin tham so client (chong IDOR).
// POST /orders doc gio hang server-side, client chi gui dia chi + coupon + provider.
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    // Chi tiet mot don — chi chu don moi xem duoc (kiem tra o service layer).
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@AuthenticationPrincipal UserPrincipal principal, @PathVariable int orderId) {
        return ResponseEntity.ok(service.getOrderById(principal.getId(), orderId));
    }

    @GetMapping("/user")
    public ResponseEntity<PageResponse<OrderResponse>> getOrdersByUserId(@AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return ResponseEntity.ok(service.getOrdersByUserId(principal.getId(), pageable));
    }

    // Tao don tu gio hang hien tai — logic nghiep vu o OrderService.createOrder.
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody OrderRequest request) {
        return new ResponseEntity<>(service.createOrder(principal.getId(), request), HttpStatus.CREATED);
    }

    // Hủy đơn hàng
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Integer orderId) {
        service.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/payments/retry")
    public ResponseEntity<PaymentCallbackResponse> retryPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int orderId,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String clientIp = org.akira.ladux.shared.util.ClientIpUtils.getClientIp(httpRequest);
        return new ResponseEntity<>(service.retryPayment(principal.getId(), orderId, clientIp), HttpStatus.CREATED);
    }

    // Gửi yêu cầu trả hàng
    @PostMapping("/{orderId}/request-return")
    public ResponseEntity<OrderResponse> requestReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int orderId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = (body != null) ? body.get("reason") : null;
        return ResponseEntity.ok(service.requestReturn(principal.getId(), orderId, reason));
    }
}
