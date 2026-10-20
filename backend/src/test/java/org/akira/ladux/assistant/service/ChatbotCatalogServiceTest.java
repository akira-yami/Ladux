package org.akira.ladux.assistant.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.akira.ladux.assistant.dto.response.ContextualChatResponse.ProductCard;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.catalog.service.impl.PricingServiceImpl;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotCatalogServiceTest {
    @Mock VectorStore vectors;
    @Mock ProductRepository products;
    @Mock ProductVariantRepository variants;

    private ChatbotCatalogService service() {
        return new ChatbotCatalogService(vectors, products, variants, new PricingServiceImpl());
    }

    @Test
    void selectedContextContainsOnlyExactSelectedSkuAndCurrentSellingPrice() {
        ProductVariant selected = variant(1, 101, 20_000_000);
        selected.setDiscountPrice(BigDecimal.valueOf(18_000_000));
        ProductVariant unselected = variant(1, 102, 10_000_000);
        selected.getProduct().addVariant(unselected);
        when(variants.findById(101)).thenReturn(Optional.of(selected));

        var context = service().resolve("so sánh", List.of(101, 101));

        assertEquals(List.of(101), context.products().stream().map(ProductCard::variantId).toList());
        assertEquals(BigDecimal.valueOf(18_000_000), context.products().getFirst().price());
        assertTrue(context.description().contains("18000000"));
        assertFalse(context.description().contains("10000000"));
        assertTrue(context.description().contains("Chưa đủ dữ liệu"));
        verify(variants).findById(101);
        verifyNoInteractions(vectors, products);
    }

    @Test
    void rejectsInactiveProductInactiveSkuSoldOutAndInvalidPriceWithoutRetrievalFallback() {
        ProductVariant inactiveProduct = variant(1, 101, 10_000_000);
        inactiveProduct.getProduct().setActive(false);
        ProductVariant inactiveSku = variant(2, 102, 10_000_000);
        inactiveSku.setActive(false);
        ProductVariant soldOut = variant(3, 103, 10_000_000);
        soldOut.setStockQuantity(0);
        ProductVariant invalidPrice = variant(4, 104, 0);
        when(variants.findById(101)).thenReturn(Optional.of(inactiveProduct));
        when(variants.findById(102)).thenReturn(Optional.of(inactiveSku));
        when(variants.findById(103)).thenReturn(Optional.of(soldOut));
        when(variants.findById(104)).thenReturn(Optional.of(invalidPrice));
        when(variants.findById(999)).thenReturn(Optional.empty());

        var result = service().resolve("laptop", List.of(101, 102, 103, 104, 999));

        assertTrue(result.products().isEmpty());
        assertEquals("", result.description());
        verifyNoInteractions(vectors, products);
    }

    @Test
    void ignoresMalformedVectorMetadataAndNeverUsesDocumentTextAsProductContext() {
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                document("product", "not-an-id"), document("product", 1.5),
                document("product", "2147483648"), document("product", -1),
                document("order", 999), document("product", 2.0), document("product", "2")));
        ProductVariant valid = variant(2, 202, 12_000_000);
        when(products.findSummariesByIdIn(List.of(2))).thenReturn(List.of(valid.getProduct()));

        var result = service().resolve("laptop", null);

        assertEquals(List.of(202), result.products().stream().map(ProductCard::variantId).toList());
        assertFalse(result.description().contains("PRIVATE DOCUMENT TEXT"));
        verifyNoInteractions(variants);
    }

    @Test
    void retrievalReturnsOneEligibleSkuPerProductAndDropsInactiveProducts() {
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document("product", 1), document("product", 2)));
        ProductVariant expensive = variant(1, 101, 30_000_000);
        ProductVariant cheapest = variant(1, 102, 20_000_000);
        ProductVariant soldOut = variant(1, 103, 10_000_000);
        soldOut.setStockQuantity(0);
        expensive.getProduct().addVariant(cheapest);
        expensive.getProduct().addVariant(soldOut);
        ProductVariant inactive = variant(2, 201, 10_000_000);
        inactive.getProduct().setActive(false);
        when(products.findSummariesByIdIn(List.of(1, 2))).thenReturn(List.of(expensive.getProduct(), inactive.getProduct()));

        var result = service().resolve("laptop", List.of());

        assertEquals(List.of(102), result.products().stream().map(ProductCard::variantId).toList());
    }

    @Test
    void boundsExplicitContextBeforeAnyDatabaseOrModelAccess() {
        assertThrows(BusinessRuleException.class, () -> service().resolve("laptop", List.of(1, 2, 3, 4, 5, 6)));
        assertThrows(BusinessRuleException.class, () -> service().resolve("laptop", List.of(0)));
        assertThrows(BusinessRuleException.class, () -> service().resolve("laptop", Arrays.asList((Integer) null)));
        verifyNoInteractions(vectors, products, variants);
    }

    private Document document(String type, Object id) {
        return Document.builder().text("PRIVATE DOCUMENT TEXT").metadata("type", type).metadata("productId", id).build();
    }

    private ProductVariant variant(int productId, int variantId, long price) {
        Product product = Product.builder().id(productId).name("Laptop " + productId).build();
        ProductVariant variant = ProductVariant.builder().id(variantId).price(BigDecimal.valueOf(price))
                .ram("16GB").rom("512GB SSD").stockQuantity(3).build();
        product.addVariant(variant);
        return variant;
    }
}
