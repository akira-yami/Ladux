package org.akira.ladux.notification.service.impl;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.security.SecurityUtils;
import org.akira.ladux.notification.dto.request.NotificationRequest;
import org.akira.ladux.notification.dto.response.NotificationResponse;
import org.akira.ladux.notification.entity.Notification;
import org.akira.ladux.notification.mapper.NotificationMapper;
import org.akira.ladux.notification.repository.NotificationRepository;
import org.akira.ladux.notification.service.NotificationService;
import org.akira.ladux.shared.dto.PageResponse;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAllNotifications(Pageable pageable) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return PageResponse.from(notificationRepository.findByRecipientIdAndIsDeletedByUserFalseOrderByCreatedAtDesc(currentUserId, pageable).map(NotificationMapper::fromEntity));
    }
    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAllUnReadNotifications(Pageable pageable) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return PageResponse.from(notificationRepository.findByRecipientIdAndIsReadFalseAndIsDeletedByUserFalseOrderByCreatedAtDesc(currentUserId, pageable).map(NotificationMapper::fromEntity));
    }
    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAllReadNotifications(Pageable pageable) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return PageResponse.from(notificationRepository.findByRecipientIdAndIsReadTrueAndIsDeletedByUserFalseOrderByCreatedAtDesc(currentUserId, pageable).map(NotificationMapper::fromEntity));
    }
    @Override
    @Transactional(readOnly = true)
    public int getUnreadNotificationCount() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return notificationRepository.countByRecipientIdAndIsReadFalseAndIsDeletedByUserFalse(currentUserId);
    }
    @Override
    @Transactional
    public void markAsRead(Integer notificationId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findByIdAndRecipientIdAndIsReadFalseAndIsDeletedByUserFalse(notificationId, currentUserId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo chưa đọc với id = " + notificationId));
        notification.setRead(true);
    }

    @Override
    @Transactional
    public void deleteNotification(Integer notificationId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findByIdAndRecipientIdAndIsDeletedByUserFalse(notificationId, currentUserId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo với id = " + notificationId));
        notification.setDeletedByUser(true);
    }
    @Override
    @Transactional
    public void deleteAllNotifications() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        notificationRepository.softDeleteAllByUserId(currentUserId);
    }

    // == Admin ==
    // Admin gửi thông báo đến tất cả người dùng
    @Override
    @Transactional
    public String broadcastNotification(NotificationRequest request) {
        int count = notificationRepository.insertBroadcastNotifications(
                request.title(),
                request.message(),
                request.type().name()
        );
        return "Đã gửi thông báo thành công tới " + count + " người dùng.";
    }
    // Admin gửi thông báo đến 1 người dùng cụ thể
    @Override
    @Transactional
    public String sendNotificationToUser(NotificationRequest request, Integer userId) {
        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id = " + userId));
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(request.title())
                .message(request.message())
                .isRead(false)
                .isDeletedByUser(false)
                .type(request.type())
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notification);
        return "Thông báo đã được gửi đến người dùng.";
    }
    // Admin xem tất cả thông báo gửi đến admin (yêu cầu trả hàng, hệ thống...)
    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAllNotificationsForAdmin(Pageable pageable) {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            return PageResponse.from(Page.empty(pageable));
        }
        return PageResponse.from(notificationRepository.findByRecipientIdAndIsDeletedByUserFalseOrderByCreatedAtDesc(admin.getId(), pageable)
                .map(NotificationMapper::fromEntity));
    }
    // Xoa thong bao cho admin (Hard delete từ DB)
    @Override
    @Transactional
    public void deleteNotificationForAdmin(Integer id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo với id = " + id));
        notificationRepository.delete(notification);
    }
    // Xoa tat ca thong bao cho admin (chỉ soft-delete thông báo gửi đến admin)
    @Override
    @Transactional
    public void deleteAllNotificationsForAdmin() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin != null) {
            notificationRepository.softDeleteAllByUserId(admin.getId());
        }
    }

    @Override
    @Transactional
    public void markAsReadForAdmin(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo với id = " + notificationId));
        notification.setRead(true);
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadNotificationCountForAdmin() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            return 0;
        }
        return notificationRepository.countByRecipientIdAndIsReadFalseAndIsDeletedByUserFalse(admin.getId());
    }
}
