package org.akira.ladux.notification.controller.admin;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.notification.dto.request.NotificationRequest;
import org.akira.ladux.notification.dto.response.NotificationResponse;
import org.akira.ladux.notification.service.NotificationService;
import org.akira.ladux.shared.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {
    private final NotificationService notificationService;
    // Admin gửi thong báo để tất cả mọi người
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> broadcastNotification(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.broadcastNotification(request));
    }
    // Admin gửi thong báo để 1 người dùng cụ thể
    @PostMapping("/user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> sendNotificationToUser(@RequestBody NotificationRequest request, @PathVariable Integer id) {
        notificationService.sendNotificationToUser(request, id);
        return ResponseEntity.ok("Thông báo đã được gửi đến người dùng.");
    }
    // Admin xem lai toan bo thong báo
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<NotificationResponse>> getAllNotifications(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getAllNotificationsForAdmin(pageable));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteNotification(@PathVariable Integer id) {
        notificationService.deleteNotificationForAdmin(id);
        return ResponseEntity.ok("Thông báo đã được xóa!");
    }
    @DeleteMapping("/delete-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAllNotifications() {
        notificationService.deleteAllNotificationsForAdmin();
        return ResponseEntity.ok("Tất cả thông báo đã được xóa!");
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id) {
        notificationService.markAsReadForAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> getUnreadNotificationCount() {
        return ResponseEntity.ok(notificationService.getUnreadNotificationCountForAdmin());
    }
}
