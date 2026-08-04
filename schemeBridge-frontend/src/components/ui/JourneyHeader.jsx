import React from "react";
import AccountSetupProgress from "./AccountSetupProgress";
import { ShieldCheck, Info } from "lucide-react";

/**
 * JourneyHeader Component
 * Displays the top welcome banner, contextual step description, and the AccountSetupProgress stepper.
 * 
 * @param {number} currentStep - 1..4
 * @param {string} stepTitle - Title of current step
 * @param {string} stepDescription - Explanation of why this step is required
 */
export default function JourneyHeader({ currentStep = 1, stepTitle, stepDescription }) {
  return (
    <div className="max-w-3xl mx-auto mb-6 text-center">
      {/* Top Banner */}
      <div className="mb-4">
        <h1 className="text-2xl md:text-3xl font-extrabold text-slate-900 tracking-tight">
          Welcome to SchemeBridge
        </h1>
        <p className="text-sm md:text-base text-slate-600 mt-1">
          Helping you discover and apply for government schemes designed for you.
        </p>
      </div>

      {/* Stepper Component */}
      <AccountSetupProgress currentStep={currentStep} />

      {/* Step Purpose Description Card */}
      {stepDescription && (
        <div className="bg-indigo-50/70 border border-indigo-100 rounded-xl p-4 text-left flex items-start space-x-3 shadow-xs">
          <ShieldCheck className="w-5 h-5 text-indigo-600 flex-shrink-0 mt-0.5" />
          <div>
            {stepTitle && <h4 className="text-sm font-semibold text-indigo-950 mb-0.5">{stepTitle}</h4>}
            <p className="text-xs md:text-sm text-indigo-800 leading-relaxed">
              {stepDescription}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
