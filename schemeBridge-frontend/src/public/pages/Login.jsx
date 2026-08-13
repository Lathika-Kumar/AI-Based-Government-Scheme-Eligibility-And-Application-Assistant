import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useToast } from "@components/ui/ToastNotification";
import { TESTING_MODE } from "@config/constants";
import {
  Building2,
  Mail,
  Lock,
  ArrowRight,
  AlertCircle,
  ShieldCheck,
  UserCheck,
} from "lucide-react";

/**
 * Login page — Email + Password sign-in flow.
 * Immediate redirect to /dashboard (or /admin/dashboard for admin credentials).
 */
export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState("");
  const [error, setError]       = useState("");
  const [loading, setLoading]   = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const trimmedEmail = email.trim();
    if (!TESTING_MODE && !trimmedEmail) {
      setError("Please enter your email address.");
      return;
    }

    setLoading(true);

    const result = await login(trimmedEmail, password);

    setLoading(false);

    if (result?.error) {
      setError(result.error);
      return;
    }

    const isUserAdmin = result?.user?.role === "super_admin" || result?.user?.role === "admin" || trimmedEmail.toLowerCase() === "admin@gmail.com";

    if (isUserAdmin) {
      showToast("Welcome Administrator!", "success");
      navigate("/admin/dashboard");
    } else {
      showToast("Signed in successfully!", "success");
      navigate("/dashboard");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-government-blue via-government-blue-light to-government-blue flex items-center justify-center px-4 py-12 relative overflow-hidden">
      <div className="absolute top-1/4 left-1/4 w-80 h-80 bg-saffron/10 rounded-full blur-3xl" />
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-india-green/10 rounded-full blur-3xl" />

      <div className="w-full max-w-md relative z-10 space-y-6">
        <div className="h-2 bg-gradient-to-r from-saffron via-white-official to-india-green rounded-full" />

        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center bg-white p-3 rounded-xl shadow-lg">
            <Building2 className="h-8 w-8 text-government-blue" />
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">SchemeBridge</h1>
          <p className="text-white/80 text-sm font-medium">
            National Public Welfare &amp; Scheme Eligibility Platform
          </p>
        </div>

        <div className="bg-white rounded-xl shadow-2xl overflow-hidden border border-gray-200">
          <div className="bg-gray-50 px-8 py-6 text-center border-b border-gray-200">
            <h2 className="text-xl font-bold text-gray-900">Sign In</h2>
            <p className="text-gray-600 text-sm mt-1 leading-relaxed">
              Enter your registered credentials to access your dashboard.
            </p>
          </div>

          <div className="px-8 py-7 space-y-6">
            {error && (
              <div className="flex items-start gap-2.5 bg-red-50 border border-red-200 text-red-700 text-sm p-3.5 rounded-lg">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="font-medium text-xs leading-normal">{error}</span>
              </div>
            )}

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
                    placeholder="citizen@example.in"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setError("");
                    }}
                    disabled={loading}
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white transition"
                    autoComplete="email"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="login-password" className="block text-sm font-medium text-gray-700 mb-1.5">
                  Password
                </label>
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
                    }}
                    disabled={loading}
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white transition"
                    autoComplete="current-password"
                  />
                </div>
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
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                    </svg>
                    <span className="font-semibold text-xs animate-pulse">Signing In…</span>
                  </span>
                ) : (
                  <>Sign In <ArrowRight className="h-4 w-4" /></>
                )}
              </button>
            </form>

            <div className="text-center text-sm text-gray-600 pt-3 border-t border-gray-100">
              New to SchemeBridge?{" "}
              <Link to="/signup" className="text-government-blue hover:text-government-blue-dark font-bold hover:underline ml-1">
                Create an Account
              </Link>
            </div>
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
