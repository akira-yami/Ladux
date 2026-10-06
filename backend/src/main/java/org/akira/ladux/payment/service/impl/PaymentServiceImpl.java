package org.akira.ladux.payment.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.ordering.dto.response.OrderResponse;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.ordering.entity.OrderHistory;
import org.akira.ladux.ordering.enums.OrderStatus;
import org.akira.ladux.ordering.mapper.OrderMapper;
import org.akira.ladux.ordering.repository.OrderRepository;
import org.akira.ladux.ordering.service.OrderLifecycleService;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.dto.request.PaymentCallbackRequest;
import org.akira.ladux.payment.dto.request.PaymentCreateRequest;
import org.akira.ladux.payment.dto.response.PaymentCallbackResponse;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.service.RefundWorkflowService;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.mapper.PaymentCallbackMapper;
import org.akira.ladux.payment.repository.PaymentRepository;
import org.akira.ladux.payment.service.PaymentService;
import org.akira.ladux.payment.service.VNPayPaymentUrlService;
import org.akira.ladux.payment.util.VNPayUtils;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

// CRUD payment va dieu phoi trang thai thanh toan tu phia client/admin.
// createPayment idempotent: SUCCESS -> chan tao moi; PENDING -> tra lai payment hien tai; FAILED -> tao attempt moi.
// Khong set order.status truc tiep — luon goi OrderLifecycleService.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository repo;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final VNPayPaymentUrlService vnPayPaymentUrlService;
    private final VNPayProperties vnPayProperties;
    private final RefundWorkflowService refundWorkflowService;
    private final RestTemplate restTemplate = createRefundRestTemplate();

    private static RestTemplate createRefundRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return new RestTemplate(factory);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<PaymentCallbackResponse> getAllPayments(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable)
                .map(PaymentCallbackMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "'id:' + #id")
    public PaymentCallbackResponse getPaymentById(int id) {
        return PaymentCallbackMapper.fromEntity(repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay payment voi id = " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "'my:' + #userId + ':order:' + #orderId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<PaymentCallbackResponse> getMyPaymentsByOrderId(int userId, int orderId, Pageable pageable) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay order"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Ban khong co quyen xem thong tin thanh toan cua don hang nay");
        }

        return PageResponse.from(repo.findByOrderId(orderId, pageable)
                .map(PaymentCallbackMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "'my:' + #userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<PaymentCallbackResponse> getMyPayments(int userId, Pageable pageable) {
        return PageResponse.from(repo.findByOrder_User_Id(userId, pageable)
                .map(PaymentCallbackMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "'my:' + #userId + ':status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<PaymentCallbackResponse> getMyPaymentsByStatus(int userId, PaymentStatus status, Pageable pageable) {
        return PageResponse.from(repo.findByOrder_User_IdAndStatus(userId, status, pageable)
                .map(PaymentCallbackMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "'status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<PaymentCallbackResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        return PageResponse.from(repo.findByStatus(status, pageable)
                .map(PaymentCallbackMapper::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "'order:' + #orderId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<PaymentCallbackResponse> getPaymentsByOrderId(int orderId, Pageable pageable) {
        return PageResponse.from(repo.findByOrderId(orderId, pageable)
                .map(PaymentCallbackMapper::fromEntity));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "payments", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true)
    })
    public PaymentCallbackResponse createPayment(int userId, PaymentCreateRequest request, String clientIp) {
        Order order = orderRepository.findByIdForUpdate(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay order voi id = " + request.orderId()));
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Ban khong co quyen tao thanh toan cho don hang nay");
        }

        ensureOrderCanAcceptPayment(order);

        Optional<Payment> lastPaymentOpt = repo.findFirstByOrderIdOrderByCreatedAtDesc(order.getId());
        if (lastPaymentOpt.isPresent()) {
            Payment lastPayment = lastPaymentOpt.get();
            // Don hang da thanh toan thanh cong -> khong tao payment moi.
            if (lastPayment.getStatus() == PaymentStatus.SUCCESS) {
                throw new BusinessRuleException("Don hang da thanh toan thanh cong, khong the tao payment moi");
            }
            // Da co payment dang cho thanh toan (duoc tao san luc tao order) -> tra ve chinh no (idempotent).
            // Neu client chon provider khac thi cap nhat lai provider cho lan thanh toan dang cho.
            if (lastPayment.getStatus() == PaymentStatus.PENDING) {
                if (request.provider() != null && request.provider() != lastPayment.getProvider()) {
                    lastPayment.setProvider(request.provider());
                }
                if (lastPayment.getProvider() == PaymentProvider.VNPAY) {
                    if (lastPayment.getMerchantTxnRef() == null || lastPayment.getMerchantTxnRef().isBlank()) {
                        lastPayment.setMerchantTxnRef(generateMerchantTxnRef(order, lastPayment));
                    }
                    String paymentUrl = vnPayPaymentUrlService.createPaymentUrl(lastPayment, clientIp);
                    lastPayment.setPaymentUrl(paymentUrl);
                    lastPayment = repo.save(lastPayment);
                }
                return PaymentCallbackMapper.fromEntity(lastPayment);
            }
            // Con lai: lastPayment FAILED -> cho phep tao attempt moi ben duoi.
        }

        PaymentProvider provider = request.provider() != null ? request.provider() : PaymentProvider.VNPAY;

        Payment payment = Payment.builder()
                .order(order)
                .provider(provider)
                .amount(order.getFinalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        payment = repo.save(payment);

        if (provider == PaymentProvider.VNPAY) {
            payment.setMerchantTxnRef(generateMerchantTxnRef(order, payment));
            payment.setPaymentUrl(vnPayPaymentUrlService.createPaymentUrl(payment, clientIp));
            payment = repo.save(payment);
        }

        return PaymentCallbackMapper.fromEntity(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentCallbackResponse getMyPaymentByMerchantTxnRef(int userId, String merchantTxnRef) {
        Payment payment = repo.findByMerchantTxnRef(merchantTxnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay giao dich thanh toan"));

        if (payment.getOrder() == null || !payment.getOrder().getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Ban khong co quyen xem giao dich thanh toan nay");
        }

        return PaymentCallbackMapper.fromEntity(payment);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "payments", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true)
    })
    public PaymentCallbackResponse updatePayment(int id, PaymentCallbackRequest request) {
        Payment payment = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay payment voi id = " + id));

        Order order = orderRepository.findWithItemsByIdForUpdate(payment.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay order cua payment id = " + id));

        if (!order.getId().equals(request.orderId())) {
            throw new BusinessRuleException("OrderId khong khop voi payment dang cap nhat");
        }

        ensureOrderCanAcceptPayment(order);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return PaymentCallbackMapper.fromEntity(payment);
        }

        if (request.provider() != null) {
            payment.setProvider(request.provider());
        }
        if (request.transactionNo() != null) {
            String transactionNo = request.transactionNo().trim();
            if (!transactionNo.isBlank()) {
                payment.setTransactionNo(transactionNo);
            }
        }
        if (request.status() != null) {
            if (request.status() == PaymentStatus.SUCCESS
                    && (payment.getTransactionNo() == null || payment.getTransactionNo().isBlank())) {
                payment.setTransactionNo(generateTransactionNo(payment));
            }
            payment.setStatus(request.status());
            applyPaymentStatus(order, request.status());
        }
        return PaymentCallbackMapper.fromEntity(payment);
    }

    private String generateTransactionNo(Payment payment) {
        return "PAY-" + payment.getOrder().getId() + "-" + payment.getId() + "-" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().substring(0, 4);
    }

    // Kiem tra don con nhan thanh toan duoc khong. Qua han -> tu huy don (hoan kho/coupon) roi nem loi.
    private void ensureOrderCanAcceptPayment(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Don hang da bi huy, khong the cap nhat thanh toan");
        }
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Don hang da duoc van chuyen, khong the cap nhat thanh toan");
        }
        if (order.getPaymentExpiresAt() != null && !order.getPaymentExpiresAt().isAfter(Instant.now())) {
            orderLifecycleService.cancelOrder(order, "Payment window expired");
            throw new BusinessRuleException("Don hang da qua han thanh toan");
        }
    }

    private void applyPaymentStatus(Order order, PaymentStatus status) {
        if (status == PaymentStatus.SUCCESS) {
            orderLifecycleService.confirmAfterSuccessfulPayment(order);
        }
        if (status == PaymentStatus.FAILED) {
            orderLifecycleService.cancelOrder(order, "Payment failed");
        }
    }
    /**
     * No outer transaction: reserve a durable refund attempt FIRST; send money
     * to the gateway OUTSIDE a DB transaction; finalize in a new transaction.
     * Unknown gateway responses are left for manual reconciliation, never retried.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "payments", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "orderHistories", allEntries = true)
    })
    public OrderResponse processRefund(int orderId, BigDecimal refundAmount, String reason, int adminId) {
        RefundWorkflowService.PreparedRefund prepared =
                refundWorkflowService.prepare(orderId, refundAmount, reason, adminId);

        if (prepared.provider() == PaymentProvider.VNPAY) {
            boolean confirmed = callVNPayRefundApi(prepared);
            if (!confirmed) {
                refundWorkflowService.markForReview(prepared.orderId(),
                        "VNPay refund response missing, failed or unverified; reconcile before any retry");
                throw new BusinessRuleException(
                        "Chưa xác nhận được hoàn tiền VNPay; yêu cầu đã được giữ để đối soát, không tự gửi lại");
            }
        }
        // Non-VNPay methods represent an explicit admin acknowledgement of manual refund.
        return refundWorkflowService.complete(prepared.orderId());
    }

    private boolean callVNPayRefundApi(RefundWorkflowService.PreparedRefund refund) {
        try {
            String vnpRequestId = refund.requestId();
            String vnpVersion = "2.1.0";
            String vnpCommand = "refund";
            String vnpTransactionType = "02"; // full refund only
            String vnpTxnRef = refund.merchantTxnRef();
            String vnpAmount = refund.amount().multiply(new BigDecimal("100")).longValueExact() + "";
            String vnpOrderInfo = "Hoan tien don hang #" + refund.orderId();
            String vnpTransactionNo = refund.transactionNo();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            java.time.ZoneId zone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
            String vnpTransactionDate = refund.paymentCreatedAt().atZone(zone).format(formatter);
            String vnpCreateBy = refund.adminUsername();
            String vnpCreateDate = java.time.ZonedDateTime.now(zone).format(formatter);
            String vnpIpAddr = "127.0.0.1";
            String signedData = String.join("|", vnpRequestId, vnpVersion, vnpCommand,
                    vnPayProperties.getTmnCode(), vnpTransactionType, vnpTxnRef, vnpAmount,
                    vnpTransactionNo, vnpTransactionDate, vnpCreateBy, vnpCreateDate,
                    vnpIpAddr, vnpOrderInfo);
            String checksum = VNPayUtils.hmacSHA512(vnPayProperties.getHashSecret(), signedData);
            Map<String, String> body = new HashMap<>();
            body.put("vnp_RequestId", vnpRequestId);
            body.put("vnp_Version", vnpVersion);
            body.put("vnp_Command", vnpCommand);
            body.put("vnp_TmnCode", vnPayProperties.getTmnCode());
            body.put("vnp_TransactionType", vnpTransactionType);
            body.put("vnp_TxnRef", vnpTxnRef);
            body.put("vnp_Amount", vnpAmount);
            body.put("vnp_OrderInfo", vnpOrderInfo);
            body.put("vnp_TransactionNo", vnpTransactionNo);
            body.put("vnp_TransactionDate", vnpTransactionDate);
            body.put("vnp_CreateBy", vnpCreateBy);
            body.put("vnp_CreateDate", vnpCreateDate);
            body.put("vnp_IpAddr", vnpIpAddr);
            body.put("vnp_SecureHash", checksum);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<?, ?> response = restTemplate.postForObject(vnPayProperties.getRefundUrl(),
                    new HttpEntity<>(body, headers), Map.class);
            // VNPay refund WebAPI 2.1.0: response checksum signs these fields in fixed order.
            // Fail closed on missing signature, mismatched payment identity or ambiguous status.
            String[] responseFields = {
                    "vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message",
                    "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode",
                    "vnp_PayDate", "vnp_TransactionNo", "vnp_TransactionType",
                    "vnp_TransactionStatus", "vnp_OrderInfo"
            };
            if (response == null || !(response.get("vnp_SecureHash") instanceof String signature)) {
                return false;
            }
            StringBuilder signedResponse = new StringBuilder();
            for (String field : responseFields) {
                if (!signedResponse.isEmpty()) signedResponse.append('|');
                signedResponse.append(response.get(field) == null ? "" : response.get(field));
            }
            String expectedSignature = VNPayUtils.hmacSHA512(
                    vnPayProperties.getHashSecret(), signedResponse.toString());
            boolean signatureValid = java.security.MessageDigest.isEqual(
                    expectedSignature.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    signature.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            boolean confirmed = signatureValid
                    && "00".equals(response.get("vnp_ResponseCode"))
                    && "00".equals(response.get("vnp_TransactionStatus"))
                    && "refund".equals(response.get("vnp_Command"))
                    && vnPayProperties.getTmnCode().equals(response.get("vnp_TmnCode"))
                    && vnpTxnRef.equals(response.get("vnp_TxnRef"))
                    && vnpAmount.equals(String.valueOf(response.get("vnp_Amount")))
                    && vnpTransactionType.equals(response.get("vnp_TransactionType"))
                    && vnpTransactionNo.equals(String.valueOf(response.get("vnp_TransactionNo")));
            if (!confirmed) log.warn("[REFUND] Unverified VNPay response for request {}", vnpRequestId);
            return confirmed;
        } catch (Exception ex) {
            log.error("[REFUND] Gateway result unknown for order {}; manual reconciliation required", refund.orderId(), ex);
            return false;
        }
    }

    private String generateMerchantTxnRef(Order order, Payment payment) {
        return "LDX"
                + order.getId()
                + payment.getId()
                + System.currentTimeMillis();
    }
}
