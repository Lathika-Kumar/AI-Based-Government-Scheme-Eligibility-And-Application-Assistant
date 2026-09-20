/**
 * @file feedbackService.js
 * @description Citizen feedback submission & Admin feedback review — connected to Scheme Service (port 8081).
 */

import { schemeApi } from "@utils/apiClient";

export async function submitPortalFeedback(data) {
  return schemeApi.post("/api/feedback", data);
}

export async function getCitizenFeedback() {
  return schemeApi.get("/api/feedback/my");
}

export async function getAdminFeedback({ page = 0, size = 20 } = {}) {
  return schemeApi.get("/api/admin/feedback", { params: { page, size } });
}

export async function updateFeedbackStatus(id, status) {
  return schemeApi.patch(`/api/admin/feedback/${encodeURIComponent(id)}/status`, { status });
}

export default {
  submitPortalFeedback,
  getCitizenFeedback,
  getAdminFeedback,
  updateFeedbackStatus,
};
