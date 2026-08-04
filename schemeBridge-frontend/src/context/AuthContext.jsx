/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState } from "react";
import authService from "@services/authService";
import { hashPassword, verifyPassword, isValidEmail, checkPasswordStrength, loginRateLimiter, signupRateLimiter } from "../utils/security";

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

  const ADMIN_EMAILS = [
    "admin@schemebridge.gov.in",
    "verify@schemebridge.gov.in",
    "schemes@schemebridge.gov.in",
  ];

  const MOCK_USERS = {
    "citizen@demo.com": {
      id: "CIT-1001",
      name: "Rajesh Patel",
      fullName: "Rajesh Patel",
      email: "citizen@demo.com",
      password: hashPassword("demo123"),
      role: "citizen",
      status: "ACTIVE",
      onboardingComplete: true,
      onboardingStep: 3,
      age: 32,
      gender: "Male",
      occupation: "Farmer",
      annualIncome: 180000,
      caste: "OBC",
      state: "Gujarat",
    },
    "admin@schemebridge.gov.in": {
      id: "ADM-1001",
      name: "Sanjay Kumar",
      fullName: "Sanjay Kumar",
      email: "admin@schemebridge.gov.in",
      password: hashPassword("Admin@123"),
      role: "super_admin",
      status: "ACTIVE",
      onboardingComplete: true,
      department: "Govt. Scheme Evaluation Board",
    },
  };

  const ADMIN_ROLES = ["super_admin", "verification_officer", "scheme_manager"];

  const isAuthenticated = !!user;
  const role = user?.role || (user?.roles && user?.roles[0]) || "citizen";
  const status = user?.status || "PENDING_VERIFICATION";
  const onboardingComplete = user?.onboardingComplete === true || user?.onboardingCompleted === true;
  const isAdmin = !!user && (ADMIN_ROLES.includes(role) || ADMIN_ROLES.includes(role?.toLowerCase()));

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
      // authService may return { user, token, refreshToken } (mock) or { accessToken, user, refreshToken } (api)
      const token = res?.accessToken || res?.token || null;
      const apiUser = res?.user || res;
      if (token) {
        try {
          localStorage.setItem("schemebridge_token", token);
          // Persist refresh token if provided
          const refresh = res?.refreshToken || res?.refresh_token || res?.refreshTokenString || null;
          if (refresh) {
            try {
              localStorage.setItem("schemebridge_refresh_token", refresh);
            } catch (e) {
              console.warn("Failed to persist refresh token:", e);
            }
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
        return { user: safeUser };
      }
    } catch (err) {
      console.warn("authService failed, falling back to local sandbox:", err?.message || err);
    }

    // Fallback Mock authentication
    if (MOCK_USERS[targetEmail]) {
      const mockUser = MOCK_USERS[targetEmail];
      if (!verifyPassword(password, mockUser.password)) {
        return { error: "Invalid email or password." };
      }
      const { password: _, ...safeUser } = mockUser;
      _persist(safeUser);
      return { user: safeUser };
    }

    const saved = localStorage.getItem(`schemebridge_user_${targetEmail}`);
    if (!saved) {
      return { error: "Account not found. Please create a new account." };
    }
    const parsedUser = JSON.parse(saved);
    if (parsedUser.password && !verifyPassword(password, parsedUser.password)) {
      return { error: "Invalid email or password." };
    }

    if (parsedUser.status === "SUSPENDED") {
      return { error: "Your account has been suspended. Please contact support." };
    }

    const { password: _, ...safeUser } = parsedUser;
    _persist(safeUser);
    return { user: safeUser };
  };

  const quickLogin = (roleType) => {
    const email = roleType === "admin" ? "admin@schemebridge.gov.in" : "citizen@demo.com";
    const mockUser = MOCK_USERS[email];
    const { password: _, ...safeUser } = mockUser;
    _persist(safeUser);
    return safeUser;
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

    // Try backend REST API first
    try {
      const response = await fetch("http://localhost:8080/api/v1/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          fullName: name.trim(),
          email: targetEmail,
          password: password,
          role: "CITIZEN",
        }),
      });

      if (response.ok) {
        const json = await response.json();
        if (json.success && json.data) {
          const apiUser = json.data;
          const safeUser = {
            id: apiUser.id,
            name: apiUser.fullName,
            fullName: apiUser.fullName,
            email: apiUser.email,
            phoneNumber: apiUser.phoneNumber,
            role: "citizen",
            status: apiUser.status || "PENDING_VERIFICATION",
            verificationMethod: null,
            onboardingComplete: false,
            onboardingStep: 1,
          };
          _persist(safeUser);
          return { user: safeUser };
        }
      }
    } catch (err) {
      console.warn("Backend API offline, using local client signup sandbox:", err.message);
    }

    // Fallback Mock Signup
    const citizenCount = Object.keys(localStorage).filter(k => k.startsWith("schemebridge_user_")).length + 2;
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

    localStorage.setItem(`schemebridge_user_${targetEmail}`, JSON.stringify(newUser));
    const { password: _, ...safeUser } = newUser;
    _persist(safeUser);
    return { user: safeUser };
  };

  const sendEmailOtp = async (email) => {
    const targetEmail = (email || user?.email)?.trim().toLowerCase();
    try {
      const response = await fetch("http://localhost:8080/api/v1/auth/send-email-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: targetEmail }),
      });
      const json = await response.json();
      if (!response.ok || !json.success) {
        return { error: json.message || "Failed to send Email OTP." };
      }
      updateUser({ verificationMethod: "EMAIL" });
      return json.data || { message: "OTP sent successfully" };
    } catch (err) {
      console.warn("Backend API offline, simulating Email OTP:", err.message);
      updateUser({ verificationMethod: "EMAIL" });
      return { message: "Simulated Email OTP sent to " + targetEmail, simulatedOtp: "123456" };
    }
  };

  const sendPhoneOtp = async (email, phoneNumber) => {
    const targetEmail = (email || user?.email)?.trim().toLowerCase();
    try {
      const response = await fetch("http://localhost:8080/api/v1/auth/send-phone-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: targetEmail, phoneNumber }),
      });
      const json = await response.json();
      if (!response.ok || !json.success) {
        return { error: json.message || "Failed to send Mobile OTP." };
      }
      updateUser({ verificationMethod: "MOBILE", phoneNumber });
      return json.data || { message: "OTP sent successfully" };
    } catch (err) {
      console.warn("Backend API offline, simulating Mobile OTP:", err.message);
      updateUser({ verificationMethod: "MOBILE", phoneNumber });
      return { message: "Simulated Mobile OTP sent to " + phoneNumber, simulatedOtp: "123456" };
    }
  };

  const verifyOtp = async (email, otp, verificationMethod) => {
    const targetEmail = (email || user?.email)?.trim().toLowerCase();
    try {
      const response = await fetch("http://localhost:8080/api/v1/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: targetEmail, otp, verificationMethod }),
      });
      const json = await response.json();
      if (!response.ok || !json.success) {
        return { error: json.message || "Invalid OTP code." };
      }
      const apiUser = json.data;
      updateUser({
        status: "ACTIVE",
        emailVerified: apiUser.emailVerified === true,
        phoneVerified: apiUser.phoneVerified === true,
        verificationMethod,
      });
      return { user: apiUser };
    } catch (err) {
      console.warn("Backend API offline, simulating OTP verification:", err.message);
      if (otp === "123456" || otp === "654321") {
        updateUser({
          status: "ACTIVE",
          emailVerified: verificationMethod === "EMAIL",
          phoneVerified: verificationMethod === "MOBILE",
          verificationMethod,
        });
        return { user: { ...user, status: "ACTIVE" } };
      }
      return { error: "Invalid OTP code. Sandbox code is 123456." };
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem("schemebridge_user");
    try {
      localStorage.removeItem("schemebridge_token");
      localStorage.removeItem("schemebridge_refresh_token");
    } catch (e) {
      // ignore
    }
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

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        role,
        status,
        onboardingComplete,
        isAdmin,
        login,
        quickLogin,
        signup,
        sendEmailOtp,
        sendPhoneOtp,
        verifyOtp,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
