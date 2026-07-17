import React, { useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { isValidEmail, checkPasswordStrength } from "@utils/security";
import {
  Building2, User, Mail, Lock, ArrowRight, AlertCircle, CheckCircle2, Sparkles
} from "lucide-react";

export default function Signup() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [name, setName]         = useState("");
  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState("");
  const [consent, setConsent]   = useState(false);
  const [error, setError]       = useState("");
  const [loading, setLoading]   = useState(false);
  const [hasPrefill, setHasPrefill] = useState(false);

  useEffect(() => {
    const isPrefillQuery = location.search.includes("prefill=1");
    const prefillData = localStorage.getItem("schemebridge_prefill_profile");
    if (isPrefillQuery && prefillData) {
      setHasPrefill(true);
    }
  }, [location.search]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!name.trim())  {
      setError("Full name is required.");
      return;
    }
    if (!email.trim()) {
      setError("Email address is required.");
      return;
    }
    if (!isValidEmail(email.trim().toLowerCase())) {
      setError("Please enter a valid email address.");
      return;
    }
    if (!password)     {
      setError("Password is required.");
      return;
    }
    const strengthResult = checkPasswordStrength(password);
    if (strengthResult.score < 2) {
      setError(strengthResult.feedback[0] || "Please choose a stronger password (min 8 characters with uppercase, lowercase, and a number).");
      return;
    }
    if (!consent)      {
      setError("You must agree to the data consent policy.");
      return;
    }

    setLoading(true);
    await new Promise(r => setTimeout(r, 450));

    const result = signup(name.trim(), email.trim(), password);
    setLoading(false);

    if (result.error) {
      setError(result.error);
      return;
    }

    navigate("/onboarding");
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
            <h2 className="text-xl font-bold text-gray-900">Register Account</h2>
            <p className="text-gray-600 text-sm mt-1 leading-relaxed">
              Create your citizen profile to access welfare schemes
            </p>
          </div>

          <div className="px-8 py-7 space-y-5">
            {hasPrefill && (
              <div className="flex items-center gap-2 bg-government-blue/5 border border-government-blue/20 text-government-blue p-3 rounded-lg text-xs font-semibold leading-relaxed">
                <Sparkles className="h-4 w-4 text-government-blue shrink-0" />
                <span>We saved your eligibility inputs! We will apply them in the wizard after registration.</span>
              </div>
            )}

            {error && (
              <div className="flex items-start gap-2.5 bg-red-50 border border-red-200 text-red-700 text-sm p-3.5 rounded-lg">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="font-medium text-xs leading-normal">{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label htmlFor="signup-name" className="block text-sm font-medium text-gray-700 mb-1.5">
                  Full Name <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <User className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="signup-name"
                    type="text"
                    placeholder="Enter your full name"
                    value={name}
                    onChange={(e) => {
                      setName(e.target.value);
                      setError("");
                    }}
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white transition"
                    autoComplete="name"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="signup-email" className="block text-sm font-medium text-gray-700 mb-1.5">
                  Email Address <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="signup-email"
                    type="email"
                    placeholder="Enter your email address"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setError("");
                    }}
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white transition"
                    autoComplete="email"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="signup-password" className="block text-sm font-medium text-gray-700 mb-1.5">
                  Create Password <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="signup-password"
                    type="password"
                    placeholder="Minimum 8 characters"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white transition"
                    autoComplete="new-password"
                  />
                </div>
              </div>

              <label className="flex items-start gap-3 cursor-pointer group pt-1">
                <div className="relative mt-0.5 shrink-0">
                  <input
                    id="signup-consent"
                    type="checkbox"
                    checked={consent}
                    onChange={(e) => {
                      setConsent(e.target.checked);
                      setError("");
                    }}
                    className="sr-only"
                  />
                  <div className={`h-5 w-5 rounded border-2 flex items-center justify-center transition
                    ${consent ? "bg-government-blue border-government-blue text-white" : "border-gray-300 bg-white group-hover:border-government-blue/50"}`}>
                    {consent && <CheckCircle2 className="h-3.5 w-3.5 text-white" />}
                  </div>
                </div>
                <span className="text-[12px] text-gray-600 leading-relaxed select-none">
                  I agree to let SchemeBridge securely verify my details and documents under official Digital India e-Governance directives.
                </span>
              </label>

              <button
                id="signup-submit"
                type="submit"
                disabled={loading}
                className="w-full bg-saffron hover:bg-saffron-dark disabled:opacity-70 text-government-blue-dark py-3.5 rounded-lg text-sm font-bold flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition"
              >
                {loading ? (
                  <span className="flex items-center gap-2">
                    <svg className="animate-spin h-4 w-4 text-government-blue-dark" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
                    </svg>
                    Creating account…
                  </span>
                ) : (
                  <>Register Now <ArrowRight className="h-4 w-4" /></>
                )}
              </button>
            </form>

            <p className="text-center text-sm text-gray-600 pt-1">
              Already registered?{" "}
              <Link to="/login" className="text-government-blue hover:text-government-blue-dark font-bold underline ml-1">
                Sign In
              </Link>
            </p>
          </div>
        </div>

        <p className="text-center text-[11px] text-white/70 tracking-wide uppercase font-medium">
          Government of India Welfare Simulator
        </p>
      </div>
    </div>
  );
}
