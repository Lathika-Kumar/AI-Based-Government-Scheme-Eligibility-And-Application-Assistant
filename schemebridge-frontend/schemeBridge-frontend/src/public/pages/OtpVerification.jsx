import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { resendOtp } from "@services/authService";
import { useToast } from "@components/ui/ToastNotification";
import { IS_FLOW_TEST_MODE } from "@utils/flowTestMode";
import JourneyHeader from "@components/ui/JourneyHeader";
import JourneySidebar from "@components/ui/JourneySidebar";
import { KeyRound, Timer, RefreshCw, Loader2, CheckCircle2, ArrowRight, ArrowLeft, ShieldAlert } from "lucide-react";



export default function OtpVerification() {
  const navigate = useNavigate();
  const { verifyOtp } = useAuth();
  const { showToast } = useToast();

  // Email is stored in sessionStorage after signup
  const pendingEmail = sessionStorage.getItem("sb_pending_email") || "";

  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [verifiedSuccess, setVerifiedSuccess] = useState(false);

  // Timers
  const [expirySeconds, setExpirySeconds] = useState(600); // 10 minutes
  const [resendCooldown, setResendCooldown] = useState(60); // 60 seconds
  const [attemptsRemaining, setAttemptsRemaining] = useState(3);

  const method = "EMAIL";
  const recipient = pendingEmail;
  const userName = "Citizen";

  // 10-Minute Overall Expiry Countdown
  useEffect(() => {
    if (expirySeconds <= 0) return;
    const timer = setInterval(() => {
      setExpirySeconds((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [expirySeconds]);

  // 60-Second Resend Cooldown Countdown
  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => {
      setResendCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    // TEMPORARY TEST MODE: Skip client-side 6-digit length guard when test mode is enabled
    if (!IS_FLOW_TEST_MODE) {
      if (!otp || otp.trim().length !== 6) {
        showToast("error", "Validation Error", "Please enter a 6-digit OTP code.");
        return;
      }
    }

    if (!pendingEmail) {
      showToast("error", "Session Expired", "Session expired. Please sign up again.");
      navigate("/signup");
      return;
    }
    if (expirySeconds <= 0) {
      showToast("error", "OTP Expired", "OTP has expired. Please resend a new OTP.");
      return;
    }

    setLoading(true);
    try {
      const res = await verifyOtp({ email: pendingEmail, otp: otp.trim() });
      if (res.error) {
        showToast("error", "Verification Failed", res.message || "Invalid OTP code.");
        setAttemptsRemaining((prev) => Math.max(0, prev - 1));
      } else {
        setVerifiedSuccess(true);
        sessionStorage.removeItem("sb_pending_email");
        showToast("success", "Verified", "Account Verified Successfully!");
        // Redirect to login — user must authenticate after verification
        setTimeout(() => navigate("/login"), 2000);
      }
    } catch (err) {
      showToast("error", "Error", err.message || "Invalid OTP code.");
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (resendCooldown > 0 || resending) return;
    if (!pendingEmail) {
      showToast("error", "Session Expired", "Please sign up again.");
      navigate("/signup");
      return;
    }

    setResending(true);
    try {
      const res = await resendOtp(pendingEmail);
      if (res.error) {
        showToast("error", "Resend Failed", res.message || "Unable to resend OTP.");
      } else {
        setResendCooldown(60);
        setExpirySeconds(600);
        setOtp("");
        setAttemptsRemaining(3);
        showToast("success", "OTP Resent", res.data?.message || "A new 6-digit verification code has been sent to your email.");
      }
    } catch (err) {
      showToast("error", "Resend Failed", err.message || "Unable to connect to the server.");
    } finally {
      setResending(false);
    }
  };

  const handleChangeMethod = () => navigate("/signup");



  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        <JourneyHeader
          currentStep={2}
          stepTitle="Enter Verification OTP"
          stepDescription="We verify your account to keep your information secure and ensure only genuine users access government schemes."
        />

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Main Card */}
          <div className="lg:col-span-7 bg-white rounded-2xl p-6 sm:p-8 shadow-sm border border-slate-200">
            {verifiedSuccess ? (
              /* Success Banner */
              <div className="text-center py-4">
                <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-4 shadow-sm animate-bounce">
                  <CheckCircle2 className="w-10 h-10" />
                </div>
                <h3 className="text-2xl font-extrabold text-slate-900 mb-2">
                  Account Verified Successfully!
                </h3>
                <p className="text-sm text-slate-600 max-w-md mx-auto mb-4 leading-relaxed">
                  Your email has been verified. Please log in to access your SchemeBridge dashboard and start exploring government schemes.
                </p>
                <div className="pt-4">
                  <button
                    onClick={() => navigate("/login")}
                    className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center space-x-2 text-base cursor-pointer"
                  >
                    <span>Proceed to Login</span>
                    <ArrowRight className="w-5 h-5" />
                  </button>
                </div>
              </div>
            ) : (
              /* Form */
              <div>
                <div className="flex items-center justify-between mb-6 pb-4 border-b border-slate-100">
                  <div>
                    <h2 className="text-xl sm:text-2xl font-bold text-slate-900">
                      OTP Verification
                    </h2>
                    <p className="text-xs text-slate-500 mt-0.5">
                      OTP sent to <strong className="text-slate-800">{recipient}</strong>
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={handleChangeMethod}
                    className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold flex items-center gap-1 bg-indigo-50 px-3 py-1.5 rounded-lg border border-indigo-100 cursor-pointer"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" />
                    <span>Change {method === "EMAIL" ? "Email" : "Mobile"}</span>
                  </button>
                </div>

                {/* Expiry & Attempt Counters */}
                <div className="flex items-center justify-between bg-slate-50 p-3 rounded-xl border border-slate-200 mb-6 text-xs">
                  <div className="flex items-center space-x-1.5 text-slate-700 font-medium">
                    <Timer className="w-4 h-4 text-indigo-600" />
                    <span>OTP Expiry: <strong className={expirySeconds < 60 ? "text-rose-600" : "text-slate-900"}>{formatTimer(expirySeconds)}</strong></span>
                  </div>

                  <div className="flex items-center space-x-1 text-slate-600">
                    <ShieldAlert className="w-4 h-4 text-amber-500" />
                    <span>Attempts left: <strong className="text-slate-900">{attemptsRemaining}</strong></span>
                  </div>
                </div>

                <form onSubmit={handleVerify} className="space-y-6">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-2 uppercase tracking-wider">
                      Enter 6-Digit OTP Code
                    </label>
                    <div className="relative">
                      <KeyRound className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        value={otp}
                        onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
                        placeholder="XXXXXX"
                        maxLength={6}
                        className="w-full pl-11 pr-4 py-3.5 border border-slate-300 rounded-xl text-center font-mono text-xl tracking-[0.5em] font-bold focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 shadow-xs"
                      />
                    </div>
                  </div>

                  <div className="space-y-3 pt-2">
                    <button
                      type="submit"
                      disabled={loading || otp.length !== 6 || expirySeconds <= 0}
                      className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center space-x-2 text-sm sm:text-base disabled:opacity-50 cursor-pointer"
                    >
                      {loading ? (
                        <>
                          <Loader2 className="w-5 h-5 animate-spin" />
                          <span>Verifying OTP...</span>
                        </>
                      ) : (
                        <>
                          <span>Verify & Activate Account</span>
                          <ArrowRight className="w-5 h-5" />
                        </>
                      )}
                    </button>

                    <div className="flex items-center justify-between text-xs pt-2">
                      <span className="text-slate-500">
                        Didn't receive the OTP?
                      </span>
                      <button
                        type="button"
                        onClick={handleResend}
                        disabled={resendCooldown > 0 || resending}
                        className="font-semibold text-indigo-600 hover:text-indigo-800 disabled:text-slate-400 disabled:cursor-not-allowed flex items-center gap-1 cursor-pointer"
                      >
                        {resending ? (
                          <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        ) : (
                          <RefreshCw className="w-3.5 h-3.5" />
                        )}
                        <span>
                          {resendCooldown > 0
                            ? `Resend in 00:${String(resendCooldown).padStart(2, "0")}`
                            : "Resend OTP"}
                        </span>
                      </button>
                    </div>
                  </div>
                </form>
              </div>
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
