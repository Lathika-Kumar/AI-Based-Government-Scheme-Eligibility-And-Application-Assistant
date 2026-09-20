import React, { useState, useEffect, useCallback } from "react";
import {
  BarChart,
  Bar,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  AreaChart,
  Area,
  LineChart,
  Line
} from "recharts";
import {
  TrendingUp,
  Users,
  FileText,
  CheckCircle,
  Clock,
  Download,
  RefreshCw,
  AlertTriangle,
  FolderOpen
} from "lucide-react";
import adminService from "@services/adminService";

export default function AnalyticsDashboard() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [metrics, setMetrics] = useState(null);
  const [analytics, setAnalytics] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [metricsRes, analyticsRes] = await Promise.all([
        adminService.getAdminMetrics(),
        adminService.getAdminAnalytics(),
      ]);

      if (metricsRes.error || analyticsRes.error) {
        setError(metricsRes.message || analyticsRes.message || "Failed to load live analytics.");
      }
      if (!metricsRes.error && metricsRes.data) {
        setMetrics(metricsRes.data);
      }
      if (!analyticsRes.error && analyticsRes.data) {
        setAnalytics(analyticsRes.data);
      }
    } catch (err) {
      console.error("Failed to load analytics data", err);
      setError(err?.message || "Failed to load live analytics.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const kpiData = analytics?.kpis || {};
  const totalApps = kpiData.totalApplications ?? metrics?.totalApplications ?? 0;
  const approvedApps = kpiData.approvedApplications ?? metrics?.approvedApplications ?? 0;
  const underReviewApps = kpiData.underReview ?? metrics?.underReviewApplications ?? 0;
  const approvalRateVal = kpiData.approvalRate;
  const approvalRateFormatted = (approvalRateVal !== null && approvalRateVal !== undefined)
    ? `${approvalRateVal}%`
    : "—";
  const activeSchemes = metrics?.activeSchemes ?? 0;

  const kpis = [
    {
      title: "Total Applications",
      value: totalApps,
      subtext: "Total submitted across all schemes",
      icon: FileText,
      color: "text-indigo-600 bg-indigo-50 border-indigo-100"
    },
    {
      title: "Approval Rate",
      value: approvalRateFormatted,
      subtext: "Approved vs Decided (Approved + Rejected)",
      icon: CheckCircle,
      color: "text-emerald-600 bg-emerald-50 border-emerald-100"
    },
    {
      title: "Pending Reviews",
      value: underReviewApps,
      subtext: "Awaiting nodal officer action",
      icon: Clock,
      color: "text-amber-600 bg-amber-50 border-amber-100"
    },
    {
      title: "Active Schemes",
      value: activeSchemes,
      subtext: "Schemes open for citizen application",
      icon: Users,
      color: "text-sky-600 bg-sky-50 border-sky-100"
    }
  ];

  const monthlyTimeline = analytics?.monthlyTimeline || [];
  const schemeDistribution = analytics?.schemeDistribution || [];
  const statusBreakdown = analytics?.statusBreakdown || {};

  const statusPieData = Object.entries(statusBreakdown).map(([name, value]) => ({
    name: name.replace(/_/g, " "),
    value
  }));

  const COLORS = ["#4F46E5", "#16A34A", "#EAB308", "#EF4444", "#8B5CF6", "#06B6D4"];

  const handleExportCSV = async () => {
    try {
      const csv = await adminService.downloadAdminReport("applications");
      if (csv) {
        const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute("download", `analytics_applications_${new Date().toISOString().split("T")[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }
    } catch (e) {
      console.error("Export failed", e);
    }
  };

  if (loading) {
    return (
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-12 rounded-2xl text-center space-y-3">
        <RefreshCw className="h-8 w-8 text-indigo-600 dark:text-indigo-400 animate-spin mx-auto" />
        <p className="text-xs font-bold text-slate-700 dark:text-slate-300">Aggregating Live MongoDB Analytics Pipeline...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-12 rounded-2xl text-center space-y-3">
        <AlertTriangle className="h-8 w-8 text-rose-500 mx-auto" />
        <p className="text-sm font-bold text-slate-800 dark:text-slate-100">Failed to Load Live Analytics</p>
        <p className="text-xs text-rose-500">{error}</p>
        <button
          onClick={fetchData}
          className="mt-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 transition inline-flex items-center gap-1.5"
        >
          <RefreshCw className="h-3.5 w-3.5" /> Retry
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header bar */}
      <div className="flex items-center justify-between bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-2xl shadow-sm">
        <div>
          <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">Operational Business Intelligence</h3>
          <p className="text-[11px] text-slate-400 dark:text-slate-400">Live aggregated statistics from Scheme Service MongoDB</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={fetchData}
            className="p-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 rounded-xl transition"
            title="Refresh Analytics"
          >
            <RefreshCw className="h-4 w-4" />
          </button>
          <button
            onClick={handleExportCSV}
            className="px-3 py-2 bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 border border-indigo-200 dark:border-indigo-800/60 rounded-xl text-xs font-bold transition flex items-center gap-1.5"
          >
            <Download className="h-3.5 w-3.5" />
            Export Live Report
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {kpis.map((kpi, idx) => {
          const Icon = kpi.icon;
          return (
            <div key={idx} className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm hover:shadow-md transition duration-200">
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

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Monthly Trend */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200 tracking-tight">
                Monthly Application Volume & Outcomes
              </h4>
              <p className="text-[10px] text-slate-400 dark:text-slate-400 font-semibold uppercase tracking-wider">
                Last 6 Months History
              </p>
            </div>
          </div>
          <div className="h-64">
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

        {/* Scheme Distribution */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200 tracking-tight">
                Applications by Scheme
              </h4>
              <p className="text-[10px] text-slate-400 dark:text-slate-400 font-semibold uppercase tracking-wider">
                Aggregated by Scheme Code
              </p>
            </div>
          </div>
          <div className="h-64">
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
    </div>
  );
}
