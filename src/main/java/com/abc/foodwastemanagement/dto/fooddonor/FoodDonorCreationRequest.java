package com.abc.foodwastemanagement.dto.fooddonor;

import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodDonorCreationRequest {

    private String name;
    private String type;
    private String address;
    private String contact;
}
