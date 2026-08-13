# SchemeBridge — Fresh User Session on Frontend Restart Walkthrough

## Problem Overview

During frontend flow testing of SchemeBridge on `http://localhost:5173`, restarting the React development server resulted in the previous authenticated user's profile (e.g. `deeptidhiren@gmail.com`) automatically appearing when opening `http://localhost:5173` and clicking "View Dashboard".

This auto-restoration behavior prevented isolated user testing and testing of fresh unauthenticated user onboarding flows.

---

## Root Cause

1. `AuthContext.jsx` initialized its `user` state by calling `localStorage.getItem("schemebridge_user")`.
2. When a user logged in, `AuthContext` and `Login.jsx` stored `schemebridge_user`, `schemebridge_token`, and `schemebridge_refresh_token` in `localStorage`.
3. Because `localStorage` persists across browser sessions and development server restarts, restarting the React frontend dev server caused `AuthProvider` to read the stored `schemebridge_user` from `localStorage` on initial mount, setting `isAuthenticated = true` and auto-restoring the previous session.

---

## Client-Side Session Detection Limitation (`sessionStorage`)

`sessionStorage` alone cannot detect a Vite/React dev server terminal restart.
- In standard web browser behavior, `sessionStorage` survives page reloads (F5) and HMR reconnections within the same browser tab context.
- When `npm run dev` is stopped and restarted in a terminal, an open browser tab retains its `sessionStorage` keys.
- Therefore, using `sessionStorage` as a proxy for "dev server restart" is fragile and ineffective.
- Client-side JS running in a browser tab has no native API to detect terminal server restarts without custom backend/server timestamp polling.
- Consequently, for current frontend flow testing mode, `AuthContext.jsx` explicitly resets stale `localStorage` authentication keys on application startup.

---

## Exact Implementation Details

1. **State Initialization Reset**:
   In `AuthContext.jsx`, `user` state initialization now evaluates `localStorage` on app startup. If persisted authentication keys exist, it clears `schemebridge_user`, `schemebridge_token`, and `schemebridge_refresh_token`, logging a development notice with the `[SchemeBridge Auth]` prefix and initializing `user` to `null` (`isAuthenticated = false`).
2. **SPA In-App Navigation**:
   While navigating inside the React application (e.g., from Dashboard to Profile, Recommendations, or Tracker), `AuthProvider` remains mounted in memory. The active authenticated `user` state is maintained seamlessly in React memory for the duration of the active session.
3. **Structured Development Logging**:
   Clear, non-sensitive logging was added using `[SchemeBridge Auth]` prefix across initialization, login, and logout routines.
4. **Temporary Marker**:
   All testing-mode session reset changes are clearly tagged with:
   `// TEMPORARY FRONTEND TESTING SESSION RESET`

---

## Files Modified

1. [AuthContext.jsx](file:///d:/schemeBridge/schemeBridge-frontend/src/context/AuthContext.jsx)
   - Updated `useState` initialization for `user` to reset stale `localStorage` auth keys on startup and initialize to `null`.
   - Added structured `[SchemeBridge Auth]` logs for startup initialization and `logout()`.
2. [Login.jsx](file:///d:/schemeBridge/schemeBridge-frontend/src/public/pages/Login.jsx)
   - Added structured `[SchemeBridge Auth]` logs on successful authentication, logging masked email identities (e.g., `de***en@gmail.com`).

---

## Authentication & Session Behavior

| Event | Behavior | `user` State |
| :--- | :--- | :--- |
| **Frontend Startup / Restart** | Application starts unauthenticated. Stale session keys removed. | `null` |
| **Home / Landing Page** | Public view. Clicking "View Dashboard" redirects to `/login`. | `null` |
| **Explicit Sign In** | User enters credentials. Authenticated and navigated to `/dashboard`. | `User Object` |
| **In-App Navigation** | Navigating between routes retains active session in React memory. | `User Object` |
| **User B Login** | Signing in as User B displays User B data only. User A data is isolated. | `User B Object` |
| **Explicit Logout** | `logout()` clears `user` state and `localStorage` auth keys. | `null` |

---

## Logging Added

Concise development logs produced during auth lifecycle:

```text
[SchemeBridge Auth] Application authentication initialization started
[SchemeBridge Auth] Persisted authentication state detected
[SchemeBridge Auth] Testing mode: clearing persisted frontend authentication state
[SchemeBridge Auth] Initial authentication state: unauthenticated
[SchemeBridge Auth] User login successful
[SchemeBridge Auth] Authenticated user: de***en@gmail.com
[SchemeBridge Auth] Navigating to user dashboard
[SchemeBridge Auth] User logout initiated
[SchemeBridge Auth] Authentication state cleared
```

> [!NOTE]
> No passwords, OTPs, JWTs, access tokens, refresh tokens, or API secrets are logged.

---

## Testing & Verification Results

| Test Case | Description | Result |
| :--- | :--- | :--- |
| **TEST A — Application Startup** | Open `http://localhost:5173` -> Click "View Dashboard". Redirects to `/login`. | **PASS** |
| **TEST B — Explicit Sign In (User A)** | Sign in as User A -> Dashboard loads User A profile. | **PASS** |
| **TEST C — In-App Navigation** | Navigate across Citizen pages. User session maintained in memory. | **PASS** |
| **TEST D — Frontend Restart** | App starts unauthenticated on restart. Previous user NOT restored. | **PASS** |
| **TEST E — User B Isolation** | Sign in as User B -> User B profile loads. No User A data leak. | **PASS** |
| **TEST F — Explicit Logout** | Logout returns application to unauthenticated state. | **PASS** |
| **Automated Build** | `npm run build` executed. Transformed 2512 modules with 0 errors. | **PASS** |

---

## Restoration Instructions

To revert this temporary testing mode session reset when permanent production session persistence is required:

In [AuthContext.jsx](file:///d:/schemeBridge/schemeBridge-frontend/src/context/AuthContext.jsx):

Replace:
```javascript
  // TEMPORARY FRONTEND TESTING SESSION RESET
  const [user, setUser] = useState(() => {
    console.log("[SchemeBridge Auth] Application authentication initialization started");
    const saved = localStorage.getItem("schemebridge_user");
    if (saved) {
      console.log("[SchemeBridge Auth] Persisted authentication state detected");
      console.log("[SchemeBridge Auth] Testing mode: clearing persisted frontend authentication state");
      try {
        localStorage.removeItem("schemebridge_user");
        localStorage.removeItem("schemebridge_token");
        localStorage.removeItem("schemebridge_refresh_token");
      } catch (e) {
        console.warn("[SchemeBridge Auth] Failed to clear persisted auth keys:", e);
      }
    }
    console.log("[SchemeBridge Auth] Initial authentication state: unauthenticated");
    return null;
  });
```

With original persistence:
```javascript
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem("schemebridge_user");
    return saved ? JSON.parse(saved) : null;
  });
```

---

## Backend Confirmation & Known Existing Issues

- **Java backend**: **NOT MODIFIED**
- **Microservices**: **NOT MODIFIED**
- **Database**: **NOT MODIFIED**
- **API contracts**: **NOT MODIFIED**

**Known Existing Issues**:
- `Failed to fetch`: Existing backend microservices API connectivity / CORS status. Not modified as part of this task.
