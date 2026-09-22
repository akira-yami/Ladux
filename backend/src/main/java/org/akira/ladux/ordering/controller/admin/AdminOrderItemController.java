package org.akira.ladux.ordering.controller.admin;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.ordering.dto.response.OrderItemResponse;
import org.akira.ladux.ordering.service.OrderItemService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/order-items")
@RequiredArgsConstructor
public class AdminOrderItemController {
    private final OrderItemService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderItemResponse>> getAllOrderItems(Pageable pageable) {
        return ResponseEntity.ok(service.getAllOrderItems(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderItemResponse> getOrderItemById(@PathVariable int id) {
        return ResponseEntity.ok(service.getOrderItemById(id));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderItemResponse>> getOrderItemsByOrderId(@PathVariable int orderId, Pageable pageable) {
        return ResponseEntity.ok(service.getOrderItemsByOrderId(orderId, pageable));
    }
}
