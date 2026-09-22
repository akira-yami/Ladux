package org.akira.ladux.notification.controller.user;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.notification.dto.response.NotificationResponse;
import org.akira.ladux.notification.service.NotificationService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    // Lấy tất cả thông báo
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getAllNotifications(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<NotificationResponse> response = notificationService.getAllNotifications(pageable);
        return ResponseEntity.ok(response);
    }

    // Lấy tất cả thông báo chưa đọc
    @GetMapping("/unread")
    public ResponseEntity<PageResponse<NotificationResponse>> getAllUnReadNotifications(@RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<NotificationResponse> response = notificationService.getAllUnReadNotifications(pageable);
        return ResponseEntity.ok(response);
    }

    // Lấy tất cả thông báo đã đọc
    @GetMapping("/read")
    public ResponseEntity<PageResponse<NotificationResponse>> getAllReadNotifications(@RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<NotificationResponse> response = notificationService.getAllReadNotifications(pageable);
        return ResponseEntity.ok(response);
    }

    // Lấy số lượng thông báo chưa đọc
    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadNotificationCount() {
        int unreadCount = notificationService.getUnreadNotificationCount();
        return ResponseEntity.ok(unreadCount);
    }

    // Đánh dấu 1 thông báo là đã đọc
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    // Xóa 1 thông báo cụ thể
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Integer id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    // Xóa tất cả thông báo
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications() {
        notificationService.deleteAllNotifications();
        return ResponseEntity.noContent().build();
    }
}
