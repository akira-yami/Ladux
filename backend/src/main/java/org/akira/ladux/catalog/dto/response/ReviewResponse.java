package org.akira.ladux.catalog.dto.response;

import java.io.Serializable;
import java.time.Instant;

public record ReviewResponse(
        Integer id,
        String reviewerName,
        String reviewerAvatar,
        int rating,
        String comment,
        Instant createdAt
) implements Serializable {
}
