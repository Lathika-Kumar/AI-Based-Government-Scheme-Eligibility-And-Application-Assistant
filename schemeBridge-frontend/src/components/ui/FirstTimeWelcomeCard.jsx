import React from "react";
import { Sparkles, CheckCircle2, ArrowRight } from "lucide-react";
import { useNavigate } from "react-router-dom";

/**
 * FirstTimeWelcomeCard Component
 * Displays celebratory welcome card on first-time arrival to Citizen Dashboard.
 */
export default function FirstTimeWelcomeCard({ userName, onClose }) {
  const navigate = useNavigate();

  const handleExplore = () => {
    if (onClose) onClose();
    navigate("/recommendations");
  };

  const capabilities = [
    "Discover government schemes matching your profile",
    "Apply online with automated eligibility checks",
    "Track application reviews and verification status",
    "Manage secure digital documents in your vault",
    "Receive personalized AI recommendations & scheme alerts",
  ];

  return (
    <div className="bg-gradient-to-br from-indigo-900 via-indigo-800 to-slate-900 text-white rounded-2xl p-6 md:p-8 shadow-2xl border border-indigo-700/50 mb-8 relative overflow-hidden">
      {/* Decorative background glow */}
      <div className="absolute top-0 right-0 -mt-10 -mr-10 w-64 h-64 bg-indigo-500/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-0 -mb-10 -ml-10 w-64 h-64 bg-emerald-500/20 rounded-full blur-3xl pointer-events-none" />

      <div className="relative z-10 max-w-3xl">
        <div className="inline-flex items-center space-x-2 px-3 py-1 bg-indigo-500/30 border border-indigo-400/30 rounded-full text-xs font-semibold text-indigo-200 mb-4 backdrop-blur-md">
          <Sparkles className="w-3.5 h-3.5 text-amber-300 animate-pulse" />
          <span>Account Setup Completed</span>
        </div>

        <h2 className="text-2xl md:text-3xl font-extrabold tracking-tight text-white mb-2">
          🎉 Welcome to SchemeBridge{userName ? `, ${userName}` : ""}!
        </h2>

        <p className="text-sm md:text-base text-indigo-100/90 mb-6 leading-relaxed">
          Your account setup and eligibility verification are complete. Your personalized experience is now active.
        </p>

        <div className="bg-white/10 backdrop-blur-md rounded-xl p-4 md:p-5 border border-white/10 mb-6">
          <h4 className="text-xs font-bold uppercase tracking-wider text-indigo-200 mb-3">
            What you can do now:
          </h4>

          <ul className="space-y-2.5">
            {capabilities.map((cap, idx) => (
              <li key={idx} className="flex items-start space-x-2.5 text-xs md:text-sm text-slate-100">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                <span>{cap}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3">
          <button
            onClick={handleExplore}
            className="w-full sm:w-auto px-6 py-3 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold rounded-xl shadow-lg hover:shadow-xl transition-all duration-200 flex items-center justify-center space-x-2"
          >
            <span>Explore My Schemes</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
