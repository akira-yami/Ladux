package org.akira.ladux.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.inventory.dto.request.StockMovementRequest;
import org.akira.ladux.inventory.dto.response.StockMovementResponse;
import org.akira.ladux.inventory.service.StockMovementService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/stock-movements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StockMovementController {

    private final StockMovementService service;

    @GetMapping
    public ResponseEntity<PageResponse<StockMovementResponse>> getAllStockMovements(Pageable pageable) {
        return ResponseEntity.ok(service.getAllMovements(pageable));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<StockMovementResponse>> getByProduct(
            @PathVariable int productId, Pageable pageable) {
        return ResponseEntity.ok(service.getMovementsByProduct(productId, pageable));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockMovementResponse> createAdjustment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StockMovementRequest request) {
        return new ResponseEntity<>(service.createAdjustment(request, principal.getId()), HttpStatus.CREATED);
    }
}
