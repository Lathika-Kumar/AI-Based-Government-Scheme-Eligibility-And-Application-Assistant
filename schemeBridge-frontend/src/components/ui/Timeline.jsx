/**
 * @file Timeline.jsx
 * @description Reusable vertical timeline component.
 * Used in Dashboard (activity log) and Tracker (application pipeline).
 */

import React from "react";

/**
 * Timeline component.
 *
 * @param {object} props
 * @param {TimelineItem[]} props.items - Timeline items array
 * @param {"sm" | "md"} [props.size="md"] - Item size variant
 * @param {string} [props.className=""] - Extra wrapper classes
 */

/**
 * @typedef {object} TimelineItem
 * @property {string} id - Unique key
 * @property {string} title - Event title
 * @property {string} [description] - Supporting description
 * @property {string} [timestamp] - ISO timestamp string
 * @property {string} [timeLabel] - Pre-formatted time label (overrides timestamp)
 * @property {string} [dotColor="bg-slate-400"] - Dot/bullet color class
 * @property {string} [meta] - Small meta text (e.g., IP address, reference)
 * @property {boolean} [isActive=false] - Highlight as active/current step
 */

function formatTime(isoString) {
  if (!isoString) {
return "";
}
  try {
    return new Date(isoString).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return isoString;
  }
}

export default function Timeline({ items = [], size = "md", className = "" }) {
  if (!items.length) {
return null;
}

  const isCompact = size === "sm";

  return (
    <div
      className={`relative pl-4 border-l border-slate-100 ${
        isCompact ? "space-y-3" : "space-y-4"
      } ${className}`}
    >
      {items.map((item) => (
        <div key={item.id} className="relative group">
          {/* Timeline bullet */}
          <div
            className={`absolute -left-[21px] top-1.5 h-2 w-2 rounded-full ring-4 ring-white transition ${
              item.isActive
                ? "bg-indigo-600 ring-indigo-100"
                : (item.dotColor ?? "bg-slate-400 group-hover:bg-indigo-600")
            }`}
          />

          <div className="space-y-0.5">
            {/* Title row */}
            <div className="flex items-center justify-between gap-2">
              <h4
                className={`font-bold text-slate-800 leading-tight ${
                  isCompact ? "text-[11px]" : "text-xs"
                }`}
              >
                {item.title}
              </h4>
              <span className="text-[9px] text-slate-400 shrink-0">
                {item.timeLabel ?? formatTime(item.timestamp)}
              </span>
            </div>

            {/* Description */}
            {item.description && (
              <p
                className={`text-slate-500 leading-normal ${
                  isCompact ? "text-[10px]" : "text-xs"
                }`}
              >
                {item.description}
              </p>
            )}

            {/* Meta (IP, reference, etc.) */}
            {item.meta && (
              <span className="text-[8px] bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded font-mono inline-block mt-0.5">
                {item.meta}
              </span>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
