package org.akira.ladux.catalog.mapper;

import org.akira.ladux.catalog.dto.response.ReviewResponse;
import org.akira.ladux.catalog.entity.Review;

public final class ReviewMapper {
    private ReviewMapper() {
    }

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) {
            return null;
        }
        // Ten/avatar nguoi danh gia lay tu Customer (ho so), fallback ve username roi "Anonymous".
        var user = review.getUser();
        var customer = user == null ? null : user.getCustomer();
        String reviewerName = "Anonymous";
        String reviewerAvatar = null;
        if (customer != null && customer.getFullName() != null) {
            reviewerName = customer.getFullName();
            reviewerAvatar = customer.getAvatarUrl();
        } else if (user != null) {
            reviewerName = user.getUsername();
        }
        return new ReviewResponse(
                review.getId(),
                reviewerName,
                reviewerAvatar,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
