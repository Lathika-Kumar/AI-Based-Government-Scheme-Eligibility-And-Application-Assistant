package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent citizen eligibility profile stored in MongoDB.
 *
 * Architecture & Relationship to Oracle:
 *   - userId = Oracle USERS.id (String matching JWT sub claim).
 *   - No passwords, credentials, or roles are stored here.
 *   - Identity remains in Oracle USERS; rich demographic eligibility attributes
 *     and document verification provenance are managed in this collection.
 *
 * Collection: citizen_profiles
 * Index: userId (unique)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "citizen_profiles")
public class CitizenProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String displayName;

    /** Date of birth (provenance from Aadhaar / Birth proof). */
    private LocalDate dob;

    /** Age in years (calculated or declared). */
    private Integer age;

    /** "Male", "Female", "Other", "Prefer not to say" */
    private String gender;

    /** "Single", "Married", "Widowed", "Divorced" */
    private String maritalStatus;

    /** Indian state or UT of residence. */
    private String state;

    /** District of residence. */
    private String district;

    /** 6-digit postal PIN code. */
    private String pincode;

    /** "RURAL", "URBAN", "SEMI_URBAN" */
    private String residentialAreaType;

    /** Annual household income in INR. */
    private Double annualIncome;

    /** Whether household is Below Poverty Line (BPL). */
    private Boolean bplStatus;

    /** Ration card type: "AAY", "PHH", "NPHH", "NONE" */
    private String rationCardType;

    /**
     * Occupation category:
     *   Farmer, Student, Salaried Employee, Self-Employed, Daily Wage Labour,
     *   Unemployed, Homemaker, Retired, Artisan, Weaver, Other.
     */
    private String occupation;

    /** Detailed employment status: "EMPLOYED", "UNEMPLOYED", "SELF_EMPLOYED", "STUDENT" */
    private String employmentStatus;

    /** Whether citizen is an active farmer / agriculturalist. */
    private Boolean isFarmer;

    /** Cultivable landholding area in hectares. */
    private Double landholdingArea;

    /** Whether citizen is an enrolled student. */
    private Boolean isStudent;

    /**
     * Social category for scheme eligibility:
     *   "General", "OBC", "SC", "ST", "EWS"
     */
    private String socialCategory;

    /** Whether citizen belongs to a notified religious/linguistic minority. */
    private Boolean minorityStatus;

    /** Education level: "Below 10th", "10th Pass", "12th Pass", "Graduate", "Post Graduate", "Doctorate", "Vocational" */
    private String education;

    /** Whether citizen has a certified disability. */
    private Boolean disabilityStatus;

    /** Type of disability (e.g., "Locomotor", "Visual", "Hearing", "Multiple") */
    private String disabilityType;

    /** Percentage of benchmark disability (e.g. 40, 60, 80). */
    private Integer disabilityPercentage;

    /** Unique Disability ID (UDID) card number. */
    private String udidNumber;

    /**
     * Map of attribute verification states and provenance.
     * Keys: "annualIncome", "socialCategory", "dob", "disabilityStatus", "landholdingArea", etc.
     */
    @Builder.Default
    private Map<String, VerifiedAttribute<?>> verifiedAttributes = new HashMap<>();

    /** Authoritative onboarding completion flag. */
    @Builder.Default
    private Boolean onboardingComplete = false;

    /** Onboarding lifecycle state: "NOT_STARTED", "IN_PROGRESS", "COMPLETE". */
    @Builder.Default
    private String onboardingStatus = "NOT_STARTED";

    /** Last active onboarding step (1–3). */
    @Builder.Default
    private Integer onboardingStep = 1;

    /** UI Accessibility preferences. */
    @Builder.Default
    private Map<String, String> accessibilityPreferences = new HashMap<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
