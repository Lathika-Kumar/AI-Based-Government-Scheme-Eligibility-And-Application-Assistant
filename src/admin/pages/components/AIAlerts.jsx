import React from "react";
import { AlertTriangle, Clock, FileText, Copy, TrendingUp, MessageSquare } from "lucide-react";

const mockAlerts = [
  {
    id: "alert-1",
    type: "deadline",
    title: "Applications Nearing Processing Deadline",
    message: "18 applications will exceed SLA within 24 hours. Priority review recommended.",
    severity: "high",
    count: 18
  },
  {
    id: "alert-2",
    type: "duplicate",
    title: "Duplicate Documents Detected",
    message: "3 potential duplicate Aadhaar uploads detected across different applications.",
    severity: "medium",
    count: 3
  },
  {
    id: "alert-3",
    type: "scheme",
    title: "Scheme Deadline Tomorrow",
    message: "PM-KISAN Gujarat registry cycle closes tomorrow. 45 pending applications.",
    severity: "high",
    count: 45
  },
  {
    id: "alert-4",
    type: "grievance",
    title: "High Grievance Volume",
    message: "Unusual spike in grievance tickets from Maharashtra region (15 new today).",
    severity: "medium",
    count: 15
  },
  {
    id: "alert-5",
    type: "performance",
    title: "Processing Time Anomaly",
    message: "Average processing time increased by 15% in last 24 hours.",
    severity: "low",
    count: 1
  }
];

const getSeverityColor = (severity) => {
  switch (severity) {
    case "high":
      return "bg-rose-50 text-rose-700 border-rose-200";
    case "medium":
      return "bg-amber-50 text-amber-700 border-amber-200";
    case "low":
      return "bg-sky-50 text-sky-700 border-sky-200";
    default:
      return "bg-slate-50 text-slate-700 border-slate-200";
  }
};

const getSeverityIcon = (type) => {
  switch (type) {
    case "deadline":
      return <Clock className="h-4 w-4" />;
    case "duplicate":
      return <Copy className="h-4 w-4" />;
    case "scheme":
      return <FileText className="h-4 w-4" />;
    case "grievance":
      return <MessageSquare className="h-4 w-4" />;
    case "performance":
      return <TrendingUp className="h-4 w-4" />;
    default:
      return <AlertTriangle className="h-4 w-4" />;
  }
};

export default function AIAlerts() {
  return (
    <div className="space-y-3.5">
      <div className="flex justify-between items-center">
        <h3 className="text-xs font-black text-slate-400 uppercase tracking-wider select-none">
          AI Alerts
        </h3>
        <span className="text-[10px] font-bold text-rose-600 bg-rose-50 px-2 py-1 rounded-lg">
          {mockAlerts.filter(a => a.severity === "high").length} critical
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {mockAlerts.map((alert) => (
          <div
            key={alert.id}
            className={`bg-white border rounded-2xl p-5 shadow-sm hover:shadow-md transition duration-200 ${
              alert.severity === "high" ? "border-rose-200" : "border-slate-200"
            }`}
          >
            <div className="flex items-start justify-between gap-3 mb-3">
              <div className={`p-2 rounded-xl border ${getSeverityColor(alert.severity)}`}>
                {getSeverityIcon(alert.type)}
              </div>
              <span className={`px-2 py-0.5 rounded-full border text-[9px] font-black uppercase tracking-wider ${getSeverityColor(alert.severity)}`}>
                {alert.severity}
              </span>
            </div>

            <h4 className="text-xs font-black text-slate-800 leading-tight mb-2">
              {alert.title}
            </h4>

            <p className="text-[10px] text-slate-600 font-semibold leading-normal mb-3">
              {alert.message}
            </p>

            <div className="flex items-center justify-between pt-3 border-t border-slate-100">
              <span className="text-[9px] font-bold text-slate-400">
                {alert.count} {alert.count === 1 ? "item" : "items"}
              </span>
              <button className="text-[10px] font-black text-indigo-600 hover:text-indigo-800 transition">
                Review &rarr;
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
