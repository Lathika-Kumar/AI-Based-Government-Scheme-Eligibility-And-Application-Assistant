/**
 * @file applicationService.js
 * @description Scheme application service — backend-integrated with mock fallbacks.
 *
 * Set VITE_USE_MOCK_API=true in .env to force mock responses.
 * All functions fall back to mock logic if the API call throws.
 */

import { MOCK_LOADING_DELAY_MS } from "../config/constants";
import { APPLICATION_STATUS } from "../constants/applicationStatus";
import apiClient from "@utils/apiClient";
import { ENDPOINTS, buildUrl } from "@config/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";

const delay = (ms = MOCK_LOADING_DELAY_MS) =>
  new Promise((resolve) => setTimeout(resolve, ms));

// ── Mock application store ────────────────────────────────────────────────────

const MOCK_APPLICATIONS = [
  {
    id: "APP-9023",
    schemeId: "atal-pension-yojana",
    schemeName: "Atal Pension Yojana (APY)",
    ministry: "Ministry of Finance",
    status: APPLICATION_STATUS.UNDER_REVIEW,
    submittedAt: "2026-06-15T16:05:00Z",
    updatedAt: "2026-06-17T10:30:00Z",
    referenceNumber: "APY/GJ/2026/009023",
    remarks: null,
    timeline: [
      { stage: APPLICATION_STATUS.SUBMITTED, completedAt: "2026-06-15T16:05:00Z", notes: "Application submitted successfully." },
      { stage: APPLICATION_STATUS.UNDER_REVIEW, completedAt: "2026-06-17T10:30:00Z", notes: "Application is being reviewed by the department." },
    ],
  },
];

const MOCK_SAVED_SCHEMES = [
  { schemeId: "pm-kisan", savedAt: "2026-06-24T09:05:00Z" },
  { schemeId: "pm-awas-yojana-urban", savedAt: "2026-06-20T14:00:00Z" },
];

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Fetch all applications for the current user.
 * Backend integration: GET /api/v1/applications
 *
 * @returns {Promise<object[]>}
 */
export async function getApplications() {
  const fallbackMock = async () => {
    await delay();
    return [...MOCK_APPLICATIONS];
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(ENDPOINTS.APPLICATIONS.LIST);
  } catch (error) {
    console.warn("getApplications API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Fetch a single application by ID.
 * Backend integration: GET /api/v1/applications/:id
 *
 * @param {string} id
 * @returns {Promise<object>}
 */
export async function getApplicationById(id) {
  const fallbackMock = async () => {
    await delay(400);
    const app = MOCK_APPLICATIONS.find((a) => a.id === id);
    if (!app) {
      const error = new Error(`Application "${id}" not found.`);
      error.status = 404;
      throw error;
    }
    return { ...app };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(buildUrl(ENDPOINTS.APPLICATIONS.DETAIL, { id }));
  } catch (error) {
    console.warn("getApplicationById API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Submit a new application for a scheme.
 * Backend integration: POST /api/v1/applications { schemeId, documents[] }
 *
 * @param {{ schemeId: string, schemeName: string, ministry: string, documents?: string[] }} data
 * @returns {Promise<object>} Newly created application
 */
export async function submitApplication({ schemeId, schemeName, ministry, documents = [] }) {
  const fallbackMock = async () => {
    await delay(1000);
    if (!schemeId) throw new Error("Scheme ID is required to submit an application.");
    const refNum = `${schemeId.toUpperCase().slice(0, 3)}/GJ/${new Date().getFullYear()}/${Math.floor(100000 + Math.random() * 900000)}`;
    const newApp = {
      id: `APP-${Math.floor(10000 + Math.random() * 90000)}`,
      schemeId,
      schemeName,
      ministry,
      status: APPLICATION_STATUS.SUBMITTED,
      submittedAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      referenceNumber: refNum,
      remarks: null,
      timeline: [
        {
          stage: APPLICATION_STATUS.SUBMITTED,
          completedAt: new Date().toISOString(),
          notes: "Application submitted successfully.",
        },
      ],
      documents,
    };
    MOCK_APPLICATIONS.push(newApp);
    return { ...newApp };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(ENDPOINTS.APPLICATIONS.SUBMIT, { schemeId, schemeName, ministry, documents });
  } catch (error) {
    console.warn("submitApplication API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Withdraw (cancel) a submitted application.
 * Backend integration: POST /api/v1/applications/:id/withdraw
 *
 * @param {string} id
 * @returns {Promise<{ success: boolean }>}
 */
export async function withdrawApplication(id) {
  const fallbackMock = async () => {
    await delay(600);
    return { success: true, id };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(buildUrl(ENDPOINTS.APPLICATIONS.WITHDRAW, { id }));
  } catch (error) {
    console.warn("withdrawApplication API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Fetch all schemes saved by the current user.
 * Backend integration: GET /api/v1/applications/saved
 *
 * @returns {Promise<Array<{ schemeId: string, savedAt: string }>>}
 */
export async function getSavedSchemes() {
  const fallbackMock = async () => {
    await delay(400);
    return [...MOCK_SAVED_SCHEMES];
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(ENDPOINTS.APPLICATIONS.SAVED_SCHEMES);
  } catch (error) {
    console.warn("getSavedSchemes API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Save a scheme to the user's saved list.
 * Backend integration: POST /api/v1/applications/saved/:schemeId
 *
 * @param {string} schemeId
 * @returns {Promise<{ success: boolean, schemeId: string }>}
 */
export async function saveScheme(schemeId) {
  const fallbackMock = async () => {
    await delay(300);
    if (!MOCK_SAVED_SCHEMES.some((s) => s.schemeId === schemeId)) {
      MOCK_SAVED_SCHEMES.push({ schemeId, savedAt: new Date().toISOString() });
    }
    return { success: true, schemeId };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(buildUrl(ENDPOINTS.APPLICATIONS.SAVE_SCHEME, { schemeId }));
  } catch (error) {
    console.warn("saveScheme API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Remove a scheme from the user's saved list.
 * Backend integration: DELETE /api/v1/applications/saved/:schemeId
 *
 * @param {string} schemeId
 * @returns {Promise<{ success: boolean, schemeId: string }>}
 */
export async function unsaveScheme(schemeId) {
  const fallbackMock = async () => {
    await delay(300);
    const idx = MOCK_SAVED_SCHEMES.findIndex((s) => s.schemeId === schemeId);
    if (idx !== -1) MOCK_SAVED_SCHEMES.splice(idx, 1);
    return { success: true, schemeId };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.delete(buildUrl(ENDPOINTS.APPLICATIONS.UNSAVE_SCHEME, { schemeId }));
  } catch (error) {
    console.warn("unsaveScheme API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Fetch the activity timeline for an application.
 * Backend integration: GET /api/v1/applications/:id/timeline
 *
 * @param {string} id
 * @returns {Promise<object[]>}
 */
export async function getApplicationTimeline(id) {
  await delay(400);
  const app = MOCK_APPLICATIONS.find((a) => a.id === id);
  return app?.timeline ?? [];
}

const applicationService = {
  getApplications,
  getApplicationById,
  submitApplication,
  withdrawApplication,
  getSavedSchemes,
  saveScheme,
  unsaveScheme,
  getApplicationTimeline,
};

export default applicationService;
