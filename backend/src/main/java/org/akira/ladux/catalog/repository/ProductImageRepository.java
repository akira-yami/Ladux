package org.akira.ladux.catalog.repository;

import java.util.List;
import org.akira.ladux.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    List<ProductImage> findByProductId(Integer productId);
}

