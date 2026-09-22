package org.akira.ladux.ordering.controller.admin;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.ordering.dto.response.OrderHistoryResponse;
import org.akira.ladux.ordering.service.OrderHistoryService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/order-histories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderHistoryController {
    private final OrderHistoryService service;

    @GetMapping
    public ResponseEntity<PageResponse<OrderHistoryResponse>> getAllOrderHistories(Pageable pageable) {
        return ResponseEntity.ok(service.getAllOrderHistories(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderHistoryResponse> getOrderHistoryById(@PathVariable int id) {
        return ResponseEntity.ok(service.getOrderHistoryById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PageResponse<OrderHistoryResponse>> getOrderHistoriesByOrderId(@PathVariable int orderId, Pageable pageable) {
        return ResponseEntity.ok(service.getOrderHistoriesByOrderId(orderId, pageable));
    }
}
