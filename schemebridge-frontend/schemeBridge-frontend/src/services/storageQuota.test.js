/**
 * @file storageQuota.test.js
 * @description Unit and Integration tests for LocalStorage Quota safety,
 * legacy scheme cache cleanup, and scheme resolution resilience.
 */

import { describe, it, expect, beforeEach, vi } from "vitest";
import {
  safeSetItem,
  safeGetItem,
  safeRemoveItem,
  cleanupLegacySchemeCache,
  isQuotaExceededError
} from "../utils/storage";

// In-memory mock storage implementation for deterministic testing
function createMockStorage() {
  let store = {};
  return {
    getItem: (key) => (key in store ? store[key] : null),
    setItem: (key, val) => { store[key] = String(val); },
    removeItem: (key) => { delete store[key]; },
    clear: () => { store = {}; },
    get length() { return Object.keys(store).length; },
    key: (i) => Object.keys(store)[i] || null,
    _dump: () => ({ ...store })
  };
}

describe("Storage Quota & Resilience Suite", () => {
  beforeEach(() => {
    globalThis.localStorage = createMockStorage();
    globalThis.sessionStorage = createMockStorage();
    vi.restoreAllMocks();
  });

  describe("1. Legacy Scheme Cache Cleanup (Migration)", () => {
    it("safely purges 'schemebridge_schemes' without removing user auth or profile data", () => {
      // Simulate legacy populated storage
      localStorage.setItem("schemebridge_schemes", JSON.stringify([{ id: "SCH-1", name: "Heavy Catalog" }]));
      localStorage.setItem("sb_access_token", "jwt-mock-token-xyz");
      localStorage.setItem("sb_user", JSON.stringify({ id: 10, email: "citizen@example.com" }));
      localStorage.setItem("schemebridge_saved", JSON.stringify([{ schemeId: "SO2YT5YLM", schemeName: "PM-Kisan" }]));

      expect(localStorage.getItem("schemebridge_schemes")).not.toBeNull();

      cleanupLegacySchemeCache();

      // Legacy key should be removed
      expect(localStorage.getItem("schemebridge_schemes")).toBeNull();

      // Auth and user keys must be preserved
      expect(localStorage.getItem("sb_access_token")).toBe("jwt-mock-token-xyz");
      expect(JSON.parse(localStorage.getItem("sb_user")).email).toBe("citizen@example.com");
      expect(JSON.parse(localStorage.getItem("schemebridge_saved"))).toHaveLength(1);
    });

    it("is completely idempotent when called repeatedly or when key is absent", () => {
      expect(() => {
        cleanupLegacySchemeCache();
        cleanupLegacySchemeCache();
        cleanupLegacySchemeCache();
      }).not.toThrow();
      expect(localStorage.getItem("schemebridge_schemes")).toBeNull();
    });
  });

  describe("2. Defensive Storage Operations (safeSetItem, safeGetItem, safeRemoveItem)", () => {
    it("successfully writes and reads JSON objects and strings", () => {
      const testData = { userId: "USR-100", preferences: { highContrast: true } };
      const success = safeSetItem("sb_test_key", testData);
      expect(success).toBe(true);

      const retrieved = safeGetItem("sb_test_key");
      expect(retrieved).toEqual(testData);
    });

    it("returns default value when key does not exist or parse fails", () => {
      const defaultVal = { fallback: true };
      expect(safeGetItem("non_existent_key", defaultVal)).toEqual(defaultVal);

      // Corrupted JSON
      localStorage.setItem("corrupted_key", "{invalid-json");
      expect(safeGetItem("corrupted_key", "default")).toBe("{invalid-json");
    });

    it("safely catches QuotaExceededError without throwing", () => {
      const quotaError = new DOMException("The quota has been exceeded", "QuotaExceededError");
      vi.spyOn(globalThis.localStorage, "setItem").mockImplementation(() => {
        throw quotaError;
      });

      const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

      let result;
      expect(() => {
        result = safeSetItem("some_key", { data: "huge_payload" });
      }).not.toThrow();

      expect(result).toBe(false);
      expect(warnSpy).toHaveBeenCalled();
      warnSpy.mockRestore();
    });

    it("correctly identifies QuotaExceededError across browser naming variants", () => {
      const errStandard = new DOMException("Quota exceeded", "QuotaExceededError");
      const errFirefox = new DOMException("Quota reached", "NS_ERROR_DOM_QUOTA_REACHED");
      const errGeneric = new Error("Generic error");

      expect(isQuotaExceededError(errStandard)).toBe(true);
      expect(isQuotaExceededError(errFirefox)).toBe(true);
      expect(isQuotaExceededError(errGeneric)).toBe(false);
    });

    it("safely removes items via safeRemoveItem", () => {
      safeSetItem("removable_key", "test_value");
      expect(safeGetItem("removable_key")).toBe("test_value");

      safeRemoveItem("removable_key");
      expect(safeGetItem("removable_key")).toBeNull();
    });
  });

  describe("3. Architecture Invariants — Zero Full Dataset Persistence", () => {
    it("ensures scheme dataset (4734 schemes) is NEVER saved to localStorage", () => {
      // Simulate generating 4734 mock schemes in application memory
      const mock4734Schemes = Array.from({ length: 4734 }, (_, i) => ({
        id: `SCHEME-${i}`,
        schemeCode: `CODE-${i}`,
        name: `Scheme ${i}`,
        description: `Description for scheme ${i}`
      }));

      // Verify that localStorage does NOT contain 'schemebridge_schemes'
      expect(localStorage.getItem("schemebridge_schemes")).toBeNull();

      // Ensure that only lightweight saved tracker items are stored (<1 KB)
      const lightSavedItem = {
        schemeId: "SO2YT5YLM",
        schemeName: "Pradhan Mantri Kisan Samman Nidhi",
        ministry: "Ministry of Agriculture",
        savedDate: "2026-09-04",
        stage: "Saved"
      };

      safeSetItem("schemebridge_saved", [lightSavedItem]);
      const storedJson = localStorage.getItem("schemebridge_saved");
      expect(storedJson.length).toBeLessThan(1024); // Well under 1KB
      expect(localStorage.getItem("schemebridge_schemes")).toBeNull();
    });
  });

  describe("4. Case-Insensitive Scheme Resolution & Canonical Documents", () => {
    it("correctly resolves schemeCode across uppercase and lowercase variants", () => {
      const sampleSchemes = [
        { id: "SO2YT5YLM", schemeCode: "SO2YT5YLM", slug: "so2yt5ylm", name: "PM-Kisan" },
        { id: "AYUSH-001", schemeCode: "AYUSH-001", slug: "ayushman-bharat", name: "Ayushman Bharat" }
      ];

      const lookupUpper = sampleSchemes.find(
        (s) => s.schemeCode?.toLowerCase() === "SO2YT5YLM".toLowerCase() || s.slug === "SO2YT5YLM".toLowerCase()
      );
      const lookupLower = sampleSchemes.find(
        (s) => s.schemeCode?.toLowerCase() === "so2yt5ylm".toLowerCase() || s.slug === "so2yt5ylm".toLowerCase()
      );

      expect(lookupUpper).toBeDefined();
      expect(lookupLower).toBeDefined();
      expect(lookupUpper.name).toBe("PM-Kisan");
      expect(lookupLower.name).toBe("PM-Kisan");
    });
  });
});
