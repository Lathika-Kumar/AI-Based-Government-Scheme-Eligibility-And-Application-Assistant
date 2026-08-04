import React from "react";
import { CheckCircle2, Circle } from "lucide-react";

/**
 * AccountSetupProgress Component
 * Renders the 4-step account setup progress indicator:
 * 1. Account Created
 * 2. Verification
 * 3. Profile Setup
 * 4. Dashboard
 * 
 * @param {number} currentStep - 1 (Account Created), 2 (Verification), 3 (Profile Setup), 4 (Dashboard)
 */
export default function AccountSetupProgress({ currentStep = 1 }) {
  const steps = [
    { id: 1, label: "Account Created" },
    { id: 2, label: "Verification" },
    { id: 3, label: "Profile Setup" },
    { id: 4, label: "Dashboard" },
  ];

  return (
    <div className="w-full bg-white border border-slate-200 rounded-xl p-4 shadow-sm mb-6">
      <div className="flex items-center justify-between max-w-2xl mx-auto">
        {steps.map((step, idx) => {
          const isCompleted = step.id < currentStep;
          const isCurrent = step.id === currentStep;

          return (
            <React.Fragment key={step.id}>
              {/* Step indicator */}
              <div className="flex flex-col items-center flex-1">
                <div
                  className={`w-9 h-9 rounded-full flex items-center justify-center font-semibold text-sm transition-all duration-200 ${
                    isCompleted
                      ? "bg-emerald-600 text-white shadow-sm"
                      : isCurrent
                      ? "bg-indigo-600 text-white ring-4 ring-indigo-100 shadow-md"
                      : "bg-slate-100 text-slate-400 border border-slate-200"
                  }`}
                >
                  {isCompleted ? (
                    <CheckCircle2 className="w-5 h-5" />
                  ) : isCurrent ? (
                    <span>{step.id}</span>
                  ) : (
                    <Circle className="w-4 h-4 text-slate-300" />
                  )}
                </div>

                <span
                  className={`text-xs mt-2 font-medium text-center ${
                    isCompleted
                      ? "text-emerald-700"
                      : isCurrent
                      ? "text-indigo-700 font-semibold"
                      : "text-slate-400"
                  }`}
                >
                  {step.label}
                </span>
              </div>

              {/* Connecting Line */}
              {idx < steps.length - 1 && (
                <div
                  className={`h-0.5 flex-1 mx-2 transition-all duration-200 ${
                    step.id < currentStep ? "bg-emerald-500" : "bg-slate-200"
                  }`}
                />
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
}
