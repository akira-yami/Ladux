package org.akira.ladux.catalog.controller.user;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.response.ProductResponse;
import org.akira.ladux.catalog.dto.response.ProductVariantResponse;
import org.akira.ladux.catalog.service.ProductService;
import org.akira.ladux.catalog.service.ProductVariantService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(value = "search", required = false) String search,
            Pageable pageable
    ) {
        return ResponseEntity.ok(search == null || search.isBlank()
                ? productService.getAllProducts(pageable)
                : productService.searchProducts(search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable int id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<PageResponse<ProductResponse>> getProductsByBrandId(
            @PathVariable int brandId,
            Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsByBrandId(brandId, pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<PageResponse<ProductResponse>> getProductsByCategoryId(
            @PathVariable int categoryId,
            Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsByCategoryId(categoryId, pageable));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<ProductVariantResponse> getProductVariantById(
            @PathVariable Integer variantId) {
        return ResponseEntity.ok(productVariantService.getProductVariantById(variantId));
    }
}
