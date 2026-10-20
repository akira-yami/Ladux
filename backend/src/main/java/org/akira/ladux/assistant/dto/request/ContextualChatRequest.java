package org.akira.ladux.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ContextualChatRequest(
        @NotBlank @Size(max = 2000) String query,
        @NotBlank @Size(max = 100) String conversationId,
        @Size(max = 5) List<@NotNull @Positive Integer> variantIds
) {}
