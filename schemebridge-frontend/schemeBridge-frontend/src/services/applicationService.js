/**
 * @file applicationService.js
 * @description Citizen application lifecycle service — connected to Scheme Service.
 *
 * Backend: http://localhost:8081
 *
 * Endpoints:
 *   POST /api/applications                                   — create application
 *   GET  /api/applications/:applicationId                    — get details
 *   GET  /api/applications/my                                — list my applications
 *   POST /api/applications/:applicationId/documents/:code    — upload document (multipart)
 *   POST /api/applications/:applicationId/submit             — submit
 *   POST /api/applications/:applicationId/cancel             — cancel
 *   GET  /api/applications/:applicationId/timeline           — event timeline
 *
 * No mock fallback. Network failures return { error: true, message }.
 */

import { schemeApi } from "@utils/apiClient";

// ─── Create ──────────────────────────────────────────────────────────────────

/**
 * Create a new scheme application.
 * Requires authentication.
 *
 * @param {{ schemeCode: string, profile: CitizenEligibilityProfile }} request
 *   CitizenEligibilityProfile: { age, gender, annualIncome, occupation, state,
 *                                socialCategory, disabilityStatus, attributes }
 *
 * @returns {Promise<{ data: ApplicationResponse } | { error, message }>}
 *   ApplicationResponse: { id, applicationNumber, userId, schemeCode, schemeTitle,
 *                          status, submittedAt, createdAt, updatedAt,
 *                          documentReadiness, documents }
 */
export async function createApplication({ schemeCode, profile } = {}) {
  const payload = { schemeCode };
  if (profile) payload.profile = profile;
  return schemeApi.post("/api/applications", payload);
}

// ─── Get Application ─────────────────────────────────────────────────────────

/**
 * Get detailed state, readiness, and documents for a specific application.
 * Only accessible by the owning citizen.
 */
export async function getApplicationDetails(applicationId) {
  return schemeApi.get(`/api/applications/${encodeURIComponent(applicationId)}`);
}

// ─── List My Applications ────────────────────────────────────────────────────

/**
 * Get all applications belonging to the currently authenticated citizen.
 * @returns {Promise<{ data: ApplicationResponse[] } | { error, message }>}
 */
export async function getMyApplications() {
  return schemeApi.get("/api/applications/my");
}

export const getApplications = getMyApplications;

// ─── Upload Document ─────────────────────────────────────────────────────────

/**
 * Upload a required document for an application.
 * Sends as multipart/form-data.
 *
 * @param {string} applicationId
 * @param {string} documentCode    — e.g. "AADHAAR", "INCOME_CERT"
 * @param {File}   file            — browser File object (PDF/JPG/PNG/JPEG, max 5MB)
 *
 * @returns {Promise<{ data: ApplicationDocumentResponse } | { error, message }>}
 *   ApplicationDocumentResponse: { id, applicationId, documentCode, documentName,
 *                                   mandatory, uploaded, fileName, contentType,
 *                                   fileSize, uploadedAt, rejectionReason, verificationStatus }
 */
export async function uploadDocument(applicationId, documentCode, file) {
  const formData = new FormData();
  formData.append("file", file);
  return schemeApi.upload(
    `/api/applications/${encodeURIComponent(applicationId)}/documents/${encodeURIComponent(documentCode)}`,
    formData
  );
}

/**
 * Download citizen uploaded document as authenticated Blob.
 *
 * @param {string} applicationId
 * @param {string} documentCode
 * @returns {Promise<{ error: boolean, data: Blob, headers: Headers } | { error: true, message: string }>}
 */
export async function downloadDocument(applicationId, documentCode) {
  if (!applicationId || !documentCode) {
    return { error: true, message: "Application ID and Document Code are required." };
  }
  return schemeApi.download(
    `/api/applications/${encodeURIComponent(applicationId)}/documents/${encodeURIComponent(documentCode)}/download`
  );
}

// ─── Submit ──────────────────────────────────────────────────────────────────

