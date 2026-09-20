/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useCallback } from "react";
import { safeGetItem, safeSetItem } from "@utils/storage";

const ThemeContext = createContext(null);

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme must be used within a ThemeProvider");
  }
  return context;
};

/**
 * Resolves the effective theme ("light" or "dark") from the stored preference.
 * When preference is "system", uses the OS media query.
 */
function resolveEffectiveTheme(preference) {
  if (preference === "system") {
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }
  return preference === "dark" ? "dark" : "light";
}

export const ThemeProvider = ({ children }) => {
  const [themePreference, setThemePreference] = useState(() => {
    return safeGetItem("schemebridge_theme", "light");
  });

  const applyTheme = useCallback((preference) => {
    const effective = resolveEffectiveTheme(preference);
    const root = document.documentElement;
    if (effective === "dark") {
      root.classList.add("dark");
    } else {
      root.classList.remove("dark");
    }
  }, []);

  // Apply on mount and when preference changes
  useEffect(() => {
    applyTheme(themePreference);
    safeSetItem("schemebridge_theme", themePreference);
  }, [themePreference, applyTheme]);

  // Listen for OS theme changes when in "system" mode
  useEffect(() => {
    if (themePreference !== "system") return;
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = () => applyTheme("system");
    mq.addEventListener("change", handler);
    return () => mq.removeEventListener("change", handler);
  }, [themePreference, applyTheme]);

  // Legacy toggle: cycles light ↔ dark (used by existing header buttons)
  const toggleTheme = () => {
    setThemePreference((prev) => {
      const effective = resolveEffectiveTheme(prev);
      return effective === "dark" ? "light" : "dark";
    });
  };

  // Explicit setter for Settings page: "system" | "light" | "dark"
  const setTheme = (mode) => {
    if (["system", "light", "dark"].includes(mode)) {
      setThemePreference(mode);
    }
  };

  const isDark = resolveEffectiveTheme(themePreference) === "dark";

  return (
    <ThemeContext.Provider value={{ theme: themePreference, toggleTheme, setTheme, isDark }}>
      {children}
    </ThemeContext.Provider>
  );
};

export default ThemeContext;
