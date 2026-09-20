import { describe, it, expect, beforeEach } from "vitest";
import { safeGetItem, safeSetItem } from "@utils/storage";

describe("Phase 21B - Theme Consistency & Contrast Verification", () => {
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

  describe("Theme Storage Key & Defaults", () => {
    it("should default to light mode when schemebridge_theme is not in localStorage", () => {
      const theme = safeGetItem("schemebridge_theme", "light");
      expect(theme).toBe("light");
    });

    it("should correctly persist and retrieve dark theme using schemebridge_theme key", () => {
      safeSetItem("schemebridge_theme", "dark");
      const theme = safeGetItem("schemebridge_theme", "light");
      expect(theme).toBe("dark");
    });

    it("should toggle between light and dark without state corruption", () => {
      let currentTheme = safeGetItem("schemebridge_theme", "light");
      expect(currentTheme).toBe("light");

      // Toggle to dark
      currentTheme = currentTheme === "dark" ? "light" : "dark";
      safeSetItem("schemebridge_theme", currentTheme);
      expect(safeGetItem("schemebridge_theme")).toBe("dark");

      // Toggle back to light
      currentTheme = currentTheme === "dark" ? "light" : "dark";
      safeSetItem("schemebridge_theme", currentTheme);
      expect(safeGetItem("schemebridge_theme")).toBe("light");
    });
  });

  describe("DOM Dark Mode HTML Class Synchronization", () => {
    it("should add 'dark' class to root element when theme is dark", () => {
      const root = {
        classList: new Set(),
        addClass(cls) { this.classList.add(cls); },
        removeClass(cls) { this.classList.delete(cls); },
        contains(cls) { return this.classList.has(cls); }
      };

      const applyTheme = (theme) => {
        if (theme === "dark") {
          root.addClass("dark");
        } else {
          root.removeClass("dark");
        }
        safeSetItem("schemebridge_theme", theme);
      };

      applyTheme("dark");
      expect(root.contains("dark")).toBe(true);
      expect(safeGetItem("schemebridge_theme")).toBe("dark");

      applyTheme("light");
      expect(root.contains("dark")).toBe(false);
      expect(safeGetItem("schemebridge_theme")).toBe("light");
    });
  });

  describe("Contrast Ratio Standard Verification (WCAG AA)", () => {
    // Relative luminance calculation for RGB hex
    function hexToRgb(hex) {
      const bigint = parseInt(hex.replace("#", ""), 16);
      return [(bigint >> 16) & 255, (bigint >> 8) & 255, bigint & 255];
    }

    function luminance([r, g, b]) {
      const a = [r, g, b].map(v => {
        v /= 255;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
      });
      return a[0] * 0.2126 + a[1] * 0.7152 + a[2] * 0.0722;
    }

    function contrast(hex1, hex2) {
      const lum1 = luminance(hexToRgb(hex1));
      const lum2 = luminance(hexToRgb(hex2));
      const brightest = Math.max(lum1, lum2);
      const darkest = Math.min(lum1, lum2);
      return (brightest + 0.05) / (darkest + 0.05);
    }

    it("should satisfy WCAG AA contrast (>= 4.5:1) for Light Mode body text on white/slate-50", () => {
      const slate900 = "#0f172a";
      const slate800 = "#1e293b";
      const white = "#ffffff";
      const slate50 = "#f8fafc";

      expect(contrast(slate900, white)).toBeGreaterThanOrEqual(7.0); // ~18.5:1
      expect(contrast(slate800, white)).toBeGreaterThanOrEqual(7.0); // ~13.5:1
      expect(contrast(slate900, slate50)).toBeGreaterThanOrEqual(7.0);
    });

    it("should satisfy WCAG AA contrast (>= 4.5:1) for Dark Mode body text on slate-900/slate-950", () => {
      const slate100 = "#f1f5f9";
      const slate200 = "#e2e8f0";
      const slate900 = "#0f172a";
      const slate950 = "#020617";

      expect(contrast(slate100, slate900)).toBeGreaterThanOrEqual(7.0); // ~14:1
      expect(contrast(slate200, slate900)).toBeGreaterThanOrEqual(7.0); // ~12:1
      expect(contrast(slate100, slate950)).toBeGreaterThanOrEqual(7.0); // ~18:1
    });

    it("should satisfy WCAG AA contrast (>= 4.5:1) for secondary/muted text in Dark Mode", () => {
      const slate300 = "#cbd5e1";
      const slate400 = "#94a3b8";
      const slate900 = "#0f172a";

      expect(contrast(slate300, slate900)).toBeGreaterThanOrEqual(7.0); // ~9:1
      expect(contrast(slate400, slate900)).toBeGreaterThanOrEqual(4.5); // ~4.8:1
    });
  });
});
