import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useToast } from "@components/ui/ToastNotification";
import JourneyHeader from "@components/ui/JourneyHeader";
import JourneySidebar from "@components/ui/JourneySidebar";
import { KeyRound, Timer, RefreshCw, Loader2, CheckCircle2, ArrowRight, ShieldAlert } from "lucide-react";

export default function OtpVerification() {
  const navigate = useNavigate();
  const { user, verifyOtp, sendEmailOtp } = useAuth();
  const { showToast } = useToast();

  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [verifiedSuccess, setVerifiedSuccess] = useState(false);

  // Timers
  const [expirySeconds, setExpirySeconds] = useState(300); // 5 minutes
  const [resendCooldown, setResendCooldown] = useState(30); // 30 seconds
  const [attemptsRemaining, setAttemptsRemaining] = useState(5);

  const method = "EMAIL";
  const recipient = user?.email;
  const userName = user?.fullName || user?.name || "Citizen";

  // 5-Minute Overall Expiry Countdown
  useEffect(() => {
    if (expirySeconds <= 0) return;
    const timer = setInterval(() => {
      setExpirySeconds((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [expirySeconds]);

  // 30-Second Resend Cooldown Countdown
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
    if (!otp || otp.trim().length !== 6) {
      showToast("Please enter a 6-digit OTP code.", "error");
      return;
    }

    if (expirySeconds <= 0) {
      showToast("⚠ OTP Expired. Please request a new OTP.", "error");
      return;
    }

    setLoading(true);
    try {
      const res = await verifyOtp(user?.email, otp.trim(), method);
      if (res.error) {
        showToast(res.error, "error");
        setAttemptsRemaining((prev) => Math.max(0, prev - 1));
      } else {
        setVerifiedSuccess(true);
        showToast("🎉 Account Verified Successfully!", "success");
        // Auto-redirect to /onboarding after 2 seconds
        setTimeout(() => {
          navigate("/onboarding");
        }, 2000);
      }
    } catch (err) {
      showToast(err.message || "Invalid OTP code.", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (resendCooldown > 0) return;

    setResending(true);
    try {
      const res = await sendEmailOtp(user?.email);

      if (res.error) {
        showToast(res.error, "error");
      } else {
        showToast("OTP sent successfully to " + recipient, "success");
        setExpirySeconds(300);
        setResendCooldown(30);
        setOtp("");
        setAttemptsRemaining(5);
      }
    } catch (err) {
      showToast(err.message || "Failed to resend OTP.", "error");
    } finally {
      setResending(false);
    }
  };

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
                  🎉 Account Verified Successfully!
                </h3>
                <p className="text-sm text-slate-600 max-w-md mx-auto mb-4 leading-relaxed">
                  Welcome to SchemeBridge. Let's complete your profile to discover the government schemes you are eligible for. You are just one step away from accessing personalized government benefits.
                </p>
                <div className="pt-4">
                  <button
                    onClick={() => navigate("/onboarding")}
                    className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center space-x-2 text-base cursor-pointer"
                  >
                    <span>Continue to Profile Setup</span>
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
                        placeholder="123456"
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
