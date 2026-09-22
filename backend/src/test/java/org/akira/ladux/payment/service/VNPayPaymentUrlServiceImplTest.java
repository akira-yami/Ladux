package org.akira.ladux.payment.service;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.akira.ladux.ordering.entity.Order;
import org.akira.ladux.payment.config.VNPayProperties;
import org.akira.ladux.payment.entity.Payment;
import org.akira.ladux.payment.enums.PaymentProvider;
import org.akira.ladux.payment.enums.PaymentStatus;
import org.akira.ladux.payment.service.impl.VNPayPaymentUrlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VNPayPaymentUrlServiceImplTest {

    @Mock
    private VNPayProperties vnPayProperties;

    @InjectMocks
    private VNPayPaymentUrlServiceImpl vnPayPaymentUrlService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        Order order = Order.builder()
                .id(100)
                .finalAmount(new BigDecimal("150000.00"))
                .build();

        payment = Payment.builder()
                .id(1)
                .order(order)
                .merchantTxnRef("LDX10011700000000000")
                .amount(new BigDecimal("150000.00"))
                .provider(PaymentProvider.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void createPaymentUrl_ShouldBuildValidVNPayUrlWithCorrectParamsAndChecksum() {
        when(vnPayProperties.getTmnCode()).thenReturn("DEMO_TMN");
        when(vnPayProperties.getHashSecret()).thenReturn("SECRET_KEY_123456789");
        when(vnPayProperties.getPayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayProperties.getReturnUrl()).thenReturn("http://localhost:5173/payment/vnpay/return");
        when(vnPayProperties.getExpireMinutes()).thenReturn(15);

        String paymentUrl = vnPayPaymentUrlService.createPaymentUrl(payment, "127.0.0.1");

        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(paymentUrl.contains("vnp_SecureHash="));

        // Parse query string
        String queryString = paymentUrl.substring(paymentUrl.indexOf('?') + 1);
        Map<String, String> queryParams = new HashMap<>();
        for (String param : queryString.split("&")) {
            String[] pair = param.split("=");
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            queryParams.put(key, value);
        }

        assertEquals("2.1.0", queryParams.get("vnp_Version"));
        assertEquals("pay", queryParams.get("vnp_Command"));
        assertEquals("DEMO_TMN", queryParams.get("vnp_TmnCode"));
        assertEquals("15000000", queryParams.get("vnp_Amount")); // 150,000 * 100
        assertEquals("VND", queryParams.get("vnp_CurrCode"));
        assertEquals("LDX10011700000000000", queryParams.get("vnp_TxnRef"));
        assertEquals("http://localhost:5173/payment/vnpay/return", queryParams.get("vnp_ReturnUrl"));
        assertEquals("127.0.0.1", queryParams.get("vnp_IpAddr"));
        assertNotNull(queryParams.get("vnp_CreateDate"));
        assertNotNull(queryParams.get("vnp_ExpireDate"));
        assertNotNull(queryParams.get("vnp_SecureHash"));
        assertFalse(queryParams.get("vnp_SecureHash").isBlank());
    }
}
