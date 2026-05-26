package com.abc.foodwastemanagement.controller;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.abc.foodwastemanagement.dto.fooddonation.CreateFoodDonationRequest;
import com.abc.foodwastemanagement.dto.fooddonation.FoodDonationResponse;
import com.abc.foodwastemanagement.dto.fooddonation.UpdateDonation;
import com.abc.foodwastemanagement.dto.page.PageResponse;
import com.abc.foodwastemanagement.dto.user.UserIdentity;
import com.abc.foodwastemanagement.service.AuthenticatedUserService;
import com.abc.foodwastemanagement.service.FoodDonationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/donations")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Food Waste Donation APIs")
@RequiredArgsConstructor
public class FoodDonationController {

    private final FoodDonationService foodDonationService;
    private final AuthenticatedUserService authenticatedUserService;
    @PostMapping
    @Operation(description = "Allows user to create a food donation.")
    public ResponseEntity<Void> createDonation(@RequestBody CreateFoodDonationRequest request) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        foodDonationService.createDonation(
                new ObjectId(request.getDonorId()),
                new ObjectId(request.getCollectionCenterId()),
                request.getFoodType(),
                request.getQuantity(),
                request.getPickupDate(),
                new ObjectId(user.getId())
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    @Operation(description = "Get a food donation by its ID.")
    public ResponseEntity<FoodDonationResponse> getById(@PathVariable String id) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        return ResponseEntity.ok(
                foodDonationService.getByIdForUser(
                        new ObjectId(id), new ObjectId(user.getId()))
        );
    }

    @PatchMapping("/{donationId}")
    @Operation(description = "Update a food donation.")
    public ResponseEntity<Void> updateDonation(@PathVariable String  donationId, @RequestBody UpdateDonation request) {

        foodDonationService.updateDonation(donationId, request);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{donationId}")
    @Operation(description = "Delete a food donation.")
    public ResponseEntity<Void> deleteDonation(@PathVariable String  donationId) {

        foodDonationService.deleteDonation(donationId);
        
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    @GetMapping("/my-donations")
    @Operation(description = "Get all the food donations of an user.")
    public ResponseEntity<PageResponse<FoodDonationResponse>> getMyDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ObjectId userId = authenticatedUserService.getCurrentUserId();

        return ResponseEntity.ok(
                foodDonationService.getDonationsForUser(
                        userId, page, size)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/collect")
    @Operation(description = "Allows ADMIN to collect a food donation.")
    public ResponseEntity<Void> collectDonation(
            @PathVariable String id) {

        foodDonationService.collectDonation(new ObjectId(id));
        return ResponseEntity.ok().build();
    }
}
