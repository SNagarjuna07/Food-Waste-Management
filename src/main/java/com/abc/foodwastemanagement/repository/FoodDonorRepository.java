package com.abc.foodwastemanagement.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.abc.foodwastemanagement.entity.FoodDonor;

public interface FoodDonorRepository extends MongoRepository<FoodDonor, ObjectId> {

    // Get all the donors created by an user
    Page<FoodDonor> findByCreatedByOrderByCreatedAt(ObjectId userId, Pageable pageable);

    FoodDonor findByName(String donorName);

    List<FoodDonor> findByCreatedBy(ObjectId userId);

}

