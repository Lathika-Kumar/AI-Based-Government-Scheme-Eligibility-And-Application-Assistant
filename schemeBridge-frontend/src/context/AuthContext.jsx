/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useMemo, useState } from "react";
import authService from "@services/authService";
import { hashPassword, isValidEmail, checkPasswordStrength, loginRateLimiter, signupRateLimiter } from "../utils/security";

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem("schemebridge_user");
    return saved ? JSON.parse(saved) : null;
  });

  const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";

  const ADMIN_EMAILS = new Set([
    "admin@schemebridge.gov.in",
    "verify@schemebridge.gov.in",
    "schemes@schemebridge.gov.in",
  ]);

  const ADMIN_ROLES = new Set(["super_admin", "verification_officer", "scheme_manager"]);

  const isAuthenticated = !!user;
  const role = user?.role || user?.roles?.[0] || "citizen";
  const status = user?.status || "PENDING_VERIFICATION";
  const onboardingComplete = user?.onboardingComplete === true || user?.onboardingCompleted === true;
  const isAdmin = !!user && (ADMIN_ROLES.has(role) || ADMIN_ROLES.has(role?.toLowerCase()));

  const _persist = (userObj) => {
    setUser(userObj);
    localStorage.setItem("schemebridge_user", JSON.stringify(userObj));
    if (userObj.email) {
      localStorage.setItem(`schemebridge_user_${userObj.email}`, JSON.stringify(userObj));
    }
  };

  const login = async (email, password) => {
    const targetEmail = email.trim().toLowerCase();

    const rateCheck = loginRateLimiter.canAttempt(targetEmail);
    if (!rateCheck.allowed) {
      return { error: `Too many login attempts. Please try again in ${rateCheck.retryAfter} seconds.` };
    }

    // Use centralized auth service (falls back to sandbox when configured)
    try {
      const res = await authService.login({ email: targetEmail, password });
      // authService may return { user, token, accessToken, refreshToken } (mock/api)
      console.log("Login response from AuthContext.login:", res);
      const token = res?.accessToken || res?.token || null;
      const refreshToken = res?.refreshToken || res?.refresh_token || res?.refreshTokenString || null;
      const apiUser = res?.user || res;
      if (token) {
        try {
          localStorage.setItem("schemebridge_token", token);
          console.log("AuthContext persisted schemebridge_token:", localStorage.getItem("schemebridge_token"));
          if (refreshToken) {
            localStorage.setItem("schemebridge_refresh_token", refreshToken);
            console.log("AuthContext persisted schemebridge_refresh_token:", localStorage.getItem("schemebridge_refresh_token"));
          }
        } catch (e) {
          console.warn("Failed to persist auth token:", e);
        }
      }

      if (apiUser) {
        const safeUser = {
          id: apiUser.id,
          name: apiUser.fullName || apiUser.name,
          fullName: apiUser.fullName || apiUser.name,
          email: apiUser.email,
          phoneNumber: apiUser.phoneNumber,
          role: apiUser.roles ? apiUser.roles[0]?.toLowerCase() : apiUser.role || "citizen",
          status: apiUser.status || "ACTIVE",
          verificationMethod: apiUser.verificationMethod,
          onboardingComplete: apiUser.onboardingCompleted === true || apiUser.onboardingComplete === true,
          token,
        };

        if (safeUser.status === "SUSPENDED") {
          return { error: "Your account has been suspended. Please contact support." };
        }

        _persist(safeUser);
        return { user: safeUser, accessToken: token, refreshToken };
      }
    } catch (err) {
      console.warn("authService failed during login:", err?.message || err);
      if (!USE_MOCK) {
        return { error: "Login failed. Please try again." };
      }
    }

    if (!USE_MOCK) {
      return { error: "Unable to authenticate. Mock auth is disabled." };
    }

    return { error: "Account not found. Please create a new account." };
  };


  const signup = async (name, email, password) => {
    const targetEmail = email.trim().toLowerCase();

    const rateCheck = signupRateLimiter.canAttempt(targetEmail);
    if (!rateCheck.allowed) {
      return { error: `Too many signup attempts. Please try again in ${rateCheck.retryAfter} seconds.` };
    }

    if (!isValidEmail(targetEmail)) {
      return { error: "Please enter a valid email address." };
    }

    if (ADMIN_EMAILS.includes(targetEmail)) {
      return { error: "This email address is reserved for administrators and cannot be used to register." };
    }

    const strengthCheck = checkPasswordStrength(password);
    if (strengthCheck.score < 2) {
      return { error: strengthCheck.feedback[0] || "Please choose a stronger password (min 8 characters, uppercase, lowercase, and a number)." };
    }

    try {
      const res = await authService.register({ name: name.trim(), email: targetEmail, phone: user?.phoneNumber || "", password });
      const accessToken = res?.token || res?.accessToken || null;
      const refreshToken = res?.refreshToken || null;
      const apiUser = res?.user || res;

      if (accessToken) {
        localStorage.setItem("schemebridge_token", accessToken);
        if (refreshToken) {
          try {
            localStorage.setItem("schemebridge_refresh_token", refreshToken);
          } catch (e) {
            console.warn("Failed to persist refresh token:", e);
          }
        }
      }

      if (apiUser) {
        const safeUser = {
          id: apiUser.id,
          name: apiUser.fullName || apiUser.name,
          fullName: apiUser.fullName || apiUser.name,
          email: apiUser.email,
          phoneNumber: apiUser.phoneNumber,
          role: apiUser.roles ? apiUser.roles[0]?.toLowerCase() : apiUser.role || "citizen",
          status: apiUser.status || "PENDING_VERIFICATION",
          verificationMethod: apiUser.verificationMethod,
          onboardingComplete: apiUser.onboardingCompleted === true || apiUser.onboardingComplete === true,
          token: accessToken,
        };
        _persist(safeUser);
        return { user: safeUser };
      }
    } catch (err) {
      console.warn("authService.register failed, falling back to local signup sandbox:", err?.message || err);
    }

    const citizenCount = Object.keys(localStorage).filter((k) => k.startsWith("schemebridge_user_")).length + 2;
    const citizenId = `CIT-${String(citizenCount).padStart(5, "0")}`;

    const newUser = {
      id: citizenId,
      name: name.trim(),
      fullName: name.trim(),
      email: targetEmail,
      password: hashPassword(password),
      role: "citizen",
      status: "PENDING_VERIFICATION",
      verificationMethod: null,
      onboardingComplete: false,
      onboardingStep: 1,
    };

    const safeUser = { ...newUser };
    delete safeUser.password;
    localStorage.setItem(`schemebridge_user_${targetEmail}`, JSON.stringify(newUser));
    _persist(safeUser);
    return { user: safeUser };
  };

  const updateUser = (updates) => {
    setUser((prev) => {
      if (!prev) return null;
      const updated = { ...prev, ...updates };
      localStorage.setItem("schemebridge_user", JSON.stringify(updated));
      if (updated.email) {
        localStorage.setItem(`schemebridge_user_${updated.email}`, JSON.stringify(updated));
      }
      return updated;
    });
  };

  const sendEmailOtp = async (email) => {
    const targetEmail = (email || user?.email || "lathikakumar798@gmail.com").trim().toLowerCase();
    try {
      const response = await authService.sendEmailOtp(targetEmail);
      updateUser({ verificationMethod: "EMAIL" });
      return response;
    } catch (err) {
      if (USE_MOCK) {
        console.warn("authService.sendEmailOtp failed, falling back to local simulation:", err?.message || err);
        updateUser({ verificationMethod: "EMAIL" });
        return { message: "Simulated Email OTP sent to " + targetEmail, simulatedOtp: "123456" };
      }
      throw err;
    }
  };


  const verifyOtp = async (email, otp, verificationMethod) => {
    const targetEmail = (email || user?.email || "lathikakumar798@gmail.com").trim().toLowerCase();
    try {
      const response = await authService.verifyOtp(targetEmail, otp, verificationMethod);
      if (response?.error) {
        return response;
      }

      const apiUser = response?.user || response;
      if (!apiUser?.email) {
        return { error: "Invalid verification response from server." };
      }

      const accessToken = response?.accessToken || response?.token || localStorage.getItem("schemebridge_token");
      const refreshToken = response?.refreshToken || response?.refresh_token || localStorage.getItem("schemebridge_refresh_token");
      if (accessToken) {
        try {
          localStorage.setItem("schemebridge_token", accessToken);
        } catch (e) {
          console.warn("Failed to persist auth token after OTP verification:", e);
        }
      }
      if (refreshToken) {
        try {
          localStorage.setItem("schemebridge_refresh_token", refreshToken);
        } catch (e) {
          console.warn("Failed to persist refresh token after OTP verification:", e);
        }
      }

      updateUser({
        ...apiUser,
        status: apiUser.status || "ACTIVE",
        emailVerified: apiUser.emailVerified === true || user?.emailVerified === true,
        verificationMethod,
        onboardingComplete: apiUser.onboardingCompleted === true || apiUser.onboardingComplete === true || user?.onboardingComplete === true,
        token: accessToken,
      });

      return { user: apiUser };
    } catch (err) {
      if (USE_MOCK) {
        console.warn("authService.verifyOtp failed, falling back to local simulation:", err?.message || err);
        if (otp === "123456" || otp === "654321") {
          updateUser({
            status: "ACTIVE",
            emailVerified: verificationMethod === "EMAIL",
            verificationMethod,
          });
          return { user: { ...user, status: "ACTIVE" } };
        }
        return { error: "Invalid OTP code. Sandbox code is 123456." };
      }
      throw err;
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem("schemebridge_user");
    try {
      localStorage.removeItem("schemebridge_token");
      localStorage.removeItem("schemebridge_refresh_token");
    } catch (e) {
      console.warn("Failed to remove auth tokens from localStorage:", e);
    }
  };

  const authValue = useMemo(
    () => ({
      user,
      isAuthenticated,
      role,
      status,
      onboardingComplete,
      isAdmin,
      login,
      signup,
      sendEmailOtp,
      verifyOtp,
      logout,
      updateUser,
    }),
    [user, isAuthenticated, role, status, onboardingComplete, isAdmin]
  );

  return (
    <AuthContext.Provider value={authValue}>
      {children}
    </AuthContext.Provider>
  );
};
