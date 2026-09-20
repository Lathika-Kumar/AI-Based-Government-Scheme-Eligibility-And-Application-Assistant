import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useTheme } from "@context/ThemeContext";
import { safeGetItem, safeSetItem } from "@utils/storage";
import { changePassword } from "@services/authService";
import {
  UserCircle,
  Palette,
  Globe,
  Bell,
  Shield,
  Accessibility,
  ChevronRight,
  Sun,
  Moon,
  Monitor,
  Check,
  ExternalLink,
  Lock,
  Eye,
  EyeOff,
  Info,
  Mail,
  Smartphone,
  AlertTriangle
} from "lucide-react";

// ── Persistence keys ────────────────────────────────────────────────────────
const NOTIF_APP_UPDATES_KEY = "schemebridge.settings.notifications.applicationUpdates";
const NOTIF_SCHEME_RECS_KEY = "schemebridge.settings.notifications.schemeRecommendations";
const NOTIF_GENERAL_KEY = "schemebridge.settings.notifications.general";
const ACCESSIBILITY_MOTION_KEY = "schemebridge.settings.accessibility.reducedMotion";

function loadSettings() {
  return {
    notifications: {
      applicationUpdates: safeGetItem(NOTIF_APP_UPDATES_KEY, true),
      schemeRecommendations: safeGetItem(NOTIF_SCHEME_RECS_KEY, true),
      generalNotifications: safeGetItem(NOTIF_GENERAL_KEY, true),
    },
    accessibility: {
      reducedMotion: safeGetItem(ACCESSIBILITY_MOTION_KEY, false),
    },
  };
}

// ── Sections definition (Exactly 6 citizen sections) ──────────────────────────
const SECTIONS = [
  { id: "account", label: "Account", icon: UserCircle },
  { id: "appearance", label: "Appearance", icon: Palette },
  { id: "language", label: "Language & Region", icon: Globe },
  { id: "notifications", label: "Notifications", icon: Bell },
  { id: "privacy", label: "Privacy & Security", icon: Shield },
  { id: "accessibility", label: "Accessibility", icon: Accessibility },
];

// ── Password Policy Regex (matches backend SignupRequest policy) ─────────────
const PASSWORD_POLICY_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;

// ── Toggle component ─────────────────────────────────────────────────────────
function Toggle({ enabled, onChange, label, disabled = false }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={enabled}
      aria-label={label}
      disabled={disabled}
      onClick={() => onChange(!enabled)}
      className={`relative w-11 h-6 rounded-full transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:focus:ring-offset-slate-900 ${
        disabled
          ? "bg-slate-200 dark:bg-slate-800 cursor-not-allowed opacity-60"
          : enabled
          ? "bg-indigo-600"
          : "bg-slate-300 dark:bg-slate-700"
      }`}
    >
      <span
        className={`block w-4 h-4 bg-white rounded-full shadow transition-transform duration-200 ${
          enabled ? "translate-x-[22px]" : "translate-x-[3px]"
        } mt-[4px]`}
      />
    </button>
  );
}

// ── Setting row ──────────────────────────────────────────────────────────────
function SettingRow({ icon: Icon, title, description, children, badge }) {
  return (
    <div className="flex items-start sm:items-center justify-between gap-4 py-4 border-b border-slate-100 dark:border-slate-800 last:border-b-0">
      <div className="flex items-start gap-3 min-w-0">
        {Icon && (
          <div className="mt-0.5 shrink-0">
            <Icon className="h-4.5 w-4.5 text-slate-400 dark:text-slate-500" />
          </div>
        )}
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-slate-800 dark:text-slate-200">{title}</span>
            {badge}
          </div>
          {description && (
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 leading-relaxed">{description}</p>
          )}
        </div>
      </div>
      <div className="shrink-0">{children}</div>
    </div>
  );
}

// ── Section card wrapper ─────────────────────────────────────────────────────
function SectionCard({ title, description, children }) {
  return (
    <div className="space-y-1">
      <h3 className="text-base font-bold text-slate-900 dark:text-white">{title}</h3>
      {description && (
        <p className="text-sm text-slate-500 dark:text-slate-400">{description}</p>
      )}
      <div className="mt-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl px-5">
        {children}
      </div>
    </div>
  );
}

