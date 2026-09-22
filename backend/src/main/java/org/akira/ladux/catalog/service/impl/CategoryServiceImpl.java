package org.akira.ladux.catalog.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.request.CategoryRequest;
import org.akira.ladux.catalog.dto.response.CategoryResponse;
import org.akira.ladux.catalog.entity.Category;
import org.akira.ladux.catalog.mapper.CategoryMapper;
import org.akira.ladux.catalog.repository.CategoryRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.service.CategoryService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.akira.ladux.shared.service.FileStorageService;
import org.akira.ladux.shared.util.SlugUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repo;
    private final ProductRepository productRepo;
    private final FileStorageService fileStorage;

    @Value("${app.upload.category-dir:categories}")
    private String categoryUploadDir;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<CategoryResponse> getAllCategories(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable)
                .map(CategoryMapper::fromEntity));
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = Category.builder()
                .name(request.name())
                .slug(SlugUtils.toSlug(request.name()))
                .imageUrl(standardizeNameCategory(request.imageUrl()))
                .build();
        return CategoryMapper.fromEntity(repo.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(int id, CategoryRequest request) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay category voi id = " + id));
        if (request.name() != null) {
            category.setName(request.name());
            category.setSlug(SlugUtils.toSlug(request.name()));
        }
        // imageUrl optional: null/absent = keep current; empty string = clear
        if (request.imageUrl() != null) {
            String newUrl = standardizeNameCategory(request.imageUrl());
            String oldUrl = category.getImageUrl();
            if (oldUrl != null && !oldUrl.equals(newUrl)) {
                fileStorage.deleteIfLocal(oldUrl);
            }
            category.setImageUrl(newUrl);
        }
        return CategoryMapper.fromEntity(category);
    }

    private static String standardizeNameCategory(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategoryById(int id) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay category voi id = " + id));
        if (productRepo.existsByCategoryId(id)) {
            throw new BusinessRuleException("Không thể xóa category này vì nó có sản phẩm liên quan");
        }
        String imageUrl = category.getImageUrl();
        repo.deleteById(id);
        fileStorage.deleteIfLocal(imageUrl);
    }

    @Override
    public String uploadCategoryImage(MultipartFile file) {
        return fileStorage.store(categoryUploadDir, file);
    }

}
