package org.akira.ladux.assistant.service.result;

import java.util.List;
import org.akira.ladux.assistant.dto.response.ContextualChatResponse.ProductCard;

/** Immutable public catalog snapshot; no entities or private account/order data reach the model. */
public record ChatbotContext(String description, List<ProductCard> products) {}
