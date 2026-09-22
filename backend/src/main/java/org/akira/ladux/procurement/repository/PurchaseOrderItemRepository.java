package org.akira.ladux.procurement.repository;

import java.util.List;
import org.akira.ladux.procurement.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Integer> {

    List<PurchaseOrderItem> findByPurchaseOrderId(Integer purchaseOrderId);
}
