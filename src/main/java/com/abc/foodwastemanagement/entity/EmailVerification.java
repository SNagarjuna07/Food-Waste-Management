package com.abc.foodwastemanagement.entity;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "email_verification_tokens")
public class EmailVerification {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String token;

    private ObjectId userId;

    @Indexed
    private LocalDateTime expiryTime;

    private boolean used;
}
