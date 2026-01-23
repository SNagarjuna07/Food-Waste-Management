package com.abc.foodwastemanagement.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.abc.foodwastemanagement.dto.user.RegisterUserRequest;
import com.abc.foodwastemanagement.entity.User;

import java.util.Optional;

import org.bson.types.ObjectId;

public interface UserRepository extends MongoRepository<User, ObjectId> {

    // Find by username
    User findByUsername(String username);

    // Find by email
    Optional<User> findByEmail(String email);

    // Check existence by username
    boolean existsByUsername(String username);

    // Check existence by email
    boolean existsByEmail(String email);

    void save(RegisterUserRequest user);
}
