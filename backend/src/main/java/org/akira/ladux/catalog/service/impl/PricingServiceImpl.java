package org.akira.ladux.catalog.service.impl;

import java.math.BigDecimal;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.service.PricingService;
import org.springframework.stereotype.Service;

// Tinh gia ban tai thoi diem hien tai. Uu tien discountPrice, khong co thi dung Price
// Gia chot vao OrderItem.priceAtPurchase — thay doi gia sau khong anh huong don cu.
// DiscountPrice la gia sau khuyen mai, Price la gia goc. Neu discountPrice = null thi dung Price.
@Service
public class PricingServiceImpl implements PricingService {
    @Override
    public BigDecimal sellingPrice(ProductVariant productVariant) {
        if (productVariant.getDiscountPrice() != null) {
            return productVariant.getDiscountPrice();
        }
        else return productVariant.getPrice();
    }
}

