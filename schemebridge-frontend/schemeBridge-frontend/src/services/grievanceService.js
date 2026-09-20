/**
 * @file grievanceService.js
 * @description Citizen grievance lodging & Admin Grievance Desk management — connected to Scheme Service (port 8081).
 */

import { schemeApi } from "@utils/apiClient";

// ─── Citizen Operations ─────────────────────────────────────────────────────

/**
 * Lodge a new citizen grievance.
 */
export async function createGrievance(grievanceData) {
  return schemeApi.post("/api/grievances", grievanceData);
}

/**
 * Get grievances for the current authenticated citizen.
 */
export async function getCitizenGrievances({ status, category, page = 0, size = 20 } = {}) {
  const params = { page, size };
  if (status && status !== "ALL") params.status = status;
  if (category && category !== "ALL") params.category = category;
  return schemeApi.get("/api/grievances", { params });
}

/**
 * Get single grievance details with chronological timeline.
 */
export async function getGrievanceById(grievanceId) {
  return schemeApi.get(`/api/grievances/${encodeURIComponent(grievanceId)}`);
}

/**
 * Citizen reply to a grievance ticket.
 */
export async function replyToGrievance(grievanceId, message, attachments = []) {
  return schemeApi.post(`/api/grievances/${encodeURIComponent(grievanceId)}/reply`, {
    message,
    attachments,
  });
}

/**
 * Citizen closes their own grievance ticket.
 */
export async function closeGrievance(grievanceId, remarks = "") {
  return schemeApi.post(`/api/grievances/${encodeURIComponent(grievanceId)}/close`, { remarks });
}

// ─── Admin Grievance Desk Operations ────────────────────────────────────────

/**
 * Admin: Get all grievances across platform with filtering and search.
 */
export async function getAdminGrievances({
  status,
  priority,
  category,
  assignedTo,
  search,
  page = 0,
  size = 20,
  sort = "createdAt",
  direction = "DESC",
} = {}) {
  const params = { page, size, sort, direction };
  if (status && status !== "ALL") params.status = status;
  if (priority && priority !== "ALL") params.priority = priority;
  if (category && category !== "ALL") params.category = category;
  if (assignedTo && assignedTo !== "ALL") params.assignedTo = assignedTo;
  if (search) params.search = search;
  return schemeApi.get("/api/admin/grievances", { params });
}

/**
 * Admin: Assign grievance to an officer.
 */
export async function assignGrievance(grievanceId, assignedTo, remarks = "") {
  return schemeApi.post(`/api/admin/grievances/${encodeURIComponent(grievanceId)}/assign`, {
    assignedTo,
    remarks,
  });
}

/**
 * Admin: Resolve grievance.
 */
export async function resolveGrievance(grievanceId, resolution, actionTaken = "") {
  return schemeApi.post(`/api/admin/grievances/${encodeURIComponent(grievanceId)}/resolve`, {
    resolution,
    actionTaken,
  });
}

/**
 * Admin: Reply to citizen on a grievance ticket.
 */
export async function adminReplyToGrievance(grievanceId, message, attachments = []) {
  return schemeApi.post(`/api/admin/grievances/${encodeURIComponent(grievanceId)}/reply`, {
    message,
    attachments,
  });
}

/**
 * Admin: Close grievance ticket.
 */
export async function adminCloseGrievance(grievanceId, remarks = "") {
  return schemeApi.post(`/api/admin/grievances/${encodeURIComponent(grievanceId)}/close`, { remarks });
}

const grievanceService = {
  createGrievance,
  getCitizenGrievances,
  getGrievanceById,
  replyToGrievance,
  closeGrievance,
  getAdminGrievances,
  assignGrievance,
  resolveGrievance,
  adminReplyToGrievance,
  adminCloseGrievance,
};

export default grievanceService;
