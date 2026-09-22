package org.akira.ladux.catalog.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.ProductPage;
import org.akira.ladux.catalog.dto.ProductSearch;
import org.akira.ladux.catalog.dto.ProductVariantView;
import org.akira.ladux.catalog.dto.ProductView;
import org.akira.ladux.catalog.mapper.ProductMapper;
import org.akira.ladux.catalog.repository.BrandRepository;
import org.akira.ladux.catalog.repository.CategoryRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Product and variant queries shared by the storefront and other services. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogQueryService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public ProductView getProduct(Integer id) {
        requirePositiveId(id, "productId");
        return productRepository.findById(id)
                .map(ProductMapper::toView)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id = " + id));
    }

    public ProductVariantView getVariant(Integer variantId) {
        requirePositiveId(variantId, "variantId");
        return variantRepository.findById(variantId)
                .map(ProductMapper::toVariantView)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm với id = " + variantId));
    }

    public boolean variantExists(Integer variantId) {
        return variantId != null && variantId > 0 && variantRepository.existsById(variantId);
    }

    public boolean productExists(Integer productId) {
        return productId != null && productId > 0 && productRepository.existsById(productId);
    }

    public ProductPage searchProducts(ProductSearch search) {
        if (search.brandId() != null && !brandRepository.existsById(search.brandId())) {
            throw new ResourceNotFoundException("Không tìm thấy brand với id = " + search.brandId());
        }
        if (search.categoryId() != null && !categoryRepository.existsById(search.categoryId())) {
            throw new ResourceNotFoundException("Không tìm thấy category với id = " + search.categoryId());
        }
        String requestedSort = search.sortBy() == null ? "id" : search.sortBy();
        String sortProperty = switch (requestedSort) {
            case "name", "createdAt", "id" -> requestedSort;
            default -> "id";
        };
        Sort sort = Sort.by(search.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, sortProperty);
        PageRequest pageable = PageRequest.of(search.pageNumber(), search.pageSize(), sort);
        String text = search.text() == null || search.text().isBlank() ? null : search.text().trim();
        var idPage = productRepository.findIds(text, search.brandId(), search.categoryId(), pageable);
        if (idPage.isEmpty()) {
            return toPage(List.of(), search, idPage.getTotalElements());
        }

        Map<Integer, ProductView> productsById = new HashMap<>();
        productRepository.findSummariesByIdIn(idPage.getContent()).forEach(product ->
                productsById.put(product.getId(), ProductMapper.toView(product)));
        List<ProductView> content = idPage.getContent().stream()
                .map(productsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return toPage(content, search, idPage.getTotalElements());
    }

    private ProductPage toPage(List<ProductView> content, ProductSearch search, long totalElements) {
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / search.pageSize());
        boolean first = search.pageNumber() == 0;
        boolean last = totalPages == 0 || search.pageNumber() >= totalPages - 1;
        return new ProductPage(content, search.pageNumber(), search.pageSize(), totalElements,
                totalPages, first, last, content.isEmpty());
    }

    private void requirePositiveId(Integer id, String name) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException(name + " must be a positive integer");
        }
    }
}
