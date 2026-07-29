import React, { useState } from "react";
import { Bell, Check, X, AlertTriangle, Info, CheckCircle, Clock } from "lucide-react";

const mockNotifications = [
  {
    id: "notif-1",
    type: "alert",
    title: "Application Deadline Approaching",
    message: "PM-KISAN Gujarat registry cycle closing in 8 days. 45 applications pending.",
    timestamp: "10 mins ago",
    read: false
  },
  {
    id: "notif-2",
    type: "success",
    title: "Batch Approval Completed",
    message: "12 scholarship applications approved by Officer Amit Singh.",
    timestamp: "45 mins ago",
    read: false
  },
  {
    id: "notif-3",
    type: "warning",
    title: "Document Verification Flagged",
    message: "Aadhaar Card uploaded by Vikram Singh flagged for OCR warning.",
    timestamp: "2 hours ago",
    read: true
  },
  {
    id: "notif-4",
    type: "info",
    title: "New Scheme Published",
    message: "Pradhan Mantri Awas Yojana - Urban 2.0 now available for applications.",
    timestamp: "4 hours ago",
    read: true
  },
  {
    id: "notif-5",
    type: "alert",
    title: "High Grievance Volume",
    message: "15 new grievance tickets from Maharashtra require attention.",
    timestamp: "Yesterday",
    read: true
  },
  {
    id: "notif-6",
    type: "success",
    title: "System Maintenance Complete",
    message: "Database optimization completed. Performance improved by 23%.",
    timestamp: "Yesterday",
    read: true
  }
];

export default function NotificationDrawer({ isOpen, onClose }) {
  const [notifications, setNotifications] = useState(mockNotifications);

  const markAsRead = (id) => {
    setNotifications(notifications.map(n => 
      n.id === id ? { ...n, read: true } : n
    ));
  };

  const markAllAsRead = () => {
    setNotifications(notifications.map(n => ({ ...n, read: true })));
  };

  const unreadCount = notifications.filter(n => !n.read).length;

  const getIcon = (type) => {
    switch (type) {
      case "alert":
        return <AlertTriangle className="h-4 w-4" />;
      case "success":
        return <CheckCircle className="h-4 w-4" />;
      case "warning":
        return <AlertTriangle className="h-4 w-4" />;
      case "info":
        return <Info className="h-4 w-4" />;
      default:
        return <Bell className="h-4 w-4" />;
    }
  };

  const getIconColor = (type) => {
    switch (type) {
      case "alert":
        return "text-rose-600 bg-rose-50 border-rose-200";
      case "success":
        return "text-emerald-600 bg-emerald-50 border-emerald-200";
      case "warning":
        return "text-amber-600 bg-amber-50 border-amber-200";
      case "info":
        return "text-indigo-600 bg-indigo-50 border-indigo-200";
      default:
        return "text-slate-600 bg-slate-50 border-slate-200";
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <div
        className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm"
        onClick={onClose}
      />
      <div className="relative w-full max-w-md bg-white border-l border-slate-200 shadow-2xl h-full flex flex-col animate-in slide-in-from-right duration-300">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-indigo-50 border border-indigo-100 rounded-xl text-indigo-600">
              <Bell className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-sm font-black text-slate-900 tracking-tight">Notifications</h2>
              <p className="text-[10px] text-slate-500 font-semibold">
                {unreadCount} unread
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {unreadCount > 0 && (
              <button
                onClick={markAllAsRead}
                className="text-[10px] font-bold text-indigo-600 hover:text-indigo-800 px-2 py-1 rounded-lg hover:bg-indigo-50 transition"
              >
                Mark all read
              </button>
            )}
            <button
              onClick={onClose}
              className="p-2 hover:bg-slate-100 rounded-xl text-slate-400 hover:text-slate-600 transition"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Notifications List */}
        <div className="flex-1 overflow-y-auto">
          {notifications.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-center px-6">
              <div className="p-4 bg-slate-50 rounded-2xl mb-4">
                <Bell className="h-8 w-8 text-slate-400" />
              </div>
              <p className="text-sm font-bold text-slate-600">No notifications</p>
              <p className="text-xs text-slate-400 mt-1">You're all caught up!</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {notifications.map((notification) => (
                <div
                  key={notification.id}
                  className={`px-6 py-4 hover:bg-slate-50 transition cursor-pointer ${
                    !notification.read ? "bg-indigo-50/30" : ""
                  }`}
                  onClick={() => markAsRead(notification.id)}
                >
                  <div className="flex gap-3">
                    <div className={`p-2 rounded-xl border shrink-0 ${getIconColor(notification.type)}`}>
                      {getIcon(notification.type)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2">
                        <h4 className="text-xs font-black text-slate-800 leading-tight">
                          {notification.title}
                        </h4>
                        {!notification.read && (
                          <div className="w-2 h-2 bg-indigo-600 rounded-full shrink-0 mt-1" />
                        )}
                      </div>
                      <p className="text-[11px] text-slate-600 font-semibold mt-1 leading-normal">
                        {notification.message}
                      </p>
                      <div className="flex items-center gap-1 mt-2">
                        <Clock className="h-3 w-3 text-slate-400" />
                        <span className="text-[10px] text-slate-400 font-semibold">
                          {notification.timestamp}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-200 bg-slate-50">
          <button className="w-full py-2.5 bg-white border border-slate-200 hover:border-indigo-300 hover:bg-indigo-50 text-slate-700 hover:text-indigo-700 rounded-xl text-xs font-black transition flex items-center justify-center gap-2">
            <span>View All Notifications</span>
          </button>
        </div>
      </div>
    </div>
  );
}
