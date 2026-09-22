package org.akira.ladux.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewUpdateRequest(
        @Min(value = 1, message = "Rating toi thieu la 1")
        @Max(value = 5, message = "Rating toi da la 5")
        Integer rating,

        @Size(max = 2000, message = "Comment khong duoc vuot qua 2000 ky tu")
        String comment
) {}
