package com.abc.foodwastemanagement.dto.user;

import org.springframework.data.mongodb.core.index.Indexed;

import com.mongodb.lang.NonNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserRequest {

    private String username;

    @Indexed(unique = true)
    private String email;

    @NonNull
    private String password;
}
