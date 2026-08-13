/**
 * @file adminService.js
 * @description Admin portal service — backend integrated with mock fallbacks.
 *
 * Microservices used:
 *  - admin-service: /api/v1/admin/* (Dashboard stats, Announcements, Feedback, Audit Logs, Officers, Reports)
 *  - core-service: /api/v1/schemes/* (Scheme CRUD)
 *  - core-service: /api/v1/applications/* (Application status update / review)
 *  - core-service: /api/v1/documents/* (Document verify / reject)
 */

import apiClient from "@utils/apiClient";
import { ENDPOINTS, buildUrl } from "@config/api";
import { updateApplicationStatus } from "./applicationService";
import { createScheme, updateScheme, deleteScheme } from "./schemeService";
import { verifyDocument, rejectDocument } from "./documentService";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";
const delay = (ms = 500) => new Promise((resolve) => setTimeout(resolve, ms));

// ── Admin Dashboard Statistics ────────────────────────────────────────────────

export async function getAdminDashboardStats() {
  if (USE_MOCK) {
    await delay();
    return {
      totalCitizens: 1245,
      activeSchemes: 48,
      totalApplications: 3120,
      approvedApplications: 2450,
      pendingApplications: 520,
      rejectedApplications: 150,
      totalDisbursedAmount: 18500000,
      pendingVerifications: 84,
      openGrievances: 12,
    };
  }

  return apiClient.get(ENDPOINTS.ADMIN.DASHBOARD_STATS);
}

// ── Announcements / Broadcasts ────────────────────────────────────────────────

export async function getAnnouncements({ page = 0, size = 10 } = {}) {
  if (USE_MOCK) {
    await delay(300);
    return [
      {
        id: "ANN-001",
        title: "New PM-KISAN 17th Installment Released",
        message: "The 17th installment of PM-KISAN has been disbursed to eligible farmers.",
        category: "SCHEME_UPDATE",
        targetAudience: "ALL_CITIZENS",
        status: "PUBLISHED",
        createdAt: new Date().toISOString(),
      },
    ];
  }

  const res = await apiClient.get(ENDPOINTS.ADMIN.ANNOUNCEMENTS, { params: { page, size } });
  return res?.content || (Array.isArray(res) ? res : []);
}

export async function createAnnouncement(announcementData) {
  if (USE_MOCK) {
    await delay(500);
    return { id: `ANN-${Date.now()}`, ...announcementData, status: "DRAFT" };
  }

  return apiClient.post(ENDPOINTS.ADMIN.ANNOUNCEMENTS, announcementData);
}

export async function broadcastAnnouncement(id) {
  if (USE_MOCK) {
    await delay(500);
    return { success: true, id };
  }

  return apiClient.post(buildUrl(ENDPOINTS.ADMIN.ANNOUNCEMENT_BROADCAST, { id }));
}

// ── Feedback & Grievance Management ─────────────────────────────────────────

export async function getGrievances({ status = "", page = 0, size = 10 } = {}) {
  if (USE_MOCK) {
    await delay(300);
    return [
      {
        id: "GRV-001",
        ticketNumber: "GRV-2026-001",
        citizenName: "Ramesh Patel",
        subject: "Delay in PM-KISAN installment",
        category: "PAYMENT_DELAY",
        status: "OPEN",
        createdAt: "2026-07-01T10:00:00Z",
      },
    ];
  }

  const res = await apiClient.get(ENDPOINTS.ADMIN.FEEDBACK, { params: { status, page, size } });
  return res?.content || (Array.isArray(res) ? res : []);
}

export async function resolveGrievance(id, resolutionNote) {
  if (USE_MOCK) {
    await delay(400);
    return { id, status: "RESOLVED", resolutionNote };
  }

  return apiClient.put(buildUrl(ENDPOINTS.ADMIN.FEEDBACK_RESOLVE, { id }), {
    status: "RESOLVED",
    adminNotes: resolutionNote,
    resolutionSummary: resolutionNote,
  });
}

// ── Audit Logs ────────────────────────────────────────────────────────────────

export async function getAuditLogs({ page = 0, size = 20 } = {}) {
  if (USE_MOCK) {
    await delay(300);
    return [
      {
        id: "LOG-001",
        action: "SCHEME_CREATED",
        actorEmail: "admin@schemebridge.gov.in",
        details: "Created scheme PM-KISAN 2026",
        timestamp: new Date().toISOString(),
      },
    ];
  }

  const res = await apiClient.get(ENDPOINTS.ADMIN.AUDIT_LOGS, { params: { page, size } });
  return res?.content || (Array.isArray(res) ? res : []);
}

// ── Admin Application Operations ────────────────────────────────────────────

export async function approveApplication(applicationId, remarks = "Application approved by Officer", benefitDetails = "") {
  return updateApplicationStatus(applicationId, {
    status: "APPROVED",
    remarks,
    benefitDetails,
  });
}

export async function rejectApplication(applicationId, rejectionReason = "Eligibility criteria not fulfilled", remarks = "") {
  return updateApplicationStatus(applicationId, {
    status: "REJECTED",
    rejectionReason,
    remarks,
  });
}

// ── Admin Document Operations ────────────────────────────────────────────────

export async function verifyAdminDocument(documentId, verificationNote = "Verified by Admin Officer") {
  return verifyDocument(documentId, { verifiedBy: "ADMIN_OFFICER", verificationNote });
}

export async function rejectAdminDocument(documentId, rejectionNote = "Document copy unclear") {
  return rejectDocument(documentId, { verifiedBy: "ADMIN_OFFICER", verificationNote: rejectionNote });
}

const adminService = {
  getAdminDashboardStats,
  getAnnouncements,
  createAnnouncement,
  broadcastAnnouncement,
  getGrievances,
  resolveGrievance,
  getAuditLogs,
  approveApplication,
  rejectApplication,
  verifyAdminDocument,
  rejectAdminDocument,
  createScheme,
  updateScheme,
  deleteScheme,
};

export default adminService;
