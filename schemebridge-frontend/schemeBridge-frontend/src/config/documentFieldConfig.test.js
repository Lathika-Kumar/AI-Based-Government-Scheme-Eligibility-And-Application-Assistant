import { describe, it, expect, vi } from "vitest";
import {
  DOCUMENT_TYPE_FIELD_CONFIG,
  getDocumentFieldConfig,
} from "./documentFieldConfig";
import { formatIsoToDisplay, parseDisplayToIso, isValidCalendarDate } from "../utils/dateUtils";

describe("Document Field Configuration & Document-Specific Extraction Form", () => {
  it("Aadhaar Card configuration requires holderName, dateOfBirth, documentNumber, gender, address, issuingAuthority, expiryDate", () => {
    const config = getDocumentFieldConfig("Aadhaar Card");
    expect(config.code).toBe("AADHAAR");
    expect(config.requiresDob).toBe(true);
    expect(config.fields).toEqual(["holderName", "dateOfBirth", "documentNumber", "gender", "address", "issuingAuthority", "expiryDate"]);
    expect(config.labels.holderName).toBe("Document Holder Name");
    expect(config.labels.dateOfBirth).toBe("Date of Birth / Year of Birth");
    expect(config.labels.documentNumber).toBe("Aadhaar Number");
    expect(config.defaultExpiry).toBe("No Expiration");
  });

  it("Income Certificate configuration includes holderName, annualIncome, documentNumber, issuingAuthority, issueDate, expiryDate", () => {
    const config = getDocumentFieldConfig("Income Certificate");
    expect(config.code).toBe("INCOME_CERTIFICATE");
    expect(config.requiresDob).toBe(false);
    expect(config.fields).toEqual(["holderName", "annualIncome", "documentNumber", "issuingAuthority", "issueDate", "expiryDate"]);
    expect(config.fields.includes("dateOfBirth")).toBe(false);
    expect(config.labels.annualIncome).toBe("Annual Family Income (₹)");
  });

  it("Caste and Community Certificate configurations include holderName, community, certificate number, issuing authority, dates", () => {
    const casteConfig = getDocumentFieldConfig("Caste Certificate");
    expect(casteConfig.code).toBe("CASTE_CERTIFICATE");
    expect(casteConfig.requiresDob).toBe(false);
    expect(casteConfig.fields).toEqual(["holderName", "community", "documentNumber", "issuingAuthority", "issueDate", "expiryDate"]);
    expect(casteConfig.defaultExpiry).toBe("No Expiration");

    const commConfig = getDocumentFieldConfig("Community Certificate");
    expect(commConfig.code).toBe("COMMUNITY_CERTIFICATE");
    expect(commConfig.fields).toContain("community");
    expect(commConfig.defaultExpiry).toBe("No Expiration");
  });

  it("Domicile Certificate configuration includes holderName, address, certificate number, issuing authority, dates", () => {
    const config = getDocumentFieldConfig("Domicile Certificate");
    expect(config.code).toBe("DOMICILE_CERTIFICATE");
    expect(config.requiresDob).toBe(false);
    expect(config.fields).toEqual(["holderName", "address", "documentNumber", "issuingAuthority", "issueDate", "expiryDate"]);
    expect(config.defaultExpiry).toBe("No Expiration");
  });

  it("Business / Occupation Certificate configuration includes holderName, businessOccupation, registration number, issuing authority, dates", () => {
    const config = getDocumentFieldConfig("Business Certificate");
    expect(config.code).toBe("BUSINESS_CERTIFICATE");
    expect(config.requiresDob).toBe(false);
    expect(config.fields).toEqual(["holderName", "businessOccupation", "documentNumber", "issuingAuthority", "issueDate", "expiryDate"]);
  });

  it("resolves various document code naming variations correctly", () => {
    expect(getDocumentFieldConfig("AADHAR").code).toBe("AADHAAR");
    expect(getDocumentFieldConfig("UIDAI").code).toBe("AADHAAR");
    expect(getDocumentFieldConfig("income_cert").code).toBe("INCOME_CERTIFICATE");
    expect(getDocumentFieldConfig("caste_cert").code).toBe("CASTE_CERTIFICATE");
    expect(getDocumentFieldConfig("domicile_cert").code).toBe("DOMICILE_CERTIFICATE");
    expect(getDocumentFieldConfig("msme_cert").code).toBe("BUSINESS_CERTIFICATE");
    expect(getDocumentFieldConfig("PAN").code).toBe("PAN");
    expect(getDocumentFieldConfig("unknown_doc").code).toBe("DEFAULT");
  });
});

describe("Date Utilities — Strict Calendar Validation for Citizen Onboarding", () => {
  it("formats ISO date YYYY-MM-DD to DD/MM/YYYY", () => {
    expect(formatIsoToDisplay("2007-03-22")).toBe("22/03/2007");
    expect(formatIsoToDisplay("1995-12-05")).toBe("05/12/1995");
  });

  it("validates real calendar dates and returns ISO string", () => {
    const res = parseDisplayToIso("22/03/2007");
    expect(res.error).toBeNull();
    expect(res.iso).toBe("2007-03-22");
    expect(res.calculatedAge).toBeGreaterThan(15);
  });

  it("rejects non-existent calendar dates (e.g. 31/02/2007 or leap year violations)", () => {
    const res1 = parseDisplayToIso("31/02/2007");
    expect(res1.error).toContain("Invalid date: Day 31 does not exist in month 2");
    expect(res1.iso).toBeNull();

    const res2 = parseDisplayToIso("29/02/2021"); // 2021 is not a leap year
    expect(res2.error).toContain("Day 29 does not exist in month 2");

    const res3 = parseDisplayToIso("29/02/2020"); // 2020 is a leap year
    expect(res3.error).toBeNull();
    expect(res3.iso).toBe("2020-02-29");
  });

  it("rejects future dates", () => {
    const futureYear = new Date().getFullYear() + 2;
    const res = parseDisplayToIso(`01/01/${futureYear}`);
    expect(res.error).toContain("cannot be in the future");
    expect(res.iso).toBeNull();
  });

  it("rejects empty or malformed strings", () => {
    expect(parseDisplayToIso("").error).toContain("Date of birth is required");
    expect(parseDisplayToIso("invalid-date").error).toContain("valid date");
  });
});
