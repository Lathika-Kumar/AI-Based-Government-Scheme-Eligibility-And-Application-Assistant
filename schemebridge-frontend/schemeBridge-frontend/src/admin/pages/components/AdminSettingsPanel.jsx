import React, { useState, useEffect, useCallback } from "react";
import { useToast } from "@components/ui/ToastNotification";
import { useTheme } from "@context/ThemeContext";
import {
  Sliders,
  Shield,
  Save,
  Database,
  Cpu,
  RefreshCw,
  Server,
  Bell,
  Mail,
  Globe,
  Settings,
  Workflow,
  Lock,
  UserCog,
  Palette,
  Activity,
  CheckCircle,
  XCircle,
  Sun,
  Moon,
  Monitor
} from "lucide-react";
import adminService from "@services/adminService";

export default function AdminSettingsPanel() {
  const { showToast } = useToast();
  const { theme: themePreference, setTheme } = useTheme();
  const [activeSection, setActiveSection] = useState("general");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // General Settings
  const [platformName, setPlatformName] = useState("SchemeBridge Government Platform");
  const [defaultTimezone, setDefaultTimezone] = useState("Asia/Kolkata");

  // Workflow Configuration
  const [slaTarget, setSlaTarget] = useState(7);
  const [maxQueueSize, setMaxQueueSize] = useState(50);
  const [enableEscalation, setEnableEscalation] = useState(true);

  // Notification Preferences
  const [notifyNewApplication, setNotifyNewApplication] = useState(true);
  const [notifyGrievance, setNotifyGrievance] = useState(true);
  const [notifySlaBreach, setNotifySlaBreach] = useState(true);

  // AI Configuration
  const [ocrConfidence, setOcrConfidence] = useState(85);
  const [enableAutoVerify, setEnableAutoVerify] = useState(true);
  const [enableRecommendations, setEnableRecommendations] = useState(true);

  // Security Settings
  const [sessionTimeout, setSessionTimeout] = useState(30);
  const [enableTwoFactor, setEnableTwoFactor] = useState(true);
  const [passwordMinLength, setPasswordMinLength] = useState(8);

  const fetchSettings = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminService.getAdminSettings();
      if (!res.error && res.data) {
        const s = res.data;
        if (s.platformName) setPlatformName(s.platformName);
        if (s.defaultTimezone) setDefaultTimezone(s.defaultTimezone);
        if (s.applicationSlaDays) setSlaTarget(s.applicationSlaDays);
        if (s.grievanceSlaHours) setMaxQueueSize(s.grievanceSlaHours);
        if (typeof s.emailNotificationsEnabled === "boolean") setNotifyNewApplication(s.emailNotificationsEnabled);
        if (typeof s.smsNotificationsEnabled === "boolean") setNotifyGrievance(s.smsNotificationsEnabled);
        if (typeof s.aiAssistedReviewEnabled === "boolean") setEnableAutoVerify(s.aiAssistedReviewEnabled);
        if (s.sessionTimeoutMinutes) setSessionTimeout(s.sessionTimeoutMinutes);
        if (typeof s.mfaEnforcedForAdmins === "boolean") setEnableTwoFactor(s.mfaEnforcedForAdmins);
      }
    } catch (err) {
      console.error("Failed to load settings", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const sections = [
    { id: "general", label: "General Settings", icon: Settings },
    { id: "appearance", label: "Appearance", icon: Palette },
    { id: "workflow", label: "Workflow Configuration", icon: Workflow },
    { id: "notifications", label: "Notification Preferences", icon: Bell },
    { id: "ai", label: "AI & Verification", icon: Cpu },
    { id: "security", label: "Security & Access", icon: Shield },
  ];

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        platformName,
        defaultTimezone,
        applicationSlaDays: Number(slaTarget),
        grievanceSlaHours: Number(maxQueueSize),
        emailNotificationsEnabled: notifyNewApplication,
        smsNotificationsEnabled: notifyGrievance,
        aiAssistedReviewEnabled: enableAutoVerify,
        sessionTimeoutMinutes: Number(sessionTimeout),
        mfaEnforcedForAdmins: enableTwoFactor,
      };

      const res = await adminService.updateAdminSettings(payload);
      if (!res.error) {
        showToast("success", "Settings Saved", "All government platform configuration settings saved to backend successfully!");
      } else {
        showToast("error", "Save Failed", res.message || "Could not save platform settings.");
      }
    } catch (err) {
      showToast("error", "Error", "Failed to save settings.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
      <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Sliders className="h-4.5 w-4.5 text-indigo-600 dark:text-indigo-400" />
          <h3 className="text-xs font-bold text-slate-800 dark:text-slate-200 uppercase tracking-wider">Government Platform Administration Settings</h3>
        </div>
        <button
          onClick={fetchSettings}
          className="p-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 rounded-xl transition"
          title="Reload Settings"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      <div className="flex flex-col lg:flex-row">
        {/* Sidebar Navigation */}
        <div className="w-full lg:w-64 border-b lg:border-b-0 lg:border-r border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
          <nav className="p-4 space-y-1">
            {sections.map((section) => {
              const Icon = section.icon;
              return (
                <button
                  key={section.id}
                  onClick={() => setActiveSection(section.id)}
                  className={`w-full flex items-center gap-2 px-3 py-2.5 rounded-xl text-xs font-bold transition duration-150 ${
                    activeSection === section.id
                      ? "bg-indigo-600 text-white shadow-sm"
                      : "text-slate-600 dark:text-slate-400 hover:bg-slate-200/50 dark:hover:bg-slate-800/60"
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  <span>{section.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* Main Content Area */}
        <div className="flex-1 p-6 overflow-y-auto">
          <form onSubmit={handleSave} className="space-y-6 text-xs font-semibold text-slate-700 dark:text-slate-300">
            
            {/* General Settings */}
            {activeSection === "general" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-2">
                  <Settings className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 dark:text-slate-200 text-[11px] uppercase tracking-wider">General Platform Settings</h4>
                </div>
                
                <div className="space-y-3">
                  <div>
                    <label className="block text-slate-500 dark:text-slate-400 mb-1">Platform Title</label>
                    <input
                      type="text"
                      value={platformName}
                      onChange={(e) => setPlatformName(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-slate-500 dark:text-slate-400 mb-1">Default System Timezone</label>
                    <select
                      value={defaultTimezone}
                      onChange={(e) => setDefaultTimezone(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    >
                      <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
                      <option value="Asia/Dubai">Asia/Dubai (GST)</option>
                      <option value="Europe/London">Europe/London (GMT)</option>
                    </select>
                  </div>
                </div>
              </div>
            )}

            {/* Appearance Settings */}
            {activeSection === "appearance" && (
              <div className="space-y-6">
                <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-2">
                  <Palette className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 dark:text-slate-200 text-[11px] uppercase tracking-wider">Appearance</h4>
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400 -mt-3">
                  Customize the visual appearance of the SchemeBridge Operations Console.
                </p>

                <div className="space-y-3">
                  <div>
                    <h5 className="font-bold text-slate-800 dark:text-slate-200 text-xs">Theme</h5>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                      Choose how SchemeBridge should appear.
                    </p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
                    {[
                      {
                        id: "light",
                        label: "Light",
                        description: "Use a light interface.",
                        icon: Sun,
                      },
                      {
                        id: "dark",
                        label: "Dark",
                        description: "Use a dark interface.",
                        icon: Moon,
                      },
                      {
                        id: "system",
                        label: "System",
                        description: "Follow your operating system preference.",
                        icon: Monitor,
                      },
                    ].map((opt) => {
                      const Icon = opt.icon;
                      const isSelected = themePreference === opt.id;
                      return (
                        <button
                          key={opt.id}
                          type="button"
                          role="radio"
                          aria-checked={isSelected}
                          aria-label={`${opt.label} theme`}
                          onClick={() => setTheme(opt.id)}
                          className={`p-3.5 rounded-xl border text-left transition-all duration-150 relative ${
                            isSelected
                              ? "border-indigo-600 dark:border-indigo-500 bg-indigo-50/50 dark:bg-indigo-950/30 ring-2 ring-indigo-500/20"
                              : "border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 bg-white dark:bg-slate-900"
                          }`}
                        >
                          <div className="flex items-center justify-between mb-2">
                            <div
                              className={`p-2 rounded-lg ${
                                isSelected
                                  ? "bg-indigo-600 text-white"
                                  : "bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400"
                              }`}
                            >
                              <Icon className="h-4 w-4" />
                            </div>
                            <div
                              className={`w-4 h-4 rounded-full border flex items-center justify-center transition-colors ${
                                isSelected
                                  ? "border-indigo-600 dark:border-indigo-400 bg-indigo-600 dark:bg-indigo-400"
                                  : "border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800"
                              }`}
                              aria-hidden="true"
                            >
                              {isSelected && <span className="w-1.5 h-1.5 rounded-full bg-white" />}
                            </div>
                          </div>
                          <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">{opt.label}</p>
                          <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-0.5 leading-snug">
                            {opt.description}
                          </p>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>
            )}

            {/* Workflow Configuration */}
            {activeSection === "workflow" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-2">
                  <Workflow className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 dark:text-slate-200 text-[11px] uppercase tracking-wider">Workflow Configuration</h4>
                </div>

                <div className="space-y-3">
                  <div>
                    <label className="block text-slate-500 dark:text-slate-400 mb-1">Application Target SLA (Days)</label>
                    <input
                      type="number"
                      value={slaTarget}
                      onChange={(e) => setSlaTarget(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-slate-500 dark:text-slate-400 mb-1">Grievance SLA Target (Hours)</label>
                    <input
                      type="number"
                      value={maxQueueSize}
                      onChange={(e) => setMaxQueueSize(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Notification Preferences */}
            {activeSection === "notifications" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-2">
                  <Bell className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 dark:text-slate-200 text-[11px] uppercase tracking-wider">Notification Dispatch Rules</h4>
                </div>

                <div className="space-y-3">
                  <div className="flex justify-between items-center bg-slate-50 dark:bg-slate-800/60 p-3 rounded-xl border border-slate-100 dark:border-slate-800">
                    <div>
                      <span className="text-slate-800 dark:text-slate-200 font-bold block">Email Notifications</span>
                      <span className="text-[10px] text-slate-400 font-medium">Auto-dispatch emails on application status milestones.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setNotifyNewApplication(!notifyNewApplication)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        notifyNewApplication ? "bg-indigo-600" : "bg-slate-300 dark:bg-slate-700"
                      }`}
                    >
                      <div className={`w-4 h-4 bg-white rounded-full transition-transform duration-200 ${notifyNewApplication ? "transform translate-x-6" : ""}`} />
                    </button>
                  </div>

                  <div className="flex justify-between items-center bg-slate-50 dark:bg-slate-800/40 p-3 rounded-xl border border-slate-100 dark:border-slate-800 opacity-75">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-slate-800 dark:text-slate-200 font-bold block">SMS Alerts</span>
                        <span className="text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-400 border border-amber-200 dark:border-amber-800">Coming Soon</span>
                      </div>
                      <span className="text-[10px] text-slate-400 font-medium">SMS gateway integration is not configured in this release.</span>
                    </div>
                    <div className="text-[10px] font-bold text-slate-400 dark:text-slate-500 bg-slate-100 dark:bg-slate-800 px-2.5 py-1 rounded-lg border border-slate-200 dark:border-slate-700">
                      Not Configured
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* AI Configuration */}
            {activeSection === "ai" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-2">
                  <Cpu className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 dark:text-slate-200 text-[11px] uppercase tracking-wider">AI & Document Engine</h4>
                </div>

                <div className="space-y-3">
                  <div className="flex justify-between items-center bg-slate-50 dark:bg-slate-800/60 p-3 rounded-xl border border-slate-100 dark:border-slate-800">
                    <div>
                      <span className="text-slate-800 dark:text-slate-200 font-bold block">AI Assisted Eligibility Evaluation</span>
                      <span className="text-[10px] text-slate-400 font-medium">Enable real-time rule engine execution and matching.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setEnableAutoVerify(!enableAutoVerify)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        enableAutoVerify ? "bg-indigo-600" : "bg-slate-300 dark:bg-slate-700"
                      }`}
                    >
                      <div className={`w-4 h-4 bg-white rounded-full transition-transform duration-200 ${enableAutoVerify ? "transform translate-x-6" : ""}`} />
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Security Settings */}
            {activeSection === "security" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-2">
                  <Shield className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 dark:text-slate-200 text-[11px] uppercase tracking-wider">Security & Session Parameters</h4>
                </div>

                <div className="space-y-3">
                  <div>
                    <label className="block text-slate-500 dark:text-slate-400 mb-1">Session Timeout (Minutes)</label>
                    <input
                      type="number"
                      value={sessionTimeout}
                      onChange={(e) => setSessionTimeout(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>

                  <div className="flex justify-between items-center bg-slate-50 dark:bg-slate-800/60 p-3 rounded-xl border border-slate-100 dark:border-slate-800">
                    <div>
                      <span className="text-slate-800 dark:text-slate-200 font-bold block">Enforce Multi-Factor Authentication</span>
                      <span className="text-[10px] text-slate-400 font-medium">Require Aadhaar OTP for administrative login elevation.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setEnableTwoFactor(!enableTwoFactor)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        enableTwoFactor ? "bg-indigo-600" : "bg-slate-300 dark:bg-slate-700"
                      }`}
                    >
                      <div className={`w-4 h-4 bg-white rounded-full transition-transform duration-200 ${enableTwoFactor ? "transform translate-x-6" : ""}`} />
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Save Button (hidden for Appearance as theme applies immediately) */}
            {activeSection !== "appearance" && (
              <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex justify-end">
                <button
                  type="submit"
                  disabled={saving}
                  className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-sm transition flex items-center gap-2 disabled:opacity-50"
                >
                  <Save className="h-4 w-4" />
                  {saving ? "Saving Changes..." : "Save Platform Settings"}
                </button>
              </div>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}
