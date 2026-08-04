package com.schemebridge.repository;

import com.schemebridge.entity.Scheme;
import com.schemebridge.enums.SchemeStatus;
import com.schemebridge.enums.SchemeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRepository extends MongoRepository<Scheme, String> {

    Optional<Scheme> findByIdAndDeletedFalse(String id);

    Optional<Scheme> findBySchemeCode(String schemeCode);

    Page<Scheme> findByStatusAndDeletedFalse(SchemeStatus status, Pageable pageable);

    Page<Scheme> findByCategoryAndStatusAndDeletedFalse(String category, SchemeStatus status, Pageable pageable);

    Page<Scheme> findBySchemeTypeAndStatusAndDeletedFalse(SchemeType schemeType, SchemeStatus status, Pageable pageable);

    Page<Scheme> findByFeaturedTrueAndStatusAndDeletedFalse(SchemeStatus status, Pageable pageable);

    List<Scheme> findTop10ByStatusAndDeletedFalseOrderByRecommendationCountDescViewCountDesc(SchemeStatus status);

    List<Scheme> findTop10ByStatusAndDeletedFalseOrderByCreatedAtDesc(SchemeStatus status);

    List<Scheme> findByStatusAndDeletedFalse(SchemeStatus status);

    @Query("{ 'status': ?0, 'deleted': false, '$or': [ { 'schemeName': { '$regex': ?1, '$options': 'i' } }, { 'description': { '$regex': ?1, '$options': 'i' } }, { 'shortDescription': { '$regex': ?1, '$options': 'i' } }, { 'tags': { '$regex': ?1, '$options': 'i' } }, { 'category': { '$regex': ?1, '$options': 'i' } } ] }")
    Page<Scheme> searchPublishedSchemes(SchemeStatus status, String keyword, Pageable pageable);

    long countByStatusAndDeletedFalse(SchemeStatus status);

    long countByCategoryAndStatusAndDeletedFalse(String category, SchemeStatus status);
}
