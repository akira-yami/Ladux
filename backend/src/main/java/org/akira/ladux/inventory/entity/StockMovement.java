package org.akira.ladux.inventory.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.inventory.enums.StockMovementType;
import org.akira.ladux.inventory.enums.StockReferenceType;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StockMovement { // Sổ nhật ký thu chi cho kho hàng

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    @ToString.Exclude
    private ProductVariant productVariant;

    /**
     * Số lượng thay đổi:
     * - Số dương (+): Nhập kho (Nhập, trả hàng)
     * - Số âm (-): Xuất kho (bán hàng, hư hỏng...)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 80, nullable = false)
    private StockMovementType movementType; // Xác định lý do tăng giảm tồn kho

    /**
     * Tham chiếu đến nguồn gốc thay đổi (ví dụ: order_id, purchase_order_id...)
     */

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 80)
    private StockReferenceType referenceType;   // ORDER, PURCHASE_ORDER, ADJUSTMENT... cái này nó chỉ rõ cụ thể hơn

    @Column(name = "reference_id")
    private Integer referenceId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User createdBy;           // Staff, Admin thực hiện thay đổi

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;
}