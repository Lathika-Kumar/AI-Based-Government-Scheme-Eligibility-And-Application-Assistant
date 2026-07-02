import React from "react";
import { FileText, FileCheck, BookOpen, Users, BarChart3, MessageSquare, FileBarChart, Settings } from "lucide-react";

const quickActions = [
  {
    title: "Review Applications",
    desc: "Triage and evaluate citizen benefit submissions.",
    tab: "applications",
    icon: FileText
  },
  {
    title: "Verify Documents",
    desc: "Audit OCR extractions and credential proofs.",
    tab: "documents",
    icon: FileCheck
  },
  {
    title: "Manage Schemes",
    desc: "Configure eligibility models and guidelines.",
    tab: "schemes",
    icon: BookOpen
  },
  {
    title: "User Management",
    desc: "Configure role access of citizens and officials.",
    tab: "users",
    icon: Users
  },
  {
    title: "Analytics",
    desc: "Inspect BI charts and operational metrics.",
    tab: "analytics",
    icon: BarChart3
  },
  {
    title: "Grievances",
    desc: "Address citizen delay complaints and tickets.",
    tab: "grievances",
    icon: MessageSquare
  },
  {
    title: "Reports",
    desc: "Generate and export operational reports.",
    tab: "audit",
    icon: FileBarChart
  },
  {
    title: "Settings",
    desc: "Configure console preferences and policies.",
    tab: "settings",
    icon: Settings
  }
];

export default function QuickActions({ onNavigate }) {
  return (
    <div className="space-y-3">
      <h3 className="text-xs font-black text-slate-400 uppercase tracking-wider select-none">
        Quick Console Operations
      </h3>
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-4">
        {quickActions.map((action, idx) => {
          const Icon = action.icon;
          return (
            <button
              key={idx}
              onClick={() => onNavigate && onNavigate(action.tab)}
              className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm hover:shadow-md hover:border-indigo-300 hover:shadow-indigo-50/40 text-left transition duration-200 flex flex-col justify-between group h-32"
            >
              <div className="p-2 bg-indigo-50 border border-indigo-100 rounded-xl text-indigo-600 group-hover:bg-indigo-600 group-hover:text-white transition duration-200">
                <Icon className="h-4.5 w-4.5" />
              </div>
              <div className="space-y-0.5">
                <h4 className="font-black text-slate-800 text-[11px] tracking-tight truncate leading-tight">
                  {action.title}
                </h4>
                <p className="text-[9px] text-slate-400 font-medium leading-tight">
                  {action.desc}
                </p>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
