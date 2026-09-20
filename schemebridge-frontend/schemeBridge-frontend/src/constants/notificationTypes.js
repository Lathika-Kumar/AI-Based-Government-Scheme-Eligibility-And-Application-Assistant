/**
 * @file notificationTypes.js
 * @description Notification type, priority, and category constants.
 * Backend integration: aligns with NotificationDTO category and priority fields.
 */

/** Notification category identifiers */
export const NOTIFICATION_TYPES = Object.freeze({
  APPLICATION: "application",
  DEADLINE: "deadline",
  DOCUMENT: "document",
  SYSTEM: "system",
  AI: "ai",
});

/** Priority levels (maps to urgency from backend) */
export const NOTIFICATION_PRIORITY = Object.freeze({
  CRITICAL: "critical",
  HIGH: "high",
  NORMAL: "normal",
  LOW: "low",
});

/** Display metadata for each notification type */
export const NOTIFICATION_TYPE_META = Object.freeze({
  [NOTIFICATION_TYPES.APPLICATION]: {
    label: "Application",
    color: "text-indigo-600",
    bg: "bg-indigo-50",
    border: "border-indigo-200",
    badge: "bg-indigo-100 text-indigo-800",
  },
  [NOTIFICATION_TYPES.DEADLINE]: {
    label: "Deadline",
    color: "text-rose-600",
    bg: "bg-rose-50",
    border: "border-rose-200",
    badge: "bg-rose-100 text-rose-800",
  },
  [NOTIFICATION_TYPES.DOCUMENT]: {
    label: "Document",
    color: "text-emerald-600",
    bg: "bg-emerald-50",
    border: "border-emerald-200",
    badge: "bg-emerald-100 text-emerald-800",
  },
  [NOTIFICATION_TYPES.SYSTEM]: {
    label: "System",
    color: "text-slate-600",
    bg: "bg-slate-50",
    border: "border-slate-200",
    badge: "bg-slate-100 text-slate-700",
  },
  [NOTIFICATION_TYPES.AI]: {
    label: "SchemeAI",
    color: "text-violet-600",
    bg: "bg-violet-50",
    border: "border-violet-200",
    badge: "bg-violet-100 text-violet-800",
  },
});

/** Display metadata for each priority level */
export const PRIORITY_META = Object.freeze({
  [NOTIFICATION_PRIORITY.CRITICAL]: {
    label: "Critical",
    color: "text-red-700",
    dot: "bg-red-500",
    badge: "bg-red-100 text-red-800 border-red-200",
    sortOrder: 0,
  },
  [NOTIFICATION_PRIORITY.HIGH]: {
    label: "High",
    color: "text-amber-700",
    dot: "bg-amber-500",
    badge: "bg-amber-100 text-amber-800 border-amber-200",
    sortOrder: 1,
  },
  [NOTIFICATION_PRIORITY.NORMAL]: {
    label: "Normal",
    color: "text-slate-700",
    dot: "bg-slate-400",
    badge: "bg-slate-100 text-slate-700 border-slate-200",
    sortOrder: 2,
  },
  [NOTIFICATION_PRIORITY.LOW]: {
    label: "Low",
    color: "text-slate-500",
    dot: "bg-slate-300",
    badge: "bg-slate-50 text-slate-500 border-slate-200",
    sortOrder: 3,
  },
});

/** All notification type labels for filter UI */
export const NOTIFICATION_TYPE_LABELS = Object.freeze(
  Object.fromEntries(
    Object.entries(NOTIFICATION_TYPE_META).map(([k, v]) => [k, v.label])
  )
);
