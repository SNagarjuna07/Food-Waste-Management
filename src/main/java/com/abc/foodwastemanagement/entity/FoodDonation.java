package com.abc.foodwastemanagement.entity;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.abc.foodwastemanagement.enums.DonationStatus;

import lombok.Data;

@Data
@Document(collection = "food_donations")
@CompoundIndex(
    name = "created_desc_idx",
    def = "{ 'createdAt': -1 }"
)
public class FoodDonation {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId donorId;

    @Indexed
    private ObjectId collectionCenterId;

    private String foodType;
    private double quantity;

    private DonationStatus status;

    private LocalDateTime pickupDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
