package com.schemebridge.repository;

import com.schemebridge.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Boolean existsByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
}
