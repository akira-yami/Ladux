package org.akira.ladux.ordering.repository;

import java.util.Optional;
import org.akira.ladux.ordering.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    boolean existsByProductVariantId(Integer productVariantId);

    @EntityGraph(attributePaths = {"order", "product"})
    @Override
    Page<OrderItem> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"order", "product"})
    @Override
    Optional<OrderItem> findById(Integer id);

    @EntityGraph(attributePaths = {"order", "product"})
    Page<OrderItem> findByOrderId(Integer orderId, Pageable pageable);
}

