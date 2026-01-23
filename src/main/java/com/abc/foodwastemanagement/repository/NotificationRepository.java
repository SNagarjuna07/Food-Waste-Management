package com.abc.foodwastemanagement.repository;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.abc.foodwastemanagement.entity.Notification;
import com.abc.foodwastemanagement.repository.custom.NotificationRepositoryCustom;

public interface NotificationRepository extends MongoRepository<Notification, ObjectId>, NotificationRepositoryCustom {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(ObjectId userId, Pageable pageable);



    long countByUserIdAndReadFalse(ObjectId userId);

    @Query(
        value = "{ 'read': true, 'createdAt': { '$lt': ?0 } }",
        delete = true
    )
    void deleteByReadTrueAndCreatedAtBefore(LocalDateTime cutoffTime);
}
