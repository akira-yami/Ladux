package org.akira.ladux.payment.service.impl;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.util.VNPayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VNPayGatewayClient {
    private final VNPayProperties properties;
    private final RestTemplate http;

    @Autowired
    public VNPayGatewayClient(VNPayProperties properties) {
        this(properties, createHttpClient());
    }

    VNPayGatewayClient(VNPayProperties properties, RestTemplate http) {
        this.properties = properties;
        this.http = http;
    }

    private static RestTemplate createHttpClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    public Map<String, String> query(String ref, String transactionDate, String serverIp) {
        if (properties.getHashSecret() == null || properties.getHashSecret().isBlank()) return null;
        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", UUID.randomUUID().toString().replace("-", ""));
        body.put("vnp_Version", "2.1.0");
        body.put("vnp_Command", "querydr");
        body.put("vnp_TmnCode", properties.getTmnCode());
        body.put("vnp_TxnRef", ref);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        body.put("vnp_IpAddr", serverIp);
        body.put("vnp_OrderInfo", "Truy van thanh toan " + ref);
        body.put("vnp_SecureHash", VNPayUtils.hmacSHA512(properties.getHashSecret(), String.join("|", body.values())));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<?, ?> response = http.postForObject(properties.getQueryUrl(), new HttpEntity<>(body, headers), Map.class);
        if (response == null) return null;
        Map<String, String> fields = new LinkedHashMap<>();
        response.forEach((key, value) -> fields.put(String.valueOf(key), value == null ? "" : String.valueOf(value)));
        String[] signedFields = { "vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message",
                "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate", "vnp_TransactionNo",
                "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo", "vnp_PromotionCode", "vnp_PromotionAmount" };
        String data = java.util.Arrays.stream(signedFields).map(key -> fields.getOrDefault(key, ""))
                .collect(java.util.stream.Collectors.joining("|"));
        if (!VNPayUtils.signaturesMatch(VNPayUtils.hmacSHA512(properties.getHashSecret(), data), fields.get("vnp_SecureHash"))
                || !"00".equals(fields.get("vnp_ResponseCode")) || !"querydr".equals(fields.get("vnp_Command"))
                || !properties.getTmnCode().equals(fields.get("vnp_TmnCode"))
                || !ref.equals(fields.get("vnp_TxnRef")) || !"01".equals(fields.get("vnp_TransactionType"))) return null;
        return fields;
    }
}
