/**
 * @file apiClient.js
 * @description Centralized HTTP client for all SchemeBridge API requests.
 *
 * Architecture:
 *  - Auth Service  → http://localhost:8080  (VITE_AUTH_API_URL)
 *  - Scheme Service → http://localhost:8081  (VITE_SCHEME_API_URL)
 *
 * In development the Vite proxy routes /api/* to the correct backend.
 * Tokens are read from localStorage on every request (no closure capture).
 *
 * Error shape returned by all methods on failure:
 *   { error: true, status: <number|null>, message: <string>, data: <object|null> }
 *
 * On 401 the client clears stored credentials and redirects to /login.
 * Requests are NOT retried on 4xx (except 429 which is surfaced as-is).
 * Network/timeout failures surface a message without leaking internals.
 */

const AUTH_BASE  = import.meta.env.VITE_AUTH_API_URL   ?? "";
const SCHEME_BASE = import.meta.env.VITE_SCHEME_API_URL ?? "";


const DEFAULT_TIMEOUT_MS = 15_000;

/** Storage keys — must match AuthContext.jsx */
const KEYS = {
  ACCESS_TOKEN:     "sb_access_token",
  REFRESH_TOKEN:    "sb_refresh_token",
  USER:             "sb_user",
  LAST_ROUTE:       "sb_last_protected_route",
  ONBOARDED_PREFIX: "sb_onboarded_",
};

export const storage = {
  getToken:        () => typeof sessionStorage !== "undefined" ? sessionStorage.getItem(KEYS.ACCESS_TOKEN) : null,
  setToken:        (t) => typeof sessionStorage !== "undefined" && sessionStorage.setItem(KEYS.ACCESS_TOKEN, t),
  getRefreshToken: () => typeof sessionStorage !== "undefined" ? sessionStorage.getItem(KEYS.REFRESH_TOKEN) : null,
  setRefreshToken: (t) => typeof sessionStorage !== "undefined" && sessionStorage.setItem(KEYS.REFRESH_TOKEN, t),
  getUser:         () => {
    try {
      return typeof sessionStorage !== "undefined" ? JSON.parse(sessionStorage.getItem(KEYS.USER)) : null;
    } catch {
      return null;
    }
  },
  setUser:         (u) => typeof sessionStorage !== "undefined" && sessionStorage.setItem(KEYS.USER, JSON.stringify(u)),
  
  // Last protected route persistence (tab-scoped)
  getLastProtectedRoute: () => typeof sessionStorage !== "undefined" ? sessionStorage.getItem(KEYS.LAST_ROUTE) : null,
  setLastProtectedRoute: (route) => {
    if (typeof sessionStorage !== "undefined" && route && typeof route === "string") {
      sessionStorage.setItem(KEYS.LAST_ROUTE, route);
    }
  },
  clearLastProtectedRoute: () => {
    if (typeof sessionStorage !== "undefined") {
      sessionStorage.removeItem(KEYS.LAST_ROUTE);
    }
  },

  // Per-user onboarding completion persistence
  isOnboarded: (userId) => {
    if (!userId || typeof sessionStorage === "undefined") return false;
    return sessionStorage.getItem(KEYS.ONBOARDED_PREFIX + userId) === "true";
  },
  setOnboarded: (userId, isComplete = true) => {
    if (userId && typeof sessionStorage !== "undefined") {
      sessionStorage.setItem(KEYS.ONBOARDED_PREFIX + userId, String(isComplete));
    }
  },

  clearAll: () => {
    if (typeof sessionStorage === "undefined") return;
    try {
      // Clear specific known keys
      sessionStorage.removeItem(KEYS.ACCESS_TOKEN);
      sessionStorage.removeItem(KEYS.REFRESH_TOKEN);
      sessionStorage.removeItem(KEYS.USER);
      sessionStorage.removeItem(KEYS.LAST_ROUTE);

      // Clean up any dynamic sb_ keys in sessionStorage
      const sessionKeysToRemove = [];
      for (let i = 0; i < sessionStorage.length; i++) {
        const k = sessionStorage.key(i);
        if (k && k.startsWith("sb_")) sessionKeysToRemove.push(k);
      }
      sessionKeysToRemove.forEach((k) => sessionStorage.removeItem(k));

      // Clean up any legacy sb_ keys in localStorage
      const localKeysToRemove = [];
      for (let i = 0; i < localStorage.length; i++) {
        const k = localStorage.key(i);
        if (k && (k.startsWith("sb_") || (k.startsWith("schemebridge_") && !k.startsWith("schemebridge_documents_")))) {
          localKeysToRemove.push(k);
        }
      }
      localKeysToRemove.forEach((k) => localStorage.removeItem(k));
    } catch (e) {
      console.warn("storage.clearAll error:", e);
    }
  },
};


