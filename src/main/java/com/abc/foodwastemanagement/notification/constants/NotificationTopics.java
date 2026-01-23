package com.abc.foodwastemanagement.notification.constants;

// Central place for Kafka topic names
// Avoids hard-coded strings everywhere

public final class NotificationTopics {

    private NotificationTopics() {
        // prevent instantiation
    }

    public static final String APP_NOTIFICATION_EVENTS = "app-notification-events";
}
