package com.abc.foodwastemanagement.dto.fooddonation;

import com.abc.foodwastemanagement.enums.DonationStatus;

import lombok.Data;

@Data
public class UpdateDonationStatusRequest {

    // COLLECTED / PROCESSED
    private DonationStatus status;
}
