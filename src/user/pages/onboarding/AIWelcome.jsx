import React from "react";
import { Sparkles, ShieldCheck, FileCheck, ArrowRight, Brain } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function AIWelcome({ user, onStart }) {
  const { t } = useApp();

  return (
    <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200">
      {/* Visual Header */}
      <div className="bg-slate-900 px-8 py-8 text-center relative overflow-hidden">
        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#4f46e5_1px,transparent_1px)] [background-size:16px_16px]"></div>
        <div className="absolute -top-12 -right-12 w-28 h-28 bg-indigo-600/20 rounded-full blur-2xl animate-pulse"></div>
        <div className="relative z-10 space-y-3">
          <div className="inline-flex items-center justify-center bg-indigo-650/40 p-2.5 rounded-2xl border border-indigo-500/30 text-indigo-400">
            <Brain className="h-8 w-8 text-indigo-400" />
          </div>
          <h2 className="text-xl font-extrabold text-white">
            {t("ob_ai_engine_title") || "Meet the SchemeBridge AI Engine"}
          </h2>
          <p className="text-slate-400 text-xs max-w-sm mx-auto leading-relaxed">
            {(t("ob_ai_welcome_desc") || "Welcome, {name}. Let's analyze your demographic profile to verify eligibility for state and central benefits.").replace("{name}", user?.name || "Citizen")}
          </p>
        </div>
      </div>

      {/* Info details */}
      <div className="px-8 py-7 space-y-6">
        <div className="space-y-4">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
            {t("ob_ai_how_it_works") || "How the AI Profiling System Works:"}
          </p>

          <div className="space-y-3">
            {/* Step 1 details */}
            <div className="flex gap-3.5 items-start p-3 hover:bg-slate-50 rounded-2xl transition duration-150 border border-transparent hover:border-slate-100">
              <div className="bg-indigo-50 p-2 rounded-xl text-indigo-600 mt-0.5">
                <Sparkles className="h-4.5 w-4.5" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-slate-800">{t("ob_ai_step1_title") || "1. Demographics Profiling"}</h4>
                <p className="text-[11px] text-slate-500 mt-0.5 leading-relaxed">
                  {t("ob_ai_step1_desc") || "We check baseline age, caste, household income thresholds, and state limits against active welfare rules."}
                </p>
              </div>
            </div>

            {/* Step 2 details */}
            <div className="flex gap-3.5 items-start p-3 hover:bg-slate-50 rounded-2xl transition duration-150 border border-transparent hover:border-slate-100">
              <div className="bg-emerald-50 p-2 rounded-xl text-emerald-600 mt-0.5">
                <ShieldCheck className="h-4.5 w-4.5" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-slate-800">{t("ob_ai_step2_title") || "2. Automated Credentials Audit"}</h4>
                <p className="text-[11px] text-slate-500 mt-0.5 leading-relaxed">
                  {t("ob_ai_step2_desc") || "Connect DigiLocker or drop manual files. Our engine scans metadata signatures to pre-verify document vault statuses."}
                </p>
              </div>
            </div>

            {/* Step 3 details */}
            <div className="flex gap-3.5 items-start p-3 hover:bg-slate-50 rounded-2xl transition duration-150 border border-transparent hover:border-slate-100">
              <div className="bg-amber-50 p-2 rounded-xl text-amber-600 mt-0.5">
                <FileCheck className="h-4.5 w-4.5" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-slate-800">{t("ob_ai_step3_title") || "3. Unified Application Matching"}</h4>
                <p className="text-[11px] text-slate-500 mt-0.5 leading-relaxed">
                  {t("ob_ai_step3_desc") || "Calculates eligibility percentages and flags document blockers so you can apply with one click."}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Advisory / Policy Callout */}
        <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-[10px] leading-relaxed text-slate-500">
          {t("ob_ai_privacy_note") || "🔒 Privacy Policy Commitment: All demographic data is secured locally under localStorage sandbox models and evaluated without transmitting private identifiers to unverified third parties."}
        </div>

        {/* Start button */}
        <button
          id="start-onboarding-wizard"
          onClick={onStart}
          className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-3.5 rounded-xl text-sm font-extrabold flex items-center justify-center gap-2 shadow-lg transition active:scale-[0.98]"
        >
          <span>{t("ob_ai_begin_btn") || "Begin AI-Driven Setup"}</span>
          <ArrowRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
