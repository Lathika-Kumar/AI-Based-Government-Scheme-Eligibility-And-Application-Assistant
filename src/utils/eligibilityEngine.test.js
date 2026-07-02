import { describe, it, expect, beforeEach } from "vitest";
import { checkEligibility } from "./eligibilityEngine";

describe("Eligibility Engine", () => {
  let mockProfile;
  let mockScheme;
  let mockDocuments;

  beforeEach(() => {
    // Reset mock data before each test
    mockProfile = {
      age: 32,
      annualIncome: 180000,
      occupation: "Farmer",
      caste: "OBC",
      gender: "Male",
      state: "Gujarat",
    };

    mockScheme = {
      id: "test-scheme",
      name: "Test Scheme",
      eligibility: {
        minAge: 18,
        maxAge: 60,
        maxIncome: 200000,
        states: ["Gujarat", "Maharashtra"],
        castes: ["OBC", "SC", "ST"],
        genders: ["Male", "Female"],
        occupations: ["Farmer", "Agricultural Worker"],
      },
      requiredDocuments: ["Aadhaar Card", "Income Certificate"],
    };

    mockDocuments = [
      { name: "Aadhaar Card", type: "Aadhaar Card", status: "verified" },
      { name: "Income Certificate", type: "Income Certificate", status: "verified" },
    ];
  });

  describe("Mandatory Criteria", () => {
    it("should return not_eligible when state does not match", () => {
      mockProfile.state = "Delhi";
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.status).toBe("not_eligible");
      expect(result.matchScore).toBe(0);
      expect(result.unmetConditions).toContain(
        "This scheme is available only for Gujarat, Maharashtra residents."
      );
    });

    it("should return not_eligible when age is below minimum", () => {
      mockProfile.age = 15;
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.status).toBe("not_eligible");
      expect(result.matchScore).toBe(0);
      expect(result.unmetConditions).toContain(
        "Your age (15 yrs) is below the minimum required age of 18 years."
      );
    });

    it("should return not_eligible when age is above maximum", () => {
      mockProfile.age = 65;
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.status).toBe("not_eligible");
      expect(result.matchScore).toBe(0);
      expect(result.unmetConditions).toContain(
        "Your age (65 yrs) exceeds the maximum cutoff of 60 years."
      );
    });

    it("should return not_eligible when caste does not match", () => {
      mockProfile.caste = "General";
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.status).toBe("not_eligible");
      expect(result.matchScore).toBe(0);
      expect(result.unmetConditions).toContain(
        'This scheme is reserved for [OBC, SC, ST] categories. Your profile is "General".'
      );
    });

    it("should return not_eligible when gender does not match", () => {
      mockScheme.eligibility.genders = ["Female"];
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.status).toBe("not_eligible");
      expect(result.matchScore).toBe(0);
      expect(result.unmetConditions).toContain(
        "This scheme is exclusive to Female applicants."
      );
    });

    it("should pass mandatory criteria when all match", () => {
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.status).not.toBe("not_eligible");
      expect(result.matchScore).toBeGreaterThan(0);
    });
  });

  describe("Weighted Scoring", () => {
    it("should give full score when all criteria match", () => {
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBe(100);
      expect(result.status).toBe("eligible");
    });

    it("should reduce score when income exceeds limit", () => {
      mockProfile.annualIncome = 250000;
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBeLessThan(100);
      expect(result.unmetConditions.some(c => c.includes("exceeds the scheme's limit"))).toBe(true);
    });

    it("should reduce score when occupation does not match", () => {
      mockProfile.occupation = "Engineer";
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBeLessThan(100);
      expect(result.unmetConditions.some(c => c.includes("targets"))).toBe(true);
    });

    it("should reduce score when documents are missing", () => {
      mockDocuments = [];
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBeLessThan(100);
    });

    it("should give full income weight when no income rule exists", () => {
      delete mockScheme.eligibility.maxIncome;
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBeGreaterThanOrEqual(65); // Base 40 + Income 25
    });

    it("should give full occupation weight when no occupation rule exists", () => {
      delete mockScheme.eligibility.occupations;
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBeGreaterThanOrEqual(60); // Base 40 + Occupation 20
    });
  });

  describe("Document Readiness", () => {
    it("should calculate document readiness score correctly", () => {
      mockDocuments = [
        { name: "Aadhaar Card", type: "Aadhaar Card", status: "verified" },
        { name: "Income Certificate", type: "Income Certificate", status: "verified" },
      ];
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBe(100);
    });

    it("should reduce score when documents are partially missing", () => {
      mockDocuments = [
        { name: "Aadhaar Card", type: "Aadhaar Card", status: "verified" },
      ];
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBeLessThan(100);
      expect(result.matchScore).toBeGreaterThan(80);
    });

    it("should reduce score when no documents are present", () => {
      mockDocuments = [];
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBeLessThan(100);
      expect(result.matchScore).toBeGreaterThan(60); // Base 40 + Income 25 + Occupation 20
    });
  });

  describe("Edge Cases", () => {
    it("should handle empty profile gracefully", () => {
      const result = checkEligibility({}, mockScheme, mockDocuments);

      expect(result).toBeDefined();
      expect(result.status).toBe("not_eligible");
    });

    it("should handle empty scheme gracefully", () => {
      const result = checkEligibility(mockProfile, {}, mockDocuments);

      expect(result).toBeDefined();
      expect(result.matchScore).toBeGreaterThanOrEqual(40); // Base score
    });

    it("should handle missing optional eligibility fields", () => {
      mockScheme.eligibility = {
        minAge: 18,
      };
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result).toBeDefined();
      expect(result.matchScore).toBeGreaterThan(0);
    });

    it("should handle zero income correctly", () => {
      mockProfile.annualIncome = 0;
      mockScheme.eligibility.maxIncome = 100000;
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBeGreaterThan(0);
      expect(result.qualifyingReasons.some(r => r.includes("within the ceiling"))).toBe(true);
    });

    it("should handle very high income correctly", () => {
      mockProfile.annualIncome = 10000000;
      mockScheme.eligibility.maxIncome = 200000;
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.matchScore).toBeLessThan(100);
      expect(result.unmetConditions.some(c => c.includes("exceeds the scheme's limit"))).toBe(true);
    });
  });

  describe("Status Determination", () => {
    it("should return eligible status when score is 100 and all conditions met", () => {
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.status).toBe("eligible");
      expect(result.matchScore).toBe(100);
    });

    it("should return possibly_eligible status when score is between 50-99", () => {
      mockProfile.annualIncome = 250000; // Exceeds limit
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.status).toBe("possibly_eligible");
      expect(result.matchScore).toBeGreaterThan(50);
      expect(result.matchScore).toBeLessThan(100);
    });

    it("should return not_eligible status when score is below 50", () => {
      mockProfile.annualIncome = 1000000; // Way exceeds limit
      mockProfile.occupation = "Engineer"; // Wrong occupation
      mockDocuments = []; // No documents
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.status).toBe("not_eligible");
      expect(result.matchScore).toBeLessThan(50);
    });
  });

  describe("Qualifying Reasons and Unmet Conditions", () => {
    it("should provide qualifying reasons for passed criteria", () => {
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.qualifyingReasons).toContain(
        "You reside in the eligible state of Gujarat."
      );
      expect(result.qualifyingReasons.some(r => r.includes("Your age"))).toBe(true);
    });

    it("should provide unmet conditions for failed criteria", () => {
      mockProfile.annualIncome = 250000;
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.unmetConditions.some(c => c.includes("exceeds the scheme's limit"))).toBe(true);
    });

    it("should track total and passed criteria count", () => {
      const result = checkEligibility(mockProfile, mockScheme, mockDocuments);

      expect(result.totalCriteria).toBe(6);
      expect(result.passedCriteria).toBeGreaterThan(0);
      expect(result.passedCriteria).toBeLessThanOrEqual(6);
    });
  });
});
