package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.CitizenProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for citizen_profiles collection.
 *
 * All queries are scoped to a single userId (Oracle USERS.id).
 * The unique index on userId enforces one profile per authenticated citizen.
 */
@Repository
public interface CitizenProfileRepository extends MongoRepository<CitizenProfile, String> {

    Optional<CitizenProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);
}
