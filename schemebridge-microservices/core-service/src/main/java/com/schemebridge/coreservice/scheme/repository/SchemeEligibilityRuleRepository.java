package com.schemebridge.coreservice.scheme.repository;

import com.schemebridge.coreservice.scheme.entity.SchemeEligibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeEligibilityRuleRepository extends JpaRepository<SchemeEligibilityRule, String> {
    List<SchemeEligibilityRule> findBySchemeIdAndStatusOrderByDisplayOrderAsc(String schemeId, String status);
    List<SchemeEligibilityRule> findBySchemeIdAndRuleTypeAndStatus(String schemeId, String ruleType, String status);
}
