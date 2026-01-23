package com.abc.foodwastemanagement.dto.notification;

import java.time.LocalDateTime;

import com.abc.foodwastemanagement.enums.KafkaNotificationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Kafka event DTO (external contract)
 * Must use primitives only.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEventDto {

    private String eventId;
    private KafkaNotificationType eventType;
    private String userId;
    private String title;
    private String message;
    private LocalDateTime createdAt;
}
