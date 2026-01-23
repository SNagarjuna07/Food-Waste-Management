package com.abc.foodwastemanagement.dto.user;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({
    "id",
    "username",
    "email",
    "enabled"
})
public class UserResponse {

    private String id;
    private String username;
    private String email;
    private boolean enabled;
}
