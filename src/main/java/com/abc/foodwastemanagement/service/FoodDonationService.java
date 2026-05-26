package com.abc.foodwastemanagement.service;

import com.abc.foodwastemanagement.dto.fooddonation.FoodDonationResponse;
import com.abc.foodwastemanagement.dto.fooddonation.UpdateDonation;
import com.abc.foodwastemanagement.dto.page.PageResponse;
import com.abc.foodwastemanagement.dto.user.UserIdentity;
import com.abc.foodwastemanagement.entity.CollectionCenter;
import com.abc.foodwastemanagement.entity.FoodDonation;
import com.abc.foodwastemanagement.entity.FoodDonor;
import com.abc.foodwastemanagement.enums.DonationStatus;
import com.abc.foodwastemanagement.enums.ErrorCode;
import com.abc.foodwastemanagement.enums.KafkaNotificationType;
import com.abc.foodwastemanagement.enums.NotificationType;
import com.abc.foodwastemanagement.exception.InvalidRequestException;
import com.abc.foodwastemanagement.exception.OperationNotAllowedException;
import com.abc.foodwastemanagement.exception.ResourceNotFoundException;
import com.abc.foodwastemanagement.exception.UnauthorizedActionException;
import com.abc.foodwastemanagement.kafka.NotificationProducer;
import com.abc.foodwastemanagement.notification.NotificationServiceImpl;
import com.abc.foodwastemanagement.repository.FoodDonationRepository;
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
public class FoodDonationService {

    private final FoodDonationRepository foodDonationRepository;

    private final FoodDonorService foodDonorService;

    private final CollectionCenterService collectionCenterService;

    private final NotificationProducer notificationProducer;

    private final AuthenticatedUserService authenticatedUserService;

    private final NotificationServiceImpl notificationServiceImpl;

    // CREATE a donation
    @Transactional  
    @CacheEvict(
        value = "foodDonationsForUser",
        key = "#ownerUserId.toHexString() + '*'",
        allEntries = true
    )
    public void createDonation(
            ObjectId donorId,
            ObjectId centerId,
            String foodType,
            double quantity,
            LocalDateTime pickupDate,
            ObjectId ownerUserId) {


        if (quantity <= 0) {
            throw new InvalidRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "Quantity must be greater than 0.");
        }

        if (pickupDate.isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException(ErrorCode.PAST_PICKUP_DATE, "Pickup date cannot be in the past.");
        }

        FoodDonor donor = foodDonorService.getDonorById(donorId);
        if (!donor.getCreatedBy().equals(ownerUserId)) {
            throw new UnauthorizedActionException(
                    ErrorCode.UNAUTHORIZED_ACTION,
                    "Donor does not belong to user.");
        }

