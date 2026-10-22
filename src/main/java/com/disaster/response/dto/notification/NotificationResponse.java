package com.disaster.response.dto.notification;

import com.disaster.response.domain.entity.Notification;
import com.disaster.response.domain.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    NotificationType type,
    String title,
    String message,
    String relatedEntityType,
    String relatedEntityId,
    Instant createdAt,
    Instant readAt,
    String deliveryStatus
) {
    public static NotificationResponse from(Notification n) {
        if (n == null) {
            return null;
        }
        return new NotificationResponse(
            n.getId(),
            n.getType(),
            n.getTitle(),
            n.getMessage(),
            n.getRelatedEntityType(),
            n.getRelatedEntityId(),
            n.getCreatedAt(),
            n.getReadAt(),
            n.getDeliveryStatus()
        );
    }
}
