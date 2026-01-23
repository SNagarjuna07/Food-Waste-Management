package com.abc.foodwastemanagement.dto.user;

import java.util.List;

import lombok.Data;

@Data
public class UserIdentity {

    private String id;
    private String username;
    private String email;
    private boolean emailVerified;
    private List<String> roles;

}
