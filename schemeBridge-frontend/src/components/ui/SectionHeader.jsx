/**
 * @file SectionHeader.jsx
 * @description Reusable section header used inside cards and panels.
 * Renders an icon + label + optional right-side slot.
 */

import React from "react";

/**
 * SectionHeader component.
 *
 * @param {object} props
 * @param {React.ComponentType} [props.icon] - Lucide icon component
 * @param {string} [props.iconColor="text-indigo-600"] - Icon color class
 * @param {string} props.title - Section title text
 * @param {React.ReactNode} [props.actions] - Right-side slot (badge, button, etc.)
 * @param {boolean} [props.divider=true] - Show bottom border divider
 * @param {string} [props.className=""] - Extra classes on the wrapper
 */
export default function SectionHeader({
  icon: Icon,
  iconColor = "text-indigo-600",
  title,
  actions,
  divider = true,
  className = "",
}) {
  return (
    <div
      className={`flex items-center justify-between gap-2 ${
        divider ? "border-b border-slate-100 pb-3 mb-4" : ""
      } ${className}`}
    >
      <div className="flex items-center gap-2">
        {Icon && <Icon className={`h-4 w-4 shrink-0 ${iconColor}`} />}
        <h2 className="text-xs font-bold text-slate-900 uppercase tracking-widest leading-none">
          {title}
        </h2>
      </div>

      {actions && (
        <div className="flex items-center gap-2 shrink-0">{actions}</div>
      )}
    </div>
  );
}
