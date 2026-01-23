package com.abc.foodwastemanagement.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.abc.foodwastemanagement.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, ObjectId> {

    Optional<PasswordResetToken> findByToken(String token);

    // or -> Logical OR, lt -> less than, ? -> Parameter, 0 -> Parameter index
    // DELETE FROM email_verification WHERE expiry_time < NOW() OR used = true;
    @Query(
        value = "{ '$or': [ { 'expiryTime': { '$lt': ?0 } }, { 'used': true } ] }",
        delete = true
    )
    void deleteExpiredOrUsed(LocalDateTime now);

}
