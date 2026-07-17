import React, { useState } from "react";
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
  XCircle
} from "lucide-react";

export default function AdminSettingsPanel() {
  const [activeSection, setActiveSection] = useState("general");

  // General Settings
  const [platformName, setPlatformName] = useState("SchemeBridge Government Portal");
  const [defaultTimezone, setDefaultTimezone] = useState("Asia/Kolkata");
  const [enableMaintenanceMode, setEnableMaintenanceMode] = useState(false);

  // Workflow Configuration
  const [slaTarget, setSlaTarget] = useState(7);
  const [maxQueueSize, setMaxQueueSize] = useState(45);
  const [enableEscalation, setEnableEscalation] = useState(true);

  // Notification Preferences
  const [notifyNewApplication, setNotifyNewApplication] = useState(true);
  const [notifyGrievance, setNotifyGrievance] = useState(true);
  const [notifySlaBreach, setNotifySlaBreach] = useState(true);

  // Language Settings
  const [defaultLanguage, setDefaultLanguage] = useState("en");
  const [enableRegionalLanguages, setEnableRegionalLanguages] = useState(true);

  // AI Configuration
  const [ocrConfidence, setOcrConfidence] = useState(85);
  const [enableAutoVerify, setEnableAutoVerify] = useState(true);
  const [enableRecommendations, setEnableRecommendations] = useState(true);

  // Security Settings
  const [sessionTimeout, setSessionTimeout] = useState(30);
  const [enableTwoFactor, setEnableTwoFactor] = useState(true);
  const [passwordMinLength, setPasswordMinLength] = useState(8);

  // Email Template Settings
  const [approvalEmailSubject, setApprovalEmailSubject] = useState("Your application has been approved!");
  const [rejectionEmailSubject, setRejectionEmailSubject] = useState("Your application status update");

  const sections = [
    { id: "general", label: "General Settings", icon: Settings },
    { id: "workflow", label: "Workflow Configuration", icon: Workflow },
    { id: "notifications", label: "Notification Preferences", icon: Bell },
    { id: "email", label: "Email Templates", icon: Mail },
    { id: "language", label: "Language Settings", icon: Globe },
    { id: "ai", label: "AI Configuration", icon: Cpu },
    { id: "security", label: "Security Settings", icon: Shield },
  ];

  const handleSave = (e) => {
    e.preventDefault();
    alert("All government platform configuration settings saved successfully!");
  };

  return (
    <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
      <div className="p-5 border-b border-slate-100 flex items-center gap-2">
        <Sliders className="h-4.5 w-4.5 text-indigo-600" />
        <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Government Platform Administration Settings</h3>
      </div>

      <div className="flex flex-col lg:flex-row">
        {/* Sidebar Navigation */}
        <div className="w-full lg:w-64 border-b lg:border-b-0 lg:border-r border-slate-200 bg-slate-50/50">
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
                      : "text-slate-600 hover:bg-slate-200/50"
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
          <form onSubmit={handleSave} className="space-y-6 text-xs font-semibold text-slate-700">
            
            {/* General Settings */}
            {activeSection === "general" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                  <Settings className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">General Platform Settings</h4>
                </div>
                
                <div className="space-y-3">
                  <div>
                    <label className="block text-slate-500 mb-1">Platform Name</label>
                    <input
                      type="text"
                      value={platformName}
                      onChange={(e) => setPlatformName(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-slate-500 mb-1">Default Timezone</label>
                    <select
                      value={defaultTimezone}
                      onChange={(e) => setDefaultTimezone(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    >
                      <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
                      <option value="Asia/Dubai">Asia/Dubai (GST)</option>
                      <option value="Europe/London">Europe/London (GMT)</option>
                    </select>
                  </div>

                  <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div>
                      <span className="text-slate-800 font-bold block">Enable Maintenance Mode</span>
                      <span className="text-[10px] text-slate-400 font-medium">Restrict access to non-administrators.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setEnableMaintenanceMode(!enableMaintenanceMode)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        enableMaintenanceMode ? "bg-rose-600" : "bg-slate-300"
                      }`}
                    >
                      <div
                        className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                          enableMaintenanceMode ? "transform translate-x-6" : ""
                        }`}
                      />
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Workflow Configuration */}
            {activeSection === "workflow" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                  <Workflow className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">Workflow & SLA Configuration</h4>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-slate-500 mb-1">Target Application SLA (Days)</label>
                    <input
                      type="number"
                      value={slaTarget}
                      onChange={(e) => setSlaTarget(Number(e.target.value))}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-500 mb-1">Max Queue Size per Nodal Officer</label>
                    <input
                      type="number"
                      value={maxQueueSize}
                      onChange={(e) => setMaxQueueSize(Number(e.target.value))}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                </div>

                <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div>
                    <span className="text-slate-800 font-bold block">Enable Automatic Escalations</span>
                    <span className="text-[10px] text-slate-400 font-medium">Escalate applications after SLA breach.</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setEnableEscalation(!enableEscalation)}
                    className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                      enableEscalation ? "bg-indigo-600" : "bg-slate-300"
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                        enableEscalation ? "transform translate-x-6" : ""
                      }`}
                    />
                  </button>
                </div>
              </div>
            )}

            {/* Notification Preferences */}
            {activeSection === "notifications" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                  <Bell className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">Notification Preferences</h4>
                </div>
                
                <div className="space-y-3">
                  <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div>
                      <span className="text-slate-800 font-bold block">New Application Submission</span>
                      <span className="text-[10px] text-slate-400 font-medium">Notify officers when new application is received.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setNotifyNewApplication(!notifyNewApplication)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        notifyNewApplication ? "bg-indigo-600" : "bg-slate-300"
                      }`}
                    >
                      <div
                        className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                          notifyNewApplication ? "transform translate-x-6" : ""
                        }`}
                      />
                    </button>
                  </div>

                  <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div>
                      <span className="text-slate-800 font-bold block">Grievance Received</span>
                      <span className="text-[10px] text-slate-400 font-medium">Notify officers when new grievance is filed.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setNotifyGrievance(!notifyGrievance)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        notifyGrievance ? "bg-indigo-600" : "bg-slate-300"
                      }`}
                    >
                      <div
                        className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                          notifyGrievance ? "transform translate-x-6" : ""
                        }`}
                      />
                    </button>
                  </div>

                  <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div>
                      <span className="text-slate-800 font-bold block">SLA Breach Alerts</span>
                      <span className="text-[10px] text-slate-400 font-medium">Notify admins when SLA target is breached.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setNotifySlaBreach(!notifySlaBreach)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        notifySlaBreach ? "bg-indigo-600" : "bg-slate-300"
                      }`}
                    >
                      <div
                        className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                          notifySlaBreach ? "transform translate-x-6" : ""
                        }`}
                      />
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Email Templates */}
            {activeSection === "email" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                  <Mail className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">Email Notification Templates</h4>
                </div>
                
                <div className="space-y-3">
                  <div>
                    <label className="block text-slate-500 mb-1">Application Approval Email Subject</label>
                    <input
                      type="text"
                      value={approvalEmailSubject}
                      onChange={(e) => setApprovalEmailSubject(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-slate-500 mb-1">Application Rejection Email Subject</label>
                    <input
                      type="text"
                      value={rejectionEmailSubject}
                      onChange={(e) => setRejectionEmailSubject(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>

                  <div className="p-4 border border-slate-200 bg-slate-50 rounded-xl">
                    <p className="text-[10px] text-slate-500 font-medium">
                      Full email body templates are managed in the backend configuration. These placeholders are for demonstration purposes.
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Language Settings */}
            {activeSection === "language" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                  <Globe className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">Language & Regionalization</h4>
                </div>
                
                <div className="space-y-3">
                  <div>
                    <label className="block text-slate-500 mb-1">Default Platform Language</label>
                    <select
                      value={defaultLanguage}
                      onChange={(e) => setDefaultLanguage(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    >
                      <option value="en">English</option>
                      <option value="hi">Hindi</option>
                      <option value="ta">Tamil</option>
                      <option value="te">Telugu</option>
                      <option value="kn">Kannada</option>
                    </select>
                  </div>

                  <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div>
                      <span className="text-slate-800 font-bold block">Enable Regional Languages</span>
                      <span className="text-[10px] text-slate-400 font-medium">Allow users to switch to regional languages.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setEnableRegionalLanguages(!enableRegionalLanguages)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        enableRegionalLanguages ? "bg-indigo-600" : "bg-slate-300"
                      }`}
                    >
                      <div
                        className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                          enableRegionalLanguages ? "transform translate-x-6" : ""
                        }`}
                      />
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* AI Configuration */}
            {activeSection === "ai" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                  <Cpu className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">Artificial Intelligence Configuration</h4>
                </div>
                
                <div className="space-y-3">
                  <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div>
                      <span className="text-slate-800 font-bold block">Auto-Verify High Confidence Documents</span>
                      <span className="text-[10px] text-slate-400 font-medium">Bypass manual review if OCR validation exceeds threshold.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setEnableAutoVerify(!enableAutoVerify)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        enableAutoVerify ? "bg-indigo-600" : "bg-slate-300"
                      }`}
                    >
                      <div
                        className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                          enableAutoVerify ? "transform translate-x-6" : ""
                        }`}
                      />
                    </button>
                  </div>

                  <div>
                    <label className="block text-slate-500 mb-1">OCR Confidence Threshold (%)</label>
                    <div className="flex items-center gap-3">
                      <input
                        type="range"
                        min="50"
                        max="99"
                        value={ocrConfidence}
                        onChange={(e) => setOcrConfidence(Number(e.target.value))}
                        className="flex-1 accent-indigo-600 h-1 bg-slate-200 rounded-lg cursor-pointer"
                      />
                      <span className="font-bold text-slate-800 text-xs w-8">{ocrConfidence}%</span>
                    </div>
                  </div>

                  <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div>
                      <span className="text-slate-800 font-bold block">Enable AI Scheme Recommendations</span>
                      <span className="text-[10px] text-slate-400 font-medium">Use AI to suggest relevant schemes to citizens.</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setEnableRecommendations(!enableRecommendations)}
                      className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                        enableRecommendations ? "bg-indigo-600" : "bg-slate-300"
                      }`}
                    >
                      <div
                        className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                          enableRecommendations ? "transform translate-x-6" : ""
                        }`}
                      />
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Security Settings */}
            {activeSection === "security" && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                  <Shield className="h-4 w-4 text-slate-400" />
                  <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">Security & Authentication</h4>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-slate-500 mb-1">Session Timeout (Minutes)</label>
                    <input
                      type="number"
                      value={sessionTimeout}
                      onChange={(e) => setSessionTimeout(Number(e.target.value))}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-500 mb-1">Minimum Password Length</label>
                    <input
                      type="number"
                      value={passwordMinLength}
                      onChange={(e) => setPasswordMinLength(Number(e.target.value))}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                </div>

                <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div>
                    <span className="text-slate-800 font-bold block">Enforce Two-Factor Authentication</span>
                    <span className="text-[10px] text-slate-400 font-medium">Require 2FA for all admin and officer accounts.</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setEnableTwoFactor(!enableTwoFactor)}
                    className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                      enableTwoFactor ? "bg-indigo-600" : "bg-slate-300"
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                        enableTwoFactor ? "transform translate-x-6" : ""
                      }`}
                    />
                  </button>
                </div>

                <div className="space-y-2 pt-2 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => alert("Simulating cache optimization...")}
                    className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl font-bold transition flex items-center gap-1.5"
                  >
                    <RefreshCw className="h-3.5 w-3.5" />
                    <span>Optimize Indices</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => alert("Simulating local storage database backup...")}
                    className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl font-bold transition flex items-center gap-1.5"
                  >
                    <Database className="h-3.5 w-3.5 text-indigo-600" />
                    <span>Backup DB Configuration</span>
                  </button>
                </div>
              </div>
            )}

            {/* Save Button */}
            <div className="pt-4 border-t border-slate-100 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => alert("All settings have been reset to defaults!")}
                className="px-4 py-2.5 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl font-bold shadow-sm transition flex items-center gap-1.5"
              >
                <RefreshCw className="h-4 w-4" />
                <span>Reset to Defaults</span>
              </button>
              <button
                type="submit"
                className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold shadow-sm transition flex items-center gap-1.5"
              >
                <Save className="h-4 w-4" />
                <span>Save All Settings</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