/**
 * Submit the application when all mandatory documents are uploaded.
 * Backend transitions status: READY_FOR_SUBMISSION → SUBMITTED.
 */
export async function submitApplication(applicationId) {
  return schemeApi.post(`/api/applications/${encodeURIComponent(applicationId)}/submit`, null);
}

// ─── Cancel ──────────────────────────────────────────────────────────────────

/**
 * Cancel a non-terminal application.
 * Allowed from: DRAFT, DOCUMENTS_PENDING, READY_FOR_SUBMISSION, SUBMITTED, UNDER_REVIEW.
 * NOT allowed from: APPROVED, REJECTED, CANCELLED.
 */
export async function cancelApplication(applicationId) {
  return schemeApi.post(`/api/applications/${encodeURIComponent(applicationId)}/cancel`, null);
}

export const ACTIVE_STATUSES = [
  "DRAFT",
  "DOCUMENTS_PENDING",
  "READY_FOR_SUBMISSION",
  "SUBMITTED",
  "UNDER_REVIEW",
  "CORRECTION_REQUIRED",
];

export const TERMINAL_STATUSES = [
  "REJECTED",
  "CANCELLED",
  "APPROVED",
];

export const REAPPLYABLE_STATUSES = TERMINAL_STATUSES;

export function isReapplyable(status) {
  return TERMINAL_STATUSES.includes(status);
}

export function isActiveApplication(status) {
  return ACTIVE_STATUSES.includes(status);
}

/**
 * Reapply for a previously terminal application (REJECTED, CANCELLED, or APPROVED).
 * Creates a brand-new application for the same scheme while automatically
 * reusing documents from the citizen's permanent Document Vault.
 *
 * @param {string} applicationId - Original application ID (must be terminal)
 * @returns {Promise<{ data: ApplicationResponse } | { error, message }>}
 */
export async function reapplyApplication(applicationId) {
  return schemeApi.post(`/api/applications/${encodeURIComponent(applicationId)}/reapply`, null);
}

// ─── Timeline ────────────────────────────────────────────────────────────────

/**
 * Get the chronological event timeline for an application.
 * Events include: APPLICATION_CREATED, DOCUMENT_UPLOADED, DOCUMENTS_PENDING,
 *                 READY_FOR_SUBMISSION, APPLICATION_SUBMITTED, UNDER_REVIEW,
 *                 DOCUMENT_VERIFIED, DOCUMENT_REJECTED, CORRECTION_REQUIRED,
 *                 APPROVED, REJECTED, CANCELLED.
 *
 * @returns {Promise<{ data: ApplicationTimelineResponse } | { error, message }>}
 *   ApplicationTimelineResponse: {
 *     applicationId, applicationNumber, currentStatus,
 *     events: [{ eventType, fromStatus, toStatus, message, createdAt, metadata }]
 *   }
 */
export async function getApplicationTimeline(applicationId) {
  return schemeApi.get(`/api/applications/${encodeURIComponent(applicationId)}/timeline`);
}

// ─── Document Checklist ──────────────────────────────────────────────────────

/**
 * Get the full required document checklist and real-time status for an application.
 * @param {string} applicationId
 */
export async function getDocumentChecklist(applicationId) {
  return schemeApi.get(`/api/applications/${encodeURIComponent(applicationId)}/document-checklist`);
}

/**
 * Get the scheme's required document checklist definition.
 * @param {string} schemeCode
 */
export async function getSchemeDocumentChecklist(schemeCode) {
  return schemeApi.get(`/api/schemes/${encodeURIComponent(schemeCode)}/document-checklist`);
}

const applicationService = {
  createApplication,
  getApplicationDetails,
  getMyApplications,
  getApplications,
  uploadDocument,
  downloadDocument,
  submitApplication,
  cancelApplication,
  reapplyApplication,
  getApplicationTimeline,
  getDocumentChecklist,
  getSchemeDocumentChecklist,
  ACTIVE_STATUSES,
  TERMINAL_STATUSES,
  REAPPLYABLE_STATUSES,
  isReapplyable,
  isActiveApplication,
};

export default applicationService;
