import React, { useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { isValidEmail, isValidPhoneNumber, normalizePhoneNumber, checkPasswordStrength } from "@utils/security";
import { formatIsoToDisplay, parseDisplayToIso } from "@utils/dateUtils";
import { IS_FLOW_TEST_MODE } from "@utils/flowTestMode";
import {
  Building2, User, Mail, Phone, Lock, ArrowRight, AlertCircle, CheckCircle2, Sparkles, Eye, EyeOff, Calendar
} from "lucide-react";

export default function Signup() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [name, setName]                 = useState("");
  const [email, setEmail]               = useState("");
  const [phoneNumber, setPhoneNumber]   = useState("");
  const [dob, setDob]                   = useState("");
  const [password, setPassword]         = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [consent, setConsent]           = useState(false);
  const [error, setError]               = useState("");
  const [loading, setLoading]           = useState(false);
  const [hasPrefill, setHasPrefill]     = useState(false);

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

    // TEMPORARY TEST MODE: When active, frontend validation guards are bypassed to test raw backend responses.
    // Set VITE_FRONTEND_FLOW_TEST_MODE=false to restore strict client validation.
    if (!IS_FLOW_TEST_MODE) {
      const trimmedName = name.trim();
      if (!trimmedName) {
        setError("Please enter your full name.");
        return;
      }
      if (!email.trim()) {
        setError("Please enter your email address.");
        return;
      }
      if (!isValidEmail(email.trim().toLowerCase())) {
        setError("Please enter a valid email address.");
        return;
      }
      if (!phoneNumber.trim()) {
        setError("Please enter your phone number.");
        return;
      }
      if (!isValidPhoneNumber(phoneNumber.trim())) {
        setError("Please enter a valid 10-digit mobile number.");
        return;
      }
      if (!dob || !dob.trim()) {
        setError("Please enter your date of birth.");
        return;
      }
      const dobValidation = parseDisplayToIso(dob);
      if (dobValidation.error) {
        setError(dobValidation.error);
        return;
      }
      if (!password) {
        setError("Please enter a password.");
        return;
      }
      if (password !== confirmPassword) {
        setError("Passwords do not match. Please check and try again.");
        return;
      }
      const strengthResult = checkPasswordStrength(password);
      if (strengthResult.score < 2) {
        setError(strengthResult.feedback[0] || "Password must be at least 8 characters long, contain uppercase, lowercase, number and special character.");
        return;
      }
      if (!consent) {
        setError("Please agree to the privacy policy and consent parameters.");
        return;
      }
    }

    const trimmedName = (name || "").trim();

    const spaceIdx = trimmedName.indexOf(" ");
    const firstName = spaceIdx === -1 ? trimmedName : trimmedName.slice(0, spaceIdx);
    const lastName  = spaceIdx === -1 ? "" : trimmedName.slice(spaceIdx + 1).trim();
    const cleanPhone = normalizePhoneNumber((phoneNumber || "").trim());
    const dobParsed = parseDisplayToIso(dob);
    const isoDob = dobParsed.iso || (dob ? dob.trim() : null);

    setLoading(true);
    const result = await signup({
      firstName,
      lastName: lastName || firstName,
      email: email.trim(),
      phoneNumber: cleanPhone,
      password,
      dob: isoDob
    });
    setLoading(false);

    if (result.error) {
      setError(result.error);
      return;
    }

    // Store email so OTP page can pre-fill it without JWT
    sessionStorage.setItem("sb_pending_email", email.trim().toLowerCase());

    // Backend sends OTP to email after signup — go straight to OTP verification
    navigate("/otp-verification");
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
            <h2 className="text-xl font-bold text-gray-900 dark:text-slate-100">Create Citizen Account</h2>
            <p className="text-gray-600 dark:text-slate-300 text-sm mt-1 leading-relaxed">
              Register for unified access to central and state government schemes
            </p>
          </div>

          <div className="px-8 py-7 space-y-5">
            {hasPrefill && (
              <div className="flex items-center gap-2 bg-government-blue/5 dark:bg-indigo-950/40 border border-government-blue/20 dark:border-indigo-800/60 text-government-blue dark:text-indigo-300 p-3 rounded-lg text-xs font-semibold leading-relaxed">
                <Sparkles className="h-4 w-4 text-government-blue dark:text-indigo-400 shrink-0" />
                <span>Demographics pre-loaded from calculator preview.</span>
              </div>
            )}

            {error && (
              <div className="flex items-start gap-2.5 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900/50 text-red-700 dark:text-red-300 text-sm p-3.5 rounded-lg">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="font-medium text-xs leading-normal">{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label htmlFor="signup-name" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
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
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                    autoComplete="name"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="signup-email" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                  Email Address <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="signup-email"
                    type="email"
                    placeholder="citizen@schemebridge.in"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setError("");
                    }}
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                    autoComplete="email"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="signup-phone" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                  Phone Number <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="signup-phone"
                    type="tel"
                    placeholder="9876543210"
                    value={phoneNumber}
                    onChange={(e) => {
                      setPhoneNumber(e.target.value);
                      setError("");
                    }}
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                    autoComplete="tel"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="signup-dob" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                  Date of Birth <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="signup-dob"
                    type="text"
                    placeholder="DD/MM/YYYY"
                    value={dob}
                    onChange={(e) => {
                      setDob(e.target.value);
                      setError("");
                    }}
                    className="w-full pl-10 pr-11 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                    autoComplete="bday"
                  />
                  <input
                    type="date"
                    tabIndex={-1}
                    max={new Date().toISOString().split("T")[0]}
                    min="1900-01-01"
                    onChange={(e) => {
                      if (e.target.value) {
                        setDob(formatIsoToDisplay(e.target.value));
                        setError("");
                      }
                    }}
                    className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 opacity-0 cursor-pointer"
                    aria-label="Pick date of birth from calendar"
                  />
                  <Calendar className="absolute right-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                </div>
                <p className="text-[11px] text-gray-500 dark:text-slate-400 mt-1">Format: DD/MM/YYYY (e.g., 22/03/2007)</p>
              </div>

              <div>
                <label htmlFor="signup-password" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                  Password <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="signup-password"
                    type={showPassword ? "text" : "password"}
                    placeholder="Min 8 characters (letters + numbers)"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full pl-10 pr-11 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                    autoComplete="new-password"
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
              </div>

              <div>
                <label htmlFor="signup-confirm-password" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                  Confirm Password <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    id="signup-confirm-password"
                    type={showConfirmPassword ? "text" : "password"}
                    placeholder="Re-enter your password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="w-full pl-10 pr-11 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                    autoComplete="new-password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword((prev) => !prev)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 focus:outline-none focus:text-government-blue rounded cursor-pointer transition"
                    aria-label={showConfirmPassword ? "Hide password" : "Show password"}
                    aria-pressed={showConfirmPassword}
                    tabIndex={0}
                  >
                    {showConfirmPassword ? (
                      <EyeOff className="h-4 w-4" aria-hidden="true" />
                    ) : (
                      <Eye className="h-4 w-4" aria-hidden="true" />
                    )}
                  </button>
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
                    ${consent ? "bg-government-blue border-government-blue text-white" : "border-gray-300 dark:border-slate-700 bg-white dark:bg-slate-800 group-hover:border-government-blue/50"}`}>
                    {consent && <CheckCircle2 className="h-3.5 w-3.5 text-white" />}
                  </div>
                </div>
                <span className="text-[12px] text-gray-600 dark:text-slate-300 leading-relaxed select-none">
                  I consent to automated scheme matching & eligibility verification.
                </span>
              </label>

              <button
                id="signup-submit"
                type="submit"
                disabled={loading}
                className="w-full bg-saffron hover:bg-saffron-dark disabled:opacity-70 text-government-blue-dark py-3.5 rounded-lg text-sm font-bold flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition cursor-pointer"
              >
                {loading ? (
                  <span className="flex items-center gap-2">
                    <svg className="animate-spin h-4 w-4 text-government-blue-dark" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
                    </svg>
                    Creating Account...
                  </span>
                ) : (
                  <>Create Account <ArrowRight className="h-4 w-4" /></>
                )}
              </button>
            </form>

            <div className="text-center text-sm text-gray-600 dark:text-slate-400 pt-3 border-t border-gray-100 dark:border-slate-800">
              Already have an account? {" "}
              <Link to="/login" className="text-government-blue dark:text-indigo-400 hover:text-government-blue-dark font-bold hover:underline ml-1">
                Sign In
              </Link>
            </div>
          </div>
        </div>

        <p className="text-center text-[11px] text-white/70 tracking-wide uppercase font-medium">
          Secure Encrypted Platform
        </p>
      </div>
    </div>
  );
}
