/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState } from "react";
import { hashPassword, verifyPassword, loginRateLimiter, signupRateLimiter } from "../utils/security";

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

  const MOCK_USERS = {
    "citizen@demo.com": {
      name: "Rajesh Patel",
      email: "citizen@demo.com",
      password: hashPassword("demo123"),
      role: "CITIZEN",
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
      name: "Sanjay Kumar (Admin)",
      email: "admin@schemebridge.gov.in",
      password: hashPassword("demo123"),
      role: "SUPER_ADMIN",
      onboardingComplete: true,
      department: "Govt. Scheme Evaluation Board",
    },
    "verify@schemebridge.gov.in": {
      name: "Amit Singh (Verification)",
      email: "verify@schemebridge.gov.in",
      password: hashPassword("demo123"),
      role: "VERIFICATION_OFFICER",
      onboardingComplete: true,
      department: "Document Verification Directorate",
    },
    "schemes@schemebridge.gov.in": {
      name: "Neha Sharma (Schemes)",
      email: "schemes@schemebridge.gov.in",
      password: hashPassword("demo123"),
      role: "SCHEME_MANAGER",
      onboardingComplete: true,
      department: "Ministry of Social Welfare",
    }
  };

  const isAuthenticated = !!user;
  const role = user?.role || null;
  const onboardingComplete = user?.onboardingComplete === true;
  const isAdmin = !!user && user.role !== "CITIZEN" && user.role !== "citizen";

  /** Persist user to both session key and per-email key */
  const _persist = (userObj) => {
    setUser(userObj);
    localStorage.setItem("schemebridge_user", JSON.stringify(userObj));
    localStorage.setItem(`schemebridge_user_${userObj.email}`, JSON.stringify(userObj));
  };

  /**
   * Real login — email must exist in mock users or localStorage.
   * Returns { user } on success or { error } on failure.
   */
  const login = (email, password) => {
    const targetEmail = email.trim().toLowerCase();

    // Rate limiting check
    const rateCheck = loginRateLimiter.canAttempt(targetEmail);
    if (!rateCheck.allowed) {
      return { error: `Too many login attempts. Please try again in ${rateCheck.retryAfter} seconds.` };
    }

    // Check seed mock users first
    if (MOCK_USERS[targetEmail]) {
      const mockUser = MOCK_USERS[targetEmail];
      if (!verifyPassword(password, mockUser.password)) {
        return { error: "Invalid password." };
      }
      // Return user without password for security
      const { password: _, ...safeUser } = mockUser;
      _persist(safeUser);
      return { user: safeUser };
    }

    const saved = localStorage.getItem(`schemebridge_user_${targetEmail}`);
    if (!saved) {
      return { error: "No account found with this email. Please sign up first." };
    }
    const parsedUser = JSON.parse(saved);
    if (parsedUser.password && !verifyPassword(password, parsedUser.password)) {
      return { error: "Invalid password." };
    }
    // Return user without password for security
    const { password: _, ...safeUser } = parsedUser;
    _persist(safeUser);
    return { user: safeUser };
  };

  /**
   * Quick demo login — creates preset citizen/admin if not already saved.
   * Returns the user object directly (always succeeds).
   */
  const quickLogin = (role) => {
    const email = role === "admin" ? "admin@schemebridge.gov.in" : "citizen@demo.com";
    const mockUser = MOCK_USERS[email];
    // Return user without password for security
    const { password: _, ...safeUser } = mockUser;
    _persist(safeUser);
    return safeUser;
  };

  /**
   * Signup — creates a brand-new citizen account.
   * Returns { user } on success or { error } if email already exists.
   */
  const signup = (name, email, password) => {
    const targetEmail = email.trim().toLowerCase();

    // Rate limiting check
    const rateCheck = signupRateLimiter.canAttempt(targetEmail);
    if (!rateCheck.allowed) {
      return { error: `Too many signup attempts. Please try again in ${rateCheck.retryAfter} seconds.` };
    }

    if (MOCK_USERS[targetEmail] || localStorage.getItem(`schemebridge_user_${targetEmail}`)) {
      return { error: "An account with this email already exists. Please log in." };
    }
    const newUser = {
      name: name.trim(),
      email: targetEmail,
      password: hashPassword(password),
      role: "CITIZEN",
      onboardingComplete: false,
      onboardingStep: 1,
    };
    // Return user without password for security
    const { password: _, ...safeUser } = newUser;
    _persist(newUser); // Store with password for verification
    return { user: safeUser };
  };

  /** Clear the active session (does NOT delete the per-email record). */
  const logout = () => {
    setUser(null);
    localStorage.removeItem("schemebridge_user");
  };

  /** Merge partial updates into the current user and re-persist. */
  const updateUser = (updates) => {
    setUser((prev) => {
      if (!prev) {
return null;
}
      const updated = { ...prev, ...updates };
      localStorage.setItem("schemebridge_user", JSON.stringify(updated));
      localStorage.setItem(`schemebridge_user_${updated.email}`, JSON.stringify(updated));
      return updated;
    });
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        role,
        onboardingComplete,
        isAdmin,
        login,
        quickLogin,
        signup,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
