package org.akira.ladux.inventory.service.result;

import java.math.BigDecimal;
import org.akira.ladux.catalog.entity.ProductVariant;

// Dong dat hang tam sau khi tru kho va chot gia — dung noi bo trong createOrder.
// product: da tru stock; priceAtPurchase: gia snapshot; lineTotal = priceAtPurchase * quantity.
public record LineDraft(
        ProductVariant productVariant,
        Integer quantity,
        BigDecimal priceAtPurchase,
        BigDecimal lineTotal
) {}