package org.akira.ladux.ordering.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    @ToString.Exclude
    private ProductVariant productVariant;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAtPurchase;
}
