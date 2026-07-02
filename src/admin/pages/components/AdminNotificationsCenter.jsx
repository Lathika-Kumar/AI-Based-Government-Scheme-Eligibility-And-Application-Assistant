import React, { useState } from "react";
import {
  Bell,
  CheckCircle,
  AlertTriangle,
  Users,
  ShieldCheck,
  FileText,
  Trash2,
  Inbox
} from "lucide-react";

export default function AdminNotificationsCenter() {
  const [notifications, setNotifications] = useState([
    { id: "admin-notif-1", category: "application", title: "12 Applications Approaching SLA Bounds", body: "Pending applications for Post Matric SC Scholarship are within 24 hours of their SLA target.", time: "10 mins ago", read: false, priority: "high" },
    { id: "admin-notif-2", category: "security", title: "⚠️ Multiple Login Anomaly Flagged", body: "Credential audit flagged duplicate administrative sessions from IP 10.120.4.155.", time: "1 hour ago", read: false, priority: "critical" },
    { id: "admin-notif-3", category: "document", title: "Nodal Verification Batches Success", body: "DigiLocker OCR daemon successfully batch verified 142 citizen identity certificates.", time: "2 hours ago", read: true, priority: "normal" },
    { id: "admin-notif-4", category: "user", title: "New Citizen Registration Spike", body: "Gujarat region logged an 8% rise in farmer registrations over the past 4 hours.", time: "4 hours ago", read: true, priority: "normal" },
    { id: "admin-notif-5", category: "system", title: "API Gateway Integration Active", body: "UCSC evolution and conservation services connected. Latency: 42ms.", time: "1 day ago", read: true, priority: "normal" }
  ]);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const markAllRead = () => {
    setNotifications(notifications.map((n) => ({ ...n, read: true })));
  };

  const handleDismiss = (id) => {
    setNotifications(notifications.filter((n) => n.id !== id));
  };

  const priorityPill = {
    critical: "bg-rose-50 text-rose-700 border-rose-200 animate-pulse",
    high: "bg-amber-50 text-amber-700 border-amber-200",
    normal: "bg-slate-100 text-slate-600 border-slate-200"
  };

  const categoryIcons = {
    application: FileText,
    security: AlertTriangle,
    document: ShieldCheck,
    user: Users,
    system: CheckCircle
  };

  return (
    <div className="space-y-4">
      {/* Header operations controls */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm flex items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <div className="p-2 bg-indigo-50 rounded-xl text-indigo-650 text-indigo-600">
            <Bell className="h-4.5 w-4.5" />
          </div>
          <div>
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Internal Notifications</h3>
            <p className="text-[10px] text-slate-400 font-semibold mt-0.5">
              {unreadCount} unread administrative logs require action
            </p>
          </div>
        </div>

        {unreadCount > 0 && (
          <button
            onClick={markAllRead}
            className="px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 border border-indigo-100 text-indigo-700 hover:text-indigo-850 rounded-xl text-xs font-bold transition"
          >
            Mark All as Read
          </button>
        )}
      </div>

      {/* Notifications stack */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden divide-y divide-slate-100">
        {notifications.length === 0 ? (
          <div className="p-16 flex flex-col items-center justify-center text-center space-y-3">
            <Inbox className="h-10 w-10 text-slate-400" />
            <div>
              <p className="text-sm font-bold text-slate-800">Clear Operations Log</p>
              <p className="text-xs text-slate-400 leading-normal max-w-sm mt-0.5">
                All administrative notifications cleared. No alert triggers currently active.
              </p>
            </div>
          </div>
        ) : (
          notifications.map((notif) => {
            const Icon = categoryIcons[notif.category] || Bell;
            return (
              <div
                key={notif.id}
                className={`p-4 flex gap-4 items-start hover:bg-slate-50/50 transition ${
                  !notif.read ? "bg-indigo-50/20" : ""
                }`}
              >
                <div className={`p-2.5 rounded-xl border bg-white shadow-sm shrink-0`}>
                  <Icon className="h-4.5 w-4.5 text-slate-550 text-slate-500" />
                </div>
                <div className="space-y-1 flex-1 text-xs">
                  <div className="flex justify-between items-start gap-2 flex-wrap">
                    <div className="flex items-center gap-2">
                      <h4 className="font-bold text-slate-800 text-xs">{notif.title}</h4>
                      <span className={`px-2 py-0.5 rounded-full border text-[8px] font-bold ${priorityPill[notif.priority] || ""}`}>
                        {notif.priority}
                      </span>
                    </div>
                    <span className="text-[10px] text-slate-400 font-semibold">{notif.time}</span>
                  </div>
                  <p className="text-slate-500 font-medium leading-relaxed max-w-2xl">{notif.body}</p>
                </div>
                <button
                  onClick={() => handleDismiss(notif.id)}
                  className="p-1 hover:bg-slate-100 rounded text-slate-400 hover:text-rose-600 transition shrink-0 self-start mt-0.5"
                  title="Dismiss alert"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
