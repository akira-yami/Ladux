package org.akira.ladux.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundResolutionRequest(@NotBlank @Size(min = 3, max = 1000) String reference) {}
