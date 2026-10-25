package org.akira.ladux.payment.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

public record RefundRequest(
        @jakarta.validation.constraints.Positive Integer paymentId,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @Size(min = 3, max = 1000) String reason) {}
