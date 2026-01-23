package com.abc.foodwastemanagement.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.abc.foodwastemanagement.entity.EmailVerification;


public interface EmailVerificationRepository extends MongoRepository<EmailVerification, ObjectId> {

    @Cacheable(
        value = "emailVerificationTokens",
        key = "#token"
    )
    public Optional<EmailVerification> findByToken(String token);

    @Query(
        value = "{ '$or': [ { 'expiryTime': { '$lt': ?0 } }, { 'used': true } ] }",
        delete = true
    )
    void deleteExpiredOrUsed(LocalDateTime now);
}

