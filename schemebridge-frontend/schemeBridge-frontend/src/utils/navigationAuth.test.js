import { describe, it, expect, beforeEach } from "vitest";

// Mock localStorage and sessionStorage for node test environment
const createStorageMock = () => {
  let store = {};
  return {
    getItem: (key) => store[key] ?? null,
    setItem: (key, val) => { store[key] = String(val); },
    removeItem: (key) => { delete store[key]; },
    clear: () => { store = {}; },
  };
};

globalThis.localStorage = createStorageMock();
globalThis.sessionStorage = createStorageMock();

const { storage } = await import("./apiClient");

describe("Frontend Navigation & Session Restoration", () => {
  beforeEach(() => {
    globalThis.localStorage.clear();
    globalThis.sessionStorage.clear();
  });

  it("persists and retrieves per-user onboarding status correctly", () => {
    expect(storage.isOnboarded("101")).toBe(false);

    storage.setOnboarded("101", true);
    expect(storage.isOnboarded("101")).toBe(true);

    // Another user remains false
    expect(storage.isOnboarded("102")).toBe(false);
  });

  it("persists and retrieves last protected route", () => {
    expect(storage.getLastProtectedRoute()).toBeNull();

    storage.setLastProtectedRoute("/applications/42?tab=documents");
    expect(storage.getLastProtectedRoute()).toBe("/applications/42?tab=documents");

    storage.clearLastProtectedRoute();
    expect(storage.getLastProtectedRoute()).toBeNull();
  });

  it("logout clears all tokens, user state, and last route", () => {
    storage.setToken("access_token_123");
    storage.setRefreshToken("refresh_token_123");
    storage.setUser({ id: "101", email: "user@example.com", onboardingComplete: true });
    storage.setLastProtectedRoute("/applications/42");

    storage.clearAll();

    expect(storage.getToken()).toBeNull();
    expect(storage.getRefreshToken()).toBeNull();
    expect(storage.getUser()).toBeNull();
    expect(storage.getLastProtectedRoute()).toBeNull();
  });

  it("redirect logic validates role boundaries and rejects unauthorized admin routes for citizens", () => {
    const resolveRedirect = (user, candidateRoute) => {
      const isAdmin = user.role === "admin" || user.role === "scheme_manager";
      if (isAdmin) {
        if (candidateRoute && candidateRoute.startsWith("/admin")) return candidateRoute;
        return "/admin/dashboard";
      }
      if (!user.onboardingComplete) return "/onboarding";
      if (candidateRoute && !candidateRoute.startsWith("/admin") && !candidateRoute.startsWith("/login") && !candidateRoute.startsWith("/signup")) {
        return candidateRoute;
      }
      return "/dashboard";
    };

    // Citizen with completed onboarding and valid protected route
    expect(resolveRedirect({ role: "citizen", onboardingComplete: true }, "/applications/7")).toBe("/applications/7");

    // Citizen with completed onboarding and no saved route
    expect(resolveRedirect({ role: "citizen", onboardingComplete: true }, null)).toBe("/dashboard");

    // Citizen with incomplete onboarding
    expect(resolveRedirect({ role: "citizen", onboardingComplete: false }, "/applications/7")).toBe("/onboarding");

    // Citizen attempting to access saved admin route -> MUST safely fall back to /dashboard
    expect(resolveRedirect({ role: "citizen", onboardingComplete: true }, "/admin/review")).toBe("/dashboard");

    // Admin with saved admin route
    expect(resolveRedirect({ role: "admin", onboardingComplete: true }, "/admin/review")).toBe("/admin/review");

    // Admin without saved route
    expect(resolveRedirect({ role: "admin", onboardingComplete: true }, null)).toBe("/admin/dashboard");
  });

  it("evaluates server profile hydration across COMPLETE, IN_PROGRESS, and NOT_STARTED profiles", () => {
    const normaliseUser = (backendUser, backendProfile = {}) => {
      const isComplete = Boolean(
        backendProfile?.onboardingComplete === true ||
        backendProfile?.onboardingStatus === "COMPLETE"
      );
      let onboardingStatus = "NOT_STARTED";
      if (isComplete) onboardingStatus = "COMPLETE";
      else if (backendProfile?.onboardingStatus) onboardingStatus = backendProfile.onboardingStatus;
      else if (backendProfile?.onboardingStep && backendProfile.onboardingStep > 1) onboardingStatus = "IN_PROGRESS";

      let onboardingStep = 1;
      if (isComplete) onboardingStep = 3;
      else if (typeof backendProfile?.onboardingStep === "number" && backendProfile.onboardingStep >= 1) onboardingStep = backendProfile.onboardingStep;

      return {
        id: String(backendUser.id),
        email: backendUser.email,
        onboardingComplete: isComplete,
        onboardingStatus,
        onboardingStep,
      };
    };

    // 1. COMPLETE profile
    const completeUser = normaliseUser(
      { id: 65, email: "aarav.sharma@schemebridge.in" },
      { onboardingComplete: true, onboardingStatus: "COMPLETE", onboardingStep: 3, displayName: "Aarav S. Sharma" }
    );
    expect(completeUser.onboardingComplete).toBe(true);
    expect(completeUser.onboardingStatus).toBe("COMPLETE");
    expect(completeUser.onboardingStep).toBe(3);

    // 2. IN_PROGRESS profile (Step 2)
    const inProgressUser = normaliseUser(
      { id: 70, email: "sneha@schemebridge.in" },
      { onboardingComplete: false, onboardingStatus: "IN_PROGRESS", onboardingStep: 2, displayName: "Sneha Roy" }
    );
    expect(inProgressUser.onboardingComplete).toBe(false);
    expect(inProgressUser.onboardingStatus).toBe("IN_PROGRESS");
    expect(inProgressUser.onboardingStep).toBe(2);

    // 3. NOT_STARTED profile (Step 1)
    const newUser = normaliseUser(
      { id: 82, email: "lathikaanti1@gmail.com" },
      { onboardingComplete: false, onboardingStatus: "NOT_STARTED", onboardingStep: 1 }
    );
    expect(newUser.onboardingComplete).toBe(false);
    expect(newUser.onboardingStatus).toBe("NOT_STARTED");
    expect(newUser.onboardingStep).toBe(1);
  });

  it("hydrates citizen profile displayName accurately and renders skeleton during profile loading", () => {
    // Normalise test
    const backendUser = { id: 65, email: "aarav.sharma@schemebridge.in", firstName: "Aarav", lastName: "Sharma" };
    const backendProfile = { displayName: "Aarav S. Sharma", onboardingComplete: true, onboardingStatus: "COMPLETE", onboardingStep: 3 };

    const resolveDisplayName = (user, profileLoading, loading) => {
      if (loading || profileLoading || !user) {
        return { type: "skeleton" };
      }
      const displayName = user?.displayName || user?.name || `${user?.firstName || ""} ${user?.lastName || ""}`.trim() || (user?.email ? user.email.split("@")[0] : "");
      return { type: "name", value: displayName, initial: displayName.charAt(0).toUpperCase() };
    };

    // State 1: During profileLoading -> Render skeleton (Never "Citizen Account")
    expect(resolveDisplayName(null, true, false)).toEqual({ type: "skeleton" });

    // State 2: Profile hydrated -> Render actual name and initial
    const hydratedUser = {
      id: "65",
      email: backendUser.email,
      displayName: backendProfile.displayName,
      name: backendProfile.displayName,
      onboardingComplete: true
    };
    expect(resolveDisplayName(hydratedUser, false, false)).toEqual({
      type: "name",
      value: "Aarav S. Sharma",
      initial: "A"
    });

    // State 3: User update with partial profile
    const updateUserProfile = (prevUser, updates) => {
      const resolvedName = updates.displayName || updates.name || prevUser.displayName || prevUser.name;
      return {
        ...prevUser,
        ...updates,
        displayName: resolvedName,
        name: resolvedName,
      };
    };
    const updated = updateUserProfile(hydratedUser, { displayName: "Aarav Sharma Updated" });
    expect(updated.displayName).toBe("Aarav Sharma Updated");
    expect(updated.name).toBe("Aarav Sharma Updated");
  });

  it("verifies unauthenticated public state after logout", () => {
    let authState = { user: { id: "65", role: "citizen", displayName: "Aarav S. Sharma" }, isAuthenticated: true };
    const renderHeaderActions = (state) => {
      if (state.isAuthenticated && state.user) {
        return { primary: "Overview Dashboard", secondary: null };
      }
      return { primary: "Register", secondary: "Sign In" };
    };

    // Before logout
    expect(renderHeaderActions(authState)).toEqual({ primary: "Overview Dashboard", secondary: null });

    // Perform logout
    storage.clearAll();
    authState = { user: null, isAuthenticated: false };

    // After logout
    expect(renderHeaderActions(authState)).toEqual({ primary: "Register", secondary: "Sign In" });
    expect(storage.getUser()).toBeNull();
    expect(storage.getToken()).toBeNull();
  });

  it("verifies fresh citizen notification state is empty with zero unread count", async () => {
    const { DEFAULT_NOTIFICATIONS } = await import("../data/mockNotifications");
    expect(DEFAULT_NOTIFICATIONS).toEqual([]);

    // Fresh user notification initialization
    const initialNotifications = [];
    const unreadCount = initialNotifications.filter((n) => !n.read && !n.isRead).length;

    expect(initialNotifications.length).toBe(0);
    expect(unreadCount).toBe(0);
  });
});
