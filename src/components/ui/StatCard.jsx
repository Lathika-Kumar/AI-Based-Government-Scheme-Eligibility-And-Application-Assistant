/**
 * @file StatCard.jsx
 * @description Reusable KPI statistic card.
 * Used in Dashboard, Admin Dashboard, and any summary panel.
 */

import React from "react";

/**
 * StatCard component.
 *
 * @param {object} props
 * @param {string} props.label - Short uppercase label (e.g., "Eligible Schemes")
 * @param {string|number} props.value - Primary value to display
 * @param {string} [props.sublabel] - Supporting text below the value
 * @param {React.ComponentType} [props.icon] - Lucide icon component
 * @param {string} [props.iconColor="text-indigo-600"] - Icon color class
 * @param {string} [props.iconBg="bg-indigo-50"] - Icon background class
 * @param {string} [props.accentBorder="border-indigo-100"] - Bottom-accent border color
 * @param {string} [props.className=""] - Extra wrapper classes
 * @param {() => void} [props.onClick] - Optional click handler (makes card interactive)
 */
export default function StatCard({
  label,
  value,
  sublabel,
  icon: Icon,
  iconColor = "text-indigo-600",
  iconBg = "bg-indigo-50",
  accentBorder = "border-indigo-100",
  className = "",
  onClick,
}) {
  const Tag = onClick ? "button" : "div";

  return (
    <Tag
      onClick={onClick}
      className={`bg-white border border-slate-200 rounded-2xl p-4 shadow-xs border-b-2 hover:shadow transition text-left w-full ${accentBorder} ${
        onClick ? "cursor-pointer hover:bg-slate-50" : ""
      } ${className}`}
    >
      <div className="flex items-center justify-between mb-2">
        <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest leading-none">
          {label}
        </span>
        {Icon && (
          <div className={`${iconBg} ${iconColor} p-1.5 rounded-xl border border-white`}>
            <Icon className="h-4 w-4" />
          </div>
        )}
      </div>
      <p className="text-2xl font-black text-slate-900 tracking-tight">
        {value ?? "—"}
      </p>
      {sublabel && (
        <p className="text-[10px] text-slate-400 mt-0.5 leading-none">{sublabel}</p>
      )}
    </Tag>
  );
}
