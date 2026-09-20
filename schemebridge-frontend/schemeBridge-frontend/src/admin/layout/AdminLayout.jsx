import React, { useState, useEffect, useMemo } from "react";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { ROLE_LABELS, ROLE_PERMISSIONS, hasPermission } from "@constants/roles";
import ErrorBoundary from "@components/ErrorBoundary";
import {
  ShieldCheck,
  LogOut,
  Building,
  UserCheck,
  RefreshCw,
  LayoutDashboard,
  FileText,
  FileCheck,
  FileSpreadsheet,
  BookOpen,
  Users,
  BarChart3,
  MessageSquare,
  History,
  Bell,
  Sliders,
  Search,
  Sparkles,
  ChevronRight,
  Globe,
  Clock,
  Menu,
  X,
  ThumbsUp
} from "lucide-react";
import { CONFIG } from "@config/env";
import NotificationDrawer from "@admin/pages/components/NotificationDrawer";
import AdminAIChatModal from "@admin/components/AdminAIChatModal";
import { globalSearch } from "../services/dashboardService";

function OfficialBanner() {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <div className="bg-slate-950 text-slate-400 border-b border-slate-900 shrink-0 select-none">
      <div className="max-w-8xl mx-auto px-4 py-2 flex flex-col sm:flex-row sm:items-center justify-between text-[11px] font-semibold tracking-wide gap-2">
        <div className="flex items-center space-x-2">
          <span>🇮🇳</span>
          <span className="uppercase text-slate-350 font-bold tracking-wider text-[9px] bg-slate-900 px-1.5 py-0.5 rounded border border-slate-800 mr-1">Official Website Simulation</span>
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
        <div className="bg-slate-950 border-t border-slate-900 px-4 py-3 text-[11px] text-slate-500 leading-relaxed">
          <div className="max-w-8xl mx-auto grid sm:grid-cols-2 gap-4">
            <div>
              <p className="font-bold text-slate-300">🔒 schemebridge.gov.in</p>
              <p className="mt-1">
                Official government domain for scheme information and services.
              </p>
            </div>
            <div>
              <p className="font-bold text-slate-300">⚙️ Sandbox Environment Notice</p>
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

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const {
    resetData,
    applications,
    schemes,
    grievances,
    documents,
    usersRegistry,
    unreadCount
  } = useApp();
  const navigate = useNavigate();
  const location = useLocation();

  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [currentTime, setCurrentTime] = useState("");
  const [aiModalOpen, setAiModalOpen] = useState(false);
  const [notificationDrawerOpen, setNotificationDrawerOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Compute dynamic user initials from user name
  const userInitials = useMemo(() => {
    const name = user?.name || "Admin User";
    const parts = name.trim().split(" ").filter(Boolean);
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }, [user?.name]);

  // Digital clock update
  useEffect(() => {
    const updateTime = () => {
      const date = new Date();
      setCurrentTime(date.toLocaleTimeString("en-US", { hour12: false }));
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  // Keyboard shortcut listener for Global Search (Ctrl/⌘ + K, /)
  useEffect(() => {
    const handleKeyDown = (e) => {
      // Ctrl + K or Cmd + K
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setSearchOpen((prev) => !prev);
      }
      // "/" key when focus is not in any input or textarea field
      if (
        e.key === "/" &&
        document.activeElement.tagName !== "INPUT" &&
        document.activeElement.tagName !== "TEXTAREA"
      ) {
        e.preventDefault();
        setSearchOpen(true);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate("/", { replace: true });
  };

  const navItems = [
    { label: "Dashboard", path: "/admin/dashboard", icon: LayoutDashboard },
    { label: "Applications", path: "/admin/applications", icon: FileText },
    { label: "Documents", path: "/admin/documents", icon: FileCheck },
    { label: "Schemes", path: "/admin/schemes", icon: BookOpen },
    { label: "Users", path: "/admin/users", icon: Users },
    { label: "Analytics & Reports", path: "/admin/analytics-reports", icon: BarChart3 },
    { label: "Grievances", path: "/admin/grievances", icon: MessageSquare },
    { label: "Feedback", path: "/admin/feedback", icon: ThumbsUp },
    { label: "Audit", path: "/admin/audit", icon: History },
    { label: "Settings", path: "/admin/settings", icon: Sliders }
  ];

  // Categorized Global Search Results using dashboardService
  const searchResults = useMemo(() => {
    if (!searchQuery.trim()) return null;
    return globalSearch(searchQuery, {
      applications,
      schemes,
      citizens: usersRegistry?.citizens || [],
      documents,
      grievances,
      officers: usersRegistry?.officers || []
    });
  }, [searchQuery, applications, schemes, grievances, documents, usersRegistry]);

  // AI Quick action command executor
  const runAiCommand = (cmd) => {
    setAiActionsOpen(false);
    if (cmd === "pending") {
      navigate("/admin/applications");
    } else if (cmd === "high-risk") {
      navigate("/admin/applications");
    } else if (cmd === "due-today") {
      navigate("/admin/applications");
    } else if (cmd === "analytics" || cmd === "reports") {
      navigate("/admin/analytics-reports");
    } else if (cmd === "export") {
      navigate("/admin/audit");
    }
  };

  return (
    <div className="flex flex-col h-screen bg-slate-50 dark:bg-slate-950 font-sans overflow-hidden transition-colors">
      {/* Top banners - fixed */}
      <div className="flex-shrink-0 z-40">
        <OfficialBanner />
        {/* Security Banner indicator */}
        <div className="bg-red-700 text-white text-[11px] py-1 px-4 font-bold tracking-wider flex items-center justify-center space-x-2 select-none border-b border-red-800">
          <ShieldCheck className="h-3.5 w-3.5 shrink-0" />
          <span>Secure Area</span>
        </div>
      </div>

      {/* Accessible skip link */}
      <a
        href="#main-admin-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:bg-white focus:px-4 focus:py-2 focus:rounded-lg focus:shadow-lg focus:text-government-blue focus:font-bold"
      >
        Skip to main content
      </a>

        {/* Layout Wrapper */}
      <div className="flex-1 flex overflow-hidden relative">

        {/* ── MOBILE BACKDROP OVERLAY ── */}
        {mobileMenuOpen && (
          <div
            onClick={() => setMobileMenuOpen(false)}
            className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs z-40 lg:hidden"
          />
        )}

        {/* ── SIDEBAR ── */}
        <aside
          className={`fixed left-0 top-[5.5rem] h-[calc(100vh-5.5rem)] w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between z-50 transition-transform duration-300 ${
            mobileMenuOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
          }`}
        >
          {/* Logo / Header Area */}
          <div className="flex flex-col">
            <div className="h-16 border-b border-slate-800 flex items-center justify-between px-4">
              <div className="flex items-center space-x-2.5">
                <div className="bg-rose-600 text-white p-2 rounded-xl">
                  <Building className="h-4.5 w-4.5" />
                </div>
                <div>
                  <span className="font-black text-xs block text-slate-100 tracking-wider">SCHEMEBRIDGE</span>
                  <span className="text-[8px] text-slate-500 block uppercase font-bold tracking-widest -mt-0.5">
                    Operations Console
                  </span>
                </div>
              </div>
              <button
                onClick={() => setMobileMenuOpen(false)}
                className="lg:hidden p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Navigation links */}
            <nav className="p-3.5 space-y-1 overflow-y-auto flex-1">
              {navItems.map((item) => {
                const isActive =
                  location.pathname === item.path ||
                  (item.path === "/admin/analytics-reports" &&
                    (location.pathname === "/admin/analytics" || location.pathname === "/admin/reports"));
                return (
                  <button
                    key={item.label}
                    onClick={() => {
                      navigate(item.path);
                      setMobileMenuOpen(false);
                    }}
                    className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-[11px] font-bold transition duration-155 ${
                      isActive
                        ? "bg-indigo-600 text-white shadow-sm shadow-indigo-600/20"
                        : "text-slate-400 hover:bg-slate-800/50 hover:text-slate-200"
                    }`}
                  >
                    <div className="flex items-center space-x-3">
                      <item.icon className={`h-4.5 w-4.5 ${isActive ? "text-white" : "text-slate-400"}`} />
                      <span>{item.label}</span>
                    </div>
                  </button>
                );
              })}
            </nav>
          </div>

          {/* Footer Area with telemetries */}
          <div className="border-t border-slate-800 bg-slate-950/40 select-none flex-shrink-0">
            {/* Telemetries */}
            <div className="px-4 py-4 text-[10px] text-slate-500 font-semibold space-y-2">
              <div className="flex items-center justify-between">
                <span>SLA Target Rate:</span>
                <span className="text-emerald-500">94.2%</span>
              </div>
              <div className="flex items-center justify-between">
                <span>Uptime SLA:</span>
                <span className="text-emerald-500">99.98%</span>
              </div>
              <div className="flex items-center justify-between pt-1 border-t border-slate-800 text-[9px] text-slate-600 font-medium">
                <span>Node IP: 10.120.4.24</span>
                <span>v{CONFIG.PORTAL_VERSION}</span>
              </div>
            </div>
          </div>
        </aside>

        {/* ── CONTENT CONTAINER ── */}
        <div className="flex-grow flex flex-col min-w-0 overflow-y-auto lg:ml-64 ml-0">
          
          {/* ── TOP HEADER ── */}
          <header className="sticky top-0 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 h-16 shrink-0 flex items-center justify-between px-4 sm:px-6 shadow-sm z-20 transition-colors">
            <div className="flex items-center gap-3">
              {/* Mobile Drawer Hamburger Trigger */}
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="lg:hidden p-2 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-300 rounded-xl transition"
                aria-label="Toggle Navigation Menu"
              >
                {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
              </button>

              {/* Global Search Bar Trigger */}
              <div
                onClick={() => setSearchOpen(true)}
                className="hidden md:flex items-center space-x-2.5 bg-slate-50 dark:bg-slate-800/80 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-700 px-3.5 py-2 w-72 rounded-xl text-slate-400 cursor-pointer select-none transition"
              >
                <Search className="h-4 w-4 shrink-0 text-slate-400" />
                <span className="text-xs font-semibold flex-grow text-left text-slate-500 dark:text-slate-300">Search anything...</span>
                <span className="text-[10px] font-bold bg-white dark:bg-slate-700 px-1.5 py-0.5 rounded border border-slate-200 dark:border-slate-600 text-slate-400 dark:text-slate-300">Ctrl K</span>
              </div>
            </div>

            {/* Header Telemetries / Profile Actions */}
            <div className="flex items-center space-x-3">

              {/* Real-time Clock display */}
              <div className="hidden lg:flex items-center space-x-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 px-3 py-1.5 rounded-xl text-slate-500 dark:text-slate-400 font-mono text-xs font-bold select-none">
                <Clock className="h-3.5 w-3.5 text-slate-400" />
                <span>{currentTime || "00:00:00"}</span>
              </div>

              {/* Notification Center Trigger */}
              <button
                onClick={() => setNotificationDrawerOpen(true)}
                className="p-2 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 hover:text-indigo-600 rounded-xl transition relative"
                title="Notifications"
                aria-label="Notifications"
              >
                <Bell className="h-4 w-4" />
                {unreadCount > 0 && (
                  <span className="absolute -top-1 -right-1 bg-rose-500 text-white text-[9px] font-black rounded-full h-4 min-w-4 px-1 flex items-center justify-center border border-white dark:border-slate-900 shadow-sm">
                    {unreadCount > 9 ? "9+" : unreadCount}
                  </span>
                )}
              </button>

              {/* Admin profile capsules with dynamic initials */}
              <div className="flex items-center space-x-2 pl-2 border-l border-slate-200 dark:border-slate-800 select-none">
                <div className="h-8 w-8 rounded-full bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200 dark:bg-rose-950 dark:text-rose-300 dark:border-rose-900 flex items-center justify-center font-bold text-xs">
                  {userInitials}
                </div>
                <div className="hidden sm:flex flex-col text-left items-start gap-0.5">
                  <span className="text-[11px] font-bold text-slate-800 dark:text-slate-200 leading-tight">{user?.name || "Sanjay Kumar"}</span>
                  <span className="inline-flex items-center px-1.5 py-0.5 rounded-full text-[8px] font-black uppercase tracking-wider bg-indigo-50 text-indigo-700 border border-indigo-100 dark:bg-indigo-950 dark:text-indigo-300 dark:border-indigo-900">
                    {ROLE_LABELS[user?.role] || user?.role || "Administrator"}
                  </span>
                </div>
              </div>

              {/* Logout Button */}
              <button
                onClick={handleLogout}
                className="p-2 hover:bg-rose-50 dark:hover:bg-rose-950 border border-slate-200 dark:border-slate-800 hover:border-rose-200 text-slate-500 dark:text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 rounded-xl transition"
                title="Logout"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          </header>

          {/* ── MAIN WORKSPACE CONTENT ── */}
          <main id="main-admin-content" className="flex-grow p-4 sm:p-6 lg:p-8 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 outline-none focus:ring-0 transition-colors">
            <div className="max-w-8xl mx-auto space-y-6">
              <ErrorBoundary>
                <Outlet />
              </ErrorBoundary>
            </div>
          </main>
        </div>
      </div>

      {/* ── GLOBAL SEARCH PALETTE OVERLAY (Ctrl K) ── */}
      {searchOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center p-4 pt-16">
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm" onClick={() => setSearchOpen(false)} />
          <div className="relative bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-2xl max-w-xl w-full max-h-[75vh] flex flex-col overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-150">
            <div className="p-4 border-b border-slate-200 dark:border-slate-700 flex items-center gap-3">
              <Search className="h-5 w-5 text-slate-400 shrink-0" />
              <input
                type="text"
                placeholder="Search by ID, citizen name, or scheme..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="flex-grow text-xs font-semibold focus:outline-none text-slate-700 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500 bg-transparent"
                autoFocus
              />
              <button
                onClick={() => setSearchOpen(false)}
                className="text-[10px] font-bold bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 px-2 py-1 rounded border border-slate-200 dark:border-slate-700 text-slate-500 dark:text-slate-400 transition"
              >
                ESC
              </button>
            </div>

            {/* Results Pane */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {!searchQuery ? (
                <div className="space-y-3">
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Recent Searches</span>
                  <div className="space-y-1">
                    {["Atal Pension Yojana", "Rajesh Patel", "GRV-7401"].map((s, idx) => (
                      <div
                        key={idx}
                        onClick={() => setSearchQuery(s)}
                        className="flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 cursor-pointer transition"
                      >
                        <History className="h-3.5 w-3.5 text-slate-400" />
                        <span>{s}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ) : searchResults ? (
                <div className="space-y-4">
                  {/* Schemes */}
                  {searchResults.schemes?.items?.length > 0 && (
                    <div className="space-y-1.5">
                      <div className="flex justify-between items-center">
                        <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Schemes Database</span>
                        {searchResults.schemes.hasMore && (
                          <button
                            onClick={() => {
                              navigate("/admin/schemes");
                              setSearchOpen(false);
                            }}
                            className="text-[9px] font-bold text-indigo-600 dark:text-indigo-400 hover:underline"
                          >
                            View All ({searchResults.schemes.totalCount})
                          </button>
                        )}
                      </div>
                      {searchResults.schemes.items.map((s) => (
                        <div
                          key={s.id}
                          onClick={() => {
                            navigate("/admin/schemes");
                            setSearchOpen(false);
                          }}
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-indigo-50/50 dark:hover:bg-indigo-950/40 cursor-pointer transition border border-transparent hover:border-indigo-100 dark:hover:border-indigo-900 text-xs font-semibold text-slate-700 dark:text-slate-300"
                        >
                          <div className="flex items-center gap-2">
                            <BookOpen className="h-3.5 w-3.5 text-indigo-600" />
                            <span>{s.name}</span>
                          </div>
                          <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Applications */}
                  {searchResults.applications?.items?.length > 0 && (
                    <div className="space-y-1.5">
                      <div className="flex justify-between items-center">
                        <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Citizen Applications</span>
                        {searchResults.applications.hasMore && (
                          <button
                            onClick={() => {
                              navigate("/admin/applications");
                              setSearchOpen(false);
                            }}
                            className="text-[9px] font-bold text-indigo-600 dark:text-indigo-400 hover:underline"
                          >
                            View All ({searchResults.applications.totalCount})
                          </button>
                        )}
                      </div>
                      {searchResults.applications.items.map((a) => (
                        <div
                          key={a.id}
                          onClick={() => {
                            navigate("/admin/applications");
                            setSearchOpen(false);
                          }}
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-indigo-50/50 dark:hover:bg-indigo-950/40 cursor-pointer transition border border-transparent hover:border-indigo-100 dark:hover:border-indigo-900 text-xs font-semibold text-slate-700 dark:text-slate-300"
                        >
                          <div className="flex items-center gap-2">
                            <FileText className="h-3.5 w-3.5 text-indigo-600" />
                            <span>{a.applicantName} ({a.id})</span>
                          </div>
                          <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Citizens */}
                  {searchResults.citizens?.items?.length > 0 && (
                    <div className="space-y-1.5">
                      <div className="flex justify-between items-center">
                        <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Registered Citizens</span>
                        {searchResults.citizens.hasMore && (
                          <button
                            onClick={() => {
                              navigate("/admin/users");
                              setSearchOpen(false);
                            }}
                            className="text-[9px] font-bold text-indigo-600 dark:text-indigo-400 hover:underline"
                          >
                            View All ({searchResults.citizens.totalCount})
                          </button>
                        )}
                      </div>
                      {searchResults.citizens.items.map((c) => (
                        <div
                          key={c.email}
                          onClick={() => {
                            navigate("/admin/users");
                            setSearchOpen(false);
                          }}
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-indigo-50/50 dark:hover:bg-indigo-950/40 cursor-pointer transition border border-transparent hover:border-indigo-100 dark:hover:border-indigo-900 text-xs font-semibold text-slate-700 dark:text-slate-300"
                        >
                          <div className="flex items-center gap-2">
                            <Users className="h-3.5 w-3.5 text-indigo-600" />
                            <span>{c.name} ({c.email})</span>
                          </div>
                          <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Documents */}
                  {searchResults.documents?.items?.length > 0 && (
                    <div className="space-y-1.5">
                      <div className="flex justify-between items-center">
                        <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Documents Vault</span>
                        {searchResults.documents.hasMore && (
                          <button
                            onClick={() => {
                              navigate("/admin/documents");
                              setSearchOpen(false);
                            }}
                            className="text-[9px] font-bold text-indigo-650 text-indigo-600 hover:underline"
                          >
                            View All ({searchResults.documents.totalCount})
                          </button>
                        )}
                      </div>
                      {searchResults.documents.items.map((d) => (
                        <div
                          key={d.id}
                          onClick={() => {
                            navigate("/admin/documents");
                            setSearchOpen(false);
                          }}
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-indigo-50/50 cursor-pointer transition border border-transparent hover:border-indigo-100 text-xs font-semibold text-slate-700"
                        >
                          <div className="flex items-center gap-2">
                            <FileCheck className="h-3.5 w-3.5 text-indigo-600" />
                            <span>{d.name} ({d.type})</span>
                          </div>
                          <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Officers */}
                  {searchResults.officers?.items?.length > 0 && (
                    <div className="space-y-1.5">
                      <div className="flex justify-between items-center">
                        <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Administrative Officers</span>
                        {searchResults.officers.hasMore && (
                          <button
                            onClick={() => {
                              navigate("/admin/users");
                              setSearchOpen(false);
                            }}
                            className="text-[9px] font-bold text-indigo-650 text-indigo-600 hover:underline"
                          >
                            View All ({searchResults.officers.totalCount})
                          </button>
                        )}
                      </div>
                      {searchResults.officers.items.map((o) => (
                        <div
                          key={o.email}
                          onClick={() => {
                            navigate("/admin/users");
                            setSearchOpen(false);
                          }}
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-indigo-50/50 cursor-pointer transition border border-transparent hover:border-indigo-100 text-xs font-semibold text-slate-700"
                        >
                          <div className="flex items-center gap-2">
                            <UserCheck className="h-3.5 w-3.5 text-indigo-600" />
                            <span>{o.name} ({o.email} - {o.dept})</span>
                          </div>
                          <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Grievances */}
                  {searchResults.grievances?.items?.length > 0 && (
                    <div className="space-y-1.5">
                      <div className="flex justify-between items-center">
                        <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Grievance Desk Tickets</span>
                        {searchResults.grievances.hasMore && (
                          <button
                            onClick={() => {
                              navigate("/admin/grievances");
                              setSearchOpen(false);
                            }}
                            className="text-[9px] font-bold text-indigo-650 text-indigo-600 hover:underline"
                          >
                            View All ({searchResults.grievances.totalCount})
                          </button>
                        )}
                      </div>
                      {searchResults.grievances.items.map((g) => (
                        <div
                          key={g.id}
                          onClick={() => {
                            navigate("/admin/grievances");
                            setSearchOpen(false);
                          }}
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-indigo-50/50 cursor-pointer transition border border-transparent hover:border-indigo-100 text-xs font-semibold text-slate-700"
                        >
                          <div className="flex items-center gap-2">
                            <MessageSquare className="h-3.5 w-3.5 text-indigo-600" />
                            <span>{g.citizenName} - {g.category} ({g.id})</span>
                          </div>
                          <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                        </div>
                      ))}
                    </div>
                  )}

                  {/* No results checks */}
                  {Object.values(searchResults).every((cat) => !cat.items || cat.items.length === 0) && (
                    <div className="p-8 text-center text-slate-400 text-xs font-bold">
                      No matching records found. Try typing another query.
                    </div>
                  )}
                </div>
              ) : null}
            </div>
          </div>
        </div>
      )}

      {/* ── FLOATING AI ASSISTANT BUTTON (Bottom Right) ── */}
      <div className="fixed bottom-6 right-6 z-40">
        <button
          onClick={() => setAiModalOpen(true)}
          className="p-3.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-full shadow-2xl transition duration-200 flex items-center justify-center transform hover:scale-105"
          title="Open SchemeBridge AI Assistant"
          aria-label="Open SchemeBridge AI Assistant"
        >
          <Sparkles className="h-5.5 w-5.5 animate-pulse" />
        </button>
      </div>

      {/* ── CONVERSATIONAL AI ASSISTANT WORKSPACE MODAL ── */}
      <AdminAIChatModal
        isOpen={aiModalOpen}
        onClose={() => setAiModalOpen(false)}
      />

      {/* ── NOTIFICATION DRAWER ── */}
      <NotificationDrawer
        isOpen={notificationDrawerOpen}
        onClose={() => setNotificationDrawerOpen(false)}
      />

    </div>
  );
}
