package org.akira.ladux.catalog.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.ProductRequest;
import org.akira.ladux.catalog.dto.request.ProductVariantRequest;
import org.akira.ladux.catalog.dto.response.ProductResponse;
import org.akira.ladux.catalog.dto.response.ProductVariantResponse;
import org.akira.ladux.catalog.entity.Brand;
import org.akira.ladux.catalog.entity.Category;
import org.akira.ladux.catalog.entity.Color;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductImage;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.mapper.ProductMapper;
import org.akira.ladux.catalog.mapper.ProductVariantMapper;
import org.akira.ladux.catalog.repository.BrandRepository;
import org.akira.ladux.catalog.repository.CategoryRepository;
import org.akira.ladux.catalog.repository.ColorRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.catalog.service.ProductService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.akira.ladux.shared.util.SlugUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository repo;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ColorRepository colorRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'v4:all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        return toSummaryPage(repo.findAllIds(pageable), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'v3:id:' + #id")
    public ProductResponse getProductById(int id) {
        return ProductMapper.fromEntity(repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay product voi id = " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'v3:slug:' + #slug")
    public ProductResponse getProductBySlug(String slug) {
        Product p = repo.findBySlug(slug);
        if (p == null) {
            throw new ResourceNotFoundException("Khong tim thay product voi slug = " + slug);
        }
        return ProductMapper.fromEntity(p);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'v4:brand:' + #brandId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public PageResponse<ProductResponse> getProductsByBrandId(int brandId, Pageable pageable) {
        if (!brandRepository.existsById(brandId)) {
            throw new IllegalArgumentException("Không tìm thấy brand với id = " + brandId);
        }
        return toSummaryPage(repo.findIdsByBrandId(brandId, pageable), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'v4:category:' + #categoryId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public PageResponse<ProductResponse> getProductsByCategoryId(int categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("Không tìm thấy category với id = " + categoryId);
        }
        return toSummaryPage(repo.findIdsByCategoryId(categoryId, pageable), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'v4:search:' + #search + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public PageResponse<ProductResponse> searchProducts(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return getAllProducts(pageable);
        }
        String searchClean = search.trim();
        return toSummaryPage(repo.searchIds(searchClean, pageable), pageable);
    }

    // Dùng phương phap Two-Phase ID Pagination để query nhanh
    private PageResponse<ProductResponse> toSummaryPage(Page<Integer> idPage, Pageable pageable) {
        if (idPage.isEmpty()) {
            return new PageResponse<>(
                    List.of(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    idPage.getTotalElements(),
                    idPage.getTotalPages(),
                    idPage.isFirst(),
                    idPage.isLast(),
                    true
            );
        }
        Map<Integer, Product> productsById = repo.findSummariesByIdIn(idPage.getContent()).stream()
                .collect(Collectors.toMap(product -> product.getId(), product -> product));
        List<ProductResponse> content = idPage.getContent().stream()
                .map(id -> productsById.get(id))
                .filter(product -> product != null)
                .map(product -> ProductMapper.summaryFromEntity(product))
                .toList();
        return new PageResponse<>(
                content,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                idPage.getTotalElements(),
                idPage.getTotalPages(),
                idPage.isFirst(),
                idPage.isLast(),
                false
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay brand voi id = " + request.brandId()));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay category voi id = " + request.categoryId()));
        Product product = Product.builder()
                .description(request.description())
                .brand(brand)
                .category(category)
                .name(request.name())
                .cpu(request.cpu())
                .gpu(request.gpu())
                .display(request.display())
                .battery(request.battery())
                .weight(request.weight())
                .numberOfFans(request.numberOfFans())
                .os(request.os())
                .isActive(request.isActive() == null ? true : request.isActive())
                .slug(SlugUtils.toSlug(request.name()))
                .build();
        if (request.variants() != null && !request.variants().isEmpty()) {
            for (ProductVariantRequest variantRequest : request.variants()) {
                Color color = colorRepository.findById(variantRequest.colorId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy màu sắc với id = " + variantRequest.colorId()));
                String rawSku = product.getName() + " " + variantRequest.ram() + " " + variantRequest.rom() + " " + color.getName();
                String sku = resolveSku(null, rawSku);
                ProductVariant variant = ProductVariant.builder()
                        .color(color)
                        .ram(variantRequest.ram())
                        .rom(variantRequest.rom())
                        .price(variantRequest.price())
                        .discountPrice(variantRequest.discountPrice())
                        .stockQuantity(variantRequest.stockQuantity())
                        .sku(sku)
                        .isActive(variantRequest.isActive())
                        .build();
                product.addVariant(variant);
            }
        }
        replaceProductImages(product, request.imageUrls());
        return ProductMapper.fromEntity(repo.save(product));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(int id, ProductRequest request) {
        Product product = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay product voi id = " + id));
        if (request.brandId() != null) {
            Brand brand = brandRepository.findById(request.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay brand voi id = " + request.brandId()));
            product.setBrand(brand);
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay category voi id = " + request.categoryId()));
            product.setCategory(category);
        }
        if (request.name() != null && !request.name().isBlank()) {
            product.setName(request.name());
            product.setSlug(SlugUtils.toSlug(request.name()));
        }
        if (request.isActive() != null) {
            product.setActive(request.isActive());
        }
        if (request.imageUrls() != null) {
            replaceProductImages(product, request.imageUrls());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.cpu() != null) {
            product.setCpu(request.cpu());
        }
        if (request.gpu() != null) {
            product.setGpu(request.gpu());
        }
        if (request.display() != null) {
            product.setDisplay(request.display());
        }
        if (request.battery() != null) {
            product.setBattery(request.battery());
        }
        if (request.weight() != null) {
            product.setWeight(request.weight());
        }
        if (request.numberOfFans() != null) {
            product.setNumberOfFans(request.numberOfFans());
        }
        if (request.os() != null) {
            product.setOs(request.os());
        }
        if (request.variants() != null && !request.variants().isEmpty()) {
            upsertProductVariants(product, request.variants());
        }
        return ProductMapper.fromEntity(repo.save(product));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProductById(int id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Khong tim thay product voi id = " + id);
        }
        repo.deleteById(id);
    }

    private String resolveSku(String requestedSku, String fallbackName) {
        String base;
        if (requestedSku != null && !requestedSku.isBlank()) {
            base = SlugUtils.toSlug(requestedSku);
            if (base.isBlank()) {
                base = SlugUtils.toSlug(fallbackName);
            }
        } else {
            base = SlugUtils.toSlug(fallbackName);
        }

        if (base.isBlank()) {
            base = "LAPTOP";
        }

        // Đổi toàn bộ SKU thành CHỮ HOA cho chuẩn thương mại điện tử
        return ensureUniqueSku(base.toUpperCase());
    }

    /** SKU column is varchar(50) — keep base short enough for numeric suffixes. */
    private String ensureUniqueSku(String rawBase) {
        String base = rawBase.length() > 40 ? rawBase.substring(0, 40).replaceAll("-+$", "") : rawBase;
        if (base.isBlank()) {
            base = "LAPTOP";
        }

        String candidate = base.length() > 50 ? base.substring(0, 50) : base;
        int suffix = 2;

        while (productVariantRepository.existsBySku(candidate)) {
            String withSuffix = base + "-" + suffix++;
            candidate = withSuffix.length() > 50 ? withSuffix.substring(0, 50) : withSuffix;
        }

        return candidate;
    }
    private void validateProductPricing(BigDecimal basePrice, BigDecimal discountPrice) {
        if (discountPrice != null && discountPrice.compareTo(basePrice) > 0) {
            throw new BusinessRuleException("DiscountPrice khong duoc lon hon BasePrice");
        }
    }

    private void replaceProductImages(Product product, List<String> imageUrls) {
        if (imageUrls == null) {
            return;
        }
        product.getImages().clear();
        List<String> distinctUrls = imageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(imageUrl -> !imageUrl.isBlank())
                .distinct()
                .toList();

        for (int i = 0; i < distinctUrls.size(); i++) {
            product.getImages().add(ProductImage.builder()
                    .product(product)
                    .imageUrl(distinctUrls.get(i))
                    .isPrimary(i == 0) // Ảnh đầu tiên luôn là ảnh chính (isPrimary = true)
                    .build());
        }
    }

    private void upsertProductVariants(Product product, List<ProductVariantRequest> variantRequests) {
        Map<Integer, ProductVariant> existingById = product.getVariants().stream()
                .filter(variant -> variant.getId() != null)
                .collect(Collectors.toMap(ProductVariant::getId, variant -> variant));

        for (ProductVariantRequest request : variantRequests) {
            ProductVariant variant = request.id() == null ? null : existingById.get(request.id());
            if (request.id() != null && variant == null) {
                throw new BusinessRuleException("VariantId khong thuoc san pham dang cap nhat: " + request.id());
            }
            if (variant == null) {
                variant = ProductVariant.builder()
                        .sku(resolveSku(null, buildVariantSkuSeed(product.getName(), request)))
                        .build();
                product.addVariant(variant);
            }
            applyVariantFields(variant, request);
        }
    }

    private void applyVariantFields(ProductVariant variant, ProductVariantRequest request) {
        variant.setColor(resolveColor(request.colorId()));
        variant.setRam(request.ram());
        variant.setRom(request.rom());
        variant.setPrice(requirePrice(request.price()));
        variant.setDiscountPrice(request.discountPrice());
        validateProductPricing(variant.getPrice(), variant.getDiscountPrice());
        variant.setStockQuantity(request.stockQuantity());
        variant.setActive(request.isActive());
    }

    private Color resolveColor(Integer colorId) {
        if (colorId == null) {
            throw new BusinessRuleException("ColorId khong duoc de trong");
        }
        return colorRepository.findById(colorId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay mau sac voi id = " + colorId));
    }

    private BigDecimal requirePrice(BigDecimal price) {
        if (price == null) {
            throw new BusinessRuleException("Price khong duoc de trong");
        }
        return price;
    }

    private String buildVariantSkuSeed(String productName, ProductVariantRequest request) {
        Color color = resolveColor(request.colorId());
        return productName + " " + request.ram() + " " + request.rom() + " " + color.getName();
    }
    // == Variant Products ==
    public ProductVariantResponse getProductVariantById(Integer variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay product variant voi id = " + variantId));
        return ProductVariantMapper.fromEntity(variant);
    }
}
