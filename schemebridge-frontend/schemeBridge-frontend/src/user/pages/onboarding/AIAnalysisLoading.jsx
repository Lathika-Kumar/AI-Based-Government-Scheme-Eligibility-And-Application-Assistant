import React, { useState, useEffect } from "react";
import { Brain, CheckCircle2, Circle, ArrowRight, Sparkles } from "lucide-react";

export default function AIAnalysisLoading({ onFinished }) {
  const [currentStep, setCurrentStep] = useState(1);
  const [progress, setProgress] = useState(0);
  const [complete, setComplete] = useState(false);

  const SIMULATOR_STEPS = [
    { id: 1, text: "Reading profile demographic parameters..." },
    { id: 2, text: "Auditing annual household income bounds..." },
    { id: 3, text: "Verifying document signatures with UIDAI registry..." },
    { id: 4, text: "Running eligibility rules against 350+ central & state schemes..." },
    { id: 5, text: "Generating personalized Citizen Dashboard..." },
  ];

  useEffect(() => {
    const progressInterval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) {
          clearInterval(progressInterval);
          return 100;
        }
        return prev + 1;
      });
    }, 30);

    return () => clearInterval(progressInterval);
  }, []);

  useEffect(() => {
    const stepDuration = 600;
    const timer = setInterval(() => {
      setCurrentStep((prev) => {
        if (prev >= SIMULATOR_STEPS.length) {
          clearInterval(timer);
          setComplete(true);
          // Auto-redirect after 2 seconds
          setTimeout(() => {
            onFinished();
          }, 2000);
          return SIMULATOR_STEPS.length;
        }
        return prev + 1;
      });
    }, stepDuration);

    return () => clearInterval(timer);
  }, [onFinished]);

  if (complete) {
    return (
      <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200 p-8 max-w-md w-full mx-auto text-center space-y-6 animate-fadeIn">
        <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto shadow-sm animate-bounce">
          <CheckCircle2 className="w-10 h-10" />
        </div>

        <div className="space-y-2">
          <h2 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            🎉 Congratulations!
          </h2>
          <p className="text-sm font-semibold text-emerald-700">
            Your SchemeBridge profile is now complete.
          </p>
        </div>

        <div className="bg-slate-50 border border-slate-200 rounded-2xl p-5 text-left space-y-3">
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">
            You can now:
          </h4>
          <ul className="space-y-2 text-xs text-slate-700 font-medium">
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
              <span>Discover government schemes matching your profile</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
              <span>Track applications in real-time</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
              <span>Manage secure documents in your vault</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
              <span>Receive personalized AI recommendations</span>
            </li>
          </ul>
        </div>

        <button
          onClick={onFinished}
          className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center space-x-2 text-base cursor-pointer"
        >
          <span>Go to Dashboard</span>
          <ArrowRight className="w-5 h-5" />
        </button>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200 p-8 max-w-md w-full mx-auto text-center space-y-6">
      <div className="relative flex justify-center py-4">
        <div className="absolute w-24 h-24 bg-indigo-500/10 rounded-full animate-ping"></div>
        <div className="absolute w-20 h-20 bg-indigo-600/10 rounded-full blur-md"></div>
        <div className="relative bg-indigo-600 text-white p-5 rounded-2xl shadow-xl z-10 animate-pulse">
          <Brain className="h-10 w-10 text-white" />
        </div>
      </div>

      <div className="space-y-2">
        <h2 className="text-lg font-extrabold text-slate-900 tracking-tight">
          AI Matching Engine Running
        </h2>
        <p className="text-slate-500 text-xs max-w-xs mx-auto leading-relaxed">
          Evaluating state policy regulations and verifying your credentials in real-time.
        </p>
      </div>

      <div className="space-y-1.5 text-left">
        <div className="flex justify-between text-[10px] font-extrabold text-slate-400 uppercase tracking-wider">
          <span>Simulation Matching Progress</span>
          <span className="text-indigo-600 font-extrabold">{progress}%</span>
        </div>
        <div className="h-2 bg-slate-100 rounded-full overflow-hidden border border-slate-200">
          <div
            className="h-full bg-gradient-to-r from-indigo-500 to-indigo-600 transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

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
    </div>
  );
}
