package org.akira.ladux.ordering.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.akira.ladux.payment.enums.PaymentProvider;

public record OrderRequest(
        String couponCode,

        @NotNull(message = "PaymentProvider khong duoc de trong")
        PaymentProvider paymentProvider,

        @NotNull(message = "ShippingAddress khong duoc de trong")
        @Valid
        ShippingAddressRequest shippingAddress,

        String carrier,

        BigDecimal shippingFee
) {
    public OrderRequest(String couponCode, PaymentProvider paymentProvider, ShippingAddressRequest shippingAddress) {
        this(couponCode, paymentProvider, shippingAddress, null, null);
    }
}

