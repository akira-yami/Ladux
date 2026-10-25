package org.akira.ladux.inventory.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.catalog.service.PricingService;
import org.akira.ladux.inventory.service.InventoryService;
import org.akira.ladux.inventory.service.result.LineDraft;
import org.akira.ladux.ordering.dto.OrderLineRequest;
import org.akira.ladux.ordering.service.impl.OrderServiceImpl;
import org.akira.ladux.shared.exception.InsufficientStockException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Quan ly ton kho tai thoi diem checkout — chong overselling bang UPDATE atomic.
// Chien luoc: UPDATE co dieu kien stockQuantity >= :qty thay vi lock bi quan dai.
// rowsAffected == 0 -> nem InsufficientStockException. CHECK constraint stock_quantity >= 0 (V9) lam luoi an toan.
// propagation = MANDATORY: chay trong transaction cua OrderServiceImpl.createOrder.
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final ProductVariantRepository productVariantRepository;
    private final PricingService pricingService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    @CacheEvict(value = "products", allEntries = true)
    public List<LineDraft> reserveStockAndPriceLines(List<OrderLineRequest> items) {
        List<LineDraft> drafts = new ArrayList<>();

        for (OrderLineRequest item : items.stream()
                .sorted(java.util.Comparator.comparing(OrderLineRequest::productId)).toList()) {
            // Bước 1: Trừ kho nguyên tử — một lệnh UPDATE, không cần giữ lock lâu.
            // SQL: UPDATE ProductVariant SET stockQuantity = stockQuantity - :qty
            //      WHERE id = :id AND stockQuantity >= :qty
            int updated = productVariantRepository.deductStockAtomically(item.productId(), item.quantity());
            if (updated == 0) {
                throw new InsufficientStockException("Không đủ hàng hoặc sản phẩm không tồn tại");
            }

            // Bước 2: Load product sau khi trừ thành công để lấy giá bán hiện tại.
            // Giá sẽ được chốt vào OrderItem.priceAtPurchase (snapshot — không đổi sau này).
            ProductVariant productVariant = productVariantRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            // The cart may already have loaded this entity before the bulk UPDATE.
            // Refresh while our UPDATE holds the row lock, including the current price.
            entityManager.refresh(productVariant);

            BigDecimal price = pricingService.sellingPrice(productVariant);
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.quantity()));

            drafts.add(new LineDraft(productVariant, item.quantity(), price, lineTotal));
        }
        return drafts;
    }
}
