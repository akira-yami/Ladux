package org.akira.ladux.catalog.controller.admin;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.ProductVariantRequest;
import org.akira.ladux.catalog.dto.response.ProductVariantResponse;
import org.akira.ladux.catalog.service.ProductVariantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/product-variants")
@RequiredArgsConstructor
public class AdminProductVariantController {
    private final ProductVariantService service;

    // Thêm ProductVariant
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> addProductVariant(@RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = service.addProductVariant(
                request.productId(),
                request.colorId(),
                request.ram(),
                request.rom(),
                request.price(),
                request.discountPrice(),
                request.stockQuantity(),
                request.isActive()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> updateProductVariant(@PathVariable Integer id, @RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = service.updateProductVariant(
                id,
                request.colorId(),
                request.ram(),
                request.rom(),
                request.price(),
                request.discountPrice(),
                request.stockQuantity(),
                request.isActive()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductVariant(@PathVariable Integer id) {
        service.deleteProductVariant(id);
        return ResponseEntity.noContent().build();
    }
}
