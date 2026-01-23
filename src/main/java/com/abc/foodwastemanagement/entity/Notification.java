package com.abc.foodwastemanagement.entity;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import com.abc.foodwastemanagement.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Document(collection = "notifications")
@CompoundIndex(
    name = "user_read_idx",
    def = "{ 'userId': 1, 'read': 1 }"
)
@CompoundIndex(
    name = "user_created_idx",
    def = "{ 'userId': 1, 'createdAt': -1 }"
)
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @Id
    private ObjectId id;

    private ObjectId userId;

    private String title;
    private String message;

    private NotificationType type;

    private boolean read;

    private LocalDateTime createdAt;
}
