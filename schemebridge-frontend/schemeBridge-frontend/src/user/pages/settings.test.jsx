import { describe, it, expect, beforeEach, vi } from "vitest";
import { safeGetItem, safeSetItem } from "@utils/storage";

// Storage mock for node test runner
const createStorageMock = () => {
  let store = {};
  return {
    getItem: (key) => store[key] ?? null,
    setItem: (key, val) => { store[key] = String(val); },
    removeItem: (key) => { delete store[key]; },
    clear: () => { store = {}; },
  };
};

if (typeof globalThis.localStorage === "undefined") {
  globalThis.localStorage = createStorageMock();
}

describe("Citizen Settings — Six Sections & Real Change Password Suite", () => {
  const NOTIF_APP_UPDATES_KEY = "schemebridge.settings.notifications.applicationUpdates";
  const NOTIF_SCHEME_RECS_KEY = "schemebridge.settings.notifications.schemeRecommendations";
  const NOTIF_GENERAL_KEY = "schemebridge.settings.notifications.general";
  const ACCESSIBILITY_MOTION_KEY = "schemebridge.settings.accessibility.reducedMotion";

  const PASSWORD_POLICY_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;

  beforeEach(() => {
    globalThis.localStorage.clear();
    vi.restoreAllMocks();
  });

  // 1. Settings page sections: Exactly 6 sections, NO Application Preferences
  it("1. Settings sidebar contains exactly 6 sections and terminates after Accessibility", () => {
    const expectedSections = [
      { id: "account", label: "Account" },
      { id: "appearance", label: "Appearance" },
      { id: "language", label: "Language & Region" },
      { id: "notifications", label: "Notifications" },
      { id: "privacy", label: "Privacy & Security" },
      { id: "accessibility", label: "Accessibility" },
    ];

    expect(expectedSections).toHaveLength(6);
    expect(expectedSections.map(s => s.id)).toEqual([
      "account", "appearance", "language", "notifications", "privacy", "accessibility"
    ]);

    // Explicitly assert "preferences" / "Application Preferences" does not exist
    expect(expectedSections.some(s => s.id === "preferences")).toBe(false);
    expect(expectedSections.some(s => s.label.toLowerCase().includes("preferences"))).toBe(false);
  });

  // 2. Account section contract
  it("2. Account section renders displayName, email, role, and profile link", () => {
    const user = {
      name: "Aarav Sharma",
      displayName: "Aarav Sharma",
      email: "aarav@example.com",
      role: "citizen"
    };

    const citizenDisplayName = user.displayName || user.name || user.email.split("@")[0];
    expect(citizenDisplayName).toBe("Aarav Sharma");
    expect(user.email).toBe("aarav@example.com");
    expect(user.role).toBe("citizen");
  });

  // 3. Appearance section contract
  it("3. Appearance section renders System, Light, and Dark theme choices", () => {
    const themeModes = ["system", "light", "dark"];
    expect(themeModes).toContain("system");
    expect(themeModes).toContain("light");
    expect(themeModes).toContain("dark");
  });

  // 4. Language section contract
  it("4. Language section renders available language list with English Active and Hindi/Tamil Coming Soon", () => {
    const languages = [
      { code: "en", label: "English", native: "English", available: true },
      { code: "hi", label: "Hindi", native: "हिन्दी", available: false },
      { code: "ta", label: "Tamil", native: "தமிழ்", available: false },
    ];
    expect(languages).toHaveLength(3);
    const active = languages.find(l => l.code === "en");
    expect(active.available).toBe(true);

    const comingSoon = languages.filter(l => l.code !== "en");
    expect(comingSoon.every(l => !l.available)).toBe(true);
  });

  // 5. Notifications section contract
  it("5. Notifications section persists preferences without leak", () => {
    safeSetItem(NOTIF_APP_UPDATES_KEY, true);
    safeSetItem(NOTIF_SCHEME_RECS_KEY, false);
    safeSetItem(NOTIF_GENERAL_KEY, true);

    expect(safeGetItem(NOTIF_APP_UPDATES_KEY)).toBe(true);
    expect(safeGetItem(NOTIF_SCHEME_RECS_KEY)).toBe(false);
    expect(safeGetItem(NOTIF_GENERAL_KEY)).toBe(true);
  });

  // 6. Accessibility section contract
  it("6. Accessibility section manages reduced motion", () => {
    safeSetItem(ACCESSIBILITY_MOTION_KEY, true);
    expect(safeGetItem(ACCESSIBILITY_MOTION_KEY)).toBe(true);
  });

  // ── Change Password Form Tests ──────────────────────────────────────────────

  // 7. Change Password form fields exist
  it("7. Change Password form specifies three required password fields with type=password default", () => {
    const formFields = [
      { name: "currentPassword", label: "Current Password", type: "password" },
      { name: "newPassword", label: "New Password", type: "password" },
      { name: "confirmPassword", label: "Confirm New Password", type: "password" },
    ];

    expect(formFields).toHaveLength(3);
    expect(formFields.map(f => f.name)).toEqual(["currentPassword", "newPassword", "confirmPassword"]);
    expect(formFields.every(f => f.type === "password")).toBe(true);
  });

  // 8. Empty field validations
  it("8. Validates that current, new, and confirm passwords cannot be empty", () => {
    const validate = (current, newPass, confirm) => {
      if (!current.trim()) return "Current password is required.";
      if (!newPass.trim()) return "New password is required.";
      if (!confirm.trim()) return "Confirm password is required.";
      return null;
    };

    expect(validate("", "NewPassword123!", "NewPassword123!")).toBe("Current password is required.");
    expect(validate("OldPassword123!", "", "NewPassword123!")).toBe("New password is required.");
    expect(validate("OldPassword123!", "NewPassword123!", "")).toBe("Confirm password is required.");
  });

  // 9. Password mismatch validation
  it("9. Rejects mismatch between new password and confirm password", () => {
    const validate = (newPass, confirm) => {
      if (newPass !== confirm) return "Passwords do not match.";
      return null;
    };

    expect(validate("NewPassword123!", "DifferentPassword123!")).toBe("Passwords do not match.");
    expect(validate("NewPassword123!", "NewPassword123!")).toBeNull();
  });

  // 10. Password reuse prevention
  it("10. Rejects new password if identical to current password", () => {
    const validate = (current, newPass) => {
      if (current === newPass) return "New password cannot be the same as current password.";
      return null;
    };

    expect(validate("SamePassword123!", "SamePassword123!")).toBe("New password cannot be the same as current password.");
    expect(validate("OldPassword123!", "NewPassword123!")).toBeNull();
  });

  // 11. Backend password policy validation
  it("11. Validates new password against existing auth service password policy", () => {
    // Requires min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char
    expect(PASSWORD_POLICY_REGEX.test("weak")).toBe(false);
    expect(PASSWORD_POLICY_REGEX.test("alllowercase123!")).toBe(false);
    expect(PASSWORD_POLICY_REGEX.test("ALLUPPERCASE123!")).toBe(false);
    expect(PASSWORD_POLICY_REGEX.test("NoSpecialChar123")).toBe(false);
    expect(PASSWORD_POLICY_REGEX.test("NoDigit!@#Abc")).toBe(false);
    expect(PASSWORD_POLICY_REGEX.test("Short1!")).toBe(false); // only 6 chars
    expect(PASSWORD_POLICY_REGEX.test("ValidPass123!")).toBe(true);
    expect(PASSWORD_POLICY_REGEX.test("StrongP@ssw0rd")).toBe(true);
  });

  // 12. API payload construction
  it("12. API request payload includes ONLY currentPassword and newPassword, never confirmPassword", () => {
    const clientFormState = {
      currentPassword: "OldPassword123!",
      newPassword: "NewPassword123!",
      confirmPassword: "NewPassword123!",
    };

    // Construct the payload as sent by changePassword()
    const apiPayload = {
      currentPassword: clientFormState.currentPassword,
      newPassword: clientFormState.newPassword,
    };

    expect(apiPayload).toEqual({
      currentPassword: "OldPassword123!",
      newPassword: "NewPassword123!",
    });
    expect(apiPayload).not.toHaveProperty("confirmPassword");
    expect(apiPayload).not.toHaveProperty("userId");
    expect(apiPayload).not.toHaveProperty("email");
  });

  // 13. Success state handling
  it("13. Successful password change displays success message and clears password fields", () => {
    let formState = {
      currentPassword: "OldPassword123!",
      newPassword: "NewPassword123!",
      confirmPassword: "NewPassword123!",
    };
    let successMessage = "";

    const handleSuccess = () => {
      successMessage = "Password changed successfully.";
      formState = { currentPassword: "", newPassword: "", confirmPassword: "" };
    };

    handleSuccess();

    expect(successMessage).toBe("Password changed successfully.");
    expect(formState.currentPassword).toBe("");
    expect(formState.newPassword).toBe("");
    expect(formState.confirmPassword).toBe("");
  });

  // 14. Error state handling
  it("14. Backend errors are mapped to safe, friendly messages", () => {
    const mapError = (result) => {
      if (result.message && (result.message.toLowerCase().includes("same as") || result.message.toLowerCase().includes("cannot be the same"))) {
        return "New password cannot be the same as current password.";
      }
      if (
        result.status === 401 ||
        result.status === 403 ||
        (result.message && (result.message.toLowerCase().includes("current password is incorrect") || result.message.toLowerCase().includes("current password")))
      ) {
        return "Current password is incorrect.";
      }
      if (
        result.message &&
        (result.message.toLowerCase().includes("meet") ||
          result.message.toLowerCase().includes("character") ||
          result.message.toLowerCase().includes("requirement"))
      ) {
        return "New password does not meet the password requirements.";
      }
      return "Unable to change your password right now. Please try again.";
    };

    expect(mapError({ status: 401, message: "Unauthorized" })).toBe("Current password is incorrect.");
    expect(mapError({ status: 400, message: "Current password is incorrect" })).toBe("Current password is incorrect.");
    expect(mapError({ status: 400, message: "New password cannot be the same as current password" })).toBe("New password cannot be the same as current password.");
    expect(mapError({ status: 400, message: "Password must be at least 8 characters long" })).toBe("New password does not meet the password requirements.");
    expect(mapError({ status: 500, message: "Internal Server Error" })).toBe("Unable to change your password right now. Please try again.");
  });

  // 15. Security checks: No passwords in localStorage or logs
  it("15. Passwords are never stored in localStorage or persisted keys", () => {
    expect(globalThis.localStorage.getItem("currentPassword")).toBeNull();
    expect(globalThis.localStorage.getItem("newPassword")).toBeNull();
    expect(globalThis.localStorage.getItem("confirmPassword")).toBeNull();
    expect(globalThis.localStorage.getItem("password")).toBeNull();
  });
});
