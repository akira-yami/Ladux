package org.akira.ladux.promotion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CouponApplyRequest(
        @NotBlank(message = "Code khong duoc de trong")
        @Size(max = 50, message = "Code khong duoc vuot qua 50 ky tu")
        String code
) {}
