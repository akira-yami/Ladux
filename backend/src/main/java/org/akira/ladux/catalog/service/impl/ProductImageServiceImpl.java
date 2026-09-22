package org.akira.ladux.catalog.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.response.ProductImageResponse;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductImage;
import org.akira.ladux.catalog.mapper.ProductImageMapper;
import org.akira.ladux.catalog.repository.ProductImageRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.service.ProductImageService;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.akira.ladux.shared.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {
    private final ProductImageRepository repo;
    private final ProductRepository productRepo;
    private final FileStorageService fileStorage;

    @Value("${app.upload.product-dir:products}")
    private String productUploadDir;

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public List<ProductImageResponse> uploadImage(int productId, List<MultipartFile> files, List<String> imageUrls) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham voi id = " + productId));

        Set<String> existingUrls = repo.findByProductId(productId).stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toSet());

        Set<String> candidateUrls = new LinkedHashSet<>();

        // 1. Thu thập URL có sẵn từ client nếu có
        if (imageUrls != null) {
            imageUrls.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(url -> !url.isBlank())
                    .forEach(candidateUrls::add);
        }

        // 2. Upload các file từ máy nếu có
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String storedUrl = fileStorage.store(productUploadDir, file);
                    if (storedUrl != null && !storedUrl.isBlank()) {
                        candidateUrls.add(storedUrl.trim());
                    }
                }
            }
        }

        if (candidateUrls.isEmpty()) {
            return List.of();
        }

        boolean currentEmpty = existingUrls.isEmpty();
        List<String> newUrls = candidateUrls.stream()
                .filter(url -> !existingUrls.contains(url))
                .toList();

        if (newUrls.isEmpty()) {
            return List.of();
        }

        List<ProductImage> productImages = new ArrayList<>();
        for (int i = 0; i < newUrls.size(); i++) {
            productImages.add(ProductImage.builder()
                    .product(product)
                    .imageUrl(newUrls.get(i))
                    .isPrimary(currentEmpty && i == 0)
                    .build());
        }

        return repo.saveAll(productImages).stream()
                .map(ProductImageMapper::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProductImageById(int productId, int imageId) {
        ProductImage image = repo.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay product image voi id = " + imageId));
        if (image.getProduct() == null || !image.getProduct().getId().equals(productId)) {
            throw new BusinessRuleException("Anh khong thuoc san pham dang thao tac");
        }
        String imageUrl = image.getImageUrl();
        repo.delete(image);
        fileStorage.deleteIfLocal(imageUrl);
    }
}
