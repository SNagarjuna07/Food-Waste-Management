package com.abc.foodwastemanagement.dto.fooddonation;

import java.time.LocalDateTime;

import com.abc.foodwastemanagement.enums.DonationStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodDonationResponse {

    private String id;
    private String donorId;
    private String collectionCenterId;

    private String foodType;
    private double quantity;
    private DonationStatus status;

    private LocalDateTime pickupDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
