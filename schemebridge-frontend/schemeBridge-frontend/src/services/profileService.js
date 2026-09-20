/**
 * @file profileService.js
 * @description Persistent citizen profile service — connected to Scheme Service.
 *
 * Backend: http://localhost:8081/api/profile
 *
 * Endpoints:
 *   GET /api/profile  — fetch persistent citizen profile & onboarding state
 *   PUT /api/profile  — upsert persistent citizen profile & onboarding state
 *
 * No mock data or fake fallbacks in production flow.
 */

import {
  EDUCATION_OPTIONS,
  OCCUPATION_OPTIONS,
  CASTE_OPTIONS,
  INDIAN_STATES_AND_UTS,
  PROFILE_COMPLETION_WEIGHTS,
  calculateCompletion,
} from "../data/mockProfile";
import { schemeApi } from "@utils/apiClient";
import { PROFILE_ENDPOINTS } from "@config/api";

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Fetch the current authenticated user's persistent profile.
 * Backend integration: GET /api/profile
 *
 * @returns {Promise<{ data: CitizenProfileResponse } | { error: true, message: string }>}
 */
export async function getProfile() {
  return schemeApi.get(PROFILE_ENDPOINTS.GET);
}

/**
 * Update/upsert the current authenticated user's persistent profile.
 * Backend integration: PUT /api/profile → returns updated CitizenProfileResponse.
 *
 * @param {Partial<object>} updates
 * @returns {Promise<{ data: CitizenProfileResponse } | { error: true, message: string }>}
 */
export async function updateProfile(updates) {
  return schemeApi.put(PROFILE_ENDPOINTS.UPDATE, updates);
}

/**
 * Calculate profile completion percentage.
 *
 * @param {object} profile
 * @returns {{ score: number, missingFields: string[] }}
 */
export function getCompletionScore(profile) {
  const score = calculateCompletion(profile);
  const missingFields = Object.entries(PROFILE_COMPLETION_WEIGHTS)
    .filter(([field]) => {
      if (field === "annualIncome") {
        const inc = profile?.annualIncome !== undefined ? profile.annualIncome : profile?.income;
        return inc === undefined || inc === null || inc === "" || isNaN(Number(inc));
      }
      return !profile?.[field];
    })
    .map(([field]) => field);
  return { score, missingFields };
}

/**
 * Fetch all Indian states and union territories.
 * @returns {string[]}
 */
export function getStates() {
  return [...INDIAN_STATES_AND_UTS];
}

/**
 * Fetch education level options.
 * @returns {string[]}
 */
export function getEducationOptions() {
  return [...EDUCATION_OPTIONS];
}

/**
 * Fetch occupation options.
 * @returns {string[]}
 */
export function getOccupationOptions() {
  return [...OCCUPATION_OPTIONS];
}

/**
 * Fetch caste category options.
 * @returns {string[]}
 */
export function getCasteOptions() {
  return [...CASTE_OPTIONS];
}

/**
 * Update user accessibility preferences via profile PUT.
 *
 * @param {object} preferences
 * @returns {Promise<{ data: CitizenProfileResponse } | { error: true, message: string }>}
 */
export async function updatePreferences(preferences) {
  return updateProfile({ accessibilityPreferences: preferences });
}

const profileService = {
  getProfile,
  updateProfile,
  getCompletionScore,
  getStates,
  getEducationOptions,
  getOccupationOptions,
  getCasteOptions,
  updatePreferences,
};

export default profileService;
