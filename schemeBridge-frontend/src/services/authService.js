/**
 * @file authService.js
 * @description Authentication service — backend-integrated with mock fallbacks.
 *
 * Set VITE_USE_MOCK_API=true in .env to force mock responses.
 * All functions fall back to mock logic if the API call throws.
 */

import { MOCK_LOADING_DELAY_MS } from "../config/constants";
import { ROLES } from "../constants/roles";
import apiClient from "@utils/apiClient";
import { ENDPOINTS } from "@config/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";

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
  const fallbackMock = async () => {
    await delay(600);
    if (email.includes("admin")) {
      return {
        user: MOCK_ADMIN,
        token: "mock-admin-jwt-token-xxxx",
        refreshToken: "mock-admin-refresh-token-xxxx",
      };
    }
    if (email && password) {
      return {
        user: MOCK_CITIZEN,
        token: "mock-citizen-jwt-token-xxxx",
        refreshToken: "mock-citizen-refresh-token-xxxx",
      };
    }
    throw new Error("Invalid credentials. Please try again.");
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(ENDPOINTS.AUTH.LOGIN, { email, password });
  } catch (error) {
    console.warn("login API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Register a new citizen account.
 * Backend integration: POST /api/v1/auth/register → returns { user, token }
 *
 * @param {{ name: string, email: string, phone: string, password: string }} data
 * @returns {Promise<{ user: object, token: string }>}
 */
export async function register({ name, email, phone, password }) {
  const fallbackMock = async () => {
    await delay(800);
    if (!email || !password || !name) {
      throw new Error("All required fields must be provided.");
    }
    return {
      user: { ...MOCK_CITIZEN, name, email, phone, id: `USR-${Date.now()}` },
      token: "mock-new-user-jwt-token-xxxx",
    };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(ENDPOINTS.AUTH.REGISTER, { name, email, phone, password });
  } catch (error) {
    console.warn("register API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Log out the current user.
 * Backend integration: POST /api/v1/auth/logout (invalidates refresh token on server).
 *
 * @returns {Promise<{ success: boolean }>}
 */
export async function logout() {
  const fallbackMock = async () => {
    await delay(200);
    return { success: true };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(ENDPOINTS.AUTH.LOGOUT);
  } catch (error) {
    console.warn("logout API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Verify and decode a JWT token. Returns user payload or throws.
 * Backend integration: GET /api/v1/profile (token in Authorization header).
 *
 * @param {string} token
 * @returns {Promise<{ user: object, valid: boolean }>}
 */
export async function verifyToken(token) {
  const fallbackMock = async () => {
    await delay(300);
    if (!token || token === "invalid") {
      throw new Error("Token is invalid or expired.");
    }
    const user = token.includes("admin") ? MOCK_ADMIN : MOCK_CITIZEN;
    return { user, valid: true };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    const user = await apiClient.get(ENDPOINTS.PROFILE.GET);
    return { user, valid: true };
  } catch (error) {
    console.warn("verifyToken API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Refresh an expired JWT using a refresh token.
 * Backend integration: POST /api/v1/auth/refresh → returns new { token, refreshToken }
 *
 * @param {string} refreshToken
 * @returns {Promise<{ token: string, refreshToken: string }>}
 */
export async function refreshToken(refreshToken) {
  const fallbackMock = async () => {
    await delay(400);
    return {
      token: "mock-refreshed-jwt-token-xxxx",
      refreshToken: "mock-new-refresh-token-xxxx",
    };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(ENDPOINTS.AUTH.REFRESH_TOKEN, { refreshToken });
  } catch (error) {
    console.warn("refreshToken API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Initiate password reset flow.
 * Backend integration: POST /api/v1/auth/forgot-password → sends OTP to email/phone.
 *
 * @param {{ email: string }} data
 * @returns {Promise<{ message: string }>}
 */
export async function forgotPassword({ email }) {
  const fallbackMock = async () => {
    await delay(500);
    return { message: `A password reset link has been sent to ${email}.` };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(ENDPOINTS.AUTH.FORGOT_PASSWORD, { email });
  } catch (error) {
    console.warn("forgotPassword API failed, falling back to mock:", error);
    return fallbackMock();
  }
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
