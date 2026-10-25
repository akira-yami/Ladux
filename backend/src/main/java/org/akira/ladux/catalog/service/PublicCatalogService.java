package org.akira.ladux.catalog.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.PublicCatalogFilter;
import org.akira.ladux.catalog.dto.response.CatalogFacetsResponse;
import org.akira.ladux.catalog.dto.response.ProductResponse;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.mapper.ProductMapper;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.PublicCatalogRepository;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCatalogService {
    private final PublicCatalogRepository catalog;
    private final ProductRepository products;
    private final org.akira.ladux.catalog.repository.ProductVariantRepository variants;

    public PageResponse<ProductResponse> search(PublicCatalogFilter filter, Pageable page) {
        var ids = catalog.search(filter, page);
        if (ids.isEmpty()) return PageResponse.from(new PageImpl<>(List.of(), page, ids.getTotalElements()));
        Map<Integer, Product> byId = products.findSummariesByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        var ratings = catalog.ratings(ids.getContent());
        var content = ids.getContent().stream().map(byId::get).filter(java.util.Objects::nonNull)
                .map(product -> {
                    double[] rating = ratings.getOrDefault(product.getId(), new double[]{0, 0});
                    return ProductMapper.publicFromEntity(product, rating[0], (long) rating[1]);
                }).toList();
        return PageResponse.from(new PageImpl<>(content, page, ids.getTotalElements()));
    }

    public ProductResponse detail(int id) {
        Product product = products.findById(id).filter(p -> p.isActive() && p.getCategory().isActive()
                && p.getVariants().stream().anyMatch(v -> v.isActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm đang được bán"));
        double[] rating = catalog.ratings(List.of(id)).getOrDefault(id, new double[]{0, 0});
        return ProductMapper.publicFromEntity(product, rating[0], (long) rating[1]);
    }

    @Cacheable(value = "products", key = "'public:facets:v1'")
    public CatalogFacetsResponse facets() { return catalog.facets(); }

    public org.akira.ladux.catalog.dto.response.ProductVariantResponse variant(int id) {
        return variants.findById(id).filter(v -> v.isActive() && v.getProduct().isActive()
                        && v.getProduct().getCategory().isActive())
                .map(org.akira.ladux.catalog.mapper.ProductVariantMapper::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình đang được bán"));
    }
}
