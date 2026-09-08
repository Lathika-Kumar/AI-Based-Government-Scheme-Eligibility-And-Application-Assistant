package com.schemebridge.scheme.ml.feature;

import com.schemebridge.scheme.document.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extracts SchemeFeatures from master Scheme and canonical SchemeVerifiedData.
 * Respects authoritative data without guessing or inventing unverified legal criteria.
 * Priority:
 * 1. Authoritative structured verified criteria (SchemeVerifiedData.structuredEligibility)
 * 2. Existing master RuleGroup criteria
 * 3. UNKNOWN when criteria cannot be reliably determined
 */
@Component
public class SchemeFeatureExtractor {

    public SchemeFeatures extractFromScheme(Scheme scheme, SchemeVerifiedData verifiedData) {
        if (scheme == null) {
            return buildEmptySchemeFeatures();
        }

        String code = scheme.getSchemeCode() != null ? scheme.getSchemeCode() : "UNKNOWN";
        String cat = (scheme.getCategory() != null && scheme.getCategory().getCode() != null)
                ? scheme.getCategory().getCode() : "UNKNOWN";
        String level = scheme.getSchemeLevel() != null ? scheme.getSchemeLevel().name() : "CENTRAL";
        String state = scheme.getStateOrUt() != null ? scheme.getStateOrUt().trim().toUpperCase() : null;

        Integer minAge = null;
        Integer maxAge = null;
        Double maxIncome = null;
        List<String> occupations = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        List<String> genders = new ArrayList<>();
        Boolean disabilityApplicable = null;

        // 1. PRIORITY 1: Authoritative canonical SchemeVerifiedData structured eligibility
        if (verifiedData != null && verifiedData.getEligibility() != null) {
            Map<String, Object> struct = verifiedData.getEligibility().getStructuredEligibility();
            if (struct != null) {
                if (struct.get("minAge") instanceof Number n) {
                    minAge = n.intValue();
                }
                if (struct.get("maxAge") instanceof Number n) {
                    maxAge = n.intValue();
                }
                if (struct.get("maxIncome") instanceof Number n) {
                    maxIncome = n.doubleValue();
                }
                if (struct.get("eligibleOccupations") instanceof List<?> list) {
                    for (Object o : list) {
                        if (o != null && !o.toString().isBlank()) {
                            String occ = o.toString().trim().toUpperCase();
                            if (!occupations.contains(occ)) occupations.add(occ);
                        }
                    }
                }
                if (struct.get("eligibleCategories") instanceof List<?> list) {
                    for (Object o : list) {
                        if (o != null && !o.toString().isBlank()) {
                            String c = o.toString().trim().toUpperCase();
                            if (!categories.contains(c)) categories.add(c);
                        }
                    }
                }
                if (struct.get("eligibleGenders") instanceof List<?> list) {
                    for (Object o : list) {
                        if (o != null && !o.toString().isBlank()) {
                            String g = o.toString().trim().toUpperCase();
                            if (!genders.contains(g)) genders.add(g);
                        }
                    }
                }
                if (struct.get("disabilityApplicable") instanceof Boolean b) {
                    disabilityApplicable = b;
                }
            }
        }

        // 2. PRIORITY 2: Master RuleGroup criteria (fills in any attributes not supplied by verifiedData)
        if (scheme.getEligibilityRules() != null) {
            RuleCriteriaHolder holder = new RuleCriteriaHolder();
            collectCriteria(scheme.getEligibilityRules(), holder);
            if (minAge == null) minAge = holder.minAge;
            if (maxAge == null) maxAge = holder.maxAge;
            if (maxIncome == null) maxIncome = holder.maxIncome;
            if (occupations.isEmpty() && !holder.occupations.isEmpty()) {
                occupations.addAll(holder.occupations);
            }
            if (categories.isEmpty() && !holder.categories.isEmpty()) {
                categories.addAll(holder.categories);
            }
            if (genders.isEmpty() && !holder.genders.isEmpty()) {
                genders.addAll(holder.genders);
            }
            if (disabilityApplicable == null) {
                disabilityApplicable = holder.disabilityApplicable;
            }
        }

        // Note: Ambiguous free-text criteria are NEVER inferred as MATCH. Unspecified fields remain null/UNKNOWN.

        String benefitCategory = (scheme.getBenefits() != null && !scheme.getBenefits().isEmpty() && scheme.getBenefits().get(0).getAmountType() != null)
                ? scheme.getBenefits().get(0).getAmountType() : "DIRECT_BENEFIT";

        return SchemeFeatures.builder()
                .schemeCode(code)
                .schemeCategory(cat.toUpperCase())
                .benefitCategory(benefitCategory)
                .schemeLevel(level)
                .stateOrUt(state)
                .minAge(minAge)
                .maxAge(maxAge)
                .maxIncome(maxIncome)
                .eligibleOccupations(occupations.isEmpty() ? null : occupations)
                .eligibleCategories(categories.isEmpty() ? null : categories)
                .eligibleGenders(genders.isEmpty() ? null : genders)
                .disabilityApplicable(disabilityApplicable)
                .build();
    }

