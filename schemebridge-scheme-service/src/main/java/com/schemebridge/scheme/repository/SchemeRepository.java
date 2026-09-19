package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.Optional;
import java.util.List;

public interface SchemeRepository extends MongoRepository<Scheme, String> {
    Optional<Scheme> findBySchemeCode(String schemeCode);
    Optional<Scheme> findBySlug(String slug);
    List<Scheme> findAllByStatus(SchemeStatus status);
    List<Scheme> findBySchemeCodeIn(java.util.Collection<String> schemeCodes);
    long countByStatus(SchemeStatus status);
    
    @Query("{ 'category.code' : ?0 }")

    List<Scheme> findAllByCategoryCode(String categoryCode);
    
    @Query("{ 'category.code' : ?0, 'status' : ?1 }")
    List<Scheme> findAllByCategoryCodeAndStatus(String categoryCode, SchemeStatus status);
}
