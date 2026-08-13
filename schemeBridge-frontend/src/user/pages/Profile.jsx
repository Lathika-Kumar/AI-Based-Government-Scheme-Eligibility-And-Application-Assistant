import React, { useState, useEffect, useMemo } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import profileService from "@services/profileService";
import {
  EDUCATION_OPTIONS,
  OCCUPATION_OPTIONS,
  CASTE_OPTIONS,
  INDIAN_STATES_AND_UTS,
  calculateCompletion
} from "@data/mockProfile";
import { FormSectionSkeleton } from "@components/ui/LoadingSkeleton";
import BackButton from "@components/ui/BackButton";
import { usePageMeta } from "@utils/usePageMeta";
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
  usePageMeta("Eligibility Profile", "Manage your socio-economic profile");
  const { user, updateUser } = useAuth();
  const { profile, updateProfile, documents, addAuditLog } = useApp();
  const navigate = useNavigate();

  // Initializing state directly from context / localStorage values
  const [formData, setFormData] = useState({
    name: profile?.name || user?.name || "",
    age: profile?.age || user?.age || "32",
    gender: profile?.gender || user?.gender || "Male",
    state: profile?.state || user?.state || "Gujarat",
    district: profile?.district || user?.district || "",
    education: profile?.education || "Graduate / Bachelor's Degree",
    occupation: profile?.occupation || user?.occupation || "Farmer",
    income: profile?.annualIncome || user?.income || "180000",
    caste: profile?.caste || user?.caste || "OBC",
    fontSize: profile?.fontSize || "normal",
    highContrast: profile?.highContrast || "standard",
    audioGuidance: profile?.audioGuidance || "disabled"
  });

  const [mfaEnabled, setMfaEnabled] = useState(() => {
    return localStorage.getItem("schemebridge_mfa_enabled") === "true";
  });

  const [savedSuccess, setSavedSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [errors, setErrors] = useState({});

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
    // Clear the error for this field as the user types
    setErrors((prev) => ({ ...prev, [name]: "" }));
  };

  const handleSave = async (e) => {
    e.preventDefault();

    // ── Inline validation ───────────────────────────────────────────────────
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = "Full name is required";
    }

    const ageNum = Number(formData.age);
    if (!formData.age || isNaN(ageNum) || ageNum < 1 || ageNum > 120) {
      newErrors.age = "Enter a valid age (1–120)";
    }

    if (!formData.district.trim()) {
      newErrors.district = "District is required";
    }

    const incomeNum = Number(formData.income);
    if (formData.income === "" || isNaN(incomeNum) || incomeNum < 0) {
      newErrors.income = "Income must be a non-negative number";
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return; // Prevent save and navigation
    }

    setErrors({});
    setIsSaving(true);

    const profilePayload = {
      fullName: formData.name,
      age: Number(formData.age),
      gender: formData.gender,
      state: formData.state,
      district: formData.district,
      annualIncome: Number(formData.income),
      category: formData.caste,
      occupation: formData.occupation,
      qualification: formData.education,
    };

    try {
      const updatedProfile = await profileService.updateProfile(profilePayload);

      const authUpdates = {
        name: updatedProfile.fullName ?? formData.name,
        fullName: updatedProfile.fullName ?? formData.name,
        age: updatedProfile.age ?? Number(formData.age),
        gender: updatedProfile.gender ?? formData.gender,
        occupation: updatedProfile.occupation ?? formData.occupation,
        annualIncome: updatedProfile.annualIncome ?? Number(formData.income),
        income: updatedProfile.annualIncome ?? Number(formData.income),
        caste: updatedProfile.category ?? formData.caste,
        state: updatedProfile.state ?? formData.state,
        district: updatedProfile.district ?? formData.district,
        onboardingComplete: updatedProfile.onboardingCompleted === true || updatedProfile.onboardingComplete === true || user?.onboardingComplete,
      };

      updateProfile({
        name: updatedProfile.fullName ?? formData.name,
        age: updatedProfile.age ?? Number(formData.age),
        gender: updatedProfile.gender ?? formData.gender,
        state: updatedProfile.state ?? formData.state,
        district: updatedProfile.district ?? formData.district,
        education: updatedProfile.qualification ?? formData.education,
        occupation: updatedProfile.occupation ?? formData.occupation,
        annualIncome: updatedProfile.annualIncome ?? Number(formData.income),
        caste: updatedProfile.category ?? formData.caste,
        isComplete: true,
      });

      updateUser(authUpdates);

      addAuditLog(
        "Profile Update",
        "Profile",
        formData.name || "Citizen",
        `Socio-economic criteria updated. New Match readiness sync triggered.`,
        formData.name || "Citizen"
      );

      setSavedSuccess(true);
      setIsSaving(false);
      setTimeout(() => {
        setSavedSuccess(false);
        navigate("/dashboard");
      }, 1200);
    } catch (err) {
      console.error("Profile update failed:", err);
      setErrors({ form: err.message || "Unable to save profile. Please try again." });
      setIsSaving(false);
    }
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
    <form onSubmit={handleSave} className="space-y-6">

      {/* Page Header */}
      <div className="bg-white border border-gray-200 p-6 rounded-2xl shadow-xs space-y-4">
        <BackButton fallbackPath="/dashboard" variant="ghost" label="Back to Dashboard" />
        <div>
          <h1 className="text-xl font-black text-gray-900 tracking-tight">Eligibility Profile</h1>
          <p className="text-xs text-gray-500 mt-0.5">
            Keep your socio-economic attributes accurate. These fields determine matching government schemes.
          </p>
        </div>

        {/* Dynamic Completion Percentage Bar */}
        <div className="bg-gray-50 border border-gray-200 p-4 rounded-xl space-y-2">
          <div className="flex justify-between items-center text-xs">
            <span className="font-bold text-gray-700">Profile Completion Progress</span>
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
              ? "✓ Profile completely populated. Match engine operating at maximum efficiency."
              : "Complete the remaining demographic parameters to receive fully authenticated recommendation files."}
          </p>
        </div>
      </div>

      {savedSuccess && (
        <div className="bg-india-green/10 border border-india-green/20 text-india-green-dark text-xs p-4 rounded-xl flex items-center gap-2 shadow-xs">
          <Sparkles className="h-4 w-4 text-india-green shrink-0" />
          <span>Profile configuration successfully saved! Redirecting to Dashboard...</span>
        </div>
      )}
      {errors.form && (
        <div className="bg-rose-50 border border-rose-200 text-rose-700 text-xs p-4 rounded-xl shadow-xs">
          {errors.form}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-6">

          {/* Section 1: Personal Information */}
          <div className="space-y-4">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-2">
              <User className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-black text-gray-900 uppercase tracking-widest">Personal Information</h3>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="profile-name" className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Full Name
                </label>
                <input
                  id="profile-name"
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  required
                  aria-required="true"
                  aria-invalid={!!errors.name}
                  placeholder="Enter your name"
                  className={`w-full text-xs px-3.5 py-2.5 border rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-1 transition ${
                    errors.name
                      ? "border-red-400 focus:ring-red-400 ring-2 ring-red-400"
                      : "border-gray-200 focus:ring-government-blue"
                  }`}
                />
                {errors.name && <p className="text-red-500 text-[10px] mt-1">{errors.name}</p>}
              </div>

              <div>
                <label htmlFor="profile-age" className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Age (Years)
                </label>
                <input
                  id="profile-age"
                  type="number"
                  name="age"
                  value={formData.age}
                  onChange={handleInputChange}
                  required
                  aria-required="true"
                  aria-invalid={!!errors.age}
                  min="1"
                  max="120"
                  className={`w-full text-xs px-3.5 py-2.5 border rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-1 transition ${
                    errors.age
                      ? "border-red-400 focus:ring-red-400 ring-2 ring-red-400"
                      : "border-gray-200 focus:ring-government-blue"
                  }`}
                />
                {errors.age && <p className="text-red-500 text-[10px] mt-1">{errors.age}</p>}
              </div>

              <div>
                <label htmlFor="profile-gender" className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Gender
                </label>
                <select
                  id="profile-gender"
                  name="gender"
                  value={formData.gender}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <div>
                <label htmlFor="profile-state" className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  State of Residence
                </label>
                <select
                  id="profile-state"
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

              <div>
                <label htmlFor="profile-district" className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  District
                </label>
                <input
                  id="profile-district"
                  type="text"
                  name="district"
                  value={formData.district}
                  onChange={handleInputChange}
                  placeholder="e.g. Ahmedabad"
                  className={`w-full text-xs px-3.5 py-2.5 border rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-1 transition ${errors.district ? "border-red-400 focus:ring-red-400 ring-2 ring-red-400" : "border-gray-200 focus:ring-government-blue"}`}
                />
                {errors.district && <p className="text-red-500 text-[10px] mt-1">{errors.district}</p>}
              </div>
            </div>
          </div>

          {/* Section 2: Education & Occupation */}
          <div className="space-y-4 pt-4 border-t border-gray-100">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-2">
              <Briefcase className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-black text-gray-900 uppercase tracking-widest">Education & Occupation</h3>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="profile-education" className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Educational Qualification
                </label>
                <select
                  id="profile-education"
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
                <label htmlFor="profile-occupation" className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Primary Occupation
                </label>
                <select
                  id="profile-occupation"
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
                <label htmlFor="profile-income" className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Annual Household Income (₹)
                </label>
                <input
                  id="profile-income"
                  type="number"
                  name="income"
                  value={formData.income}
                  onChange={handleInputChange}
                  required
                  aria-required="true"
                  aria-invalid={!!errors.income}
                  min="0"
                  className={`w-full text-xs px-3.5 py-2.5 border rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-1 transition ${
                    errors.income
                      ? "border-red-400 focus:ring-red-400 ring-2 ring-red-400"
                      : "border-gray-200 focus:ring-government-blue"
                  }`}
                />
                {errors.income && <p className="text-red-500 text-[10px] mt-1">{errors.income}</p>}
                <span className="text-[9px] text-gray-400 font-semibold block mt-1 leading-none">
                  Provide gross annual income. This will be validated against your Income Certificate.
                </span>
              </div>
            </div>
          </div>

          {/* Section 3: Social Group */}
          <div className="space-y-4 pt-4 border-t border-gray-100">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-2">
              <MapPin className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-black text-gray-900 uppercase tracking-widest">Socio-demographic Category</h3>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                Caste / Reservation Category
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
              <h3 className="text-xs font-black text-gray-900 uppercase tracking-widest">System Preferences & Accessibility</h3>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Portal Language
                </label>
                <div className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-100 text-gray-700 font-semibold cursor-not-allowed">
                  English (Default)
                </div>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Display Font Size
                </label>
                <select
                  name="fontSize"
                  value={formData.fontSize}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  <option value="normal">Normal Text</option>
                  <option value="large">Large Text</option>
                  <option value="xlarge">Extra Large Text</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Color Contrast Mode
                </label>
                <select
                  name="highContrast"
                  value={formData.highContrast}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  <option value="standard">Standard Contrast</option>
                  <option value="high-contrast">High Contrast Mode</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  Audio Screen Narration
                </label>
                <select
                  name="audioGuidance"
                  value={formData.audioGuidance}
                  onChange={handleInputChange}
                  className="w-full text-xs border border-gray-200 rounded-xl px-3.5 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100/50 focus:outline-none focus:ring-1 focus:ring-government-blue cursor-pointer"
                >
                  <option value="disabled">Disabled</option>
                  <option value="enabled">Enabled (Text-to-Speech Assistance)</option>
                </select>
              </div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex justify-end pt-4 border-t border-gray-100">
            <button
              type="submit"
              disabled={isSaving}
              className={`inline-flex items-center gap-2 text-white text-xs font-bold px-5 py-3 rounded-xl shadow-xs transition active:scale-95 ${isSaving ? "bg-gray-400 cursor-not-allowed" : "bg-government-blue hover:bg-government-blue-dark"}`}
            >
              <Save className="h-4.5 w-4.5" />
              {isSaving ? "Saving..." : "Save Profile Attributes"}
            </button>
          </div>

        </div>

        {/* Right Side Column: Documents List & Security */}
        <div className="space-y-6">

          {/* Document Vault Sync */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-xs space-y-4">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
              <FileText className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-bold text-gray-900 uppercase tracking-widest">Document Vault Status</h3>
            </div>

            {documents.length === 0 ? (
              <div className="text-center py-6 text-gray-400">
                <FileText className="h-8 w-8 mx-auto mb-2 opacity-30" />
                <p className="text-xs">No documents uploaded.</p>
                <Link to="/documents" className="text-xs text-government-blue font-bold underline mt-1 inline-block">
                  Go to Document Vault
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
                        {doc.status === "pending_review" ? "Pending Review" : doc.status}
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
                    Manage Document Vault
                  </Link>
                </div>
              </div>
            )}
          </div>

          {/* Secure Aadhaar Session Details */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-xs space-y-4">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
              <ShieldCheck className="h-4.5 w-4.5 text-government-blue" />
              <h3 className="text-xs font-bold text-gray-900 uppercase tracking-widest">Authentication Security</h3>
            </div>

            <div className="flex items-center justify-between p-3.5 bg-gray-50 border border-gray-200 rounded-xl">
              <div>
                <p className="text-xs font-bold text-gray-700">Aadhaar MFA Protection</p>
                <p className="text-[9px] text-gray-400 mt-0.5 leading-snug">
                  Require Aadhaar OTP for final application disbursements.
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
                <span className="opacity-70">Active Session Status</span>
                <span className="text-india-green font-bold">Secure SSL Link</span>
              </div>
              <div className="flex justify-between">
                <span className="opacity-70">Audited IP Address</span>
                <span className="font-mono text-[10px]">152.168.1.42</span>
              </div>
              <div className="flex justify-between">
                <span className="opacity-70">Central Sync Token</span>
                <span className="font-mono text-[10px]">SB-9023-F2X</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      </form>
  );
}
