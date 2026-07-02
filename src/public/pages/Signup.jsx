import React, { useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import {
  Building2, User, Mail, Lock, ArrowRight, AlertCircle, CheckCircle2, Sparkles
} from "lucide-react";

export default function Signup() {
  const { signup } = useAuth();
  const { t } = useApp();
  const navigate = useNavigate();
  const location = useLocation();

  const [name, setName]         = useState("");
  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState("");
  const [consent, setConsent]   = useState(false);
  const [error, setError]       = useState("");
  const [loading, setLoading]   = useState(false);
  const [hasPrefill, setHasPrefill] = useState(false);

  // Check for landing page calculator prefill
  useEffect(() => {
    const isPrefillQuery = location.search.includes("prefill=1");
    const prefillData = localStorage.getItem("schemebridge_prefill_profile");
    if (isPrefillQuery && prefillData) {
      setHasPrefill(true);
      try {
        const parsed = JSON.parse(prefillData);
        // We can pre-fill name or email if we had it, but here we just show a banner.
      } catch (e) {
        // Ignore
      }
    }
  }, [location.search]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!name.trim())  {
 setError(t("login_error_name_req") || "Full name is required."); return;
}
    if (!email.trim()) {
 setError(t("login_error_email_req") || "Email address is required."); return;
}
    if (!password)     {
 setError(t("login_error_pass_req") || "Password is required."); return;
}
    if (password.length < 6) {
 setError(t("login_error_pass_min") || "Password must be at least 6 characters."); return;
}
    if (!consent)      {
 setError(t("login_error_consent_req") || "You must agree to the data consent policy."); return;
}

    setLoading(true);
    await new Promise(r => setTimeout(r, 450));

    // Attempt registration
    const result = signup(name.trim(), email.trim(), password);
    setLoading(false);

    if (result.error) {
      setError(result.error);
      return;
    }

    // Redirect to onboarding (starts at AI Welcome Screen)
    navigate("/onboarding");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 flex items-center justify-center px-4 py-12 relative overflow-hidden">
      {/* Background decorations */}
      <div className="absolute top-1/4 left-1/4 w-80 h-80 bg-indigo-650/10 rounded-full blur-3xl"></div>
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-indigo-505 bg-indigo-600/5 rounded-full blur-3xl"></div>

      <div className="w-full max-w-md relative z-10 space-y-6">
        {/* Brand Logo & Name */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center bg-indigo-600 h-14 w-14 rounded-2xl shadow-xl hover:scale-105 transition duration-300">
            <Building2 className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">SchemeBridge</h1>
          <p className="text-indigo-200 text-xs tracking-wide uppercase font-semibold">{t("login_brand_subtitle") || "National e-Governance Welfare Portal"}</p>
        </div>

        {/* Form Card */}
        <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200/80">
          {/* Card Header */}
          <div className="bg-slate-900 px-8 py-7 text-center sm:text-left border-b border-slate-800">
            <h2 className="text-xl font-extrabold text-white">{t("signup_title") || "Register Account"}</h2>
            <p className="text-slate-400 text-xs mt-1.5 leading-relaxed">
              {t("signup_subtitle") || "Access central and state programs with one unified digital profile."}
            </p>
          </div>

          <div className="px-8 py-7 space-y-5">
            {/* Prefill indicator */}
            {hasPrefill && (
              <div className="flex items-center gap-2 bg-indigo-50 border border-indigo-200 text-indigo-850 text-indigo-750 text-indigo-700 p-3 rounded-xl text-xs font-semibold leading-relaxed">
                <Sparkles className="h-4 w-4 text-indigo-600 shrink-0" />
                <span>{t("signup_prefill_msg") || "We saved your eligibility inputs! We will apply them in the wizard after registration."}</span>
              </div>
            )}

            {/* Error banner */}
            {error && (
              <div className="flex items-start gap-2.5 bg-rose-50 border border-rose-200 text-rose-700 text-sm p-3.5 rounded-xl">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="font-semibold text-xs leading-normal">{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Full Name */}
              <div>
                <label htmlFor="signup-name" className="block text-[10px] font-extrabold text-slate-550 mb-1.5 uppercase tracking-wide text-slate-600">
                  {t("signup_full_name") || "Full Name"} <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <User className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <input
                    id="signup-name"
                    type="text"
                    placeholder={t("signup_name_placeholder") || "e.g. Priya Sharma"}
                    value={name}
                    onChange={(e) => {
 setName(e.target.value); setError("");
}}
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-55 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition"
                    autoComplete="name"
                  />
                </div>
              </div>

              {/* Email */}
              <div>
                <label htmlFor="signup-email" className="block text-[10px] font-extrabold text-slate-550 mb-1.5 uppercase tracking-wide text-slate-600">
                  {t("login_email_label") || "Email Address"} <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <input
                    id="signup-email"
                    type="email"
                    placeholder={t("login_email_placeholder") || "name@example.com"}
                    value={email}
                    onChange={(e) => {
 setEmail(e.target.value); setError("");
}}
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition"
                    autoComplete="email"
                  />
                </div>
              </div>

              {/* Password */}
              <div>
                <label htmlFor="signup-password" className="block text-[10px] font-extrabold text-slate-550 mb-1.5 uppercase tracking-wide text-slate-600">
                  {t("login_password_label") || "Choose Password"} <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <input
                    id="signup-password"
                    type="password"
                    placeholder={t("login_password_min_placeholder") || "Minimum 6 characters"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition"
                    autoComplete="new-password"
                  />
                </div>
              </div>

              {/* Consent */}
              <label className="flex items-start gap-3 cursor-pointer group pt-1">
                <div className="relative mt-0.5 shrink-0">
                  <input
                    id="signup-consent"
                    type="checkbox"
                    checked={consent}
                    onChange={(e) => {
 setConsent(e.target.checked); setError("");
}}
                    className="sr-only"
                  />
                  <div className={`h-5 w-5 rounded-md border-2 flex items-center justify-center transition
                    ${consent ? "bg-indigo-600 border-indigo-600 text-white" : "border-slate-350 bg-white group-hover:border-indigo-400"}`}>
                    {consent && <CheckCircle2 className="h-3.5 w-3.5 text-white" />}
                  </div>
                </div>
                <span className="text-[11px] text-slate-500 leading-relaxed select-none">
                  {t("signup_agree") || "I agree to let SchemeBridge securely verify my socio-economic parameters and audit uploaded documents under official Digital India e-Governance directives."}
                </span>
              </label>

              <button
                id="signup-submit"
                type="submit"
                disabled={loading}
                className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white py-3 rounded-xl text-sm font-semibold flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition"
              >
                {loading ? (
                  <span className="flex items-center gap-2">
                    <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
                    </svg>
                    {t("signup_creating") || "Creating account…"}
                  </span>
                ) : (
                  <>{t("signup_btn_submit") || "Create Citizen Account"} <ArrowRight className="h-4 w-4" /></>
                )}
              </button>
            </form>

            <p className="text-center text-xs text-slate-500 pt-1">
              {t("signup_already_registered") || "Already registered?"}{" "}
              <Link to="/login" className="text-indigo-600 hover:text-indigo-800 font-semibold underline ml-1">
                {t("login_btn_submit") || "Sign In"}
              </Link>
            </p>
          </div>
        </div>

        <p className="text-center text-[10px] text-slate-400 mt-6 tracking-wide uppercase font-medium">
          {t("signup_secured_sandbox") || "🇮🇳 Secured Government Welfare Simulator Sandbox"}
        </p>
      </div>
    </div>
  );
}
