package com.abc.foodwastemanagement.notification.event;

import java.time.Instant;

import com.abc.foodwastemanagement.enums.KafkaNotificationType;

import lombok.Getter;

/**
 * Kafka event contract.
 * Must be timezone-safe and serializable.
 */
@Getter
public class NotificationEvent {

    private String eventId;
    private KafkaNotificationType eventType;
    private String userId;
    private String title;
    private String message;
    private Instant createdAt;

    protected NotificationEvent() {}

    public NotificationEvent(
            String eventId,
            KafkaNotificationType eventType,
            String userId,
            String title,
            String message,
            Instant createdAt
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
    }
}
