/**
 * @file constants.js
 * @description Application-wide constants: limits, magic numbers, and feature toggles.
 * Re-exports env.js CONFIG and adds frontend-specific constants.
 *
 * Backend integration: some values (e.g., MAX_DOCUMENTS) may become dynamic
 * from /api/v1/config/features once the backend is live.
 */

export { CONFIG } from "./env.js";

// ── Pagination ────────────────────────────────────────────────────────────────

/** Default page size for list APIs */
export const DEFAULT_PAGE_SIZE = 20;

/** Scheme listing page size */
export const SCHEMES_PAGE_SIZE = 12;

/** Notification listing page size */
export const NOTIFICATIONS_PAGE_SIZE = 15;

/** Admin listing page size */
export const ADMIN_PAGE_SIZE = 25;

// ── Documents ─────────────────────────────────────────────────────────────────

/** Maximum number of documents a citizen can store in their vault */
export const MAX_DOCUMENTS = 50;

/** Maximum file size for document upload (in bytes) — 5 MB */
export const MAX_UPLOAD_SIZE_BYTES = 5 * 1024 * 1024;

/** Maximum file size label for display */
export const MAX_UPLOAD_SIZE_LABEL = "5 MB";

/** Accepted document MIME types */
export const ACCEPTED_DOC_TYPES = [
  "application/pdf",
  "image/jpeg",
  "image/jpg",
  "image/png",
  "image/webp",
];

/** Accepted document type extensions (for display) */
export const ACCEPTED_DOC_EXTENSIONS = ".pdf, .jpg, .jpeg, .png, .webp";

// ── Profile ───────────────────────────────────────────────────────────────────

/** Minimum age for scheme eligibility */
export const MIN_ELIGIBLE_AGE = 18;

/** Maximum age for senior citizen schemes */
export const MAX_SENIOR_AGE = 100;

/** Minimum income value (₹0 for students/unemployed) */
export const MIN_ANNUAL_INCOME = 0;

/** Maximum income value for display purposes (₹50 lakh) */
export const MAX_ANNUAL_INCOME = 5000000;

// ── UI / UX ───────────────────────────────────────────────────────────────────

/** Simulated loading delay for mock service calls (ms) */
export const MOCK_LOADING_DELAY_MS = 800;

/** Short UI debounce delay for search inputs (ms) */
export const SEARCH_DEBOUNCE_MS = 300;

/** Toast notification auto-dismiss delay (ms) */
export const TOAST_DISMISS_MS = 4000;

/** Skeleton loader fade-in/out transition (ms) */
export const SKELETON_TRANSITION_MS = 300;

// ── Applications ──────────────────────────────────────────────────────────────

/** Maximum saved schemes per citizen */
export const MAX_SAVED_SCHEMES = 20;

/** Maximum active applications per citizen */
export const MAX_ACTIVE_APPLICATIONS = 10;

// ── Eligibility ───────────────────────────────────────────────────────────────

/** Score threshold above which a scheme shows as "High Match" */
export const HIGH_MATCH_SCORE_THRESHOLD = 85;

/** Score threshold for "Moderate Match" */
export const MODERATE_MATCH_SCORE_THRESHOLD = 65;

// ── Localization ──────────────────────────────────────────────────────────────

/** Default locale for date/currency formatting */
export const DEFAULT_LOCALE = "en-IN";

/** Currency code for Indian Rupee */
export const CURRENCY_CODE = "INR";

/** Currency symbol */
export const CURRENCY_SYMBOL = "₹";

/**
 * Formats a number as Indian currency.
 * @param {number} amount
 * @returns {string}
 */
export function formatCurrency(amount) {
  return new Intl.NumberFormat(DEFAULT_LOCALE, {
    style: "currency",
    currency: CURRENCY_CODE,
    maximumFractionDigits: 0,
  }).format(amount);
}
