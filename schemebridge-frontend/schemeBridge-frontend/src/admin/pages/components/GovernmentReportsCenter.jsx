import React, { useState, useEffect, useCallback } from "react";
import { useToast } from "@components/ui/ToastNotification";
import {
  FileText,
  FileSpreadsheet,
  Printer,
  Download,
  Calendar,
  Users,
  Settings,
  Filter,
  BarChart3,
  PieChart,
  LineChart,
  TrendingUp,
  ShieldCheck,
  CheckCircle,
  Clock,
  RefreshCw,
  AlertTriangle
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart as RechartsPieChart,
  Pie,
  Cell,
  LineChart as RechartsLineChart,
  Line,
  AreaChart,
  Area
} from "recharts";
import adminService from "@services/adminService";

export default function GovernmentReportsCenter({ applications = [] } = {}) {
  const { showToast } = useToast();
  const [activeReportType, setActiveReportType] = useState("applications");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [analytics, setAnalytics] = useState({
    monthlyTrendData: [],
    schemePerformance: [],
    statusDistribution: {},
  });

  const fetchAnalytics = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await adminService.getAdminAnalytics();
      if (!res.error && res.data) {
        const raw = res.data;
        const trend = (raw.monthlyTrendData || raw.monthlyTimeline || []).map((item) => ({
          month: item.month,
          applications: item.applications ?? item.submitted ?? 0,
          submitted: item.submitted ?? item.applications ?? 0,
          approved: item.approved ?? 0,
          rejected: item.rejected ?? 0,
        }));
        const performance = (raw.schemePerformance || raw.schemeDistribution || []).map((item) => ({
          schemeCode: item.schemeCode,
          applications: item.applications ?? item.applicationsCount ?? item.count ?? 0,
          count: item.count ?? item.applicationsCount ?? item.applications ?? 0,
        }));
        setAnalytics({
          ...raw,
          monthlyTrendData: trend,
          schemePerformance: performance,
          statusDistribution: raw.statusDistribution || raw.statusBreakdown || {},
        });
        setError(null);
      } else {
        setError(res?.message || "Failed to load live reports data from backend.");
      }
    } catch (err) {
      console.error("Failed to load analytics", err);
      setError(err?.message || "Network error loading live reports data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics, applications]);

  const COLORS = ["#19478C", "#FF9933", "#138808", "#4F46E5", "#F59E0B"];

  const handleExportCSV = async (type = activeReportType) => {
    showToast("info", "Report Export Initiated", `Downloading ${type} CSV report from official database.`);
    try {
      const res = await adminService.downloadAdminReport(type);
      if (res && typeof res === "string") {
        const blob = new Blob([res], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute("download", `official_${type}_report_${new Date().toISOString().split('T')[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showToast("success", "Export Complete", "Official report downloaded successfully.");
      } else {
        showToast("error", "Export Failed", "Could not generate report from backend.");
      }
    } catch (err) {
      showToast("error", "Export Error", "Failed to download report.");
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const reportTypes = [
    { id: "applications", label: "Applications Register", icon: <FileSpreadsheet className="h-4 w-4" /> },
    { id: "schemes", label: "Schemes Master Directory", icon: <FileText className="h-4 w-4" /> },
    { id: "grievances", label: "Grievance Redressal Audit", icon: <ShieldCheck className="h-4 w-4" /> }
  ];

  return (
    <div className="space-y-6">
      {/* Control Bar */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 rounded-2xl shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <FileText className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
              Government Analytics & Audit Reports
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 font-semibold mt-1">
              Live MongoDB aggregated reporting and direct CSV exports for government audits
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={fetchAnalytics}
              disabled={loading}
              className="flex items-center gap-2 px-3 py-2 bg-indigo-50 dark:bg-indigo-950/60 hover:bg-indigo-100 dark:hover:bg-indigo-900 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 rounded-xl text-xs font-bold transition"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
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
              Export Applications CSV
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

        {/* Tab Navigation */}
        <div className="flex gap-2 mt-5 border-t border-slate-100 dark:border-slate-800 pt-4 overflow-x-auto">
          {reportTypes.map(r => (
            <button
              key={r.id}
              onClick={() => setActiveReportType(r.id)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
                activeReportType === r.id
                  ? "bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300"
                  : "bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
              }`}
            >
              {r.icon}
              {r.label}
            </button>
          ))}
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-8 rounded-2xl text-center space-y-3">
          <AlertTriangle className="h-8 w-8 text-rose-500 mx-auto" />
          <p className="text-sm font-bold text-slate-800 dark:text-slate-100">Failed to Load Live Reports Analytics</p>
          <p className="text-xs text-rose-500">{error}</p>
          <button
            onClick={fetchAnalytics}
            className="mt-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 transition inline-flex items-center gap-1.5"
          >
            <RefreshCw className="h-3.5 w-3.5" /> Retry
          </button>
        </div>
      )}

      {/* Analytics Visuals */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Monthly Trend */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-800 dark:text-slate-200 flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
              Application Submission & Approval Trends
            </h4>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={analytics.monthlyTrendData || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="month" textAnchor="end" tick={{ fontSize: 10 }} />
                <YAxis tick={{ fontSize: 10 }} />
                <Tooltip />
                <Legend />
                <Area type="monotone" dataKey="applications" stroke="#4f46e5" fill="#e0e7ff" name="Applications" />
                <Area type="monotone" dataKey="approved" stroke="#16a34a" fill="#dcfce7" name="Approved" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Scheme Popularity */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-800 dark:text-slate-200 flex items-center gap-2">
              <BarChart3 className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
              Scheme Application Distribution
            </h4>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={analytics.schemePerformance || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="schemeCode" tick={{ fontSize: 10 }} />
                <YAxis tick={{ fontSize: 10 }} />
                <Tooltip />
                <Bar dataKey="applications" fill="#4f46e5" radius={[4, 4, 0, 0]} name="Applications" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
