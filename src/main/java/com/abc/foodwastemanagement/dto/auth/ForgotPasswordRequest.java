package com.abc.foodwastemanagement.dto.auth;

import com.mongodb.lang.NonNull;

import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NonNull
    private String email;
}
