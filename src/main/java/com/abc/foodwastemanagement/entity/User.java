package com.abc.foodwastemanagement.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.abc.foodwastemanagement.enums.AuthProvider;
import com.mongodb.lang.NonNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")

public class User {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @NonNull
    private String username;

    @Indexed(unique = true)
    private String email;

    private AuthProvider authProvider; // LOCAL | GOOGLE

    @NonNull
    private String password;

    private List<String> roles;

    private boolean emailVerified;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
