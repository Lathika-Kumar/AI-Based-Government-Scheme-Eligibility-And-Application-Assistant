import React from "react";
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
  Line,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar
} from "recharts";
import { getAnalyticsData, refreshMetrics } from "../../services/dashboardService";
import {
  TrendingUp,
  Users,
  FileText,
  CheckCircle,
  Clock,
  Download
} from "lucide-react";

export default function AnalyticsDashboard({ applications, schemes, documents, grievances }) {
  const analyticsData = getAnalyticsData(applications, schemes);
  const metrics = refreshMetrics({ applications, schemes, documents, grievances });
  
  const kpis = [
    {
      title: "Total Applications",
      value: metrics.totalApps,
      change: "+12.4%",
      isPositive: true,
      subtext: "vs last month",
      icon: FileText,
      color: "text-indigo-600 bg-indigo-50 border-indigo-100"
    },
    {
      title: "Approval Rate",
      value: `${Math.round((applications.filter(a => a.currentStage === "Approved").length / (applications.length || 1)) * 100)}%`,
      change: "+3.2%",
      isPositive: true,
      subtext: "vs last month",
      icon: CheckCircle,
      color: "text-emerald-600 bg-emerald-50 border-emerald-100"
    },
    {
      title: "Pending Reviews",
      value: metrics.pendingReviewsCount,
      change: "-5.1%",
      isPositive: true,
      subtext: "vs last week",
      icon: Clock,
      color: "text-amber-600 bg-amber-50 border-amber-100"
    },
    {
      title: "Active Schemes",
      value: metrics.activeSchemesCount,
      change: "+2",
      isPositive: true,
      subtext: "this quarter",
      icon: Users,
      color: "text-sky-600 bg-sky-50 border-sky-100"
    }
  ];

  const schemePerformance = schemes.map(s => {
    const count = applications.filter(a => a.schemeId === s.id).length;
    const approved = applications.filter(a => a.schemeId === s.id && a.currentStage === "Approved").length;
    return {
      name: s.name,
      applications: count,
      approved: approved,
      approvalRate: count > 0 ? Math.round((approved / count) * 100) : 0
    };
  }).filter(s => s.applications > 0);

  const radarData = [
    { subject: 'Efficiency', A: 85, fullMark: 100 },
    { subject: 'SLA Compliance', A: 92, fullMark: 100 },
    { subject: 'User Satisfaction', A: 78, fullMark: 100 },
    { subject: 'Accuracy', A: 95, fullMark: 100 },
    { subject: 'Response Time', A: 88, fullMark: 100 },
    { subject: 'Availability', A: 99, fullMark: 100 },
  ];

  return (
    <div className="space-y-6">
      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {kpis.map((kpi, idx) => {
          const Icon = kpi.icon;
          return (
            <div key={idx} className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm hover:shadow-md transition duration-200">
              <div className="flex items-center justify-between mb-3">
                <div className={`p-3 rounded-xl border ${kpi.color}`}>
                  <Icon className="h-5 w-5" />
                </div>
                <span className={`text-[10px] font-black px-2 py-0.5 rounded-full ${
                  kpi.isPositive 
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' 
                    : 'bg-rose-50 text-rose-700 border border-rose-100'
                }`}>
                  {kpi.change}
                </span>
              </div>
              <h4 className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                {kpi.title}
              </h4>
              <p className="text-2xl font-black text-slate-900 mb-1">
                {kpi.value}
              </p>
              <p className="text-[10px] font-semibold text-slate-500">
                {kpi.subtext}
              </p>
            </div>
          );
        })}
      </div>

      {/* Top Row Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Monthly Trend */}
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-xs font-bold text-slate-800 tracking-tight">
                Monthly Application Trend
              </h4>
              <p className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">
                Last 6 Months
              </p>
            </div>
            <button 
              onClick={() => {
                const data = analyticsData.monthlyTrendData;
                const csv = "Month,Total,Approved,Rejected\n" + 
                  data.map(d => `${d.month},${d.applications},${d.approved},${d.rejected}`).join("\n");
                const blob = new Blob([csv], { type: "text/csv" });
                const url = URL.createObjectURL(blob);
                const a = document.createElement("a");
                a.href = url;
                a.download = "monthly-trend.csv";
                a.click();
              }}
              className="flex items-center gap-1 text-[10px] font-bold text-slate-500 hover:text-slate-700 border border-slate-200 px-2 py-1 rounded-lg"
            >
              <Download className="h-3 w-3" /> Export
            </button>
          </div>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={analyticsData.monthlyTrendData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="month" stroke="#94a3b8" fontSize={10} />
                <YAxis stroke="#94a3b8" fontSize={10} />
                <Tooltip contentStyle={{ fontSize: "11px", borderRadius: "8px" }} />
                <Legend iconType="circle" wrapperStyle={{ fontSize: "10px" }} />
                <Line type="monotone" dataKey="applications" stroke="#4F46E5" strokeWidth={2.5} dot={{ r: 4 }} name="Total" />
                <Line type="monotone" dataKey="approved" stroke="#16A34A" strokeWidth={2.5} dot={{ r: 4 }} name="Approved" />
                <Line type="monotone" dataKey="rejected" stroke="#DC2626" strokeWidth={2.5} dot={{ r: 4 }} name="Rejected" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Approval Rate Trend */}
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-xs font-bold text-slate-800 tracking-tight">
                Approval Rate (%)
              </h4>
              <p className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">
                SLA Compliance
              </p>
            </div>
          </div>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={analyticsData.approvalRateData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorRateFull" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#16A34A" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#16A34A" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="month" stroke="#94a3b8" fontSize={10} />
                <YAxis domain={[70, 100]} stroke="#94a3b8" fontSize={10} />
                <Tooltip contentStyle={{ fontSize: "11px", borderRadius: "8px" }} />
                <Area type="monotone" dataKey="rate" stroke="#16A34A" strokeWidth={2.5} fillOpacity={1} fill="url(#colorRateFull)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Second Row Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Applications by State */}
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-xs font-bold text-slate-800 tracking-tight">
                Applications by State
              </h4>
              <p className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">
                Geographic Distribution
              </p>
            </div>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={analyticsData.applicationsByState} layout="vertical" margin={{ left: -5, right: 10, top: 10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" horizontal={false} />
                <XAxis type="number" stroke="#94a3b8" fontSize={9} />
                <YAxis dataKey="name" type="category" stroke="#94a3b8" fontSize={9} width={70} />
                <Tooltip contentStyle={{ fontSize: "11px", borderRadius: "8px" }} />
                <Bar dataKey="value" fill="#4F46E5" radius={[0, 4, 4, 0]}>
                  {analyticsData.applicationsByState.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={index % 2 === 0 ? "#4F46E5" : "#2563EB"} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Applications by Category */}
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-xs font-bold text-slate-800 tracking-tight">
                Applications by Category
              </h4>
              <p className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">
                Scheme Categories
              </p>
            </div>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={analyticsData.applicationsByCategory}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={70}
                  paddingAngle={4}
                  dataKey="value"
                  label={({name, percent}) => `${name} ${(percent * 100).toFixed(0)}%`}
                  labelLine={false}
                >
                  {analyticsData.applicationsByCategory.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ fontSize: "11px", borderRadius: "8px" }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* System Performance Radar */}
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-xs font-bold text-slate-800 tracking-tight">
                System Performance
              </h4>
              <p className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">
                KPIs Overview
              </p>
            </div>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <RadarChart cx="50%" cy="50%" outerRadius="80%" data={radarData}>
                <PolarGrid stroke="#e2e8f0" />
                <PolarAngleAxis dataKey="subject" tick={{ fontSize: 9 }} />
                <PolarRadiusAxis angle={90} domain={[0, 100]} tick={false} axisLine={false} />
                <Radar
                  name="Performance"
                  dataKey="A"
                  stroke="#4F46E5"
                  fill="#4F46E5"
                  fillOpacity={0.3}
                />
                <Tooltip contentStyle={{ fontSize: "11px", borderRadius: "8px" }} />
              </RadarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Scheme Performance Table */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-200 flex items-center justify-between">
          <div>
            <h4 className="text-xs font-bold text-slate-800 tracking-tight">
              Scheme Performance
            </h4>
            <p className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">
              Approval Rates & Application Counts
            </p>
          </div>
          <button 
            onClick={() => {
              const csv = "Scheme,Applications,Approved,Approval Rate\n" + 
                schemePerformance.map(d => `${d.name},${d.applications},${d.approved},${d.approvalRate}%`).join("\n");
              const blob = new Blob([csv], { type: "text/csv" });
              const url = URL.createObjectURL(blob);
              const a = document.createElement("a");
              a.href = url;
              a.download = "scheme-performance.csv";
              a.click();
            }}
            className="flex items-center gap-1 text-[10px] font-bold text-slate-500 hover:text-slate-700 border border-slate-200 px-2 py-1 rounded-lg"
          >
            <Download className="h-3 w-3" /> Export
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-slate-50/70 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-bold text-[9px]">
              <tr>
                <th className="px-5 py-3">Scheme Name</th>
                <th className="px-5 py-3">Total Apps</th>
                <th className="px-5 py-3">Approved</th>
                <th className="px-5 py-3">Approval Rate</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-semibold text-slate-700">
              {schemePerformance.map((scheme, idx) => (
                <tr key={idx} className="hover:bg-slate-50 transition">
                  <td className="px-5 py-3 font-bold text-slate-800">{scheme.name}</td>
                  <td className="px-5 py-3">{scheme.applications}</td>
                  <td className="px-5 py-3 text-emerald-700">{scheme.approved}</td>
                  <td className="px-5 py-3">
                    <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${
                      scheme.approvalRate >= 80 ? 'bg-emerald-50 text-emerald-700 border-emerald-100' :
                      scheme.approvalRate >= 60 ? 'bg-amber-50 text-amber-700 border-amber-100' :
                      'bg-rose-50 text-rose-700 border-rose-100'
                    }`}>
                      {scheme.approvalRate}%
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
