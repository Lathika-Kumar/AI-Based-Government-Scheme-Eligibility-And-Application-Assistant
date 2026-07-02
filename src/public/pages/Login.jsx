import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { validateSchema, getFieldError, loginSchema } from "@utils/validation";
import {
  Building2, Mail, Lock, ArrowRight, AlertCircle, Sparkles, UserCheck, ShieldCheck
} from "lucide-react";

export default function Login() {
  const { login, quickLogin } = useAuth();
  const { t } = useApp();
  const navigate = useNavigate();

  const [email, setEmail]             = useState("");
  const [password, setPassword]       = useState("");
  const [error, setError]             = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading]         = useState(false);
  const [loadingPhase, setLoadingPhase] = useState("");

  const redirectAfterLogin = (loggedInUser) => {
    const isUserAdmin = loggedInUser.role === "SUPER_ADMIN" ||
                        loggedInUser.role === "VERIFICATION_OFFICER" ||
                        loggedInUser.role === "SCHEME_MANAGER";

    if (isUserAdmin) {
      navigate("/admin/dashboard");
    } else if (!loggedInUser.onboardingComplete) {
      // Check if there's prefilled data from landing page calculator
      const hasPrefill = localStorage.getItem("schemebridge_prefill_profile");
      if (hasPrefill && loggedInUser.onboardingStep === 1) {
        // If they have prefilled data, start onboarding
        navigate("/onboarding");
      } else {
        navigate("/onboarding");
      }
    } else {
      navigate("/dashboard");
    }
  };

  const handleQuickLogin = async (roleType) => {
    setError("");
    setLoading(true);
    setLoadingPhase(t("login_signing_in") || "Signing in...");
    await new Promise(r => setTimeout(r, 400));

    const loggedUser = quickLogin(roleType);
    setLoading(false);
    setLoadingPhase("");
    redirectAfterLogin(loggedUser);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setFieldErrors({});

    // Validate form using Zod schema
    const validationResult = validateSchema(loginSchema, { email, password });
    if (!validationResult.success) {
      setFieldErrors(validationResult.errors);
      return;
    }

    setLoading(true);

    setLoadingPhase(t("login_signing_in") || "Signing in...");
    await new Promise(r => setTimeout(r, 400));

    const result = login(email, password);
    if (result.error) {
      setError(result.error);
      setLoading(false);
      setLoadingPhase("");
      return;
    }

    setLoadingPhase(t("login_signing_in") || "Signing in...");
    await new Promise(r => setTimeout(r, 350));

    setLoading(false);
    setLoadingPhase("");
    redirectAfterLogin(result.user);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 flex items-center justify-center px-4 py-12 relative overflow-hidden">
      {/* Decorative Blur elements */}
      <div className="absolute top-1/4 left-1/4 w-80 h-80 bg-indigo-600/10 rounded-full blur-3xl"></div>
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-emerald-500/5 rounded-full blur-3xl"></div>

      <div className="w-full max-w-md relative z-10 space-y-6">
        {/* Brand Logo & Name */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center bg-indigo-600 h-14 w-14 rounded-2xl shadow-xl hover:scale-105 transition duration-300">
            <Building2 className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">SchemeBridge</h1>
          <p className="text-indigo-200 text-xs tracking-wide uppercase font-semibold">{t("login_brand_subtitle")}</p>
        </div>

        {/* Form Card */}
        <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200/80">
          {/* Card Header */}
          <div className="bg-slate-900 px-8 py-7 text-center sm:text-left border-b border-slate-800">
            <h2 className="text-xl font-extrabold text-white">{t("login_header")}</h2>
            <p className="text-slate-400 text-xs mt-1.5 leading-relaxed">
              {t("login_header_desc")}
            </p>
          </div>

          <div className="px-8 py-7 space-y-6">
            {/* Display authentication errors */}
            {error && (
              <div className="flex items-start gap-2.5 bg-rose-50 border border-rose-200 text-rose-700 text-sm p-3.5 rounded-xl">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="font-semibold text-xs leading-normal">{error}</span>
              </div>
            )}

            {/* Quick Demo Login Panel */}
            <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-2.5">
              <span className="text-[10px] font-extrabold text-slate-450 uppercase tracking-wider block text-slate-500 text-center sm:text-left">
                {t("login_sandbox_logins")}
              </span>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => handleQuickLogin("citizen")}
                  disabled={loading}
                  className="bg-white hover:bg-indigo-50 hover:border-indigo-300 border border-slate-200 rounded-xl py-2 px-3 text-left transition text-xs flex flex-col justify-between h-[64px]"
                  title={t("login_citizen_profile")}
                >
                  <span className="font-bold text-slate-800">{t("login_citizen_profile")}</span>
                  <span className="text-[10px] text-slate-400">{t("login_citizen_demo_name")}</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickLogin("admin")}
                  disabled={loading}
                  className="bg-white hover:bg-indigo-50 hover:border-indigo-300 border border-slate-200 rounded-xl py-2 px-3 text-left transition text-xs flex flex-col justify-between h-[64px]"
                  title={t("login_admin_evaluator")}
                >
                  <span className="font-bold text-slate-800">{t("login_admin_evaluator")}</span>
                  <span className="text-[10px] text-slate-400">{t("login_admin_demo_name")}</span>
                </button>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Email Address */}
              <div>
                <label htmlFor="login-email" className="block text-[10px] font-extrabold text-slate-550 mb-1.5 uppercase tracking-wide text-slate-600">
                  {t("login_email_label")}
                </label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <input
                    id="login-email"
                    type="email"
                    placeholder={t("login_email_placeholder_full")}
                    value={email}
                    onChange={(e) => {
 setEmail(e.target.value); setError(""); setFieldErrors({});
}}
                    disabled={loading}
                    className={`w-full pl-10 pr-4 py-3 bg-slate-50 border rounded-xl text-sm focus:outline-none focus:ring-2 focus:bg-white transition ${
                      fieldErrors.email ? 'border-rose-300 focus:ring-rose-500' : 'border-slate-200 focus:ring-indigo-500'
                    }`}
                    autoComplete="email"
                    aria-invalid={!!fieldErrors.email}
                    aria-describedby={fieldErrors.email ? "email-error" : undefined}
                  />
                </div>
                {fieldErrors.email && (
                  <p id="email-error" className="mt-1.5 text-xs text-rose-600 font-medium flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {fieldErrors.email}
                  </p>
                )}
              </div>

              {/* Password */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label htmlFor="login-password" className="block text-[10px] font-extrabold text-slate-550 uppercase tracking-wide text-slate-600">
                    {t("login_password_label")}
                  </label>
                  <button
                    type="button"
                    onClick={() => alert(t("login_forgot_alert") || "Mock password dispatch: Use credentials 'demo123' for standard pre-seeded users.")}
                    className="text-[10px] font-bold text-indigo-600 hover:underline hover:text-indigo-800"
                  >
                    {t("login_forgot_password")}
                  </button>
                </div>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <input
                    id="login-password"
                    type="password"
                    placeholder={t("login_password_placeholder_full")}
                    value={password}
                    onChange={(e) => {
 setPassword(e.target.value); setError(""); setFieldErrors({});
}}
                    disabled={loading}
                    className={`w-full pl-10 pr-4 py-3 bg-slate-50 border rounded-xl text-sm focus:outline-none focus:ring-2 focus:bg-white transition ${
                      fieldErrors.password ? 'border-rose-300 focus:ring-rose-500' : 'border-slate-200 focus:ring-indigo-500'
                    }`}
                    autoComplete="current-password"
                    aria-invalid={!!fieldErrors.password}
                    aria-describedby={fieldErrors.password ? "password-error" : undefined}
                  />
                </div>
                {fieldErrors.password && (
                  <p id="password-error" className="mt-1.5 text-xs text-rose-600 font-medium flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {fieldErrors.password}
                  </p>
                )}
              </div>

              {/* Submit Button & Interactive Loading Phases */}
              <button
                id="login-submit"
                type="submit"
                disabled={loading}
                className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-80 text-white py-3.5 rounded-xl text-sm font-bold flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition duration-200"
              >
                {loading ? (
                  <span className="flex items-center gap-2.5">
                    <svg className="animate-spin h-4 w-4 text-white" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
                    </svg>
                    <span className="font-semibold text-xs animate-pulse">{loadingPhase}</span>
                  </span>
                ) : (
<>{t("login_btn_submit")} <ArrowRight className="h-4 w-4" /></>
                )}
              </button>
            </form>

            {/* Registration Prompt */}
            <div className="text-center text-xs text-slate-500 pt-2 border-t border-slate-100">
              {t("login_new_to")}{" "}
              <Link to="/signup" className="text-indigo-600 hover:text-indigo-800 font-bold hover:underline ml-1">
                {t("login_create_account")}
              </Link>
            </div>
          </div>
        </div>
        <p className="text-center text-[10px] text-slate-400 tracking-wide uppercase font-medium">
          {t("login_secured_sandbox")}
        </p>
      </div>
    </div>
  );
}
