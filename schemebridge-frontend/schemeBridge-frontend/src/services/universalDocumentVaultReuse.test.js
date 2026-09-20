import { describe, it, expect } from "vitest";
import {
  getDocReadinessForScheme,
  findMatchingVaultDocument,
  isVaultDocValidAndReusable,
} from "../utils/documentReadiness";

describe("Universal Document Vault Reuse & Readiness Validation", () => {
  const validAadhaar = {
    id: "vault_1",
    documentCode: "DOC_AADHAAR",
    documentName: "Aadhaar Card",
    name: "Aadhaar Card",
    holderName: "Lathika K",
    detailedStatus: "AI_VERIFIED",
    verificationStatus: "PENDING",
    status: "verified",
    verificationScore: 96,
    identityMatchStatus: "MATCH",
    verified: false,
    expiryDate: "2030-12-31T23:59:59Z",
  };

  const validIncome = {
    id: "vault_2",
    documentCode: "DOC_INCOME",
    documentName: "Income Certificate",
    name: "Income Certificate",
    holderName: "Lathika K",
    detailedStatus: "AI_VERIFIED",
    verificationStatus: "PENDING",
    status: "verified",
    verificationScore: 92,
    identityMatchStatus: "MATCH",
    verified: false,
  };

  const validCommunity = {
    id: "vault_3",
    documentCode: "DOC_CASTE",
    documentName: "Community Certificate",
    name: "Community Certificate",
    holderName: "Lathika K",
    detailedStatus: "AI_VERIFIED",
    verificationStatus: "PENDING",
    status: "verified",
    verificationScore: 95,
    identityMatchStatus: "MATCH",
    verified: false,
  };

  const mismatchDoc = {
    id: "vault_4",
    documentCode: "DOC_AADHAAR",
    documentName: "Aadhaar Card",
    name: "Aadhaar Card",
    holderName: "Ramesh Kumar",
    detailedStatus: "AI_VERIFIED",
    verificationStatus: "PENDING",
    status: "uploaded",
    verificationScore: 90,
    identityMatchStatus: "MISMATCH",
  };

  const uncertainDoc = {
    id: "vault_5",
    documentCode: "DOC_AADHAAR",
    documentName: "Aadhaar Card",
    name: "Aadhaar Card",
    detailedStatus: "AI_VERIFIED",
    verificationStatus: "PENDING",
    status: "uploaded",
    verificationScore: 70,
    identityMatchStatus: "UNCERTAIN",
  };

  const rejectedDoc = {
    id: "vault_6",
    documentCode: "DOC_INCOME",
    documentName: "Income Certificate",
    name: "Income Certificate",
    detailedStatus: "AI_REJECTED",
    verificationStatus: "REJECTED",
    status: "REJECTED",
    verificationScore: 20,
    identityMatchStatus: "MATCH",
  };

  const expiredDoc = {
    id: "vault_7",
    documentCode: "DOC_INCOME",
    documentName: "Income Certificate",
    name: "Income Certificate",
    detailedStatus: "AI_VERIFIED",
    verificationStatus: "PENDING",
    status: "verified",
    verificationScore: 90,
    identityMatchStatus: "MATCH",
    expiryDate: "2020-01-01T00:00:00Z", // Past date
  };

  it("1. Successfully matches and reuses valid Aadhaar Card", () => {
    const match = findMatchingVaultDocument(["Aadhaar Card"], [validAadhaar]);
    expect(match).not.toBeNull();
    expect(match.documentCode).toBe("DOC_AADHAAR");

    const readiness = getDocReadinessForScheme(["Aadhaar Card"], [validAadhaar]);
    expect(readiness.readinessScore).toBe(100);
    expect(readiness.readinessLabel).toBe("Ready");
    expect(readiness.totalAvailable).toBe(1);
    expect(readiness.evaluatedItems[0].isAvailable).toBe(true);
  });

  it("2. Successfully matches Income Certificate and Community Certificate", () => {
    const vault = [validIncome, validCommunity];
    const requirements = ["Income Certificate", "Caste Certificate"];
    const readiness = getDocReadinessForScheme(requirements, vault);

    expect(readiness.readinessScore).toBe(100);
    expect(readiness.availableCount).toBe(2);
    expect(readiness.missingCount).toBe(0);
  });

  it("3. Disqualifies vault documents with MISMATCH identity status", () => {
    expect(isVaultDocValidAndReusable(mismatchDoc)).toBe(false);

    const match = findMatchingVaultDocument(["Aadhaar Card"], [mismatchDoc]);
    expect(match).toBeNull();

    const readiness = getDocReadinessForScheme(["Aadhaar Card"], [mismatchDoc]);
    expect(readiness.readinessScore).toBe(0);
    expect(readiness.missingCount).toBe(1);
    expect(readiness.evaluatedItems[0].isAvailable).toBe(false);
  });

  it("4. Disqualifies vault documents with UNCERTAIN identity status", () => {
    expect(isVaultDocValidAndReusable(uncertainDoc)).toBe(false);

    const match = findMatchingVaultDocument(["Aadhaar Card"], [uncertainDoc]);
    expect(match).toBeNull();

    const readiness = getDocReadinessForScheme(["Aadhaar Card"], [uncertainDoc]);
    expect(readiness.readinessScore).toBe(0);
  });

  it("5. Disqualifies rejected documents", () => {
    expect(isVaultDocValidAndReusable(rejectedDoc)).toBe(false);

    const match = findMatchingVaultDocument(["Income Certificate"], [rejectedDoc]);
    expect(match).toBeNull();

    const readiness = getDocReadinessForScheme(["Income Certificate"], [rejectedDoc]);
    expect(readiness.readinessScore).toBe(0);
  });

  it("6. Disqualifies expired documents", () => {
    expect(isVaultDocValidAndReusable(expiredDoc)).toBe(false);

    const match = findMatchingVaultDocument(["Income Certificate"], [expiredDoc]);
    expect(match).toBeNull();

    const readiness = getDocReadinessForScheme(["Income Certificate"], [expiredDoc]);
    expect(readiness.readinessScore).toBe(0);
  });

  it("7. Accurately calculates partial readiness when some documents are missing", () => {
    const vault = [validAadhaar]; // Only Aadhaar in vault
    const requirements = ["Aadhaar Card", "Income Certificate"]; // Requires both
    const readiness = getDocReadinessForScheme(requirements, vault);

    expect(readiness.readinessScore).toBe(50);
    expect(readiness.readinessLabel).toBe("Partially Ready");
    expect(readiness.availableCount).toBe(1);
    expect(readiness.missingCount).toBe(1);
    expect(readiness.availableDocs).toContain("Aadhaar Card");
    expect(readiness.missingDocs).toContain("Income Certificate");
  });

  it("8. Resolves ONE_OF alternative groups when any valid alternative exists in vault", () => {
    const vault = [validCommunity]; // Community Certificate in vault
    const req = {
      type: "ONE_OF",
      documentName: "Category Certificate",
      options: ["Caste Certificate", "Community Certificate", "Tribe Certificate"],
    };

    const readiness = getDocReadinessForScheme([req], vault);
    expect(readiness.readinessScore).toBe(100);
    expect(readiness.totalAvailable).toBe(1);
    expect(readiness.evaluatedItems[0].isAvailable).toBe(true);
  });
});
