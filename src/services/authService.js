/**
 * @file authService.js
 * @description Authentication service — mock implementation.
 *
 * Public API is stable. Backend integration: replace each function body
 * with a fetch() call to the corresponding ENDPOINTS.AUTH path.
 *
 * All functions return Promises to match the async/await contract
 * that the backend integration will require.
 */

import { MOCK_LOADING_DELAY_MS } from "../config/constants";
import { ROLES } from "../constants/roles";

/** Simulate network latency */
const delay = (ms = MOCK_LOADING_DELAY_MS) =>
  new Promise((resolve) => setTimeout(resolve, ms));

// ── Mock data ─────────────────────────────────────────────────────────────────

const MOCK_CITIZEN = {
  id: "USR-001",
  name: "Rajesh Patel",
  email: "rajesh.patel@example.com",
  phone: "9876543210",
  role: ROLES.CITIZEN,
  onboardingComplete: true,
  createdAt: "2026-01-15T00:00:00Z",
  lastLogin: new Date().toISOString(),
};

const MOCK_ADMIN = {
  id: "ADM-001",
  name: "Sanjay Kumar",
  email: "admin@schemebridge.gov.in",
  phone: "9000000001",
  role: ROLES.ADMIN,
  onboardingComplete: true,
  createdAt: "2025-11-01T00:00:00Z",
  lastLogin: new Date().toISOString(),
};

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Authenticate a user with email and password.
 * Backend integration: POST /api/v1/auth/login → returns { user, token, refreshToken }
 *
 * @param {{ email: string, password: string }} credentials
 * @returns {Promise<{ user: object, token: string, refreshToken: string }>}
 */
export async function login({ email, password }) {
  await delay(600);

  // Mock: admin credentials
  if (email.includes("admin")) {
    return {
      user: MOCK_ADMIN,
      token: "mock-admin-jwt-token-xxxx",
      refreshToken: "mock-admin-refresh-token-xxxx",
    };
  }

  // Mock: valid citizen
  if (email && password) {
    return {
      user: MOCK_CITIZEN,
      token: "mock-citizen-jwt-token-xxxx",
      refreshToken: "mock-citizen-refresh-token-xxxx",
    };
  }

  throw new Error("Invalid credentials. Please try again.");
}

/**
 * Register a new citizen account.
 * Backend integration: POST /api/v1/auth/register → returns { user, token }
 *
 * @param {{ name: string, email: string, phone: string, password: string }} data
 * @returns {Promise<{ user: object, token: string }>}
 */
export async function register({ name, email, phone, password }) {
  await delay(800);

  if (!email || !password || !name) {
    throw new Error("All required fields must be provided.");
  }

  return {
    user: { ...MOCK_CITIZEN, name, email, phone, id: `USR-${  Date.now()}` },
    token: "mock-new-user-jwt-token-xxxx",
  };
}

/**
 * Log out the current user.
 * Backend integration: POST /api/v1/auth/logout (invalidates refresh token on server).
 *
 * @returns {Promise<{ success: boolean }>}
 */
export async function logout() {
  await delay(200);
  return { success: true };
}

/**
 * Verify and decode a JWT token. Returns user payload or throws.
 * Backend integration: GET /api/v1/auth/verify (token in Authorization header).
 *
 * @param {string} token
 * @returns {Promise<{ user: object, valid: boolean }>}
 */
export async function verifyToken(token) {
  await delay(300);

  if (!token || token === "invalid") {
    throw new Error("Token is invalid or expired.");
  }

  const user = token.includes("admin") ? MOCK_ADMIN : MOCK_CITIZEN;
  return { user, valid: true };
}

/**
 * Refresh an expired JWT using a refresh token.
 * Backend integration: POST /api/v1/auth/refresh → returns new { token, refreshToken }
 *
 * @param {string} refreshToken
 * @returns {Promise<{ token: string, refreshToken: string }>}
 */
export async function refreshToken(refreshToken) {
  await delay(400);
  return {
    token: "mock-refreshed-jwt-token-xxxx",
    refreshToken: "mock-new-refresh-token-xxxx",
  };
}

/**
 * Initiate password reset flow.
 * Backend integration: POST /api/v1/auth/forgot-password → sends OTP to email/phone.
 *
 * @param {{ email: string }} data
 * @returns {Promise<{ message: string }>}
 */
export async function forgotPassword({ email }) {
  await delay(500);
  return {
    message: `A password reset link has been sent to ${email}.`,
  };
}

const authService = {
  login,
  register,
  logout,
  verifyToken,
  refreshToken,
  forgotPassword,
};

export default authService;
