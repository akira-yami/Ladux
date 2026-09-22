package org.akira.ladux.ordering.dto.response;

import java.io.Serializable;
import java.time.Instant;

public record OrderHistoryResponse(
        Integer id,
        Integer orderId,
        String status,
        String description,
        Instant createdAt
) implements Serializable {
}
