package com.abc.foodwastemanagement.controller;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.abc.foodwastemanagement.dto.collectioncenter.CollectionCenterRequest;
import com.abc.foodwastemanagement.dto.collectioncenter.CollectionCenterResponse;
import com.abc.foodwastemanagement.dto.page.PageResponse;
import com.abc.foodwastemanagement.service.CollectionCenterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/collection-centers")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Food Waste Collection Center APIs")
@RequiredArgsConstructor
public class CollectionCenterController {

    private final CollectionCenterService collectionCenterService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(description = "Allows ADMIN to create a waste collection center.")
    @PostMapping
    public ResponseEntity<Void> createCollectionCenter(@RequestBody CollectionCenterRequest request) {

        collectionCenterService.createCenter(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    @Operation(description = "Allows ADMIN to update a waste collection center.")
    public ResponseEntity<Void> updateCollectionCenter(@PathVariable String id, @RequestBody CollectionCenterRequest request) {

        collectionCenterService.updateCenter(new ObjectId(id), request);

        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(description = "Allows ADMIN to delete a waste collection center.")
    public ResponseEntity<Void> deleteCollectionCenter(@PathVariable String id) {

        collectionCenterService.deleteCenter(new ObjectId(id));

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    @Operation(description = "Get all the ACTIVE waste collection centers.")
    public ResponseEntity<PageResponse<CollectionCenterResponse>> getActiveCenters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(collectionCenterService.getActiveCenters(page, size));
    }

    @GetMapping
    @Operation(description = "Get all the waste collection centers.")
    public ResponseEntity<PageResponse<CollectionCenterResponse>> getAllCenters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(collectionCenterService.getAll(page, size));
    }

    @GetMapping("/{id}")
    @Operation(description = "Gets a waste collection center by its ID.")
    public ResponseEntity<CollectionCenterResponse> getCenterById(@PathVariable String id) {

        return ResponseEntity.ok(collectionCenterService.getResponseById(new ObjectId(id)));
    }
}
