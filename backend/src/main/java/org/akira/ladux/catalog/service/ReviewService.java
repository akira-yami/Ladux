package org.akira.ladux.catalog.service;

import org.akira.ladux.catalog.dto.request.ReviewCreateRequest;
import org.akira.ladux.catalog.dto.request.ReviewUpdateRequest;
import org.akira.ladux.catalog.dto.response.ReviewResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    PageResponse<ReviewResponse> getAllReviews(Pageable pageable);

    ReviewResponse getReviewById(int id);

    PageResponse<ReviewResponse> getReviewsByProductId(int productId, Pageable pageable);

    PageResponse<ReviewResponse> getReviewsByUserId(int userId, Pageable pageable);

    ReviewResponse createReview(int userId, ReviewCreateRequest request);

    ReviewResponse updateReview(int userId, int reviewId, ReviewUpdateRequest request);

    void deleteReviewById(int userId, int reviewId);

    PageResponse<ReviewResponse> findReviewByNameUser(String name, Pageable pageable);
}
