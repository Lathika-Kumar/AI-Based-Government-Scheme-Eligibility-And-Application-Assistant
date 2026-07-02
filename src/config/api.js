/**
 * @file api.js
 * @description API endpoint configuration for SchemeBridge backend integration.
 *
 * Backend integration instructions:
 * 1. Set VITE_API_BASE_URL in .env files (e.g., VITE_API_BASE_URL=http://localhost:8080/api)
 * 2. All services import ENDPOINTS from this file — no hard-coded URLs anywhere else.
 * 3. buildUrl() constructs parameterised paths (e.g., /schemes/:id → /schemes/pm-kisan).
 */

/** Base API URL from environment variable; falls back to localhost for dev. */
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

/** API version prefix */
export const API_VERSION = "v1";

/** Full versioned API base (e.g., http://localhost:8080/api/v1) */
export const API_BASE = `${API_BASE_URL}/${API_VERSION}`;

/**
 * All REST endpoint path templates.
 * Use buildUrl() to fill in path parameters.
 */
export const ENDPOINTS = Object.freeze({
  // ── Auth ──────────────────────────────────────────────────────────────────
  AUTH: {
    LOGIN: "/auth/login",
    LOGOUT: "/auth/logout",
    REGISTER: "/auth/register",
    REFRESH_TOKEN: "/auth/refresh",
    VERIFY_OTP: "/auth/verify-otp",
    FORGOT_PASSWORD: "/auth/forgot-password",
    RESET_PASSWORD: "/auth/reset-password",
  },

  // ── Profile ───────────────────────────────────────────────────────────────
  PROFILE: {
    GET: "/profile",
    UPDATE: "/profile",
    COMPLETION: "/profile/completion",
    PREFERENCES: "/profile/preferences",
  },

  // ── Schemes ───────────────────────────────────────────────────────────────
  SCHEMES: {
    LIST: "/schemes",
    DETAIL: "/schemes/:id",
    RECOMMENDATIONS: "/schemes/recommendations",
    CATEGORIES: "/schemes/categories",
    SEARCH: "/schemes/search",
    ELIGIBILITY: "/schemes/:id/eligibility",
  },

  // ── Documents ─────────────────────────────────────────────────────────────
  DOCUMENTS: {
    LIST: "/documents",
    UPLOAD: "/documents",
    DELETE: "/documents/:id",
    VERIFY: "/documents/:id/verify",
    DOWNLOAD: "/documents/:id/download",
    DIGILOCKER_SYNC: "/documents/digilocker/sync",
  },

  // ── Applications ──────────────────────────────────────────────────────────
  APPLICATIONS: {
    LIST: "/applications",
    SUBMIT: "/applications",
    DETAIL: "/applications/:id",
    WITHDRAW: "/applications/:id/withdraw",
    SAVED_SCHEMES: "/applications/saved",
    SAVE_SCHEME: "/applications/saved/:schemeId",
    UNSAVE_SCHEME: "/applications/saved/:schemeId",
    TIMELINE: "/applications/:id/timeline",
  },

  // ── Notifications ─────────────────────────────────────────────────────────
  NOTIFICATIONS: {
    LIST: "/notifications",
    MARK_READ: "/notifications/:id/read",
    MARK_ALL_READ: "/notifications/read-all",
    DELETE: "/notifications/:id",
    PREFERENCES: "/notifications/preferences",
    UNREAD_COUNT: "/notifications/unread-count",
  },

  // ── AI / SchemeAI ─────────────────────────────────────────────────────────
  AI: {
    ELIGIBILITY_ANALYSIS: "/ai/eligibility",
    DAILY_BRIEF: "/ai/daily-brief",
    CHAT: "/ai/chat",
    SCHEME_INSIGHTS: "/ai/scheme/:id/insights",
    DOCUMENT_SUGGESTIONS: "/ai/document-suggestions",
  },

  // ── Admin ─────────────────────────────────────────────────────────────────
  ADMIN: {
    DASHBOARD_STATS: "/admin/stats",
    USERS_LIST: "/admin/users",
    AUDIT_LOGS: "/admin/audit",
    GRIEVANCES: "/admin/grievances",
    GRIEVANCE_RESOLVE: "/admin/grievances/:id/resolve",
    SCHEME_CREATE: "/admin/schemes",
    SCHEME_UPDATE: "/admin/schemes/:id",
    SCHEME_DELETE: "/admin/schemes/:id",
    SCHEME_PUBLISH: "/admin/schemes/:id/publish",
    APPLICATION_REVIEW: "/admin/applications/:id/review",
  },

  // ── Config / Meta ─────────────────────────────────────────────────────────
  CONFIG: {
    LANGUAGES: "/config/languages",
    CATEGORIES: "/config/categories",
    STATES: "/config/states",
    FEATURE_FLAGS: "/config/features",
  },
});

/**
 * Builds a full API URL with path parameters substituted.
 *
 * @example
 * buildUrl(ENDPOINTS.SCHEMES.DETAIL, { id: "pm-kisan" })
 * // → "http://localhost:8080/api/v1/schemes/pm-kisan"
 *
 * @param {string} path - Endpoint path (may contain :param tokens)
 * @param {Record<string, string>} [params={}] - Path parameter values
 * @returns {string} Full URL
 */
export function buildUrl(path, params = {}) {
  let resolvedPath = path;
  for (const [key, value] of Object.entries(params)) {
    resolvedPath = resolvedPath.replace(`:${key}`, encodeURIComponent(value));
  }
  return `${API_BASE}${resolvedPath}`;
}

/**
 * Default HTTP request headers.
 * Backend integration: attach Authorization Bearer token here.
 * @param {string|null} [token]
 * @returns {Record<string, string>}
 */
export function getDefaultHeaders(token = null) {
  const headers = {
    "Content-Type": "application/json",
    Accept: "application/json",
    "X-App-Version": "1.2.0",
    "X-Client": "SchemeBridge-Web",
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  return headers;
}
