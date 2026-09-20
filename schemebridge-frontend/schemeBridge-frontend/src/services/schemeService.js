/**
 * @file schemeService.js
 * @description Scheme discovery and eligibility service — connected to Scheme Service.
 *
 * Backend: http://localhost:8081
 *
 * Endpoints:
 *   GET  /api/schemes                        — list all ACTIVE schemes
 *   GET  /api/schemes/:id                    — scheme by MongoDB id
 *   GET  /api/schemes/code/:schemeCode        — scheme by schemeCode
 *   GET  /api/schemes/category/:categoryCode  — schemes by category
 *   GET  /api/schemes/search                  — search/filter with pagination
 *   POST /api/schemes/:id/eligibility/evaluate
 *   POST /api/schemes/code/:code/eligibility/evaluate
 *   POST /api/schemes/eligibility/evaluate-all
 *   POST /api/schemes/recommendations
 *
 * No mock fallback for production flows.
 * Network failures return { error: true, message } without fake data.
 */

import { schemeApi } from "@utils/apiClient";

// ─── Scheme Listing ──────────────────────────────────────────────────────────

/**
 * List all ACTIVE schemes (optionally filtered by categoryCode).
 * Returns array of SchemeResponse objects.
 */
export async function getSchemes({ categoryCode } = {}) {
  const params = {};
  if (categoryCode) params.categoryCode = categoryCode;
  return schemeApi.get("/api/schemes", { params });
}

/**
 * Get a single scheme by its MongoDB _id.
 */
export async function getSchemeById(id) {
  return schemeApi.get(`/api/schemes/${encodeURIComponent(id)}`);
}

/**
 * Get a single scheme by its schemeCode (e.g. "SCH-PMKISAN-001").
 */
export async function getSchemeByCode(schemeCode) {
  return schemeApi.get(`/api/schemes/code/${encodeURIComponent(schemeCode)}`);
}

/**
 * Get schemes by category code.
 */
export async function getSchemesByCategory(categoryCode) {
  return schemeApi.get(`/api/schemes/category/${encodeURIComponent(categoryCode)}`);
}

// ─── Search ──────────────────────────────────────────────────────────────────

/**
 * Search and filter schemes with pagination.
 * All parameters are optional.
 *
 * @param {{
 *   q?: string,
 *   categoryCode?: string,
 *   schemeLevel?: string,
 *   stateOrUt?: string,
 *   beneficiaryType?: string,
 *   schemeType?: string,
 *   tags?: string,
 *   page?: number,
 *   size?: number,
 *   sort?: string,
 *   direction?: string,
 * }} params
 *
 * @returns {Promise<{ data: PagedSchemeResponse } | { error, message }>}
 *   PagedSchemeResponse: { content, page, size, totalElements, totalPages }
 */
export async function searchSchemes(params = {}) {
  const qs = {};
  if (params.q)               qs.q              = params.q;
  if (params.categoryCode)    qs.categoryCode   = params.categoryCode;
  if (params.schemeLevel)     qs.schemeLevel    = params.schemeLevel;
  if (params.stateOrUt)       qs.stateOrUt      = params.stateOrUt;
  if (params.beneficiaryType) qs.beneficiaryType = params.beneficiaryType;
  if (params.schemeType)      qs.schemeType     = params.schemeType;
  if (params.status)          qs.status         = params.status;
  if (params.tags)            qs.tags           = params.tags;
  qs.page      = params.page      ?? 0;
  qs.size      = params.size      ?? 20;
  qs.sort      = params.sort      ?? "schemeCode";
  qs.direction = params.direction ?? "asc";

  return schemeApi.get("/api/schemes/search", { params: qs });
}

// ─── Eligibility ─────────────────────────────────────────────────────────────

/**
 * Evaluate a citizen profile against a specific scheme (by MongoDB id).
 *
 * @param {string} schemeId
 * @param {CitizenEligibilityProfile} profile
 *   { age, gender, annualIncome, occupation, state, socialCategory, disabilityStatus, attributes }
 *
 * @returns {Promise<{ data: EligibilityEvaluationResponse } | { error, message }>}
 *   EligibilityEvaluationResponse: { schemeId, schemeCode, status, matchedConditions,
 *                                    failedConditions, missingInformation, details }
 *   status: "ELIGIBLE" | "NOT_ELIGIBLE" | "INDETERMINATE"
 */
export async function evaluateEligibilityById(schemeId, profile) {
  return schemeApi.post(`/api/schemes/${encodeURIComponent(schemeId)}/eligibility/evaluate`, profile);
}

/**
 * Evaluate a citizen profile against a specific scheme (by schemeCode).
 */
export async function evaluateEligibilityByCode(schemeCode, profile) {
  return schemeApi.post(`/api/schemes/code/${encodeURIComponent(schemeCode)}/eligibility/evaluate`, profile);
}

