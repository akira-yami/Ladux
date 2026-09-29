package org.akira.ladux.ordering.controller;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.ordering.dto.response.OrderItemResponse;
import org.akira.ladux.ordering.service.OrderItemService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/order-items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OrderItemController {

    private final OrderItemService service;

    @GetMapping
    public ResponseEntity<PageResponse<OrderItemResponse>> getAllOrderItems(Pageable pageable) {
        return ResponseEntity.ok(service.getAllOrderItems(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderItemResponse> getOrderItemById(@PathVariable int id) {
        return ResponseEntity.ok(service.getOrderItemById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PageResponse<OrderItemResponse>> getOrderItemsByOrderId(
            @PathVariable int orderId, Pageable pageable) {
        return ResponseEntity.ok(service.getOrderItemsByOrderId(orderId, pageable));
    }
}
