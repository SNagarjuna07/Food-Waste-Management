package com.abc.foodwastemanagement.repository;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.abc.foodwastemanagement.entity.CollectionCenter;

public interface CollectionCenterRepository extends MongoRepository<CollectionCenter, ObjectId> {

    Page<CollectionCenter> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    long countByActiveTrue();

}
