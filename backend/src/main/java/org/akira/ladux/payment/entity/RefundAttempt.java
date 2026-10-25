package org.akira.ladux.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

/** A permanent one-per-payment guard; ambiguous results require reconciliation. */
@Entity
@Table(name = "refund_attempts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RefundAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Integer orderId;
    @Column(name = "payment_id", nullable = false, unique = true)
    private Integer paymentId;
    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;
    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(name = "requested_by_user_id", nullable = false)
    private Integer requestedByUserId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "resolution_reference", length = 1000)
    private String resolutionReference;
    @Column(name = "resolved_by_user_id")
    private Integer resolvedByUserId;

    public enum Status { PROCESSING, SUCCEEDED, REVIEW_REQUIRED }
}
