import React from "react";
import { AlertTriangle, Clock, User, ChevronRight } from "lucide-react";

const mockPriorityApps = [
  {
    id: "APP-1042",
    citizen: "Rajesh Patel",
    scheme: "PM-KISAN Samman Nidhi",
    priority: "Critical",
    deadline: "Today",
    assignedOfficer: "Amit Singh (Verification)"
  },
  {
    id: "APP-1041",
    citizen: "Priya Sharma",
    scheme: "Pradhan Mantri Awas Yojana",
    priority: "High",
    deadline: "Tomorrow",
    assignedOfficer: "Neha Sharma (Schemes)"
  },
  {
    id: "APP-1040",
    citizen: "Vikram Singh",
    scheme: "Scholarship Scheme",
    priority: "High",
    deadline: "2 days",
    assignedOfficer: "Sanjay Kumar (Admin)"
  },
  {
    id: "APP-1039",
    citizen: "Anita Devi",
    scheme: "Ayushman Bharat",
    priority: "Medium",
    deadline: "3 days",
    assignedOfficer: "Priya Patel (Support)"
  }
];

const getPriorityColor = (priority) => {
  switch (priority) {
    case "Critical":
      return "bg-rose-50 text-rose-700 border-rose-200";
    case "High":
      return "bg-amber-50 text-amber-700 border-amber-200";
    case "Medium":
      return "bg-sky-50 text-sky-700 border-sky-200";
    default:
      return "bg-slate-50 text-slate-700 border-slate-200";
  }
};

const getPriorityIcon = (priority) => {
  switch (priority) {
    case "Critical":
      return <AlertTriangle className="h-3.5 w-3.5" />;
    case "High":
      return <AlertTriangle className="h-3.5 w-3.5" />;
    default:
      return <Clock className="h-3.5 w-3.5" />;
  }
};

export default function PriorityQueue({ onReview }) {
  return (
    <div className="space-y-3.5">
      <div className="flex justify-between items-center">
        <h3 className="text-xs font-black text-slate-400 uppercase tracking-wider select-none">
          Priority Queue
        </h3>
        <span className="text-[10px] font-bold text-indigo-600 bg-indigo-50 px-2 py-1 rounded-lg">
          {mockPriorityApps.length} pending
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {mockPriorityApps.map((app) => (
          <div
            key={app.id}
            className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition duration-200 flex flex-col justify-between h-full"
          >
            <div className="space-y-3">
              {/* Header with ID and Priority */}
              <div className="flex justify-between items-center gap-2">
                <span className="text-[10px] font-black text-slate-400 tracking-wider bg-slate-50 px-2 py-0.5 rounded border border-slate-100">
                  {app.id}
                </span>
                <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold shrink-0 flex items-center gap-1 ${getPriorityColor(app.priority)}`}>
                  {getPriorityIcon(app.priority)}
                  {app.priority}
                </span>
              </div>

              {/* Citizen */}
              <div className="space-y-0.5">
                <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider block">
                  Citizen
                </span>
                <div className="flex items-center gap-1.5">
                  <User className="h-3 w-3 text-slate-400" />
                  <span className="text-xs font-black text-slate-800 truncate">
                    {app.citizen}
                  </span>
                </div>
              </div>

              {/* Scheme */}
              <div className="space-y-0.5">
                <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider block">
                  Scheme
                </span>
                <span className="text-xs font-bold text-slate-700 leading-tight block truncate" title={app.scheme}>
                  {app.scheme}
                </span>
              </div>

              {/* Deadline */}
              <div className="space-y-0.5">
                <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider block">
                  Deadline
                </span>
                <div className="flex items-center gap-1.5">
                  <Clock className="h-3 w-3 text-slate-400" />
                  <span className="text-xs font-bold text-slate-700">
                    {app.deadline}
                  </span>
                </div>
              </div>

              {/* Assigned Officer */}
              <div className="space-y-0.5">
                <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider block">
                  Assigned Officer
                </span>
                <span className="text-xs font-bold text-slate-700 truncate block" title={app.assignedOfficer}>
                  {app.assignedOfficer}
                </span>
              </div>
            </div>

            {/* Quick Review Button */}
            <div className="pt-3 border-t border-slate-100 mt-3">
              <button
                onClick={() => onReview && onReview(app)}
                className="w-full py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 rounded-xl text-xs font-black transition flex items-center justify-center gap-1.5"
              >
                <span>Quick Review</span>
                <ChevronRight className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
