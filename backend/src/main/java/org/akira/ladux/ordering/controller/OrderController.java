package org.akira.ladux.ordering.controller;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.ordering.dto.request.OrderRequest;
import org.akira.ladux.ordering.dto.request.OrderStatusUpdateRequest;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.service.OrderService;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @GetMapping("/api/v1/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int orderId) {
        return ResponseEntity.ok(service.getOrderById(principal.getId(), orderId));
    }

    @GetMapping("/api/v1/orders/user")
    public ResponseEntity<PageResponse<OrderResponse>> getOrdersByUserId(
            @AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return ResponseEntity.ok(service.getOrdersByUserId(principal.getId(), pageable));
    }

    @PostMapping("/api/v1/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OrderRequest request) {
        return new ResponseEntity<>(service.createOrder(principal.getId(), request), HttpStatus.CREATED);
    }

    @DeleteMapping("/api/v1/orders/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer orderId) {
        service.cancelOrder(principal.getId(), orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/orders/{orderId}/payments/retry")
    public ResponseEntity<PaymentCallbackResponse> retryPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int orderId,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientIp = org.akira.ladux.shared.util.ClientIpUtils.getClientIp(httpRequest);
        return new ResponseEntity<>(service.retryPayment(principal.getId(), orderId, clientIp), HttpStatus.CREATED);
    }

    @PostMapping("/api/v1/orders/{orderId}/request-return")
    public ResponseEntity<OrderResponse> requestReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int orderId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(service.requestReturn(principal.getId(), orderId, reason));
    }

    @GetMapping("/api/v1/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(service.getAllOrders(pageable));
    }

    @GetMapping("/api/v1/admin/orders/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status, Pageable pageable) {
        return ResponseEntity.ok(service.getOrdersByStatus(status, pageable));
    }

    @GetMapping("/api/v1/admin/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> getOrderByIdForAdmin(@PathVariable int orderId) {
        return ResponseEntity.ok(service.getOrderByIdForAdmin(orderId));
    }

    @PatchMapping("/api/v1/admin/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable int orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateOrderStatus(orderId, request));
    }
}
