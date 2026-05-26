package com.abc.foodwastemanagement.kafka;

import com.abc.foodwastemanagement.enums.KafkaNotificationType;
import com.abc.foodwastemanagement.notification.constants.NotificationTopics;
import com.abc.foodwastemanagement.notification.event.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public void publish(
            String userId,
            KafkaNotificationType eventType,
            String title,
            String message
    ) {

        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID().toString(),
                eventType,
                userId,
                title,
                message,
                Instant.now()
        );

        try {

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    NotificationTopics.APP_NOTIFICATION_EVENTS,
                    userId,
                    payload
            );

            log.info(
                "Kafka PRODUCER -> userId={}, type={}",
                userId,
                eventType
            );

        } catch (Exception ex) {

            log.warn(
                "Kafka connection failed. Notification skipped. eventId={}, userId={}, type={}",
                event.getEventId(),
                userId,
                eventType,
                ex
            );
        }
    }
}
