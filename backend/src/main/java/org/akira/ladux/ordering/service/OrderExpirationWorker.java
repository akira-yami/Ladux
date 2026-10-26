package org.akira.ladux.ordering.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Huy MOT don het han thanh toan trong transaction rieng (REQUIRES_NEW).
 *
 * <p>Tach khoi job de: (1) loi o mot don khong rollback cac don khac trong lo,
 * (2) khoa dong chi giu trong thoi gian xu ly don do, (3) trang thai duoc kiem tra lai
 * sau khi khoa — webhook thanh toan co the da xac nhan don trong luc job chay.
 */
@Service
@RequiredArgsConstructor
public class OrderExpirationWorker {
    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService;

    /** @return true neu don da duoc huy boi lan goi nay. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expireIfStillPending(Integer orderId, Instant cutoff) {
        Order order = orderRepository.findWithItemsByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING
                || order.getPaymentExpiresAt() == null || order.getPaymentExpiresAt().isAfter(cutoff)) {
            return false;
        }
        orderLifecycleService.cancelOrder(order, "Đơn hàng tự động hủy do quá hạn thanh toán");
        return true;
    }
}
