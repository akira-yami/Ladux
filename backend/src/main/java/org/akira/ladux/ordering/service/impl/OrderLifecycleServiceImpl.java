package org.akira.ladux.ordering.service.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.inventory.enums.StockMovementType;
import org.akira.ladux.inventory.enums.StockReferenceType;
import org.akira.ladux.inventory.service.StockMovementService;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.entity.OrderHistory;
import org.akira.ladux.ordering.entity.OrderItem;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.mapper.OrderMapper;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.OrderLifecycleService;
import org.akira.ladux.promotion.entity.Coupon;
import org.akira.ladux.promotion.repository.CouponRepository;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Tach side-effect khoi state machine — xu ly hau qua khi don hang doi trang thai do thanh toan.
// Transaction propagation = MANDATORY: bat buoc chay trong transaction cua caller (PaymentWebhook,
// PaymentService, OrderStateMachine). Hoan kho + huy coupon + doi status cung commit hoac cung rollback.
// Khong goi truc tiep tu Controller — chi tu luong thanh toan hoac huy don.
// Idempotent: goi lai khi da CONFIRMED/CANCELLED khong gay side-effect trung lap.
// Luong huy don (cancelOrder): hoan kho -> ghi so cai RETURN_IN -> hoan coupon -> set CANCELLED -> ghi OrderHistory.
@Service
@RequiredArgsConstructor
public class OrderLifecycleServiceImpl implements OrderLifecycleService {

    private final ProductVariantRepository productVariantRepository;
    private final CouponRepository couponRepository;
    private final StockMovementService stockMovementService;
    private final OrderRepository orderRepository;

    // Xac nhan don sau thanh toan thanh cong: PENDING -> CONFIRMED, xoa han thanh toan, ghi audit trail.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmAfterSuccessfulPayment(Order order) {
        // Đơn đã hủy không thể quay lại — tránh race giữa webhook và job hết hạn.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Don hang da bi huy, khong the xac nhan thanh toan");
        }
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Don hang da duoc van chuyen, khong the cap nhat thanh toan");
        }
        // Idempotent: webhook VNPay có thể gọi lại nhiều lần khi payment đã SUCCESS.
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            order.setPaymentExpiresAt(null);
            return;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentExpiresAt(null);
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .user(order.getUser())
                .status(OrderStatus.CONFIRMED)
                .description("Thanh toán thành công qua cổng thanh toán")
                .build());
    }

    // Huy don kem hoan tac: tra kho, hoan coupon, ghi lich su. description = ly do huy.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelOrder(Order order, String description) {
        // Idempotent: đã CANCELLED thì bỏ qua, không hoàn kho lần hai.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.SHIPPED) {
            throw new BusinessRuleException("Don hang da duoc van chuyen, khong the huy");
        }

        releaseReservedInventory(order);
        rollbackCouponUsage(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentExpiresAt(null);
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .user(order.getUser())
                .status(OrderStatus.CANCELLED)
                .description(description)
                .build());
    }

    // Hoàn tiền cho khách (qua refundPayment) khi đơn đã RETURNED. Không hoàn kho nữa.
    @Override
    @Transactional
    public void refundOrder(Order order, String description) {
        if (order.getStatus() != OrderStatus.RETURNED) {
            throw new BusinessRuleException("Chỉ hoàn tiền cho đơn đã được trả lại");
        }
        // Logic để hoàn tiền (ví dụ: gọi API thanh toán)
        BigDecimal refundAmount = order.getFinalAmount();
    }

    // Hoan ton kho khi huy don: cong lai so luong da tru luc checkout, ghi so cai RETURN_IN (chi ghi so).
    private void releaseReservedInventory(Order order) {
        Integer orderRef = order.getId().intValue();
        for (OrderItem item : order.getItems()) {
            Integer productVariantId = item.getProductVariant().getId();
            ProductVariant productVariant = productVariantRepository.findByIdForUpdate(productVariantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay product variant voi id = " + productVariantId));
            productVariant.setStockQuantity(productVariant.getStockQuantity() + item.getQuantity());
            // Ton kho da duoc cong lai o tren -> chi GHI SO (RETURN_IN), tranh cong kep.
            stockMovementService.recordLedgerEntry(
                    productVariant,
                    item.getQuantity(),
                    StockMovementType.RETURN_IN,
                    StockReferenceType.ORDER,
                    orderRef,
                    "Hoàn kho do hủy hoặc hết hạn đơn #" + order.getId(),
                    order.getUser());
        }
    }

    // Giam usedCount cua coupon khi don bi huy — doi xung voi redeem luc tao don.
    private void rollbackCouponUsage(Order order) {
        if (order.getCoupon() == null) {
            return;
        }
        Integer couponId = order.getCoupon().getId();
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay coupon voi id = " + couponId));
        if (coupon.getUsedCount() > 0) {
            coupon.setUsedCount(coupon.getUsedCount() - 1);
        }
    }

    @Override
    @Transactional
    public OrderResponse processReturnOrder(int orderId, String reason, User admin) {
        Order order = orderRepository.findWithItemsByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng id = " + orderId));
        // B1: Chỉ cho phép trả hàng khi đơn đã DELIVERED hoặc khách đã gửi RETURN_REQUESTED
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessRuleException("Chỉ đơn hàng đã giao thành công hoặc có yêu cầu trả mới được nhận lại hàng");
        }

        // B2: Cập nhật trạng thái sang RETURNED (Đã nhận lại hàng về kho)
        order.setStatus(OrderStatus.RETURNED);

        // B3: HOÀN TỒN KHO - Tăng lại stock_quantity cho từng Variant
        Integer orderRef = order.getId().intValue();
        for (OrderItem item : order.getItems()) {
            stockMovementService.recordMovement(
                    item.getProductVariant(),
                    item.getQuantity(), // Số lượng dương (+) = Nhập lại kho
                    StockMovementType.RETURN_IN,
                    StockReferenceType.ORDER,
                    orderRef,
                    "Hoàn kho từ đơn trả hàng #" + order.getId() + ". Lý do: " + reason,
                    admin
            );
        }

        // B4: Ghi vết lịch sử đơn hàng (Audit Trail)
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .user(admin)
                .status(OrderStatus.RETURNED)
                .description("Xác nhận đã nhận lại hàng về kho. Lý do: " + reason)
                .build());

        return OrderMapper.fromEntity(orderRepository.save(order));
    }
}
