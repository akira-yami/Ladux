package org.akira.ladux.notification.dto.request;

import java.io.Serializable;
import org.akira.ladux.notification.enums.NotificationType;

public record NotificationRequest(
        String title,
        String message,
        NotificationType type
) implements Serializable {
}
