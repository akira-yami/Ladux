package org.akira.ladux.payment.service.impl;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.service.VNPayPaymentUrlService;
import org.akira.ladux.payment.util.VNPayUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VNPayPaymentUrlServiceImpl implements VNPayPaymentUrlService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final String DEFAULT_TMN_CODE = "7WS5ZFBQ";
    private static final String DEFAULT_HASH_SECRET = "OQBTTWFTSNPSBAFCEVXGWYNNEKYRIZLE";
    private static final String DEFAULT_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String DEFAULT_RETURN_URL = "http://localhost:5173/payment/vnpay/return";

    private final VNPayProperties vnPayProperties;

    @Override
    public String createPaymentUrl(Payment payment, String clientIp) {
        String tmnCode = (vnPayProperties != null && vnPayProperties.getTmnCode() != null && !vnPayProperties.getTmnCode().isBlank())
                ? vnPayProperties.getTmnCode()
                : DEFAULT_TMN_CODE;

        String hashSecret = (vnPayProperties != null && vnPayProperties.getHashSecret() != null && !vnPayProperties.getHashSecret().isBlank())
                ? vnPayProperties.getHashSecret()
                : DEFAULT_HASH_SECRET;

        String payUrl = (vnPayProperties != null && vnPayProperties.getPayUrl() != null && !vnPayProperties.getPayUrl().isBlank())
                ? vnPayProperties.getPayUrl()
                : DEFAULT_PAY_URL;

        String returnUrl = (vnPayProperties != null && vnPayProperties.getReturnUrl() != null && !vnPayProperties.getReturnUrl().isBlank())
                ? vnPayProperties.getReturnUrl()
                : DEFAULT_RETURN_URL;

        int expireMinutes = (vnPayProperties != null && vnPayProperties.getExpireMinutes() > 0)
                ? vnPayProperties.getExpireMinutes()
                : 15;

        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);

        long vnpAmount = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        String ip = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getMerchantTxnRef());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getOrder().getId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", ip);
        params.put("vnp_CreateDate", now.format(VNP_DATE_FORMAT));
        params.put("vnp_ExpireDate", now.plusMinutes(expireMinutes).format(VNP_DATE_FORMAT));

        String signedQuery = buildEncodedQuery(params);
        String secureHash = VNPayUtils.hmacSHA512(hashSecret, signedQuery);

        return payUrl
                + "?"
                + signedQuery
                + "&vnp_SecureHash="
                + secureHash;
    }

    private String buildEncodedQuery(Map<String, String> params) {
        return params.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> VNPayUtils.encode(entry.getKey()) + "=" + VNPayUtils.encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }
}