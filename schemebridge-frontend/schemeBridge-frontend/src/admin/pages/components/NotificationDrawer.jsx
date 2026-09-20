import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, Check, X, AlertTriangle, Info, CheckCircle, Clock, RefreshCw, ArrowRight } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function NotificationDrawer({ isOpen, onClose }) {
  const navigate = useNavigate();
  const {
    notifications,
    unreadCount,
    markNotificationRead,
    markAllNotificationsRead,
    refreshNotifications
  } = useApp();

  useEffect(() => {
    if (isOpen && typeof refreshNotifications === "function") {
      refreshNotifications();
    }
  }, [isOpen, refreshNotifications]);

  const markAsRead = (id) => {
    if (typeof markNotificationRead === "function") {
      markNotificationRead(id);
    }
  };

  const markAllAsRead = () => {
    if (typeof markAllNotificationsRead === "function") {
      markAllNotificationsRead();
    }
  };

  const getIcon = (type) => {
    switch (type) {
      case "SYSTEM_ALERT":
      case "alert":
        return <AlertTriangle className="h-4 w-4" />;
      case "APPLICATION_APPROVED":
      case "DOCUMENT_VERIFIED":
      case "success":
        return <CheckCircle className="h-4 w-4" />;
      case "DOCUMENT_REJECTED":
      case "APPLICATION_REJECTED":
      case "warning":
        return <AlertTriangle className="h-4 w-4" />;
      default:
        return <Info className="h-4 w-4" />;
    }
  };

  const getIconColor = (type) => {
    switch (type) {
      case "SYSTEM_ALERT":
      case "alert":
        return "text-rose-600 bg-rose-50 border-rose-200";
      case "APPLICATION_APPROVED":
      case "DOCUMENT_VERIFIED":
      case "success":
        return "text-emerald-600 bg-emerald-50 border-emerald-200";
      case "DOCUMENT_REJECTED":
      case "APPLICATION_REJECTED":
      case "warning":
        return "text-amber-600 bg-amber-50 border-amber-200";
      default:
        return "text-indigo-600 bg-indigo-50 border-indigo-200";
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 shadow-2xl border-l border-slate-200 dark:border-slate-800 flex flex-col">
          
          {/* Header */}
          <div className="p-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Bell className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
              <h3 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider">
                NOTIFICATIONS ({unreadCount} NEW)
              </h3>
            </div>
            <div className="flex items-center gap-2">
              {unreadCount > 0 && (
                <button
                  onClick={markAllAsRead}
                  className="text-[10px] font-bold text-indigo-600 hover:text-indigo-800 dark:text-indigo-400 dark:hover:text-indigo-300 flex items-center gap-1 transition"
                >
                  <Check className="h-3 w-3" />
                  Mark All Read
                </button>
              )}
              <button
                onClick={onClose}
                className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition"
                aria-label="Close notifications"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* List */}
          <div className="flex-1 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-800">
            {notifications.length === 0 ? (
              <div className="p-16 text-center space-y-2">
                <Bell className="h-8 w-8 text-slate-300 dark:text-slate-600 mx-auto" />
                <p className="text-xs font-bold text-slate-700 dark:text-slate-300">No Notifications</p>
                <p className="text-[10px] text-slate-400 dark:text-slate-500">All alerts and notices have been cleared.</p>
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => (!n.read && !n.isRead) && markAsRead(n.id)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      if (!n.read && !n.isRead) markAsRead(n.id);
                    }
                  }}
                  className={`p-4 transition cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/60 ${
                    (!n.read && !n.isRead) ? "bg-indigo-50/20 dark:bg-indigo-950/30" : ""
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <div className={`p-2 rounded-xl border shrink-0 ${getIconColor(n.type)}`}>
                      {getIcon(n.type)}
                    </div>
                    <div className="flex-1 min-w-0 space-y-1">
                      <div className="flex items-center justify-between gap-2">
                        <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100 truncate">
                          {n.title}
                        </h4>
                        {(!n.read && !n.isRead) && (
                          <span className="h-2 w-2 rounded-full bg-indigo-600 dark:bg-indigo-400 shrink-0" />
                        )}
                      </div>
                      <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                        {n.message || n.body}
                      </p>
                      <div className="flex items-center justify-between pt-1">
                        <div className="flex items-center gap-1 text-[10px] font-semibold text-slate-400 dark:text-slate-500">
                          <Clock className="h-3 w-3" />
                          <span>{n.createdAt || n.timestamp ? new Date(n.createdAt || n.timestamp).toLocaleString("en-IN") : "Recent"}</span>
                        </div>
                        {n.actionRoute && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              if (!n.read && !n.isRead) markAsRead(n.id);
                              if (typeof onClose === "function") onClose();
                              navigate(n.actionRoute);
                            }}
                            className="text-[10px] font-bold text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300 flex items-center gap-0.5"
                          >
                            <span>View Details</span>
                            <ArrowRight className="h-3 w-3" />
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          <div className="p-3 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-950 flex justify-between items-center text-[10px] font-semibold text-slate-500 dark:text-slate-400">
            <span>Server-Sent Event Stream Connected</span>
            <button 
              onClick={refreshNotifications}
              className="text-indigo-600 hover:text-indigo-800 dark:text-indigo-400 dark:hover:text-indigo-300 font-bold flex items-center gap-1"
            >
              <RefreshCw className="h-3 w-3" />
              Refresh
            </button>
          </div>

        </div>
      </div>
    </div>
  );
}
