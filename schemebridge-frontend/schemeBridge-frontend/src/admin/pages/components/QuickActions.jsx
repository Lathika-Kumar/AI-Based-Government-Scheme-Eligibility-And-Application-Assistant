import React from "react";
import { FileText, FileCheck, BookOpen, Users, BarChart3, MessageSquare, FileBarChart, Settings } from "lucide-react";

export default function QuickActions({ onNavigate }) {
  const quickActions = [
    {
      title: "Applications",
      desc: "Manage & review citizen applications",
      tab: "applications",
      icon: FileText
    },
    {
      title: "Documents",
      desc: "Verify citizen vault documents",
      tab: "documents",
      icon: FileCheck
    },
    {
      title: "Schemes",
      desc: "Manage government schemes",
      tab: "schemes",
      icon: BookOpen
    },
    {
      title: "Users Registry",
      desc: "User access & accounts control",
      tab: "users",
      icon: Users
    },
    {
      title: "Analytics & Reports",
      desc: "Platform stats & reports",
      tab: "analytics-reports",
      icon: BarChart3
    },
    {
      title: "Grievances",
      desc: "Citizen grievances desk",
      tab: "grievances",
      icon: MessageSquare
    },
    {
      title: "Reports & Audit",
      desc: "Audit logs & reports center",
      tab: "audit",
      icon: FileBarChart
    },
    {
      title: "Settings",
      desc: "Platform configuration",
      tab: "settings",
      icon: Settings
    }
  ];
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
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-2xl shadow-sm hover:shadow-md hover:border-indigo-300 dark:hover:border-indigo-500 hover:shadow-indigo-50/40 text-left transition duration-200 flex flex-col justify-between group h-32"
            >
              <div className="p-2 bg-indigo-50 dark:bg-indigo-950/50 border border-indigo-100 dark:border-indigo-900/50 rounded-xl text-indigo-600 dark:text-indigo-400 group-hover:bg-indigo-600 group-hover:text-white transition duration-200">
                <Icon className="h-4.5 w-4.5" />
              </div>
              <div className="space-y-0.5">
                <h4 className="font-black text-slate-800 dark:text-slate-100 text-[11px] tracking-tight truncate leading-tight">
                  {action.title}
                </h4>
                <p className="text-[9px] text-slate-400 dark:text-slate-500 font-medium leading-tight">
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
