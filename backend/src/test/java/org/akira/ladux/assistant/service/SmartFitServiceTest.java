package org.akira.ladux.assistant.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.akira.ladux.assistant.dto.request.SmartFitRequest;
import org.akira.ladux.assistant.dto.request.SmartFitRequest.Priority;
import org.akira.ladux.assistant.dto.request.SmartFitRequest.Purpose;
import org.akira.ladux.assistant.dto.response.SmartFitResponse;
import org.akira.ladux.assistant.dto.response.SmartFitResponse.Recommendation;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductImage;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.SmartFitCatalogRepository;
import org.akira.ladux.catalog.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartFitServiceTest {
    @Mock SmartFitCatalogRepository candidates;
    @Mock ProductRepository products;

    private SmartFitService service() {
        return new SmartFitService(candidates, products, new PricingServiceImpl());
    }

    @Test
    void rejectsInactiveSoldOutOverBudgetAndUnverifiedRequiredSpecsEvenIfRepositoryReturnsThem() {
        ProductVariant valid = variant(1, 101, 10_000_000);
        ProductVariant inactiveProduct = variant(2, 102, 10_000_000);
        inactiveProduct.getProduct().setActive(false);
        ProductVariant inactiveSku = variant(3, 103, 10_000_000);
        inactiveSku.setActive(false);
        ProductVariant soldOut = variant(4, 104, 10_000_000);
        soldOut.setStockQuantity(0);
        ProductVariant overBudget = variant(5, 105, 30_000_001);
        ProductVariant lowRam = variant(6, 106, 10_000_000);
        lowRam.setRam("8GB");
        ProductVariant unknownSsd = variant(7, 107, 10_000_000);
        unknownSsd.setRom("512GB");
        ProductVariant lowSsd = variant(8, 108, 10_000_000);
        lowSsd.setRom("256GB SSD");
        ProductVariant badPrice = variant(9, 109, 0);
        ProductVariant unknownRam = variant(10, 110, 10_000_000);
        unknownRam.setRam("DDR5");
        stubCandidates(List.of(valid, inactiveProduct, inactiveSku, soldOut, overBudget, lowRam, unknownSsd, lowSsd, badPrice, unknownRam));

        SmartFitResponse result = service().recommend(request(Purpose.PROGRAMMING, Priority.PERFORMANCE, 16, 512, 5));

        assertEquals(List.of(101), result.recommendations().stream().map(Recommendation::variantId).toList());
        verify(candidates).findEligibleCandidates(BigDecimal.valueOf(30_000_000), PageRequest.of(0, 500));
        assertTrue(result.disclaimer().contains("ít hơn 3"));
    }

    @Test
    void usesTheActualDiscountedSkuPriceAndIncludesBudgetBoundary() {
        ProductVariant sale = variant(1, 101, 35_000_000);
        sale.setDiscountPrice(BigDecimal.valueOf(30_000_000));
        stubCandidates(List.of(sale));

        Recommendation result = service().recommend(request(Purpose.OFFICE, Priority.VALUE, null, null, 3)).recommendations().getFirst();

        assertEquals(BigDecimal.valueOf(30_000_000), result.price());
        assertEquals(101, result.variantId());
    }

    @Test
    void gamingScoresRecognizedDiscreteGraphicsAboveIntegratedWithoutModelBenchmarks() {
        ProductVariant integrated = variant(1, 101, 20_000_000);
        ProductVariant discrete = variant(2, 102, 20_000_000);
        discrete.getProduct().setGpu("NVIDIA GeForce RTX 4050 6GB GDDR6");
        stubCandidates(List.of(integrated, discrete));

        List<Recommendation> result = service().recommend(request(Purpose.GAMING, Priority.PERFORMANCE, null, null, 3)).recommendations();

        assertEquals(2, result.getFirst().productId());
        assertEquals(27.5, result.getFirst().score() - result.getLast().score(), .11);
        assertTrue(result.getFirst().reasons().stream().anyMatch(reason -> reason.contains("GPU rời")));
        assertTrue(result.getLast().tradeoffs().stream().anyMatch(reason -> reason.contains("Chưa xác nhận GPU rời")));
    }

    @Test
    void prioritiesApplyDifferentPublishedWeightsAndCanChangeTheWinner() {
        ProductVariant powerful = variant(1, 101, 29_000_000);
        powerful.getProduct().setGpu("RTX 4060");
        powerful.getProduct().setWeight("2.8 kg");
        ProductVariant mobile = variant(2, 102, 16_000_000);
        mobile.getProduct().setWeight("1.2 kg");
        stubCandidates(List.of(mobile, powerful));

        SmartFitResponse performance = service().recommend(request(Purpose.GAMING, Priority.PERFORMANCE, null, null, 3));
        SmartFitResponse mobility = service().recommend(request(Purpose.GAMING, Priority.MOBILITY, null, null, 3));

        assertEquals(1, performance.recommendations().getFirst().productId());
        assertEquals(2, mobility.recommendations().getFirst().productId());
        assertEquals(new SmartFitResponse.Weights(50, 15, 25, 10), performance.weights());
        assertEquals(new SmartFitResponse.Weights(30, 20, 15, 35), mobility.weights());
    }

    @Test
    void showsEachProductOnceAndUsesItsBestScoringEligibleVariant() {
        ProductVariant lowMemory = variant(1, 101, 15_000_000);
        lowMemory.setRam("8GB");
        ProductVariant highMemory = variant(1, 102, 16_000_000);
        stubCandidates(List.of(lowMemory, highMemory, variant(2, 201, 18_000_000)));

        List<Recommendation> result = service().recommend(request(Purpose.PROGRAMMING, Priority.PERFORMANCE, null, null, 5)).recommendations();

        assertEquals(2, result.size());
        assertEquals(102, result.stream().filter(item -> item.productId() == 1).findFirst().orElseThrow().variantId());
        assertEquals(result.size(), result.stream().map(Recommendation::productId).distinct().count());
    }

    @Test
    void unknownSpecsStayNullableAndHaveExplicitTradeoffs() {
        ProductVariant unknown = variant(1, 101, 10_000_000);
        unknown.setRam(null);
        unknown.setRom("Storage unknown");
        unknown.getProduct().setCpu(null);
        unknown.getProduct().setGpu(null);
        unknown.getProduct().setWeight("light");
        unknown.getProduct().setBattery(null);
        stubCandidates(List.of(unknown));

        Recommendation result = service().recommend(request(Purpose.DESIGN, Priority.VALUE, null, null, null)).recommendations().getFirst();

        assertNull(result.specs().cpu());
        assertNull(result.specs().gpu());
        assertNull(result.specs().ramGb());
        assertNull(result.specs().ssdGb());
        assertNull(result.specs().weightKg());
        assertNull(result.specs().battery());
        assertTrue(result.tradeoffs().stream().filter(item -> item.contains("Chưa đủ dữ liệu")).count() >= 4);
        assertTrue(result.score() >= 0 && result.score() <= 100);
    }

    @Test
    void batteryTextDoesNotCreateRuntimeOrInfluenceRanking() {
        ProductVariant one = variant(1, 101, 20_000_000);
        ProductVariant two = variant(2, 102, 20_000_000);
        one.getProduct().setBattery("90 Wh (marketing: 30 hours)");
        two.getProduct().setBattery(null);
        stubCandidates(List.of(two, one));

        List<Recommendation> result = service().recommend(request(Purpose.OFFICE, Priority.MOBILITY, null, null, 3)).recommendations();

        assertEquals(result.getFirst().score(), result.getLast().score());
        assertEquals("90 Wh (marketing: 30 hours)", result.getFirst().specs().battery());
        assertTrue(result.getFirst().tradeoffs().stream().anyMatch(item -> item.contains("không được dùng để dự đoán")));
    }

    @Test
    void tiesAndResultLimitRemainDeterministicAcrossRepositoryOrdering() {
        List<ProductVariant> variants = new ArrayList<>();
        for (int id = 7; id >= 1; id--) variants.add(variant(id, id + 100, 20_000_000));
        stubCandidates(variants);

        List<Recommendation> result = service().recommend(request(Purpose.OFFICE, Priority.VALUE, null, null, 5)).recommendations();

        assertEquals(List.of(1, 2, 3, 4, 5), result.stream().map(Recommendation::productId).toList());
    }

    @Test
    void readsImagesOnlyForSelectedProductsAndPrefersPrimaryImage() {
        ProductVariant variant = variant(1, 101, 20_000_000);
        Product product = variant.getProduct();
        product.getImages().add(ProductImage.builder().id(1).imageUrl("/other.jpg").build());
        product.getImages().add(ProductImage.builder().id(2).imageUrl("/primary.jpg").isPrimary(true).build());
        stubCandidates(List.of(variant));
        when(products.findSummariesByIdIn(List.of(1))).thenReturn(List.of(product));

        Recommendation result = service().recommend(request(Purpose.OFFICE, Priority.VALUE, null, null, 3)).recommendations().getFirst();

        assertEquals("/primary.jpg", result.imageUrl());
        verify(products).findSummariesByIdIn(List.of(1));
    }

    @Test
    void emptyMatchesDoNotInventAlternativesOrReadDetails() {
        stubCandidates(List.of());
        assertTrue(service().recommend(request(Purpose.GAMING, Priority.PERFORMANCE, 64, 2048, 3)).recommendations().isEmpty());
        verifyNoInteractions(products);
    }

    private void stubCandidates(List<ProductVariant> variants) {
        when(candidates.findEligibleCandidates(any(), any())).thenReturn(variants);
    }

    private SmartFitRequest request(Purpose purpose, Priority priority, Integer ram, Integer ssd, Integer limit) {
        return new SmartFitRequest(BigDecimal.valueOf(30_000_000), purpose, priority, ram, ssd, limit);
    }

    private ProductVariant variant(int productId, int variantId, long price) {
        Product product = Product.builder().id(productId).name("Laptop " + productId).slug("laptop-" + productId)
                .cpu("Intel Core i5-1340P").gpu("Intel Iris Xe Graphics").weight("1.5 kg").battery("55 Wh").build();
        return ProductVariant.builder().id(variantId).product(product).ram("16GB DDR5").rom("512GB SSD NVMe")
                .price(BigDecimal.valueOf(price)).stockQuantity(3).build();
    }
}
