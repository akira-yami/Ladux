package org.akira.ladux.assistant.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SmartFitResponse(
        List<Recommendation> recommendations,
        Weights weights,
        String disclaimer
) {
    public record Recommendation(
            Integer productId, Integer variantId, String name, String slug, String imageUrl,
            BigDecimal price, double score, List<String> reasons, List<String> tradeoffs, Specs specs
    ) {}

    public record Specs(String cpu, String gpu, Integer ramGb, Integer ssdGb, Double weightKg, String battery) {}

    public record Weights(int purpose, int budget, int memory, int mobility) {}
}
