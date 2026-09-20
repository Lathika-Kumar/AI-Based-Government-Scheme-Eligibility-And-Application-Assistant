import { describe, it, expect, vi } from "vitest";
import { parseDisplayToIso, formatIsoToDisplay, isValidCalendarDate } from "../../utils/dateUtils";
import { signup } from "../../services/authService";
import { authApi } from "../../utils/apiClient";

describe("Citizen Signup — Date of Birth Requirements & Flow", () => {
  it("rejects empty DOB during citizen signup validation", () => {
    const res = parseDisplayToIso("");
    expect(res.error).toBe("Date of birth is required");
    expect(res.iso).toBeNull();
  });

  it("validates that the date is a real calendar date (DD/MM/YYYY)", () => {
    const valid = parseDisplayToIso("22/03/2007");
    expect(valid.error).toBeNull();
    expect(valid.iso).toBe("2007-03-22");
    expect(isValidCalendarDate("22/03/2007")).toBe(true);
  });

  it("strictly rejects malformed or non-existent calendar dates", () => {
    // 31st of February does not exist
    const feb31 = parseDisplayToIso("31/02/2007");
    expect(feb31.error).toContain("Invalid date: Day 31 does not exist in month 2");
    expect(feb31.iso).toBeNull();
    expect(isValidCalendarDate("31/02/2007")).toBe(false);

    // 31st of April does not exist (April has 30 days)
    const apr31 = parseDisplayToIso("31/04/2005");
    expect(apr31.error).toContain("Day 31 does not exist in month 4");

    // Month 13 does not exist
    const m13 = parseDisplayToIso("15/13/2000");
    expect(m13.error).toContain("Month must be between 01 and 12");
  });

  it("strictly rejects future dates", () => {
    const futureDate = `15/08/${new Date().getFullYear() + 2}`;
    const res = parseDisplayToIso(futureDate);
    expect(res.error).toContain("cannot be in the future");
    expect(res.iso).toBeNull();
  });

  it("correctly handles leap years for 29 February", () => {
    // 2024 is a leap year -> valid
    const leap2024 = parseDisplayToIso("29/02/2024");
    expect(leap2024.error).toBeNull();
    expect(leap2024.iso).toBe("2024-02-29");

    // 2023 is not a leap year -> rejected
    const notLeap2023 = parseDisplayToIso("29/02/2023");
    expect(notLeap2023.error).toContain("Day 29 does not exist in month 2");
  });

  it("submits dob in ISO format through authService.signup", async () => {
    const postSpy = vi.spyOn(authApi, "post").mockResolvedValueOnce({
      data: { message: "OTP sent successfully" }
    });

    const citizenPayload = {
      firstName: "Lathika",
      lastName: "Kumar",
      email: "lathika@example.com",
      phoneNumber: "9876543210",
      password: "Password@123",
      dob: "2007-03-22"
    };

    const res = await signup(citizenPayload);
    expect(res.data?.message).toBe("OTP sent successfully");
    expect(postSpy).toHaveBeenCalledWith("/api/auth/signup", citizenPayload);

    postSpy.mockRestore();
  });
});
