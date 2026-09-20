/**
 * @file authService.js
 * @description Authentication service integrated with the Auth Service backend.
 *
 * Backend: http://localhost:8080/api/auth/*
 *
 * Endpoints used:
 *   POST /api/auth/signup         — { firstName, lastName, email, password, phoneNumber }
 *   POST /api/auth/verify-otp     — { email, otp }
 *   POST /api/auth/login          — { email, password }
 *   POST /api/auth/refresh        — { refreshToken }
 *   POST /api/auth/logout         — { refreshToken }
 *   GET  /api/auth/me             — Bearer token → UserInfoDto { id, email, roles }
 *
 * No mock fallback in production flows.
 * Network failures surface a human-readable { error } object — never fake data.
 */

import { authApi, storage } from "@utils/apiClient";

// ─── Signup ───────────────────────────────────────────────────────────────────

/**
 * Register a new citizen account.
 * @param {{ firstName, lastName, email, password, phoneNumber?, dob?: string }} data
 * @returns {Promise<{ data } | { error, message }>}
 */
export async function signup({ firstName, lastName, email, password, phoneNumber, dob }) {
  return authApi.post("/api/auth/signup", { firstName, lastName, email, password, phoneNumber, dob });
}

// ─── OTP Verification ────────────────────────────────────────────────────────

/**
 * Verify the 6-digit registration OTP.
 * @param {{ email, otp }} data
 * @returns {Promise<{ data } | { error, message }>}
 */
export async function verifyOtp({ email, otp }) {
  return authApi.post("/api/auth/verify-otp", { email, otp });
}

// ─── Login ────────────────────────────────────────────────────────────────────

/**
 * Authenticate with email and password.
 * On success stores accessToken and refreshToken in localStorage.
 * @param {{ email, password }} credentials
 * @returns {Promise<{ data: LoginResponse } | { error, message }>}
 *
 * LoginResponse shape (from backend):
 *   { message, accessToken, refreshToken, tokenType, expiresIn, user: { id, email, roles } }
 */
export async function login({ email, password }) {
  const result = await authApi.post("/api/auth/login", { email, password });
  if (!result.error && result.data) {
    const { accessToken, refreshToken, user } = result.data;
    if (accessToken) storage.setToken(accessToken);
    if (refreshToken) storage.setRefreshToken(refreshToken);
    if (user) storage.setUser(user);
  }
  return result;
}

// ─── Refresh ─────────────────────────────────────────────────────────────────

/**
 * Rotate the access token using a valid refresh token.
 * Automatically updates stored tokens on success.
 * @returns {Promise<{ data: TokenRefreshResponse } | { error, message }>}
 */
export async function refresh() {
  const refreshToken = storage.getRefreshToken();
  if (!refreshToken) {
    return { error: true, status: 401, message: "No refresh token available." };
  }
  const result = await authApi.post("/api/auth/refresh", { refreshToken });
  if (!result.error && result.data) {
    if (result.data.accessToken)  storage.setToken(result.data.accessToken);
    if (result.data.refreshToken) storage.setRefreshToken(result.data.refreshToken);
  }
  return result;
}

// ─── Logout ───────────────────────────────────────────────────────────────────

/**
 * Revoke the current refresh token on the backend and clear local session.
 * Always clears local storage even if the backend call fails.
 */
export async function logout() {
  const refreshToken = storage.getRefreshToken();
  if (refreshToken) {
    // Best-effort server-side revocation — don't block logout on failure
    try {
      await authApi.post("/api/auth/logout", { refreshToken });
    } catch {
      // ignore network errors during logout
    }
  }
  storage.clearAll();
  return { error: false, data: null };
}

// ─── /me  ─────────────────────────────────────────────────────────────────────

/**
 * Restore session using the stored JWT.
 * Returns { id, email, roles } on success.
 * @returns {Promise<{ data: UserInfoDto } | { error, message }>}
 */
export async function getMe() {
  return authApi.get("/api/auth/me");
}

// ─── Forgot Password ─────────────────────────────────────────────────────────

/**
 * Request a password reset OTP.
 * Backend sends OTP to the registered email if found (always 200 generic message).
 * @param {string} email
 * @returns {Promise<{ data: { message } } | { error, message }>}
 */
export async function forgotPassword(email) {
  return authApi.post("/api/auth/forgot-password", { email });
}

// ─── Reset Password (Atomic) ────────────────────────────────────────────────

/**
 * Reset account password atomically with OTP and new password.
 * Backend verifies OTP and updates password in a single transaction.
 * @param {{ email, otp, newPassword }} data
 * @returns {Promise<{ data: { message } } | { error, message }>}
 */
export async function resetPassword({ email, otp, newPassword }) {
  return authApi.post("/api/auth/reset-password", { email, otp, newPassword });
}

// ─── Resend OTP ──────────────────────────────────────────────────────────────

/**
 * Resend registration verification OTP with 60-second cooldown.
 * @param {string} email
 * @returns {Promise<{ data: { message } } | { error, message }>}
 */
export async function resendOtp(email) {
  return authApi.post("/api/auth/resend-otp", { email });
}

// ─── Change Password ────────────────────────────────────────────────────────

/**
 * Change password for the currently authenticated user.
 * Verified on the server using the authenticated JWT identity.
 * @param {{ currentPassword: string, newPassword: string }} data
 * @returns {Promise<{ data: { message: string } } | { error: boolean, status: number|null, message: string }>}
 */
export async function changePassword({ currentPassword, newPassword }) {
  return authApi.post("/api/auth/change-password", { currentPassword, newPassword });
}

const authService = { signup, verifyOtp, login, refresh, logout, getMe, forgotPassword, resetPassword, resendOtp, changePassword };
export default authService;

