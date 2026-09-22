package org.akira.ladux.notification.repository;

import java.util.Optional;
import org.akira.ladux.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    Page<Notification> findByRecipientIdAndIsDeletedByUserFalseOrderByCreatedAtDesc(Integer currentUserId, Pageable pageable);
    Page<Notification> findByRecipientIdAndIsReadFalseAndIsDeletedByUserFalseOrderByCreatedAtDesc(Integer currentUserId, Pageable pageable);
    Page<Notification> findByRecipientIdAndIsReadTrueAndIsDeletedByUserFalseOrderByCreatedAtDesc(Integer currentUserId, Pageable pageable);

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    int countByRecipientIdAndIsReadFalseAndIsDeletedByUserFalse(Integer currentUserId);

    int countByIsReadFalse();

    Optional<Notification> findByIdAndRecipientIdAndIsReadFalseAndIsDeletedByUserFalse(Integer notificationId, Integer currentUserId);

    Optional<Notification> findByIdAndRecipientIdAndIsDeletedByUserFalse(Integer notificationId, Integer currentUserId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :currentUserId AND n.isRead = false AND n.isDeletedByUser = false")
    void markAllAsReadByUserId(Integer currentUserId);

    @Modifying
    @Query("UPDATE Notification n SET n.isDeletedByUser = true WHERE n.recipient.id = :currentUserId AND n.isDeletedByUser = false")
    void softDeleteAllByUserId(Integer currentUserId);

    @Modifying
    @Query(value = """
    INSERT INTO notifications (
        user_id,
        title,
        message,
        is_read,
        is_deleted_by_user,
        type,
        created_at
    )
    SELECT
        u.id,
        :title,
        :message,
        false,
        false,
        :type,
        CURRENT_TIMESTAMP
    FROM users u
    WHERE u.username != 'admin'
    """, nativeQuery = true)
    int insertBroadcastNotifications(
            @Param("title") String title,
            @Param("message") String message,
            @Param("type") String type
    );
}
