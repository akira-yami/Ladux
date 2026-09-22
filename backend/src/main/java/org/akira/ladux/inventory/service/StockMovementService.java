package org.akira.ladux.inventory.service;

import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.inventory.dto.request.StockMovementRequest;
import org.akira.ladux.inventory.dto.response.StockMovementResponse;
import org.akira.ladux.inventory.entity.StockMovement;
import org.akira.ladux.inventory.enums.StockMovementType;
import org.akira.ladux.inventory.enums.StockReferenceType;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface StockMovementService {

    /** Tao bien dong kho thu cong (dieu chinh kiem ke, hang hong...). */
    StockMovementResponse createAdjustment(StockMovementRequest request, Integer createdByUserId);

    PageResponse<StockMovementResponse> getMovementsByProduct(int productId, Pageable pageable);

    PageResponse<StockMovementResponse> getAllMovements(Pageable pageable);

    /**
     * Ghi nhan bien dong kho + cap nhat ton kho cua product (1 noi duy nhat).
     * signedQuantity: + nhap kho, - xuat kho. Dung tu cac luong khac (vd nhan hang PO).
     * Yeu cau chay trong transaction cua caller.
     */
    StockMovement recordMovement(
            ProductVariant productVariant,
            int signedQuantity,
            StockMovementType movementType,
            StockReferenceType referenceType,
            Integer referenceId,
            String note,
            User createdBy
    );

    /**
     * CHI GHI SO bien dong kho — KHONG thay doi product.stockQuantity.
     * Dung cho cac luong da tu thay doi ton kho bang co che rieng (tranh tru/cong KEP), vi du:
     *  - Ban hang: ton da bi tru boi {@code deductStockAtomically} (UPDATE atomic chong oversell).
     *  - Huy/het han don: ton da duoc cong lai truc tiep tren entity khi giai phong giu cho.
     * Yeu cau chay trong transaction cua caller.
     */
    StockMovement recordLedgerEntry(
            ProductVariant productVariant,
            int signedQuantity,
            StockMovementType movementType,
            StockReferenceType referenceType,
            Integer referenceId,
            String note,
            User createdBy
    );
}
