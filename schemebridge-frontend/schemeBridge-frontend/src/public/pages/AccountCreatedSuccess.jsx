import React from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import JourneyHeader from "@components/ui/JourneyHeader";
import JourneySidebar from "@components/ui/JourneySidebar";
import { ArrowRight, CheckCircle2, ShieldCheck } from "lucide-react";

export default function AccountCreatedSuccess() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const userName = user?.fullName || user?.name || "";

  const handleContinue = () => {
    navigate("/verification-method");
  };

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        <JourneyHeader
          currentStep={1}
          stepTitle="Account Created"
          stepDescription="Your account has been created successfully. The next step is two-factor verification to secure your profile."
        />

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Main Card */}
          <div className="lg:col-span-7 bg-white rounded-2xl p-6 sm:p-8 shadow-sm border border-slate-200">
            <div className="flex items-center space-x-3 mb-6">
              <div className="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-2xl flex items-center justify-center shadow-xs">
                <CheckCircle2 className="w-7 h-7 animate-bounce" />
              </div>
              <div>
                <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
                  Step 1 Complete
                </span>
                <h2 className="text-xl sm:text-2xl font-bold text-slate-900 mt-1">
                  🎉 Welcome to SchemeBridge{userName ? `, ${userName}` : ""}!
                </h2>
              </div>
            </div>

            <p className="text-sm text-slate-600 mb-6 leading-relaxed">
              Your account has been created successfully. To protect your information and provide personalized government scheme recommendations, we need to verify your account.
            </p>

            <div className="bg-indigo-50/60 border border-indigo-100 rounded-xl p-4 mb-6">
              <div className="flex items-start space-x-3">
                <ShieldCheck className="w-5 h-5 text-indigo-600 flex-shrink-0 mt-0.5" />
                <p className="text-xs sm:text-sm text-indigo-900 font-medium leading-relaxed">
                  Verification takes less than 30 seconds and guarantees that eligible government benefits reach you securely.
                </p>
              </div>
            </div>

            <button
              onClick={handleContinue}
              className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center space-x-2 text-sm sm:text-base cursor-pointer"
            >
              <span>Continue to Verification</span>
              <ArrowRight className="w-5 h-5" />
            </button>
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