        CollectionCenter center = collectionCenterService.getById(centerId);
        if (!center.isActive()) {
            throw new InvalidRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "Collection center inactive.");
        }

        FoodDonation donation = new FoodDonation();
        donation.setDonorId(donorId);
        donation.setCollectionCenterId(centerId);
        donation.setFoodType(foodType);
        donation.setQuantity(quantity);
        donation.setStatus(DonationStatus.CREATED);
        donation.setPickupDate(pickupDate);
        donation.setCreatedAt(LocalDateTime.now());

        foodDonationRepository.save(donation);
    }

    // ---------- READ (USER) ----------
    @Transactional(readOnly = true)
    @Cacheable(
        value = "foodDonationByIdForUser",
        key = "#donationId.toHexString() + ':' + #userId.toHexString()",
        unless = "#result == null"
    )
    
    // Returns the donation of a particular user by checking ownership
    public FoodDonationResponse getByIdForUser(ObjectId donationId, ObjectId userId) {

        FoodDonation donation = foodDonationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.DONATION_NOT_FOUND,
                        "Donation not found."));

        FoodDonor donor = foodDonorService.getDonorById(donation.getDonorId());
        if (!donor.getCreatedBy().equals(userId)) {
            throw new UnauthorizedActionException(
                    ErrorCode.UNAUTHORIZED_ACTION,
                    "Unauthorized to access this donation.");
        }

        return mapToResponse(donation);
    }

    @Transactional(readOnly = true)
    @Cacheable(
        value = "foodDonationsForUser",
        key = "#userId.toHexString() + ':page=' + #page + '&size=' + #size",
        unless = "#result == null || #result.content.isEmpty()"
    )

    // Returns all donation with ownership vaildation
    public PageResponse<FoodDonationResponse> getDonationsForUser(ObjectId userId, int page, int size) {

        List<ObjectId> donorIds = foodDonorService.getAllDonorIdsByUser(userId);

        Pageable pageable = PageRequest.of(page, size);

        return PageResponse.from(foodDonationRepository
            .findByDonorIdInOrderByCreatedAt(donorIds, pageable)
            .map(this::mapToResponse));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "foodDonationByIdForUser", allEntries = true),
        @CacheEvict(value = "foodDonationsForUser", allEntries = true)
    })
    public void updateDonation(String donationId, UpdateDonation request) {
    
        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();
    
        FoodDonation donation = foodDonationRepository.findById(new ObjectId(donationId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.DONATION_NOT_FOUND,
                        "Donation not found.")
                );
    
        // Ownership check
        FoodDonor donor = foodDonorService.getDonorById(donation.getDonorId());
        if (!donor.getCreatedBy().equals(new ObjectId(user.getId()))) {
            throw new UnauthorizedActionException(
                    ErrorCode.UNAUTHORIZED_ACTION,
                    "You are not allowed to update this donation."
            );
        }
    
        // Status check
        if (donation.getStatus() != DonationStatus.CREATED) {
            throw new OperationNotAllowedException(
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only CREATED donations can be updated."
            );
        }
    
        donation.setFoodType(request.getFoodType());
        donation.setQuantity(request.getQuantity());
        donation.setPickupDate(request.getPickupDate());
        donation.setUpdatedAt(LocalDateTime.now());
    
        foodDonationRepository.save(donation);
    }
    

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "foodDonationByIdForUser", allEntries = true),
        @CacheEvict(value = "foodDonationsForUser", allEntries = true)
    })
    public void deleteDonation(String donationId) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        FoodDonation donation = foodDonationRepository.findById(new ObjectId(donationId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.DONATION_NOT_FOUND,
                        "Donation not found.")
                );

        // Ownership check
        FoodDonor donor = foodDonorService.getDonorById(donation.getDonorId());
        if (!donor.getCreatedBy().equals(new ObjectId(user.getId()))) {
            throw new UnauthorizedActionException(
                    ErrorCode.UNAUTHORIZED_ACTION,
                    "You are not allowed to delete this donation."
            );
        }

        // Status check
        if (donation.getStatus() != DonationStatus.CREATED) {
            throw new OperationNotAllowedException(
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only CREATED donations can be deleted."
            );
        }

        foodDonationRepository.delete(donation);
    }


    // ---------- COLLECT ----------
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "foodDonationByIdForUser", allEntries = true),
        @CacheEvict(value = "foodDonationsForUser", allEntries = true)
    })

    // Marks a donation collected (ADMIN) and notifies the user through Kafka
    public void collectDonation(ObjectId donationId) {

        UserIdentity user = authenticatedUserService.getCurrentUserIdentity();

        FoodDonation donation = foodDonationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.DONATION_NOT_FOUND,
                        "Donation not found."));

        if (donation.getStatus() != DonationStatus.CREATED) {
            throw new OperationNotAllowedException(
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only CREATED donations can be collected.");
        }

        donation.setStatus(DonationStatus.COLLECTED);
        donation.setUpdatedAt(LocalDateTime.now());

        foodDonationRepository.save(donation);

        // Save notification to DB
        notificationServiceImpl.saveNotification(
            new ObjectId(user.getId()), 
            "Donation collected", 
            "Your donation scheduled to pickup on " + donation.getPickupDate() + " is collected.", 
            NotificationType.INFO 
        );

        // Publish through Kafka
        notificationProducer.publish(
            user.getId(), 
            KafkaNotificationType.DONATION_COLLECTED, 
            "Donation collected", 
            "Your donation scheduled to pickup on " + donation.getPickupDate() + " is collected."
        );

        log.info("Donation with id: {} collected succesfully. Donated by: {} ({})", donation.getId(), user.getId(), user.getUsername());

    }

    // ---------- Mapper ----------
    private FoodDonationResponse mapToResponse(FoodDonation d) {

        FoodDonationResponse dto = new FoodDonationResponse();
        dto.setId(d.getId().toHexString());
        dto.setDonorId(d.getDonorId().toHexString());
        dto.setCollectionCenterId(d.getCollectionCenterId().toHexString());
        dto.setFoodType(d.getFoodType());
        dto.setQuantity(d.getQuantity());
        dto.setStatus(d.getStatus());
        dto.setPickupDate(d.getPickupDate());
        dto.setCreatedAt(d.getCreatedAt());
        dto.setUpdatedAt(d.getUpdatedAt());

        return dto;
    }
}
