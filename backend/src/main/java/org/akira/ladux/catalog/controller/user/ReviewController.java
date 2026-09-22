package org.akira.ladux.catalog.controller.user;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByProductId(@PathVariable int productId, Pageable pageable) {
        return ResponseEntity.ok(service.getReviewsByProductId(productId, pageable));
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
