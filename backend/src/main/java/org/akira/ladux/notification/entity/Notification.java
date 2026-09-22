package org.akira.ladux.notification.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.notification.enums.NotificationType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Người nhận thông báo (Khách hàng hoặc Admin/Staff)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User recipient;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // Trạng thái đã đọc chưa (để hiện chấm đỏ notification trên UI)
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // Trạng thái ẩn/xóa phía người dùng (soft delete cho user)
    @Builder.Default
    @Column(name = "is_deleted_by_user", nullable = false)
    private boolean isDeletedByUser = false;

    // Phân loại thông báo (ORDER, SYSTEM, PROMOTION, INVENTORY...)
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private NotificationType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;
}