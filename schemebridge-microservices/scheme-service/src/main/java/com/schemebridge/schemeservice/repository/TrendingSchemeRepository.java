package com.schemebridge.schemeservice.repository;

import com.schemebridge.schemeservice.entity.TrendingScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrendingSchemeRepository extends JpaRepository<TrendingScheme, String> {
    List<TrendingScheme> findByStatusOrderByRankPositionAsc(String status);
    boolean existsBySchemeId(String schemeId);
}
