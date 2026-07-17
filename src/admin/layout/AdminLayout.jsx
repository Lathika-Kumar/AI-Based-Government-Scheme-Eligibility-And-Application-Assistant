import React, { useState, useEffect, useMemo } from "react";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { ROLE_LABELS, ROLE_PERMISSIONS, hasPermission } from "@constants/roles";
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
  Clock
} from "lucide-react";
import { CONFIG } from "@config/env";
import NotificationDrawer from "@admin/pages/components/NotificationDrawer";
import { globalSearch } from "../services/dashboardService";

function OfficialBanner() {
  const [isOpen, setIsOpen] = useState(false);
  const { t } = useApp();
  return (
    <div className="bg-slate-950 text-slate-400 border-b border-slate-900 shrink-0 select-none">
      <div className="max-w-8xl mx-auto px-4 py-2 flex flex-col sm:flex-row sm:items-center justify-between text-[11px] font-semibold tracking-wide gap-2">
        <div className="flex items-center space-x-2">
          <span>🇮🇳</span>
          <span className="uppercase text-slate-350 font-bold tracking-wider text-[9px] bg-slate-900 px-1.5 py-0.5 rounded border border-slate-800 mr-1">{t("official_sim_title")}</span>
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
        <div className="bg-slate-950 border-t border-slate-900 px-4 py-3 text-[11px] text-slate-500 leading-relaxed">
          <div className="max-w-8xl mx-auto grid sm:grid-cols-2 gap-4">
            <div>
              <p className="font-bold text-slate-300">🔒 {t("official_domain_val")}</p>
              <p className="mt-1">
                {t("official_domain_desc")}
              </p>
            </div>
            <div>
              <p className="font-bold text-slate-300">⚙️ {t("official_sandbox_title")}</p>
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

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const {
    resetData,
    language,
    changeLanguage,
    t,
    applications,
    schemes,
    grievances,
    documents,
    usersRegistry
  } = useApp();
  const navigate = useNavigate();
  const location = useLocation();


  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [currentTime, setCurrentTime] = useState("");
  const [aiActionsOpen, setAiActionsOpen] = useState(false);
  const [notificationDrawerOpen, setNotificationDrawerOpen] = useState(false);

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

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const navItems = [
    { label: "Dashboard", path: "/admin/dashboard", icon: LayoutDashboard },
    { label: "Applications", path: "/admin/applications", icon: FileText },
    { label: "Document Verification", path: "/admin/documents", icon: FileCheck },
    { label: "Scheme Management", path: "/admin/schemes", icon: BookOpen },
    { label: "User Management", path: "/admin/users", icon: Users },
    { label: "Analytics", path: "/admin/analytics", icon: BarChart3 },
    { label: "Reports", path: "/admin/reports", icon: FileSpreadsheet },
    { label: "Grievances", path: "/admin/grievances", icon: MessageSquare },
    { label: "Audit Logs", path: "/admin/audit", icon: History },
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
    } else if (cmd === "analytics") {
      navigate("/admin/analytics");
    } else if (cmd === "export") {
      navigate("/admin/audit");
    }
  };

  return (
    <div className="flex flex-col h-screen bg-slate-50 font-sans overflow-hidden">
      {/* Top banners - fixed */}
      <div className="flex-shrink-0 z-40">
        <OfficialBanner />
        {/* Security Banner indicator */}
        <div className="bg-red-700 text-white text-[11px] py-1 px-4 font-bold tracking-wider flex items-center justify-center space-x-2 select-none border-b border-red-800">
          <ShieldCheck className="h-3.5 w-3.5 shrink-0" />
          <span>{t("admin_secure_area")} (Nodal Evaluation Console)</span>
        </div>
      </div>

      {/* Accessible skip link */}
      <a
        href="#main-admin-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-3 focus:left-3 focus:bg-indigo-650 focus:text-white focus:px-4 focus:py-2 focus:rounded-xl focus:z-50 font-bold shadow-lg transition"
      >
        {t("nav_skip_content")}
      </a>

      {/* Layout Wrapper */}
      <div className="flex-1 flex overflow-hidden relative">
        
        {/* ── SIDEBAR ── */}
        <aside
          className="fixed left-0 top-[5.5rem] h-[calc(100vh-5.5rem)] w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between z-30"
        >
          {/* Logo / Header Area */}
          <div className="flex flex-col">
            <div className="h-16 border-b border-slate-800 flex items-center px-4">
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
            </div>

            {/* Navigation links */}
            <nav className="p-3.5 space-y-1 overflow-y-auto flex-1">
              {navItems.map((item) => {
                const isActive = location.pathname === item.path;
                return (
                  <button
                    key={item.label}
                    onClick={() => {
                      navigate(item.path);
                    }}
                    className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-[11px] font-bold transition duration-155 ${
                      isActive
                        ? "bg-indigo-650 bg-indigo-600 text-white shadow-sm shadow-indigo-600/20"
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

          {/* Footer Area with sign out and telemetries */}
          <div className="border-t border-slate-800 bg-slate-950/40 select-none flex-shrink-0">
            {/* Sign Out Button */}
            <div className="p-4">
              <button
                onClick={handleLogout}
                className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-medium text-rose-400 hover:bg-rose-500/10 transition duration-150"
              >
                <LogOut className="h-4 w-4 shrink-0" />
                <span>Sign Out</span>
              </button>
            </div>
            {/* Telemetries */}
            <div className="px-4 pb-4 text-[10px] text-slate-500 font-semibold space-y-2 border-t border-slate-800 pt-4">
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
        <div className="flex-grow flex flex-col min-w-0 overflow-y-auto ml-64">
          
          {/* ── TOP HEADER ── */}
          <header className="sticky top-0 bg-white border-b border-slate-200 h-16 shrink-0 flex items-center justify-between px-4 sm:px-6 shadow-sm">
            <div className="flex items-center gap-3">

              {/* Global Search Bar Trigger */}
              <div
                onClick={() => setSearchOpen(true)}
                className="hidden md:flex items-center space-x-2.5 bg-slate-50 hover:bg-slate-100 border border-slate-200 px-3.5 py-2 w-72 rounded-xl text-slate-400 cursor-pointer select-none transition"
              >
                <Search className="h-4 w-4 shrink-0 text-slate-400" />
                <span className="text-xs font-semibold flex-grow text-left">Search anything...</span>
                <span className="text-[10px] font-bold bg-white px-1.5 py-0.5 rounded border border-slate-200 text-slate-400">Ctrl K</span>
              </div>
            </div>

            {/* Header Telemetries / Profile Actions */}
            <div className="flex items-center space-x-3">
              
              {/* Real-time Clock display */}
              <div className="hidden lg:flex items-center space-x-1.5 bg-slate-50 border border-slate-200 px-3 py-1.5 rounded-xl text-slate-500 font-mono text-xs font-bold select-none">
                <Clock className="h-3.5 w-3.5 text-slate-400" />
                <span>{currentTime || "00:00:00"}</span>
              </div>

              {/* Language Switcher */}
              <div className="relative inline-flex items-center">
                <Globe className="absolute left-2.5 h-3.5 w-3.5 text-slate-400 pointer-events-none" />
                <select
                  value={language}
                  onChange={(e) => changeLanguage(e.target.value)}
                  className="pl-7 pr-8 py-1.5 bg-slate-50 border border-slate-200 hover:bg-slate-100 text-slate-700 rounded-xl text-xs font-bold shadow-sm transition focus:outline-none cursor-pointer appearance-none"
                  aria-label="Select Language"
                >
                  <option value="en">EN</option>
                  <option value="hi">HI</option>
                  <option value="ta">TA</option>
                  <option value="te">TE</option>
                  <option value="kn">KN</option>
                </select>
                <span className="absolute right-2 text-[8px] text-slate-500 pointer-events-none select-none">&#9662;</span>
              </div>

              {/* Telemetry Reset trigger */}
              <button
                onClick={resetData}
                className="p-2 hover:bg-slate-100 border border-slate-200 text-slate-500 hover:text-slate-700 rounded-xl transition"
                title="Reset all simulation records"
              >
                <RefreshCw className="h-4 w-4" />
              </button>

              {/* Notification Center Trigger */}
              <button
                onClick={() => setNotificationDrawerOpen(true)}
                className="p-2 hover:bg-slate-100 border border-slate-200 text-slate-500 hover:text-indigo-600 rounded-xl transition relative"
                title="Notification Logs"
              >
                <Bell className="h-4 w-4" />
                <span className="absolute top-1 right-1 h-2 w-2 bg-indigo-600 rounded-full"></span>
              </button>

              {/* Admin profile capsules */}
              <div className="flex items-center space-x-2 pl-2 border-l border-slate-200 select-none">
                <div className="h-8 w-8 rounded-full bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200 flex items-center justify-center font-bold text-xs">
                  SK
                </div>
                <div className="hidden sm:flex flex-col text-left items-start gap-0.5">
                  <span className="text-[11px] font-bold text-slate-800 leading-tight">{user?.name || "Sanjay Kumar"}</span>
                  <span className="inline-flex items-center px-1.5 py-0.5 rounded-full text-[8px] font-black uppercase tracking-wider bg-indigo-50 text-indigo-700 border border-indigo-100">
                    {user?.role || "Super Admin"}
                  </span>
                </div>
              </div>

              {/* Logout Button */}
              <button
                onClick={handleLogout}
                className="p-2 hover:bg-rose-50 border border-slate-200 hover:border-rose-200 text-slate-500 hover:text-rose-600 rounded-xl transition"
                title="Logout Console Session"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          </header>

          {/* ── MAIN WORKSPACE CONTENT ── */}
          <main id="main-admin-content" className="flex-grow p-4 sm:p-6 lg:p-8 bg-slate-50 outline-none focus:ring-0">
            <div className="max-w-8xl mx-auto space-y-6">
              <Outlet />
            </div>
          </main>
        </div>
      </div>

      {/* ── GLOBAL SEARCH PALETTE OVERLAY (Ctrl K) ── */}
      {searchOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center p-4 pt-16">
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm" onClick={() => setSearchOpen(false)} />
          <div className="relative bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-xl w-full max-h-[75vh] flex flex-col overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-150">
            <div className="p-4 border-b border-slate-200 flex items-center gap-3">
              <Search className="h-5 w-5 text-slate-400 shrink-0" />
              <input
                type="text"
                placeholder="Search across schemes, applications, audit logs..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="flex-grow text-xs font-semibold focus:outline-none text-slate-700 placeholder-slate-400"
                autoFocus
              />
              <button
                onClick={() => setSearchOpen(false)}
                className="text-[10px] font-bold bg-slate-100 hover:bg-slate-200 px-2 py-1 rounded border border-slate-200 text-slate-500 transition"
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
                        className="flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-50 cursor-pointer transition"
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
                            className="text-[9px] font-bold text-indigo-650 text-indigo-600 hover:underline"
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
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-indigo-50/50 cursor-pointer transition border border-transparent hover:border-indigo-100 text-xs font-semibold text-slate-700"
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
                            className="text-[9px] font-bold text-indigo-650 text-indigo-600 hover:underline"
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
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-indigo-50/50 cursor-pointer transition border border-transparent hover:border-indigo-100 text-xs font-semibold text-slate-700"
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
                            className="text-[9px] font-bold text-indigo-650 text-indigo-600 hover:underline"
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
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-indigo-50/50 cursor-pointer transition border border-transparent hover:border-indigo-100 text-xs font-semibold text-slate-700"
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
                        <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Grievance desk Tickets</span>
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

      {/* ── FLOATING AI ACTIONS PANEL (Bottom Right) ── */}
      <div className="fixed bottom-6 right-6 z-40 flex flex-col items-end space-y-3">
        {aiActionsOpen && (
          <div className="bg-gradient-to-b from-slate-900 to-indigo-950 text-white rounded-2xl shadow-2xl border border-indigo-900/60 p-4 w-72 space-y-3 animate-in slide-in-from-bottom-5 duration-200">
            <div className="flex items-center gap-2 border-b border-indigo-900/65 pb-2">
              <Sparkles className="h-4.5 w-4.5 text-indigo-400 animate-pulse" />
              <h4 className="text-xs font-black text-indigo-200 tracking-wider">AI Quick Actions</h4>
            </div>
            <div className="space-y-1.5 text-xs font-semibold">
              <button
                onClick={() => runAiCommand("pending")}
                className="w-full flex items-center justify-between px-3 py-2 rounded-xl bg-slate-800/40 hover:bg-slate-800 text-left text-slate-200 border border-slate-700/20 hover:border-indigo-500/20 transition"
              >
                <span>Review pending applications</span>
                <ChevronRight className="h-3.5 w-3.5 text-slate-500" />
              </button>
              <button
                onClick={() => runAiCommand("high-risk")}
                className="w-full flex items-center justify-between px-3 py-2 rounded-xl bg-slate-800/40 hover:bg-slate-800 text-left text-slate-200 border border-slate-700/20 hover:border-indigo-500/20 transition"
              >
                <span>Show high-risk applications</span>
                <ChevronRight className="h-3.5 w-3.5 text-slate-500" />
              </button>
              <button
                onClick={() => runAiCommand("due-today")}
                className="w-full flex items-center justify-between px-3 py-2 rounded-xl bg-slate-800/40 hover:bg-slate-800 text-left text-slate-200 border border-slate-700/20 hover:border-indigo-500/20 transition"
              >
                <span>Applications due today</span>
                <ChevronRight className="h-3.5 w-3.5 text-slate-500" />
              </button>
              <button
                onClick={() => runAiCommand("analytics")}
                className="w-full flex items-center justify-between px-3 py-2 rounded-xl bg-slate-800/40 hover:bg-slate-800 text-left text-slate-200 border border-slate-700/20 hover:border-indigo-500/20 transition"
              >
                <span>Generate analytics dashboard</span>
                <ChevronRight className="h-3.5 w-3.5 text-slate-500" />
              </button>
              <button
                onClick={() => runAiCommand("export")}
                className="w-full flex items-center justify-between px-3 py-2 rounded-xl bg-slate-800/40 hover:bg-slate-800 text-left text-slate-200 border border-slate-700/20 hover:border-indigo-500/20 transition"
              >
                <span>Export logs & telemetry reports</span>
                <ChevronRight className="h-3.5 w-3.5 text-slate-500" />
              </button>
            </div>
          </div>
        )}
        <button
          onClick={() => setAiActionsOpen(!aiActionsOpen)}
          className="p-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-full shadow-2xl transition duration-200 flex items-center justify-center transform hover:scale-105"
          title="AI Assistant Options"
        >
          {aiActionsOpen ? <X className="h-5.5 w-5.5" /> : <Sparkles className="h-5.5 w-5.5 animate-pulse" />}
        </button>
      </div>

      {/* ── NOTIFICATION DRAWER ── */}
      <NotificationDrawer
        isOpen={notificationDrawerOpen}
        onClose={() => setNotificationDrawerOpen(false)}
      />

    </div>
  );
}
