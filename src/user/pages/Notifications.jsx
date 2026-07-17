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

function timeAgo(timestamp, language) {
  const now = Date.now();
  const then = new Date(timestamp).getTime();
  const diff = Math.floor((now - then) / 1000);
  const isHi = language === "hi";
  if (diff < 60) {
    return isHi ? "अभी" : "Just now";
  }
  if (diff < 3600) {
    const mins = Math.floor(diff / 60);
    return isHi ? `${mins} मिनट पहले` : `${mins}m ago`;
  }
  if (diff < 86400) {
    const hours = Math.floor(diff / 3600);
    return isHi ? `${hours} घंटे पहले` : `${hours}h ago`;
  }
  const days = Math.floor(diff / 86400);
  return isHi ? `${days} दिन पहले` : `${days}d ago`;
}

function NotificationCard({ notif, onRead, onDismiss }) {
  const { t, language } = useApp();
  const navigate = useNavigate();
  const IconComp = ICON_MAP[notif.icon] || Bell;
  const priority = PRIORITY_CONFIG[notif.priority] || PRIORITY_CONFIG.normal;
  const catColor = CATEGORY_COLORS[notif.category] || "bg-gray-100 text-gray-700";

  const handleAction = () => {
    if (!notif.read) {
      onRead(notif.id);
    }
    if (notif.actionRoute) {
      navigate(notif.actionRoute);
    }
  };

  return (
    <div
      className={`bg-white border border-gray-200 border-l-4 ${priority.border} rounded-2xl p-4 transition hover:shadow-md ${
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
              <h3 className={`text-xs font-bold leading-snug ${!notif.read ? "text-gray-900" : "text-gray-600"}`}>
                {t("notif_" + notif.id + "_title") || notif.title}
              </h3>
              {!notif.read && (
                <span className="h-2 w-2 bg-government-blue rounded-full shrink-0" />
              )}
            </div>
            <button
              onClick={() => onDismiss(notif.id)}
              className="text-gray-300 hover:text-red-400 p-1 rounded-lg transition shrink-0"
              title={t("notif_dismiss") || "Dismiss"}
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </div>

          <p className="text-[11px] text-gray-500 leading-relaxed mt-1">
            {t("notif_" + notif.id + "_body") || notif.body}
          </p>

          <div className="flex items-center justify-between mt-2.5 flex-wrap gap-2">
            <div className="flex items-center gap-2 flex-wrap">
              <span className={`text-[9px] font-bold uppercase tracking-wide px-2 py-0.5 rounded-full border ${priority.badge}`}>
                {t("vault_priority_" + notif.priority) || notif.priority}
              </span>
              <span className={`text-[9px] font-semibold uppercase px-2 py-0.5 rounded-full border ${catColor} border-gray-200`}>
                {t("notif_cat_" + notif.category) || notif.category}
              </span>
              <span className="text-[10px] text-gray-400">{timeAgo(notif.timestamp, language)}</span>
            </div>

            <div className="flex items-center gap-2">
              {!notif.read && (
                <button
                  onClick={() => onRead(notif.id)}
                  className="text-[10px] text-gray-400 hover:text-government-blue font-semibold transition flex items-center gap-1"
                >
                  <CheckCircle2 className="h-3 w-3" />
                  {t("notif_mark_read_btn") || "Mark read"}
                </button>
              )}
              {notif.actionRoute && (
                <button
                  onClick={handleAction}
                  className="inline-flex items-center gap-1 text-[10px] font-bold text-government-blue hover:text-government-blue-dark bg-government-blue/10 hover:bg-government-blue/20 px-2.5 py-1 rounded-lg transition"
                >
                  {t("notif_" + notif.id + "_action") || notif.actionLabel || "View"}
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
    t,
  } = useApp();

  const [activeCategory, setActiveCategory] = useState("all");
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5;

  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 700);
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
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm animate-pulse">
            <div className="h-5 w-36 bg-gray-200 rounded mb-2" />
            <div className="h-3 w-48 bg-gray-100 rounded" />
          </div>
          {[1,2,3,4].map(i => <ListRowSkeleton key={i} />)}
        </div>
      ) : (<>
      {/* Header */}
      <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="bg-government-blue p-2.5 rounded-xl">
              <Bell className="h-5 w-5 text-white" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-gray-900">{t("notif_title") || "Notifications"}</h1>
              <p className="text-gray-500 text-xs mt-0.5">
                {unreadCount > 0
                  ? t("notif_unread_" + (unreadCount === 1 ? "singular" : "plural"), { n: unreadCount }) || `${unreadCount} unread notification${unreadCount > 1 ? "s" : ""}`
                  : t("notif_all_read") || "All caught up"}
              </p>
            </div>
          </div>
          {unreadCount > 0 && (
            <button
              onClick={markAllNotificationsRead}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-government-blue hover:text-government-blue-dark bg-government-blue/10 hover:bg-government-blue/20 px-4 py-2 rounded-xl transition shrink-0"
            >
              <CheckCheck className="h-4 w-4" />
              {t("notif_mark_all_as_read") || "Mark all as read"}
            </button>
          )}
        </div>

        {/* Critical banner */}
        {criticalCount > 0 && (
          <div className="mt-4 flex items-start gap-2.5 bg-red-50 border border-red-200 p-3.5 rounded-xl text-xs text-red-800">
            <AlertCircle className="h-4 w-4 text-red-600 shrink-0 mt-0.5" />
            <p>
              <strong>
                {t(criticalCount === 1 ? "notif_critical_alerts" : "notif_critical_alerts_plural", { n: criticalCount }) ||
                  `${criticalCount} critical alert${criticalCount > 1 ? "s" : ""}`}{" "}
              </strong>
              {t("notif_critical_attention") || "require your immediate attention — check deadlines below."}
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
                  ? "bg-gray-900 text-white shadow-sm"
                  : "bg-white border border-gray-200 text-gray-600 hover:border-gray-300"
              }`}
            >
              {t("notif_cat_" + cat.id) || cat.label}
              <span
                className={`text-[9px] font-bold px-1.5 py-0.5 rounded-full ${
                  activeCategory === cat.id
                    ? "bg-white/20 text-white"
                    : "bg-gray-100 text-gray-500"
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
        <EmptyState
          icon={BellOff}
          title={
            activeCategory === "all"
              ? (t("notif_all_caught_up") || "You're all caught up!")
              : (t("notif_no_category", { cat: t("notif_cat_" + activeCategory) || activeCategory }) || `No ${activeCategory} notifications`)
          }
          description={
            activeCategory === "all"
              ? (t("notif_all_caught_up_desc") || "All notifications have been read or cleared. Check back later for updates.")
              : (t("notif_no_category_desc", { cat: t("notif_cat_" + activeCategory) || activeCategory }) || `No ${activeCategory} notifications at this time.`)
          }
          action={{
            label: t("notif_go_dashboard") || "Go to Dashboard",
            href: "/dashboard",
            icon: ArrowRight
          }}
        />
      ) : (
        <div className="space-y-3">
          {/* Unread section in current page */}
          {paginated.some((n) => !n.read) && (
            <div className="space-y-3">
              <p className="text-[10px] text-gray-400 uppercase tracking-widest font-bold px-1">
                {t("notif_unread_label") || "Unread"}
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
              <p className="text-[10px] text-gray-400 uppercase tracking-widest font-bold px-1 mt-2">
                {t("notif_earlier_label") || "Earlier"}
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
            <div className="flex items-center justify-between bg-white border border-gray-200 rounded-2xl p-3 text-xs">
              <span className="text-gray-500 font-semibold">
                Page <span className="text-gray-800 font-bold">{currentPage}</span> of <span className="text-gray-800 font-bold">{totalPages}</span>
              </span>
              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                  disabled={currentPage === 1}
                  className="px-2.5 py-1 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
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
                        : "border border-gray-200 text-gray-600 hover:bg-gray-50"
                    }`}
                  >
                    {page}
                  </button>
                ))}
                <button
                  onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                  disabled={currentPage === totalPages}
                  className="px-2.5 py-1 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
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
