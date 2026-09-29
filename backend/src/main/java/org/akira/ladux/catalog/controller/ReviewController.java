package org.akira.ladux.catalog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.ReviewCreateRequest;
import org.akira.ladux.catalog.dto.request.ReviewUpdateRequest;
import org.akira.ladux.catalog.dto.response.ReviewResponse;
import org.akira.ladux.catalog.service.ReviewService;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/reviews", "/api/v1/admin/reviews"})
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReviewResponse>> getAllReviews(Pageable pageable) {
        return ResponseEntity.ok(service.getAllReviews(pageable));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByProductId(@PathVariable int productId, Pageable pageable) {
        return ResponseEntity.ok(service.getReviewsByProductId(productId, pageable));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByUserId(@PathVariable int userId, Pageable pageable) {
        return ResponseEntity.ok(service.getReviewsByUserId(userId, pageable));
    }

    @GetMapping("/search/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReviewResponse>> searchReviewsByProductId(@PathVariable int productId, Pageable pageable) {
        return ResponseEntity.ok(service.getReviewsByProductId(productId, pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReviewResponse>> findReviewByNameUser(@RequestParam(required = false) String name, Pageable pageable) {
        return ResponseEntity.ok(service.findReviewByNameUser(name, pageable));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return new ResponseEntity<>(service.createReview(principal.getId(), request), HttpStatus.CREATED);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateReview(principal.getId(), reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReviewById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable int reviewId
    ) {
        service.deleteReviewById(principal.getId(), reviewId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
