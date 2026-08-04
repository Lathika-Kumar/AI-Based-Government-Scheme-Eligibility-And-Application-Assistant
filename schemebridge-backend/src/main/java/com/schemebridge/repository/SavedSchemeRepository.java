package com.schemebridge.repository;

import com.schemebridge.entity.SavedScheme;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedSchemeRepository extends MongoRepository<SavedScheme, String> {

    List<SavedScheme> findByUserId(String userId);

    Optional<SavedScheme> findByUserIdAndSchemeId(String userId, String schemeId);

    void deleteByUserIdAndSchemeId(String userId, String schemeId);
}
