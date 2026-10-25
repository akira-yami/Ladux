package org.akira.ladux.payment.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.service.VNPayPaymentUrlService;
import org.akira.ladux.payment.util.VNPayUtils;
import org.akira.ladux.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VNPayPaymentUrlServiceImpl implements VNPayPaymentUrlService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final VNPayProperties vnPayProperties;

    @Override
    public String createPaymentUrl(Payment payment, String clientIp) {
        if (vnPayProperties.getTmnCode() == null || vnPayProperties.getTmnCode().isBlank()
                || vnPayProperties.getHashSecret() == null || vnPayProperties.getHashSecret().isBlank())
            throw new BusinessRuleException("Chưa cấu hình mã merchant/khóa VNPay");
        if (vnPayProperties.getPayUrl() == null || vnPayProperties.getReturnUrl() == null)
            throw new BusinessRuleException("Chưa cấu hình URL VNPay");
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0
                || payment.getAmount().stripTrailingZeros().scale() > 0)
            throw new BusinessRuleException("Số tiền VNPay phải là số nguyên VND lớn hơn 0");
        if (payment.getMerchantTxnRef() == null || !payment.getMerchantTxnRef().matches("[A-Za-z0-9]{1,100}"))
            throw new BusinessRuleException("Mã tham chiếu thanh toán VNPay không hợp lệ");
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(Math.max(1, vnPayProperties.getExpireMinutes()) * 60L);
        Instant orderExpiry = payment.getOrder().getPaymentExpiresAt();
        if (orderExpiry != null && orderExpiry.isBefore(expires)) expires = orderExpiry;
        if (!expires.isAfter(now)) throw new BusinessRuleException("Đơn hàng đã hết hạn thanh toán");
        String amount = payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString();
        if (amount.length() > 12) throw new BusinessRuleException("Số tiền vượt giới hạn VNPay");
        String createDate = DATE.format(now);
        payment.setVnpCreateDate(createDate);
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        params.put("vnp_Amount", amount);
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getMerchantTxnRef());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getOrder().getId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", DATE.format(expires));
        String query = VNPayUtils.canonicalQuery(params);
        return vnPayProperties.getPayUrl() + "?" + query + "&vnp_SecureHash="
                + VNPayUtils.hmacSHA512(vnPayProperties.getHashSecret(), query);
    }
}
