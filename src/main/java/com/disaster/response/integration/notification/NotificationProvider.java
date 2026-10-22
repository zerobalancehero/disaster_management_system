package com.disaster.response.integration.notification;

import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.NotificationType;

public interface NotificationProvider {
    void dispatch(User recipient, NotificationType type, String title, String message);
    boolean isExternalDeliveryConfigured();
}
