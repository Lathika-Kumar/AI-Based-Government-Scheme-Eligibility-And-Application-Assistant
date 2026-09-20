/**
 * @file api.js
 * @description API endpoint path constants for SchemeBridge.
 *
 * Base URL configuration:
 *   Auth Service  → VITE_AUTH_API_URL   (default: http://localhost:8080)
 *   Scheme Service → VITE_SCHEME_API_URL (default: http://localhost:8081)
 *
 * All paths below are relative to their respective service base.
 * Do NOT add a /v1 prefix — the backend does not use API versioning.
 *
 * Usage:
 *   import { AUTH_ENDPOINTS, SCHEME_ENDPOINTS } from "@config/api";
 *   schemeApi.get(SCHEME_ENDPOINTS.SEARCH, { params: { q: "farm" } })
 */

// ─── Auth Service Endpoints ──────────────────────────────────────────────────

export const AUTH_ENDPOINTS = Object.freeze({
  SIGNUP:     "/api/auth/signup",
  VERIFY_OTP: "/api/auth/verify-otp",
  LOGIN:      "/api/auth/login",
  REFRESH:    "/api/auth/refresh",
  LOGOUT:     "/api/auth/logout",
  ME:              "/api/auth/me",
  CHANGE_PASSWORD: "/api/auth/change-password",
});

// ─── Scheme Service Endpoints ────────────────────────────────────────────────

export const SCHEME_ENDPOINTS = Object.freeze({
  LIST:                "/api/schemes",
  BY_ID:               "/api/schemes/:id",
  BY_CODE:             "/api/schemes/code/:schemeCode",
  BY_CATEGORY:         "/api/schemes/category/:categoryCode",
  SEARCH:              "/api/schemes/search",
  ELIGIBILITY_BY_ID:   "/api/schemes/:id/eligibility/evaluate",
  ELIGIBILITY_BY_CODE: "/api/schemes/code/:schemeCode/eligibility/evaluate",
  ELIGIBILITY_ALL:     "/api/schemes/eligibility/evaluate-all",
  RECOMMENDATIONS:     "/api/schemes/recommendations",
});

export const APPLICATION_ENDPOINTS = Object.freeze({
  CREATE:   "/api/applications",
  MY:       "/api/applications/my",
  BY_ID:    "/api/applications/:applicationId",
  DOCUMENT: "/api/applications/:applicationId/documents/:documentCode",
  SUBMIT:   "/api/applications/:applicationId/submit",
  CANCEL:   "/api/applications/:applicationId/cancel",
  REAPPLY:  "/api/applications/:applicationId/reapply",
  TIMELINE: "/api/applications/:applicationId/timeline",
});

export const ADMIN_ENDPOINTS = Object.freeze({
  QUEUE:           "/api/admin/applications",
  START_REVIEW:    "/api/admin/applications/:applicationId/review/start",
  VERIFY_DOC:      "/api/admin/applications/:applicationId/documents/:documentCode/verify",
  REJECT_DOC:      "/api/admin/applications/:applicationId/documents/:documentCode/reject",
  REVIEW:          "/api/admin/applications/:applicationId/review",
  REQUEST_DOCS:    "/api/admin/applications/:applicationId/review/request-documents",
  APPROVE:         "/api/admin/applications/:applicationId/review/approve",
  REJECT:          "/api/admin/applications/:applicationId/review/reject",
});

export const PROFILE_ENDPOINTS = Object.freeze({
  GET:    "/api/profile",
  UPDATE: "/api/profile",
});

export const DASHBOARD_ENDPOINTS = Object.freeze({
  SUMMARY: "/api/dashboard/summary",
});

export const AI_ENDPOINTS = Object.freeze({
  CITIZEN_CHAT: "/api/ai/citizen/chat",
  ADMIN_CHAT:   "/api/ai/admin/chat",
});

export const FEEDBACK_ENDPOINTS = Object.freeze({
  SUBMIT: "/api/feedback",
  MY:     "/api/feedback/my",
  ADMIN:  "/api/admin/feedback",
});

export const DOCUMENT_ENDPOINTS = Object.freeze({
  LIST:           "/api/documents/vault",
  UPLOAD:         "/api/documents/vault/upload",
  BY_ID:          "/api/documents/vault/:documentId",
  DOWNLOAD:       "/api/documents/vault/:documentId/download",
  DELETE:         "/api/documents/vault/:documentId",
  VERIFY:         "/api/documents/:documentId/verify",
  EXTRACT:        "/api/documents/extract",
  OCR:            "/api/documents/ocr",
  VAULT_EXTRACT:  "/api/documents/vault/extract",
});

/**
 * Fill path parameter tokens.
 * @example
 *   buildPath(SCHEME_ENDPOINTS.BY_ID, { id: "abc123" })
 *   // → "/api/schemes/abc123"
 */
export function buildPath(template, params = {}) {
  let path = template;
  for (const [key, value] of Object.entries(params)) {
    path = path.replace(`:${key}`, encodeURIComponent(String(value)));
  }
  return path;
}

/** @deprecated Use buildPath instead */
export const buildUrl = buildPath;

// ─── Legacy & Global exports ──────────────────────────────────────────────────
export const ENDPOINTS = Object.freeze({
  ...AUTH_ENDPOINTS,
  AUTH: AUTH_ENDPOINTS,
  SCHEMES: SCHEME_ENDPOINTS,
  APPLICATIONS: APPLICATION_ENDPOINTS,
  ADMIN: ADMIN_ENDPOINTS,
  PROFILE: PROFILE_ENDPOINTS,
  DASHBOARD: DASHBOARD_ENDPOINTS,
  AI: AI_ENDPOINTS,
  FEEDBACK: FEEDBACK_ENDPOINTS,
  DOCUMENTS: DOCUMENT_ENDPOINTS,
});
