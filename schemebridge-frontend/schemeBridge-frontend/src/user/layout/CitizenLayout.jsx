import React, { useState, useEffect, useMemo } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { CONFIG } from "@config/env";
import * as LucideIcons from "lucide-react";
import ErrorBoundary from "@components/ErrorBoundary";
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
  ThumbsUp,
  Search,
  FolderLock,
  UserCheck,
  MessageSquareShare,
  Sliders
} from "lucide-react";

function OfficialBanner() {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <div className="bg-slate-900 text-slate-350 border-b border-slate-800 shrink-0 select-none">
      <div className="max-w-7xl mx-auto px-4 py-2 flex flex-col sm:flex-row sm:items-center justify-between text-[11px] font-medium tracking-wide gap-2">
        <div className="flex items-center space-x-2">
          <span>🇮🇳</span>
          <span className="uppercase text-slate-400 font-bold tracking-wider text-[9px] bg-slate-800 px-1.5 py-0.5 rounded mr-1">Official Website Simulation</span>
          <span>This is a simulated government portal for demonstration purposes only.</span>
        </div>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="text-indigo-400 hover:text-indigo-300 underline font-semibold flex items-center space-x-1 focus:outline-none focus:ring-1 focus:ring-indigo-400 px-1 rounded transition self-start sm:self-auto"
          aria-expanded={isOpen}
        >
          {isOpen ? "Hide Details" : "Verify Authenticity"}
        </button>
      </div>
      {isOpen && (
        <div className="bg-slate-950 border-t border-slate-800 px-4 py-3 text-[11px] text-slate-400 leading-relaxed">
          <div className="max-w-7xl mx-auto grid sm:grid-cols-2 gap-4">
            <div>
              <p className="font-bold text-slate-200">🔒 schemebridge.gov.in</p>
              <p className="mt-1">
                Official government domain for scheme information and services.
              </p>
            </div>
            <div>
              <p className="font-bold text-slate-200">⚙️ Sandbox Environment Notice</p>
              <p className="mt-1">
                All data entered here is stored locally in your browser and is not transmitted to any government servers.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// Relative time helper
function timeAgo(timestamp) {
  const now = Date.now();
  const then = new Date(timestamp).getTime();
  const diff = Math.floor((now - then) / 1000);
  if (diff < 60) {
return "just now";
}
  if (diff < 3605) {
return `${Math.floor(diff / 60)} min ago`;
}
  if (diff < 86400) {
return `${Math.floor(diff / 3600)} hour ago`;
}
  return `${Math.floor(diff / 86400)} day ago`;
}

// Category Badge Color Helper
function getCatBadge(category) {
  switch (category) {
    case "application":
      return { label: "Application", style: "bg-indigo-50 text-indigo-700 border-indigo-200" };
    case "document":
      return { label: "Document", style: "bg-emerald-50 text-emerald-700 border-emerald-200" };
    case "deadline":
      return { label: "Deadline", style: "bg-rose-50 text-rose-700 border-rose-200" };
    case "system":
      return { label: "System", style: "bg-violet-50 text-violet-700 border-violet-200" };
    default:
      return { label: category, style: "bg-slate-50 text-slate-600 border-slate-200" };
  }
}

// Priority Badge Color Helper
function getPriorityBadge(priority) {
  switch (priority) {
    case "critical":
      return { label: "Critical", style: "bg-rose-100 text-rose-800 border-rose-200 font-extrabold" };
    case "high":
      return { label: "High", style: "bg-amber-100 text-amber-800 border-amber-200 font-bold" };
    default:
      return { label: "Normal", style: "bg-slate-100 text-slate-700 border-slate-200" };
  }
}

export default function CitizenLayout() {
  const { user, profileLoading, loading, logout } = useAuth();
  const {
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

  const citizenDisplayName = useMemo(() => {
    return user?.displayName || user?.name || `${user?.firstName || ""} ${user?.lastName || ""}`.trim() || (user?.email ? user.email.split("@")[0] : "");
  }, [user]);

  const citizenInitial = useMemo(() => {
    return citizenDisplayName ? citizenDisplayName.charAt(0).toUpperCase() : "C";
  }, [citizenDisplayName]);

  const handleLogout = async () => {
    await logout();
    navigate("/", { replace: true });
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
      if (activeFilter === "all") return true;
      if (activeFilter === "application") return n.category === "application";
      if (activeFilter === "document") return n.category === "document";
      if (activeFilter === "deadline") return n.category === "deadline";
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
      }).length,
      unread: notifications.filter(n => !n.isRead).length,
      read: notifications.filter(n => n.isRead).length,
    };
  }, [notifications]);

  // Nav Items (Notifications removed from sidebar per Phase 20 requirements)
  const navItems = [
    { name: "Overview Dashboard", path: "/dashboard", icon: LayoutDashboard },
    { name: "Matching Schemes", path: "/schemes", icon: Sparkles },
    { name: "My Applications", path: "/applications", icon: FileText },
    { name: "Document Vault", path: "/documents", icon: FolderLock },
    { name: "Citizen Profile", path: "/profile", icon: UserCheck },
    { name: "Help & Grievance", path: "/help", icon: HelpCircle },
    { name: "Portal Feedback", path: "/feedback", icon: MessageSquareShare },
    { name: "Settings", path: "/settings", icon: Sliders },
  ];

  return (
    <div className="flex h-screen bg-slate-900 overflow-hidden font-sans text-slate-100 antialiased selection:bg-indigo-500 selection:text-white">
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
              <span className="text-[9px] text-slate-400 block tracking-widest uppercase font-semibold">Citizen Portal</span>
            </div>
          </Link>
        </div>

        {/* User Card */}
        <div className="p-4 border-b border-slate-800 bg-slate-900/50">
          {loading || profileLoading || !user ? (
            <div className="flex items-center space-x-3 animate-pulse">
              <div className="bg-slate-800 h-10 w-10 rounded-full shrink-0" />
              <div className="flex-1 space-y-1.5 overflow-hidden">
                <div className="h-3.5 bg-slate-800 rounded w-28" />
                <div className="h-2.5 bg-slate-800/60 rounded w-36" />
              </div>
            </div>
          ) : (
            <div className="flex items-center space-x-3">
              <div className="bg-indigo-500/20 text-indigo-400 font-bold h-10 w-10 rounded-full flex items-center justify-center border border-indigo-500/30 shrink-0">
                {citizenInitial}
              </div>
              <div className="overflow-hidden">
                <p className="text-xs font-semibold text-slate-200 truncate">{citizenDisplayName || user.email}</p>
                <p className="text-[10px] text-slate-400 truncate">{user.email}</p>
              </div>
            </div>
          )}
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
              <span className="flex-1">{item.name}</span>
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
            <span>Logout</span>
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
                <span className="font-bold text-sm">SchemeBridge</span>
              </Link>
              <button
                onClick={() => setIsMobileOpen(false)}
                className="text-slate-400 hover:text-white p-1 rounded-md"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="p-4 border-b border-slate-800">
              {loading || profileLoading || !user ? (
                <div className="flex items-center space-x-3 animate-pulse">
                  <div className="bg-slate-800 h-9 w-9 rounded-full shrink-0" />
                  <div className="flex-1 space-y-1.5 overflow-hidden">
                    <div className="h-3.5 bg-slate-800 rounded w-24" />
                    <div className="h-2.5 bg-slate-800/60 rounded w-32" />
                  </div>
                </div>
              ) : (
                <div className="flex items-center space-x-3">
                  <div className="bg-indigo-500/20 text-indigo-400 font-bold h-9 w-9 rounded-full flex items-center justify-center border border-indigo-500/30 shrink-0">
                    {citizenInitial}
                  </div>
                  <div className="overflow-hidden">
                    <p className="text-xs font-semibold truncate">{citizenDisplayName || user.email}</p>
                    <p className="text-[10px] text-slate-400 truncate">{user.email}</p>
                  </div>
                </div>
              )}
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
                  <item.icon className="h-4 w-4 shrink-0" />
                  <span>{item.name}</span>
                </NavLink>
              ))}
            </nav>

            <div className="p-4 border-t border-slate-800 bg-slate-950 space-y-2 shrink-0">
              <button
                onClick={handleLogout}
                className="flex items-center space-x-3 w-full px-3 py-2 rounded-lg text-sm text-rose-400 hover:bg-rose-500/10 transition"
              >
                <LogOut className="h-4 w-4" />
                <span>Logout</span>
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
        <header className="h-16 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between px-4 sm:px-6 shrink-0 shadow-sm z-30 relative transition-colors">
          <div className="flex items-center space-x-4">
            <button
              onClick={() => setIsMobileOpen(true)}
              className="lg:hidden text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white p-1 rounded-md"
            >
              <Menu className="h-6 w-6" />
            </button>
            <div className="text-sm font-semibold text-slate-800 dark:text-slate-200">
              National E-Gov System
            </div>
          </div>

          <div className="flex items-center space-x-3">
            {/* Notification Bell (Only Navigation Point) */}
            <button
              onClick={() => setIsDrawerOpen(!isDrawerOpen)}
              className="relative text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-100 p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 transition focus:outline-none focus:ring-2 focus:ring-indigo-500"
              aria-label="Toggle notifications drawer"
            >
              <Bell className="h-5 w-5" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-rose-500 text-white text-[9px] font-black rounded-full h-4 min-w-4 px-1 flex items-center justify-center border border-white">
                  {unreadCount > 9 ? "9+" : unreadCount}
                </span>
              )}
            </button>

            {/* User Avatar Initial */}
            <div className="flex items-center space-x-2 pl-2 border-l border-slate-200 dark:border-slate-800">
              <div className="bg-indigo-600 text-white font-bold h-8 w-8 rounded-full flex items-center justify-center text-xs shadow-sm">
                {citizenInitial}
              </div>
            </div>
          </div>
        </header>

        {/* Main Work Area */}
        <main id="main-content" tabIndex="-1" className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 focus:outline-none transition-colors">
          <div className="max-w-6xl mx-auto space-y-6">
            <ErrorBoundary>
              <Outlet />
            </ErrorBoundary>
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
            className={`w-screen max-w-md bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 shadow-2xl flex flex-col transform transition-transform duration-300 ease-in-out border-l border-slate-200 dark:border-slate-800 ${
              isDrawerOpen ? "translate-x-0" : "translate-x-full"
            }`}
          >
            {/* Header */}
            <div className="px-5 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-base" role="img" aria-label="bell">🔔</span>
                <span className="text-xs font-bold text-slate-800 dark:text-slate-200 uppercase tracking-widest">
                  Notifications
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
                    className="text-[10px] text-indigo-600 hover:text-indigo-800 dark:text-indigo-400 dark:hover:text-indigo-300 font-bold uppercase transition"
                  >
                    Mark All as Read
                  </button>
                )}
                <button
                  onClick={() => setIsDrawerOpen(false)}
                  className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1.5 rounded-md hover:bg-slate-100 dark:hover:bg-slate-800 transition"
                  aria-label="Close drawer"
                >
                  <X className="h-4.5 w-4.5" />
                </button>
              </div>
            </div>

            {/* Filter chips list */}
            <div className="px-4 py-3 border-b border-slate-100 dark:border-slate-800 flex gap-1.5 overflow-x-auto shrink-0 scrollbar-none">
              {[
                { id: "all", label: "All" },
                { id: "application", label: "Application" },
                { id: "document", label: "Document" },
                { id: "deadline", label: "Deadline" },
                { id: "ai", label: "AI" },
                { id: "system", label: "System" }
              ].map(chip => {
                const count = filterCounts[chip.id];
                const isSelected = activeFilter === chip.id;
                return (
                  <button
                    key={chip.id}
                    onClick={() => setActiveFilter(chip.id)}
                    className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold whitespace-nowrap border transition ${
                      isSelected
                        ? "bg-indigo-600 border-indigo-600 text-white"
                        : "bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
                    }`}
                  >
                    <span>{chip.label}</span>
                    <span className={`text-[9px] font-bold rounded-full px-1.5 py-0.2 ${
                      isSelected ? "bg-white/20 text-white" : "bg-slate-200 dark:bg-slate-700 text-slate-500 dark:text-slate-400"
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
                  <p className="font-semibold text-slate-700 dark:text-slate-300 text-xs">No notifications</p>
                  <p className="text-[10px] max-w-[220px] text-slate-400 dark:text-slate-500 leading-relaxed">You have no new notifications</p>
                </div>
              ) : (
                filteredNotifications.map(notif => {
                  const IconComp = LucideIcons[notif.icon] || LucideIcons.Bell;
                  const catBadge = getCatBadge(notif.category);
                  const priorityBadge = getPriorityBadge(notif.priority);

                  return (
                    <div
                      key={notif.id}
                      onClick={() => (!notif.read) && markNotificationRead(notif.id)}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          if (!notif.read) markNotificationRead(notif.id);
                        }
                      }}
                      className={`p-3.5 border rounded-2xl transition duration-200 hover:-translate-y-0.5 hover:shadow-md flex flex-col gap-2.5 cursor-pointer ${
                        notif.read ? "bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 opacity-80" : "bg-indigo-50/20 dark:bg-indigo-950/30 border-indigo-150 dark:border-indigo-900/60 ring-1 ring-indigo-50/10 shadow-xs"
                      }`}
                    >
                      <div className="flex gap-2.5 items-start">
                        <div className={`p-2 rounded-xl border shrink-0 ${
                          notif.read ? "bg-slate-100 dark:bg-slate-800 text-slate-400 dark:text-slate-500 border-slate-200 dark:border-slate-700" : "bg-indigo-50 dark:bg-indigo-900/50 text-indigo-600 dark:text-indigo-400 border-indigo-100 dark:border-indigo-800"
                        }`}>
                          <IconComp className="h-4.5 w-4.5" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex justify-between items-start gap-1">
                            <h4 className={`text-xs font-bold text-slate-800 dark:text-slate-200 truncate leading-snug ${
                              notif.read ? "" : "font-black text-slate-900 dark:text-white"
                            }`}>
                              {notif.title}
                            </h4>
                            {!notif.read && (
                              <span className="h-1.5 w-1.5 rounded-full bg-indigo-600 shrink-0 mt-1.5" />
                            )}
                          </div>
                          <p className="text-[10px] text-slate-500 dark:text-slate-400 leading-relaxed mt-1">
                            {notif.body}
                          </p>
                        </div>
                      </div>

                      {/* Meta badge line */}
                      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100/60 dark:border-slate-800 pt-2 text-[9px] font-bold">
                        <div className="flex gap-1.5">
                          <span className={`px-2 py-0.5 border rounded-full uppercase ${catBadge.style}`}>
                            {catBadge.label}
                          </span>
                          <span className={`px-2 py-0.5 border rounded-full uppercase ${priorityBadge.style}`}>
                            {priorityBadge.label}
                          </span>
                          <span className="text-slate-400 dark:text-slate-500 font-medium self-center">{timeAgo(notif.timestamp)}</span>
                        </div>

                        <div className="flex gap-2 items-center">
                          {!notif.read && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                markNotificationRead(notif.id);
                              }}
                              className="text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 transition"
                              title="Mark read"
                            >
                              <Check className="h-3.5 w-3.5 text-slate-450" />
                            </button>
                          )}
                          {notif.actionRoute && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                if (!notif.read) {
                                  markNotificationRead(notif.id);
                                }
                                setIsDrawerOpen(false);
                                navigate(notif.actionRoute);
                              }}
                              className="inline-flex items-center gap-0.5 text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300 font-black"
                            >
                              {notif.actionLabelKey ? notif.actionLabelKey : (notif.actionLabel || "View")}
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
            <div className="p-4 border-t border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 text-center shrink-0">
              <button
                onClick={() => {
                  setIsDrawerOpen(false);
                  navigate("/notifications");
                }}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1"
              >
                <span>View All</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </button>
            </div>

          </div>
        </div>
      </div>

    </div>
  );
}
