package org.akira.ladux.ordering.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.inventory.enums.StockMovementType;
import org.akira.ladux.inventory.enums.StockReferenceType;
import org.akira.ladux.inventory.service.InventoryService;
import org.akira.ladux.inventory.service.StockMovementService;
import org.akira.ladux.inventory.service.result.LineDraft;
import org.akira.ladux.notification.entity.Notification;
import org.akira.ladux.notification.enums.NotificationType;
import org.akira.ladux.notification.repository.NotificationRepository;
import org.akira.ladux.ordering.dto.OrderLineRequest;
import org.akira.ladux.ordering.dto.request.OrderRequest;
import org.akira.ladux.ordering.dto.request.OrderStatusUpdateRequest;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.entity.Cart;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.entity.OrderHistory;
import org.akira.ladux.ordering.entity.OrderItem;
import org.akira.ladux.ordering.entity.ShippingAddress;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.mapper.OrderMapper;
import org.akira.ladux.ordering.repository.CartRepository;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.OrderLifecycleService;
import org.akira.ladux.ordering.service.OrderService;
import org.akira.ladux.ordering.service.OrderStateMachine;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.service.PaymentAttemptService;
import org.akira.ladux.promotion.entity.Coupon;
import org.akira.ladux.promotion.service.CouponRedemptionService;
import org.akira.ladux.promotion.service.result.CouponRedemptionResult;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

