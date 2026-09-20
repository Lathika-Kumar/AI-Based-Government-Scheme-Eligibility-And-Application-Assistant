import React, { useState, useMemo, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { ListRowSkeleton } from "@components/ui/LoadingSkeleton";
import EmptyState from "@components/ui/EmptyState";
import {
  Bell,
  BellOff,
  CheckCheck,
  ClipboardList,
  Calendar,
  ShieldCheck,
  AlertCircle,
  Sparkles,
  Bot,
  ArrowRight,
  X,
  Trash2,
  CheckCircle2,
} from "lucide-react";

const ICON_MAP = {
  ClipboardList,
  Calendar,
  ShieldCheck,
  AlertCircle,
  Sparkles,
  Bot,
  Bell,
};

const CATEGORIES = [
  { id: "all", label: "All" },
  { id: "application", label: "Applications" },
  { id: "document", label: "Documents" },
  { id: "deadline", label: "Deadlines" },
  { id: "system", label: "System" },
];

const PRIORITY_CONFIG = {
  critical: {
    border: "border-l-red-500",
    dot: "bg-red-500",
    badge: "bg-red-50 text-red-800 border-red-200",
    ring: "ring-red-100",
  },
  high: {
    border: "border-l-saffron-dark",
    dot: "bg-saffron-dark",
    badge: "bg-saffron/10 text-saffron-dark border-saffron/20",
    ring: "ring-saffron/10",
  },
  normal: {
    border: "border-l-government-blue",
    dot: "bg-government-blue",
    badge: "bg-government-blue/10 text-government-blue border-government-blue/20",
    ring: "ring-government-blue/10",
  },
};

const CATEGORY_COLORS = {
  application: "bg-government-blue/10 text-government-blue",
  document: "bg-india-green/10 text-india-green",
  deadline: "bg-saffron/10 text-saffron-dark",
  system: "bg-gray-100 text-gray-700",
};

function timeAgo(timestamp) {
  const now = Date.now();
  const then = new Date(timestamp).getTime();
  const diff = Math.floor((now - then) / 1000);
  if (diff < 60) {
    return "Just now";
  }
  if (diff < 3600) {
    const mins = Math.floor(diff / 60);
    return `${mins}m ago`;
  }
  if (diff < 86400) {
    const hours = Math.floor(diff / 3600);
    return `${hours}h ago`;
  }
  const days = Math.floor(diff / 86400);
  return `${days}d ago`;
}

function NotificationCard({ notif, onRead, onDismiss }) {
  const navigate = useNavigate();
  const IconComp = ICON_MAP[notif.icon] || Bell;
  const priority = PRIORITY_CONFIG[notif.priority] || PRIORITY_CONFIG.normal;
  const catColor = CATEGORY_COLORS[notif.category] || "bg-gray-100 dark:bg-slate-800 text-gray-700 dark:text-slate-300";

  const handleAction = (e) => {
    if (e && e.stopPropagation) e.stopPropagation();
    if (!notif.read) {
      onRead(notif.id);
    }
    if (notif.actionRoute && notif.actionRoute !== "/notifications") {
      navigate(notif.actionRoute);
    }
  };

  const handleCardClick = () => {
    if (!notif.read) {
      onRead(notif.id);
    }
  };

  return (
    <div
      onClick={handleCardClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          handleCardClick();
        }
      }}
      className={`bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 border-l-4 ${priority.border} rounded-2xl p-4 transition hover:shadow-md cursor-pointer ${
        !notif.read ? "shadow-sm" : "opacity-80"
      }`}
    >
      <div className="flex items-start gap-3">
        {/* Icon */}
        <div
          className={`p-2.5 rounded-xl shrink-0 ${catColor} mt-0.5`}
        >
          <IconComp className="h-4 w-4" />
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className={`text-xs font-bold leading-snug ${!notif.read ? "text-gray-900 dark:text-slate-100" : "text-gray-600 dark:text-slate-400"}`}>
                {notif.title}
              </h3>
              {!notif.read && (
                <span className="h-2 w-2 bg-government-blue dark:bg-indigo-400 rounded-full shrink-0" />
              )}
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onDismiss(notif.id);
              }}
              className="text-gray-300 dark:text-slate-600 hover:text-red-400 p-1 rounded-lg transition shrink-0"
              title="Dismiss"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </div>

          <p className="text-[11px] text-gray-500 dark:text-slate-400 leading-relaxed mt-1">
            {notif.body}
          </p>

          <div className="flex items-center justify-between mt-2.5 flex-wrap gap-2">
            <div className="flex items-center gap-2 flex-wrap">
              <span className={`text-[9px] font-bold uppercase tracking-wide px-2 py-0.5 rounded-full border ${priority.badge}`}>
                {notif.priority}
              </span>
              <span className={`text-[9px] font-semibold uppercase px-2 py-0.5 rounded-full border ${catColor} border-gray-200 dark:border-slate-700`}>
                {notif.category}
              </span>
              <span className="text-[10px] text-gray-400 dark:text-slate-500">{timeAgo(notif.timestamp)}</span>
            </div>

            <div className="flex items-center gap-2">
              {!notif.read && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onRead(notif.id);
                  }}
                  className="text-[11px] font-semibold text-government-blue dark:text-indigo-400 hover:text-blue-800 dark:hover:text-indigo-300 transition"
                  title="Mark as read"
                >
                  Mark as read
                </button>
              )}
              {notif.actionRoute && notif.actionRoute !== "/notifications" && (
                <button
                  onClick={handleAction}
                  className="text-[11px] font-semibold text-slate-700 dark:text-slate-200 hover:text-slate-900 dark:hover:text-white bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 px-2.5 py-1 rounded-lg transition inline-flex items-center gap-1"
                >
                  <span>View Details</span>
                  <ArrowRight className="h-3 w-3" />
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function Notifications() {
  const {
    notifications,
    unreadCount,
    markNotificationRead,
    markAllNotificationsRead,
    dismissNotification,
  } = useApp();

  const [activeCategory, setActiveCategory] = useState("all");
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5;

  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 500);
    return () => clearTimeout(timer);
  }, []);

  const filtered = useMemo(() => {
    return notifications.filter((n) =>
      activeCategory === "all" ? true : n.category === activeCategory
    );
  }, [notifications, activeCategory]);

  // Reset page when filtered changes
  useEffect(() => {
    setCurrentPage(1);
  }, [filtered]);

  // Paginated list
  const paginated = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return filtered.slice(start, start + itemsPerPage);
  }, [filtered, currentPage, itemsPerPage]);

  const totalPages = Math.ceil(filtered.length / itemsPerPage);

  const criticalCount = notifications.filter((n) => n.priority === "critical" && !n.read).length;

  return (
    <div className="space-y-5">
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-16 bg-gray-100 animate-pulse rounded-xl" />
          ))}
        </div>
      ) : (<>
      {/* Header */}
      <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="bg-government-blue p-2.5 rounded-xl text-white">
              <Bell className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-gray-900 dark:text-slate-100 uppercase tracking-wide">
                Notifications ({unreadCount} NEW)
              </h1>
              <p className="text-gray-500 dark:text-slate-400 text-xs mt-0.5">
                {unreadCount > 0
                  ? `${unreadCount} unread notification${unreadCount > 1 ? "s" : ""}`
                  : "All caught up"}
              </p>
            </div>
          </div>
          {unreadCount > 0 && (
            <button
              onClick={markAllNotificationsRead}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-government-blue dark:text-indigo-400 hover:text-government-blue-dark dark:hover:text-indigo-300 bg-government-blue/10 dark:bg-indigo-950/50 hover:bg-government-blue/20 px-4 py-2 rounded-xl transition shrink-0"
            >
              <CheckCheck className="h-4 w-4" />
              Mark all as read
            </button>
          )}
        </div>

        {/* Critical banner */}
        {criticalCount > 0 && (
          <div className="mt-4 flex items-start gap-2.5 bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-900/50 p-3.5 rounded-xl text-xs text-red-800 dark:text-red-300">
            <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
            <p>
              <strong>
                {`${criticalCount} critical alert${criticalCount > 1 ? "s" : ""}`}{" "}
              </strong>
              require your immediate attention — check deadlines below.
            </p>
          </div>
        )}
      </div>

      {/* Category Filter Tabs */}
      <div className="flex gap-1.5 overflow-x-auto pb-1">
        {CATEGORIES.map((cat) => {
          const catCount = cat.id === "all"
            ? notifications.length
            : notifications.filter((n) => n.category === cat.id).length;
          return (
            <button
              key={cat.id}
              onClick={() => setActiveCategory(cat.id)}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition shrink-0 ${
                activeCategory === cat.id
                  ? "bg-gray-900 dark:bg-indigo-600 text-white shadow-sm"
                  : "bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 text-gray-600 dark:text-slate-300 hover:border-gray-300 dark:hover:border-slate-700"
              }`}
            >
              {cat.label}
              <span
                className={`text-[9px] font-bold px-1.5 py-0.5 rounded-full ${
                  activeCategory === cat.id
                    ? "bg-white/20 text-white"
                    : "bg-gray-100 dark:bg-slate-800 text-gray-500 dark:text-slate-400"
                }`}
              >
                {catCount}
              </span>
            </button>
          );
        })}
      </div>

      {/* Notifications List */}
      {filtered.length === 0 ? (
        <div className="py-16 flex flex-col items-center text-center text-gray-400 dark:text-slate-500">
          <Bell className="h-12 w-12 opacity-40 mb-4" />
          <h3 className="text-sm font-bold text-gray-600 dark:text-slate-300">All Caught Up!</h3>
          <p className="text-xs mt-1 max-w-xs">No new notifications at this time.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {/* Unread section in current page */}
          {paginated.some((n) => !n.read) && (
            <div className="space-y-3">
              <p className="text-[10px] text-gray-400 dark:text-slate-500 uppercase tracking-widest font-bold px-1">
                Unread
              </p>
              {paginated
                .filter((n) => !n.read)
                .map((notif) => (
                  <NotificationCard
                    key={notif.id}
                    notif={notif}
                    onRead={markNotificationRead}
                    onDismiss={dismissNotification}
                  />
                ))}
            </div>
          )}

          {/* Read section in current page */}
          {paginated.some((n) => n.read) && (
            <div className="space-y-3">
              <p className="text-[10px] text-gray-400 dark:text-slate-500 uppercase tracking-widest font-bold px-1 mt-2">
                Earlier
              </p>
              {paginated
                .filter((n) => n.read)
                .map((notif) => (
                  <NotificationCard
                    key={notif.id}
                    notif={notif}
                    onRead={markNotificationRead}
                    onDismiss={dismissNotification}
                  />
                ))}
            </div>
          )}

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-3 text-xs">
              <span className="text-gray-500 dark:text-slate-400 font-semibold">
                Page <span className="text-gray-800 dark:text-slate-200 font-bold">{currentPage}</span> of <span className="text-gray-800 dark:text-slate-200 font-bold">{totalPages}</span>
              </span>
              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                  disabled={currentPage === 1}
                  className="px-2.5 py-1 rounded-lg border border-gray-200 dark:border-slate-700 text-gray-600 dark:text-slate-300 hover:bg-gray-50 dark:hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  Prev
                </button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className={`px-2.5 py-1 rounded-lg text-sm font-bold transition ${
                      page === currentPage
                        ? "bg-government-blue text-white shadow-sm"
                        : "border border-gray-200 dark:border-slate-700 text-gray-600 dark:text-slate-300 hover:bg-gray-50 dark:hover:bg-slate-800"
                    }`}
                  >
                    {page}
                  </button>
                ))}
                <button
                  onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                  disabled={currentPage === totalPages}
                  className="px-2.5 py-1 rounded-lg border border-gray-200 dark:border-slate-700 text-gray-600 dark:text-slate-300 hover:bg-gray-50 dark:hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </> )}
    </div>
  );
}
