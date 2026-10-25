package org.akira.ladux.ordering.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    // Page only IDs first so pagination always happens in PostgreSQL. Fetching a
    // collection (payments) in the paged query makes Hibernate paginate in memory.
    @Query("select o.id from Order o")
    Page<Integer> findAllIds(Pageable pageable);

    @Query("select o.id from Order o where o.status = :status")
    Page<Integer> findIdsByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "coupon", "payments"})
    @Query("select distinct o from Order o where o.id in :ids")
    List<Order> findSummariesByIdIn(@Param("ids") List<Integer> ids);

    // Lấy page ID đơn giản, KHÔNG join collection — tránh MultipleBagFetchException với Pageable
    @Query("select o.id from Order o where o.user.id = :userId order by o.id desc")
    Page<Integer> findIdsByUserId(@Param("userId") Integer userId, Pageable pageable);

    // Fetch đầy đủ items + product cho danh sách IDs đã biết (tránh MultipleBagFetchException khi join cả payments)
    @EntityGraph(attributePaths = {"user", "coupon", "items", "items.product", "items.productVariant"})
    @Query("select o from Order o where o.id in :ids")
    List<Order> findByIdIn(@Param("ids") List<Integer> ids);

    @EntityGraph(attributePaths = {"user", "items", "items.productVariant", "coupon"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findWithItemsById(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findWithItemsByIdForUpdate(@Param("id") Integer id);

    @Query("""
            select count(oi) > 0
            from OrderItem oi
            where oi.order.user.id = :userId
              and oi.productVariant.product.id = :productId
              and oi.order.status = :status
            """)
    boolean existsOrderContainingProductWithStatus(
            @Param("userId") Integer userId,
            @Param("productId") Integer productId,
            @Param("status") OrderStatus status
    );

    Optional<Order> findByUserIdAndId(int userId, int orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId AND o.user.id = :userId")
    Optional<Order> findOwnedWithItemsForUpdate(@Param("userId") int userId, @Param("orderId") int orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from Order o
            where o.status = :status
              and o.paymentExpiresAt is not null
              and o.paymentExpiresAt <= :now
            order by o.id
            """)
    List<Order> findExpiredOrdersForUpdate(@Param("status") OrderStatus status, @Param("now") Instant now, Pageable pageable);
}
