package org.akira.ladux.notification.dto.response;

import java.io.Serializable;
import java.time.Instant;
import org.akira.ladux.notification.enums.NotificationType;

public record NotificationResponse(
        Integer id,
        Integer userId,
        String userName,
        String title,
        String message,
        boolean isRead,
        NotificationType type,
        Instant createdAt
) implements Serializable {
}
