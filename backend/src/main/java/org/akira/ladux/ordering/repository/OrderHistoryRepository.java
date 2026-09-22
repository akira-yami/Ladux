package org.akira.ladux.ordering.repository;

import java.util.Optional;
import org.akira.ladux.ordering.entity.OrderHistory;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Integer> {
    @EntityGraph(attributePaths = {"order"})
    @Override
    @NonNull
    Page<OrderHistory> findAll(@NonNull Pageable pageable);

    @NonNull
    @EntityGraph(attributePaths = {"order"})
    @Override
    Optional<OrderHistory> findById(@NonNull Integer id);

    @EntityGraph(attributePaths = {"order"})
    Page<OrderHistory> findByOrderId(@NonNull Integer orderId, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"order"})
    @NonNull
    Page<OrderHistory> findByUserId(@NonNull Integer userId, @NonNull Pageable pageable);
}

