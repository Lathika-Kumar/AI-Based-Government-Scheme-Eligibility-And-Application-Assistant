/**
 * @file adminService.js
 * @description Administrative review & operations service — connected to Scheme Service.
 *
 * Backend: http://localhost:8081
 * Authorization: ROLE_ADMIN, ROLE_SCHEME_MANAGER, or ROLE_VERIFICATION_OFFICER (enforced by backend).
 */

import { schemeApi } from "@utils/apiClient";

// ─── Application Queue & Review Operations ──────────────────────────────────

/**
 * Get the paginated administrative application review queue.
 */
export async function getApplicationsQueue({
  status, schemeCode, page = 0, size = 20, sort = "createdAt", direction = "DESC"
} = {}) {
  const params = { page, size, sort, direction };
  if (status)     params.status = status;
  if (schemeCode) params.schemeCode = schemeCode;
  return schemeApi.get("/api/admin/applications", { params });
}

/**
 * Move an application from SUBMITTED → UNDER_REVIEW.
 */
export async function startReview(applicationId) {
  return schemeApi.post(`/api/admin/applications/${encodeURIComponent(applicationId)}/review/start`, null);
}

/**
 * Mark a citizen document as verified.
 */
export async function verifyDocument(applicationId, documentCode) {
  return schemeApi.post(
    `/api/admin/applications/${encodeURIComponent(applicationId)}/documents/${encodeURIComponent(documentCode)}/verify`,
    null
  );
}

/**
 * Reject a citizen document with a mandatory reason.
 */
export async function rejectDocument(applicationId, documentCode, reason) {
  return schemeApi.post(
    `/api/admin/applications/${encodeURIComponent(applicationId)}/documents/${encodeURIComponent(documentCode)}/reject`,
    { reason }
  );
}

/**
 * Request correction on a citizen document by document code.
 */
export async function requestDocumentCorrectionByCode(applicationId, documentCode, reason) {
  return schemeApi.post(
    `/api/admin/applications/${encodeURIComponent(applicationId)}/documents/${encodeURIComponent(documentCode)}/correction`,
    { reason }
  );
}

/**
 * Get all documents pending officer review.
 */
export async function getPendingDocumentsQueue() {
  return schemeApi.get("/api/admin/documents/pending-verification");
}

/**
 * Mark a document as verified by document ID.
 */
export async function verifyDocumentById(documentId) {
  return schemeApi.post(`/api/admin/documents/${encodeURIComponent(documentId)}/verify`, null);
}

/**
 * Reject a document by document ID with a mandatory reason.
 */
export async function rejectDocumentById(documentId, reason) {
  return schemeApi.post(`/api/admin/documents/${encodeURIComponent(documentId)}/reject`, { reason });
}

/**
 * Request correction on a document by document ID with notes.
 */
export async function requestDocumentCorrection(documentId, reason) {
  return schemeApi.post(`/api/admin/documents/${encodeURIComponent(documentId)}/correction`, { reason });
}

/**
 * Approve the application. All mandatory documents must be VERIFIED.
 */
export async function approveApplication(applicationId, remarks = "Approved") {
  return schemeApi.post(
    `/api/admin/applications/${encodeURIComponent(applicationId)}/review/approve`,
    { remarks }
  );
}

/**
 * Reject the application with a mandatory decision reason.
 */
export async function rejectApplication(applicationId, reason) {
  const remarksText = reason || "Rejected";
  return schemeApi.post(
    `/api/admin/applications/${encodeURIComponent(applicationId)}/review/reject`,
    { remarks: remarksText, reason: remarksText }
  );
}

/**
 * Request more documents or corrections on the application.
 */
export async function requestMoreDocuments(applicationId, reason) {
  const remarksText = reason || "Additional documents / correction required.";
  return schemeApi.post(
    `/api/admin/applications/${encodeURIComponent(applicationId)}/review/request-documents`,
    { remarks: remarksText, reason: remarksText }
  );
}

/**
 * General canonical review handler for ApplicationReviewWorkspace and operations.
 * Submits canonical review action: APPROVE, REJECT, REQUEST_MORE_DOCUMENTS, or START.
 */
