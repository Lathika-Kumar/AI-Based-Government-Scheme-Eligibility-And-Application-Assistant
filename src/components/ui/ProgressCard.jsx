/**
 * @file ProgressCard.jsx
 * @description Reusable progress/percentage card with a progress bar.
 * Used for Profile Completion, Document Readiness, Vault Score, etc.
 */

import React from "react";

/**
 * ProgressCard component.
 *
 * @param {object} props
 * @param {string} props.label - Card title/label
 * @param {number} props.value - Progress percentage 0–100
 * @param {string} [props.description] - Supporting description below the bar
 * @param {React.ComponentType} [props.icon] - Lucide icon component
 * @param {"indigo" | "emerald" | "amber" | "rose" | "violet"} [props.color="indigo"] - Color theme
 * @param {boolean} [props.showPercentage=true] - Show numeric percentage
 * @param {string} [props.className=""] - Extra wrapper classes
 */
export default function ProgressCard({
  label,
  value = 0,
  description,
  icon: Icon,
  color = "indigo",
  showPercentage = true,
  className = "",
}) {
  const pct = Math.min(Math.max(Math.round(value), 0), 100);

  const colorConfig = {
    indigo: {
      bar: "bg-indigo-600",
      text: "text-indigo-700",
      bg: "bg-indigo-50",
      track: "bg-indigo-100",
    },
    emerald: {
      bar: "bg-emerald-600",
      text: "text-emerald-700",
      bg: "bg-emerald-50",
      track: "bg-emerald-100",
    },
    amber: {
      bar: "bg-amber-500",
      text: "text-amber-700",
      bg: "bg-amber-50",
      track: "bg-amber-100",
    },
    rose: {
      bar: "bg-rose-600",
      text: "text-rose-700",
      bg: "bg-rose-50",
      track: "bg-rose-100",
    },
    violet: {
      bar: "bg-violet-600",
      text: "text-violet-700",
      bg: "bg-violet-50",
      track: "bg-violet-100",
    },
  };

  const c = colorConfig[color] ?? colorConfig.indigo;

  return (
    <div
      className={`bg-white border border-slate-200 rounded-2xl p-4 shadow-xs ${className}`}
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          {Icon && (
            <div className={`${c.bg} ${c.text} p-1.5 rounded-lg`}>
              <Icon className="h-3.5 w-3.5" />
            </div>
          )}
          <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">
            {label}
          </span>
        </div>
        {showPercentage && (
          <span className={`text-sm font-black ${c.text}`}>{pct}%</span>
        )}
      </div>

      {/* Progress bar */}
      <div
        className={`h-2 w-full rounded-full ${c.track} overflow-hidden`}
        role="progressbar"
        aria-valuenow={pct}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`${label}: ${pct}%`}
      >
        <div
          className={`h-full rounded-full ${c.bar} transition-all duration-500 ease-out`}
          style={{ width: `${pct}%` }}
        />
      </div>

      {/* Description */}
      {description && (
        <p className="text-[10px] text-slate-400 mt-2 leading-snug">{description}</p>
      )}
    </div>
  );
}
