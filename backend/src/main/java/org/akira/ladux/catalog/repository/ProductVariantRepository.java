package org.akira.ladux.catalog.repository;

import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Optional;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    @Override
    @EntityGraph(attributePaths = {"product", "color"})
    Optional<ProductVariant> findById(Integer id);

    boolean existsBySku(String candidate);

    @Query("UPDATE ProductVariant p SET p.stockQuantity = p.stockQuantity - :qty, p.version = p.version + 1 " +
            "WHERE p.id = :id AND :qty > 0 AND p.stockQuantity >= :qty AND p.isActive = true " +
            "AND EXISTS (SELECT product.id FROM Product product WHERE product.id = p.product.id " +
            "AND product.isActive = true AND product.category.isActive = true)")
    @Modifying
    int deductStockAtomically(
            @Param("id") @NotNull(message = "ProductId khong duoc de trong") @Positive(message = "ProductId phai la so duong") Integer id,
            @Param("qty") Integer quantity);

    @Modifying
    @Query(value = """
            UPDATE product_variants SET stock_quantity = stock_quantity + :delta, version = version + 1
            WHERE id = :id AND CAST(stock_quantity AS bigint) + :delta BETWEEN 0 AND 2147483647
            """, nativeQuery = true)
    int adjustStockAtomically(@Param("id") Integer id, @Param("delta") int delta);

    boolean existsByColorId(int id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductVariant p where p.id = :productVariantId")
    Optional<ProductVariant> findByIdForUpdate(@Param("productVariantId") Integer productVariantId);
}
