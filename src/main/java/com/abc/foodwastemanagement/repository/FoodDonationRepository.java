package com.abc.foodwastemanagement.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.abc.foodwastemanagement.entity.FoodDonation;

public interface FoodDonationRepository extends MongoRepository<FoodDonation, ObjectId> {

    Page<FoodDonation> findByDonorId(ObjectId donorId, Pageable pageable);

    Page<FoodDonation> findByDonorIdInOrderByCreatedAt(List<ObjectId> donorIds, Pageable pageable);
}
