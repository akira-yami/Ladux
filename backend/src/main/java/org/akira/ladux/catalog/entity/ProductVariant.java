package org.akira.ladux.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    @Column(nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @Column(nullable = false, unique = true, length = 50)
    private String sku; // Mã SKU riêng cho phiên bản (VD: IP15PM-256GB-BLK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id", nullable = false)
    @ToString.Exclude
    private Color color; // // Mối quan hệ tới thực thể Color (lưu mã Hex, tên màu)

    @Column(name = "ram", length = 50)
    private String ram;

    @Column(name = "rom", length = 50)
    private String rom; // VD: 256GB

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price; // Giá gốc biến thể

    @Column(precision = 15, scale = 2)
    private BigDecimal discountPrice; // Giá khuyến mãi (nếu có)

    @Column(nullable = false)
    @Builder.Default
    private int stockQuantity = 0; // Tồn kho riêng của bản này

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

}
