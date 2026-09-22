package org.akira.ladux.procurement.service;

import org.akira.ladux.procurement.dto.request.AdminPurchaseOrderReceiveRequest;
import org.akira.ladux.procurement.dto.request.PurchaseOrderCreateRequest;
import org.akira.ladux.procurement.dto.request.PurchaseOrderStatusUpdateRequest;
import org.akira.ladux.procurement.dto.response.PurchaseOrderResponse;
import org.akira.ladux.procurement.enums.PurchaseOrderStatus;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderService {

    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request, Integer createdByUserId);

    PurchaseOrderResponse getPurchaseOrderById(int id);

    PageResponse<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable);

    PageResponse<PurchaseOrderResponse> getPurchaseOrdersByStatus(PurchaseOrderStatus status, Pageable pageable);

    PageResponse<PurchaseOrderResponse> getPurchaseOrdersBySupplier(int supplierId, Pageable pageable);

    PurchaseOrderResponse updateStatus(int id, PurchaseOrderStatusUpdateRequest request);

    /** Nhan hang (toan bo/tung phan): cong ton kho + ghi stock movement + cap nhat trang thai. */
    PurchaseOrderResponse receiveGoods(int id, AdminPurchaseOrderReceiveRequest request, Integer receivedByUserId);
}
