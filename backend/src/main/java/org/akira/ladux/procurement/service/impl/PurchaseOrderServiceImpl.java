package org.akira.ladux.procurement.service.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.inventory.enums.StockMovementType;
import org.akira.ladux.inventory.enums.StockReferenceType;
import org.akira.ladux.inventory.service.StockMovementService;
import org.akira.ladux.procurement.dto.request.AdminPurchaseOrderItemRequest;
import org.akira.ladux.procurement.dto.request.AdminPurchaseOrderReceiveRequest;
import org.akira.ladux.procurement.dto.request.PurchaseOrderCreateRequest;
import org.akira.ladux.procurement.dto.request.PurchaseOrderStatusUpdateRequest;
import org.akira.ladux.procurement.dto.response.PurchaseOrderResponse;
import org.akira.ladux.procurement.entity.PurchaseOrder;
import org.akira.ladux.procurement.entity.PurchaseOrderItem;
import org.akira.ladux.procurement.entity.Supplier;
import org.akira.ladux.procurement.enums.PurchaseOrderStatus;
import org.akira.ladux.procurement.mapper.PurchaseOrderMapper;
import org.akira.ladux.procurement.repository.ProductSupplierRepository;
import org.akira.ladux.procurement.repository.PurchaseOrderRepository;
import org.akira.ladux.procurement.repository.SupplierRepository;
import org.akira.ladux.procurement.service.PurchaseOrderService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Quan ly don mua hang tu nha cung cap (luong nhap kho).
// Vong doi: PENDING -> CONFIRMED -> PARTIALLY_RECEIVED -> RECEIVED (hoac CANCELLED).
// receiveGoods: moi lan nhan hang -> cong ton kho + ghi PURCHASE_IN. Tu dong RECEIVED khi nhan du.
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository repo;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final UserRepository userRepository;
    private final StockMovementService stockMovementService;

    @Override
    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request, Integer createdByUserId) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nha cung cap id = " + request.supplierId()));

        User createdBy = createdByUserId == null ? null
                : userRepository.findById(createdByUserId).orElse(null);

        PurchaseOrder po = PurchaseOrder.builder()
                .supplier(supplier)
                .status(PurchaseOrderStatus.PENDING)
                .note(request.note())
                .createdBy(createdBy)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (AdminPurchaseOrderItemRequest line : request.items()) {
            ProductVariant productVariant = productVariantRepository.findById(line.productVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham id = " + line.productVariantId()));

            boolean isLinked = productSupplierRepository.existsByProductIdAndSupplierId(productVariant.getProduct().getId(), supplier.getId());
            if (!isLinked) {
                throw new BusinessRuleException("San pham '" + productVariant.getProduct().getName() + "' (ID " + productVariant.getProduct().getId() + ") chưa duoc lien ket voi nha cung cap '" + supplier.getName() + "'");
            }

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .productVariant(productVariant)
                    .quantity(line.quantity())
                    .costPrice(line.costPrice())
                    .receivedQuantity(0)
                    .note(line.note())
                    .build();
            po.getItems().add(item);
            total = total.add(line.costPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }
        po.setTotalAmount(total);

        return PurchaseOrderMapper.fromEntity(repo.save(po));
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(int id) {
        return PurchaseOrderMapper.fromEntity(repo.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don mua hang id = " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable).map(PurchaseOrderMapper::summaryFromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> getPurchaseOrdersByStatus(PurchaseOrderStatus status, Pageable pageable) {
        return PageResponse.from(repo.findByStatus(status, pageable).map(PurchaseOrderMapper::summaryFromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> getPurchaseOrdersBySupplier(int supplierId, Pageable pageable) {
        return PageResponse.from(repo.findBySupplierId(supplierId, pageable).map(PurchaseOrderMapper::summaryFromEntity));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse updateStatus(int id, PurchaseOrderStatusUpdateRequest request) {
        PurchaseOrder po = repo.findWithItemsByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don mua hang id = " + id));

        PurchaseOrderStatus current = po.getStatus();
        PurchaseOrderStatus target = request.status();

        if (current == PurchaseOrderStatus.RECEIVED || current == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Don mua hang da o trang thai cuoi (" + current + "), khong the doi");
        }
        if (target == PurchaseOrderStatus.CANCELLED
                && !(current == PurchaseOrderStatus.PENDING || current == PurchaseOrderStatus.CONFIRMED)) {
            throw new BusinessRuleException("Chi huy don khi dang PENDING hoac CONFIRMED");
        }
        if (!(current == PurchaseOrderStatus.PENDING && target == PurchaseOrderStatus.CONFIRMED)
                && target != PurchaseOrderStatus.CANCELLED && target != current)
            throw new BusinessRuleException("Trạng thái nhận hàng chỉ được cập nhật bằng nghiệp vụ nhận hàng");
        po.setStatus(target);
        return PurchaseOrderMapper.fromEntity(po);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse receiveGoods(int id, AdminPurchaseOrderReceiveRequest request, Integer receivedByUserId) {
        PurchaseOrder po = repo.findWithItemsByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don mua hang id = " + id));

        if (po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Don mua hang da bi huy, khong the nhan hang");
        }
        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new BusinessRuleException("Don mua hang da nhan du, khong the nhan them");
        }

        User receivedBy = receivedByUserId == null ? null
                : userRepository.findById(receivedByUserId).orElse(null);

        java.util.Set<Integer> receivedLines = new java.util.HashSet<>();
        for (AdminPurchaseOrderReceiveRequest.ReceiveLine line : request.lines().stream()
                .sorted(java.util.Comparator.comparing(line -> po.getItems().stream()
                        .filter(item -> item.getId().equals(line.itemId())).findFirst()
                        .orElseThrow(() -> new BusinessRuleException("Dòng nhận hàng không thuộc đơn mua"))
                        .getProductVariant().getId())).toList()) {
            if (!receivedLines.add(line.itemId()) || (line.receivedQuantity() != null && line.receivedQuantity() < 0))
                throw new BusinessRuleException("Dòng nhận hàng bị lặp hoặc có số lượng âm");
            if (line.receivedQuantity() == null || line.receivedQuantity() == 0) {
                continue;
            }
            PurchaseOrderItem item = po.getItems().stream()
                    .filter(i -> i.getId().equals(line.itemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "Dong " + line.itemId() + " khong thuoc don mua hang " + id));

            int alreadyReceived = item.getReceivedQuantity() == null ? 0 : item.getReceivedQuantity();
            int newReceived = alreadyReceived + line.receivedQuantity();
            if (newReceived > item.getQuantity()) {
                throw new BusinessRuleException("So luong nhan vuot qua so luong dat cho dong " + item.getId());
            }

            // Cong ton kho + ghi stock movement (PURCHASE_IN), khoa product de tranh race.
            ProductVariant productVariant = productVariantRepository.findByIdForUpdate(item.getProductVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Khong tim thay san pham id = " + item.getProductVariant().getId()));
            stockMovementService.recordMovement(
                    productVariant,
                    line.receivedQuantity(),
                    StockMovementType.PURCHASE_IN,
                    StockReferenceType.PURCHASE_ORDER,
                    po.getId().intValue(),
                    "Nhận hàng từ đơn mua #" + po.getId(),
                    receivedBy);

            item.setReceivedQuantity(newReceived);
        }

        po.setStatus(resolveStatusAfterReceive(po));
        return PurchaseOrderMapper.fromEntity(po);
    }

    private PurchaseOrderStatus resolveStatusAfterReceive(PurchaseOrder po) {
        boolean allReceived = po.getItems().stream()
                .allMatch(i -> (i.getReceivedQuantity() == null ? 0 : i.getReceivedQuantity()) >= i.getQuantity());
        if (allReceived) {
            return PurchaseOrderStatus.RECEIVED;
        }
        boolean anyReceived = po.getItems().stream()
                .anyMatch(i -> (i.getReceivedQuantity() == null ? 0 : i.getReceivedQuantity()) > 0);
        return anyReceived ? PurchaseOrderStatus.PARTIALLY_RECEIVED : po.getStatus();
    }
}
