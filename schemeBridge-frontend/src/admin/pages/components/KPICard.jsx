import React from "react";
import { TrendingUp, TrendingDown } from "lucide-react";
import { AreaChart, Area, ResponsiveContainer } from "recharts";

const Sparkline = ({ data, color }) => {
  const sparklineData = data || [40, 45, 42, 50, 48, 55, 52, 60, 58, 65];
  
  return (
    <ResponsiveContainer width="100%" height={40}>
      <AreaChart data={sparklineData}>
        <defs>
          <linearGradient id={`gradient-${color}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor={color} stopOpacity={0.3}/>
            <stop offset="95%" stopColor={color} stopOpacity={0}/>
          </linearGradient>
        </defs>
        <Area
          type="monotone"
          dataKey="value"
          stroke={color}
          strokeWidth={1.5}
          fillOpacity={1}
          fill={`url(#gradient-${color})`}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default function KPICard({ title, value, change, isPositive, subtext, icon: Icon, color, sparklineData }) {
  const trendColor = isPositive ? "#16A34A" : "#DC2626";
  const sparklineColor = isPositive ? "#16A34A" : "#DC2626";

  return (
    <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm hover:shadow-md transition duration-250 flex flex-col justify-between group">
      <div className="flex justify-between items-start">
        <span className="text-[10px] font-black text-slate-400 uppercase tracking-wider">
          {title}
        </span>
        <div className={`p-2 rounded-xl border ${color} group-hover:scale-110 transition duration-200`}>
          <Icon className="h-4 w-4" />
        </div>
      </div>
      
      <div className="mt-4">
        <h3 className="text-2xl font-black text-slate-900 tracking-tight">
          {value}
        </h3>
        
        <div className="flex items-center gap-1.5 mt-1">
          {isPositive ? (
            <TrendingUp className="h-3.5 w-3.5 text-emerald-600" />
          ) : (
            <TrendingDown className="h-3.5 w-3.5 text-rose-600" />
          )}
          <span className={`text-xs font-bold ${isPositive ? "text-emerald-600" : "text-rose-600"}`}>
            {change}
          </span>
          <span className="text-[9px] text-slate-400 font-semibold">{subtext}</span>
        </div>
      </div>

      {/* Sparkline */}
      <div className="mt-3 pt-3 border-t border-slate-100">
        <Sparkline data={sparklineData} color={sparklineColor} />
      </div>
    </div>
  );
}
