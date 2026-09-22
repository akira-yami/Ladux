package org.akira.ladux.catalog.repository;

import java.util.List;
import org.akira.ladux.catalog.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {
    @EntityGraph(attributePaths = {"product", "product.brand", "product.category", "product.images"})
    List<Wishlist> findByUserId(Integer userId);

    @EntityGraph(attributePaths = {"user", "product", "product.brand", "product.category"})
    List<Wishlist> findByProductId(Integer productId);

    boolean existsByUserIdAndProductId(int userId, int productId);

    @EntityGraph(attributePaths = {"product", "product.brand", "product.category", "product.images"})
    Wishlist findByUserIdAndProductId(int userId, int productId);
}

