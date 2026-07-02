/**
 * @file StatusBadge.jsx
 * @description Reusable status badge component for applications, documents, notifications.
 */

import React from "react";

/**
 * Color palette for arbitrary status strings.
 * Extend this map as new statuses are added from the backend.
 */
const STATUS_STYLES = {
  // Application statuses
  Saved: "bg-slate-100 text-slate-700 border-slate-200",
  "Preparing Documents": "bg-amber-50 text-amber-800 border-amber-200",
  "Ready to Apply": "bg-blue-50 text-blue-800 border-blue-200",
  Submitted: "bg-indigo-50 text-indigo-800 border-indigo-200",
  "Under Review": "bg-violet-50 text-violet-800 border-violet-200",
  Approved: "bg-emerald-50 text-emerald-800 border-emerald-200",
  Rejected: "bg-rose-50 text-rose-800 border-rose-200",
  Disbursed: "bg-teal-50 text-teal-800 border-teal-200",

  // Document statuses
  verified: "bg-emerald-50 text-emerald-800 border-emerald-200",
  uploaded: "bg-blue-50 text-blue-800 border-blue-200",
  pending_review: "bg-amber-50 text-amber-800 border-amber-200",
  rejected: "bg-rose-50 text-rose-800 border-rose-200",
  expired: "bg-orange-50 text-orange-800 border-orange-200",

  // Eligibility statuses
  eligible: "bg-emerald-50 text-emerald-800 border-emerald-200",
  possibly_eligible: "bg-amber-50 text-amber-800 border-amber-200",
  not_eligible: "bg-rose-50 text-rose-800 border-rose-200",

  // Priority / urgency
  critical: "bg-red-100 text-red-800 border-red-200",
  high: "bg-amber-100 text-amber-800 border-amber-200",
  normal: "bg-slate-100 text-slate-700 border-slate-200",
  low: "bg-slate-50 text-slate-500 border-slate-200",

  // Generic
  active: "bg-emerald-50 text-emerald-800 border-emerald-200",
  inactive: "bg-slate-100 text-slate-500 border-slate-200",
  draft: "bg-amber-50 text-amber-700 border-amber-200",
  published: "bg-blue-50 text-blue-800 border-blue-200",
  archived: "bg-slate-100 text-slate-500 border-slate-200",
};

const DOT_COLORS = {
  Approved: "bg-emerald-500",
  verified: "bg-emerald-500",
  eligible: "bg-emerald-500",
  Submitted: "bg-indigo-500",
  "Under Review": "bg-violet-500",
  Rejected: "bg-rose-500",
  rejected: "bg-rose-500",
  not_eligible: "bg-rose-500",
  expired: "bg-orange-500",
  possibly_eligible: "bg-amber-500",
  "Preparing Documents": "bg-amber-400",
  uploaded: "bg-blue-400",
  "Ready to Apply": "bg-blue-500",
  archived: "bg-slate-400",
};

/**
 * StatusBadge component.
 *
 * @param {object} props
 * @param {string} props.status - The status string to display (key in STATUS_STYLES)
 * @param {string} [props.label] - Override display label (defaults to status)
 * @param {"pill" | "dot-pill" | "outline"} [props.variant="pill"] - Visual variant
 * @param {"xs" | "sm" | "md"} [props.size="sm"] - Size variant
 * @param {string} [props.className=""] - Extra classes
 */
export default function StatusBadge({
  status,
  label,
  variant = "pill",
  size = "sm",
  className = "",
}) {
  const styles = STATUS_STYLES[status] ?? "bg-slate-100 text-slate-700 border-slate-200";
  const dotColor = DOT_COLORS[status] ?? "bg-slate-400";
  const displayLabel = label ?? formatStatusLabel(status);

  const sizeClasses = {
    xs: "text-[9px] px-1.5 py-0.5 font-black",
    sm: "text-[10px] px-2 py-0.5 font-bold",
    md: "text-xs px-2.5 py-1 font-semibold",
  };

  const baseClass = `inline-flex items-center gap-1.5 rounded-full border ${styles} ${sizeClasses[size] ?? sizeClasses.sm} ${className}`;

  if (variant === "dot-pill") {
    return (
      <span className={baseClass}>
        <span className={`h-1.5 w-1.5 rounded-full shrink-0 ${dotColor}`} />
        {displayLabel}
      </span>
    );
  }

  if (variant === "outline") {
    return (
      <span className={`${baseClass} bg-transparent`}>{displayLabel}</span>
    );
  }

  return <span className={baseClass}>{displayLabel}</span>;
}

/** Convert snake_case / CamelCase status strings to readable labels */
function formatStatusLabel(status = "") {
  if (!status) {
return "Unknown";
}
  return status
    .replace(/_/g, " ")
    .replace(/([A-Z])/g, " $1")
    .trim()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}
