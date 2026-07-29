/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState } from "react";
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

  // ── Predefined Administrator Accounts ────────────────────────────────────────
  // These are seeded at startup and do not require registration.
  const ADMIN_EMAILS = [
    "admin@schemebridge.gov.in",
    "verify@schemebridge.gov.in",
    "schemes@schemebridge.gov.in",
  ];

  const MOCK_USERS = {
    // Demo citizen (for quick-login sandbox)
    "citizen@demo.com": {
      id: "CIT-1001",
      name: "Rajesh Patel",
      email: "citizen@demo.com",
      password: hashPassword("demo123"),
      role: "citizen",
      onboardingComplete: true,
      onboardingStep: 3,
      age: 32,
      gender: "Male",
      occupation: "Farmer",
      annualIncome: 180000,
      caste: "OBC",
      state: "Gujarat",
    },
    // Super Administrator
    "admin@schemebridge.gov.in": {
      id: "ADM-1001",
      name: "Sanjay Kumar",
      email: "admin@schemebridge.gov.in",
      password: hashPassword("Admin@123"),
      role: "super_admin",
      onboardingComplete: true,
      department: "Govt. Scheme Evaluation Board",
    },
    // Verification Officer
    "verify@schemebridge.gov.in": {
      id: "ADM-1002",
      name: "Amit Singh",
      email: "verify@schemebridge.gov.in",
      password: hashPassword("Verify@123"),
      role: "verification_officer",
      onboardingComplete: true,
      department: "Document Verification Directorate",
    },
    // Scheme Manager
    "schemes@schemebridge.gov.in": {
      id: "ADM-1003",
      name: "Neha Sharma",
      email: "schemes@schemebridge.gov.in",
      password: hashPassword("Scheme@123"),
      role: "scheme_manager",
      onboardingComplete: true,
      department: "Ministry of Social Welfare",
    }
  };

  // ── Derived auth state ────────────────────────────────────────────────────────
  const ADMIN_ROLES = ["super_admin", "verification_officer", "scheme_manager"];

  const isAuthenticated = !!user;
  const role = user?.role || null;
  const onboardingComplete = user?.onboardingComplete === true;
  const isAdmin = !!user && ADMIN_ROLES.includes(user.role);

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

    // Check seed mock users first (predefined admin + demo citizen accounts)
    if (MOCK_USERS[targetEmail]) {
      const mockUser = MOCK_USERS[targetEmail];
      if (!verifyPassword(password, mockUser.password)) {
        return { error: "Invalid email or password." };
      }
      // Return user without password for security
      const { password: _, ...safeUser } = mockUser;
      _persist(safeUser);
      return { user: safeUser };
    }

    // Check registered citizen accounts in localStorage
    const saved = localStorage.getItem(`schemebridge_user_${targetEmail}`);
    if (!saved) {
      return { error: "Account not found. Please create a new account." };
    }
    const parsedUser = JSON.parse(saved);
    if (parsedUser.password && !verifyPassword(password, parsedUser.password)) {
      return { error: "Invalid email or password." };
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
  const quickLogin = (roleType) => {
    const email = roleType === "admin" ? "admin@schemebridge.gov.in" : "citizen@demo.com";
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

    // Validate email format
    if (!isValidEmail(targetEmail)) {
      return { error: "Please enter a valid email address." };
    }

    // Prevent registration using predefined admin email addresses
    if (ADMIN_EMAILS.includes(targetEmail)) {
      return { error: "This email address is reserved for administrators and cannot be used to register." };
    }

    // Prevent duplicate registrations
    if (MOCK_USERS[targetEmail] || localStorage.getItem(`schemebridge_user_${targetEmail}`)) {
      return { error: "An account with this email already exists. Please sign in instead." };
    }

    // Validate password strength (minimum score of 2)
    const strengthCheck = checkPasswordStrength(password);
    if (strengthCheck.score < 2) {
      return { error: strengthCheck.feedback[0] || "Please choose a stronger password (min 8 characters, uppercase, lowercase, and a number)." };
    }

    // Generate a unique citizen ID
    const citizenCount = Object.keys(localStorage).filter(k => k.startsWith("schemebridge_user_")).length + 2;
    const citizenId = `CIT-${String(citizenCount).padStart(5, "0")}`;

    const newUser = {
      id: citizenId,
      name: name.trim(),
      email: targetEmail,
      password: hashPassword(password),
      role: "citizen",
      onboardingComplete: false,
      onboardingStep: 1,
    };
    // Store full record (with password) for future logins
    localStorage.setItem(`schemebridge_user_${targetEmail}`, JSON.stringify(newUser));
    // Persist session without password
    const { password: _, ...safeUser } = newUser;
    _persist(safeUser);
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
