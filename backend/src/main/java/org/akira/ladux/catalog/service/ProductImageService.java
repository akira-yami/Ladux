package org.akira.ladux.catalog.service;

import java.util.List;
import org.akira.ladux.catalog.dto.response.ProductImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageService {
    List<ProductImageResponse> uploadImage(int productId, List<MultipartFile> files, List<String> imageUrls);

    void deleteProductImageById(int productId, int imageId);
}