    private static class RuleCriteriaHolder {
        Integer minAge;
        Integer maxAge;
        Double maxIncome;
        List<String> occupations = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        List<String> genders = new ArrayList<>();
        Boolean disabilityApplicable;
    }

    private void collectCriteria(RuleGroup group, RuleCriteriaHolder holder) {
        if (group == null) return;
        if (group.getConditions() != null) {
            for (EligibilityCondition cond : group.getConditions()) {
                if (cond.getField() == null || cond.getValue() == null) continue;
                String f = cond.getField().toLowerCase();
                String v = cond.getValue().toString().trim().toUpperCase();
                RuleOperator op = cond.getOperator();

                if (f.equals("age")) {
                    try {
                        int ageVal = Integer.parseInt(v);
                        if (op == RuleOperator.GREATER_THAN_OR_EQUAL || op == RuleOperator.GREATER_THAN) {
                            holder.minAge = ageVal;
                        } else if (op == RuleOperator.LESS_THAN_OR_EQUAL || op == RuleOperator.LESS_THAN) {
                            holder.maxAge = ageVal;
                        }
                    } catch (NumberFormatException ignored) {}
                } else if (f.contains("income")) {
                    try {
                        double incVal = Double.parseDouble(v);
                        if (op == RuleOperator.LESS_THAN_OR_EQUAL || op == RuleOperator.LESS_THAN) {
                            holder.maxIncome = incVal;
                        }
                    } catch (NumberFormatException ignored) {}
                } else if (f.equals("occupation")) {
                    if (!holder.occupations.contains(v)) holder.occupations.add(v);
                } else if (f.equals("socialcategory") || f.equals("caste")) {
                    if (!holder.categories.contains(v)) holder.categories.add(v);
                } else if (f.equals("gender")) {
                    if (!holder.genders.contains(v)) holder.genders.add(v);
                } else if (f.contains("disability")) {
                    if ("TRUE".equalsIgnoreCase(v)) holder.disabilityApplicable = true;
                }
            }
        }
        if (group.getGroups() != null) {
            for (RuleGroup sub : group.getGroups()) {
                collectCriteria(sub, holder);
            }
        }
    }

    public SchemeFeatures buildEmptySchemeFeatures() {
        return SchemeFeatures.builder()
                .schemeCode("UNKNOWN")
                .schemeCategory("UNKNOWN")
                .benefitCategory("UNKNOWN")
                .schemeLevel("CENTRAL")
                .stateOrUt(null)
                .minAge(null)
                .maxAge(null)
                .maxIncome(null)
                .eligibleOccupations(null)
                .eligibleCategories(null)
                .eligibleGenders(null)
                .disabilityApplicable(null)
                .build();
    }
}
