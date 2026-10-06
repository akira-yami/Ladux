package org.akira.ladux.ordering.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.security.SecurityUtils;
import org.akira.ladux.notification.entity.Notification;
import org.akira.ladux.notification.enums.NotificationType;
import org.akira.ladux.notification.repository.NotificationRepository;
import org.akira.ladux.ordering.dto.request.OrderStatusUpdateRequest;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.entity.OrderHistory;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.event.OrderDeliveredEvent;
import org.akira.ladux.ordering.mapper.OrderMapper;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.OrderLifecycleService;
import org.akira.ladux.ordering.service.OrderStateMachine;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// State machine quan ly vong doi don hang.
// Chuyen trang thai hop le:
//   PENDING    -> CONFIRMED | CANCELLED
//   CONFIRMED  -> SHIPPED   | CANCELLED
//   SHIPPED    -> DELIVERED
//   CANCELLED, DELIVERED -> trang thai cuoi, khong chuyen tiep
// CONFIRMED thuong do confirmAfterSuccessfulPayment (luong thanh toan), khong qua updateOrderStatus.
// Huy don (-> CANCELLED) luon qua OrderLifecycleService.cancelOrder de hoan kho/coupon.
@Service
@RequiredArgsConstructor
public class OrderStateMachineImpl implements OrderStateMachine {
    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true)
    })
    public OrderResponse updateOrderStatus(int orderId, OrderStatusUpdateRequest request) {
        // Khóa bi quan order + items để tránh hai admin cùng đổi trạng thái song song.
        Order order = orderRepository.findWithItemsByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        User admin = resolveAdminUser(order);

        OrderStatus current = order.getStatus();
        OrderStatus target = request.status();
        if (current == target) {
            return OrderMapper.fromEntity(order);
        }

        validateTransition(current, target);
        if (target == OrderStatus.CANCELLED) {
            orderLifecycleService.cancelOrder(order, "Đơn hàng đã được người dùng hủy");
            sendOrderStatusNotification(order, target);
            return OrderMapper.fromEntity(order);
        }

        if (current == OrderStatus.RETURN_REQUESTED && target == OrderStatus.DELIVERED) {
            order.setStatus(target);
            order.getHistories().add(OrderHistory.builder()
                    .order(order)
                    .user(admin)
                    .status(target)
                    .description("Quản trị viên từ chối yêu cầu trả hàng của khách")
                    .build());
            sendReturnDecisionNotification(order, false);
            return OrderMapper.fromEntity(order);
        }

        if (target == OrderStatus.RETURNED) {
            OrderResponse resp = orderLifecycleService.processReturnOrder(orderId, "Quản trị viên xác nhận chấp nhận trả hàng và nhập lại kho", admin);
            sendReturnDecisionNotification(order, true);
            return resp;
        }

        if (target == OrderStatus.REFUNDED) {
            throw new BusinessRuleException("Hoàn tiền phải dùng endpoint /api/v1/admin/payments/order/{orderId}/refund để bảo đảm idempotency");
        }
        // Tao ma trackingNumber khi chuyen sang SHIPPED (tu dong neu khong duoc truyen hoac rong).
        if (target == OrderStatus.SHIPPED) {
            String trackingNumber = (request != null && request.trackingNumber() != null && !request.trackingNumber().isBlank())
                    ? request.trackingNumber().trim()
                    : generateTrackingNumber();
            order.setTrackingNumber(trackingNumber);
        }

        order.setStatus(target);
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .user(order.getUser())
                .status(target)
                .description("Trạng thái đơn hàng chuyển từ " + formatStatusVi(current) + " sang " + formatStatusVi(target))
                .build());
        sendOrderStatusNotification(order, target);
        if (target == OrderStatus.DELIVERED) {
            eventPublisher.publishEvent(new OrderDeliveredEvent(order));
        }
        return OrderMapper.fromEntity(order);
    }

    @Override
    @Scheduled(fixedDelayString = "${ladux.order-expiration.fixed-delay-ms:60000}")
    @Transactional
    @SchedulerLock(name = "expirePendingOrdersLock", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    @Caching(evict = {

            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "coupons", allEntries = true)
    })
    // Job dinh ky (mac dinh 60s): huy don PENDING qua paymentExpiresAt. ShedLock chan chay trung khi scale ngang.
    public void expirePendingOrders() {
        List<Order> expiredOrders = orderRepository.findExpiredOrdersForUpdate(OrderStatus.PENDING, Instant.now());
        for (Order order : expiredOrders) {
            orderLifecycleService.cancelOrder(order, "Payment window expired");
        }
    }

    // Kiem tra ma tran chuyen trang thai — nem BusinessRuleException neu khong hop le.
    private void validateTransition(OrderStatus current, OrderStatus target) {
        // 1. Các trạng thái cuối cùng, hoàn tất hoàn toàn không thể chuyển tiếp
        if (current == OrderStatus.CANCELLED || current == OrderStatus.REFUNDED) {
            throw new BusinessRuleException("Đơn hàng ở trạng thái " + current + " không thể chuyển trạng thái nữa");
        }

        // Đơn hàng đã giao (DELIVERED) thì admin không thể thay đổi trạng thái đơn hàng nữa
        if (current == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Đơn hàng đã được giao (DELIVERED), admin không thể thay đổi trạng thái đơn hàng.");
        }

        // 2. Kiểm tra điều kiện HỦY ĐƠN (CANCELLED)
        if (target == OrderStatus.CANCELLED) {
            if (current == OrderStatus.PENDING || current == OrderStatus.CONFIRMED) {
                return;
            }
            throw new BusinessRuleException("Chỉ hủy đơn khi đơn đang PENDING hoặc CONFIRMED");
        }

        // 3. Ma trận chuyển đổi trạng thái hợp lệ
        boolean allowed = switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED;
            case CONFIRMED -> target == OrderStatus.SHIPPED;
            case SHIPPED -> target == OrderStatus.DELIVERED;

            // LUỒNG ĐỔI TRẢ & HOÀN TIỀN:
            // Khi khách yêu cầu trả (RETURN_REQUESTED): Admin có thể duyệt (RETURNED) hoặc từ chối (DELIVERED)
            case RETURN_REQUESTED -> target == OrderStatus.RETURNED || target == OrderStatus.DELIVERED;
            case RETURNED -> target == OrderStatus.REFUNDED;

            default -> false;
        };

        if (!allowed) {
            throw new BusinessRuleException("Trạng thái đơn hàng không hợp lệ khi chuyển từ " + current + " sang " + target);
        }
    }
    private User resolveAdminUser(Order order) {
        try {
            Integer adminId = SecurityUtils.getCurrentUserId();
            if (adminId != null) {
                User user = userRepository.findById(adminId).orElse(null);
                if (user != null) {
                    return user;
                }
            }
        } catch (Exception ignored) {
        }
        return userRepository.findByUsername("admin").orElse(order.getUser());
    }

    private String formatStatusVi(OrderStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> "Chờ xử lý";
            case CONFIRMED -> "Đã xác nhận";
            case SHIPPED -> "Đang giao hàng";
            case DELIVERED -> "Đã giao thành công";
            case RETURN_REQUESTED -> "Yêu cầu trả hàng";
            case RETURNED -> "Đã trả hàng";
            case REFUNDED -> "Đã hoàn tiền";
            case CANCELLED -> "Đã hủy";
        };
    }

    private void sendOrderStatusNotification(Order order, OrderStatus status) {
        if (order.getUser() == null) return;
        String title;
        String message;
        switch (status) {
            case CONFIRMED -> {
                title = "Đơn hàng #" + order.getId() + " đã được xác nhận";
                message = "Đơn hàng #" + order.getId() + " của bạn đã được xác nhận và đang được chuẩn bị hàng.";
            }
            case SHIPPED -> {
                title = "Đơn hàng #" + order.getId() + " đang được giao";
                message = "Đơn hàng #" + order.getId() + " đã được xuất kho và đang trên đường giao đến bạn."
                        + (order.getTrackingNumber() != null ? " Mã vận đơn: " + order.getTrackingNumber() : "");
            }
            case DELIVERED -> {
                title = "Đơn hàng #" + order.getId() + " đã giao thành công";
                message = "Đơn hàng #" + order.getId() + " đã được giao thành công. Cảm ơn bạn đã mua sắm tại Ladux!";
            }
            case CANCELLED -> {
                title = "Đơn hàng #" + order.getId() + " đã bị hủy";
                message = "Đơn hàng #" + order.getId() + " của bạn đã được cập nhật sang trạng thái đã hủy.";
            }
            default -> {
                return;
            }
        }
        try {
            notificationRepository.save(Notification.builder()
                    .recipient(order.getUser())
                    .title(title)
                    .message(message)
                    .type(NotificationType.ORDER_STATUS)
                    .isRead(false)
                    .isDeletedByUser(false)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception ignored) {
        }
    }

    private void sendReturnDecisionNotification(Order order, boolean accepted) {
        if (order.getUser() == null) return;
        String title = accepted
                ? "Yêu cầu trả hàng đơn #" + order.getId() + " đã được chấp nhận"
                : "Yêu cầu trả hàng đơn #" + order.getId() + " đã bị từ chối";
        String message = accepted
                ? "Yêu cầu trả hàng cho đơn hàng #" + order.getId() + " của bạn đã được chấp nhận. Sản phẩm đã được nhận lại về kho."
                : "Yêu cầu trả hàng cho đơn hàng #" + order.getId() + " của bạn đã bị từ chối bởi quản trị viên.";
        try {
            notificationRepository.save(Notification.builder()
                    .recipient(order.getUser())
                    .title(title)
                    .message(message)
                    .type(NotificationType.ORDER_STATUS)
                    .isRead(false)
                    .isDeletedByUser(false)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception ignored) {
        }
    }

    private String generateTrackingNumber() {
         String prefix = "TRK" + UUID.randomUUID().toString().substring(0, 8);
         return prefix.toUpperCase();
    }
}
