package org.akira.ladux.catalog.controller.admin;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.response.ReviewResponse;
import org.akira.ladux.catalog.service.ReviewService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {
    private final ReviewService service;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> getAllReviews(Pageable pageable) {
        return ResponseEntity.ok(service.getAllReviews(pageable));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByUserId(@PathVariable int userId, Pageable pageable) {
        return ResponseEntity.ok(service.getReviewsByUserId(userId, pageable));
    }
    @GetMapping("/search/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByProductId(@PathVariable int productId, Pageable pageable) {
        return ResponseEntity.ok(service.getReviewsByProductId(productId, pageable));
    }
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReviewResponse>> findReviewByNameUser(@RequestParam(required = false) String name, Pageable pageable) {
        return ResponseEntity.ok(service.findReviewByNameUser(name, pageable));
    }
}