/**
 * Evaluate a citizen profile against ALL active schemes.
 * Returns per-scheme results.
 *
 * @returns {Promise<{ data: BulkEligibilityEvaluationResponse } | { error, message }>}
 */
export async function evaluateEligibilityForAll(profile) {
  return schemeApi.post("/api/schemes/eligibility/evaluate-all", profile);
}

// ─── Recommendations ─────────────────────────────────────────────────────────

/**
 * Get personalised ranked scheme recommendations.
 * Requires authentication (Bearer token).
 *
 * NOTE: matchScore is the percentage of evaluated leaf conditions that passed.
 * It is NOT a probability of government approval or guaranteed eligibility.
 *
 * @param {CitizenEligibilityProfile} profile
 * @param {{ page?, size? }} pagination
 *
 * @returns {Promise<{ data: PersonalizedRecommendationResponse } | { error, message }>}
 *   PersonalizedRecommendationResponse: {
 *     totalSchemesEvaluated, eligibleCount, nearMatchCount, indeterminateCount,
 *     recommendations: RecommendationResponseItem[]
 *   }
 *   RecommendationResponseItem: {
 *     schemeId, schemeCode, slug, title, shortDescription, schemeLevel, stateOrUt,
 *     beneficiaryType, schemeType, matchScore, recommendationCategory, eligibilityStatus,
 *     matchedConditions, failedConditions, missingInformation, reasons, details
 *   }
 */
export async function getRecommendations(profile, { page = 0, size = 10 } = {}) {
  const safePage = Math.max(0, Number(page) || 0);
  const safeSize = Math.min(50, Math.max(1, Number(size) || 10));
  return schemeApi.post("/api/schemes/recommendations", profile, {
    params: { page: safePage, size: safeSize },
  });
}

/**
 * Get personalized recommendations for the authenticated citizen from /api/recommendations.
 * Evaluates the current user's profile against canonical schemes, strictly filtering out
 * NOT_ELIGIBLE and INSUFFICIENT_DATA before AI/ML ranking.
 */
export async function getPersonalizedRecommendations({ page = 0, size = 50 } = {}) {
  const safePage = Math.max(0, Number(page) || 0);
  const safeSize = Math.min(50, Math.max(1, Number(size) || 50));
  return schemeApi.get("/api/recommendations", {
    params: { page: safePage, size: safeSize },
  });
}

/**
 * Get scheme-specific document checklist, application steps, and verification requirements.
 */
export async function getSchemeChecklist(schemeCode) {
  return schemeApi.get(`/api/schemes/${encodeURIComponent(schemeCode)}/checklist`);
}

/**
 * Track a genuine citizen interaction event for recommendation learning.
 * @param {{ schemeCode: string, eventType: string, recommendationRank?: number, recommendationScore?: number, sessionId?: string, metadata?: object }} eventData
 */
export async function trackRecommendationEvent(eventData) {
  try {
    return await schemeApi.post("/api/recommendations/events", eventData);
  } catch (err) {
    console.debug("Telemetry event dispatch error:", err);
    return null;
  }
}

// ─── Circular Extraction & Scheme Creation (Admin) ──────────────────────────

/**
 * Upload government circular PDF to extract structured scheme draft via Gemini AI.
 * @param {File} file
 * @returns {Promise<{ data: SchemeDraftResponse } | { error, message }>}
 */
export async function extractCircularScheme(file) {
  const formData = new FormData();
  formData.append("file", file);
  return schemeApi.post("/api/schemes/extract-circular", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

/**
 * Create a new scheme (DRAFT or ACTIVE).
 * @param {Object} schemeData
 */
export async function createScheme(schemeData) {
  return schemeApi.post("/api/schemes", schemeData);
}

/**
 * Update an existing scheme.
 * @param {string} id
 * @param {Object} schemeData
 */
export async function updateScheme(id, schemeData) {
  return schemeApi.put(`/api/schemes/${encodeURIComponent(id)}`, schemeData);
}

/**
 * Update scheme status (e.g. DRAFT, ACTIVE, INACTIVE, ARCHIVED).
 * @param {string} id
 * @param {string} status
 */
export async function updateStatus(id, status) {
  return schemeApi.patch(`/api/schemes/${encodeURIComponent(id)}/status?status=${encodeURIComponent(status)}`);
}

/**
 * Delete an existing scheme by ID.
 * @param {string} id
 */
export async function deleteScheme(id) {
  return schemeApi.delete(`/api/schemes/${encodeURIComponent(id)}`);
}

const schemeService = {
  getSchemes,
  getSchemeById,
  getSchemeByCode,
  getSchemesByCategory,
  searchSchemes,
  evaluateEligibilityById,
  evaluateEligibilityByCode,
  evaluateEligibilityForAll,
  getRecommendations,
  getPersonalizedRecommendations,
  getSchemeChecklist,
  extractCircularScheme,
  createScheme,
  updateScheme,
  updateStatus,
  deleteScheme,
};

export default schemeService;

