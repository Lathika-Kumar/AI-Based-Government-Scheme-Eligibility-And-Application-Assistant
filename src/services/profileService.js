/**
 * @file profileService.js
 * @description Profile service — mock implementation.
 *
 * Backend integration: replace function bodies with fetch() calls to ENDPOINTS.PROFILE paths.
 */

import { MOCK_LOADING_DELAY_MS } from "../config/constants";
import {
  EDUCATION_OPTIONS,
  OCCUPATION_OPTIONS,
  CASTE_OPTIONS,
  INDIAN_STATES_AND_UTS,
  PROFILE_COMPLETION_WEIGHTS,
  calculateCompletion,
} from "../data/mockProfile";

const delay = (ms = MOCK_LOADING_DELAY_MS) =>
  new Promise((resolve) => setTimeout(resolve, ms));

// ── Mock profile ──────────────────────────────────────────────────────────────

const MOCK_PROFILE = {
  id: "PROF-001",
  userId: "USR-001",
  name: "Rajesh Patel",
  age: 32,
  gender: "Male",
  occupation: "Farmer",
  annualIncome: 180000,
  caste: "OBC (Other Backward Class)",
  state: "Gujarat",
  education: "Secondary Education (Class 9-10)",
  disability: false,
  bplCard: false,
  bankAccount: true,
  aadhaarLinked: true,
  updatedAt: "2026-06-26T10:00:00Z",
};

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Fetch the current user's profile.
 * Backend integration: GET /api/v1/profile
 *
 * @returns {Promise<object>} Profile data
 */
export async function getProfile() {
  await delay();
  return { ...MOCK_PROFILE };
}

/**
 * Update the current user's profile.
 * Backend integration: PUT /api/v1/profile → returns updated profile.
 *
 * @param {Partial<object>} updates
 * @returns {Promise<object>} Updated profile
 */
export async function updateProfile(updates) {
  await delay(600);
  return { ...MOCK_PROFILE, ...updates, updatedAt: new Date().toISOString() };
}

/**
 * Calculate profile completion percentage.
 * Backend integration: GET /api/v1/profile/completion → returns { score, fields }
 *
 * @param {object} profile
 * @returns {Promise<{ score: number, missingFields: string[] }>}
 */
export async function getCompletionScore(profile) {
  await delay(200);
  const score = calculateCompletion(profile);
  const missingFields = Object.entries(PROFILE_COMPLETION_WEIGHTS)
    .filter(([field]) => !profile?.[field])
    .map(([field]) => field);
  return { score, missingFields };
}

/**
 * Fetch all Indian states and union territories.
 * Backend integration: GET /api/v1/config/states
 *
 * @returns {Promise<string[]>}
 */
export async function getStates() {
  await delay(100);
  return [...INDIAN_STATES_AND_UTS];
}

/**
 * Fetch education level options.
 * Backend integration: GET /api/v1/config/education-levels
 *
 * @returns {Promise<string[]>}
 */
export async function getEducationOptions() {
  await delay(100);
  return [...EDUCATION_OPTIONS];
}

/**
 * Fetch occupation options.
 * Backend integration: GET /api/v1/config/occupations
 *
 * @returns {Promise<string[]>}
 */
export async function getOccupationOptions() {
  await delay(100);
  return [...OCCUPATION_OPTIONS];
}

/**
 * Fetch caste category options.
 * Backend integration: GET /api/v1/config/caste-categories
 *
 * @returns {Promise<string[]>}
 */
export async function getCasteOptions() {
  await delay(100);
  return [...CASTE_OPTIONS];
}

/**
 * Update user accessibility preferences.
 * Backend integration: PUT /api/v1/profile/preferences
 *
 * @param {object} preferences
 * @returns {Promise<{ success: boolean }>}
 */
export async function updatePreferences(preferences) {
  await delay(400);
  return { success: true, preferences };
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
