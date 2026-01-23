package com.abc.foodwastemanagement.dto.user;

import com.mongodb.lang.NonNull;

import lombok.Data;

@Data
public class UserLoginDto {

    @NonNull
    private String username;
    
    @NonNull
    private String password;
}
