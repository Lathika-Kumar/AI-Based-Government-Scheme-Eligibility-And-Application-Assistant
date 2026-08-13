import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { Building2, CheckCircle2 } from "lucide-react";

import profileService from "@services/profileService";
import { useToast } from "@components/ui/ToastNotification";
import AIWelcome        from "./AIWelcome";
import Step1Personal    from "./Step1Personal";
import Step2Eligibility from "./Step2Eligibility";
import Step3Documents   from "./Step3Documents";
import AIAnalysisLoading from "./AIAnalysisLoading";

export default function Onboarding() {
  const { user, updateUser } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Resume from the step where user left off, or start at 0 (AI Welcome Screen)
  const [step, setStep] = useState(() => {
    if (user?.onboardingStep === 1 || !user?.onboardingStep) {
      return 0;
    }
    return user.onboardingStep;
  });
  const [error, setError] = useState("");

  const getStepAlignmentClass = (idx) => {
    if (idx === 0) return "items-start";
    if (idx === STEPS.length - 1) return "items-end";
    return "";
  };

  const getStepCircleClass = (done, current) => {
    if (done) return "bg-indigo-650 border-indigo-600 bg-indigo-600 text-white";
    if (current) return "bg-white border-indigo-400 text-indigo-700 shadow-lg shadow-indigo-500/30";
    return "bg-slate-800 border-slate-600 text-slate-500";
  };

  const getStepLabelClass = (done, current) => {
    if (current) return "text-white";
    if (done) return "text-indigo-400";
    return "text-slate-500";
  };

  const STEPS = [
    { id: 1, label: "Personal", desc: "Demographic Info" },
    { id: 2, label: "Eligibility", desc: "Socio-economic Details" },
    { id: 3, label: "Documents", desc: "Vault Verification" },
  ];

  /** Called by each step to advance */
  const handleStepNext = (stepData) => {
    const nextStep = step + 1;
    updateUser({ ...stepData, onboardingStep: nextStep });
    setStep(nextStep);
  };

  const handleStepBack = () => {
    const prevStep = step - 1;
    updateUser({ onboardingStep: prevStep });
    setStep(prevStep);
  };

  /** Final completion after simulated AI analysis loading */
  const handleComplete = async (stepData = {}) => {
    if (isSubmitting) return;
    setError("");
    setIsSubmitting(true);

    const profilePayload = {
      fullName: user?.fullName || user?.name || "Test Citizen",
      age: user?.age,
      gender: user?.gender,
      state: user?.state,
      district: user?.district,
      category: user?.caste || user?.category,
      occupation: user?.occupation,
      annualIncome: user?.annualIncome,
      qualification: user?.education || user?.qualification,
    };

    try {
      const updatedProfile = await profileService.updateProfile(profilePayload);
      const onboardingCompleted = updatedProfile?.onboardingCompleted === true || updatedProfile?.onboardingComplete === true;

      updateUser({
        ...stepData,
        name: updatedProfile?.fullName || user?.name || "Test Citizen",
        fullName: updatedProfile?.fullName || user?.fullName || "Test Citizen",
        age: updatedProfile?.age || user?.age,
        gender: updatedProfile?.gender || user?.gender,
        state: updatedProfile?.state || user?.state,
        district: updatedProfile?.district || user?.district,
        occupation: updatedProfile?.occupation || user?.occupation,
        annualIncome: updatedProfile?.annualIncome || user?.annualIncome,
        caste: updatedProfile?.category || user?.caste,
        category: updatedProfile?.category || user?.caste,
        onboardingComplete: true,
        onboardingStep: 3,
      });

      // TEMPORARILY DISABLED FOR TESTING
      // if (!onboardingCompleted) {
      //   showToast("error", "Incomplete Profile", "Please complete all mandatory profile fields before continuing.");
      //   return;
      // }

      localStorage.removeItem("schemebridge_prefill_profile");
      navigate("/dashboard");
    } catch (err) {
      console.error("Onboarding completion failed:", err);
      setError(err?.message || "Unable to complete onboarding. Please try again.");
      showToast("error", "Onboarding Failed", err?.message || "Unable to complete onboarding. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const progressPct = step > 0 && step <= 3 ? ((step - 1) / (STEPS.length - 1)) * 100 : 0;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 flex flex-col font-sans">
      {/* Top Bar */}
      <header className="shrink-0 px-6 py-4 flex items-center justify-between border-b border-white/10">
        <div className="flex items-center gap-2.5">
          <div className="bg-indigo-600 p-1.5 rounded-lg">
            <Building2 className="h-5 w-5 text-white" />
          </div>
          <div>
            <span className="text-white font-bold text-sm">SchemeBridge</span>
            <span className="text-indigo-300 text-[10px] block leading-none">Setup Wizard</span>
          </div>
        </div>
        <div className="text-indigo-300 text-xs">
          Welcome,{" "}
          <span className="text-white font-bold">{user?.name?.split(" ")[0] || "Citizen"}</span>
        </div>
      </header>

      {/* Step Indicator - Hidden during Step 0 (AI Welcome) and Step 4 (AI Analysis Loading) */}
      {step > 0 && step < 4 && (
        <div className="shrink-0 bg-slate-900/60 border-b border-white/10 px-6 py-5">
          <div className="max-w-xl mx-auto">
            <div className="flex items-center relative">
              {/* Progress line */}
              <div className="absolute top-4 left-0 right-0 h-0.5 bg-white/10 z-0">
                <div
                  className="h-full bg-indigo-500 transition-all duration-500"
                  style={{ width: `${progressPct}%` }}
                />
              </div>

              {STEPS.map((s, idx) => {
                const done    = step > s.id;
                const current = step === s.id;
                return (
                  <div key={s.id} className={`relative z-10 flex flex-col items-center flex-1 ${getStepAlignmentClass(idx)}`}>
                    {/* Circle */}
                    <div className={`h-8 w-8 rounded-full flex items-center justify-center text-xs font-bold border-2 transition-all duration-300 ${getStepCircleClass(done, current)}`}>
                      {done ? <CheckCircle2 className="h-4 w-4 text-white" /> : s.id}
                    </div>
                    {/* Label */}
                    <div className="mt-2 text-center">
                      <p className={`text-xs font-bold transition ${getStepLabelClass(done, current)}`}>
                        {s.label}
                      </p>
                      <p className="text-[9px] text-slate-500 hidden sm:block">{s.desc}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Step Content */}
      <main className="flex-1 flex items-center justify-center px-4 py-8 overflow-y-auto">
        <div className="w-full max-w-xl">
          {step === 0 && (
            <AIWelcome
              user={user}
              onStart={() => handleStepNext({ onboardingStep: 1 })}
            />
          )}
          {step === 1 && (
            <Step1Personal
              initialData={{ name: user?.name || "", age: user?.age || "", gender: user?.gender || "" }}
              onNext={handleStepNext}
            />
          )}
          {step === 2 && (
            <Step2Eligibility
              initialData={{
                occupation:   user?.occupation   || "",
                annualIncome: user?.annualIncome  || "",
                caste:        user?.caste         || "",
                state:        user?.state         || "",
                district:     user?.district      || "",
              }}
              onNext={handleStepNext}
              onBack={handleStepBack}
            />
          )}
          {step === 3 && (
            <Step3Documents
              onComplete={() => setStep(4)}
              onBack={handleStepBack}
            />
          )}
          {step === 4 && (
            <AIAnalysisLoading
              onFinished={handleComplete}
            />
          )}
          {error && (
            <div className="mt-4 rounded-xl bg-rose-50 border border-rose-200 p-4 text-rose-700 text-sm">
              {error}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
