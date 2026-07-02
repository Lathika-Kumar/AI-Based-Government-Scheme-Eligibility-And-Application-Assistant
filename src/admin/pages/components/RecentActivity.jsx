import React from "react";
import { CheckCircle, AlertCircle, BookOpen, MessageSquare, ShieldAlert, User, Clock } from "lucide-react";

const mockActivities = [
  {
    id: "act-1",
    officer: "Amit Singh",
    action: "Application Approved",
    module: "Applications",
    detail: "Application APP-1042 was verified and approved.",
    timestamp: "10 mins ago",
    icon: CheckCircle,
    iconColor: "text-emerald-600 bg-emerald-50 border-emerald-100"
  },
  {
    id: "act-2",
    officer: "Neha Sharma",
    action: "Document Flagged",
    module: "Documents",
    detail: "Aadhaar Card uploaded by Vikram Singh flagged for OCR warning.",
    timestamp: "45 mins ago",
    icon: AlertCircle,
    iconColor: "text-amber-600 bg-amber-50 border-amber-100"
  },
  {
    id: "act-3",
    officer: "Sanjay Kumar",
    action: "New Scheme Drafted",
    module: "Schemes",
    detail: "Created new scheme draft for Pradhan Mantri Awas Yojana.",
    timestamp: "2 hours ago",
    icon: BookOpen,
    iconColor: "text-indigo-600 bg-indigo-50 border-indigo-100"
  },
  {
    id: "act-4",
    officer: "Priya Patel",
    action: "Grievance Resolved",
    module: "Grievances",
    detail: "Ticket GRV-5219 marked as resolved.",
    timestamp: "4 hours ago",
    icon: MessageSquare,
    iconColor: "text-emerald-600 bg-emerald-50 border-emerald-100"
  },
  {
    id: "act-5",
    officer: "System",
    action: "MFA Check Triggered",
    module: "Security",
    detail: "System-wide security policy enforced: Aadhaar OTP-based authentication.",
    timestamp: "Yesterday",
    icon: ShieldAlert,
    iconColor: "text-rose-600 bg-rose-50 border-rose-100"
  },
  {
    id: "act-6",
    officer: "Amit Singh",
    action: "User Role Updated",
    module: "Users",
    detail: "Updated role for citizen ID CIT-4521 to Verified.",
    timestamp: "Yesterday",
    icon: User,
    iconColor: "text-sky-600 bg-sky-50 border-sky-100"
  }
];

export default function RecentActivity({ onViewAll }) {
  return (
    <div className="space-y-3.5">
      <div className="flex justify-between items-center">
        <h3 className="text-xs font-black text-slate-400 uppercase tracking-wider select-none">
          Recent Activity
        </h3>
        <button
          onClick={onViewAll}
          className="text-[10px] font-black text-indigo-600 hover:text-indigo-800 transition"
        >
          View All &rarr;
        </button>
      </div>

      <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm flex flex-col h-[500px] justify-between">
        <div className="overflow-y-auto pr-1">
          <div className="flow-root">
            <ul className="-mb-8">
              {mockActivities.map((act, actIdx) => {
                const IconComponent = act.icon;
                return (
                  <li key={act.id}>
                    <div className="relative pb-8">
                      {actIdx !== mockActivities.length - 1 ? (
                        <span className="absolute top-4 left-4 -ml-px h-full w-0.5 bg-slate-100" aria-hidden="true" />
                      ) : null}
                      <div className="relative flex space-x-3">
                        <div className="shrink-0">
                          <span className={`h-8 w-8 rounded-xl border flex items-center justify-center ring-4 ring-white ${act.iconColor}`}>
                            <IconComponent className="h-4.5 w-4.5" />
                          </span>
                        </div>
                        <div className="flex-1 min-w-0 pt-0.5">
                          <div className="flex items-center justify-between gap-1.5">
                            <p className="text-[11px] font-black text-slate-800">
                              {act.action}
                            </p>
                            <span className="text-[8px] text-slate-400 font-bold whitespace-nowrap flex items-center gap-1">
                              <Clock className="h-3 w-3" />
                              {act.timestamp}
                            </span>
                          </div>
                          <p className="text-[10px] text-slate-500 font-semibold mt-1 leading-normal">
                            {act.detail}
                          </p>
                          <div className="flex items-center gap-2 mt-1.5">
                            <span className="text-[9px] font-bold text-slate-400">
                              {act.officer}
                            </span>
                            <span className="text-[9px] text-slate-300">•</span>
                            <span className="text-[9px] font-bold text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded">
                              {act.module}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </li>
                );
              })}
            </ul>
          </div>
        </div>

        <div className="border-t border-slate-100 pt-4 mt-4">
          <button
            onClick={onViewAll}
            className="w-full py-2 bg-slate-50 hover:bg-slate-100 text-slate-600 rounded-xl text-xs font-black border border-slate-200 transition"
          >
            View Full Activity Log
          </button>
        </div>
      </div>
    </div>
  );
}
