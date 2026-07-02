import React, { useState, useEffect } from "react";
import { Brain, CheckCircle2, Circle } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function AIAnalysisLoading({ onFinished }) {
  const { t } = useApp();
  const [currentStep, setCurrentStep] = useState(1);
  const [progress, setProgress] = useState(0);

  const SIMULATOR_STEPS = [
    { id: 1, text: t("ob_ai_sim_step1") || "Reading profile demographic parameters..." },
    { id: 2, text: t("ob_ai_sim_step2") || "Auditing annual household income bounds..." },
    { id: 3, text: t("ob_ai_sim_step3") || "Verifying document signatures with UIDAI registry..." },
    { id: 4, text: t("ob_ai_sim_step4") || "Running eligibility rules against 350+ central & state schemes..." },
    { id: 5, text: t("ob_ai_sim_step5") || "Generating personalized Citizen Dashboard..." },
  ];

  useEffect(() => {
    // Increment progress bar smoothly
    const progressInterval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) {
          clearInterval(progressInterval);
          return 100;
        }
        return prev + 1;
      });
    }, 35); // Takes ~3.5 seconds to reach 100%

    return () => clearInterval(progressInterval);
  }, []);

  useEffect(() => {
    // Advance simulation steps
    const stepDuration = 700; // 700ms per step
    const timer = setInterval(() => {
      setCurrentStep((prev) => {
        if (prev >= SIMULATOR_STEPS.length) {
          clearInterval(timer);
          // Wait slightly before calling onFinished to show complete state
          setTimeout(() => {
            onFinished();
          }, 400);
          return SIMULATOR_STEPS.length;
        }
        return prev + 1;
      });
    }, stepDuration);

    return () => clearInterval(timer);
  }, [onFinished]);

  return (
    <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200 p-8 max-w-md w-full mx-auto text-center space-y-6">
      {/* AI animation container */}
      <div className="relative flex justify-center py-4">
        {/* Animated outer pulsing rings */}
        <div className="absolute w-24 h-24 bg-indigo-500/10 rounded-full animate-ping"></div>
        <div className="absolute w-20 h-20 bg-indigo-650/15 bg-indigo-600/10 rounded-full blur-md"></div>

        {/* Central brain icon */}
        <div className="relative bg-indigo-600 text-white p-5 rounded-2xl shadow-xl z-10 animate-pulse">
          <Brain className="h-10 w-10 text-white" />
        </div>
      </div>

      <div className="space-y-2">
        <h2 className="text-lg font-extrabold text-slate-900 tracking-tight">
          {t("ob_ai_loading_title") || "AI Matching Engine Running"}
        </h2>
        <p className="text-slate-500 text-xs max-w-xs mx-auto leading-relaxed">
          {t("ob_ai_loading_desc") || "Evaluating state policy regulations and verifying your credentials in real-time."}
        </p>
      </div>

      {/* Progress Bar */}
      <div className="space-y-1.5 text-left">
        <div className="flex justify-between text-[10px] font-extrabold text-slate-400 uppercase tracking-wider">
          <span>{t("ob_ai_sim_progress") || "Simulation Matching Progress"}</span>
          <span className="text-indigo-600 font-extrabold">{progress}%</span>
        </div>
        <div className="h-2 bg-slate-100 rounded-full overflow-hidden border border-slate-200">
          <div
            className="h-full bg-gradient-to-r from-indigo-500 to-indigo-600 transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      {/* Simulation Steps logs list */}
      <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 text-left space-y-3">
        {SIMULATOR_STEPS.map((s) => {
          const isDone = currentStep > s.id;
          const isCurrent = currentStep === s.id;

          return (
            <div
              key={s.id}
              className={`flex items-start gap-3 transition-opacity duration-300 ${
                isCurrent ? "opacity-100" : isDone ? "opacity-75" : "opacity-30"
              }`}
            >
              {isDone ? (
                <CheckCircle2 className="h-4.5 w-4.5 text-emerald-600 shrink-0 mt-0.5" />
              ) : isCurrent ? (
                <div className="h-4.5 w-4.5 rounded-full border-2 border-indigo-600 border-t-transparent animate-spin shrink-0 mt-0.5"></div>
              ) : (
                <Circle className="h-4.5 w-4.5 text-slate-350 shrink-0 mt-0.5" />
              )}
              <span className={`text-[11px] font-semibold leading-relaxed ${
                isCurrent ? "text-indigo-900 font-bold" : isDone ? "text-slate-700" : "text-slate-400"
              }`}>
                {s.text}
              </span>
            </div>
          );
        })}
      </div>

      {/* Advisory Footer */}
      <p className="text-[10px] text-slate-400 tracking-wide uppercase font-medium">
        {t("ob_ai_footer") || "⚡ Evaluation Gateway · sandboxed local thread"}
      </p>
    </div>
  );
}
