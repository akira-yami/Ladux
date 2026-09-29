package org.akira.ladux.notification.controller;

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
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/v1/notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getAllNotifications(pageable));
    }

    @GetMapping("/api/v1/notifications/unread")
    public ResponseEntity<PageResponse<NotificationResponse>> getAllUnReadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getAllUnReadNotifications(pageable));
    }

    @GetMapping("/api/v1/notifications/read")
    public ResponseEntity<PageResponse<NotificationResponse>> getAllReadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getAllReadNotifications(pageable));
    }

    @GetMapping("/api/v1/notifications/unread-count")
    public ResponseEntity<Integer> getUnreadNotificationCount() {
        return ResponseEntity.ok(notificationService.getUnreadNotificationCount());
    }

    @PatchMapping("/api/v1/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/notifications/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Integer id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/notifications")
    public ResponseEntity<Void> deleteAllNotifications() {
        notificationService.deleteAllNotifications();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> broadcastNotification(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.broadcastNotification(request));
    }

    @PostMapping("/api/v1/admin/notifications/user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> sendNotificationToUser(
            @RequestBody NotificationRequest request, @PathVariable Integer id) {
        notificationService.sendNotificationToUser(request, id);
        return ResponseEntity.ok("Thông báo đã được gửi đến người dùng.");
    }

    @GetMapping("/api/v1/admin/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<NotificationResponse>> getAllNotificationsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getAllNotificationsForAdmin(pageable));
    }

    @DeleteMapping("/api/v1/admin/notifications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteNotificationForAdmin(@PathVariable Integer id) {
        notificationService.deleteNotificationForAdmin(id);
        return ResponseEntity.ok("Thông báo đã được xóa!");
    }

    @DeleteMapping("/api/v1/admin/notifications/delete-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAllNotificationsForAdmin() {
        notificationService.deleteAllNotificationsForAdmin();
        return ResponseEntity.ok("Tất cả thông báo đã được xóa!");
    }

    @PatchMapping("/api/v1/admin/notifications/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAsReadForAdmin(@PathVariable Integer id) {
        notificationService.markAsReadForAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/notifications/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> getUnreadNotificationCountForAdmin() {
        return ResponseEntity.ok(notificationService.getUnreadNotificationCountForAdmin());
    }
}
