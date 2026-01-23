package com.abc.foodwastemanagement.kafka;

import com.abc.foodwastemanagement.dto.notification.NotificationEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    public NotificationConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "app-notification-events", groupId = "app-notification-group")
    public void consume(String message, Acknowledgment acknowledgment) {
        
        try {
            // Deserialize JSON → DTO
            NotificationEventDto event = objectMapper.readValue(message, NotificationEventDto.class);

            log.info(
                "Kafka CONSUMER <- eventId={}, userId={}, type={}",
                event.getEventId(),
                event.getUserId(),
                event.getEventType()
            );

            // Acknowledge message ONLY after success
            acknowledgment.acknowledge();

        } catch (Exception ex) {

            log.error(
                "Failed to process Kafka notification event. Message={}",
                message,
                ex
            );
        }
    }
}


