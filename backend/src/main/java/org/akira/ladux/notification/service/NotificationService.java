package org.akira.ladux.notification.service;

import org.akira.ladux.notification.dto.request.NotificationRequest;
import org.akira.ladux.notification.dto.response.NotificationResponse;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    PageResponse<NotificationResponse> getAllNotifications(Pageable pageable);

    PageResponse<NotificationResponse> getAllUnReadNotifications(Pageable pageable);

    PageResponse<NotificationResponse> getAllReadNotifications(Pageable pageable);

    int getUnreadNotificationCount();

    void markAsRead(Integer id);

    void deleteNotification(Integer id);

    void deleteAllNotifications();

    // == Admin ==
    String broadcastNotification(NotificationRequest request);

    String sendNotificationToUser(NotificationRequest request, Integer id);

    PageResponse<NotificationResponse> getAllNotificationsForAdmin(Pageable pageable);

    void deleteNotificationForAdmin(Integer id);

    void deleteAllNotificationsForAdmin();

    void markAsReadForAdmin(Integer id);

    int getUnreadNotificationCountForAdmin();
}
