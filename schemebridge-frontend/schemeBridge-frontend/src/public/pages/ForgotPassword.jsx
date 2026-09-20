import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Building2, Mail, Lock, KeyRound, ArrowRight, ArrowLeft, CheckCircle, AlertCircle, Eye, EyeOff, RefreshCw } from "lucide-react";
import { isValidEmail, isValidPassword } from "@utils/security";
import { forgotPassword, resetPassword } from "@services/authService";
import { useToast } from "@components/ui/ToastNotification";
import { IS_FLOW_TEST_MODE } from "@utils/flowTestMode";


export default function ForgotPassword() {
  const { showToast } = useToast();
  const navigate = useNavigate();

  // Step 1: "request_otp", Step 2: "reset_password", Step 3: "success"
  const [step, setStep] = useState("request_otp");
  
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);

  useEffect(() => {
    let timer;
    if (resendCooldown > 0) {
      timer = setTimeout(() => setResendCooldown((c) => c - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [resendCooldown]);

  // Step 1: Request Password Reset OTP
  const handleRequestOtp = async (e) => {
    e.preventDefault();
    setError("");

    const targetEmail = email.trim().toLowerCase();
    // TEMPORARY TEST MODE: Skip client-side email format validation when test mode is enabled
    if (!IS_FLOW_TEST_MODE) {
      if (!targetEmail) {
        setError("Please enter your registered email address.");
        return;
      }

      if (!isValidEmail(targetEmail)) {
        setError("Please enter a valid email address.");
        return;
      }
    }


    setLoading(true);
    try {
      const res = await forgotPassword(targetEmail);
      if (res.error) {
        setError(res.message || "Failed to request password reset code.");
        showToast("Request Failed", "Error", res.message || "Failed to send reset code.");
      } else {
        setStep("reset_password");
        setResendCooldown(60);
        showToast("Code Sent", "Success", "If the account exists, a 6-digit reset code has been generated.");
      }
    } catch (err) {
      setError(err.message || "Unable to connect to the server.");
    } finally {
      setLoading(false);
    }
  };

  // Resend Reset OTP
  const handleResendOtp = async () => {
    if (resendCooldown > 0 || loading) return;
    setError("");
    setLoading(true);
    try {
      const res = await forgotPassword(email.trim().toLowerCase());
      if (res.error) {
        setError(res.message || "Failed to resend reset code.");
      } else {
        setResendCooldown(60);
        showToast("Code Resent", "Success", "A new reset code has been sent to your email.");
      }
    } catch (err) {
      setError(err.message || "Failed to resend reset code.");
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Atomic Password Reset (OTP + New Password)
  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError("");

    const cleanOtp = (otp || "").trim();
    // TEMPORARY TEST MODE: Skip client-side reset password format validation when test mode is enabled
    if (!IS_FLOW_TEST_MODE) {
      if (!cleanOtp || cleanOtp.length !== 6 || !/^\d{6}$/.test(cleanOtp)) {
        setError("Please enter the 6-digit reset code.");
        return;
      }

      if (!newPassword) {
        setError("Please enter a new password.");
        return;
      }

      if (!isValidPassword(newPassword)) {
        setError("Password must be at least 8 characters long with uppercase, lowercase, number, and special character.");
        return;
      }

      if (newPassword !== confirmPassword) {
        setError("Passwords do not match.");
        return;
      }
    }


    setLoading(true);
    try {
      const res = await resetPassword({
        email: email.trim().toLowerCase(),
        otp: cleanOtp,
        newPassword,
      });

      if (res.error) {
        setError(res.message || "Failed to reset password. Please check your reset code.");
        showToast("Reset Failed", "Error", res.message || "Invalid reset code.");
      } else {
        setStep("success");
        showToast("Password Reset", "Success", "Your password has been reset successfully.");
      }
    } catch (err) {
      setError(err.message || "Unable to connect to the server.");
    } finally {
      setLoading(false);
    }
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
            <h2 className="text-xl font-bold text-gray-900 dark:text-slate-100">
              {step === "success" ? "Password Reset Complete" : "Reset Password"}
            </h2>
            <p className="text-gray-600 dark:text-slate-300 text-sm mt-1 leading-relaxed">
              {step === "request_otp" && "Enter your registered email to receive a 6-digit reset code."}
              {step === "reset_password" && `Enter the 6-digit code sent to ${email} and your new password.`}
              {step === "success" && "Your password has been changed. All previous sessions have been signed out."}
            </p>
          </div>

          <div className="px-8 py-7 space-y-6">
            {error && (
              <div className="flex items-start gap-2.5 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900/50 text-red-700 dark:text-red-300 text-sm p-3.5 rounded-lg">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="font-medium text-xs leading-normal">{error}</span>
              </div>
            )}

            {/* STEP 1: Enter Email */}
            {step === "request_otp" && (
              <form onSubmit={handleRequestOtp} className="space-y-4">
                <div>
                  <label htmlFor="forgot-email" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                    Registered Email Address
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <input
                      id="forgot-email"
                      type="email"
                      placeholder="citizen@schemebridge.in"
                      value={email}
                      onChange={(e) => {
                        setEmail(e.target.value);
                        setError("");
                      }}
                      disabled={loading}
                      className="w-full pl-10 pr-4 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                      autoComplete="email"
                      required
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-3 bg-government-blue hover:bg-government-blue-dark disabled:opacity-50 text-white rounded-lg font-bold text-sm shadow-md transition flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <span>Sending Reset Code...</span>
                  ) : (
                    <>
                      <span>Send Reset Code</span>
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>

                <div className="text-center pt-2">
                  <Link
                    to="/login"
                    className="inline-flex items-center text-xs font-semibold text-gray-500 dark:text-slate-400 hover:text-gray-800 dark:hover:text-slate-200 transition gap-1"
                  >
                    <ArrowLeft className="h-3.5 w-3.5" /> Back to Login
                  </Link>
                </div>
              </form>
            )}

            {/* STEP 2: Atomic Reset Form (OTP + New Password) */}
            {step === "reset_password" && (
              <form onSubmit={handleResetPassword} className="space-y-4">
                <div>
                  <label htmlFor="reset-otp" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                    6-Digit Verification Code
                  </label>
                  <div className="relative">
                    <KeyRound className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <input
                      id="reset-otp"
                      type="text"
                      maxLength={6}
                      placeholder="XXXXXX"
                      value={otp}
                      onChange={(e) => {
                        setOtp(e.target.value.replace(/\D/g, ""));
                        setError("");
                      }}
                      disabled={loading}
                      className="w-full pl-10 pr-4 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm tracking-widest font-mono text-center font-bold text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                      required
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="new-password" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                    New Password
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <input
                      id="new-password"
                      type={showPassword ? "text" : "password"}
                      placeholder="••••••••"
                      value={newPassword}
                      onChange={(e) => {
                        setNewPassword(e.target.value);
                        setError("");
                      }}
                      disabled={loading}
                      className="w-full pl-10 pr-10 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 focus:outline-none"
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <label htmlFor="confirm-password" className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-1.5">
                    Confirm New Password
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <input
                      id="confirm-password"
                      type={showConfirmPassword ? "text" : "password"}
                      placeholder="••••••••"
                      value={confirmPassword}
                      onChange={(e) => {
                        setConfirmPassword(e.target.value);
                        setError("");
                      }}
                      disabled={loading}
                      className="w-full pl-10 pr-10 py-3 bg-gray-50 dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-lg text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white dark:focus:bg-slate-800 transition"
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 focus:outline-none"
                    >
                      {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-3 bg-government-blue hover:bg-government-blue-dark disabled:opacity-50 text-white rounded-lg font-bold text-sm shadow-md transition flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <span>Resetting Password...</span>
                  ) : (
                    <>
                      <span>Reset Password & Sign In</span>
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>

                <div className="flex items-center justify-between text-xs pt-2">
                  <button
                    type="button"
                    onClick={() => {
                      setStep("request_otp");
                      setError("");
                    }}
                    className="font-medium text-gray-500 dark:text-slate-400 hover:text-gray-800 dark:hover:text-slate-200 transition"
                  >
                    Change Email
                  </button>

                  <button
                    type="button"
                    onClick={handleResendOtp}
                    disabled={resendCooldown > 0 || loading}
                    className="font-medium text-government-blue dark:text-indigo-400 hover:text-government-blue-dark disabled:text-gray-400 transition flex items-center gap-1"
                  >
                    <RefreshCw className={`h-3 w-3 ${loading ? "animate-spin" : ""}`} />
                    {resendCooldown > 0 ? `Resend code in ${resendCooldown}s` : "Resend code"}
                  </button>
                </div>
              </form>
            )}

            {/* STEP 3: Success Screen */}
            {step === "success" && (
              <div className="text-center space-y-4 py-4">
                <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100 dark:bg-green-950/60 text-green-600 dark:text-green-400 border border-green-200 dark:border-green-800">
                  <CheckCircle className="h-6 w-6" />
                </div>
                <h3 className="text-base font-bold text-gray-900 dark:text-slate-100">Success!</h3>
                <p className="text-xs text-gray-600 dark:text-slate-300 leading-relaxed bg-gray-50 dark:bg-slate-800 p-4 rounded-xl border border-gray-200 dark:border-slate-700">
                  Your password has been updated securely. You can now log in with your new password.
                </p>
                <div className="pt-2">
                  <button
                    type="button"
                    onClick={() => navigate("/login")}
                    className="inline-flex items-center justify-center gap-2 w-full py-3 bg-government-blue hover:bg-government-blue-dark text-white rounded-lg text-sm font-bold shadow-md transition"
                  >
                    <ArrowLeft className="h-4 w-4" /> Proceed to Login
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
