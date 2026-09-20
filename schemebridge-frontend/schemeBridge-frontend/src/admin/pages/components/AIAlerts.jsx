import React from "react";
import { useApp } from "@context/AppContext";
import { AlertTriangle, Clock, FileText, Copy, TrendingUp, MessageSquare, X } from "lucide-react";



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
  const { notifications = [], dismissNotification = () => {} } = useApp();
  const alerts = (notifications || []).filter(n => n.type === "alert");


  return (
    <div className="space-y-3.5">
      <div className="flex justify-between items-center">
        <h3 className="text-xs font-black text-slate-400 uppercase tracking-wider select-none">
          AI Alerts
        </h3>
        <span className="text-[10px] font-bold text-rose-600 bg-rose-50 px-2 py-1 rounded-lg">
          {alerts.filter(a => a.severity === "high").length} critical
        </span>
      </div>

      {alerts.length === 0 ? (
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 text-center shadow-sm">
          <p className="text-xs font-semibold text-slate-600 dark:text-slate-300">No active critical alerts</p>
          <p className="text-[10px] text-slate-400 dark:text-slate-500 mt-0.5">All operational queues are within standard SLA thresholds.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className={`bg-white dark:bg-slate-900 border rounded-2xl p-5 shadow-sm hover:shadow-md transition duration-200 ${
                alert.severity === "high" ? "border-rose-200 dark:border-rose-900/60" : "border-slate-200 dark:border-slate-800"
              }`}
            >
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className={`p-2 rounded-xl border ${getSeverityColor(alert.severity)}`}> {getSeverityIcon(alert.type)} </div>
                <span className={`px-2 py-0.5 rounded-full border text-[9px] font-black uppercase tracking-wider ${getSeverityColor(alert.severity)}`}>{alert.severity}</span>
                <button onClick={() => dismissNotification(alert.id)} className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition"><X className="h-4 w-4"/></button>
              </div>

              <h4 className="text-xs font-black text-slate-800 dark:text-slate-100 leading-tight mb-2">
                {alert.title}
              </h4>

              <p className="text-[10px] text-slate-600 dark:text-slate-300 font-semibold leading-normal mb-3">
                {alert.message}
              </p>

              <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800">
                <span className="text-[9px] font-bold text-slate-400 dark:text-slate-400">
                  {alert.count} {alert.count === 1 ? "item" : "items"}
                </span>
                <button className="text-[10px] font-black text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300 transition">
                  Review &rarr;
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
