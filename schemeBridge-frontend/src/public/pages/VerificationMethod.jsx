import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useToast } from "@components/ui/ToastNotification";
import JourneyHeader from "@components/ui/JourneyHeader";
import JourneySidebar from "@components/ui/JourneySidebar";
import { Mail, Loader2, ArrowRight, Shield, Check, X } from "lucide-react";

export default function VerificationMethod() {
  const navigate = useNavigate();
  const { user, sendEmailOtp, verifyOtp } = useAuth();
  const { showToast } = useToast();

  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [otp, setOtp] = useState("");
  const [countdown, setCountdown] = useState(0);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const userName = user?.fullName || user?.name || "Citizen";

  // Countdown timer effect for resend OTP
  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  const handleSendOtp = async (e) => {
    e.preventDefault();
    console.log("Button clicked");
    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const res = await sendEmailOtp(user?.email);
      console.log("API response:", res);
      if (res.error) {
        showToast(res.error, "error");
        setErrorMessage(res.error);
      } else {
        console.log("Setting otpSent=true");
        setOtpSent(true);
        console.log("Current state before setOtpSent:", otpSent);
        setOtp("");
        setCountdown(60);
      }
    } catch (err) {
      const errMsg = err.message || "Failed to send OTP. Please try again.";
      console.log("Send OTP error:", err);
      showToast(errMsg, "error");
      setErrorMessage(errMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleOtpChange = (e) => {
    const value = e.target.value.replace(/\D/g, "").slice(0, 6);
    setOtp(value);
    setErrorMessage("");
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    if (otp.length !== 6) {
      setErrorMessage("OTP must be exactly 6 digits");
      return;
    }

    setVerifying(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const response = await verifyOtp(user?.email, otp, "EMAIL");
      if (response?.error) {
        const errMsg = response.error;
        setErrorMessage(errMsg);
        showToast(errMsg, "error");
        if (/expired/i.test(errMsg)) {
          setCountdown(0);
        }
      } else {
        setSuccessMessage("Email verified successfully!");
        showToast("Email verified successfully!", "success");
        setTimeout(() => {
          navigate("/onboarding");
        }, 1500);
      }
    } catch (err) {
      const errMsg = err?.message || "OTP verification failed. Please try again.";
      setErrorMessage(errMsg);
      showToast(errMsg, "error");
      if (/expired/i.test(errMsg)) {
        setCountdown(0);
      }
    } finally {
      setVerifying(false);
    }
  };

  const handleResendOtp = async (e) => {
    e.preventDefault();
    if (countdown > 0) return;
    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const res = await sendEmailOtp(user?.email);
      if (res.error) {
        setErrorMessage(res.error);
        showToast(res.error, "error");
      } else {
        setOtp("");
        setCountdown(60);
        setSuccessMessage("OTP resent successfully. Please check your email.");
        showToast("OTP resent successfully", "success");
      }
    } catch (err) {
      const errMsg = err?.message || "Failed to resend OTP. Please try again.";
      setErrorMessage(errMsg);
      showToast(errMsg, "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        <JourneyHeader
          currentStep={2}
          stepTitle="Email Verification"
          stepDescription="We verify your email to keep your information secure and ensure only genuine users access government schemes."
        />

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Main Card */}
          <div className="lg:col-span-7 bg-white rounded-2xl p-6 sm:p-8 shadow-sm border border-slate-200">
            <div className="mb-6">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900">
                Welcome, {userName}!
              </h2>
              <p className="text-sm text-slate-600 mt-1">
                {otpSent 
                  ? "Enter the 6-digit OTP code sent to your email to verify your account."
                  : "Click below to send a one-time verification code to your registered email."}
              </p>
            </div>

            {/* Error Message */}
            {errorMessage && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-start space-x-3">
                <X className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <p className="text-sm text-red-800">{errorMessage}</p>
              </div>
            )}

            {/* Success Message */}
            {successMessage && (
              <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg flex items-start space-x-3">
                <Check className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                <p className="text-sm text-green-800">{successMessage}</p>
              </div>
            )}

            {/* Email Card - Send OTP Section */}
            {!otpSent && (
              <form onSubmit={handleSendOtp} className="space-y-4">
                <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm">
                  <div className="flex items-center space-x-3">
                    <Mail className="w-5 h-5 text-indigo-600" />
                    <div>
                      <p className="text-sm font-semibold text-slate-900">Verify using Email OTP</p>
                      <p className="text-xs text-slate-500 mt-1">
                        Receive a 6-digit OTP code at <strong className="text-slate-800">{user?.email || "lathikakumar798@gmail.com"}</strong>
                      </p>
                    </div>
                  </div>
                </div>

                <div className="pt-4">
                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center space-x-2 text-sm sm:text-base disabled:opacity-60 cursor-pointer"
                  >
                    {loading ? (
                      <>
                        <Loader2 className="w-5 h-5 animate-spin" />
                        <span>Sending OTP...</span>
                      </>
                    ) : (
                      <>
                        <span>Send OTP Code</span>
                        <ArrowRight className="w-5 h-5" />
                      </>
                    )}
                  </button>
                </div>
              </form>
            )}

            {/* OTP Input & Verification Section */}
            {otpSent && (
              <form onSubmit={handleVerifyOtp} className="space-y-4">
                {/* OTP Input Field */}
                <div>
                  <label htmlFor="otp-input" className="block text-sm font-semibold text-slate-900 mb-2">
                    Enter 6-Digit OTP
                  </label>
                  <input
                    id="otp-input"
                    type="text"
                    inputMode="numeric"
                    maxLength="6"
                    value={otp}
                    onChange={handleOtpChange}
                    placeholder="000000"
                    className="w-full px-4 py-3 text-center text-2xl font-mono tracking-widest border-2 border-slate-300 rounded-xl focus:border-indigo-600 focus:outline-none transition-colors"
                  />
                  <p className="text-xs text-slate-500 mt-2">
                    {otp.length}/6 digits entered
                  </p>
                </div>

                {/* Verify Button */}
                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={verifying || otp.length !== 6}
                    className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center space-x-2 text-sm sm:text-base disabled:cursor-not-allowed"
                  >
                    {verifying ? (
                      <>
                        <Loader2 className="w-5 h-5 animate-spin" />
                        <span>Verifying OTP...</span>
                      </>
                    ) : (
                      <>
                        <Check className="w-5 h-5" />
                        <span>Verify OTP</span>
                      </>
                    )}
                  </button>
                </div>

                {/* Resend OTP Section */}
                <div className="pt-2 text-center">
                  {countdown > 0 ? (
                    <p className="text-sm text-slate-600">
                      Resend OTP in <strong>{countdown}</strong> seconds
                    </p>
                  ) : (
                    <button
                      type="button"
                      onClick={handleResendOtp}
                      disabled={loading}
                      className="text-sm text-indigo-600 hover:text-indigo-700 font-semibold underline disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
                    >
                      {loading ? "Resending..." : "Resend OTP"}
                    </button>
                  )}
                </div>
              </form>
            )}
          </div>

          {/* Desktop Motivational Sidebar */}
          <div className="lg:col-span-5">
            <JourneySidebar />
          </div>
        </div>
      </div>
    </div>
  );
}
