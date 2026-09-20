import { describe, it, expect } from "vitest";
import { normalizeName, isNameConsistent, compareApplicantName } from "./nameComparison";

describe("Name Comparison & Normalization Tests", () => {
  describe("normalizeName", () => {
    it("trims whitespace and converts to lowercase", () => {
      expect(normalizeName("  Lathika  ")).toBe("lathika");
      expect(normalizeName("LATHIKA KUMAR")).toBe("lathika kumar");
    });

    it("strips common Indian honorifics", () => {
      expect(normalizeName("Selvi Lathika")).toBe("lathika");
      expect(normalizeName("Thiru. Ramesh Kumar")).toBe("ramesh kumar");
      expect(normalizeName("Shri Aditya Kumar")).toBe("aditya kumar");
      expect(normalizeName("Smt. Priya Sharma")).toBe("priya sharma");
      expect(normalizeName("Dr. Rajesh Patel")).toBe("rajesh patel");
      expect(normalizeName("Mr. Suresh")).toBe("suresh");
      expect(normalizeName("Kumari Ananya")).toBe("ananya");
    });

    it("removes punctuation and collapses extra spacing", () => {
      expect(normalizeName("Lathika.K")).toBe("lathika k");
      expect(normalizeName("K. Lathika")).toBe("k lathika");
      expect(normalizeName("Lathika - Kumar")).toBe("lathika kumar");
      expect(normalizeName("  Priya   Sharma,  ")).toBe("priya sharma");
    });

    it("handles null or non-string input gracefully", () => {
      expect(normalizeName(null)).toBe("");
      expect(normalizeName(undefined)).toBe("");
      expect(normalizeName(123)).toBe("");
    });
  });

  describe("isNameConsistent", () => {
    it("returns true for exact matches", () => {
      expect(isNameConsistent("Lathika", "Lathika")).toBe(true);
      expect(isNameConsistent("lathika", "LATHIKA")).toBe(true);
      expect(isNameConsistent("Lathika Kumar", "Lathika Kumar")).toBe(true);
    });

    it("handles initials and abbreviations", () => {
      expect(isNameConsistent("Lathika.K", "Lathika")).toBe(true);
      expect(isNameConsistent("Lathika.K", "Lathika Kumar")).toBe(true);
      expect(isNameConsistent("Lathika K", "Lathika")).toBe(true);
      expect(isNameConsistent("K. Lathika", "Lathika")).toBe(true);
      expect(isNameConsistent("Lathika", "Lathika K")).toBe(true);
      expect(isNameConsistent("Nivetha N", "Nivetha")).toBe(true);
    });

    it("handles honorific prefixes in document holder name", () => {
      expect(isNameConsistent("Lathika.K", "Selvi Lathika")).toBe(true);
      expect(isNameConsistent("Lathika Kumar", "Selvi Lathika Kumar")).toBe(true);
      expect(isNameConsistent("Ramesh Kumar", "Thiru Ramesh Kumar")).toBe(true);
    });

    it("returns false for contradictory names (mismatches)", () => {
      expect(isNameConsistent("Lathika.K", "Aditya Kumar")).toBe(false);
      expect(isNameConsistent("Aditya Kumar", "Lathika")).toBe(false);
      expect(isNameConsistent("Priya Sharma", "Ramesh Kumar")).toBe(false);
      expect(isNameConsistent("Lathika Kumar", "Priya Kumar")).toBe(false);
      expect(isNameConsistent("Nivetha N", "Lathika")).toBe(false);
    });

    it("returns false for empty or null inputs", () => {
      expect(isNameConsistent("", "Lathika")).toBe(false);
      expect(isNameConsistent("Lathika", "")).toBe(false);
      expect(isNameConsistent(null, "Lathika")).toBe(false);
      expect(isNameConsistent("Lathika", null)).toBe(false);
    });
  });

  describe("compareApplicantName", () => {
    it("matches when applicant profile object has matching displayName", () => {
      const applicant = { displayName: "Lathika.K", email: "lathika@test.com" };
      const res = compareApplicantName(applicant, "Lathika");
      expect(res.isMatch).toBe(true);
      expect(res.status).toBe("MATCH");
      expect(res.applicantName).toBe("Lathika.K");
      expect(res.holderName).toBe("Lathika");
      expect(res.reason).toBeNull();
    });

    it("detects MISMATCH with clear explanation", () => {
      const applicant = { displayName: "Lathika.K" };
      const res = compareApplicantName(applicant, "Aditya Kumar");
      expect(res.isMatch).toBe(false);
      expect(res.status).toBe("MISMATCH");
      expect(res.applicantName).toBe("Lathika.K");
      expect(res.holderName).toBe("Aditya Kumar");
      expect(res.reason).toContain("Applicant Name Mismatch");
      expect(res.reason).toContain("Aditya Kumar");
      expect(res.reason).toContain("Lathika.K");
    });

    it("returns UNCERTAIN if extracted holder name is empty", () => {
      const applicant = { displayName: "Lathika.K" };
      const res = compareApplicantName(applicant, "");
      expect(res.isMatch).toBe(false);
      expect(res.status).toBe("UNCERTAIN");
      expect(res.reason).toContain("could not be detected");
    });
  });
});
