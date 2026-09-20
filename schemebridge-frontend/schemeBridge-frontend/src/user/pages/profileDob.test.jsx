import { describe, it, expect } from "vitest";
import { formatIsoToDisplay, parseDisplayToIso } from "./Profile";

describe("Profile DOB Parsing and Calendar Validation", () => {
  it("formats ISO date string to DD/MM/YYYY display format", () => {
    expect(formatIsoToDisplay("2007-03-22")).toBe("22/03/2007");
    expect(formatIsoToDisplay("2005-08-14")).toBe("14/08/2005");
    expect(formatIsoToDisplay("1990-05-15T00:00:00.000Z")).toBe("15/05/1990");
    expect(formatIsoToDisplay("")).toBe("");
    expect(formatIsoToDisplay(null)).toBe("");
  });

  it("accepts valid calendar date in DD/MM/YYYY and calculates exact age", () => {
    const res = parseDisplayToIso("22/03/2007");
    expect(res.error).toBeNull();
    expect(res.iso).toBe("2007-03-22");
    expect(res.calculatedAge).toBeGreaterThanOrEqual(18);
    expect(res.calculatedAge).toBeLessThanOrEqual(20);
  });

  it("handles leap year dates correctly (e.g. 29/02/2004)", () => {
    const res = parseDisplayToIso("29/02/2004");
    expect(res.error).toBeNull();
    expect(res.iso).toBe("2004-02-29");
  });

  it("strictly rejects non-leap year 29 Feb without silent conversion (e.g. 29/02/2023)", () => {
    const res = parseDisplayToIso("29/02/2023");
    expect(res.error).toBeTruthy();
    expect(res.error).toContain("does not exist in month 2");
    expect(res.iso).toBeNull();
  });

  it("strictly rejects invalid day for 30-day months (e.g. 31/04/2000)", () => {
    const res = parseDisplayToIso("31/04/2000");
    expect(res.error).toBeTruthy();
    expect(res.error).toContain("does not exist in month 4");
    expect(res.iso).toBeNull();
  });

  it("strictly rejects invalid months (e.g. 14/13/2005)", () => {
    const res = parseDisplayToIso("14/13/2005");
    expect(res.error).toBeTruthy();
    expect(res.error).toContain("Month must be between 01 and 12");
  });

  it("rejects future dates", () => {
    const nextYear = new Date().getFullYear() + 2;
    const res = parseDisplayToIso(`15/08/${nextYear}`);
    expect(res.error).toBeTruthy();
  });

  it("rejects empty or missing DOB", () => {
    const res = parseDisplayToIso("");
    expect(res.error).toBe("Date of birth is required");
    expect(res.iso).toBeNull();
  });

  it("rejects non-date gibberish", () => {
    const res = parseDisplayToIso("not-a-date");
    expect(res.error).toContain("valid date in DD/MM/YYYY");
  });
});
