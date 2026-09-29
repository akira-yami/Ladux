package org.akira.ladux.procurement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.procurement.dto.request.AdminPurchaseOrderReceiveRequest;
import org.akira.ladux.procurement.dto.request.PurchaseOrderCreateRequest;
import org.akira.ladux.procurement.dto.request.PurchaseOrderStatusUpdateRequest;
import org.akira.ladux.procurement.dto.response.PurchaseOrderResponse;
import org.akira.ladux.procurement.enums.PurchaseOrderStatus;
import org.akira.ladux.procurement.service.PurchaseOrderService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @GetMapping
    public ResponseEntity<PageResponse<PurchaseOrderResponse>> getAllPurchaseOrders(Pageable pageable) {
        return ResponseEntity.ok(service.getAllPurchaseOrders(pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<PageResponse<PurchaseOrderResponse>> getByStatus(
            @PathVariable PurchaseOrderStatus status, Pageable pageable) {
        return ResponseEntity.ok(service.getPurchaseOrdersByStatus(status, pageable));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<PageResponse<PurchaseOrderResponse>> getBySupplier(
            @PathVariable int supplierId, Pageable pageable) {
        return ResponseEntity.ok(service.getPurchaseOrdersBySupplier(supplierId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getPurchaseOrderById(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PurchaseOrderCreateRequest request) {
        return new ResponseEntity<>(service.createPurchaseOrder(request, principal.getId()), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PurchaseOrderResponse> updateStatus(
            @PathVariable int id, @Valid @RequestBody PurchaseOrderStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrderResponse> receive(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int id,
            @Valid @RequestBody AdminPurchaseOrderReceiveRequest request) {
        return ResponseEntity.ok(service.receiveGoods(id, request, principal.getId()));
    }
}
