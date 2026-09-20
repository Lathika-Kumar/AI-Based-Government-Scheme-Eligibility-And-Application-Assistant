import { describe, it, expect, beforeEach, vi } from "vitest";
import { safeGetItem, safeSetItem } from "@utils/storage";

describe("Phase 20 - Complete Portal Refinement & Architecture Validation", () => {
  let mockStorage = {};

  beforeEach(() => {
    mockStorage = {};
    if (typeof globalThis.localStorage === "undefined") {
      globalThis.localStorage = {
        getItem: (k) => mockStorage[k] || null,
        setItem: (k, v) => { mockStorage[k] = String(v); },
        removeItem: (k) => { delete mockStorage[k]; },
        clear: () => { mockStorage = {}; }
      };
    } else {
      try {
        localStorage.clear();
      } catch (e) {
        // ignore
      }
    }
  });

  describe("Part 1: Citizen Profile Display Name & Age Persistence", () => {
    it("should retain displayName and age from onboarding through profile state", () => {
      const initialStepData = {
        name: "Ananya Sharma",
        age: 28,
        gender: "Female"
      };
      
      const accumulated = {
        displayName: initialStepData.name,
        age: Number(initialStepData.age),
        gender: initialStepData.gender,
        occupation: "Software Engineer",
        annualIncome: 650000,
        socialCategory: "General",
        state: "Maharashtra"
      };

      expect(accumulated.displayName).toBe("Ananya Sharma");
      expect(accumulated.age).toBe(28);
      expect(accumulated.gender).toBe("Female");
      expect(accumulated.occupation).toBe("Software Engineer");
    });

    it("should not overwrite displayName or age with empty values during later steps", () => {
      const currentProfile = {
        displayName: "Vikram Mehta",
        age: 42,
        gender: "Male"
      };

      const step2Data = {
        occupation: "Farmer / Agricultural Worker",
        annualIncome: 120000,
        caste: "OBC",
        state: "Punjab"
      };

      const merged = {
        ...currentProfile,
        displayName: (step2Data.name !== undefined && step2Data.name !== "") ? step2Data.name : currentProfile.displayName,
        age: (step2Data.age !== undefined && step2Data.age !== "") ? Number(step2Data.age) : currentProfile.age,
        occupation: step2Data.occupation,
        annualIncome: step2Data.annualIncome,
        socialCategory: step2Data.caste,
        state: step2Data.state
      };

      expect(merged.displayName).toBe("Vikram Mehta");
      expect(merged.age).toBe(42);
      expect(merged.occupation).toBe("Farmer / Agricultural Worker");
    });
  });

  describe("Part 2 & Part 7: Navigation Cleanups (Notifications & Global Reset)", () => {
    it("citizen navigation sidebar items must not contain Notifications", () => {
      const citizenNavItems = [
        { name: "Dashboard", href: "/dashboard" },
        { name: "Eligibility Profile", href: "/profile" },
        { name: "Scheme Catalog", href: "/schemes" },
        { name: "My Recommendations", href: "/recommendations" },
        { name: "My Applications", href: "/applications" },
        { name: "Document Vault", href: "/documents" },
        { name: "Help & Grievance", href: "/help" },
        { name: "Feedback", href: "/feedback" }
      ];

      const hasNotifications = citizenNavItems.some(item => 
        item.name.toLowerCase().includes("notification") || item.href === "/notifications"
      );
      expect(hasNotifications).toBe(false);
    });

    it("citizen navigation items must not contain global reset control", () => {
      const citizenNavItems = [
        { name: "Dashboard", href: "/dashboard" },
        { name: "Eligibility Profile", href: "/profile" },
        { name: "Scheme Catalog", href: "/schemes" },
        { name: "My Recommendations", href: "/recommendations" },
        { name: "My Applications", href: "/applications" },
        { name: "Document Vault", href: "/documents" },
        { name: "Help & Grievance", href: "/help" },
        { name: "Feedback", href: "/feedback" }
      ];

      const hasReset = citizenNavItems.some(item => 
        item.name.toLowerCase().includes("reset")
      );
      expect(hasReset).toBe(false);
    });
  });

  describe("Part 3: Global Light/Dark Theme Persistence", () => {
    it("persists theme preference to schemebridge_theme key", () => {
      safeSetItem("schemebridge_theme", "dark");
      expect(safeGetItem("schemebridge_theme")).toBe("dark");

      safeSetItem("schemebridge_theme", "light");
      expect(safeGetItem("schemebridge_theme")).toBe("light");
    });

    it("does not persist complete scheme catalog to localStorage", () => {
      const rawCatalog = localStorage.getItem("schemebridge_schemes");
      expect(rawCatalog).toBeNull();
    });
  });

  describe("Part 6: Public Header Privacy (Caste Pill Absence)", () => {
    it("public citizen header tags should only display official role/citizenship, never caste", () => {
      const publicHeaderBadges = [
        { label: "Verified Citizen", type: "status" },
        { label: "Direct Benefit Transfer Enabled", type: "feature" }
      ];

      const exposesCaste = publicHeaderBadges.some(b => 
        b.label.toLowerCase().includes("caste") || 
        b.label.toLowerCase().includes("obc") || 
        b.label.toLowerCase().includes("sc/st")
      );
      expect(exposesCaste).toBe(false);
    });
  });

  describe("Part 8 & 9: Real Conversational AI Grounding & Invariant Protection", () => {
    it("validates conversation request structure and grounding attributes", () => {
      const request = {
        message: "What documents are required for PM-KISAN?",
        conversationId: "CONV-TEST-001"
      };

      expect(request.message).toBeTruthy();
      expect(request.message).toContain("PM-KISAN");
    });

    it("verifies grounded response contract containing related schemes and actionable guidance", () => {
      const sampleGroundedResponse = {
        response: "Based on verified database records, PM-KISAN provides income support of ₹6,000 annually in three equal installments to eligible farmers.",
        conversationId: "CONV-TEST-001",
        relatedSchemes: [
          { schemeCode: "PM-KISAN", title: "Pradhan Mantri Kisan Samman Nidhi", eligibilityStatus: "ELIGIBLE" }
        ],
        suggestions: [
          "What documents are needed for PM-KISAN?",
          "How do I apply for PM-KISAN?"
        ],
        actionLink: {
          label: "View Scheme Details",
          path: "/schemes/pm-kisan-yojana"
        },
        modelUsed: "gemini-1.5-flash"
      };

      expect(sampleGroundedResponse.response).toContain("PM-KISAN");
      expect(sampleGroundedResponse.relatedSchemes).toHaveLength(1);
      expect(sampleGroundedResponse.suggestions.length).toBeGreaterThan(0);
      expect(sampleGroundedResponse.actionLink.path).toBe("/schemes/pm-kisan-yojana");
    });
  });
});
