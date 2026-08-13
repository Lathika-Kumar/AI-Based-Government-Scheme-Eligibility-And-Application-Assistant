package com.schemebridge.coreservice.scheme.repository;

import com.schemebridge.coreservice.scheme.entity.Scheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, String> {

    Optional<Scheme> findBySchemeCodeAndStatus(String schemeCode, String status);

    Optional<Scheme> findBySchemeCode(String schemeCode);

    Optional<Scheme> findBySlug(String slug);

    List<Scheme> findByStatusAndFeaturedTrue(String status);

    List<Scheme> findByStatusAndNewlyAddedTrue(String status);

    @Query("SELECT s FROM Scheme s WHERE s.status = 'ACTIVE' AND " +
           "(:categoryCode IS NULL OR s.category.code = :categoryCode) AND " +
           "(:schemeType IS NULL OR s.schemeType = :schemeType) AND " +
           "(:featured IS NULL OR s.featured = :featured) AND " +
           "(LOWER(s.titleEnglish) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword IS NULL)")
    Page<Scheme> searchSchemes(
            @Param("keyword") String keyword,
            @Param("categoryCode") String categoryCode,
            @Param("schemeType") String schemeType,
            @Param("featured") Boolean featured,
            Pageable pageable);

    @Query("SELECT s FROM Scheme s WHERE s.status = 'ACTIVE' AND " +
           "(:category IS NULL OR s.category.code = :category OR EXISTS (SELECT r FROM SchemeEligibilityRule r WHERE r.scheme = s AND r.ruleType = 'CATEGORY' AND LOWER(r.valueString) LIKE LOWER(CONCAT('%', :category, '%')) AND r.status = 'ACTIVE')) AND " +
           "(:income IS NULL OR NOT EXISTS (SELECT r FROM SchemeEligibilityRule r WHERE r.scheme = s AND r.ruleType = 'INCOME' AND r.valueNumberMax < :income AND r.status = 'ACTIVE'))")
    List<Scheme> findEligibleSchemes(@Param("category") String category, @Param("income") java.math.BigDecimal income);


    List<Scheme> findByStatusOrderByPriorityAscPopularityScoreDesc(String status);
}
