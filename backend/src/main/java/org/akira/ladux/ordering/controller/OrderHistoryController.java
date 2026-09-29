package org.akira.ladux.ordering.controller;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.ordering.dto.response.OrderHistoryResponse;
import org.akira.ladux.ordering.service.OrderHistoryService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderHistoryController {

    private final OrderHistoryService service;

    @GetMapping("/api/v1/order-histories/my")
    public ResponseEntity<PageResponse<OrderHistoryResponse>> getOrdersHistoryByUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal, Pageable pageable) {
        return ResponseEntity.ok(service.getOrdersHistoryByUser(userPrincipal.getId(), pageable));
    }

    @GetMapping("/api/v1/admin/order-histories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderHistoryResponse>> getAllOrderHistories(Pageable pageable) {
        return ResponseEntity.ok(service.getAllOrderHistories(pageable));
    }

    @GetMapping("/api/v1/admin/order-histories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderHistoryResponse> getOrderHistoryById(@PathVariable int id) {
        return ResponseEntity.ok(service.getOrderHistoryById(id));
    }

    @GetMapping("/api/v1/admin/order-histories/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderHistoryResponse>> getOrderHistoriesByOrderId(
            @PathVariable int orderId, Pageable pageable) {
        return ResponseEntity.ok(service.getOrderHistoriesByOrderId(orderId, pageable));
    }
}
