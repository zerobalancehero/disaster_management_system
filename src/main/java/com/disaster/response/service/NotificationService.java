package com.disaster.response.service;

import com.disaster.response.domain.entity.Notification;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.NotificationType;
import com.disaster.response.dto.notification.NotificationResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification sendInAppNotification(User recipient, NotificationType type, String title,
                                              String message, String relatedType, String relatedId) {
        Notification notification = new Notification(recipient, type, title, message, relatedType, relatedId);
        Notification saved = notificationRepository.save(notification);
        log.info("In-app notification sent to user {}: '{}'", recipient.getEmail(), title);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        notification.markAsRead();
        return NotificationResponse.from(notification);
    }
}
