package org.akira.ladux.ordering.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.ordering.dto.request.OrderStatusUpdateRequest;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.service.OrderService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(service.getAllOrders(pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status, Pageable pageable) {
        return ResponseEntity.ok(service.getOrdersByStatus(status, pageable));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable int orderId) {
        return ResponseEntity.ok(service.getOrderByIdForAdmin(orderId));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable int orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateOrderStatus(orderId, request));
    }
}