/** Parse error body safely — never expose stack traces. */
async function parseError(response) {
  try {
    const body = await response.json();
    return {
      error: true,
      status: response.status,
      message: body.message || body.error || `Request failed (${response.status})`,
      data: body,
    };
  } catch {
    return { error: true, status: response.status, message: `Request failed (${response.status})`, data: null };
  }
}

// Single-flight refresh mutex to prevent concurrent refresh token rotations
let activeRefreshPromise = null;

async function executeTokenRefresh() {
  const refreshToken = storage.getRefreshToken();
  if (!refreshToken) return false;

  try {
    const response = await fetch(`${AUTH_BASE}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      storage.clearAll();
      return false;
    }

    const data = await response.json();
    if (data.accessToken) storage.setToken(data.accessToken);
    if (data.refreshToken) storage.setRefreshToken(data.refreshToken);
    return true;
  } catch {
    storage.clearAll();
    return false;
  }
}

const tryRefreshToken = executeTokenRefresh;

/** Core request function. */
async function request(baseUrl, method, path, { body, params, isMultipart = false, timeoutMs = DEFAULT_TIMEOUT_MS, isRetry = false, responseType = "json" } = {}) {
  const token = storage.getToken();
  const headers = {};

  if (token) headers["Authorization"] = `Bearer ${token}`;
  if (!isMultipart && responseType !== "blob") headers["Content-Type"] = "application/json";

  // Build URL with query params
  let url = `${baseUrl}${path}`;
  if (params) {
    const qs = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== null && v !== "")
      )
    ).toString();
    if (qs) url += `?${qs}`;
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(url, {
      method,
      headers,
      body: isMultipart ? body : (body !== undefined ? JSON.stringify(body) : undefined),
      signal: controller.signal,
    });

    clearTimeout(timer);

    // 401 Unauthorized — attempt token refresh (except for /auth endpoints)
    if (response.status === 401 && !path.startsWith("/api/auth/") && !isRetry) {
      if (!activeRefreshPromise) {
        activeRefreshPromise = tryRefreshToken().finally(() => {
          activeRefreshPromise = null;
        });
      }
      const refreshed = await activeRefreshPromise;
      if (refreshed) {
        // Retry original request once with new token
        return request(baseUrl, method, path, { body, params, isMultipart, timeoutMs, isRetry: true, responseType });
      }

      // If cannot refresh or is auth endpoint, clear session and safely redirect
      storage.clearAll();
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
      return { error: true, status: 401, message: "Your session has expired. Please log in again.", data: null };
    }

    // 204 No Content
    if (response.status === 204) return { error: false, data: null };

    if (!response.ok) return parseError(response);

    if (responseType === "blob") {
      const blob = await response.blob();
      return { error: false, data: blob, headers: response.headers };
    }

    const data = await response.json();
    return { error: false, data };

  } catch (err) {
    clearTimeout(timer);
    if (err.name === "AbortError") {
      return { error: true, status: null, message: "Request timed out. Please check your connection.", data: null };
    }
    return { error: true, status: null, message: "Unable to connect to the server. Please try again.", data: null };
  }
}


// ─────────────────────────────────────────────────────────────────────────────
// Auth Service client  (port 8080)
// ─────────────────────────────────────────────────────────────────────────────
export const authApi = {
  get:    (path, opts)        => request(AUTH_BASE,   "GET",    path, opts),
  post:   (path, body, opts)  => request(AUTH_BASE,   "POST",   path, { ...opts, body }),
  put:    (path, body, opts)  => request(AUTH_BASE,   "PUT",    path, { ...opts, body }),
  patch:  (path, body, opts)  => request(AUTH_BASE,   "PATCH",  path, { ...opts, body }),
  delete: (path, opts)        => request(AUTH_BASE,   "DELETE", path, opts),
};


// ─────────────────────────────────────────────────────────────────────────────
// Scheme Service client  (port 8081)
// ─────────────────────────────────────────────────────────────────────────────
export const schemeApi = {
  get:      (path, opts)        => request(SCHEME_BASE, "GET",    path, opts),
  post:     (path, body, opts)  => request(SCHEME_BASE, "POST",   path, { ...opts, body }),
  put:      (path, body, opts)  => request(SCHEME_BASE, "PUT",    path, { ...opts, body }),
  patch:    (path, body, opts)  => request(SCHEME_BASE, "PATCH",  path, { ...opts, body }),
  delete:   (path, opts)        => request(SCHEME_BASE, "DELETE", path, opts),
  upload:   (path, formData)    => request(SCHEME_BASE, "POST",   path, { body: formData, isMultipart: true }),
  download: (path, opts)        => request(SCHEME_BASE, "GET",    path, { ...opts, responseType: "blob" }),
};

export default { authApi, schemeApi, storage };
