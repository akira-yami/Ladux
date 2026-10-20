package org.akira.ladux.catalog.repository;

import java.math.BigDecimal;
import java.util.List;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** A bounded, live read: no collection fetch joins that would move pagination into memory. */
public interface SmartFitCatalogRepository extends JpaRepository<ProductVariant, Integer> {
    @EntityGraph(attributePaths = "product")
    @Query("""
            select v from ProductVariant v
            where v.isActive = true and v.product.isActive = true and v.stockQuantity > 0
              and coalesce(v.discountPrice, v.price) > 0
              and coalesce(v.discountPrice, v.price) <= :budget
            order by v.id asc
            """)
    List<ProductVariant> findEligibleCandidates(@Param("budget") BigDecimal budget, Pageable pageable);
}
