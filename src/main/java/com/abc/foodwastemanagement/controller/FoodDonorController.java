package com.abc.foodwastemanagement.controller;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.abc.foodwastemanagement.dto.fooddonor.FoodDonorCreationRequest;
import com.abc.foodwastemanagement.dto.fooddonor.FoodDonorResponse;
import com.abc.foodwastemanagement.dto.fooddonor.FoodDonorUpdate;
import com.abc.foodwastemanagement.dto.page.PageResponse;
import com.abc.foodwastemanagement.dto.user.UserIdentity;
import com.abc.foodwastemanagement.service.AuthenticatedUserService;
import com.abc.foodwastemanagement.service.FoodDonorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@PreAuthorize("hasAnyRole('USER','ADMIN')")
@RestController
@RequestMapping("/donors")
@Tag(name = "Food Waste Donor APIs")
@RequiredArgsConstructor
public class FoodDonorController {

    private final FoodDonorService foodDonorService;
    private final AuthenticatedUserService authenticatedUserService;

    // CREATE 
    @PostMapping
    @Operation(description = "Create a food waste donor.")
    public ResponseEntity<Void> createDonor(@RequestBody FoodDonorCreationRequest request) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        foodDonorService.createDonor(new ObjectId(user.getId()), request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // GET ALL DONORS 
    @GetMapping
    @Operation(description = "Get all food waste donors of an user.")
    public ResponseEntity<PageResponse<FoodDonorResponse>> getAllDonors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        return ResponseEntity.ok(
                foodDonorService.getDonorsByUser(new ObjectId(user.getId()), page, size)
        );
    }

    // Update a donor
    @PatchMapping("/{donorId}")
    @Operation(description = "Update a food waste donor.")
    public ResponseEntity<Void> updateDonor(@PathVariable String donorId, @RequestBody FoodDonorUpdate request) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        foodDonorService.updateDonor(new ObjectId(user.getId()), donorId, request);

        return ResponseEntity.noContent().build();
    }

    // DELETE DONOR 
    @Operation(description = "Delete a food waste donor.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{donorId}")
    public ResponseEntity<Void> deleteDonor(@PathVariable String donorId) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        foodDonorService.deleteDonor(new ObjectId(user.getId()), donorId);

        return ResponseEntity.noContent().build();
    }
}