package org.akira.ladux.assistant.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.assistant.dto.request.SmartFitRequest;
import org.akira.ladux.assistant.dto.request.SmartFitRequest.Purpose;
import org.akira.ladux.assistant.dto.response.SmartFitResponse;
import org.akira.ladux.assistant.dto.response.SmartFitResponse.Recommendation;
import org.akira.ladux.assistant.dto.response.SmartFitResponse.Specs;
import org.akira.ladux.assistant.dto.response.SmartFitResponse.Weights;
import org.akira.ladux.assistant.util.SmartFitSpecs;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductImage;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.SmartFitCatalogRepository;
import org.akira.ladux.catalog.service.PricingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmartFitService {
    /** MVP bound; inspect/replace the candidate policy before scaling beyond this catalog size. */
    static final int CANDIDATE_LIMIT = 500;
    private static final String DISCLAIMER = "Điểm phù hợp dùng quy tắc thử nghiệm, không phải benchmark hay cam kết hiệu năng. "
            + "Trọng số gốc 40/25/20/15 được điều chỉnh theo ưu tiên đã chọn. "
            + "Pin chỉ hiển thị dữ liệu danh mục, không suy ra thời lượng sử dụng. "
            + "Có thể có ít hơn 3 lựa chọn nếu không đủ SKU đáp ứng; giá và tồn kho được kiểm tra lại khi thanh toán.";
    private final SmartFitCatalogRepository candidateRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    @Value("${app.base-url:}")
    private String baseUrl;

    public SmartFitResponse recommend(SmartFitRequest request) {
        Weights weights = switch (request.priority()) {
            case PERFORMANCE -> new Weights(50, 15, 25, 10);
            case VALUE -> new Weights(35, 40, 15, 10);
            case MOBILITY -> new Weights(30, 20, 15, 35);
        };
        List<Recommendation> ranked = candidateRepository
                .findEligibleCandidates(request.budgetMax(), PageRequest.of(0, CANDIDATE_LIMIT)).stream()
                .filter(variant -> eligible(variant, request))
                .map(variant -> rank(variant, request, weights))
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed()
                        .thenComparing(Recommendation::price)
                        .thenComparing(Recommendation::productId)
                        .thenComparing(Recommendation::variantId))
                .toList();
        // A more expensive/high-capacity SKU may win for its product; show each model once.
        Map<Integer, Recommendation> unique = new LinkedHashMap<>();
        for (Recommendation recommendation : ranked) unique.putIfAbsent(recommendation.productId(), recommendation);
        List<Recommendation> selected = unique.values().stream().limit(request.resultLimit()).toList();
        if (selected.isEmpty()) return new SmartFitResponse(List.of(), weights, DISCLAIMER);
        Map<Integer, Product> products = productRepository.findSummariesByIdIn(
                        selected.stream().map(Recommendation::productId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Recommendation> result = selected.stream().map(recommendation -> new Recommendation(
                recommendation.productId(), recommendation.variantId(), recommendation.name(), recommendation.slug(),
                imageUrl(products.get(recommendation.productId())), recommendation.price(), recommendation.score(),
                recommendation.reasons(), recommendation.tradeoffs(), recommendation.specs())).toList();
        return new SmartFitResponse(result, weights, DISCLAIMER);
    }

    private boolean eligible(ProductVariant variant, SmartFitRequest request) {
        Product product = variant.getProduct();
        if (product == null || !product.isActive() || !variant.isActive() || variant.getStockQuantity() <= 0) return false;
        BigDecimal price = pricingService.sellingPrice(variant);
        if (price == null || price.signum() <= 0 || price.compareTo(request.budgetMax()) > 0) return false;
        return meetsMinimum(SmartFitSpecs.ramGb(variant.getRam()), request.minRamGb())
                && meetsMinimum(SmartFitSpecs.ssdGb(variant.getRom()), request.minSsdGb());
    }

    private boolean meetsMinimum(Integer actual, Integer minimum) {
        return minimum == null || actual != null && actual >= minimum;
    }

    private Recommendation rank(ProductVariant variant, SmartFitRequest request, Weights weights) {
        Product product = variant.getProduct();
        BigDecimal price = pricingService.sellingPrice(variant);
        Integer ram = SmartFitSpecs.ramGb(variant.getRam());
        Integer ssd = SmartFitSpecs.ssdGb(variant.getRom());
        Double weight = SmartFitSpecs.weightKg(product.getWeight());
        int ramTarget = request.purpose() == Purpose.DESIGN ? 32 : request.purpose() == Purpose.OFFICE ? 8 : 16;
        int ssdTarget = request.purpose() == Purpose.DESIGN ? 1024 : request.purpose() == Purpose.OFFICE ? 256 : 512;
        double ramFit = capacityFit(ram, ramTarget);
        double ssdFit = capacityFit(ssd, ssdTarget);
        boolean cpu = SmartFitSpecs.recognizedCpu(product.getCpu());
        boolean discrete = SmartFitSpecs.discreteGpu(product.getGpu());
        boolean appleGpu = SmartFitSpecs.appleGpu(product.getGpu());
        // Features, not model-number performance estimates. Unknown features contribute zero.
        double purposeFit = switch (request.purpose()) {
            case PROGRAMMING -> (cpu ? .4 : 0) + .35 * ramFit + .25 * ssdFit;
            case GAMING -> (cpu ? .2 : 0) + (discrete ? .55 : 0) + .15 * ramFit + .1 * ssdFit;
            case DESIGN -> (cpu ? .15 : 0) + (discrete || appleGpu ? .45 : 0) + .25 * ramFit + .15 * ssdFit;
            case OFFICE -> (cpu ? .4 : 0) + .4 * ramFit + .2 * ssdFit;
        };
        double budgetRatio = price.divide(request.budgetMax(), 8, RoundingMode.HALF_UP).doubleValue();
        double budgetFit = .4 + .6 * (1 - budgetRatio);
        double memoryFit = .65 * ramFit + .35 * ssdFit;
        double mobilityFit = weight == null ? 0 : weight <= 1.3 ? 1 : weight <= 1.6 ? .85 : weight <= 1.9 ? .6 : weight <= 2.3 ? .35 : .1;
        double score = Math.round((purposeFit * weights.purpose() + budgetFit * weights.budget()
                + memoryFit * weights.memory() + mobilityFit * weights.mobility()) * 10) / 10.0;

        List<String> reasons = new ArrayList<>();
        List<String> tradeoffs = new ArrayList<>();
        reasons.add("SKU đang bán, còn hàng và giá hiện tại trong ngân sách.");
        if (budgetRatio <= .8) reasons.add("Giá dùng tối đa 80% ngân sách, còn dư cho phụ kiện hoặc nâng cấp.");
        if (cpu) reasons.add("CPU thuộc nhóm Core/Ryzen/Apple M/Snapdragon X nhận diện được; không quy đổi thành benchmark.");
        else tradeoffs.add("CPU: Chưa đủ dữ liệu để phân loại theo quy tắc SmartFit.");
        capacityExplanation("RAM", ram, ramTarget, reasons, tradeoffs);
        capacityExplanation("SSD", ssd, ssdTarget, reasons, tradeoffs);
        if (request.purpose() == Purpose.GAMING || request.purpose() == Purpose.DESIGN) {
            if (discrete) reasons.add("Danh mục ghi nhận GPU rời; cộng điểm tiêu chí đồ họa theo mục đích đã chọn.");
            else if (appleGpu && request.purpose() == Purpose.DESIGN) reasons.add("Danh mục ghi nhận Apple GPU; cộng điểm tiêu chí đồ họa, cần kiểm tra phần mềm tương thích.");
            else tradeoffs.add("Chưa xác nhận GPU rời phù hợp; cần kiểm tra yêu cầu game/phần mềm trước khi mua.");
        }
        if (weight == null) tradeoffs.add("Trọng lượng: Chưa đủ dữ liệu; không cộng điểm di động.");
        else if (weight <= 1.6) reasons.add("Trọng lượng " + weight + " kg, đạt ngưỡng di động ≤ 1,6 kg của SmartFit.");
        else tradeoffs.add("Trọng lượng " + weight + " kg; ít điểm di động hơn nhóm ≤ 1,6 kg.");
        tradeoffs.add(product.getBattery() == null || product.getBattery().isBlank()
                ? "Pin: Chưa đủ dữ liệu."
                : "Chưa có phép đo thời lượng pin đồng nhất; thông số pin không được dùng để dự đoán số giờ.");
        if (request.purpose() == Purpose.DESIGN) tradeoffs.add("Chưa chấm độ phủ màu màn hình hoặc khả năng chạy từng phần mềm thiết kế.");
        return new Recommendation(product.getId(), variant.getId(), product.getName(), product.getSlug(), null,
                price, score, List.copyOf(reasons), List.copyOf(tradeoffs), new Specs(
                textOrNull(product.getCpu()), textOrNull(product.getGpu()), ram, ssd, weight, textOrNull(product.getBattery())));
    }

    private double capacityFit(Integer actual, int target) {
        return actual == null ? 0 : Math.min(1, (double) actual / target);
    }

    private void capacityExplanation(String label, Integer actual, int target, List<String> reasons, List<String> tradeoffs) {
        if (actual == null) tradeoffs.add(label + ": Chưa đủ dữ liệu để xác nhận dung lượng.");
        else if (actual >= target) reasons.add(label + " " + actual + " GB đạt mốc gợi ý " + target + " GB cho mục đích đã chọn.");
        else tradeoffs.add(label + " " + actual + " GB thấp hơn mốc gợi ý " + target + " GB cho mục đích đã chọn.");
    }

    private String textOrNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw;
    }

    private String imageUrl(Product product) {
        if (product == null || product.getImages() == null) return null;
        String raw = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::isPrimary).reversed()
                        .thenComparing(ProductImage::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(ProductImage::getImageUrl).filter(value -> value != null && !value.isBlank())
                .findFirst().orElse(null);
        if (raw == null || raw.startsWith("http://") || raw.startsWith("https://") || baseUrl == null || baseUrl.isBlank()) return raw;
        return baseUrl.replaceAll("/$", "") + (raw.startsWith("/") ? raw : "/" + raw);
    }
}
