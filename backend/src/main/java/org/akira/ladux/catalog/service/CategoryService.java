package org.akira.ladux.catalog.service;

import org.akira.ladux.catalog.dto.request.CategoryRequest;
import org.akira.ladux.catalog.dto.response.CategoryResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CategoryService {
    PageResponse<CategoryResponse> getAllCategories(Pageable pageable);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(int id, CategoryRequest request);

    void deleteCategoryById(int id);

    String uploadCategoryImage(MultipartFile file);
}
