package com.schemebridge.scheme.service.verification;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.VerificationCheckDetail;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Validates consistency between extracted document text/attributes and authenticated citizen profile.
 * Implements strict rules: unextracted fields are marked NOT_CHECKED and NEVER awarded positive score.
 */
@Component
@Slf4j
public class ProfileConsistencyChecker {

    @Getter
    @Builder
    public static class ConsistencyResult {
        private final double totalConsistencyScore; // max 30.0 (Name 15, DOB 10, State 5)
        private final List<VerificationCheckDetail> checks;
        private final List<String> inconsistencies;
    }

    public ConsistencyResult checkConsistency(Map<String, Object> extractedFields, String rawText, CitizenProfile profile) {
        List<VerificationCheckDetail> checks = new ArrayList<>();
        List<String> inconsistencies = new ArrayList<>();
        double totalScore = 0.0;

        if (profile == null) {
            checks.add(VerificationCheckDetail.builder()
                    .check("NAME_MATCH")
                    .status("NOT_CHECKED")
                    .message("Citizen profile not available for cross-referencing")
                    .score(0.0)
                    .weight(15.0)
                    .build());
            checks.add(VerificationCheckDetail.builder()
                    .check("DOB_MATCH")
                    .status("NOT_CHECKED")
                    .message("Citizen profile not available for DOB cross-referencing")
                    .score(0.0)
                    .weight(10.0)
                    .build());
            checks.add(VerificationCheckDetail.builder()
                    .check("RESIDENCE_MATCH")
                    .status("NOT_CHECKED")
                    .message("Citizen profile state not available for cross-referencing")
                    .score(0.0)
                    .weight(5.0)
                    .build());
            return ConsistencyResult.builder()
                    .totalConsistencyScore(0.0)
                    .checks(checks)
                    .inconsistencies(inconsistencies)
                    .build();
        }

        // 1. Name Match (Weight: 15.0)
        String profileName = profile.getDisplayName() != null ? profile.getDisplayName().trim().toLowerCase() : "";
        String extractedName = extractedFields != null && extractedFields.get("name") != null ?
                extractedFields.get("name").toString().trim().toLowerCase() : null;

        if (profileName.isEmpty() || (extractedName == null && (rawText == null || rawText.isEmpty()))) {
            checks.add(VerificationCheckDetail.builder()
                    .check("NAME_MATCH")
                    .status("NOT_CHECKED")
                    .message("Name information not available in document or profile")
                    .score(0.0)
                    .weight(15.0)
                    .build());
        } else {
            boolean matches = false;
            double similarity = 0.0;

            if (extractedName != null) {
                similarity = calculateSimilarity(profileName, extractedName);
                matches = similarity >= 0.75;
            } else if (rawText != null) {
                // Check if profile name tokens exist in raw text
                String[] tokens = profileName.split("\\s+");
                int found = 0;
                String lowerRaw = rawText.toLowerCase();
                for (String t : tokens) {
                    if (t.length() >= 3 && lowerRaw.contains(t)) {
                        found++;
                    }
                }
                matches = (found >= Math.max(1, tokens.length - 1));
                similarity = matches ? 0.85 : 0.40;
            }

            if (matches) {
                double score = 15.0;
                totalScore += score;
                checks.add(VerificationCheckDetail.builder()
                        .check("NAME_MATCH")
                        .status("PASSED")
                        .message("Extracted name is consistent with citizen profile (" + profile.getDisplayName() + ")")
                        .score(score)
                        .weight(15.0)
                        .build());
            } else {
                inconsistencies.add("Extracted name does not match citizen profile name: '" + profile.getDisplayName() + "'");
                checks.add(VerificationCheckDetail.builder()
                        .check("NAME_MATCH")
                        .status("FAILED")
                        .message("Extracted name does not match citizen profile ('" + profile.getDisplayName() + "')")
                        .score(0.0)
                        .weight(15.0)
                        .build());
            }
        }

        // 2. Date of Birth Match (Weight: 10.0)
        String extractedDob = extractedFields != null && extractedFields.get("dateOfBirth") != null ?
                extractedFields.get("dateOfBirth").toString().trim() : null;
        Integer profileAge = profile.getAge();

        if (extractedDob == null) {
            checks.add(VerificationCheckDetail.builder()
                    .check("DOB_MATCH")
                    .status("NOT_CHECKED")
                    .message("Date of birth not detected in document")
                    .score(0.0)
                    .weight(10.0)
                    .build());
        } else {
            // Check year or date consistency
            boolean dobConsistent = false;
            String yearStr = extractYear(extractedDob);
            if (yearStr != null && profileAge != null && profileAge > 0) {
                int birthYear = Integer.parseInt(yearStr);
                int currentYear = LocalDate.now().getYear();
                int derivedAge = currentYear - birthYear;
                if (Math.abs(derivedAge - profileAge) <= 2) {
                    dobConsistent = true;
                }
            } else if (profile.getDob() != null) {
                dobConsistent = profile.getDob().toString().contains(yearStr != null ? yearStr : extractedDob);
            }

            if (dobConsistent) {
                double score = 10.0;
                totalScore += score;
                checks.add(VerificationCheckDetail.builder()
                        .check("DOB_MATCH")
                        .status("PASSED")
                        .message("Date of birth / age aligns with citizen profile")
                        .score(score)
                        .weight(10.0)
                        .build());
            } else if (profileAge == null && profile.getDob() == null) {
                checks.add(VerificationCheckDetail.builder()
                        .check("DOB_MATCH")
                        .status("NOT_CHECKED")
                        .message("Citizen profile does not specify age/DOB for comparison")
                        .score(0.0)
                        .weight(10.0)
                        .build());
            } else {
                inconsistencies.add("Extracted DOB (" + extractedDob + ") conflicts with citizen profile age (" + profileAge + ")");
                checks.add(VerificationCheckDetail.builder()
                        .check("DOB_MATCH")
                        .status("FAILED")
                        .message("Extracted DOB conflicts with profile record")
                        .score(0.0)
                        .weight(10.0)
                        .build());
            }
        }

        // 3. Residence / State Match (Weight: 5.0)
        String profileState = profile.getState() != null ? profile.getState().trim().toLowerCase() : "";
        if (profileState.isEmpty()) {
            checks.add(VerificationCheckDetail.builder()
                    .check("RESIDENCE_MATCH")
                    .status("NOT_CHECKED")
                    .message("Citizen profile state is not specified")
                    .score(0.0)
                    .weight(5.0)
                    .build());
        } else {
            String lowerRaw = rawText != null ? rawText.toLowerCase() : "";
            boolean stateFound = lowerRaw.contains(profileState);
            if (stateFound) {
                double score = 5.0;
                totalScore += score;
                checks.add(VerificationCheckDetail.builder()
                        .check("RESIDENCE_MATCH")
                        .status("PASSED")
                        .message("Document address/state confirms residency in " + profile.getState())
                        .score(score)
                        .weight(5.0)
                        .build());
            } else {
                checks.add(VerificationCheckDetail.builder()
                        .check("RESIDENCE_MATCH")
                        .status("NOT_CHECKED")
                        .message("State (" + profile.getState() + ") was not explicitly identified in document text")
                        .score(0.0)
                        .weight(5.0)
                        .build());
            }
        }

        return ConsistencyResult.builder()
                .totalConsistencyScore(totalScore)
                .checks(checks)
                .inconsistencies(inconsistencies)
                .build();
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int distance = computeLevenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return maxLen == 0 ? 1.0 : (1.0 - (double) distance / maxLen);
    }

    private int computeLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    private String extractYear(String dob) {
        if (dob == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(dob);
        return m.find() ? m.group(1) : null;
    }
}
