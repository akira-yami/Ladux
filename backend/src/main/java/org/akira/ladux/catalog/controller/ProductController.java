package org.akira.ladux.catalog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.ProductRequest;
import org.akira.ladux.catalog.dto.response.ProductResponse;
import org.akira.ladux.catalog.dto.response.ProductVariantResponse;
import org.akira.ladux.catalog.service.ProductService;
import org.akira.ladux.catalog.service.ProductVariantService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/products", "/api/v1/admin/products"})
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final org.akira.ladux.catalog.service.PublicCatalogService publicCatalog;

    @GetMapping("/facets")
    public ResponseEntity<org.akira.ladux.catalog.dto.response.CatalogFacetsResponse> facets() {
        return ResponseEntity.ok(publicCatalog.facets());
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(value = "search", required = false) String search,
            @Valid @ModelAttribute org.akira.ladux.catalog.dto.request.PublicCatalogFilter filter,
            jakarta.servlet.http.HttpServletRequest request,
            Pageable pageable
    ) {
        if (!request.getRequestURI().startsWith("/api/v1/admin/"))
            return ResponseEntity.ok(publicCatalog.search(filter, pageable));
        return ResponseEntity.ok(search == null || search.isBlank()
                ? productService.getAllProducts(pageable)
                : productService.searchProducts(search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable int id, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(request.getRequestURI().startsWith("/api/v1/admin/")
                ? productService.getProductById(id) : publicCatalog.detail(id));
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<PageResponse<ProductResponse>> getProductsByBrandId(
            @PathVariable int brandId,
            jakarta.servlet.http.HttpServletRequest request,
            Pageable pageable) {
        if (!request.getRequestURI().startsWith("/api/v1/admin/"))
            return ResponseEntity.ok(publicCatalog.search(new org.akira.ladux.catalog.dto.request.PublicCatalogFilter(
                    null, null, brandId, null, null, null, null, null), pageable));
        return ResponseEntity.ok(productService.getProductsByBrandId(brandId, pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<PageResponse<ProductResponse>> getProductsByCategoryId(
            @PathVariable int categoryId,
            jakarta.servlet.http.HttpServletRequest request,
            Pageable pageable) {
        if (!request.getRequestURI().startsWith("/api/v1/admin/"))
            return ResponseEntity.ok(publicCatalog.search(new org.akira.ladux.catalog.dto.request.PublicCatalogFilter(
                    null, null, null, categoryId, null, null, null, null), pageable));
        return ResponseEntity.ok(productService.getProductsByCategoryId(categoryId, pageable));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<ProductVariantResponse> getProductVariantById(
            @PathVariable Integer variantId, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(request.getRequestURI().startsWith("/api/v1/admin/")
                ? productVariantService.getProductVariantById(variantId) : publicCatalog.variant(variantId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable int id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductById(@PathVariable int id) {
        productService.deleteProductById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
