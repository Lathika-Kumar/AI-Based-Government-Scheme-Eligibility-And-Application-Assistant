/**
 * @file PageHeader.jsx
 * @description Reusable page header with badge + H1 + subtitle pattern.
 * Used at the top of every main page (Dashboard, Recommendations, Documents, etc.)
 */

import React from "react";

/**
 * PageHeader component.
 *
 * @param {object} props
 * @param {string} [props.badge] - Small label above the title (e.g., "Citizen Dashboard")
 * @param {string} props.title - Main H1 page title
 * @param {string} [props.subtitle] - Supporting subtitle text
 * @param {React.ReactNode} [props.actions] - Slot for right-side action buttons/controls
 * @param {string} [props.className=""] - Extra wrapper classes
 * @param {"light" | "dark"} [props.theme="light"] - Light (white card) or dark (slate-900)
 */
export default function PageHeader({
  badge,
  title,
  subtitle,
  actions,
  className = "",
  theme = "light",
}) {
  const isDark = theme === "dark";

  return (
    <div
      className={`flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl shadow-xs border ${
        isDark
          ? "bg-slate-900 border-slate-800 text-white"
          : "bg-white border-slate-200/80 text-slate-900"
      } ${className}`}
    >
      {/* Left: badge + title + subtitle */}
      <div>
        {badge && (
          <span
            className={`text-[10px] px-2 py-0.5 rounded font-bold uppercase tracking-wider border ${
              isDark
                ? "bg-white/10 border-white/10 text-indigo-300"
                : "bg-indigo-50 border-indigo-200/50 text-indigo-700"
            }`}
          >
            {badge}
          </span>
        )}
        <h1
          className={`text-xl sm:text-2xl font-black tracking-tight mt-1.5 ${
            isDark ? "text-white" : "text-slate-900"
          }`}
        >
          {title}
        </h1>
        {subtitle && (
          <p
            className={`text-xs mt-0.5 ${
              isDark ? "text-slate-400" : "text-slate-500"
            }`}
          >
            {subtitle}
          </p>
        )}
      </div>

      {/* Right: actions slot */}
      {actions && (
        <div className="flex items-center gap-2 shrink-0 self-start sm:self-auto">
          {actions}
        </div>
      )}
    </div>
  );
}
