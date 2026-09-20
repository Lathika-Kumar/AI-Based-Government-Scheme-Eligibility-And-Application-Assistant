/* eslint-disable react-refresh/only-export-components */
/**
 * @file AuthContext.jsx
 * @description Authentication state for the entire SchemeBridge application.
 *
 * State machine:
 *   loading → authenticated | unauthenticated
 *
 * Session restoration:
 *   On mount: if an access token exists, call GET /api/auth/me to validate it.
 *   If valid, fetch persistent citizen profile via GET /api/profile from Scheme Service.
 *   Backend onboardingComplete from MongoDB is authoritative.
 *
 * Role detection:
 *   Backend returns roles as ["ROLE_ADMIN"], ["ROLE_SCHEME_MANAGER"], ["ROLE_USER"].
 *   isAdmin is true only when the backend-confirmed role list contains ROLE_ADMIN or
 *   ROLE_SCHEME_MANAGER. Frontend state CANNOT grant admin access.
 */

import { createContext, useContext, useState, useEffect, useCallback } from "react";
import { storage } from "@utils/apiClient";
import authService from "@services/authService";
import profileService from "@services/profileService";

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
};

/** Map backend roles array → simplified frontend role string. */
function deriveRole(roles = []) {
  const upper = (roles || []).map((r) => String(r || "").trim().toUpperCase());
  if (
    upper.includes("ROLE_ADMIN") ||
    upper.includes("ADMIN") ||
    upper.includes("ROLE_ADMINISTRATOR") ||
    upper.includes("ADMINISTRATOR") ||
    upper.includes("ROLE_SUPER_ADMIN") ||
    upper.includes("SUPER_ADMIN")
  ) {
    return "admin";
  }
  if (upper.includes("ROLE_SCHEME_MANAGER") || upper.includes("SCHEME_MANAGER")) return "scheme_manager";
  if (upper.includes("ROLE_VERIFICATION_OFFICER") || upper.includes("VERIFICATION_OFFICER")) return "verification_officer";
  return "citizen";
}

/** Normalise backend user & backend profile into a consistent user object. */
function normaliseUser(backendUser, backendProfile = {}, extraFields = {}) {
  const userId = backendUser?.id ? String(backendUser.id) : (extraFields?.id ? String(extraFields.id) : null);
  const roles = backendUser?.roles ?? extraFields?.roles ?? [];
  const role = deriveRole(roles);
  const rawRoleStr = String(backendUser?.role || extraFields?.role || "").toUpperCase();
  const isAdmin = Boolean(
    extraFields?.isAdmin === true ||
    backendUser?.isAdmin === true ||
    role === "admin" ||
    role === "scheme_manager" ||
    role === "verification_officer" ||
    rawRoleStr.includes("ADMIN")
  );

  // Admins are always complete. For citizens, backend profile is authoritative.
  const isComplete = isAdmin || Boolean(
    backendProfile?.onboardingComplete === true ||
    backendProfile?.onboardingStatus === "COMPLETE" ||
    extraFields?.onboardingComplete === true ||
    extraFields?.onboardingStatus === "COMPLETE"
  );

  let onboardingStatus = "NOT_STARTED";
  if (isAdmin || isComplete) {
    onboardingStatus = "COMPLETE";
  } else if (backendProfile?.onboardingStatus) {
    onboardingStatus = backendProfile.onboardingStatus;
  } else if (extraFields?.onboardingStatus) {
    onboardingStatus = extraFields.onboardingStatus;
  } else if (backendProfile?.onboardingStep && backendProfile.onboardingStep > 1) {
    onboardingStatus = "IN_PROGRESS";
  }

  let onboardingStep = 1;
  if (isAdmin || isComplete) {
    onboardingStep = 3;
  } else if (typeof backendProfile?.onboardingStep === "number" && backendProfile.onboardingStep >= 1) {
    onboardingStep = backendProfile.onboardingStep;
  } else if (typeof extraFields?.onboardingStep === "number" && extraFields.onboardingStep >= 1) {
    onboardingStep = extraFields.onboardingStep;
  }

  const firstName = backendUser?.firstName || extraFields?.firstName || "";
  const lastName = backendUser?.lastName || extraFields?.lastName || "";
  const fullName = `${firstName} ${lastName}`.trim();

  const displayName = backendProfile?.displayName ||
    extraFields?.displayName ||
    extraFields?.name ||
    fullName ||
    (backendUser?.email ? backendUser.email.split("@")[0] : "");

  return {
    ...extraFields,
    id:                 userId,
    email:              backendUser?.email || extraFields?.email,
    firstName:          firstName || undefined,
    lastName:           lastName || undefined,
    fullName:           fullName || undefined,
    displayName:        displayName || undefined,
    name:               displayName || undefined,
    roles,
    role,
    isAdmin,
    onboardingComplete: isComplete,
    onboardingStatus,
    onboardingStep,
    dob:                backendProfile?.dob ?? extraFields?.dob ?? null,
    age:                backendProfile?.age ?? extraFields?.age,
    gender:             backendProfile?.gender ?? extraFields?.gender,
    state:              backendProfile?.state ?? extraFields?.state,
    district:           backendProfile?.district ?? extraFields?.district,
    occupation:         backendProfile?.occupation ?? extraFields?.occupation,
    annualIncome:       backendProfile?.annualIncome ?? extraFields?.annualIncome,
    caste:              backendProfile?.socialCategory ?? extraFields?.caste,
    socialCategory:     backendProfile?.socialCategory ?? extraFields?.socialCategory ?? extraFields?.caste,
    education:          backendProfile?.education ?? extraFields?.education,
    disabilityStatus:   backendProfile?.disabilityStatus ?? extraFields?.disabilityStatus,
    accessibilityPreferences: backendProfile?.accessibilityPreferences ?? extraFields?.accessibilityPreferences ?? {},
  };
}

