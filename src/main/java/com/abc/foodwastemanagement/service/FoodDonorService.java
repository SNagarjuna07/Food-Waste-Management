package com.abc.foodwastemanagement.service;

import com.abc.foodwastemanagement.dto.fooddonor.FoodDonorCreationRequest;
import com.abc.foodwastemanagement.dto.fooddonor.FoodDonorResponse;
import com.abc.foodwastemanagement.dto.fooddonor.FoodDonorUpdate;
import com.abc.foodwastemanagement.dto.page.PageResponse;
import com.abc.foodwastemanagement.entity.FoodDonor;
import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.exception.InvalidRequestException;
import com.abc.foodwastemanagement.exception.OperationNotAllowedException;
import com.abc.foodwastemanagement.exception.ResourceNotFoundException;
import com.abc.foodwastemanagement.exception.UnauthorizedActionException;
import com.abc.foodwastemanagement.repository.FoodDonorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FoodDonorService {

    private final FoodDonorRepository foodDonorRepository;

    // Create a DONOR
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "foodDonorsByUser", allEntries = true),
        @CacheEvict(value = "foodDonorIdsByUser", key = "#userId.toHexString()")
    })
    public void createDonor(ObjectId userId, FoodDonorCreationRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new InvalidRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "Donor name is required"
            );
        }

        FoodDonor donor = new FoodDonor();
        donor.setName(request.getName());
        donor.setType(request.getType());
        donor.setAddress(request.getAddress());
        donor.setContact(request.getContact());
        donor.setCreatedBy(userId);
        donor.setCreatedAt(LocalDateTime.now());

        foodDonorRepository.save(donor);
    }

    // Returns all the donors created by a particular user
    @Transactional(readOnly = true)
    @Cacheable(
        value = "foodDonorsByUser",
        key = "#userId.toHexString() + ':page=' + #page + '&size=' + #size",
        unless = "#result == null || #result.content.isEmpty()"
    )
    public PageResponse<FoodDonorResponse> getDonorsByUser(
            ObjectId userId,
            int page,
            int size) {

        log.info("Hitting DB...");
    
        Pageable pageable = PageRequest.of(page, size);

        return PageResponse.from(
                foodDonorRepository
                        .findByCreatedByOrderByCreatedAt(userId, pageable)
                        .map(this::mapToResponse)
        );
    }

    // Deletes a donor
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "foodDonorsByUser", allEntries = true),
        @CacheEvict(value = "foodDonorIdsByUser", key = "#userId.toHexString()")
    })
    public void deleteDonor(ObjectId userId, String donorId) {

        ObjectId donorObjectId;

        try {

            donorObjectId = new ObjectId(donorId);

        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid donor ID format"
            );
        }

        FoodDonor donor = foodDonorRepository.findById(donorObjectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.DONATION_NOT_FOUND,
                        "Food donor not found"
                ));

        if (!donor.getCreatedBy().equals(userId)) {
            throw new UnauthorizedActionException(
                    ErrorCode.UNAUTHORIZED_ACTION,
                    "Donor does not belong to user"
            );
        }

        foodDonorRepository.delete(donor);
    }

    // Returns donor by ID
    @Transactional(readOnly = true)
    public FoodDonor getDonorById(ObjectId donorId) {
        return foodDonorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.DONATION_NOT_FOUND,
                        "Food donor not found"
                ));
    }

    // Updates a donor
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "foodDonorsByUser", allEntries = true),
        @CacheEvict(value = "foodDonorIdsByUser", key = "#userId.toHexString()")
    })
    public void updateDonor(ObjectId userId, String donorId, FoodDonorUpdate request) {
    
        ObjectId donorObjectId;
        try {
            donorObjectId = new ObjectId(donorId);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid donor ID format"
            );
        }
    
        FoodDonor donor = foodDonorRepository.findById(donorObjectId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        ErrorCode.DONOR_NOT_FOUND,
                        "Food donor not found."
                    )
                );
    
        if (!donor.getCreatedBy().equals(userId)) {
            throw new OperationNotAllowedException(
                    ErrorCode.UNAUTHORIZED_ACTION,
                    "You cannot update this donor."
            );
        }
    
        donor.setName(request.getName());
        donor.setType(request.getType());
        donor.setAddress(request.getAddress());
        donor.setContact(request.getContact());
        donor.setUpdatedAt(LocalDateTime.now());
    
        foodDonorRepository.save(donor);
    }
    
    //Cached internal helper (used by FoodDonationService)
    @Transactional(readOnly = true)
    @Cacheable(
        value = "foodDonorIdsByUser",
        key = "#userId.toHexString()",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<ObjectId> getAllDonorIdsByUser(ObjectId userId) {

        return foodDonorRepository.findByCreatedBy(userId)
                .stream()
                .map(FoodDonor::getId)
                .toList();
    }

    // ========================= MAPPER =========================

    private FoodDonorResponse mapToResponse(FoodDonor donor) {

        FoodDonorResponse dto = new FoodDonorResponse();
        dto.setId(donor.getId().toHexString());
        dto.setName(donor.getName());
        dto.setType(donor.getType());
        dto.setAddress(donor.getAddress());
        dto.setContact(donor.getContact());
        dto.setCreatedBy(donor.getCreatedBy().toHexString());
        dto.setCreatedAt(donor.getCreatedAt());

        return dto;
    }
}