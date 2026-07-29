/**
 * @file EmptyState.jsx
 * @description Reusable centered empty state component.
 * Used in Documents, Tracker, Recommendations, and Notifications
 * when no data is available to display.
 */

import React from "react";
import { Inbox } from "lucide-react";

/**
 * EmptyState component.
 *
 * @param {object} props
 * @param {React.ComponentType} [props.icon=Inbox] - Lucide icon component
 * @param {string} props.title - Primary heading text
 * @param {string} [props.description] - Supporting description text
 * @param {{ label: string, onClick?: () => void, href?: string, variant?: "primary" | "secondary" }} [props.action] - CTA button config
 * @param {React.ReactNode} [props.children] - Optional additional content slot
 * @param {string} [props.className=""] - Extra wrapper classes
 * @param {"sm" | "md" | "lg"} [props.size="md"] - Controls icon and text sizing
 */
export default function EmptyState({
  icon: Icon = Inbox,
  title,
  description,
  action,
  children,
  className = "",
  size = "md",
}) {
  const sizeConfig = {
    sm: {
      wrapper: "py-8 px-4",
      iconBox: "h-10 w-10",
      icon: "h-5 w-5",
      title: "text-sm font-bold",
      description: "text-xs",
    },
    md: {
      wrapper: "py-12 px-6",
      iconBox: "h-14 w-14",
      icon: "h-7 w-7",
      title: "text-base font-bold",
      description: "text-sm",
    },
    lg: {
      wrapper: "py-16 px-8",
      iconBox: "h-20 w-20",
      icon: "h-10 w-10",
      title: "text-lg font-bold",
      description: "text-sm",
    },
  };

  const s = sizeConfig[size] ?? sizeConfig.md;

  return (
    <div
      className={`flex flex-col items-center justify-center text-center ${s.wrapper} ${className}`}
      role="status"
      aria-label={title ?? "No data available"}
    >
      {/* Icon */}
      <div
        className={`${s.iconBox} rounded-2xl bg-slate-100 border border-slate-200 flex items-center justify-center mb-4 shadow-inner`}
      >
        <Icon className={`${s.icon} text-slate-400`} />
      </div>

      {/* Title */}
      {title && (
        <h3 className={`${s.title} text-slate-800 leading-snug mb-1.5`}>
          {title}
        </h3>
      )}

      {/* Description */}
      {description && (
        <p className={`${s.description} text-slate-500 max-w-xs leading-relaxed`}>
          {description}
        </p>
      )}

      {/* Additional slot content */}
      {children && <div className="mt-3">{children}</div>}

      {/* Action button */}
      {action && (
        <div className="mt-5">
          {action.href ? (
            <a
              href={action.href}
              className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold transition ${
                action.variant === "secondary"
                  ? "bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-200"
                  : "bg-slate-900 text-white hover:bg-slate-800 shadow-xs"
              }`}
            >
              {action.icon && <action.icon className="h-4 w-4" />}
              {action.label}
            </a>
          ) : (
            <button
              onClick={action.onClick}
              className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold transition ${
                action.variant === "secondary"
                  ? "bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-200"
                  : "bg-slate-900 text-white hover:bg-slate-800 shadow-xs"
              }`}
            >
              {action.icon && <action.icon className="h-4 w-4" />}
              {action.label}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
