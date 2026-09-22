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

    @Query("UPDATE ProductVariant p SET p.stockQuantity = p.stockQuantity - :qty " +
            "WHERE p.id = :id AND p.stockQuantity >= :qty")
    @Modifying
    int deductStockAtomically(
            @Param("id") @NotNull(message = "ProductId khong duoc de trong") @Positive(message = "ProductId phai la so duong") Integer id,
            @Param("qty") Integer quantity);

    boolean existsByColorId(int id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductVariant p where p.id = :productVariantId")
    Optional<ProductVariant> findByIdForUpdate(@Param("productVariantId") Integer productVariantId);
}
