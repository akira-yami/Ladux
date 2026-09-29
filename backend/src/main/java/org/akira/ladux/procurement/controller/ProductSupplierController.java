package org.akira.ladux.procurement.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.procurement.dto.request.ProductSupplierRequest;
import org.akira.ladux.procurement.dto.response.ProductSupplierResponse;
import org.akira.ladux.procurement.service.ProductSupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/product-suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductSupplierController {

    private final ProductSupplierService service;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductSupplierResponse>> getByProduct(@PathVariable int productId) {
        return ResponseEntity.ok(service.getByProduct(productId));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<ProductSupplierResponse>> getBySupplier(@PathVariable int supplierId) {
        return ResponseEntity.ok(service.getBySupplier(supplierId));
    }

    @PostMapping
    public ResponseEntity<ProductSupplierResponse> link(@Valid @RequestBody ProductSupplierRequest request) {
        return new ResponseEntity<>(service.link(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductSupplierResponse> update(
            @PathVariable long id, @Valid @RequestBody ProductSupplierRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unlink(@PathVariable long id) {
        service.unlink(id);
        return ResponseEntity.noContent().build();
    }
}
