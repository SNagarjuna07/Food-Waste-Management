package com.abc.foodwastemanagement.dto.fooddonation;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UpdateDonation {

    private String foodType;
    private double quantity;
    private LocalDateTime pickupDate;
}
