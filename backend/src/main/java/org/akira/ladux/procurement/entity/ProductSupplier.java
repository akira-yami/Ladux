package org.akira.ladux.procurement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import org.akira.ladux.catalog.entity.Product;

@Entity
@Table(
        name = "product_suppliers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_supplier", columnNames = {"product_id", "supplier_id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSupplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @ToString.Exclude
    private Supplier supplier;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;           // Giá nhập từ nhà cung cấp

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;           // Thời gian giao hàng (ngày)

}