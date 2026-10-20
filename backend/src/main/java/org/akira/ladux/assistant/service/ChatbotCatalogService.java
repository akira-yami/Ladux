package org.akira.ladux.assistant.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.assistant.dto.response.ContextualChatResponse.ProductCard;
import org.akira.ladux.assistant.service.result.ChatbotContext;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.catalog.service.PricingService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vector search only supplies candidate IDs. Current catalog rows decide every displayed SKU and price. */
@Service
@RequiredArgsConstructor
public class ChatbotCatalogService {
    private static final int LIMIT = 5;
    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final PricingService pricingService;

    @Transactional(readOnly = true)
    public ChatbotContext resolve(String query, List<Integer> selectedIds) {
        List<ProductVariant> variants;
        if (selectedIds != null && !selectedIds.isEmpty()) {
            if (selectedIds.size() > LIMIT || selectedIds.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new BusinessRuleException("Chỉ được chọn tối đa 5 mã cấu hình hợp lệ");
            }
            // An explicit selection is a strict boundary. Never replace an unavailable SKU with another SKU.
            variants = new LinkedHashSet<>(selectedIds).stream()
                    .map(variantRepository::findById).flatMap(java.util.Optional::stream)
                    .filter(this::eligible).toList();
        } else {
            variants = retrieve(query);
        }
        List<ProductCard> cards = new ArrayList<>();
        StringBuilder description = new StringBuilder();
        for (ProductVariant variant : variants) {
            Product product = variant.getProduct();
            BigDecimal price = pricingService.sellingPrice(variant);
            String image = product.getImages() == null ? null : product.getImages().stream()
                    .filter(Objects::nonNull).map(item -> item.getImageUrl()).filter(Objects::nonNull).findFirst().orElse(null);
            cards.add(new ProductCard(product.getId(), variant.getId(), product.getName(), price, image));
            description.append("Cấu hình được xác thực: ").append(variant.getId()).append('\n');
            append(description, "Tên", product.getName());
            append(description, "CPU", product.getCpu());
            append(description, "GPU", product.getGpu());
            append(description, "RAM", variant.getRam());
            append(description, "Lưu trữ", variant.getRom());
            append(description, "Pin", product.getBattery());
            append(description, "Trọng lượng", product.getWeight());
            description.append("Giá bán hiện tại (VND): ").append(price.toPlainString()).append('\n');
            description.append("Đang được phép bán và còn hàng.\n\n");
        }
        return new ChatbotContext(description.toString(), List.copyOf(cards));
    }

    private List<ProductVariant> retrieve(String query) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(LIMIT).build());
        if (documents == null) return List.of();
        List<Integer> ids = documents.stream().filter(Objects::nonNull)
                .filter(document -> "product".equals(document.getMetadata().get("type")))
                .map(document -> productId(document.getMetadata().get("productId")))
                .filter(Objects::nonNull).distinct().limit(LIMIT).toList();
        if (ids.isEmpty()) return List.of();
        Map<Integer, Product> products = productRepository.findSummariesByIdIn(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (first, ignored) -> first));
        List<ProductVariant> result = new ArrayList<>();
        for (Integer id : ids) {
            Product product = products.get(id);
            if (product == null || !product.isActive() || product.getVariants() == null) continue;
            product.getVariants().stream().filter(this::eligible)
                    .min(Comparator.comparing(pricingService::sellingPrice).thenComparing(ProductVariant::getId))
                    .ifPresent(result::add);
        }
        return result;
    }

    private boolean eligible(ProductVariant variant) {
        if (variant.getId() == null || variant.getProduct() == null || variant.getProduct().getId() == null
                || !variant.isActive() || !variant.getProduct().isActive() || variant.getStockQuantity() <= 0) return false;
        BigDecimal price = pricingService.sellingPrice(variant);
        return price != null && price.signum() > 0;
    }

    private Integer productId(Object value) {
        try {
            if (!(value instanceof Number) && !(value instanceof String text && text.matches("[1-9]\\d{0,9}"))) return null;
            int id = new BigDecimal(value.toString()).intValueExact();
            return id > 0 ? id : null;
        } catch (NumberFormatException | ArithmeticException ignored) {
            return null;
        }
    }

    private void append(StringBuilder description, String label, String value) {
        description.append(label).append(": ").append(value == null || value.isBlank() ? "Chưa đủ dữ liệu" : value).append('\n');
    }
}
