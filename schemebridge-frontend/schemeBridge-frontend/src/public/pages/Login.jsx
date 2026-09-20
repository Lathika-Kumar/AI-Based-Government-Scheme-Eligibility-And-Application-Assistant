import React, { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { storage } from "@utils/apiClient";

import { useToast } from "@components/ui/ToastNotification";
import { validateSchema, loginSchema } from "@utils/validation";
import { IS_FLOW_TEST_MODE } from "@utils/flowTestMode";
import {
  Building2, Mail, Lock, ArrowRight, AlertCircle, Eye, EyeOff, ShieldCheck, UserCheck
} from "lucide-react";


export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const showToast = useToast();

  const [email, setEmail]             = useState("");
  const [password, setPassword]       = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError]             = useState("");

  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading]         = useState(false);
  const [loadingPhase, setLoadingPhase] = useState("");

  const ADMIN_DOMAIN_PATTERN = /^.+@schemebridge\.gov\.in$/i;
  const isAdminEmail = ADMIN_DOMAIN_PATTERN.test(email);
  const location = useLocation();


  const redirectAfterLogin = (loggedInUser) => {
    // Determine return route from navigation state or storage
    const targetFromState = location.state?.from?.pathname
      ? location.state.from.pathname + (location.state.from.search || "")
      : null;
    const targetFromStorage = storage.getLastProtectedRoute();
    const candidateRoute = targetFromState || targetFromStorage;

    // Clear saved return route once consumed
    storage.clearLastProtectedRoute();

    const isAdmin = loggedInUser.role === "admin" || loggedInUser.role === "scheme_manager" || loggedInUser.role === "verification_officer";

    // 1. Admin, Scheme Managers & Verification Officers
    if (isAdmin) {
      if (candidateRoute && candidateRoute.startsWith("/admin")) {
        navigate(candidateRoute, { replace: true });
        return;
      }
      navigate("/admin/dashboard", { replace: true });
      return;
    }

    // 2. Citizens requiring onboarding
    if (!loggedInUser.onboardingComplete) {
      navigate("/onboarding", { replace: true });
      return;
    }

    // 3. Active Citizens with completed profile
    if (candidateRoute && !candidateRoute.startsWith("/admin") && !candidateRoute.startsWith("/login") && !candidateRoute.startsWith("/signup")) {
      navigate(candidateRoute, { replace: true });
      return;
    }

    navigate("/dashboard", { replace: true });
  };


  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setFieldErrors({});

    // TEMPORARY TEST MODE: Skip client-side schema validation when test mode is enabled
    if (!IS_FLOW_TEST_MODE) {
      const validationResult = validateSchema(loginSchema, { email, password });
      if (!validationResult.success) {
        setFieldErrors(validationResult.errors);
        return;
      }
    }


    setLoading(true);
    setLoadingPhase("Signing In...");

    const result = await login(email, password);
    if (result.error) {
      setError(result.error);
      setLoading(false);
      setLoadingPhase("");
      return;
    }

    setLoading(false);
    setLoadingPhase("");
    redirectAfterLogin(result.user);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-government-blue via-government-blue-light to-government-blue flex items-center justify-center px-4 py-12 relative overflow-hidden">
      <div className="absolute top-1/4 left-1/4 w-80 h-80 bg-saffron/10 rounded-full blur-3xl"></div>
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-india-green/10 rounded-full blur-3xl"></div>

      <div className="w-full max-w-md relative z-10 space-y-6">
        <div className="h-2 bg-gradient-to-r from-saffron via-white-official to-india-green rounded-full"></div>

        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center bg-white p-3 rounded-xl shadow-lg">
            <Building2 className="h-8 w-8 text-government-blue" />
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">SchemeBridge</h1>
          <p className="text-white/80 text-sm font-medium">National Public Welfare & Scheme Eligibility Platform</p>
        </div>

        <div className="bg-white dark:bg-slate-900 rounded-xl shadow-2xl overflow-hidden border border-gray-200 dark:border-slate-800">
          <div className="bg-gray-50 dark:bg-slate-800/80 px-8 py-6 text-center border-b border-gray-200 dark:border-slate-800">
            <h2 className="text-xl font-bold text-gray-900 dark:text-slate-100">Sign In</h2>
            <p className="text-gray-600 dark:text-slate-300 text-sm mt-1 leading-relaxed">
              Access your eligibility dashboard or administrative portal
            </p>
          </div>

          <div className="px-8 py-7 space-y-6">
            {error && (
              <div className="flex items-start gap-2.5 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900/50 text-red-700 dark:text-red-300 text-sm p-3.5 rounded-lg">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="font-medium text-xs leading-normal">{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label htmlFor="login-email" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                  Email Address
                </label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="login-email"
                    type="email"
                    placeholder="citizen@schemebridge.in"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setError("");
                      setFieldErrors({});
                    }}
                    disabled={loading}
                    className={`w-full pl-10 pr-4 py-3 bg-gray-50 dark:bg-slate-800 border rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-800 transition ${
                      fieldErrors.email ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 dark:border-slate-700 focus:ring-government-blue'
                    }`}
                    autoComplete="email"
                  />
                </div>
                {fieldErrors.email && (
                  <p id="email-error" className="mt-1.5 text-xs text-red-600 dark:text-red-400 font-medium flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {fieldErrors.email}
                  </p>
                )}
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label htmlFor="login-password" className="block text-sm font-medium text-gray-700 dark:text-slate-300">
                    Password
                  </label>
                  <Link
                    to="/forgot-password"
                    className="text-xs font-medium text-government-blue dark:text-indigo-400 hover:underline"
                  >
                    Forgot Password?
                  </Link>
                </div>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="login-password"
                    type={showPassword ? "text" : "password"}
                    placeholder="Enter your password"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      setError("");
                      setFieldErrors({});
                    }}
                    disabled={loading}
                    className={`w-full pl-10 pr-11 py-3 bg-gray-50 dark:bg-slate-800 border rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:bg-white dark:focus:bg-slate-800 transition ${
                      fieldErrors.password ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 dark:border-slate-700 focus:ring-government-blue'
                    }`}
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 focus:outline-none focus:text-government-blue rounded cursor-pointer transition"
                    aria-label={showPassword ? "Hide password" : "Show password"}
                    aria-pressed={showPassword}
                    tabIndex={0}
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4" aria-hidden="true" />
                    ) : (
                      <Eye className="h-4 w-4" aria-hidden="true" />
                    )}
                  </button>
                </div>
                {fieldErrors.password && (
                  <p id="password-error" className="mt-1.5 text-xs text-red-600 dark:text-red-400 font-medium flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {fieldErrors.password}
                  </p>
                )}
              </div>


              <button
                id="login-submit"
                type="submit"
                disabled={loading}
                className="w-full bg-government-blue hover:bg-government-blue-dark disabled:opacity-70 text-white py-3.5 rounded-lg text-sm font-bold flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition duration-200 cursor-pointer"
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
                  <>Sign In <ArrowRight className="h-4 w-4" /></>
                )}
              </button>
            </form>

            {!isAdminEmail && (
              <div className="text-center text-sm text-gray-600 dark:text-slate-400 pt-3 border-t border-gray-100 dark:border-slate-800">
                New to SchemeBridge? {" "}
                <Link to="/signup" className="text-government-blue dark:text-indigo-400 hover:text-government-blue-dark font-bold hover:underline ml-1">
                  Create an Account
                </Link>
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center justify-center gap-4 text-xs text-white/70">
          <div className="flex items-center gap-1">
            <ShieldCheck className="h-3.5 w-3.5" />
            <span>Secure</span>
          </div>
          <div className="flex items-center gap-1">
            <UserCheck className="h-3.5 w-3.5" />
            <span>Encrypted Platform</span>
          </div>
        </div>
      </div>
    </div>
  );
}
