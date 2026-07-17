import React, { useState, useEffect, useMemo } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import {
  EDUCATION_OPTIONS,
  OCCUPATION_OPTIONS,
  CASTE_OPTIONS,
  INDIAN_STATES_AND_UTS,
  calculateCompletion
} from "@data/mockProfile";
import { FormSectionSkeleton } from "@components/ui/LoadingSkeleton";
import {
  User,
  Briefcase,
  MapPin,
  Save,
  Sparkles,
  FileText,
  ShieldCheck,
  Eye
} from "lucide-react";

export default function Profile() {
  const { user, updateUser } = useAuth();
  const { profile, updateProfile, documents, addAuditLog, t } = useApp();
  const navigate = useNavigate();

  // Initializing state directly from context / localStorage values
  const [formData, setFormData] = useState({
    name: profile?.name || user?.name || "",
    age: profile?.age || user?.age || "32",
    gender: profile?.gender || user?.gender || "Male",
    state: profile?.state || user?.state || "Gujarat",
    education: profile?.education || "Graduate / Bachelor's Degree",
    occupation: profile?.occupation || user?.occupation || "Farmer",
    income: profile?.annualIncome || user?.income || "180000",
    caste: profile?.caste || user?.caste || "OBC",
    preferredLanguage: profile?.preferredLanguage || "en",
    fontSize: profile?.fontSize || "normal",
    highContrast: profile?.highContrast || "standard",
    audioGuidance: profile?.audioGuidance || "disabled"
  });

  const [mfaEnabled, setMfaEnabled] = useState(() => {
    return localStorage.getItem("schemebridge_mfa_enabled") === "true";
  });

  const [savedSuccess, setSavedSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Simulate async profile data fetch
  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 700);
    return () => clearTimeout(timer);
  }, []);

  // Apply Accessibility Classes dynamically
  useEffect(() => {
    // Dynamic High Contrast
    const rootEl = document.documentElement;
    if (formData.highContrast === "high-contrast") {
      rootEl.classList.add("high-contrast-mode");
      // Add standard high contrast color rules inject to page
      let styleEl = document.getElementById("high-contrast-styles");
      if (!styleEl) {
        styleEl = document.createElement("style");
        styleEl.id = "high-contrast-styles";
        styleEl.innerHTML = `
          .high-contrast-mode {
            filter: contrast(1.2);
          }
          .high-contrast-mode .bg-white {
            background-color: #000000 !important;
            color: #FFFF00 !important;
            border-color: #FFFF00 !important;
          }
          .high-contrast-mode .text-gray-800,
          .high-contrast-mode .text-gray-900 {
            color: #FFFF00 !important;
          }
          .high-contrast-mode select,
          .high-contrast-mode input {
            background-color: #000000 !important;
            color: #FFFF00 !important;
            border: 2px solid #FFFF00 !important;
          }
        `;
        document.head.appendChild(styleEl);
      }
    } else {
      rootEl.classList.remove("high-contrast-mode");
      const styleEl = document.getElementById("high-contrast-styles");
      if (styleEl) {
        styleEl.remove();
      }
    }

    // Dynamic Font Size adjustment on main container
    const mainContent = document.getElementById("main-content");
    if (mainContent) {
      if (formData.fontSize === "large") {
        mainContent.style.fontSize = "16px";
      } else if (formData.fontSize === "xlarge") {
        mainContent.style.fontSize = "18px";
      } else {
        mainContent.style.fontSize = "";
      }
    }
  }, [formData.fontSize, formData.highContrast]);

  const profileCompletionPercentage = useMemo(() => {
    const profileObjForCalc = {
      name: formData.name,
      age: Number(formData.age),
      gender: formData.gender,
      occupation: formData.occupation,
      annualIncome: Number(formData.income),
      caste: formData.caste,
      state: formData.state,
      education: formData.education
    };
    return calculateCompletion(profileObjForCalc);
  }, [formData]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setSavedSuccess(false);
  };

  const handleSave = (e) => {
    e.preventDefault();

    const formattedProfile = {
      name: formData.name,
      age: Number(formData.age),
      gender: formData.gender,
      state: formData.state,
      education: formData.education,
      occupation: formData.occupation,
      annualIncome: Number(formData.income),
      caste: formData.caste,
      preferredLanguage: formData.preferredLanguage,
      fontSize: formData.fontSize,
      highContrast: formData.highContrast,
      audioGuidance: formData.audioGuidance,
      isComplete: true
    };

    // Update global app state (persists to localStorage)
    updateProfile(formattedProfile);

    // Sync Auth session
    updateUser({
      name: formData.name,
      age: Number(formData.age),
      gender: formData.gender,
      occupation: formData.occupation,
      income: Number(formData.income),
      caste: formData.caste,
      state: formData.state
    });

    addAuditLog(
      "Profile Update",
      "Profile",
      formData.name || "Citizen",
      `Socio-economic criteria updated. New Match readiness sync triggered.`,
      formData.name || "Citizen"
    );

    setSavedSuccess(true);
    setTimeout(() => {
      setSavedSuccess(false);
      navigate("/dashboard");
    }, 1200);
  };

  const toggleMfa = () => {
    const newVal = !mfaEnabled;
    setMfaEnabled(newVal);
    localStorage.setItem("schemebridge_mfa_enabled", String(newVal));
    addAuditLog(
      "MFA Toggle",
      "Security",
      formData.name || "Citizen",
      `Simulated Aadhaar-MFA state set to ${newVal ? "ENABLED" : "DISABLED"}`,
      formData.name || "Citizen"
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 p-6 rounded-2xl shadow-xs animate-pulse">
          <div className="h-6 w-48 bg-gray-200 rounded mb-2" />
          <div className="h-3 w-80 bg-gray-100 rounded" />
        </div>
        <FormSectionSkeleton />
        <FormSectionSkeleton />
        <FormSectionSkeleton />
      </div>
    );
  }

  return (
    <div className="space-y-6">

      {/* Page Header */}
      <div className="bg-white border border-gray-200 p-6 rounded-2xl shadow-xs space-y-4">
        <div>
          <h1 className="text-xl font-black text-gray-900 tracking-tight">{t("profile_title") || "Eligibility Profile"}</h1>
          <p className="text-xs text-gray-500 mt-0.5">
            {t("profile_subtitle") || "Keep your socio-economic attributes accurate. These fields determine matching government schemes."}
          </p>
        </div>

        {/* Dynamic Completion Percentage Bar */}
        <div className="bg-gray-50 border border-gray-200 p-4 rounded-xl space-y-2">
          <div className="flex justify-between items-center text-xs">
            <span className="font-bold text-gray-700">{t("profile_completion") || "Profile Completion Progress"}</span>
            <span className="font-black text-government-blue">{profileCompletionPercentage}%</span>
          </div>
          <div className="h-2.5 bg-gray-200 rounded-full overflow-hidden">
            <div
              className="h-full bg-government-blue rounded-full transition-all duration-500"
              style={{ width: `${profileCompletionPercentage}%` }}
            />
          </div>
          <p className="text-[10px] text-gray-400 font-medium leading-none">
            {profileCompletionPercentage === 100
              ? (t("profile_complete_msg") || "✓ Profile completely populated. Match engine operating at maximum efficiency.")
              : (t("profile_incomplete_msg") || "Complete the remaining demographic parameters to receive fully authenticated recommendation files.")}
          </p>
        </div>
      </div>

      {savedSuccess && (
        <div className="bg-india-green/10 border border-india-green/20 text-india-green-dark text-xs p-4 rounded-xl flex items-center gap-2 shadow-xs">
          <Sparkles className="h-4 w-4 text-india-green shrink-0" />
          <span>{t("profile_saved_success") || "Profile configuration successfully saved! Redirecting to Dashboard..."}</span>
        </div>
      )}

      {/* Main Two-Column Content Grid */}
      <form onSubmit={handleSave} className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">

        {/* Left Columns: Form Fields */}
        <div className="lg:col-span-2 space-y-6 bg-white border border-gray-200 p-6 rounded-2xl shadow-xs">

          {/* Section 1: Personal Information */}
          <div className="space-y-4">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-2">
              <User className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-black text-gray-900 uppercase tracking-widest">{t("profile_personal_info") || "Personal Information"}</h3>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_full_name") || "Full Name"}
                </label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  required
                  placeholder={t("profile_enter_name") || "Enter your name"}
                  className="w-full text-xs px-3.5 py-2.5 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-1 focus:ring-government-blue transition"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_age") || "Age (Years)"}
                </label>
                <input
                  type="number"
                  name="age"
                  value={formData.age}
                  onChange={handleInputChange}
                  required
                  min="1"
                  max="120"
                  className="w-full text-xs px-3.5 py-2.5 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-1 focus:ring-government-blue transition"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_gender") || "Gender"}
                </label>
                <select
                  name="gender"
                  value={formData.gender}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  <option value="Male">{t("profile_gender_male") || "Male"}</option>
                  <option value="Female">{t("profile_gender_female") || "Female"}</option>
                  <option value="Other">{t("profile_gender_other") || "Other"}</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_state") || "State of Residence"}
                </label>
                <select
                  name="state"
                  value={formData.state}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  {INDIAN_STATES_AND_UTS.map((st) => (
                    <option key={st} value={st}>{st}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* Section 2: Education & Occupation */}
          <div className="space-y-4 pt-4 border-t border-gray-100">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-2">
              <Briefcase className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-black text-gray-900 uppercase tracking-widest">{t("profile_education_occupation") || "Education & Occupation"}</h3>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_edu_qualification") || "Educational Qualification"}
                </label>
                <select
                  name="education"
                  value={formData.education}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  {EDUCATION_OPTIONS.map((edu) => (
                    <option key={edu} value={edu}>{edu}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_primary_occupation") || "Primary Occupation"}
                </label>
                <select
                  name="occupation"
                  value={formData.occupation}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  {OCCUPATION_OPTIONS.map((occ) => (
                    <option key={occ} value={occ}>{occ}</option>
                  ))}
                </select>
              </div>

              <div className="sm:col-span-2">
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_annual_income") || "Annual Household Income (₹)"}
                </label>
                <input
                  type="number"
                  name="income"
                  value={formData.income}
                  onChange={handleInputChange}
                  required
                  min="0"
                  className="w-full text-xs px-3.5 py-2.5 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-1 focus:ring-government-blue transition"
                />
                <span className="text-[9px] text-gray-400 font-semibold block mt-1 leading-none">
                  {t("profile_income_hint") || "Provide gross annual income. This will be validated against your Income Certificate."}
                </span>
              </div>
            </div>
          </div>

          {/* Section 3: Social Group */}
          <div className="space-y-4 pt-4 border-t border-gray-100">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-2">
              <MapPin className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-black text-gray-900 uppercase tracking-widest">{t("profile_social_group") || "Socio-demographic Category"}</h3>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                {t("profile_caste_category") || "Caste / Reservation Category"}
              </label>
              <select
                name="caste"
                value={formData.caste}
                onChange={handleInputChange}
                className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
              >
                {CASTE_OPTIONS.map((cst) => (
                  <option key={cst} value={cst}>{cst}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Section 4: System Preferences (Language & Accessibility) */}
          <div className="space-y-4 pt-4 border-t border-gray-100">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-2">
              <Eye className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-black text-gray-900 uppercase tracking-widest">{t("profile_sys_prefs") || "System Preferences & Accessibility"}</h3>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_pref_language") || "Preferred Portal Language"}
                </label>
                <select
                  name="preferredLanguage"
                  value={formData.preferredLanguage}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  <option value="en">English</option>
                  <option value="hi">हिंदी (Hindi)</option>
                  <option value="ta">தமிழ் (Tamil)</option>
                  <option value="te">తెలుగు (Telugu)</option>
                  <option value="kn">ಕನ್ನಡ (Kannada)</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_font_size") || "Display Font Size"}
                </label>
                <select
                  name="fontSize"
                  value={formData.fontSize}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  <option value="normal">{t("profile_font_normal") || "Normal Text"}</option>
                  <option value="large">{t("profile_font_large") || "Large Text"}</option>
                  <option value="xlarge">{t("profile_font_xlarge") || "Extra Large Text"}</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_contrast_mode") || "Color Contrast Mode"}
                </label>
                <select
                  name="highContrast"
                  value={formData.highContrast}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  <option value="standard">{t("profile_contrast_standard") || "Standard Contrast"}</option>
                  <option value="high-contrast">{t("profile_contrast_high") || "High Contrast Mode"}</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  {t("profile_audio_narration") || "Audio Screen Narration"}
                </label>
                <select
                  name="audioGuidance"
                  value={formData.audioGuidance}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  <option value="disabled">{t("profile_audio_disabled") || "Disabled"}</option>
                  <option value="enabled">{t("profile_audio_enabled") || "Enabled (Text-to-Speech Assistance)"}</option>
                </select>
              </div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex justify-end pt-4 border-t border-gray-100">
            <button
              type="submit"
              className="inline-flex items-center gap-2 bg-government-blue hover:bg-government-blue-dark text-white text-xs font-bold px-5 py-3 rounded-xl shadow-xs transition active:scale-95"
            >
              <Save className="h-4.5 w-4.5" />
              {t("profile_save_btn") || "Save Profile Attributes"}
            </button>
          </div>
        </div>

        {/* Right Side Column: Documents List & Security */}
        <div className="space-y-6">

          {/* Document Vault Sync */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-xs space-y-4">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
              <FileText className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-bold text-gray-900 uppercase tracking-widest">{t("profile_vault_status") || "Document Vault Status"}</h3>
            </div>

            {documents.length === 0 ? (
              <div className="text-center py-6 text-gray-400">
                <FileText className="h-8 w-8 mx-auto mb-2 opacity-30" />
                <p className="text-xs">{t("profile_no_docs") || "No documents uploaded."}</p>
                <Link to="/documents" className="text-xs text-government-blue font-bold underline mt-1 inline-block">
                  {t("profile_go_vault") || "Go to Document Vault"}
                </Link>
              </div>
            ) : (
              <div className="space-y-2">
                {documents.map((doc) => (
                  <div key={doc.id} className="border border-gray-100 p-3 rounded-xl bg-gray-50/50 flex flex-col gap-1.5 text-xs">
                    <div className="flex justify-between items-center">
                      <span className="font-bold text-gray-800 truncate pr-1">{doc.name}</span>
                      <span className={`text-[8px] font-black uppercase px-2 py-0.5 rounded border ${
                        doc.status === "verified"
                          ? "bg-india-green/10 border-india-green/20 text-india-green"
                          : doc.status === "uploaded" || doc.status === "pending_review"
                          ? "bg-saffron/10 border-saffron/20 text-saffron-dark"
                          : "bg-red-50 border-red-200 text-red-700"
                      }`}>
                        {doc.status === "pending_review" ? (t("vault_status_pending") || "Pending Review") : doc.status}
                      </span>
                    </div>
                    <div className="flex justify-between text-[10px] text-gray-400 font-semibold mt-0.5 leading-none">
                      <span>Updated: {doc.date}</span>
                      <span>Issuer: {doc.issuer ? doc.issuer.split(" ")[0] : "Govt"}</span>
                    </div>
                  </div>
                ))}

                <div className="pt-2">
                  <Link
                    to="/documents"
                    className="w-full inline-flex items-center justify-center gap-1.5 bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700 text-xs font-bold py-2 rounded-xl transition"
                  >
                    {t("profile_manage_vault") || "Manage Document Vault"}
                  </Link>
                </div>
              </div>
            )}
          </div>

          {/* Secure Aadhaar Session Details */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-xs space-y-4">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
              <ShieldCheck className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-bold text-gray-900 uppercase tracking-widest">{t("profile_auth_security") || "Authentication Security"}</h3>
            </div>

            <div className="flex items-center justify-between p-3.5 bg-gray-50 border border-gray-200 rounded-xl">
              <div>
                <p className="text-xs font-bold text-gray-700">{t("profile_mfa_title") || "Aadhaar MFA Protection"}</p>
                <p className="text-[9px] text-gray-400 mt-0.5 leading-snug">
                  {t("profile_mfa_desc") || "Require Aadhaar OTP for final application disbursements."}
                </p>
              </div>
              <button
                type="button"
                onClick={toggleMfa}
                className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                  mfaEnabled ? "bg-government-blue" : "bg-gray-300"
                }`}
                role="switch"
                aria-checked={mfaEnabled}
              >
                <span
                  className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow-xs transition duration-200 ease-in-out ${
                    mfaEnabled ? "translate-x-4" : "translate-x-0"
                  }`}
                />
              </button>
            </div>

            <div className="bg-gradient-to-br from-government-blue-dark to-gray-900 text-government-blue-light p-4 border border-government-blue/20 rounded-xl space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="opacity-70">{t("profile_session_status") || "Active Session Status"}</span>
                <span className="text-india-green font-bold">{t("profile_secure_ssl") || "Secure SSL Link"}</span>
              </div>
              <div className="flex justify-between">
                <span className="opacity-70">{t("profile_audited_ip") || "Audited IP Address"}</span>
                <span className="font-mono text-[10px]">152.168.1.42</span>
              </div>
              <div className="flex justify-between">
                <span className="opacity-70">{t("profile_sync_token") || "Central Sync Token"}</span>
                <span className="font-mono text-[10px]">SB-9023-F2X</span>
              </div>
            </div>
          </div>

        </div>

      </form>

    </div>
  );
}
