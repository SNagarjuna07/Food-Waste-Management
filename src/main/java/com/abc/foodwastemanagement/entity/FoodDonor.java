package com.abc.foodwastemanagement.entity;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "food_donors")
@CompoundIndex(
    name = "createdBy_created_idx",
    def = "{ 'createdBy': 1, 'createdAt': -1 }"
)
public class FoodDonor {

    @Id
    private ObjectId id;

    private String name;
    private String type;
    private String address;

    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$")
    private String contact;

    @Indexed
    private ObjectId createdBy; // User ID 
    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
