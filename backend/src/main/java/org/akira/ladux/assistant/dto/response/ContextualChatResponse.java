package org.akira.ladux.assistant.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ContextualChatResponse(String text, List<ProductCard> products) {
    public record ProductCard(Integer productId, Integer variantId, String name, BigDecimal price, String imageUrl) {}
}
