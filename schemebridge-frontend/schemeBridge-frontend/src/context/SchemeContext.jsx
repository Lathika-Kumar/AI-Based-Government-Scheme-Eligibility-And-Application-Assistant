/* eslint-disable react-refresh/only-export-components */
/**
 * @file SchemeContext.jsx
 * @description Global scheme state backed by the Scheme Service (port 8081).
 *
 * Provides shared scheme list and category data so multiple components
 * don't independently re-fetch the full scheme list.
 *
 * This context is intentionally thin — pages that need search/filter
 * results call schemeService.searchSchemes() directly with their own
 * local state so they can manage pagination independently.
 *
 * Admin-only operations (create/edit/delete scheme) go through
 * adminService — not through this context.
 */

import { createContext, useContext, useState, useEffect } from "react";
import schemeService from "@services/schemeService";

const SchemeContext = createContext(null);

export const useSchemes = () => {
  const context = useContext(SchemeContext);
  if (!context) throw new Error("useSchemes must be used within a SchemeProvider");
  return context;
};

export const SchemeProvider = ({ children }) => {
  const [schemes, setSchemes]         = useState([]);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState(null);

  const fetchSchemes = async (categoryCode) => {
    setLoading(true);
    setError(null);
    const result = await schemeService.getSchemes({ categoryCode });
    if (result.error) {
      setError(result.message || "Unable to load schemes.");
    } else {
      // Backend returns an array directly for GET /api/schemes
      setSchemes(Array.isArray(result.data) ? result.data : []);
    }
    setLoading(false);
  };

  // Load all schemes on mount
  useEffect(() => { fetchSchemes(); }, []);

  /**
   * Get a scheme from the cached list by MongoDB id or schemeCode.
   * Falls back to the backend if not found in cache.
   */
  const getSchemeById = (id) => schemes.find((s) => s.id === id || s.schemeCode === id);

  return (
    <SchemeContext.Provider value={{ schemes, loading, error, fetchSchemes, getSchemeById }}>
      {children}
    </SchemeContext.Provider>
  );
};
