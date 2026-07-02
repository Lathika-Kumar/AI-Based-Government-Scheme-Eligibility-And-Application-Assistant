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
  Line
} from "recharts";

const applicationsByState = [
  { name: "Uttar Pradesh", value: 310 },
  { name: "Gujarat", value: 240 },
  { name: "Maharashtra", value: 198 },
  { name: "Tamil Nadu", value: 167 },
  { name: "Karnataka", value: 145 }
];

const applicationsByCategory = [
  { name: "Agriculture", value: 450, color: "#4F46E5" },
  { name: "Housing", value: 300, color: "#2563EB" },
  { name: "Social Welfare", value: 250, color: "#16A34A" },
  { name: "Education", value: 380, color: "#D97706" }
];

const approvalRateData = [
  { month: "Jan", rate: 82 },
  { month: "Feb", rate: 85 },
  { month: "Mar", rate: 88 },
  { month: "Apr", rate: 87 },
  { month: "May", rate: 90 },
  { month: "Jun", rate: 91.5 }
];

const monthlyTrendData = [
  { month: "Jan", applications: 420, approved: 345, rejected: 75 },
  { month: "Feb", applications: 480, approved: 408, rejected: 72 },
  { month: "Mar", applications: 520, approved: 458, rejected: 62 },
  { month: "Apr", applications: 490, approved: 426, rejected: 64 },
  { month: "May", applications: 550, approved: 495, rejected: 55 },
  { month: "Jun", applications: 580, approved: 531, rejected: 49 }
];

export default function AnalyticsPreview() {
  return (
    <div className="space-y-3.5">
      <h3 className="text-xs font-black text-slate-400 uppercase tracking-wider select-none">
        Analytics Preview
      </h3>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Applications by State */}
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm flex flex-col justify-between h-80">
          <div className="flex justify-between items-center mb-3">
            <h4 className="text-xs font-bold text-slate-800 tracking-tight">
              Applications by State
            </h4>
            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">
              Geographic
            </span>
          </div>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={applicationsByState} layout="vertical" margin={{ left: -5, right: 10, top: 10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" horizontal={false} />
                <XAxis type="number" stroke="#94a3b8" fontSize={9} fontStyle="italic" />
                <YAxis dataKey="name" type="category" stroke="#94a3b8" fontSize={9} width={70} />
                <Tooltip contentStyle={{ fontSize: "10px", borderRadius: "8px" }} />
                <Bar dataKey="value" fill="#4F46E5" radius={[0, 4, 4, 0]}>
                  {applicationsByState.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={index % 2 === 0 ? "#4F46E5" : "#2563EB"} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Applications by Category */}
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm flex flex-col justify-between h-80">
          <div className="flex justify-between items-center mb-3">
            <h4 className="text-xs font-bold text-slate-800 tracking-tight">
              Applications by Category
            </h4>
            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">
              Functional
            </span>
          </div>
          <div className="h-56 flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={applicationsByCategory}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={70}
                  paddingAngle={4}
                  dataKey="value"
                >
                  {applicationsByCategory.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ fontSize: "10px", borderRadius: "8px" }} />
                <Legend iconType="circle" wrapperStyle={{ fontSize: "9px", marginTop: "5px" }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Approval Rate */}
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm flex flex-col justify-between h-80">
          <div className="flex justify-between items-center mb-3">
            <h4 className="text-xs font-bold text-slate-800 tracking-tight">
              Approval Rate (%)
            </h4>
            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">
              SLA Ratio
            </span>
          </div>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={approvalRateData} margin={{ top: 10, right: 10, left: -25, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorRate" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#16A34A" stopOpacity={0.25}/>
                    <stop offset="95%" stopColor="#16A34A" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="month" stroke="#94a3b8" fontSize={9} />
                <YAxis stroke="#94a3b8" fontSize={9} domain={[70, 100]} />
                <Tooltip contentStyle={{ fontSize: "10px", borderRadius: "8px" }} />
                <Area type="monotone" dataKey="rate" stroke="#16A34A" strokeWidth={2.5} fillOpacity={1} fill="url(#colorRate)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Monthly Trend */}
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm flex flex-col justify-between h-80">
          <div className="flex justify-between items-center mb-3">
            <h4 className="text-xs font-bold text-slate-800 tracking-tight">
              Monthly Trend
            </h4>
            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">
              6 Months
            </span>
          </div>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={monthlyTrendData} margin={{ top: 10, right: 10, left: -25, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="month" stroke="#94a3b8" fontSize={9} />
                <YAxis stroke="#94a3b8" fontSize={9} />
                <Tooltip contentStyle={{ fontSize: "10px", borderRadius: "8px" }} />
                <Legend iconType="circle" wrapperStyle={{ fontSize: "9px", marginTop: "5px" }} />
                <Line type="monotone" dataKey="applications" stroke="#4F46E5" strokeWidth={2} dot={{ r: 3 }} name="Total" />
                <Line type="monotone" dataKey="approved" stroke="#16A34A" strokeWidth={2} dot={{ r: 3 }} name="Approved" />
                <Line type="monotone" dataKey="rejected" stroke="#DC2626" strokeWidth={2} dot={{ r: 3 }} name="Rejected" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
