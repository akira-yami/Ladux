package org.akira.ladux.catalog.service;

import java.math.BigDecimal;
import org.akira.ladux.catalog.entity.ProductVariant;

public interface PricingService {
    BigDecimal sellingPrice(ProductVariant productVariant);
}

