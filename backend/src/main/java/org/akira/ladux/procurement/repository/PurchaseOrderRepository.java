package org.akira.ladux.procurement.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.akira.ladux.procurement.entity.PurchaseOrder;
import org.akira.ladux.procurement.enums.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    Page<PurchaseOrder> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    Page<PurchaseOrder> findByStatus(PurchaseOrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    Page<PurchaseOrder> findBySupplierId(Integer supplierId, Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "createdBy", "items", "items.productVariant"})
    @Query("select po from PurchaseOrder po where po.id = :id")
    Optional<PurchaseOrder> findWithItemsById(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select po from PurchaseOrder po where po.id = :id")
    Optional<PurchaseOrder> findWithItemsByIdForUpdate(@Param("id") Integer id);
}