export async function reviewApplication(applicationId, payload = {}, maybeRemarks) {
  if (!applicationId) {
    return { error: true, message: "Application ID is required." };
  }

  // Handle positional invocation: reviewApplication(appId, "APPROVE", "Remarks")
  let action, remarks, reason, reviewerNotes;
  if (typeof payload === "string") {
    action = payload;
    remarks = maybeRemarks;
  } else if (payload && typeof payload === "object") {
    action = payload.action || payload.reviewAction || payload.status;
    remarks = payload.remarks;
    reason = payload.reason || payload.correctionReason;
    reviewerNotes = payload.reviewerNotes;
  }

  if (!action) {
    return { error: true, message: "Unknown review action: undefined" };
  }

  const normalizedAction = String(action).trim().toUpperCase();
  const effectiveRemarks = remarks || reviewerNotes || reason || "";
  const effectiveReason = reason || reviewerNotes || remarks || "";

  let canonicalAction;
  if (normalizedAction === "APPROVE" || normalizedAction === "APPROVED") {
    canonicalAction = "APPROVE";
  } else if (normalizedAction === "REJECT" || normalizedAction === "REJECTED") {
    canonicalAction = "REJECT";
  } else if (
    normalizedAction === "REQUEST_MORE_DOCUMENTS" ||
    normalizedAction === "REQUEST_DOCUMENTS" ||
    normalizedAction === "CORRECTION_REQUIRED" ||
    normalizedAction === "CORRECTION"
  ) {
    canonicalAction = "REQUEST_MORE_DOCUMENTS";
  } else if (normalizedAction === "START" || normalizedAction === "START_REVIEW") {
    canonicalAction = "START";
  } else {
    return { error: true, message: `Unknown review action: ${action}` };
  }

  return schemeApi.post(
    `/api/admin/applications/${encodeURIComponent(applicationId)}/review`,
    {
      action: canonicalAction,
      remarks: effectiveRemarks || (canonicalAction === "APPROVE" ? "Approved" : ""),
      reason: effectiveReason,
      reviewerNotes: reviewerNotes || effectiveRemarks
    }
  );
}

/**
 * Fetch or process OCR text & extracted fields for an application document.
 */
export async function getDocumentOcr(applicationId, documentCode) {
  return schemeApi.post(
    `/api/applications/${encodeURIComponent(applicationId)}/documents/${encodeURIComponent(documentCode)}/ocr`,
    null
  );
}

/**
 * Fetch all required and uploaded documents for an application.
 */
export async function getApplicationDocuments(applicationId) {
  if (!applicationId) return { error: true, message: "Application ID is required." };
  return schemeApi.get(`/api/applications/${encodeURIComponent(applicationId)}/documents`);
}

// ─── Cross-Application Document Verification Queue ──────────────────────────

/**
 * Get all citizen documents across all applications with filtering.
 */
export async function getAdminDocumentsQueue({
  status, schemeCode, query, search, page = 0, size = 20
} = {}) {
  const params = { page, size };
  if (status)     params.status = status;
  if (schemeCode) params.schemeCode = schemeCode;
  if (search || query) params.search = search || query;
  return schemeApi.get("/api/admin/documents", { params });
}

/** Alias for getAdminDocumentsQueue */
export const getAdminDocuments = getAdminDocumentsQueue;

/**
 * Get exact application details by ID (for exact detail review).
 */
export async function getAdminApplicationById(applicationId) {
  if (!applicationId) return { error: true, message: "Application ID is required." };
  return schemeApi.get(`/api/admin/applications/${encodeURIComponent(applicationId)}`);
}

/**
 * Download citizen uploaded document as authenticated Blob.
 */
export async function downloadAdminDocument(applicationId, documentCode) {
  if (!applicationId || !documentCode) return { error: true, message: "Application ID and Document Code are required." };
  return schemeApi.download(`/api/applications/${encodeURIComponent(applicationId)}/documents/${encodeURIComponent(documentCode)}/download`);
}

/**
 * Download citizen uploaded document by Document ID as authenticated Blob.
 */
export async function downloadAdminDocumentById(documentId) {
  if (!documentId) return { error: true, message: "Document ID is required." };
  return schemeApi.download(`/api/admin/documents/${encodeURIComponent(documentId)}/download`);
}

/**
 * Get single document metadata by ID.
 */
export async function getAdminDocumentById(documentId) {
  if (!documentId) return { error: true, message: "Document ID is required." };
  return schemeApi.get(`/api/admin/documents/${encodeURIComponent(documentId)}`);
}

/**
 * Fetch explainable AI verification findings, scores, and checks for a document.
 */
export async function getDocumentVerification(documentId) {
  if (!documentId) return { error: true, message: "Document ID is required." };
  return schemeApi.get(`/api/admin/documents/${encodeURIComponent(documentId)}/verification`);
}

/**
 * Fetch explainable AI verification findings for an application document.
 */