// ── Change Password Form Component ───────────────────────────────────────────
function ChangePasswordForm() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleReset = () => {
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setError("");
    setSuccess("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!currentPassword.trim()) {
      setError("Current password is required.");
      return;
    }
    if (!newPassword.trim()) {
      setError("New password is required.");
      return;
    }
    if (!confirmPassword.trim()) {
      setError("Confirm password is required.");
      return;
    }
    if (newPassword === currentPassword) {
      setError("New password cannot be the same as current password.");
      return;
    }
    if (!PASSWORD_POLICY_REGEX.test(newPassword)) {
      setError("New password does not meet the password requirements.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);

    try {
      const result = await changePassword({ currentPassword, newPassword });

      if (result.error) {
        if (result.message && (result.message.toLowerCase().includes("same as") || result.message.toLowerCase().includes("cannot be the same"))) {
          setError("New password cannot be the same as current password.");
        } else if (
          result.status === 401 ||
          result.status === 403 ||
          (result.message && (result.message.toLowerCase().includes("current password is incorrect") || result.message.toLowerCase().includes("current password")))
        ) {
          setError("Current password is incorrect.");
        } else if (
          result.message &&
          (result.message.toLowerCase().includes("meet") ||
            result.message.toLowerCase().includes("character") ||
            result.message.toLowerCase().includes("requirement"))
        ) {
          setError("New password does not meet the password requirements.");
        } else {
          setError("Unable to change your password right now. Please try again.");
        }
        return;
      }

      setSuccess("Password changed successfully.");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setError("");
    } catch {
      setError("Unable to change your password right now. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="mt-4 space-y-3.5 max-w-md" noValidate>
      {/* Notifications */}
      {error && (
        <div
          role="alert"
          aria-live="polite"
          className="p-3 text-xs font-medium text-rose-700 dark:text-rose-300 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 rounded-lg flex items-start gap-2"
        >
          <AlertTriangle className="h-4 w-4 shrink-0 text-rose-500 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div
          role="status"
          aria-live="polite"
          className="p-3 text-xs font-medium text-emerald-700 dark:text-emerald-300 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-lg flex items-start gap-2"
        >
          <Check className="h-4 w-4 shrink-0 text-emerald-500 mt-0.5" />
          <span>{success}</span>
        </div>
      )}

      {/* Current Password */}
      <div>
        <label
          htmlFor="currentPassword"
          className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1"
        >
          Current Password
        </label>
        <div className="relative">
          <input
            id="currentPassword"
            name="currentPassword"
            type={showCurrent ? "text" : "password"}
            value={currentPassword}
            onChange={(e) => {
              setCurrentPassword(e.target.value);
              if (error) setError("");
            }}
            disabled={isSubmitting}
            placeholder="•••••••••••••••"
            autoComplete="current-password"
            className="w-full text-sm bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white rounded-lg px-3 py-2 pr-10 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-colors placeholder:text-slate-400"
          />
          <button
            type="button"
            aria-label={showCurrent ? "Hide current password" : "Show current password"}
            onClick={() => setShowCurrent(!showCurrent)}
            className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 focus:outline-none"
          >
            {showCurrent ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
      </div>

      {/* New Password */}
      <div>
        <label
          htmlFor="newPassword"
          className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1"
        >
          New Password
        </label>
        <div className="relative">
          <input
            id="newPassword"
            name="newPassword"
            type={showNew ? "text" : "password"}
            value={newPassword}
            onChange={(e) => {
              setNewPassword(e.target.value);
              if (error) setError("");
            }}
            disabled={isSubmitting}
            placeholder="•••••••••••••••"
            autoComplete="new-password"
            className="w-full text-sm bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white rounded-lg px-3 py-2 pr-10 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-colors placeholder:text-slate-400"
          />
          <button
            type="button"
            aria-label={showNew ? "Hide new password" : "Show new password"}
            onClick={() => setShowNew(!showNew)}
            className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 focus:outline-none"
          >
            {showNew ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
        <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1">
          Must be at least 8 characters with uppercase, lowercase, number, and special character (@$!%*?&).
        </p>
      </div>

      {/* Confirm New Password */}
      <div>
        <label
          htmlFor="confirmPassword"
          className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1"
        >
          Confirm New Password
        </label>
        <div className="relative">
          <input
            id="confirmPassword"
            name="confirmPassword"
            type={showConfirm ? "text" : "password"}
            value={confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value);
              if (error) setError("");
            }}
            disabled={isSubmitting}
            placeholder="•••••••••••••••"
            autoComplete="new-password"
            className="w-full text-sm bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 text-slate-900 dark:text-white rounded-lg px-3 py-2 pr-10 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-colors placeholder:text-slate-400"
          />
          <button
            type="button"
            aria-label={showConfirm ? "Hide confirm password" : "Show confirm password"}
            onClick={() => setShowConfirm(!showConfirm)}
            className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 focus:outline-none"
          >
            {showConfirm ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-2.5 pt-2">
        <button
          type="button"
          onClick={handleReset}
          disabled={isSubmitting || (!currentPassword && !newPassword && !confirmPassword && !error && !success)}
          className="px-3.5 py-1.5 text-xs font-semibold text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={isSubmitting}
          className="px-4 py-1.5 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 rounded-lg transition shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1.5"
        >
          {isSubmitting && <span className="inline-block w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin" />}
          <span>{isSubmitting ? "Updating..." : "Change Password"}</span>
        </button>
      </div>
    </form>
  );
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN SETTINGS COMPONENT
// ═══════════════════════════════════════════════════════════════════════════════
export default function Settings() {
  const { user } = useAuth();
  const { theme: themePreference, setTheme, isDark } = useTheme();
  const navigate = useNavigate();

  const [activeSection, setActiveSection] = useState("account");
  const [settings, setSettings] = useState(loadSettings);

  const citizenDisplayName = user?.displayName || user?.name || `${user?.firstName || ""} ${user?.lastName || ""}`.trim() || (user?.email ? user.email.split("@")[0] : "");

  // Persist settings whenever they change
  useEffect(() => {
    safeSetItem(NOTIF_APP_UPDATES_KEY, settings.notifications.applicationUpdates);
    safeSetItem(NOTIF_SCHEME_RECS_KEY, settings.notifications.schemeRecommendations);
    safeSetItem(NOTIF_GENERAL_KEY, settings.notifications.generalNotifications);
    safeSetItem(ACCESSIBILITY_MOTION_KEY, settings.accessibility.reducedMotion);
  }, [settings]);

  // Reduced motion: apply class to document root and respect OS preference
  useEffect(() => {
    const isMotionReduced = settings.accessibility?.reducedMotion;
    if (isMotionReduced) {
      document.documentElement.classList.add("reduce-motion");
    } else {
      document.documentElement.classList.remove("reduce-motion");
    }
  }, [settings.accessibility?.reducedMotion]);

  const updateNotification = useCallback((key, value) => {
    setSettings((prev) => ({
      ...prev,
      notifications: { ...prev.notifications, [key]: value },
    }));
  }, []);

  const updateAccessibility = useCallback((key, value) => {
    setSettings((prev) => ({
      ...prev,
      accessibility: { ...prev.accessibility, [key]: value },
    }));
  }, []);

  // ── Section renderers ────────────────────────────────────────────────────────

  const renderAccount = () => (
    <SectionCard title="Account" description="Your profile and account information.">
      <SettingRow icon={UserCircle} title="Display Name" description="Your registered name on SchemeBridge.">
        <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
          {citizenDisplayName || "—"}
        </span>
      </SettingRow>
      <SettingRow icon={Mail} title="Email Address" description="Primary account email.">
        <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
          {user?.email || "—"}
        </span>
      </SettingRow>
      <SettingRow icon={Shield} title="Account Type" description="Your role on the platform.">
        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300 border border-indigo-100 dark:border-indigo-900 capitalize">
          {user?.role || "Citizen"}
        </span>
      </SettingRow>
      <SettingRow
        icon={ExternalLink}
        title="Full Profile"
        description="View and update your personal details, caste category, and state for scheme eligibility."
      >
        <button
          onClick={() => navigate("/profile")}
          className="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-semibold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 rounded-lg transition"
        >
          <span>Go to Profile</span>
          <ChevronRight className="h-3.5 w-3.5" />
        </button>
      </SettingRow>
    </SectionCard>
  );

  const renderAppearance = () => {
    const themeOptions = [
      {
        id: "system",
        label: "System",
        description: "Follow your device operating system setting",
        icon: Monitor,
      },
      {
        id: "light",
        label: "Light",
        description: "Clean light appearance with crisp contrast",
        icon: Sun,
      },
      {
        id: "dark",
        label: "Dark",
        description: "Dark interface for low-light environments",
        icon: Moon,
      },
    ];

    return (
      <SectionCard title="Appearance" description="Customize how SchemeBridge looks on your device.">
        <div className="py-4 space-y-3">
          <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
            Theme Preference
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {themeOptions.map((opt) => {
              const Icon = opt.icon;
              const isSelected = themePreference === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => setTheme(opt.id)}
                  className={`p-3.5 rounded-xl border text-left transition-all duration-150 relative ${
                    isSelected
                      ? "border-indigo-600 dark:border-indigo-500 bg-indigo-50/50 dark:bg-indigo-950/30 ring-2 ring-indigo-500/20"
                      : "border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 bg-white dark:bg-slate-900"
                  }`}
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className={`p-2 rounded-lg ${isSelected ? "bg-indigo-600 text-white" : "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400"}`}>
                      <Icon className="h-4 w-4" />
                    </div>
                    {isSelected && (
                      <span className="h-2 w-2 rounded-full bg-indigo-600 dark:bg-indigo-400" />
                    )}
                  </div>
                  <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">{opt.label}</p>
                  <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-0.5 leading-snug">{opt.description}</p>
                </button>
              );
            })}
          </div>
        </div>
      </SectionCard>
    );
  };

  const renderLanguage = () => {
    const languages = [
      { code: "en", label: "English", native: "English", available: true },
      { code: "hi", label: "Hindi", native: "हिन्दी", available: false },
      { code: "ta", label: "Tamil", native: "தமிழ்", available: false },
    ];

    return (
      <SectionCard title="Language & Region" description="Choose your preferred language for the SchemeBridge interface.">
        <div className="py-4 space-y-2.5">
          {languages.map((lang) => (
            <div
              key={lang.code}
              className={`flex items-center justify-between p-3.5 rounded-xl border transition-colors ${
                lang.available
                  ? "border-indigo-200 dark:border-indigo-900/60 bg-indigo-50/30 dark:bg-indigo-950/20"
                  : "border-slate-100 dark:border-slate-800/60 opacity-60 cursor-not-allowed"
              }`}
            >
              <div className="flex items-center gap-3">
                <span className="text-sm font-semibold text-slate-800 dark:text-slate-200">{lang.native}</span>
                <span className="text-xs text-slate-400">({lang.label})</span>
              </div>
              {lang.available ? (
                <span className="inline-flex items-center gap-1 text-xs font-bold text-indigo-600 dark:text-indigo-400">
                  <Check className="h-3.5 w-3.5" />
                  Active
                </span>
              ) : (
                <span className="text-[11px] font-semibold text-slate-400 dark:text-slate-500 uppercase tracking-wider bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded">
                  Coming Soon
                </span>
              )}
            </div>
          ))}
          <p className="text-[11px] text-slate-400 dark:text-slate-500 flex items-start gap-1.5 pt-1">
            <Info className="h-3.5 w-3.5 mt-0.5 shrink-0" />
            Additional languages will be available in a future release.
          </p>
        </div>
      </SectionCard>
    );
  };

  const renderNotifications = () => (
    <SectionCard title="Notifications" description="Control which notifications you receive.">
      <SettingRow
        icon={Bell}
        title="Application Updates"
        description="Get notified when your application status changes."
      >
        <Toggle
          enabled={settings.notifications?.applicationUpdates ?? true}
          onChange={(v) => updateNotification("applicationUpdates", v)}
          label="Toggle application update notifications"
        />
      </SettingRow>
      <SettingRow
        icon={Bell}
        title="Scheme Recommendations"
        description="Receive updates about newly matched schemes."
      >
        <Toggle
          enabled={settings.notifications?.schemeRecommendations ?? true}
          onChange={(v) => updateNotification("schemeRecommendations", v)}
          label="Toggle scheme recommendation notifications"
        />
      </SettingRow>
      <SettingRow
        icon={Bell}
        title="General Notifications"
        description="Platform announcements and system updates."
      >
        <Toggle
          enabled={settings.notifications?.generalNotifications ?? true}
          onChange={(v) => updateNotification("generalNotifications", v)}
          label="Toggle general notifications"
        />
      </SettingRow>
      <SettingRow
        icon={Smartphone}
        title="SMS Notifications"
        description="SMS notification service is not configured in this release."
        badge={
          <span className="inline-flex items-center px-2 py-0.5 text-[9px] font-bold uppercase tracking-wider bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-400 rounded-full border border-amber-200 dark:border-amber-800">
            Not Configured
          </span>
        }
      >
        <span className="text-xs font-semibold text-slate-400 dark:text-slate-500 italic">Unavailable</span>
      </SettingRow>
    </SectionCard>
  );

  const renderPrivacy = () => (
    <SectionCard title="Privacy & Security" description="Your account security and password management.">
      <SettingRow
        icon={Lock}
        title="Authentication Method"
        description="How you sign in to SchemeBridge."
      >
        <span className="text-xs font-semibold text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 px-3 py-1.5 rounded-lg">
          Email & Password
        </span>
      </SettingRow>
      <SettingRow
        icon={Shield}
        title="Account Status"
        description="Your account verification and security state."
      >
        <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-3 py-1.5 rounded-lg border border-emerald-200 dark:border-emerald-800">
          <Check className="h-3.5 w-3.5" />
          Verified
        </span>
      </SettingRow>
      <SettingRow
        icon={Eye}
        title="Data Privacy"
        description="Your personal data is encrypted and stored securely. SchemeBridge follows government data protection standards."
      >
        <button
          onClick={() => navigate("/terms")}
          className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline"
        >
          View Policy
        </button>
      </SettingRow>

      {/* Genuine Change Password Section */}
      <div className="py-4 border-t border-slate-100 dark:border-slate-800">
        <div className="flex items-start gap-3">
          <div className="mt-0.5 shrink-0">
            <Lock className="h-4.5 w-4.5 text-slate-400 dark:text-slate-500" />
          </div>
          <div className="flex-1 min-w-0">
            <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200">Change Password</h4>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Update your SchemeBridge account password.
            </p>
            <ChangePasswordForm />
          </div>
        </div>
      </div>
    </SectionCard>
  );

  const renderAccessibility = () => (
    <SectionCard title="Accessibility" description="Configure accessibility preferences for a better experience.">
      <SettingRow
        icon={Accessibility}
        title="Reduce Motion"
        description="Minimize animations and transitions throughout the interface."
      >
        <Toggle
          enabled={settings.accessibility?.reducedMotion ?? false}
          onChange={(v) => updateAccessibility("reducedMotion", v)}
          label="Toggle reduced motion"
        />
      </SettingRow>
      <div className="py-4 border-b border-slate-100 dark:border-slate-800 last:border-b-0">
        <div className="flex items-start gap-3">
          <Info className="h-4.5 w-4.5 text-slate-400 dark:text-slate-500 mt-0.5 shrink-0" />
          <div>
            <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">Keyboard Navigation</p>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 leading-relaxed">
              SchemeBridge supports full keyboard navigation. Use <kbd className="px-1.5 py-0.5 text-[10px] font-bold bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded">Tab</kbd> to navigate between elements and <kbd className="px-1.5 py-0.5 text-[10px] font-bold bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded">Enter</kbd> to activate.
            </p>
          </div>
        </div>
      </div>
    </SectionCard>
  );

  const sectionRenderers = {
    account: renderAccount,
    appearance: renderAppearance,
    language: renderLanguage,
    notifications: renderNotifications,
    privacy: renderPrivacy,
    accessibility: renderAccessibility,
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">Settings</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          Manage your account, appearance, privacy, and security settings.
        </p>
      </div>

      {/* Two-column layout */}
      <div className="flex flex-col lg:flex-row gap-6">
        {/* Left: Section navigation */}
        <nav className="w-full lg:w-60 shrink-0" aria-label="Settings sections">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-2 space-y-0.5 lg:sticky lg:top-4">
            {SECTIONS.map((section) => {
              const Icon = section.icon;
              const isActive = activeSection === section.id;
              return (
                <button
                  key={section.id}
                  onClick={() => setActiveSection(section.id)}
                  className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 ${
                    isActive
                      ? "bg-indigo-50 dark:bg-indigo-950/50 text-indigo-700 dark:text-indigo-300 font-semibold"
                      : "text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800/60 hover:text-slate-800 dark:hover:text-slate-200"
                  }`}
                  aria-current={isActive ? "page" : undefined}
                >
                  <Icon className={`h-4 w-4 ${isActive ? "text-indigo-600 dark:text-indigo-400" : "text-slate-400 dark:text-slate-500"}`} />
                  <span>{section.label}</span>
                </button>
              );
            })}
          </div>
        </nav>

        {/* Right: Active section content */}
        <div className="flex-1 min-w-0">
          {sectionRenderers[activeSection]?.()}
        </div>
      </div>
    </div>
  );
}
