package org.akira.ladux.notification.mapper;

import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.notification.dto.response.NotificationResponse;
import org.akira.ladux.notification.entity.Notification;

public final class NotificationMapper {
    private NotificationMapper() {
    }

    public static NotificationResponse fromEntity(Notification notification) {
        User recipient = notification.getRecipient();
        Customer customer = recipient != null ? recipient.getCustomer() : null;
        String name = customer != null && customer.getFullName() != null && !customer.getFullName().isBlank()
                ? customer.getFullName()
                : (recipient != null ? recipient.getUsername() : null);

        return new NotificationResponse(
                notification.getId(),
                recipient != null ? recipient.getId() : null,
                name,
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getType(),
                notification.getCreatedAt()
        );
    }
}
