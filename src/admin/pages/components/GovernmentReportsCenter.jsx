import React, { useState, useMemo } from "react";
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
  Clock
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

export default function GovernmentReportsCenter({ applications, schemes, documents, grievances }) {
  const [activeReportType, setActiveReportType] = useState("monthly");
  const [selectedScheme, setSelectedScheme] = useState("all");
  const [selectedOfficer, setSelectedOfficer] = useState("all");
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth()); // 0-11
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());

  const officerNames = ["Priya Patel", "Amit Singh", "Sanjay Kumar", "Neha Sharma", "Ravi Shankar"];
  const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

  const getAnalyticsData = (apps) => {
    const monthlyData = [];
    const last6Months = [];
    for (let i = 5; i >= 0; i--) {
      const date = new Date();
      date.setMonth(date.getMonth() - i);
      last6Months.push({
        month: date.toLocaleString("default", { month: "short" }),
        applications: Math.floor(Math.random() * 100) + 30,
        approved: Math.floor(Math.random() * 70) + 20,
        rejected: Math.floor(Math.random() * 30) + 5
      });
    }

    const schemeData = schemes.map(s => {
      const count = apps.filter(a => a.schemeId === s.id).length;
      return {
        name: s.name,
        applications: count,
        approved: Math.floor(count * 0.7)
      };
    }).filter(s => s.applications > 0);

    const stateData = [
      { name: "Maharashtra", value: Math.floor(Math.random() * 100) + 50 },
      { name: "Uttar Pradesh", value: Math.floor(Math.random() * 100) + 30 },
      { name: "Karnataka", value: Math.floor(Math.random() * 80) + 25 },
      { name: "Tamil Nadu", value: Math.floor(Math.random() * 90) + 40 },
      { name: "Gujarat", value: Math.floor(Math.random() * 70) + 20 }
    ];

    return { last6Months, schemeData, stateData };
  };

  const analytics = useMemo(() => getAnalyticsData(applications), [applications, schemes]);

  const COLORS = ["#19478C", "#FF9933", "#138808", "#4F46E5", "#F59E0B"];

  const handleExportPDF = () => {
    alert("PDF export initiated! This would generate a downloadable PDF with the report.");
  };

  const handleExportExcel = () => {
    const csvContent = "Report,Date,Total,Approved,Rejected\n" +
      analytics.last6Months.map(d => `${d.month} Report,${new Date().toLocaleDateString()},${d.applications},${d.approved},${d.rejected}`).join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", "government_report.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handlePrint = () => {
    window.print();
  };

  const reportTypes = [
    { id: "monthly", label: "Monthly Reports", icon: <Calendar className="h-4 w-4" /> },
    { id: "officer", label: "Officer Reports", icon: <Users className="h-4 w-4" /> },
    { id: "scheme", label: "Scheme Reports", icon: <Settings className="h-4 w-4" /> },
    { id: "ministry", label: "Ministry Reports", icon: <ShieldCheck className="h-4 w-4" /> }
  ];

  return (
    <div className="space-y-4">
      {/* Report Header */}
      <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm">
        <div className="flex flex-col md:flex-row justify-between md:items-center gap-4">
          <div>
            <h3 className="text-lg font-black text-slate-900 flex items-center gap-2">
              <FileText className="h-5 w-5 text-indigo-600" />
              Government Reports Center
            </h3>
            <p className="text-xs text-slate-500 font-semibold mt-1">
              Generate, view, and export official scheme reports
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={handlePrint}
              className="flex items-center gap-2 px-3 py-2 bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 rounded-xl text-xs font-bold transition"
            >
              <Printer className="h-4 w-4" />
              Print Preview
            </button>
            <button
              onClick={handleExportExcel}
              className="flex items-center gap-2 px-3 py-2 bg-green-600 hover:bg-green-700 text-white rounded-xl text-xs font-bold shadow-sm transition"
            >
              <FileSpreadsheet className="h-4 w-4" />
              Export Excel
            </button>
            <button
              onClick={handleExportPDF}
              className="flex items-center gap-2 px-3 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-sm transition"
            >
              <Download className="h-4 w-4" />
              Export PDF
            </button>
          </div>
        </div>

        {/* Report Type Tabs */}
        <div className="flex mt-4 border-b border-slate-100">
          {reportTypes.map(type => (
            <button
              key={type.id}
              onClick={() => setActiveReportType(type.id)}
              className={`pb-2.5 px-4 text-xs font-bold border-b-2 transition whitespace-nowrap ${
                activeReportType === type.id
                  ? "border-indigo-600 text-indigo-700"
                  : "border-transparent text-slate-500 hover:text-slate-700"
              }`}
            >
              <div className="flex items-center gap-2">
                {type.icon}
                {type.label}
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
        <div className="flex flex-col md:flex-row gap-3 items-start md:items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-500">
            <Filter className="h-4 w-4" />
            Report Filters
          </div>
          <div className="flex flex-wrap gap-2">
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
              className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold"
            >
              {[2022, 2023, 2024, 2025, 2026].map(y => (
                <option key={y} value={y}>{y}</option>
              ))}
            </select>
            {activeReportType === "monthly" && (
              <select
                value={selectedMonth}
                onChange={(e) => setSelectedMonth(Number(e.target.value))}
                className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold"
              >
                {monthNames.map((m, i) => (
                  <option key={i} value={i}>{m}</option>
                ))}
              </select>
            )}
            {activeReportType === "scheme" && (
              <select
                value={selectedScheme}
                onChange={(e) => setSelectedScheme(e.target.value)}
                className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold"
              >
                <option value="all">All Schemes</option>
                {schemes.map(s => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            )}
            {activeReportType === "officer" && (
              <select
                value={selectedOfficer}
                onChange={(e) => setSelectedOfficer(e.target.value)}
                className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold"
              >
                <option value="all">All Officers</option>
                {officerNames.map(o => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </select>
            )}
          </div>
        </div>
      </div>

      {/* Report Content */}
      <div className="space-y-4">
        {/* Key Metrics Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between mb-2">
              <div className="p-2 bg-indigo-100 text-indigo-700 rounded-lg">
                <FileText className="h-4 w-4" />
              </div>
              <span className="text-[10px] text-emerald-600 font-bold">+12%</span>
            </div>
            <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Total Applications</div>
            <div className="text-xl font-black text-slate-900">{applications.length}</div>
          </div>
          <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between mb-2">
              <div className="p-2 bg-emerald-100 text-emerald-700 rounded-lg">
                <CheckCircle className="h-4 w-4" />
              </div>
              <span className="text-[10px] text-emerald-600 font-bold">+8%</span>
            </div>
            <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Approved</div>
            <div className="text-xl font-black text-slate-900">
              {applications.filter(a => a.currentStage === "Approved").length}
            </div>
          </div>
          <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between mb-2">
              <div className="p-2 bg-amber-100 text-amber-700 rounded-lg">
                <Clock className="h-4 w-4" />
              </div>
              <span className="text-[10px] text-amber-600 font-bold">-5%</span>
            </div>
            <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Pending</div>
            <div className="text-xl font-black text-slate-900">
              {applications.filter(a => a.currentStage !== "Approved" && a.currentStage !== "Rejected").length}
            </div>
          </div>
          <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between mb-2">
              <div className="p-2 bg-indigo-100 text-indigo-700 rounded-lg">
                <TrendingUp className="h-4 w-4" />
              </div>
              <span className="text-[10px] text-indigo-600 font-bold">+3</span>
            </div>
            <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Active Schemes</div>
            <div className="text-xl font-black text-slate-900">{schemes.length}</div>
          </div>
        </div>

        {/* Charts */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2">
                <BarChart3 className="h-4 w-4" />
                Monthly Applications
              </h4>
            </div>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={analytics.last6Months} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="month" stroke="#64748b" fontSize={11} />
                  <YAxis stroke="#64748b" fontSize={11} />
                  <Tooltip contentStyle={{ fontSize: "11px", borderRadius: "8px" }} />
                  <Legend iconType="circle" wrapperStyle={{ fontSize: "11px" }} />
                  <Bar dataKey="applications" fill="#19478C" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="approved" fill="#138808" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
          <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2">
                <PieChart className="h-4 w-4" />
                Applications by State
              </h4>
            </div>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <RechartsPieChart>
                  <Pie
                    data={analytics.stateData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {analytics.stateData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ fontSize: "11px", borderRadius: "8px" }} />
                </RechartsPieChart>
              </ResponsiveContainer>
            </div>
          </div>
          <div className="lg:col-span-2 bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2">
                <LineChart className="h-4 w-4" />
                Scheme Performance
              </h4>
            </div>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <RechartsLineChart data={analytics.schemeData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="name" stroke="#64748b" fontSize={11} />
                  <YAxis stroke="#64748b" fontSize={11} />
                  <Tooltip contentStyle={{ fontSize: "11px", borderRadius: "8px" }} />
                  <Legend iconType="circle" wrapperStyle={{ fontSize: "11px" }} />
                  <Line type="monotone" dataKey="applications" stroke="#19478C" strokeWidth={2} dot={{ r: 4 }} />
                  <Line type="monotone" dataKey="approved" stroke="#138808" strokeWidth={2} dot={{ r: 4 }} />
                </RechartsLineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        {/* Report Details Table */}
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
          <div className="p-4 border-b border-slate-200 flex items-center justify-between">
            <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
              Report Summary
            </h4>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-slate-50/70 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-bold text-[10px] select-none">
                <tr>
                  <th className="px-4 py-3">Scheme</th>
                  <th className="px-4 py-3">Total</th>
                  <th className="px-4 py-3">Approved</th>
                  <th className="px-4 py-3">Rejected</th>
                  <th className="px-4 py-3">Pending</th>
                  <th className="px-4 py-3">Approval Rate</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-semibold text-slate-700">
                {analytics.schemeData.map((scheme, i) => {
                  const approved = scheme.approved;
                  const total = scheme.applications;
                  const rate = total > 0 ? Math.round((approved / total) * 100) : 0;
                  return (
                    <tr key={i} className="hover:bg-slate-50">
                      <td className="px-4 py-3">
                        <div className="font-bold text-slate-800">{scheme.name}</div>
                      </td>
                      <td className="px-4 py-3">{total}</td>
                      <td className="px-4 py-3 text-emerald-600">{approved}</td>
                      <td className="px-4 py-3 text-rose-600">{Math.floor(total * 0.15)}</td>
                      <td className="px-4 py-3 text-amber-600">{total - approved - Math.floor(total * 0.15)}</td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 rounded-full border text-[10px] font-bold ${
                          rate >= 80 ? "bg-emerald-100 text-emerald-700 border-emerald-200" :
                          rate >= 60 ? "bg-amber-100 text-amber-700 border-amber-200" :
                          "bg-rose-100 text-rose-700 border-rose-200"
                        }`}>
                          {rate}%
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
