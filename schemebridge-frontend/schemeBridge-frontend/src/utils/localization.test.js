import { describe, it, expect } from "vitest";
import { resolveLocalized } from "./localization";
import { renderFormattedText } from "./textFormatter";

describe("Centralized Safe Localization Resolver (resolveLocalized)", () => {
  const sampleMultilingualObject = {
    english: "Pradhan Mantri Awas Yojana",
    tamil: "பிரதம மந்திரி ஆவாஸ் யோஜனா",
    translations: {
      hi: "प्रधानमंत्री आवास योजना",
      bn: "প্রধানমন্ত্রী আবাস যোজনা"
    }
  };

  it("1. English rendering: resolves english property when lang is 'en' or default", () => {
    expect(resolveLocalized(sampleMultilingualObject, "en")).toBe("Pradhan Mantri Awas Yojana");
    expect(resolveLocalized(sampleMultilingualObject)).toBe("Pradhan Mantri Awas Yojana");
    expect(resolveLocalized(sampleMultilingualObject, "english")).toBe("Pradhan Mantri Awas Yojana");
  });

  it("2. Active-language rendering: resolves tamil and nested hindi when active language matches", () => {
    expect(resolveLocalized(sampleMultilingualObject, "ta")).toBe("பிரதம மந்திரி ஆவாஸ் யோஜனா");
    expect(resolveLocalized(sampleMultilingualObject, "tamil")).toBe("பிரதம மந்திரி ஆவாஸ் யோஜனா");
    expect(resolveLocalized(sampleMultilingualObject, "hi")).toBe("प्रधानमंत्री आवास योजना");
    expect(resolveLocalized(sampleMultilingualObject, "bn")).toBe("প্রধানমন্ত্রী আবাস যোজনা");
  });

  it("3. English fallback: falls back to english when requested language is unavailable", () => {
    expect(resolveLocalized(sampleMultilingualObject, "fr")).toBe("Pradhan Mantri Awas Yojana");
    expect(resolveLocalized(sampleMultilingualObject, "unknown_lang")).toBe("Pradhan Mantri Awas Yojana");

    const noEnglishObject = {
      tamil: "மாநில உதவி",
      translations: {}
    };
    expect(resolveLocalized(noEnglishObject, "fr")).toBe("மாநில உதவி");
  });

  it("4. null/undefined handling: returns safe fallback string or custom placeholder", () => {
    expect(resolveLocalized(null)).toBe("");
    expect(resolveLocalized(undefined)).toBe("");
    expect(resolveLocalized(null, "en", "—")).toBe("—");
    expect(resolveLocalized(undefined, "ta", "Default Placeholder")).toBe("Default Placeholder");
  });

  it("5. Existing plain string and primitive rendering", () => {
    expect(resolveLocalized("Plain Scheme Name")).toBe("Plain Scheme Name");
    expect(resolveLocalized("")).toBe("");
    expect(resolveLocalized(12345)).toBe("12345");
    expect(resolveLocalized(true)).toBe("true");
  });

  it("6. Invariant: NEVER returns raw localization object as React child", () => {
    const problematicObject = {
      english: "Scholarship Support",
      tamil: "கல்வி உதவி",
      translations: {}
    };

    const result = resolveLocalized(problematicObject);
    expect(typeof result).toBe("string");
    expect(result).not.toBeInstanceOf(Object);
    expect(result).toBe("Scholarship Support");

    // Empty object
    const emptyResult = resolveLocalized({}, "en", "Fallback");
    expect(typeof emptyResult).toBe("string");
    expect(emptyResult).toBe("Fallback");
  });

  it("7. Integrates with renderFormattedText without throwing on object inputs", () => {
    const formatted = renderFormattedText(sampleMultilingualObject, "en");
    expect(typeof formatted).toBe("string");
    expect(formatted).toBe("Pradhan Mantri Awas Yojana");

    const formattedWithBreaks = renderFormattedText({
      english: "Line 1<br/>Line 2\nLine 3",
      tamil: "வரி 1<br/>வரி 2",
      translations: {}
    }, "en");
    expect(formattedWithBreaks).not.toBeNull();
  });
});
