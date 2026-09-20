import React, { useState, useEffect, useCallback, useMemo, useContext } from "react";
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from "recharts";
import {
  TrendingUp,
  BarChart3,
  Users,
  FileText,
  FileSpreadsheet,
  CheckCircle,
  Clock,
  Download,
  Printer,
  RefreshCw,
  AlertTriangle,
  FolderOpen,
  ShieldCheck,
  Search,
  BookOpen,
  MessageSquare,
  Filter
} from "lucide-react";
import adminService from "@services/adminService";
import { useToast } from "@components/ui/ToastNotification";
import { AppContext } from "@context/AppContext";
import { resolveLocalized } from "@utils/localization";

export default function AnalyticsReportsCenter({
  applications = [],
  schemes = [],
  grievances = [],
  documents = [],
  initialMetrics = null,
  initialAnalytics = null,
  initialLoading = null,
  initialError = null,
  initialActiveReportTab = "applications",
  activeLanguage = "en"
} = {}) {
  const appContext = useContext(AppContext);
  const language = appContext?.currentLanguage || appContext?.language || activeLanguage || "en";
  const toast = useToast?.();
  const showToast = (type, title, msg) => {
    if (toast?.showToast) {
      toast.showToast(type, title, msg);
    }
  };

  const [loading, setLoading] = useState(
    initialLoading !== null ? initialLoading : (!initialMetrics && !initialAnalytics)
  );
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(initialError);
  const [metrics, setMetrics] = useState(initialMetrics);
  const [analytics, setAnalytics] = useState(initialAnalytics);

  // Detailed Report Register state
  const [activeReportTab, setActiveReportTab] = useState(initialActiveReportTab);
  const [registerFilter, setRegisterFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const fetchData = useCallback(async (isRefresh = false) => {
    if (isRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);

    try {
      const [metricsRes, analyticsRes] = await Promise.all([
        adminService.getAdminMetrics(),
        adminService.getAdminAnalytics(),
      ]);

      if (metricsRes.error && analyticsRes.error) {
        setError(metricsRes.message || analyticsRes.message || "Reports and analytics are temporarily unavailable.");
      } else {
        if (!metricsRes.error && metricsRes.data) {
          setMetrics(metricsRes.data);
        }
        if (!analyticsRes.error && analyticsRes.data) {
          const raw = analyticsRes.data;
          const trend = (raw.monthlyTimeline || raw.monthlyTrendData || []).map((item) => ({
            month: item.month,
            submitted: item.submitted ?? item.applications ?? 0,
            approved: item.approved ?? 0,
            rejected: item.rejected ?? 0,
          }));
          const distribution = (raw.schemeDistribution || raw.schemePerformance || []).map((item) => ({
            schemeCode: item.schemeCode,
            applicationsCount: item.applicationsCount ?? item.applications ?? item.count ?? 0,
          }));
          setAnalytics({
            ...raw,
            monthlyTimeline: trend,
            schemeDistribution: distribution,
          });
        }
      }
    } catch (err) {
      console.error("Failed to load live analytics and reports data:", err);
      setError("Reports and analytics are temporarily unavailable.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Derived KPI metrics
  const kpiData = analytics?.kpis || {};
  const totalApps = kpiData.totalApplications ?? metrics?.totalApplications ?? applications.length ?? 0;
  const approvedApps = kpiData.approvedApplications ?? metrics?.approvedApplications ?? 0;
  const underReviewApps = kpiData.underReview ?? metrics?.underReviewApplications ?? 0;
  
  let approvalRateFormatted = "—";
  if (kpiData.approvalRate !== null && kpiData.approvalRate !== undefined) {
    approvalRateFormatted = `${kpiData.approvalRate}%`;
  } else if (metrics?.approvalRate !== null && metrics?.approvalRate !== undefined) {
    approvalRateFormatted = `${metrics.approvalRate}%`;
  } else if (totalApps > 0 && approvedApps >= 0) {
    approvalRateFormatted = `${Math.round((approvedApps / totalApps) * 100)}%`;
  }

  const activeSchemes = metrics?.activeSchemes ?? schemes.filter(s => s.status === "ACTIVE" || s.isActive).length ?? schemes.length ?? 0;

  const kpiCards = [
    {
      title: "Total Applications",
      value: totalApps,
      subtext: "Total submitted across all schemes",
      icon: FileText,
      color: "text-indigo-600 bg-indigo-50 border-indigo-100 dark:bg-indigo-950/40 dark:border-indigo-900/40 dark:text-indigo-400"
    },
    {
      title: "Approval Rate",
      value: approvalRateFormatted,
      subtext: "Approved vs Decided submissions",
      icon: CheckCircle,
      color: "text-emerald-600 bg-emerald-50 border-emerald-100 dark:bg-emerald-950/40 dark:border-emerald-900/40 dark:text-emerald-400"
    },
    {
      title: "Pending Reviews",
      value: underReviewApps,
      subtext: "Awaiting nodal officer evaluation",
      icon: Clock,
      color: "text-amber-600 bg-amber-50 border-amber-100 dark:bg-amber-950/40 dark:border-amber-900/40 dark:text-amber-400"
    },
    {
      title: "Active Schemes",
      value: activeSchemes,
      subtext: "Open for citizen applications",
      icon: BookOpen,
      color: "text-sky-600 bg-sky-50 border-sky-100 dark:bg-sky-950/40 dark:border-sky-900/40 dark:text-sky-400"
    }
  ];

  // Chart data
  const monthlyTimeline = analytics?.monthlyTimeline || [];
  const schemeDistribution = analytics?.schemeDistribution || [];

  // Export handlers
  const handleExportCSV = async (type = "applications") => {
    showToast("info", "Report Export Initiated", `Downloading official ${type} CSV report.`);
    try {
      const csv = await adminService.downloadAdminReport(type);
      if (csv && typeof csv === "string") {
        const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute("download", `official_${type}_report_${new Date().toISOString().split("T")[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showToast("success", "Export Complete", `Official ${type} report downloaded successfully.`);
      } else {
        showToast("error", "Export Failed", "Could not generate report from backend.");
      }
    } catch (e) {
      console.error(`Export ${type} failed:`, e);
      showToast("error", "Export Error", `Failed to download ${type} report.`);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  // Filtered registers
  const filteredApplications = useMemo(() => {
    const q = registerFilter.trim().toLowerCase();
    return applications.filter((app) => {
      const idMatch = !q || (app.id || app.applicationNumber || "").toLowerCase().includes(q);
      const schemeTitleStr = resolveLocalized(app.schemeTitle || app.schemeName, language, "");
      const schemeCodeStr = app.schemeCode || "";
      const schemeMatch = !q || schemeCodeStr.toLowerCase().includes(q) || schemeTitleStr.toLowerCase().includes(q);
      const applicantStr = resolveLocalized(app.citizenName || app.applicantName, language, app.userId || "");
      const applicantMatch = !q || applicantStr.toLowerCase().includes(q);
      const status = (app.status || "").toUpperCase();
      const statusOk = statusFilter === "ALL" || status === statusFilter;
      return (idMatch || schemeMatch || applicantMatch) && statusOk;
    });
  }, [applications, registerFilter, statusFilter, language]);

  const filteredSchemes = useMemo(() => {
    const q = registerFilter.trim().toLowerCase();
    return schemes.filter((s) => {
      const codeMatch = !q || (s.code || s.schemeCode || "").toLowerCase().includes(q);
      const titleStr = resolveLocalized(s.title || s.name, language, "");
      const titleMatch = !q || titleStr.toLowerCase().includes(q);
      const deptStr = resolveLocalized(s.ministry || s.department, language, "");
      const deptMatch = !q || deptStr.toLowerCase().includes(q);
      const catStr = resolveLocalized(s.category, language, "");
      const catMatch = !q || catStr.toLowerCase().includes(q);
      return codeMatch || titleMatch || deptMatch || catMatch;
    });
  }, [schemes, registerFilter, language]);

  const filteredGrievances = useMemo(() => {
    const q = registerFilter.trim().toLowerCase();
    return grievances.filter((g) => {
      const idMatch = !q || (g.id || g.ticketId || "").toLowerCase().includes(q);
      const subjectStr = resolveLocalized(g.subject || g.title, language, "");
      const subjectMatch = !q || subjectStr.toLowerCase().includes(q);
      const status = (g.status || "").toUpperCase();
      const statusOk = statusFilter === "ALL" || status === statusFilter;
      return (idMatch || subjectMatch) && statusOk;
    });
  }, [grievances, registerFilter, statusFilter, language]);

  if (loading) {
    return (
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-12 rounded-2xl text-center space-y-3">
        <RefreshCw className="h-8 w-8 text-indigo-600 dark:text-indigo-400 animate-spin mx-auto" />
        <p className="text-xs font-bold text-slate-700 dark:text-slate-300">
          Aggregating Live MongoDB Analytics & Reports Pipeline...
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-12 rounded-2xl text-center space-y-3">
        <AlertTriangle className="h-8 w-8 text-rose-500 mx-auto" />
        <p className="text-sm font-bold text-slate-800 dark:text-slate-100">
          Reports and analytics are temporarily unavailable.
        </p>
        <p className="text-xs text-rose-500">{error}</p>
        <button
          onClick={() => fetchData(false)}
          className="mt-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 transition inline-flex items-center gap-1.5"
        >
          <RefreshCw className="h-3.5 w-3.5" /> Retry
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* ── HEADER & TOOLBAR ── */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 rounded-2xl shadow-sm">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <BarChart3 className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
              Operational Business Intelligence & Audit Reports
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 font-semibold mt-1">
              Live aggregated statistics and official audit reporting from Scheme Service MongoDB
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => fetchData(true)}
              disabled={refreshing}
              className="flex items-center gap-2 px-3 py-2 bg-indigo-50 dark:bg-indigo-950/60 hover:bg-indigo-100 dark:hover:bg-indigo-900 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 rounded-xl text-xs font-bold transition disabled:opacity-50"
              title="Refresh Analytics"
            >
              <RefreshCw className={`h-4 w-4 ${refreshing ? "animate-spin" : ""}`} />
              Refresh Analytics
            </button>
            <button
              onClick={handlePrint}
              className="flex items-center gap-2 px-3 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-xl text-xs font-bold transition"
            >
              <Printer className="h-4 w-4" />
              Print Preview
            </button>
            <button
              onClick={() => handleExportCSV("applications")}
              className="flex items-center gap-2 px-3 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-sm transition"
            >
              <FileSpreadsheet className="h-4 w-4" />
              Export Live Report
            </button>
            <button
              onClick={() => handleExportCSV("schemes")}
              className="flex items-center gap-2 px-3 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-sm transition"
            >
              <Download className="h-4 w-4" />
              Export Schemes CSV
            </button>
            <button
              onClick={() => handleExportCSV("grievances")}
              className="flex items-center gap-2 px-3 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-bold shadow-sm transition"
            >
              <ShieldCheck className="h-4 w-4" />
              Export Grievances CSV
            </button>
          </div>
        </div>
      </div>

      {/* ── KPI SECTION ── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {kpiCards.map((kpi, idx) => {
          const Icon = kpi.icon;
          return (
            <div
              key={idx}
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm hover:shadow-md transition duration-200"
            >
              <div className="flex items-center justify-between mb-3">
                <div className={`p-3 rounded-xl border ${kpi.color}`}>
                  <Icon className="h-5 w-5" />
                </div>
              </div>
              <h4 className="text-[10px] font-bold text-slate-400 dark:text-slate-400 uppercase tracking-wider mb-1">
                {kpi.title}
              </h4>
              <p className="text-2xl font-black text-slate-900 dark:text-slate-100 mb-1">
                {kpi.value}
              </p>
              <p className="text-[10px] font-semibold text-slate-500 dark:text-slate-400">
                {kpi.subtext}
              </p>
            </div>
          );
        })}
      </div>

      {/* ── AUTHORITATIVE CHARTS ROW ── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Chart 1: Application Volume & Outcomes Trend */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200 tracking-tight flex items-center gap-1.5">
                <TrendingUp className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
                Application Volume & Outcomes Trend
              </h4>
              <p className="text-[10px] text-slate-400 dark:text-slate-400 font-semibold uppercase tracking-wider mt-0.5">
                Last 6 Months History
              </p>
            </div>
          </div>
          <div className="h-64" data-testid="application-volume-chart">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={monthlyTimeline}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="month" tick={{ fontSize: 10 }} />
                <YAxis tick={{ fontSize: 10 }} />
                <Tooltip />
                <Legend />
                <Area type="monotone" dataKey="submitted" stroke="#4f46e5" fill="#e0e7ff" name="Submitted" />
                <Area type="monotone" dataKey="approved" stroke="#16a34a" fill="#dcfce7" name="Approved" />
                <Area type="monotone" dataKey="rejected" stroke="#ef4444" fill="#fee2e2" name="Rejected" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Chart 2: Applications by Scheme */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200 tracking-tight flex items-center gap-1.5">
                <BarChart3 className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
                Applications by Scheme
              </h4>
              <p className="text-[10px] text-slate-400 dark:text-slate-400 font-semibold uppercase tracking-wider mt-0.5">
                Aggregated by Scheme Code
              </p>
            </div>
          </div>
          <div className="h-64" data-testid="applications-by-scheme-chart">
            {schemeDistribution.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center text-slate-400 dark:text-slate-500">
                <FolderOpen className="h-8 w-8 mb-2" />
                <p className="text-xs font-semibold">No application submissions logged yet</p>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={schemeDistribution}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="schemeCode" tick={{ fontSize: 10 }} />
                  <YAxis tick={{ fontSize: 10 }} />
                  <Tooltip />
                  <Bar dataKey="applicationsCount" fill="#4f46e5" radius={[4, 4, 0, 0]} name="Applications" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* ── DETAILED REPORT REGISTERS ── */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
        {/* Register Tabs & Filter Header */}
        <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex gap-2 overflow-x-auto">
            <button
              onClick={() => {
                setActiveReportTab("applications");
                setRegisterFilter("");
              }}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
                activeReportTab === "applications"
                  ? "bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300"
                  : "bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
              }`}
            >
              <FileSpreadsheet className="h-4 w-4" />
              Applications Register
            </button>
            <button
              onClick={() => {
                setActiveReportTab("schemes");
                setRegisterFilter("");
              }}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
                activeReportTab === "schemes"
                  ? "bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300"
                  : "bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
              }`}
            >
              <BookOpen className="h-4 w-4" />
              Schemes Master Directory
            </button>
            <button
              onClick={() => {
                setActiveReportTab("grievances");
                setRegisterFilter("");
              }}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
                activeReportTab === "grievances"
                  ? "bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300"
                  : "bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
              }`}
            >
              <ShieldCheck className="h-4 w-4" />
              Grievance Redressal Audit
            </button>
          </div>

          {/* Quick Search & Export for Active Tab */}
          <div className="flex items-center gap-2.5">
            <div className="relative">
              <Search className="h-3.5 w-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={registerFilter}
                onChange={(e) => setRegisterFilter(e.target.value)}
                placeholder="Filter register..."
                className="pl-8 pr-3 py-1.5 text-xs bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500 text-slate-800 dark:text-slate-200 w-48 sm:w-56"
              />
            </div>
            <button
              onClick={() => handleExportCSV(activeReportTab)}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-xl text-xs font-bold transition"
              title={`Download ${activeReportTab} CSV`}
            >
              <Download className="h-3.5 w-3.5" />
              Export
            </button>
          </div>
        </div>

        {/* ── REGISTER TABLE VIEW ── */}
        <div className="overflow-x-auto">
          {activeReportTab === "applications" && (
            <table className="w-full text-left text-xs text-slate-600 dark:text-slate-300">
              <thead className="bg-slate-50 dark:bg-slate-800/60 border-b border-slate-100 dark:border-slate-800 text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                <tr>
                  <th className="py-3 px-4">Application ID</th>
                  <th className="py-3 px-4">Scheme</th>
                  <th className="py-3 px-4">Applicant</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {filteredApplications.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-slate-400 font-semibold">
                      No applications found matching criteria
                    </td>
                  </tr>
                ) : (
                  filteredApplications.slice(0, 50).map((app, idx) => (
                    <tr key={app.id || idx} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition">
                      <td className="py-3 px-4 font-mono font-bold text-indigo-600 dark:text-indigo-400">
                        {app.applicationNumber || app.id}
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-800 dark:text-slate-200">
                        {app.schemeCode ? `[${app.schemeCode}] ` : ""}{resolveLocalized(app.schemeTitle || app.schemeName, language, "—")}
                      </td>
                      <td className="py-3 px-4 font-medium">
                        {resolveLocalized(app.citizenName || app.applicantName, language, app.userId || "Citizen")}
                      </td>
                      <td className="py-3 px-4">
                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          (app.status === "APPROVED")
                            ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300"
                            : (app.status === "REJECTED")
                            ? "bg-rose-50 text-rose-700 dark:bg-rose-950/50 dark:text-rose-300"
                            : "bg-amber-50 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300"
                        }`}>
                          {app.status || "SUBMITTED"}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-400 text-[11px]">
                        {app.createdAt ? new Date(app.createdAt).toLocaleDateString() : "—"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}

          {activeReportTab === "schemes" && (
            <table className="w-full text-left text-xs text-slate-600 dark:text-slate-300">
              <thead className="bg-slate-50 dark:bg-slate-800/60 border-b border-slate-100 dark:border-slate-800 text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                <tr>
                  <th className="py-3 px-4">Scheme Code</th>
                  <th className="py-3 px-4">Title</th>
                  <th className="py-3 px-4">Ministry / Department</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4">Active Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {filteredSchemes.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-slate-400 font-semibold">
                      No schemes found matching criteria
                    </td>
                  </tr>
                ) : (
                  filteredSchemes.slice(0, 50).map((scheme, idx) => (
                    <tr key={scheme.id || scheme.code || idx} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition">
                      <td className="py-3 px-4 font-mono font-bold text-indigo-600 dark:text-indigo-400">
                        {scheme.code || scheme.schemeCode}
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-800 dark:text-slate-200">
                        {resolveLocalized(scheme.title || scheme.name, language, scheme.code || scheme.schemeCode || "Untitled Scheme")}
                      </td>
                      <td className="py-3 px-4 font-medium text-slate-500 dark:text-slate-400">
                        {resolveLocalized(scheme.ministry || scheme.department, language, "Central / State")}
                      </td>
                      <td className="py-3 px-4">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                          {resolveLocalized(scheme.category, language, "General Welfare")}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          (scheme.status === "ACTIVE" || scheme.isActive !== false)
                            ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300"
                            : "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300"
                        }`}>
                          {scheme.status || (scheme.isActive !== false ? "ACTIVE" : "INACTIVE")}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}

          {activeReportTab === "grievances" && (
            <table className="w-full text-left text-xs text-slate-600 dark:text-slate-300">
              <thead className="bg-slate-50 dark:bg-slate-800/60 border-b border-slate-100 dark:border-slate-800 text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                <tr>
                  <th className="py-3 px-4">Ticket ID</th>
                  <th className="py-3 px-4">Subject</th>
                  <th className="py-3 px-4">Priority</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Filed Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {filteredGrievances.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-slate-400 font-semibold">
                      No grievances logged in audit register
                    </td>
                  </tr>
                ) : (
                  filteredGrievances.slice(0, 50).map((g, idx) => (
                    <tr key={g.id || idx} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition">
                      <td className="py-3 px-4 font-mono font-bold text-indigo-600 dark:text-indigo-400">
                        {g.ticketId || g.id}
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-800 dark:text-slate-200">
                        {resolveLocalized(g.subject || g.title, language, "—")}
                      </td>
                      <td className="py-3 px-4">
                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          (g.priority === "HIGH" || g.priority === "CRITICAL")
                            ? "bg-rose-50 text-rose-700 dark:bg-rose-950/50 dark:text-rose-300"
                            : "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300"
                        }`}>
                          {g.priority || "NORMAL"}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          (g.status === "RESOLVED" || g.status === "CLOSED")
                            ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300"
                            : "bg-amber-50 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300"
                        }`}>
                          {g.status || "OPEN"}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-400 text-[11px]">
                        {g.createdAt ? new Date(g.createdAt).toLocaleDateString() : "—"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