export async function getApplicationDocumentVerification(applicationId, documentCode) {
  if (!applicationId || !documentCode) return { error: true, message: "Application ID and Document Code are required." };
  return schemeApi.get(`/api/applications/${encodeURIComponent(applicationId)}/documents/${encodeURIComponent(documentCode)}/verification`);
}

/**
 * Fetch application document readiness details.
 */
export async function getApplicationDocumentReadiness(applicationId) {
  if (!applicationId) return { error: true, message: "Application ID is required." };
  return schemeApi.get(`/api/applications/${encodeURIComponent(applicationId)}/document-readiness`);
}

/**
 * Administrative officer verification & approval of citizen document.
 */
export async function verifyAdminDocument(documentId, remarks = "Document verified by administrative officer.") {
  if (!documentId) return { error: true, message: "Document ID is required." };
  return schemeApi.post(`/api/admin/documents/${encodeURIComponent(documentId)}/verify`, {
    decision: "APPROVE",
    remarks
  });
}

/**
 * Administrative officer requests correction / re-upload from citizen.
 */
export async function requestAdminDocumentCorrection(documentId, reason = "Document requires correction or re-upload.") {
  if (!documentId) return { error: true, message: "Document ID is required." };
  return schemeApi.post(`/api/admin/documents/${encodeURIComponent(documentId)}/correction`, {
    decision: "REQUEST_CORRECTION",
    reason,
    correctionReason: reason,
    remarks: reason
  });
}

/**
 * Administrative officer rejects citizen document with mandatory reason.
 */
export async function rejectAdminDocument(documentId, reason) {
  if (!documentId) return { error: true, message: "Document ID is required." };
  if (!reason || !reason.trim()) return { error: true, message: "Rejection reason is mandatory." };
  return schemeApi.post(`/api/admin/documents/${encodeURIComponent(documentId)}/reject`, {
    decision: "REJECT",
    reason,
    remarks: reason
  });
}

// ─── Operations Metrics & Analytics ─────────────────────────────────────────

/**
 * Get aggregated operations dashboard metrics across schemes, applications, and documents.
 */
export async function getAdminMetrics() {
  return schemeApi.get("/api/admin/metrics");
}

/**
 * Get detailed analytics data including approval rates and intake timelines.
 */
export async function getAdminAnalytics() {
  return schemeApi.get("/api/admin/analytics");
}

/**
 * Download operational CSV reports (applications, schemes, grievances).
 */
export async function downloadAdminReport(type, { status, schemeCode } = {}) {
  const params = {};
  if (status) params.status = status;
  if (schemeCode) params.schemeCode = schemeCode;
  return schemeApi.get(`/api/admin/reports/${encodeURIComponent(type)}`, { params });
}

// ─── Audit Logs ─────────────────────────────────────────────────────────────

/**
 * Get paginated administrative and system audit logs.
 */
export async function getAdminAuditLogs({
  actor, action, entity, entityType, startDate, endDate, page = 0, size = 20, sort = "createdAt", direction = "DESC"
} = {}) {
  const params = { page, size, sort, direction };
  if (actor)               params.actor = actor;
  if (action)              params.action = action;
  if (entity || entityType) params.entity = entity || entityType;
  if (startDate)           params.startDate = startDate;
  if (endDate)             params.endDate = endDate;
  return schemeApi.get("/api/admin/audit-logs", { params });
}

// ─── Admin Settings ─────────────────────────────────────────────────────────

/**
 * Get current platform administrative settings.
 */
export async function getAdminSettings() {
  return schemeApi.get("/api/admin/settings");
}

/**
 * Update platform administrative settings.
 */
export async function updateAdminSettings(settings) {
  return schemeApi.put("/api/admin/settings", settings);
}

const adminService = {
  getApplicationsQueue,
  getApplicationDocuments,
  startReview,
  verifyDocument,
  rejectDocument,
  requestDocumentCorrectionByCode,
  getPendingDocumentsQueue,
  verifyDocumentById,
  rejectDocumentById,
  requestDocumentCorrection,
  approveApplication,
  rejectApplication,
  requestMoreDocuments,
  reviewApplication,
  getDocumentOcr,
  getAdminDocumentsQueue,
  getAdminDocuments,
  getAdminApplicationById,
  downloadAdminDocument,
  downloadAdminDocumentById,
  getAdminMetrics,
  getAdminAnalytics,
  downloadAdminReport,
  getAdminAuditLogs,
  getAdminSettings,
  updateAdminSettings,
};

export default adminService;
