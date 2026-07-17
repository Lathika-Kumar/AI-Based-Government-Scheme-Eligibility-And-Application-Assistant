import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { validateSchema, getFieldError, loginSchema } from "@utils/validation";
import {
  Building2, Mail, Lock, ArrowRight, AlertCircle, UserCheck, ShieldCheck
} from "lucide-react";

export default function Login() {
  const { login, quickLogin } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail]             = useState("");
  const [password, setPassword]       = useState("");
  const [error, setError]             = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading]         = useState(false);
  const [loadingPhase, setLoadingPhase] = useState("");

  const ADMIN_DOMAIN_PATTERN = /^.+@schemebridge\.gov\.in$/i;
  const isAdminEmail = ADMIN_DOMAIN_PATTERN.test(email);

  const redirectAfterLogin = (loggedInUser) => {
    const adminRoles = ["super_admin", "verification_officer", "scheme_manager"];
    const isUserAdmin = adminRoles.includes(loggedInUser.role);

    if (isUserAdmin) {
      navigate("/admin/dashboard");
    } else if (!loggedInUser.onboardingComplete) {
      navigate("/onboarding");
    } else {
      navigate("/dashboard");
    }
  };

  const handleQuickLogin = async (roleType) => {
    setError("");
    setLoading(true);
    setLoadingPhase("Signing in...");
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

    const validationResult = validateSchema(loginSchema, { email, password });
    if (!validationResult.success) {
      setFieldErrors(validationResult.errors);
      return;
    }

    setLoading(true);
    setLoadingPhase("Signing in...");
    await new Promise(r => setTimeout(r, 400));

    const result = login(email, password);
    if (result.error) {
      setError(result.error);
      setLoading(false);
      setLoadingPhase("");
      return;
    }

    setLoadingPhase("Signing in...");
    await new Promise(r => setTimeout(r, 350));

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
          <p className="text-white/80 text-sm font-medium">Government of India Welfare Portal</p>
        </div>

        <div className="bg-white rounded-xl shadow-2xl overflow-hidden border border-gray-200">
          <div className="bg-gray-50 px-8 py-6 text-center border-b border-gray-200">
            <h2 className="text-xl font-bold text-gray-900">Sign In</h2>
            <p className="text-gray-600 text-sm mt-1 leading-relaxed">
              Access your citizen profile and welfare scheme applications
            </p>
          </div>

          <div className="px-8 py-7 space-y-6">
            {error && (
              <div className="flex items-start gap-2.5 bg-red-50 border border-red-200 text-red-700 text-sm p-3.5 rounded-lg">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="font-medium text-xs leading-normal">{error}</span>
              </div>
            )}

            <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 space-y-3">
              <span className="text-xs font-bold text-gray-500 uppercase tracking-wider block text-center">
                Quick Demo Access
              </span>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => handleQuickLogin("citizen")}
                  disabled={loading}
                  className="bg-white hover:bg-government-blue/5 hover:border-government-blue/30 border border-gray-200 rounded-lg py-3 px-3 text-left transition text-xs flex flex-col justify-between"
                >
                  <span className="font-bold text-gray-800">Citizen Profile</span>
                  <span className="text-[11px] text-gray-500">Demo Access</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickLogin("admin")}
                  disabled={loading}
                  className="bg-white hover:bg-government-blue/5 hover:border-government-blue/30 border border-gray-200 rounded-lg py-3 px-3 text-left transition text-xs flex flex-col justify-between"
                >
                  <span className="font-bold text-gray-800">Administrator</span>
                  <span className="text-[11px] text-gray-500">Officer Access</span>
                </button>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label htmlFor="login-email" className="block text-sm font-medium text-gray-700 mb-1.5">
                  Email Address
                </label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="login-email"
                    type="email"
                    placeholder="Enter your registered email"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setError("");
                      setFieldErrors({});
                    }}
                    disabled={loading}
                    className={`w-full pl-10 pr-4 py-3 bg-gray-50 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:bg-white transition ${
                      fieldErrors.email ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 focus:ring-government-blue'
                    }`}
                    autoComplete="email"
                    aria-invalid={!!fieldErrors.email}
                    aria-describedby={fieldErrors.email ? "email-error" : undefined}
                  />
                </div>
                {fieldErrors.email && (
                  <p id="email-error" className="mt-1.5 text-xs text-red-600 font-medium flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {fieldErrors.email}
                  </p>
                )}
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label htmlFor="login-password" className="block text-sm font-medium text-gray-700">
                    Password
                  </label>
                  <button
                    type="button"
                    onClick={() => alert("Mock password recovery: Use 'demo123' for standard demo users.")}
                    className="text-xs font-medium text-government-blue hover:underline"
                  >
                    Forgot Password?
                  </button>
                </div>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="login-password"
                    type="password"
                    placeholder="Enter your password"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      setError("");
                      setFieldErrors({});
                    }}
                    disabled={loading}
                    className={`w-full pl-10 pr-4 py-3 bg-gray-50 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:bg-white transition ${
                      fieldErrors.password ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 focus:ring-government-blue'
                    }`}
                    autoComplete="current-password"
                    aria-invalid={!!fieldErrors.password}
                    aria-describedby={fieldErrors.password ? "password-error" : undefined}
                  />
                </div>
                {fieldErrors.password && (
                  <p id="password-error" className="mt-1.5 text-xs text-red-600 font-medium flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {fieldErrors.password}
                  </p>
                )}
              </div>

              <button
                id="login-submit"
                type="submit"
                disabled={loading}
                className="w-full bg-government-blue hover:bg-government-blue-dark disabled:opacity-70 text-white py-3.5 rounded-lg text-sm font-bold flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition duration-200"
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
              <div className="text-center text-sm text-gray-600 pt-3 border-t border-gray-100">
                New to SchemeBridge?{" "}
                <Link to="/signup" className="text-government-blue hover:text-government-blue-dark font-bold hover:underline ml-1">
                  Register Now
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
            <span>Government Verified</span>
          </div>
        </div>
      </div>
    </div>
  );
}