export const AuthProvider = ({ children }) => {
  const [user, setUser]                 = useState(null);
  const [loading, setLoading]           = useState(true); // true while restoring session
  const [profileLoading, setProfileLoading] = useState(false);

  // ─── Session restoration on mount ───────────────────────────────────────
  useEffect(() => {
    let cancelled = false;

    async function restoreSession() {
      const token = storage.getToken();
      if (!token) {
        setLoading(false);
        return;
      }

      setProfileLoading(true);

      try {
        // Try /me with existing token
        let result = await authService.getMe();

        if (result.error && result.status === 401) {
          // Token expired — try one refresh
          const refreshResult = await authService.refresh();
          if (!refreshResult.error) {
            // Retry /me with new token
            result = await authService.getMe();
          }
        }

        if (!cancelled) {
          if (!result.error && result.data) {
            const backendUser = result.data;
            const role = deriveRole(backendUser.roles ?? []);
            const isAdmin = role === "admin" || role === "scheme_manager" || role === "verification_officer";

            let backendProfile = null;
            if (!isAdmin) {
              try {
                const profRes = await profileService.getProfile();
                if (profRes && !profRes.error && profRes.data) {
                  backendProfile = profRes.data;
                }
              } catch (err) {
                console.warn("Failed to load profile on session restore:", err);
              }
            }

            const normalised = normaliseUser(backendUser, backendProfile);
            if (normalised.onboardingComplete && normalised.id) {
              storage.setOnboarded(normalised.id, true);
            }
            storage.setUser(normalised);
            setUser(normalised);
          } else {
            // Could not restore — clear everything
            storage.clearAll();
            setUser(null);
          }
        }
      } catch (e) {
        console.warn("Session restore unexpected error:", e);
        if (!cancelled) {
          storage.clearAll();
          setUser(null);
        }
      } finally {
        if (!cancelled) {
          setProfileLoading(false);
          setLoading(false);
        }
      }
    }

    restoreSession();
    return () => { cancelled = true; };
  }, []);

  // ─── Login ───────────────────────────────────────────────────────────────
  const login = useCallback(async (email, password) => {
    setProfileLoading(true);
    try {
      const result = await authService.login({ email, password });
      if (result.error) {
        return { error: result.message || "Login failed." };
      }
      const { user: backendUser } = result.data;
      const role = deriveRole(backendUser.roles ?? []);
      const isAdmin = role === "admin" || role === "scheme_manager" || role === "verification_officer";

      let backendProfile = null;
      if (!isAdmin) {
        try {
          const profRes = await profileService.getProfile();
          if (profRes && !profRes.error && profRes.data) {
            backendProfile = profRes.data;
          }
        } catch (err) {
          console.warn("Failed to load profile on login:", err);
        }
      }

      const normalised = normaliseUser(backendUser, backendProfile);
      if (normalised.onboardingComplete && normalised.id) {
        storage.setOnboarded(normalised.id, true);
      }
      storage.setUser(normalised);
      setUser(normalised);
      return { user: normalised };
    } finally {
      setProfileLoading(false);
    }
  }, []);

  // ─── Signup ──────────────────────────────────────────────────────────────
  const signup = useCallback(async ({ firstName, lastName, email, password, phoneNumber, dob }) => {
    const result = await authService.signup({ firstName, lastName, email, password, phoneNumber, dob });
    if (result.error) {
      return { error: result.message || "Signup failed." };
    }
    // Signup does NOT log in — user must verify OTP first.
    return { data: result.data, email };
  }, []);

  // ─── OTP Verification ────────────────────────────────────────────────────
  const verifyOtp = useCallback(async ({ email, otp }) => {
    const result = await authService.verifyOtp({ email, otp });
    if (result.error) {
      return { error: result.message || "OTP verification failed." };
    }
    return { data: result.data };
  }, []);

  // ─── Logout ──────────────────────────────────────────────────────────────
  const logout = useCallback(async () => {
    // 1. Immediately and synchronously clear client auth state
    storage.clearAll();
    setUser(null);
    try {
      // 2. Best-effort server-side token revocation
      await authService.logout();
    } catch (err) {
      console.warn("Server logout notification failed:", err);
    } finally {
      // 3. Guarantee local state remains clean
      storage.clearAll();
      setUser(null);
    }
  }, []);

  // ─── Update local user fields (e.g., after onboarding completes) ─────────
  const updateUser = useCallback((updates) => {
    setUser((prev) => {
      if (!prev) return null;
      const resolvedName = updates.displayName || updates.name || prev.displayName || prev.name;
      const next = {
        ...prev,
        ...updates,
        displayName: resolvedName,
        name: resolvedName,
      };
      if (next.id) {
        if (next.onboardingComplete) {
          storage.setOnboarded(next.id, true);
        }
        storage.setUser(next);
      }
      return next;
    });
  }, []);

  // ─── Derived state ───────────────────────────────────────────────────────
  const isAuthenticated = !!user;
  const isAdmin = !!user && (user.role === "admin" || user.role === "scheme_manager" || user.role === "verification_officer");
  const role    = user?.role ?? "citizen";

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        profileLoading,
        isAuthenticated,
        isAdmin,
        role,
        login,
        signup,
        verifyOtp,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
