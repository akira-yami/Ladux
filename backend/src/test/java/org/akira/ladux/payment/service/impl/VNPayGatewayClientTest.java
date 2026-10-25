package org.akira.ladux.payment.service.impl;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.util.VNPayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VNPayGatewayClientTest {
    private VNPayGatewayClient client;
    private MockRestServiceServer server;
    private final ObjectMapper json = new ObjectMapper();
    private static final String SECRET = "query-test-secret";
    @BeforeEach void setup() {
        RestTemplate http = new RestTemplate();
        server = MockRestServiceServer.bindTo(http).build();
        VNPayProperties properties = new VNPayProperties();
        properties.setTmnCode("TESTTMN1"); properties.setHashSecret(SECRET);
        client = new VNPayGatewayClient(properties, http);
    }
    private Map<String, String> response() {
        Map<String, String> fields = new LinkedHashMap<>();
        String[] names = {"vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message", "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate", "vnp_TransactionNo", "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo", "vnp_PromotionCode", "vnp_PromotionAmount"};
        String[] values = {"R1", "querydr", "00", "Success", "TESTTMN1", "LDX1", "20000000", "NCB", "20261021180000", "14000111", "01", "00", "Thanh toan don hang 1", "", ""};
        for (int i = 0; i < names.length; i++) fields.put(names[i], values[i]);
        fields.put("vnp_SecureHash", VNPayUtils.hmacSHA512(SECRET, String.join("|", fields.values())));
        return fields;
    }
    private void expect(Map<String, String> response) throws Exception {
        server.expect(requestTo("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction"))
                .andExpect(request -> {
                    var requestBody = ((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsString();
                    var fields = json.readTree(requestBody);
                    assertEquals("20261021175500", fields.path("vnp_TransactionDate").asText());
                    String[] names = {"vnp_RequestId", "vnp_Version", "vnp_Command", "vnp_TmnCode", "vnp_TxnRef", "vnp_TransactionDate", "vnp_CreateDate", "vnp_IpAddr", "vnp_OrderInfo"};
                    String data = Arrays.stream(names).map(name -> fields.path(name).asText()).collect(java.util.stream.Collectors.joining("|"));
                    assertEquals(VNPayUtils.hmacSHA512(SECRET, data), fields.path("vnp_SecureHash").asText());
                    assertEquals(32, fields.path("vnp_RequestId").asText().length());
                }).andRespond(withSuccess(json.writeValueAsString(response), MediaType.APPLICATION_JSON));
    }
    @Test void signsQueryWithOriginalGatewayDateAndVerifiesPromotionFields() throws Exception {
        expect(response());
        assertEquals("00", client.query("LDX1", "20261021175500", "127.0.0.1").get("vnp_TransactionStatus"));
        server.verify();
    }
    @Test void ignoresUnsignedOrTamperedGatewayResults() throws Exception {
        var result = response(); result.put("vnp_Amount", "1"); expect(result);
        assertNull(client.query("LDX1", "20261021175500", "127.0.0.1"));
    }
    @Test void signedResultForWrongReferenceCannotSettleAnotherPayment() throws Exception {
        var result = response(); result.remove("vnp_SecureHash"); result.put("vnp_TxnRef", "OTHER");
        result.put("vnp_SecureHash", VNPayUtils.hmacSHA512(SECRET, String.join("|", result.values()))); expect(result);
        assertNull(client.query("LDX1", "20261021175500", "127.0.0.1"));
    }
    @Test void successfulQueryWithPendingTransactionRemainsPending() throws Exception {
        var result = response(); result.remove("vnp_SecureHash"); result.put("vnp_TransactionStatus", "01");
        result.put("vnp_SecureHash", VNPayUtils.hmacSHA512(SECRET, String.join("|", result.values()))); expect(result);
        assertEquals("01", client.query("LDX1", "20261021175500", "127.0.0.1").get("vnp_TransactionStatus"));
    }
}
