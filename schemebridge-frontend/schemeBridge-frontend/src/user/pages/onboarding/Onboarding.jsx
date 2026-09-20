import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { Building2, CheckCircle2 } from "lucide-react";
import profileService from "@services/profileService";
import { safeRemoveItem } from "@utils/storage";

import AIWelcome        from "./AIWelcome";
import Step1Personal    from "./Step1Personal";
import Step2Eligibility from "./Step2Eligibility";
import Step3Documents   from "./Step3Documents";
import AIAnalysisLoading from "./AIAnalysisLoading";

export default function Onboarding() {
  const { user, updateUser } = useAuth();
  const navigate = useNavigate();

  // If user already completed onboarding, redirect to dashboard
  React.useEffect(() => {
    if (user?.onboardingComplete) {
      navigate("/dashboard", { replace: true });
    }
  }, [user?.onboardingComplete, navigate]);

  // Resume from the exact step where user left off, or start at 0 (AI Welcome Screen)
  const [step, setStep] = useState(() => {
    if (user?.onboardingStep && user.onboardingStep > 1) {
      return user.onboardingStep;
    }
    if (user?.onboardingStep === 1) {
      return 1;
    }
    return 0;
  });

  const STEPS = [
    { id: 1, label: "Personal", desc: "Demographic Info" },
    { id: 2, label: "Eligibility", desc: "Socio-economic Details" },
    { id: 3, label: "Documents", desc: "Vault Verification" },
  ];

  // Accumulate onboarding form data across steps to ensure no fields are lost
  const [accumulatedData, setAccumulatedData] = useState(() => ({
    displayName: user?.displayName || user?.name || "",
    age: user?.age !== undefined && user?.age !== null ? user.age : undefined,
    gender: user?.gender || "",
    occupation: user?.occupation || "",
    annualIncome: user?.annualIncome !== undefined && user?.annualIncome !== null ? user.annualIncome : undefined,
    socialCategory: user?.socialCategory || user?.caste || "",
    state: user?.state || "",
    district: user?.district || "",
    education: user?.education || "",
    disabilityStatus: user?.disabilityStatus ?? false,
  }));

  /** Called by each step to advance and persist progress */
  const handleStepNext = async (stepData = {}) => {
    const nextStep = step === 0 ? 1 : step + 1;
    setStep(nextStep);

    const merged = {
      ...accumulatedData,
      displayName: (stepData.name !== undefined && stepData.name !== "") ? stepData.name : (stepData.displayName || accumulatedData.displayName || user?.name || user?.displayName),
      age: (stepData.age !== undefined && stepData.age !== "") ? Number(stepData.age) : (accumulatedData.age !== undefined ? accumulatedData.age : (user?.age !== undefined && user?.age !== "" ? Number(user.age) : undefined)),
      gender: stepData.gender || accumulatedData.gender || user?.gender,
      district: stepData.district || accumulatedData.district || user?.district,
      occupation: stepData.occupation || accumulatedData.occupation || user?.occupation,
      annualIncome: (stepData.annualIncome !== undefined && stepData.annualIncome !== "") ? Number(stepData.annualIncome) : (accumulatedData.annualIncome !== undefined ? accumulatedData.annualIncome : (user?.annualIncome !== undefined ? Number(user.annualIncome) : undefined)),
      socialCategory: stepData.caste || stepData.socialCategory || accumulatedData.socialCategory || user?.caste,
      education: stepData.education || accumulatedData.education || user?.education,
      state: stepData.state || accumulatedData.state || user?.state,
      disabilityStatus: stepData.disabilityStatus !== undefined ? stepData.disabilityStatus : (accumulatedData.disabilityStatus ?? user?.disabilityStatus),
      onboardingStep: nextStep,
    };

    setAccumulatedData(merged);

    try {
      const payload = { ...merged };
      const res = await profileService.updateProfile(payload);
      if (res && !res.error && res.data) {
        updateUser({ ...res.data, name: res.data.displayName || merged.displayName, age: res.data.age ?? merged.age });
      } else {
        updateUser({ ...merged, name: merged.displayName, onboardingStep: nextStep });
      }
    } catch (err) {
      console.warn("Failed to persist onboarding step progress:", err);
      updateUser({ ...merged, name: merged.displayName, onboardingStep: nextStep });
    }
  };

  const handleStepBack = () => {
    const prevStep = Math.max(1, step - 1);
    updateUser({ onboardingStep: prevStep });
    setStep(prevStep);
  };

  /** Final completion after simulated AI analysis loading */
  const handleComplete = async (stepData = {}) => {
    const finalData = {
      ...accumulatedData,
      ...stepData,
      displayName: accumulatedData.displayName || user?.displayName || user?.name,
      age: accumulatedData.age !== undefined ? accumulatedData.age : (user?.age ? Number(user.age) : undefined),
      gender: accumulatedData.gender || user?.gender,
      state: accumulatedData.state || user?.state,
      district: accumulatedData.district || user?.district,
      occupation: accumulatedData.occupation || user?.occupation,
      annualIncome: accumulatedData.annualIncome !== undefined ? accumulatedData.annualIncome : (user?.annualIncome ? Number(user.annualIncome) : undefined),
      socialCategory: accumulatedData.socialCategory || user?.socialCategory || user?.caste,
      education: accumulatedData.education || user?.education,
      disabilityStatus: accumulatedData.disabilityStatus ?? user?.disabilityStatus ?? false,
      onboardingStep: 3,
      onboardingComplete: true,
    };

    try {
      const res = await profileService.updateProfile(finalData);
      if (res && !res.error && res.data) {
        updateUser({ ...res.data, name: res.data.displayName || finalData.displayName, age: res.data.age ?? finalData.age, onboardingComplete: true, onboardingStatus: "COMPLETE", onboardingStep: 3 });
      } else {
        updateUser({ ...finalData, name: finalData.displayName, onboardingComplete: true, onboardingStatus: "COMPLETE", onboardingStep: 3 });
      }
    } catch (err) {
      console.warn("Failed to persist final onboarding profile:", err);
      updateUser({ ...finalData, name: finalData.displayName, onboardingComplete: true, onboardingStatus: "COMPLETE", onboardingStep: 3 });
    }

    // Remove calculator prefill key upon successful completion
    safeRemoveItem("schemebridge_prefill_profile");
    navigate("/dashboard", { replace: true });
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
                  <div key={s.id} className={`relative z-10 flex flex-col items-center flex-1 ${idx === 0 ? "items-start" : idx === STEPS.length - 1 ? "items-end" : ""}`}>
                    {/* Circle */}
                    <div className={`h-8 w-8 rounded-full flex items-center justify-center text-xs font-bold border-2 transition-all duration-300
                      ${done    ? "bg-indigo-650 border-indigo-600 bg-indigo-600 text-white"
                      : current ? "bg-white border-indigo-400 text-indigo-700 shadow-lg shadow-indigo-500/30"
                      :           "bg-slate-800 border-slate-600 text-slate-500"}`}>
                      {done ? <CheckCircle2 className="h-4 w-4 text-white" /> : s.id}
                    </div>
                    {/* Label */}
                    <div className="mt-2 text-center">
                      <p className={`text-xs font-bold transition ${current ? "text-white" : done ? "text-indigo-400" : "text-slate-500"}`}>
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
              initialData={{
                name: accumulatedData.displayName || user?.displayName || user?.name || "",
                age: accumulatedData.age !== undefined && accumulatedData.age !== null ? accumulatedData.age : (user?.age !== undefined && user?.age !== null ? user.age : ""),
                gender: accumulatedData.gender || user?.gender || "",
              }}
              onNext={handleStepNext}
            />
          )}
          {step === 2 && (
            <Step2Eligibility
              initialData={{
                occupation:   accumulatedData.occupation || user?.occupation || "",
                annualIncome: accumulatedData.annualIncome !== undefined && accumulatedData.annualIncome !== null ? accumulatedData.annualIncome : (user?.annualIncome !== undefined ? user.annualIncome : ""),
                caste:        accumulatedData.socialCategory || user?.socialCategory || user?.caste || "",
                state:        accumulatedData.state || user?.state || "",
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
        </div>
      </main>
    </div>
  );
}
