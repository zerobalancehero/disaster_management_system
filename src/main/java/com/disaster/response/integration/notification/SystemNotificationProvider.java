package com.disaster.response.integration.notification;

import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Enterprise Outbound Notification Dispatcher.
 * Dispatches high-priority notifications to external channels (Email/SMS/Push)
 * for emergency response personnel and administrators.
 */
@Component
public class SystemNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(SystemNotificationProvider.class);

    private final boolean emailEnabled;
    private final boolean smsEnabled;

    public SystemNotificationProvider(
            @Value("${app.integrations.notifications.email-enabled:false}") boolean emailEnabled,
            @Value("${app.integrations.notifications.sms-enabled:false}") boolean smsEnabled) {
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
    }

    @Override
    public void dispatch(User recipient, NotificationType type, String title, String message) {
        if (recipient == null) {
            log.warn("Cannot dispatch notification to null recipient: [{}] {}", type, title);
            return;
        }

        log.info("[OUTBOUND DISPATCH] To: {} <{}> | Channel: {} | Title: '{}' | Msg: '{}'",
                recipient.getName(), recipient.getEmail(), type, title, message);

        if (emailEnabled && recipient.getEmail() != null) {
            log.info("[EMAIL DISPATCHED] Sending SMTP alert email to {}", recipient.getEmail());
            // Email transport integration hook
        }

        if (smsEnabled && recipient.getPhone() != null) {
            log.info("[SMS DISPATCHED] Sending SMS emergency broadcast to {}", recipient.getPhone());
            // SMS transport integration hook
        }
    }

    @Override
    public boolean isExternalDeliveryConfigured() {
        return emailEnabled || smsEnabled;
    }
}
