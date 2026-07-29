/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect } from "react";
import { mockSchemes as initialSchemes } from "../data/mockSchemes";

const SchemeContext = createContext(null);

export const useSchemes = () => {
  const context = useContext(SchemeContext);
  if (!context) {
    throw new Error("useSchemes must be used within a SchemeProvider");
  }
  return context;
};

const generateId = () => `SCHEME-${  Math.floor(10000 + Math.random() * 90000)}`;

export const SchemeProvider = ({ children }) => {
  const [schemes, setSchemes] = useState(() => {
    const saved = localStorage.getItem("schemebridge_schemes");
    return saved ? JSON.parse(saved) : initialSchemes;
  });

  // Persist schemes to localStorage
  useEffect(() => {
    localStorage.setItem("schemebridge_schemes", JSON.stringify(schemes));
  }, [schemes]);

  // ── Scheme Actions ──────────────────────────────────────────────────────

  const addScheme = (scheme) => {
    const now = new Date().toISOString().split("T")[0];
    const newScheme = {
      ...scheme,
      id: scheme.name.toLowerCase().replace(/[^a-z0-9]+/g, "-").slice(0, 30),
      status: scheme.status || "draft",
      lastUpdated: now,
      tags: scheme.tags || [],
    };
    setSchemes((prev) => [...prev, newScheme]);
  };

  const editScheme = (updatedScheme) => {
    const now = new Date().toISOString().split("T")[0];
    setSchemes((prev) =>
      prev.map((s) =>
        s.id === updatedScheme.id ? { ...updatedScheme, lastUpdated: now } : s
      )
    );
  };

  const deleteScheme = (schemeId) => {
    setSchemes((prev) => prev.filter((s) => s.id !== schemeId));
  };

  const getSchemeById = (schemeId) => {
    return schemes.find((s) => s.id === schemeId);
  };

  const getSchemesByCategory = (category) => {
    return schemes.filter((s) => s.category === category);
  };

  const searchSchemes = (query) => {
    if (!query) {
return schemes;
}
    const q = query.toLowerCase();
    return schemes.filter(
      (s) =>
        s.name?.toLowerCase().includes(q) ||
        s.ministry?.toLowerCase().includes(q) ||
        s.description?.toLowerCase().includes(q) ||
        s.category?.toLowerCase().includes(q)
    );
  };

  return (
    <SchemeContext.Provider
      value={{
        schemes,
        addScheme,
        editScheme,
        deleteScheme,
        getSchemeById,
        getSchemesByCategory,
        searchSchemes,
      }}
    >
      {children}
    </SchemeContext.Provider>
  );
};
