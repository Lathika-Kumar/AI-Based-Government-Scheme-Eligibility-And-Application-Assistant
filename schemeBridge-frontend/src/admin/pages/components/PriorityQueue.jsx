// src/admin/pages/components/PriorityQueue.jsx
import React from "react";
import { AlertTriangle, Clock, User, ChevronRight } from "lucide-react";

// The component receives a `data` prop containing priority applications.
// It no longer uses hard‑coded mock data; all data comes from the Dashboard service.

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
    case "High":
      return <AlertTriangle className="h-3.5 w-3.5" />;
    default:
      return <Clock className="h-3.5 w-3.5" />;
  }
};

export default function PriorityQueue({ data = [], onReview, onSetPriority }) {
  if (!data.length) {
    return (
      <div className="p-4 text-sm text-gray-500">No high‑priority applications at this time.</div>
    );
  }

  return (
    <div className="space-y-3.5">
      <div className="flex justify-between items-center">
        <h3 className="text-xs font-black text-slate-400 uppercase tracking-wider select-none">
          Priority Queue
        </h3>
        <span className="text-[10px] font-bold text-indigo-600 bg-indigo-50 px-2 py-1 rounded-lg">
          {data.length} pending
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {data.map((app) => (
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
                <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider block">Citizen</span>
                <div className="flex items-center gap-1.5">
                  <User className="h-3 w-3 text-slate-400" />
                  <span className="text-xs font-black text-slate-800 truncate">{app.citizen}</span>
                </div>
              </div>

              {/* Scheme */}
              <div className="space-y-0.5">
                <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider block">Scheme</span>
                <span className="text-xs font-bold text-slate-700 leading-tight block truncate" title={app.scheme}>
                  {app.scheme}
                </span>
              </div>

              {/* Deadline */}
              <div className="space-y-0.5">
                <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider block">Deadline</span>
                <div className="flex items-center gap-1.5">
                  <Clock className="h-3 w-3 text-slate-400" />
                  <span className="text-xs font-bold text-slate-700">{app.deadline}</span>
                </div>
              </div>

              {/* Assigned Officer */}
              <div className="space-y-0.5">
                <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider block">Assigned Officer</span>
                <span className="text-xs font-bold text-slate-700 truncate block" title={app.assignedOfficer}>
                  {app.assignedOfficer}
                </span>
              </div>

              {/* Set Priority Override */}
              <div className="flex items-center gap-1.5 pt-2 border-t border-slate-50">
                <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider">Set Priority:</span>
                <select
                  value={app.priority}
                  onChange={(e) => onSetPriority && onSetPriority(app.id, e.target.value)}
                  className="bg-slate-50 border border-slate-200 text-slate-700 text-[9px] font-bold rounded px-1 py-0.5 focus:outline-none focus:ring-1 focus:ring-indigo-500 transition cursor-pointer"
                >
                  <option value="Critical">Critical</option>
                  <option value="High">High</option>
                  <option value="Medium">Medium</option>
                  <option value="Low">Low</option>
                </select>
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
