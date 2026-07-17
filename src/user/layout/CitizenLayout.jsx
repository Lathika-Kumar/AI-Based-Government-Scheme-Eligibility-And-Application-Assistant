import React, { useState, useEffect, useMemo } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { CONFIG } from "@config/env";
import * as LucideIcons from "lucide-react";
import {
  LayoutDashboard,
  UserCircle,
  Sparkles,
  ClipboardList,
  FileText,
  LogOut,
  Menu,
  X,
  Building2,
  Bell,
  RefreshCw,
  HelpCircle,
  Check,
  ArrowRight,
  ThumbsUp
} from "lucide-react";

function OfficialBanner() {
  const [isOpen, setIsOpen] = useState(false);
  const { t } = useApp();
  return (
    <div className="bg-slate-900 text-slate-350 border-b border-slate-800 shrink-0 select-none">
      <div className="max-w-7xl mx-auto px-4 py-2 flex flex-col sm:flex-row sm:items-center justify-between text-[11px] font-medium tracking-wide gap-2">
        <div className="flex items-center space-x-2">
          <span>🇮🇳</span>
          <span className="uppercase text-slate-400 font-bold tracking-wider text-[9px] bg-slate-800 px-1.5 py-0.5 rounded mr-1">{t("official_sim_title")}</span>
          <span>{t("official_sim_desc")}</span>
        </div>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="text-indigo-400 hover:text-indigo-300 underline font-semibold flex items-center space-x-1 focus:outline-none focus:ring-1 focus:ring-indigo-400 px-1 rounded transition self-start sm:self-auto"
          aria-expanded={isOpen}
        >
          {isOpen ? t("official_hide_btn") : t("official_verify_btn")}
        </button>
      </div>
      {isOpen && (
        <div className="bg-slate-950 border-t border-slate-800 px-4 py-3 text-[11px] text-slate-400 leading-relaxed">
          <div className="max-w-7xl mx-auto grid sm:grid-cols-2 gap-4">
            <div>
              <p className="font-bold text-slate-200">🔒 {t("official_domain_val")}</p>
              <p className="mt-1">
                {t("official_domain_desc")}
              </p>
            </div>
            <div>
              <p className="font-bold text-slate-200">⚙️ {t("official_sandbox_title")}</p>
              <p className="mt-1">
                {t("official_sandbox_desc")}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// Relative time helper
function timeAgo(timestamp, t) {
  const now = Date.now();
  const then = new Date(timestamp).getTime();
  const diff = Math.floor((now - then) / 1000);
  if (diff < 60) {
return t("time_just_now");
}
  if (diff < 3605) {
return t("time_m_ago").replace("{n}", Math.floor(diff / 60));
}
  if (diff < 86400) {
return t("time_h_ago").replace("{n}", Math.floor(diff / 3600));
}
  return t("time_d_ago").replace("{n}", Math.floor(diff / 86400));
}

// Category Badge Color Helper
function getCatBadge(category) {
  switch (category) {
    case "application":
      return { label: "notif_cat_application", style: "bg-indigo-50 text-indigo-700 border-indigo-200" };
    case "document":
      return { label: "notif_cat_document", style: "bg-emerald-50 text-emerald-700 border-emerald-200" };
    case "deadline":
      return { label: "notif_cat_deadline", style: "bg-rose-50 text-rose-700 border-rose-200" };
    case "system":
      return { label: "notif_cat_system", style: "bg-violet-50 text-violet-700 border-violet-200" };
    default:
      return { label: category, style: "bg-slate-50 text-slate-600 border-slate-200" };
  }
}

// Priority Badge Color Helper
function getPriorityBadge(priority) {
  switch (priority) {
    case "critical":
      return { label: "notif_priority_critical", style: "bg-rose-100 text-rose-800 border-rose-200 font-extrabold" };
    case "high":
      return { label: "notif_priority_high", style: "bg-amber-100 text-amber-800 border-amber-200 font-bold" };
    default:
      return { label: "notif_priority_normal", style: "bg-slate-100 text-slate-700 border-slate-200" };
  }
}

export default function CitizenLayout() {
  const { user, logout } = useAuth();
  const {
    resetData,
    language,
    changeLanguage,
    t,
    unreadCount,
    notifications,
    markNotificationRead,
    markAllNotificationsRead
  } = useApp();

  const navigate = useNavigate();
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  // Drawer & Filter States
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [activeFilter, setActiveFilter] = useState("all");

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  // Esc key closure listener
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        setIsDrawerOpen(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  // Filtered notifications logic
  const filteredNotifications = useMemo(() => {
    return notifications.filter((n) => {
      if (activeFilter === "all") {
return true;
}
      if (activeFilter === "application") {
return n.category === "application";
}
      if (activeFilter === "document") {
return n.category === "document";
}
      if (activeFilter === "deadline") {
return n.category === "deadline";
}
      if (activeFilter === "ai") {
        return n.icon === "Bot" || n.title.toLowerCase().includes("recommend") || n.title.toLowerCase().includes("schemeai");
      }
      if (activeFilter === "system") {
        const isAI = n.icon === "Bot" || n.title.toLowerCase().includes("recommend") || n.title.toLowerCase().includes("schemeai");
        return n.category === "system" && !isAI;
      }
      return true;
    });
  }, [notifications, activeFilter]);

  // Counts per filter category
  const filterCounts = useMemo(() => {
    return {
      all: notifications.length,
      application: notifications.filter(n => n.category === "application").length,
      document: notifications.filter(n => n.category === "document").length,
      deadline: notifications.filter(n => n.category === "deadline").length,
      ai: notifications.filter(n => n.icon === "Bot" || n.title.toLowerCase().includes("recommend") || n.title.toLowerCase().includes("schemeai")).length,
      system: notifications.filter(n => {
        const isAI = n.icon === "Bot" || n.title.toLowerCase().includes("recommend") || n.title.toLowerCase().includes("schemeai");
        return n.category === "system" && !isAI;
      }).length
    };
  }, [notifications]);

  // Unified list of sidebar items (with Notifications completely removed)
  const navItems = [
    { name: "Overview Dashboard", translationKey: "nav_dashboard", path: "/dashboard", icon: LayoutDashboard },
    { name: "Eligibility Profile", translationKey: "nav_profile", path: "/profile", icon: UserCircle },
    { name: "Matching Schemes", translationKey: "nav_schemes", path: "/recommendations", icon: Sparkles },
    { name: "Document Vault", translationKey: "nav_vault", path: "/documents", icon: FileText },
    { name: "Application Tracker", translationKey: "nav_tracker", path: "/tracker", icon: ClipboardList },
    { name: "Help & Support", translationKey: "nav_help", path: "/help", icon: HelpCircle },
    { name: "Your Feedback", translationKey: "nav_feedback", path: "/feedback", icon: ThumbsUp }
  ];

  return (
    <div className="flex h-screen bg-slate-100 overflow-hidden">
      {/* Accessible skip link */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-3 focus:left-3 focus:bg-indigo-600 focus:text-white focus:px-4 focus:py-2 focus:rounded-xl focus:z-50 font-bold shadow-lg transition"
      >
        {t("nav_skip_content")}
      </a>

      {/* SIDEBAR FOR DESKTOP */}
      <aside className="hidden lg:flex lg:flex-col lg:w-64 bg-slate-900 text-white shrink-0 border-r border-slate-800">
        {/* Brand Header */}
        <div className="h-16 flex items-center px-6 bg-slate-950 border-b border-slate-800">
          <Link to="/" className="flex items-center space-x-2.5">
            <div className="bg-indigo-600 p-1.5 rounded-lg text-white">
              <Building2 className="h-5 w-5" />
            </div>
            <div>
              <span className="font-bold text-sm block tracking-wide">SchemeBridge</span>
              <span className="text-[9px] text-slate-400 block tracking-widest uppercase font-semibold">{t("nav_citizen_account")}</span>
            </div>
          </Link>
        </div>

        {/* User Card */}
        <div className="p-4 border-b border-slate-800 bg-slate-900/50">
          <div className="flex items-center space-x-3">
            <div className="bg-indigo-500/20 text-indigo-400 font-bold h-10 w-10 rounded-full flex items-center justify-center border border-indigo-500/30">
              {user?.name ? user.name[0] : "C"}
            </div>
            <div className="overflow-hidden">
              <p className="text-xs font-semibold text-slate-200 truncate">{user?.name || t("nav_citizen_account")}</p>
              <p className="text-[10px] text-slate-400 truncate">{user?.email || "citizen@schemebridge.in"}</p>
            </div>
          </div>
        </div>

        {/* Sidebar Links */}
        <nav className="flex-1 px-4 py-4 space-y-1.5 overflow-y-auto">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition duration-150 ${
                  isActive
                    ? "bg-indigo-600 text-white shadow-sm"
                    : "text-slate-400 hover:bg-slate-800 hover:text-slate-100"
                }`
              }
            >
              <item.icon className="h-4 w-4 shrink-0" />
              <span className="flex-1">{t(item.translationKey) || item.name}</span>
            </NavLink>
          ))}
        </nav>

        {/* Sidebar Footer / Logout */}
        <div className="p-4 border-t border-slate-800 bg-slate-950 space-y-2 shrink-0">
          <button
            onClick={handleLogout}
            className="flex items-center space-x-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-rose-400 hover:bg-rose-500/10 transition duration-150"
          >
            <LogOut className="h-4 w-4 shrink-0" />
            <span>{t("nav_logout")}</span>
          </button>
          <button
            onClick={resetData}
            className="flex items-center space-x-3 w-full px-3 py-2 rounded-lg text-xs font-semibold text-slate-400 hover:bg-slate-800 hover:text-slate-250 transition duration-150"
            title={t("nav_reset_tooltip")}
          >
            <RefreshCw className="h-3.5 w-3.5 shrink-0 text-slate-500" />
            <span>{t("nav_reset")}</span>
          </button>
        </div>

        {/* Release Version Badge */}
        <div className="px-4 py-2 bg-slate-955 bg-slate-950 border-t border-slate-800/60 text-center text-[10px] text-slate-500 flex items-center justify-between select-none shrink-0">
          <span>Ver: {CONFIG.PORTAL_VERSION}</span>
          <span className="bg-indigo-950 text-indigo-400 border border-indigo-900 px-1.5 py-0.2 rounded font-bold uppercase tracking-wider text-[8px]">
            {CONFIG.ENVIRONMENT}
          </span>
        </div>
      </aside>

      {/* MOBILE DRAWER */}
      {isMobileOpen && (
        <div className="lg:hidden fixed inset-0 z-50 flex">
          {/* Overlay backdrop */}
          <div
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm"
            onClick={() => setIsMobileOpen(false)}
          />

          {/* Drawer panel */}
          <div className="relative flex flex-col w-64 max-w-xs bg-slate-900 text-white h-full shadow-xl">
            <div className="h-16 flex items-center justify-between px-6 bg-slate-950 border-b border-slate-800">
              <Link to="/" className="flex items-center space-x-2">
                <Building2 className="h-5 w-5 text-indigo-500" />
                <span className="font-bold text-sm">{t("notif_sidebar_nav")}</span>
              </Link>
              <button
                onClick={() => setIsMobileOpen(false)}
                className="text-slate-400 hover:text-white p-1 rounded-md"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="p-4 border-b border-slate-800">
              <div className="flex items-center space-x-3">
                <div className="bg-indigo-500/20 text-indigo-400 font-bold h-9 w-9 rounded-full flex items-center justify-center">
                  {user?.name ? user.name[0] : "C"}
                </div>
                <div>
                  <p className="text-xs font-semibold truncate">{user?.name || t("nav_citizen_account")}</p>
                  <p className="text-[10px] text-slate-400 truncate">{user?.email}</p>
                </div>
              </div>
            </div>

            <nav className="flex-1 px-4 py-4 space-y-1.5 overflow-y-auto">
              {navItems.map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  onClick={() => setIsMobileOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                      isActive
                        ? "bg-indigo-600 text-white"
                        : "text-slate-400 hover:bg-slate-800 hover:text-slate-100"
                    }`
                  }
                >
                  <item.icon className="h-4 w-4" />
                  <span className="flex-1">{t(item.translationKey) || item.name}</span>
                </NavLink>
              ))}
            </nav>

            <div className="p-4 border-t border-slate-800 bg-slate-950 space-y-2 shrink-0">
              <button
                onClick={handleLogout}
                className="flex items-center space-x-3 w-full px-3 py-2 rounded-lg text-sm text-rose-400 hover:bg-rose-500/10 transition"
              >
                <LogOut className="h-4 w-4" />
                <span>{t("nav_logout")}</span>
              </button>
              <button
                onClick={resetData}
                className="flex items-center space-x-3 w-full px-3 py-2 rounded-lg text-xs font-semibold text-slate-400 hover:bg-slate-800 transition"
              >
                <RefreshCw className="h-4 w-4 text-slate-500" />
                <span>{t("nav_reset")}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* RIGHT SIDE MAIN CONTAINER */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Official Banner */}
        <OfficialBanner />

        {/* Top Navbar */}
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 sm:px-6 shrink-0 shadow-sm z-30">
          <div className="flex items-center space-x-4">
            <button
              onClick={() => setIsMobileOpen(true)}
              className="lg:hidden text-slate-600 hover:text-slate-900 p-1 rounded-md"
            >
              <Menu className="h-6 w-6" />
            </button>
            <div className="text-sm font-semibold text-slate-800">
              {t("national_egov_system")}
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {/* Language Switcher */}
            <div className="relative inline-flex items-center">
              <span className="absolute left-2.5 text-xs pointer-events-none">🌐</span>
              <select
                value={language}
                onChange={(e) => changeLanguage(e.target.value)}
                className="pl-7 pr-8 py-1.5 bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-semibold shadow-sm transition focus:outline-none focus:ring-2 focus:ring-indigo-500 appearance-none cursor-pointer"
                aria-label="Select Language"
              >
                <option value="en">English</option>
                <option value="hi">हिंदी (Hindi)</option>
                <option value="ta">தமிழ் (Tamil)</option>
                <option value="te">తెలుగు (Telugu)</option>
                <option value="kn">ಕನ್ನಡ (Kannada)</option>
              </select>
              <span className="absolute right-2 text-[10px] text-slate-400 pointer-events-none">&#9662;</span>
            </div>

            {/* Notification Bell (Only Navigation Point) */}
            <button
              onClick={() => setIsDrawerOpen(!isDrawerOpen)}
              className="relative text-slate-500 hover:text-slate-850 hover:text-slate-800 p-2 rounded-full hover:bg-slate-100 transition focus:outline-none focus:ring-2 focus:ring-indigo-500"
              aria-label="Toggle notifications drawer"
            >
              <Bell className="h-5 w-5" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-rose-500 text-white text-[9px] font-black rounded-full h-4 min-w-4 px-1 flex items-center justify-center border border-white">
                  {unreadCount > 9 ? "9+" : unreadCount}
                </span>
              )}
            </button>

            {/* Profile Pill */}
            <div className="flex items-center space-x-2 pl-2 border-l border-slate-200">
              <span className="hidden sm:inline text-xs font-medium text-slate-700 bg-slate-100 px-2.5 py-1 rounded-full border border-slate-200">
                {t("category_caste_label")}: <strong className="text-indigo-705 text-indigo-700 font-semibold">{user?.caste || "OBC"}</strong>
              </span>
            </div>
          </div>
        </header>

        {/* Main Work Area */}
        <main id="main-content" tabIndex="-1" className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 bg-slate-50 focus:outline-none">
          <div className="max-w-6xl mx-auto space-y-6">
            <Outlet />
          </div>
        </main>
      </div>

      {/* ================= NOTIFICATION DRAWER ================= */}
      <div
        className={`fixed inset-0 z-50 overflow-hidden transition-opacity duration-300 ease-in-out ${
          isDrawerOpen ? "opacity-100 pointer-events-auto" : "opacity-0 pointer-events-none"
        }`}
        role="dialog"
        aria-modal="true"
      >
        {/* Click outside target */}
        <div
          onClick={() => setIsDrawerOpen(false)}
          className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity duration-300"
        />

        <div className="fixed inset-y-0 right-0 pl-10 max-w-full flex sm:pl-16">
          <div
            className={`w-screen max-w-md bg-white shadow-2xl flex flex-col transform transition-transform duration-300 ease-in-out ${
              isDrawerOpen ? "translate-x-0" : "translate-x-full"
            }`}
          >
            {/* Header */}
            <div className="px-5 py-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-base" role="img" aria-label="bell">🔔</span>
                <span className="text-xs font-bold text-slate-800 uppercase tracking-widest">
                  {t("notif_title")}
                </span>
                {unreadCount > 0 && (
                  <span className="bg-rose-500 text-white text-[9px] font-black rounded-full h-4 min-w-4 px-1.5 flex items-center justify-center">
                    {unreadCount}
                  </span>
                )}
              </div>

              <div className="flex items-center gap-3">
                {unreadCount > 0 && (
                  <button
                    onClick={markAllNotificationsRead}
                    className="text-[10px] text-indigo-650 text-indigo-600 hover:text-indigo-800 font-bold uppercase transition"
                  >
                    {t("notif_mark_all_read")}
                  </button>
                )}
                <button
                  onClick={() => setIsDrawerOpen(false)}
                  className="text-slate-400 hover:text-slate-600 p-1.5 rounded-md hover:bg-slate-100 transition"
                  aria-label="Close drawer"
                >
                  <X className="h-4.5 w-4.5" />
                </button>
              </div>
            </div>

            {/* Filter chips list */}
            <div className="px-4 py-3 border-b border-slate-100 flex gap-1.5 overflow-x-auto shrink-0 scrollbar-none">
              {[
                { id: "all", labelKey: "notif_cat_all" },
                { id: "application", labelKey: "notif_cat_application" },
                { id: "document", labelKey: "notif_cat_document" },
                { id: "deadline", labelKey: "notif_cat_deadline" },
                { id: "ai", labelKey: "notif_cat_ai" },
                { id: "system", labelKey: "notif_cat_system" }
              ].map(chip => {
                const count = filterCounts[chip.id];
                const isSelected = activeFilter === chip.id;
                return (
                  <button
                    key={chip.id}
                    onClick={() => setActiveFilter(chip.id)}
                    className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold whitespace-nowrap border transition ${
                      isSelected
                        ? "bg-slate-900 border-slate-950 text-white"
                        : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100"
                    }`}
                  >
                    <span>{t(chip.labelKey)}</span>
                    <span className={`text-[9px] font-bold rounded-full px-1.5 py-0.2 ${
                      isSelected ? "bg-white/20 text-white" : "bg-slate-200 text-slate-500"
                    }`}>
                      {count}
                    </span>
                  </button>
                );
              })}
            </div>

            {/* Notifications body wrapper */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3">
              {filteredNotifications.length === 0 ? (
                <div className="flex flex-col items-center justify-center text-center py-24 text-slate-400 space-y-2.5">
                  <span className="text-3xl" role="img" aria-label="envelope">📬</span>
                  <p className="font-semibold text-slate-700 text-xs">{t("notif_empty_title")}</p>
                  <p className="text-[10px] max-w-[220px] text-slate-400 leading-relaxed">{t("notif_empty_desc")}</p>
                </div>
              ) : (
                filteredNotifications.map(notif => {
                  const IconComp = LucideIcons[notif.icon] || LucideIcons.Bell;
                  const catBadge = getCatBadge(notif.category);
                  const priorityBadge = getPriorityBadge(notif.priority);

                  return (
                    <div
                      key={notif.id}
                      className={`p-3.5 border rounded-2xl transition duration-200 hover:-translate-y-0.5 hover:shadow-md flex flex-col gap-2.5 ${
                        notif.read ? "bg-white border-slate-200 opacity-80" : "bg-indigo-50/20 border-indigo-150 ring-1 ring-indigo-50/10 shadow-xs"
                      }`}
                    >
                      <div className="flex gap-2.5 items-start">
                        <div className={`p-2 rounded-xl border shrink-0 ${
                          notif.read ? "bg-slate-550 bg-slate-100 text-slate-400 border-slate-200" : "bg-indigo-50 text-indigo-600 border-indigo-100"
                        }`}>
                          <IconComp className="h-4.5 w-4.5" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex justify-between items-start gap-1">
                            <h4 className={`text-xs font-bold text-slate-800 truncate leading-snug ${
                              notif.read ? "" : "font-black text-slate-900"
                            }`}>
                              {notif.title}
                            </h4>
                            {!notif.read && (
                              <span className="h-1.5 w-1.5 rounded-full bg-indigo-600 shrink-0 mt-1.5" />
                            )}
                          </div>
                          <p className="text-[10px] text-slate-500 leading-relaxed mt-1">
                            {notif.body}
                          </p>
                        </div>
                      </div>

                      {/* Meta badge line */}
                      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100/60 pt-2 text-[9px] font-bold">
                        <div className="flex gap-1.5">
                          <span className={`px-2 py-0.5 border rounded-full uppercase ${catBadge.style}`}>
                            {t(catBadge.label)}
                          </span>
                          <span className={`px-2 py-0.5 border rounded-full uppercase ${priorityBadge.style}`}>
                            {t(priorityBadge.label)}
                          </span>
                          <span className="text-slate-400 font-medium self-center">{timeAgo(notif.timestamp, t)}</span>
                        </div>

                        <div className="flex gap-2 items-center">
                          {!notif.read && (
                            <button
                              onClick={() => markNotificationRead(notif.id)}
                              className="text-slate-400 hover:text-indigo-600 transition"
                              title="Mark read"
                            >
                              <Check className="h-3.5 w-3.5 text-slate-450" />
                            </button>
                          )}
                          {notif.actionRoute && (
                            <button
                              onClick={() => {
                                setIsDrawerOpen(false);
                                navigate(notif.actionRoute);
                              }}
                              className="inline-flex items-center gap-0.5 text-indigo-600 hover:text-indigo-800 font-black"
                            >
                              <span>{notif.actionLabelKey ? t(notif.actionLabelKey) : (notif.actionLabel || t("notif_action_view"))}</span>
                              <ArrowRight className="h-3 w-3" />
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* View All footer link */}
            <div className="p-4 border-t border-slate-200 bg-slate-50 text-center shrink-0">
              <button
                onClick={() => {
                  setIsDrawerOpen(false);
                  navigate("/notifications");
                }}
                className="w-full py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1"
              >
                <span>{t("notif_view_all")}</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </button>
            </div>

          </div>
        </div>
      </div>

    </div>
  );
}
