import { describe, it, expect, vi } from "vitest";
import ocrService, { extractDocumentData, crossVerifyWithProfile } from "./ocrService";

describe("ocrService — AI OCR Data Extraction & Cross-Verification", () => {
  it("extracts Aadhaar card data with high confidence score", async () => {
    const result = await extractDocumentData({ type: "aadhaar", fileName: "my_aadhaar.pdf" });
    
    expect(result.success).toBe(true);
    expect(result.data.documentType).toBe("Aadhaar Card");
    expect(result.data.documentNumber).toBe("5482-9102-4589");
    expect(result.data.confidenceScore).toBeGreaterThanOrEqual(0.9);
    expect(result.data.isVerified).toBe(true);
  });

  it("extracts PAN card data correctly", async () => {
    const result = await extractDocumentData({ type: "pan", fileName: "pan_scan.jpg" });

    expect(result.success).toBe(true);
    expect(result.data.documentType).toBe("PAN Card");
    expect(result.data.documentNumber).toBe("BKPPS4589F");
  });

  it("extracts Income Certificate attributes including annual income", async () => {
    const result = await extractDocumentData({ type: "income_cert" });

    expect(result.success).toBe(true);
    expect(result.data.annualIncome).toBe(180000);
    expect(result.data.documentNumber).toContain("UP/INC/");
  });

  it("crossVerifies matching OCR data with profile correctly", () => {
    const ocrData = {
      extractedName: "Rajesh Kumar Sharma",
      category: "OBC",
      annualIncome: 180000,
    };

    const profileData = {
      fullName: "Rajesh Kumar Sharma",
      category: "OBC",
      annualIncome: 180000,
    };

    const verification = crossVerifyWithProfile(ocrData, profileData);

    expect(verification.isMatch).toBe(true);
    expect(verification.matchScore).toBe(100);
    expect(verification.discrepancies).toHaveLength(0);
  });

  it("identifies discrepancies when OCR data differs from profile", () => {
    const ocrData = {
      extractedName: "Rajesh Kumar Sharma",
      category: "General",
      annualIncome: 350000,
    };

    const profileData = {
      fullName: "Rajesh Kumar Sharma",
      category: "OBC",
      annualIncome: 180000,
    };

    const verification = crossVerifyWithProfile(ocrData, profileData);

    expect(verification.isMatch).toBe(false);
    expect(verification.discrepancies.length).toBeGreaterThan(0);
    expect(verification.matchScore).toBeLessThan(100);
  });

  it("handles null/undefined values gracefully", () => {
    const verification = crossVerifyWithProfile(null, null);
    expect(verification.isMatch).toBe(false);
    expect(verification.matchScore).toBe(0);
  });
});
