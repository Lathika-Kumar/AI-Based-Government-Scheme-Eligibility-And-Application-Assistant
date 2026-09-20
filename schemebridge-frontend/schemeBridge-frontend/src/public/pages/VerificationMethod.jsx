import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useToast } from "@components/ui/ToastNotification";
import JourneyHeader from "@components/ui/JourneyHeader";
import JourneySidebar from "@components/ui/JourneySidebar";
import { Mail, Phone, Loader2, ArrowRight, Shield } from "lucide-react";

export default function VerificationMethod() {
  const navigate = useNavigate();
  const { user, sendEmailOtp, sendPhoneOtp } = useAuth();
  const showToast = useToast();

  const [selectedMethod, setSelectedMethod] = useState("EMAIL"); // "EMAIL" or "MOBILE"
  const [phoneNumber, setPhoneNumber] = useState(user?.phoneNumber || "");
  const [loading, setLoading] = useState(false);

  const userName = user?.fullName || user?.name || "Citizen";

  const handleSendOtp = async (e) => {
    e.preventDefault();

    if (selectedMethod === "MOBILE") {
      if (!phoneNumber || phoneNumber.trim().length < 10) {
        showToast("Please enter a valid 10-digit mobile number.", "error");
        return;
      }
    }

    setLoading(true);
    try {
      if (selectedMethod === "EMAIL") {
        const res = await sendEmailOtp(user?.email);
        if (res.error) {
          showToast(res.error, "error");
        } else {
          showToast(res.message || "OTP sent successfully to " + user?.email, "success");
          navigate("/otp-verification");
        }
      } else {
        const res = await sendPhoneOtp(user?.email, phoneNumber.trim());
        if (res.error) {
          showToast(res.error, "error");
        } else {
          showToast(res.message || "OTP sent successfully to " + phoneNumber, "success");
          navigate("/otp-verification");
        }
      }
    } catch (err) {
      showToast(err.message || "Failed to send OTP. Please try again.", "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        <JourneyHeader
          currentStep={2}
          stepTitle="Select Verification Method"
          stepDescription="We verify your account to keep your information secure and ensure only genuine users access government schemes."
        />

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Main Card */}
          <div className="lg:col-span-7 bg-white rounded-2xl p-6 sm:p-8 shadow-sm border border-slate-200">
            <div className="mb-6">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900">
                Welcome, {userName}!
              </h2>
              <p className="text-sm text-slate-600 mt-1">
                Please choose your preferred one-time verification method.
              </p>
            </div>

            <form onSubmit={handleSendOtp} className="space-y-4">
              {/* Method Option 1: Email OTP */}
              <label
                className={`flex items-start space-x-4 p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  selectedMethod === "EMAIL"
                    ? "border-indigo-600 bg-indigo-50/50 shadow-xs"
                    : "border-slate-200 hover:border-slate-300 bg-white"
                }`}
              >
                <input
                  type="radio"
                  name="verificationMethod"
                  value="EMAIL"
                  checked={selectedMethod === "EMAIL"}
                  onChange={() => setSelectedMethod("EMAIL")}
                  className="mt-1 text-indigo-600 focus:ring-indigo-500"
                />
                <div className="flex-1">
                  <div className="flex items-center space-x-2">
                    <Mail className="w-5 h-5 text-indigo-600" />
                    <span className="text-sm font-semibold text-slate-900">Verify using Email OTP</span>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    Receive a 6-digit OTP code at <strong className="text-slate-800">{user?.email || "registered email"}</strong>
                  </p>
                </div>
              </label>

              {/* Method Option 2: Mobile OTP */}
              <label
                className={`flex items-start space-x-4 p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  selectedMethod === "MOBILE"
                    ? "border-indigo-600 bg-indigo-50/50 shadow-xs"
                    : "border-slate-200 hover:border-slate-300 bg-white"
                }`}
              >
                <input
                  type="radio"
                  name="verificationMethod"
                  value="MOBILE"
                  checked={selectedMethod === "MOBILE"}
                  onChange={() => setSelectedMethod("MOBILE")}
                  className="mt-1 text-indigo-600 focus:ring-indigo-500"
                />
                <div className="flex-1">
                  <div className="flex items-center space-x-2">
                    <Phone className="w-5 h-5 text-indigo-600" />
                    <span className="text-sm font-semibold text-slate-900">Verify using Mobile OTP</span>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    Receive SMS OTP on your mobile phone number.
                  </p>

                  {selectedMethod === "MOBILE" && (
                    <div className="mt-3 pt-3 border-t border-indigo-100" onClick={(e) => e.stopPropagation()}>
                      <label className="block text-xs font-medium text-slate-700 mb-1">
                        Mobile Phone Number
                      </label>
                      <div className="relative">
                        <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-xs font-semibold text-slate-500">
                          +91
                        </span>
                        <input
                          type="tel"
                          value={phoneNumber}
                          onChange={(e) => setPhoneNumber(e.target.value)}
                          placeholder="9876543210"
                          maxLength={10}
                          className="w-full pl-12 pr-4 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        />
                      </div>
                    </div>
                  )}
                </div>
              </label>

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
