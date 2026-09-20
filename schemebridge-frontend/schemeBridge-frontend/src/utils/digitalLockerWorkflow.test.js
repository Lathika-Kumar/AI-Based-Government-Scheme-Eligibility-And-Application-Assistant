import { describe, it, expect } from "vitest";
import { normalizeName, isNameConsistent, compareApplicantName } from "./nameComparison";
import { getDocumentFieldConfig } from "../config/documentFieldConfig";
import { extractDocumentData } from "../services/ocrService";

describe("Digital Locker Workflow — Name Comparison & Strict Mismatch Gate", () => {
  const profile = {
    name: "Lathika.K",
    displayName: "Lathika Kumar",
    email: "lathika@gmail.com",
  };

  it("normalizes names properly handling initials, spaces, and punctuation", () => {
    expect(normalizeName("  Lathika.K  ")).toBe("lathika k");
    expect(normalizeName("Thiru Dr. Lathika Kumar")).toBe("lathika kumar");
    expect(normalizeName("LATHIKA   KUMAR")).toBe("lathika kumar");
  });

  it("confirms applicant name match on valid variations", () => {
    expect(compareApplicantName(profile, "Lathika.K").status).toBe("MATCH");
    expect(compareApplicantName(profile, "Lathika K").status).toBe("MATCH");
    expect(compareApplicantName(profile, "K Lathika").status).toBe("MATCH");
    expect(compareApplicantName(profile, "Lathika Kumar").status).toBe("MATCH");
    expect(compareApplicantName(profile, "Selvi Lathika Kumar").status).toBe("MATCH");
  });

  it("strictly flags MISMATCH when document belongs to another person", () => {
    const mismatch1 = compareApplicantName(profile, "Aditya Kumar");
    expect(mismatch1.status).toBe("MISMATCH");
    expect(mismatch1.isMatch).toBe(false);
    expect(mismatch1.reason).toContain("does not match logged-in applicant profile");

    const mismatch2 = compareApplicantName(profile, "Ramesh Sharma");
    expect(mismatch2.status).toBe("MISMATCH");
    expect(mismatch2.isMatch).toBe(false);

    const mismatch3 = compareApplicantName(profile, "Priya Nair");
    expect(mismatch3.status).toBe("MISMATCH");
    expect(mismatch3.isMatch).toBe(false);
  });
});

describe("Digital Locker Workflow — Dynamic Field Rendering per Document Type", () => {
  it("Aadhaar Card renders holderName, DOB, Aadhaar number, gender, address, issuing authority, expiryDate", () => {
    const config = getDocumentFieldConfig("Aadhaar Card");
    expect(config.fields).toEqual([
      "holderName",
      "dateOfBirth",
      "documentNumber",
      "gender",
      "address",
      "issuingAuthority",
      "expiryDate",
    ]);
    expect(config.requiresDob).toBe(true);
    expect(config.defaultExpiry).toBe("No Expiration");
    expect(config.defaultIssuer).toContain("UIDAI");
  });

  it("Caste / Community Certificate renders holderName, community, certificate number, issuing authority, issue/validity dates without forcing expiry", () => {
    const config = getDocumentFieldConfig("Community Certificate");
    expect(config.fields).toEqual([
      "holderName",
      "community",
      "documentNumber",
      "issuingAuthority",
      "issueDate",
      "expiryDate",
    ]);
    expect(config.requiresDob).toBe(false);
    expect(config.defaultExpiry).toBe("No Expiration");
  });

  it("Domicile Certificate renders holderName, address, certificate number, issuing authority, issue/validity dates without forcing expiry", () => {
    const config = getDocumentFieldConfig("Domicile Certificate");
    expect(config.fields).toEqual([
      "holderName",
      "address",
      "documentNumber",
      "issuingAuthority",
      "issueDate",
      "expiryDate",
    ]);
    expect(config.requiresDob).toBe(false);
    expect(config.defaultExpiry).toBe("No Expiration");
  });

  it("Income Certificate renders holderName, annual income, certificate number, issuing authority, dates", () => {
    const config = getDocumentFieldConfig("Income Certificate");
    expect(config.fields).toEqual([
      "holderName",
      "annualIncome",
      "documentNumber",
      "issuingAuthority",
      "issueDate",
      "expiryDate",
    ]);
    expect(config.requiresDob).toBe(false);
    expect(config.defaultExpiry).toBe("Valid for 1 Year");
  });

  it("Business / Occupation Certificate renders holderName, businessOccupation, registration number, issuing authority, dates", () => {
    const config = getDocumentFieldConfig("Business / Occupation Certificate");
    expect(config.fields).toEqual([
      "holderName",
      "businessOccupation",
      "documentNumber",
      "issuingAuthority",
      "issueDate",
      "expiryDate",
    ]);
    expect(config.requiresDob).toBe(false);
  });
});

describe("Digital Locker Workflow — Status Separation & Accuracy", () => {
  it("keeps OCR confidence, classification confidence, and applicant-name matching distinct", async () => {
    const ocrResult = await extractDocumentData({ fileName: "aadhaar_card.pdf" });
    expect(ocrResult.success).toBe(true);
    expect(ocrResult.data.ocrConfidence).toBeDefined();
    expect(ocrResult.data.classificationConfidence).toBeDefined();
    expect(ocrResult.data.confidenceScore).toBeDefined();

    // Name match evaluated against profile of matching citizen (Rajesh Patel)
    const matchForRajesh = compareApplicantName({ name: "Rajesh Patel" }, ocrResult.data.holderName);
    expect(matchForRajesh.status).toBe("MATCH");

    // Strict mismatch evaluated against different citizen (Lathika.K)
    const mismatchForLathika = compareApplicantName({ name: "Lathika.K" }, ocrResult.data.holderName);
    expect(mismatchForLathika.status).toBe("MISMATCH");

    // Check terminology & dynamic attributes
    expect(ocrResult.data.documentType).toBe("Aadhaar Card");
    expect(ocrResult.data.holderName).toBe("Rajesh Patel");
    expect(ocrResult.data.gender).toBe("Male");
    expect(ocrResult.data.address).toBeDefined();
  });
});
