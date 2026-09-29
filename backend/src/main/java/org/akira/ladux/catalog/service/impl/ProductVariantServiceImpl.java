package org.akira.ladux.catalog.service.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.catalog.dto.response.ProductVariantResponse;
import org.akira.ladux.catalog.entity.Color;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.mapper.ProductVariantMapper;
import org.akira.ladux.catalog.repository.ColorRepository;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.catalog.repository.ProductVariantRepository;
import org.akira.ladux.catalog.service.ProductVariantService;
import org.akira.ladux.catalog.util.SkuUtils;
import org.akira.ladux.ordering.repository.CartItemRepository;
import org.akira.ladux.ordering.repository.OrderItemRepository;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.akira.ladux.catalog.util.SkuUtils.generateSku;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ColorRepository colorRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getProductVariantById(Integer variantId) {
        if (variantId == null || variantId < 1) {
            throw new IllegalArgumentException("variantId phải là số nguyên dương");
        }
        return productVariantRepository.findById(variantId)
                .map(ProductVariantMapper::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy ProductVariant với id: " + variantId));
    }

    // Dành cho Admin
    @Override
    @Transactional
    public ProductVariantResponse addProductVariant(Integer productId, Integer colorId, String ram, String rom, BigDecimal price, BigDecimal discountPrice, int stockQuantity, boolean active) {
        if (productId == null || colorId == null || ram == null || rom == null || price == null || stockQuantity < 0) {
            throw new IllegalArgumentException("Không được để trống bất kì trường nào và StockQuantity không được âm");
        }
        if (ram.isBlank() || rom.isBlank()) {
            throw new IllegalArgumentException("RAM và ROM không được để trống");
        }
        if (colorId == null) {
            throw new IllegalArgumentException("Màu sắc không hợp lệ");
        }
        if (discountPrice != null && discountPrice.compareTo(price) > 0) {
            throw new IllegalArgumentException("Giá khuyến mãi không được lớn hơn giá gốc");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + productId));
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu sắc với id: " + colorId));
        ProductVariant productVariant = ProductVariant.builder()
                .product(product)
                .sku(generateSku(product.getName(), ram, rom, color.getName()))
                .color(color)
                .ram(ram)
                .rom(rom)
                .price(price)
                .discountPrice(discountPrice)
                .stockQuantity(stockQuantity)
                .isActive(active)
                .build();
        return ProductVariantMapper.fromEntity(productVariantRepository.save(productVariant));
    }
    @Transactional
    public ProductVariantResponse updateProductVariant(Integer id, Integer colorId, String ram, String rom, BigDecimal price, BigDecimal discountPrice, int stockQuantity, boolean active) {
        if (id == null || colorId == null || ram == null || rom == null || price == null || stockQuantity < 0) {
            throw new IllegalArgumentException("Không được để trống bất kì trường nào và StockQuantity không được âm");
        }
        if (ram.isBlank() || rom.isBlank()) {
            throw new IllegalArgumentException("RAM và ROM không được để trống");
        }
        ProductVariant existingVariant = productVariantRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ProductVariant với id: " + id));
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu sắc với id: " + colorId));
        if (discountPrice != null && discountPrice.compareTo(price) > 0) {
            throw new IllegalArgumentException("Giá khuyến mãi không được lớn hơn giá gốc");
        }

        existingVariant.setColor(color);
        existingVariant.setRam(ram);
        existingVariant.setRom(rom);
        existingVariant.setPrice(price);
        existingVariant.setDiscountPrice(discountPrice);
        existingVariant.setStockQuantity(stockQuantity);
        existingVariant.setActive(active);
        existingVariant.setSku(SkuUtils.generateSku(existingVariant.getProduct().getName(), ram, rom, color.getName()));
        productVariantRepository.save(existingVariant);
        return ProductVariantMapper.fromEntity(existingVariant);
    }
    @Override
    @Transactional
    public void deleteProductVariant(Integer variantId) {
        if (variantId == null) {
            throw new IllegalArgumentException("variantId không được để trống");
        }
        ProductVariant productVariantExisting = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ProductVariant với id: " + variantId));
        // KIểm tra xem ProductVariant có tồn tại trong OrderItem hay không, nếu có thì không được xóa
        if (orderItemRepository.existsByProductVariantId(variantId)) {
            productVariantExisting.setActive(false);
            productVariantExisting.setStockQuantity(0);
            productVariantRepository.save(productVariantExisting);
            return;
        }
        // Xóa ProductVariant ra tất cả đơn hàng
        cartItemRepository.deleteByProductVariantId(variantId);

        productVariantRepository.delete(productVariantExisting);
    }
}
