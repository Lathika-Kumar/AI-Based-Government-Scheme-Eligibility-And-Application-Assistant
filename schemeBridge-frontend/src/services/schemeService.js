/**
 * @file schemeService.js
 * @description Scheme service — backend-integrated with mock fallbacks.
 *
 * Set VITE_USE_MOCK_API=true in .env to force mock responses.
 * All functions fall back to mock logic if the API call throws.
 */

import { MOCK_LOADING_DELAY_MS, SCHEMES_PAGE_SIZE } from "../config/constants";
import { mockSchemes } from "../data/mockSchemes";
import {
  MOCK_SCHEME_RECOMMENDATIONS,
  getSchemeRecommendationDetails,
} from "../data/mockRecommendations";
import apiClient from "@utils/apiClient";
import { ENDPOINTS, buildUrl } from "@config/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";

const delay = (ms = MOCK_LOADING_DELAY_MS) =>
  new Promise((resolve) => setTimeout(resolve, ms));

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Fetch all schemes (optionally paginated).
 * Backend integration: GET /api/v1/schemes?page=0&size=12&category=&search=
 *
 * @param {{ page?: number, size?: number, category?: string, search?: string }} options
 * @returns {Promise<{ data: object[], total: number, page: number, totalPages: number }>}
 */
export async function getSchemes({ page = 0, size = SCHEMES_PAGE_SIZE, category = "", search = "" } = {}) {
  const fallbackMock = async () => {
    await delay();
    let filtered = [...mockSchemes];
    if (category) filtered = filtered.filter((s) => s.category === category);
    if (search) {
      const q = search.toLowerCase();
      filtered = filtered.filter(
        (s) =>
          s.name?.toLowerCase().includes(q) ||
          s.ministry?.toLowerCase().includes(q) ||
          s.description?.toLowerCase().includes(q)
      );
    }
    const total = filtered.length;
    const totalPages = Math.ceil(total / size);
    const start = page * size;
    const data = filtered.slice(start, start + size);
    return { data, total, page, totalPages };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(ENDPOINTS.SCHEMES.LIST, {
      params: { page, size, category, search },
    });
  } catch (error) {
    console.warn("getSchemes API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Fetch a single scheme by ID.
 * Backend integration: GET /api/v1/schemes/:id
 *
 * @param {string} id
 * @returns {Promise<object>} Scheme object
 */
export async function getSchemeById(id) {
  const fallbackMock = async () => {
    await delay(400);
    const scheme = mockSchemes.find((s) => s.id === id);
    if (!scheme) {
      const error = new Error(`Scheme with id "${id}" not found.`);
      error.status = 404;
      throw error;
    }
    return { ...scheme };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(buildUrl(ENDPOINTS.SCHEMES.DETAIL, { id }));
  } catch (error) {
    console.warn("getSchemeById API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Fetch all schemes (raw array — for client-side filtering in AppContext).
 * Backend integration: will become getSchemes() with paginated results.
 *
 * @returns {Promise<object[]>}
 */
export async function getAllSchemes() {
  await delay(600);
  return [...mockSchemes];
}

/**
 * Fetch AI-powered scheme recommendations for the current user's profile.
 * Backend integration: POST /api/v1/schemes/recommendations { profile }
 *
 * @param {object} [profile] - User profile for matching
 * @returns {Promise<object>} Recommendations map keyed by schemeId
 */
export async function getRecommendations(profile) {
  const fallbackMock = async () => {
    await delay(700);
    return { ...MOCK_SCHEME_RECOMMENDATIONS };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(ENDPOINTS.SCHEMES.RECOMMENDATIONS, { profile });
  } catch (error) {
    console.warn("getRecommendations API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Get recommendation details (opportunity score, AI badge, deadline) for a specific scheme.
 * Backend integration: GET /api/v1/schemes/:id/recommendation?userId=...
 *
 * @param {string} schemeId
 * @param {string} [schemeName]
 * @returns {Promise<object>}
 */
export async function getSchemeRecommendation(schemeId, schemeName = "") {
  await delay(200);
  return getSchemeRecommendationDetails(schemeId, schemeName);
}

/**
 * Check eligibility for a specific scheme against a user profile.
 * Backend integration: POST /api/v1/schemes/:id/eligibility { profile, documents }
 *
 * @param {string} schemeId
 * @param {object} profile
 * @param {object[]} documents
 * @returns {Promise<{ status: string, failedCriteria: string[], passedCriteria: string[] }>}
 */
export async function checkSchemeEligibility(schemeId, profile, documents) {
  const fallbackMock = async () => {
    await delay(500);
    const { checkEligibility } = await import("../utils/eligibilityEngine");
    const scheme = await getSchemeById(schemeId);
    return checkEligibility(profile, scheme, documents);
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(buildUrl(ENDPOINTS.SCHEMES.ELIGIBILITY, { id: schemeId }), {
      profile,
      documents,
    });
  } catch (error) {
    console.warn("checkSchemeEligibility API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Search schemes by text query.
 * Backend integration: GET /api/v1/schemes/search?q=...
 *
 * @param {string} query
 * @returns {Promise<object[]>}
 */
export async function searchSchemes(query) {
  const fallbackMock = async () => {
    await delay(400);
    if (!query?.trim()) return [...mockSchemes];
    const q = query.toLowerCase();
    return mockSchemes.filter(
      (s) =>
        s.name?.toLowerCase().includes(q) ||
        s.ministry?.toLowerCase().includes(q) ||
        s.description?.toLowerCase().includes(q) ||
        s.category?.toLowerCase().includes(q)
    );
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(ENDPOINTS.SCHEMES.SEARCH, { params: { q: query } });
  } catch (error) {
    console.warn("searchSchemes API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Fetch available scheme categories.
 * Backend integration: GET /api/v1/schemes/categories
 *
 * @returns {Promise<string[]>}
 */
export async function getCategories() {
  const fallbackMock = async () => {
    await delay(200);
    const categories = [...new Set(mockSchemes.map((s) => s.category).filter(Boolean))];
    return categories.sort();
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(ENDPOINTS.SCHEMES.CATEGORIES);
  } catch (error) {
    console.warn("getCategories API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

const schemeService = {
  getSchemes,
  getSchemeById,
  getAllSchemes,
  getRecommendations,
  getSchemeRecommendation,
  checkSchemeEligibility,
  searchSchemes,
  getCategories,
};

export default schemeService;
