package org.akira.ladux.catalog.controller;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.response.ProductImageResponse;
import org.akira.ladux.catalog.service.ProductImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/products/{productId}/images", "/api/v1/admin/products/{productId}/images"})
@RequiredArgsConstructor
@Validated
public class ProductImageController {

    private final ProductImageService service;

    @PostMapping(value = {"", "/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductImageResponse>> uploadProductImage(
            @PathVariable int productId,
            @RequestParam(value = "file", required = false) List<MultipartFile> files,
            @RequestParam(value = "files", required = false) List<MultipartFile> filesAlt,
            @RequestParam(value = "imageUrls", required = false) List<String> imageUrls
    ) {
        List<MultipartFile> allFiles = new ArrayList<>();
        if (files != null) allFiles.addAll(files);
        if (filesAlt != null) allFiles.addAll(filesAlt);
        return new ResponseEntity<>(service.uploadImage(productId, allFiles, imageUrls), HttpStatus.CREATED);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductImageById(@PathVariable int productId, @PathVariable int imageId) {
        service.deleteProductImageById(productId, imageId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
