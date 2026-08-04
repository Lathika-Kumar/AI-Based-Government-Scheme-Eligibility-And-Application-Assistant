import React from "react";
import { CheckCircle, Lock, Sparkles, FileText, Activity, RefreshCw } from "lucide-react";

/**
 * JourneySidebar Component
 * Renders a desktop sidebar panel (or mobile footer) with security & guidance tips.
 */
export default function JourneySidebar() {
  const tips = [
    {
      icon: Lock,
      title: "Encrypted & Secure",
      text: "Your information is encrypted and securely stored following government compliance standards.",
    },
    {
      icon: Sparkles,
      title: "Accurate Recommendations",
      text: "Completing your profile allows our AI engine to accurately match eligible benefits.",
    },
    {
      icon: FileText,
      title: "Flexible Document Upload",
      text: "You can upload official documents now or add them later in your Document Vault.",
    },
    {
      icon: Activity,
      title: "Real-Time Tracking",
      text: "Track application updates, verification milestones, and officer reviews anytime.",
    },
    {
      icon: RefreshCw,
      title: "Easy Profile Updates",
      text: "Update demographic, income, or employment changes whenever your situation updates.",
    },
  ];

  return (
    <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-xl border border-slate-800 flex flex-col justify-between">
      <div>
        <div className="flex items-center space-x-2 mb-4">
          <Sparkles className="w-5 h-5 text-amber-400" />
          <h3 className="text-base font-bold text-slate-100">Why SchemeBridge?</h3>
        </div>

        <p className="text-xs text-slate-400 mb-6 leading-relaxed">
          Guiding citizens across India to access financial support, scholarships, agricultural subsidies, and healthcare benefits easily.
        </p>

        <div className="space-y-4">
          {tips.map((item, idx) => {
            const IconComp = item.icon;
            return (
              <div key={idx} className="flex items-start space-x-3 text-left">
                <div className="p-1.5 bg-slate-800 text-emerald-400 rounded-lg flex-shrink-0 mt-0.5 border border-slate-700">
                  <IconComp className="w-4 h-4" />
                </div>
                <div>
                  <h5 className="text-xs font-semibold text-slate-200 flex items-center gap-1">
                    <CheckCircle className="w-3 h-3 text-emerald-400 inline" /> {item.title}
                  </h5>
                  <p className="text-xs text-slate-400 mt-0.5 leading-snug">{item.text}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="mt-8 pt-4 border-t border-slate-800 text-center">
        <p className="text-xs text-slate-500">
          Official Government Scheme Eligibility Platform • Version 1.0
        </p>
      </div>
    </div>
  );
}