// Trung tam luong checkout — tao don tu gio hang.
// Luong createOrder (mot transaction):
//   1. Kiem tra user active
//   2. Lay cart co khoa (FOR UPDATE)
//   3. Tru kho atomic + chot gia (InventoryService)
//   4. Redeem coupon (CouponRedemptionService)
//   5. Tao Order PENDING + OrderItem + OrderHistory
//   6. Khoi tao Payment PENDING (PaymentAttemptService)
//   7. Ghi so cai SALE_OUT (StockMovementService.recordLedgerEntry)
//   8. Don sach gio hang
// Chong IDOR: getOrderById doi chieu order.user.id == userId.
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository repo;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final NotificationRepository notificationRepository;
    private final InventoryService inventoryService;
    private final CouponRedemptionService couponRedemptionService;
    private final PaymentAttemptService paymentAttemptService;
    private final OrderStateMachine orderStateMachine;
    private final StockMovementService stockMovementService;
    private final OrderLifecycleService orderLifecycleService;

    @Value("${app.order.shipping-fee:30000}")
    private BigDecimal configuredShippingFee;

    @Value("${app.order.carrier:VNPOST}")
    private String configuredCarrier;

    /** Chong giu kho: COD khong co han thanh toan nen gioi han so don dang cho xu ly cua moi khach. */
    @Value("${app.order.max-pending-per-user:5}")
    private int maxPendingOrdersPerUser = 5;

    /** So ngay toi da sau khi giao de khach yeu cau tra hang. */
    @Value("${app.order.return-window-days:7}")
    private int returnWindowDays = 7;

    @jakarta.annotation.PostConstruct
    void validateOrderPolicy() {
        if (configuredShippingFee == null || configuredShippingFee.signum() < 0) {
            throw new IllegalStateException("app.order.shipping-fee must be >= 0");
        }
        if (maxPendingOrdersPerUser < 1 || returnWindowDays < 0) {
            throw new IllegalStateException("app.order.max-pending-per-user must be >= 1 and return-window-days >= 0");
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'v2:all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        return toSummaryPage(repo.findAllIds(pageable), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'v2:user:' + #userId + ':order:' + #orderId")
    public OrderResponse getOrderById(int userId, int orderId) {
        // Lấy đơn hàng kèm danh sách item để phục vụ kiểm tra quyền và hiển thị chi tiết.
        Order order = repo.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // Chỉ chủ đơn mới được xem; trả 404 giống đơn không tồn tại để không fffn của người khác.
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng");
        }
        return OrderMapper.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'v2:admin:order:' + #orderId")
    public OrderResponse getOrderByIdForAdmin(int orderId) {
        Order order = repo.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return OrderMapper.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByUserId(int userId, Pageable pageable) {
        // Bước 1: Lấy page IDs với pagination đúng — query đơn giản, không JOIN collection.
        Page<Integer> idPage = repo.findIdsByUserId(userId, pageable);
        if (idPage.isEmpty()) {
            return PageResponse.from(idPage.map(id -> (OrderResponse) null)); // trả empty page giữ metadata
        }
        // Bước 2: Fetch đầy đủ entity (có items + payments) theo IDs đã biết.
        List<Order> orders = repo.findByIdIn(idPage.getContent());
        // Giữ thứ tự của page gốc (sort theo pageable).
        Map<Integer, Order> byId = orders.stream()
                .collect(java.util.stream.Collectors.toMap(Order::getId, o -> o));
        List<OrderResponse> content = idPage.getContent().stream()
                .map(id -> OrderMapper.fromEntity(byId.get(id)))
                .filter(r -> r != null)
                .toList();
        return PageResponse.from(new org.springframework.data.domain.PageImpl<>(content, pageable, idPage.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'v2:status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public PageResponse<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return toSummaryPage(repo.findIdsByStatus(status, pageable), pageable);
    }

    private PageResponse<OrderResponse> toSummaryPage(Page<Integer> idPage, Pageable pageable) {
        if (idPage.isEmpty()) {
            return PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, idPage.getTotalElements()));
        }
        Map<Integer, Order> ordersById = repo.findSummariesByIdIn(idPage.getContent()).stream()
                .collect(java.util.stream.Collectors.toMap(Order::getId, order -> order));
        List<OrderResponse> content = idPage.getContent().stream()
                .map(ordersById::get)
                .filter(java.util.Objects::nonNull)
                .map(OrderMapper::summaryFromEntity)
                .toList();
        return PageResponse.from(new org.springframework.data.domain.PageImpl<>(content, pageable, idPage.getTotalElements()));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderItems", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "coupons", allEntries = true),
            @CacheEvict(value = "carts", allEntries = true)
    })
    public OrderResponse createOrder(int userId, OrderRequest request) {
        // B1: kiểm tra user tồn tại hay không.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user voi id = " + userId));

        // B2: chỉ cho phép đặt hàng nếu tài khoản còn hoạt động.
        if (!user.isActive()) {
            throw new BusinessRuleException("Tai khoan dang bi khoa, khong the dat hang");
        }
        if (repo.countByUserIdAndStatus(userId, OrderStatus.PENDING) >= maxPendingOrdersPerUser) {
            throw new BusinessRuleException("Bạn đang có quá nhiều đơn hàng chờ xử lý. Vui lòng thanh toán hoặc hủy bớt đơn trước khi đặt thêm");
        }

        // B3: lấy giỏ hàng của user (kèm khóa để tránh race), bắt buộc phải có hàng.
        Cart cart = cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessRuleException("Gio hang trong, khong the tao don hang"));
        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleException("Gio hang trong, khong the tao don hang");
        }

        // B4: ánh xạ từng item trong giỏ thành dòng đặt hàng (user không cần nhập tay).
        List<OrderLineRequest> lineRequests = cart.getItems().stream()
                .map(item -> new OrderLineRequest(item.getProductVariant().getId(), item.getQuantity()))
                .toList();

        // B5: khóa tồn kho từng sản phẩm, kiểm tra đủ số lượng, đồng thời tính giá tại thời điểm mua.
        List<LineDraft> lineDrafts = inventoryService.reserveStockAndPriceLines(lineRequests);

        // B6: cộng tổng tiền trước khi giảm giá.
        BigDecimal subTotal = lineDrafts.stream()
                .map(LineDraft::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);

        CouponRedemptionResult redemption = couponRedemptionService.redeem(userId, request.couponCode(), subTotal);
        Coupon coupon = redemption.coupon();
        // Phong thu: giam gia khong bao gio am hoac vuot subTotal, ke ca khi cau hinh coupon sai.
        BigDecimal discountAmount = redemption.discountAmount().max(BigDecimal.ZERO).min(subTotal);
        BigDecimal shippingFee = configuredShippingFee;
        String carrier = configuredCarrier;
        // B7: tính số tiền cuối cùng sau khi trừ giảm giá, đảm bảo không âm.
        BigDecimal finalAmount = subTotal.subtract(discountAmount)
                .add(shippingFee)
                .max(BigDecimal.ZERO)
                .setScale(0, RoundingMode.HALF_UP);
        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            finalAmount = BigDecimal.ZERO;
        }
        if (request.expectedAmount() != null && finalAmount.compareTo(request.expectedAmount()) != 0)
            throw new BusinessRuleException("Giá, giỏ hàng hoặc ưu đãi đã thay đổi. Vui lòng kiểm tra lại tổng tiền trước khi đặt hàng");
        ShippingAddress shippingAddress = ShippingAddress.builder()
                .receiverName(request.shippingAddress().receiverName())
                .phone(request.shippingAddress().phone())
                .street(request.shippingAddress().street())
                .ward(request.shippingAddress().ward())
                .district(request.shippingAddress().district())
                .city(request.shippingAddress().city())
                .build();
        Order order = Order.builder()
                .coupon(coupon)
                .subTotal(subTotal)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .user(user)
                .carrierName(carrier)
                .shippingFee(shippingFee)
                .build();

        // B8: thêm các dòng sản phẩm vào đơn hàng.
        for (LineDraft draft : lineDrafts) {
            ProductVariant variant = draft.productVariant();
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(variant == null ? null : variant.getProduct())
                    .productVariant(variant)
                    .quantity(draft.quantity())
                    .priceAtPurchase(draft.priceAtPurchase())
                    .build());
        }

        // B9: ghi lại lịch sử khởi tạo đơn để audit sau này.
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .user(order.getUser())
                .status(OrderStatus.PENDING)
                .description("Đơn hàng đã được tạo thành công")
                .build());

        // B10: tạo payment ban đầu và set hạn thanh toán.
        paymentAttemptService.initializePayment(order, request.paymentProvider(), finalAmount);

        OrderResponse response = OrderMapper.fromEntity(repo.save(order));

        // B11: ghi so cai bien dong kho (SALE_OUT) cho tung dong — ton kho da bi tru atomic o B5,
        // nen chi GHI SO (recordLedgerEntry) de tranh tru kep. Tham chieu ve don hang vua tao.
        Integer orderRef = order.getId() == null ? null : order.getId().intValue();
        for (LineDraft draft : lineDrafts) {
            stockMovementService.recordLedgerEntry(
                    draft.productVariant(),
                    -draft.quantity(),
                    StockMovementType.SALE_OUT,
                    StockReferenceType.ORDER,
                    orderRef,
                    "Bán hàng từ đơn #" + order.getId(),
                    user);
        }

        // B12: dat hang thanh cong thi don sach gio (orphanRemoval se xoa cart_items khi flush).
        cart.getItems().clear();

        return response;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true)
    })
    public OrderResponse updateOrderStatus(int orderId, OrderStatusUpdateRequest request) {
        return orderStateMachine.updateOrderStatus(orderId, request);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "coupons", allEntries = true)
    })
    public void expirePendingOrders() {
        orderStateMachine.expirePendingOrders();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "payments", allEntries = true)
    })
    public PaymentCallbackResponse retryPayment(int userId, int orderId, String clientIp) {
        return paymentAttemptService.retryPayment(userId, orderId, clientIp);
    }
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "coupons", allEntries = true),
            @CacheEvict(value = "payments", allEntries = true)
    })
    public void cancelOrder(int userId, Integer orderId) {
        // Ownership and row lock must be checked atomically to prevent IDOR and cancel/payment races.
        Order order = repo.findOwnedWithItemsForUpdate(userId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Chỉ có thể hủy đơn hàng đang ở trạng thái PENDING");
        }
        orderLifecycleService.cancelOrder(order, "Đơn hàng được người dùng hủy");
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true)
    })
    public OrderResponse requestReturn(int userId, int orderId, String rawReason) {
        final String reason = rawReason == null ? null : rawReason.trim();
        // Khoa + kiem tra chu so huu trong mot truy van; don cua nguoi khac tra 404 nhu don khong ton tai.
        Order order = repo.findOwnedWithItemsForUpdate(userId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Chỉ có thể yêu cầu trả hàng khi đơn hàng đã ở trạng thái ĐÃ GIAO HÀNG (DELIVERED)");
        }
        // Moi don chi duoc yeu cau tra mot lan; neu admin da tu choi, khach lien he CSKH thay vi spam yeu cau.
        boolean requestedBefore = order.getHistories().stream()
                .anyMatch(history -> history.getStatus() == OrderStatus.RETURN_REQUESTED);
        if (requestedBefore) {
            throw new BusinessRuleException("Đơn hàng này đã từng gửi yêu cầu trả hàng. Vui lòng liên hệ CSKH để được hỗ trợ");
        }
        java.time.Instant deliveredAt = order.getHistories().stream()
                .filter(history -> history.getStatus() == OrderStatus.DELIVERED && history.getCreatedAt() != null)
                .map(OrderHistory::getCreatedAt)
                .max(java.util.Comparator.naturalOrder())
                .orElse(order.getUpdatedAt());
        if (deliveredAt != null
                && deliveredAt.plus(java.time.Duration.ofDays(returnWindowDays)).isBefore(java.time.Instant.now())) {
            throw new BusinessRuleException("Đã quá thời hạn " + returnWindowDays + " ngày để yêu cầu trả hàng");
        }
        if (reason != null && reason.length() > 500) {
            throw new BusinessRuleException("Lý do trả hàng không được vượt quá 500 ký tự");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnReason(reason);
        String desc = "Khách hàng yêu cầu trả hàng" + (reason != null && !reason.isBlank() ? ". Lý do: " + reason : "");
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .user(order.getUser())
                .status(OrderStatus.RETURN_REQUESTED)
                .description(desc)
                .build());

        Order savedOrder = repo.save(order);

        // Gửi thông báo đến tài khoản admin duy nhất
        userRepository.findByUsername("admin").ifPresent(admin -> {
            String customerName = (order.getUser() != null && order.getUser().getCustomer() != null && order.getUser().getCustomer().getFullName() != null)
                    ? order.getUser().getCustomer().getFullName()
                    : (order.getUser() != null ? order.getUser().getUsername() : "Khách hàng");
            String reasonText = (reason != null && !reason.isBlank()) ? reason : "Không có lý do cụ thể";
            Notification notification = Notification.builder()
                    .recipient(admin)
                    .title("Yêu cầu trả hàng đơn #" + orderId)
                    .message("Khách hàng " + customerName + " yêu cầu trả hàng đơn #" + orderId + ". Lý do: " + reasonText)
                    .type(NotificationType.ORDER_STATUS)
                    .isRead(false)
                    .isDeletedByUser(false)
                    .createdAt(java.time.Instant.now())
                    .build();
            notificationRepository.save(notification);
        });

        return OrderMapper.fromEntity(savedOrder);
    }
}
