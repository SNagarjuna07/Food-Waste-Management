package com.abc.foodwastemanagement.dto.fooddonor;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FoodDonorResponse {

    private String id;
    private String name;
    private String type;
    private String address;
    private String contact;
    private String createdBy;
    private LocalDateTime createdAt;
}
