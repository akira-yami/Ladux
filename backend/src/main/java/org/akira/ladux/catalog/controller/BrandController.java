package org.akira.ladux.catalog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.BrandRequest;
import org.akira.ladux.catalog.dto.response.BrandResponse;
import org.akira.ladux.catalog.service.BrandService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/brands", "/api/v1/admin/brands"})
@RequiredArgsConstructor
public class BrandController {

    private final BrandService service;

    @GetMapping
    public ResponseEntity<PageResponse<BrandResponse>> getAllBrands(Pageable pageable) {
        return ResponseEntity.ok(service.getAllBrands(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandResponse> getBrandById(@PathVariable int id) {
        return ResponseEntity.ok(service.getBrandById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> getBrandByName(@RequestParam String name) {
        BrandResponse response = service.getBrandByName(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> getBrandBySlug(@PathVariable String slug) {
        BrandResponse response = service.getBrandBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> createBrand(@Valid @RequestBody BrandRequest request) {
        BrandResponse response = service.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> updateBrand(@PathVariable int id, @Valid @RequestBody BrandRequest brand) {
        BrandResponse response = service.updateBrand(id, brand);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBrandById(@PathVariable int id) {
        service.deleteBrandById(id);
        return ResponseEntity.noContent().build();
    }
}
