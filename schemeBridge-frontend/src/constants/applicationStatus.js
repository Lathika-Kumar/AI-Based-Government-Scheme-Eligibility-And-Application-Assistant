/**
 * @file applicationStatus.js
 * @description Application lifecycle status constants.
 * Extracted from Tracker.jsx for reuse across pages.
 * Backend integration: aligns with ApplicationDTO.status field values.
 */

/** All canonical application status values */
export const APPLICATION_STATUS = Object.freeze({
  SAVED: "Saved",
  PREPARING: "Preparing Documents",
  READY: "Ready to Apply",
  SUBMITTED: "Submitted",
  UNDER_REVIEW: "Under Review",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  DISBURSED: "Disbursed",
});

/** Full 7-stage pipeline for saved/in-progress applications */
export const STAGE_ORDER = Object.freeze([
  APPLICATION_STATUS.SAVED,
  APPLICATION_STATUS.PREPARING,
  APPLICATION_STATUS.READY,
  APPLICATION_STATUS.SUBMITTED,
  APPLICATION_STATUS.UNDER_REVIEW,
  APPLICATION_STATUS.APPROVED,
  APPLICATION_STATUS.REJECTED,
]);

/** 3-stage pipeline for submitted applications (post-submission tracking) */
export const APPLIED_STAGE_ORDER = Object.freeze([
  APPLICATION_STATUS.SUBMITTED,
  APPLICATION_STATUS.UNDER_REVIEW,
  APPLICATION_STATUS.APPROVED,
]);

/** Display configuration per status — colors, icons, borders */
export const STATUS_CONFIG = Object.freeze({
  [APPLICATION_STATUS.SAVED]: {
    color: "bg-slate-100 text-slate-600 border-slate-200",
    dot: "bg-slate-400",
    borderLeft: "border-l-slate-300",
    label: "Saved",
    description: "Scheme saved for future application.",
  },
  [APPLICATION_STATUS.PREPARING]: {
    color: "bg-amber-50 text-amber-800 border-amber-200",
    dot: "bg-amber-400",
    borderLeft: "border-l-amber-400",
    label: "Preparing Documents",
    description: "Gathering required documents.",
  },
  [APPLICATION_STATUS.READY]: {
    color: "bg-blue-50 text-blue-800 border-blue-200",
    dot: "bg-blue-500",
    borderLeft: "border-l-blue-400",
    label: "Ready to Apply",
    description: "All documents verified. Ready to submit.",
  },
  [APPLICATION_STATUS.SUBMITTED]: {
    color: "bg-indigo-50 text-indigo-800 border-indigo-200",
    dot: "bg-indigo-500",
    borderLeft: "border-l-indigo-500",
    label: "Submitted",
    description: "Application submitted to the government portal.",
  },
  [APPLICATION_STATUS.UNDER_REVIEW]: {
    color: "bg-violet-50 text-violet-800 border-violet-200",
    dot: "bg-violet-500",
    borderLeft: "border-l-violet-500",
    label: "Under Review",
    description: "Application is being reviewed by the department.",
  },
  [APPLICATION_STATUS.APPROVED]: {
    color: "bg-emerald-50 text-emerald-800 border-emerald-200",
    dot: "bg-emerald-500",
    borderLeft: "border-l-emerald-500",
    label: "Approved",
    description: "Application approved. Benefits processing initiated.",
  },
  [APPLICATION_STATUS.REJECTED]: {
    color: "bg-rose-50 text-rose-800 border-rose-200",
    dot: "bg-rose-500",
    borderLeft: "border-l-rose-500",
    label: "Rejected",
    description: "Application rejected. Check reason and re-apply.",
  },
  [APPLICATION_STATUS.DISBURSED]: {
    color: "bg-teal-50 text-teal-800 border-teal-200",
    dot: "bg-teal-500",
    borderLeft: "border-l-teal-500",
    label: "Disbursed",
    description: "Benefits have been disbursed to your account.",
  },
});

/** Terminal statuses that end the pipeline */
export const TERMINAL_STATUSES = [
  APPLICATION_STATUS.APPROVED,
  APPLICATION_STATUS.REJECTED,
  APPLICATION_STATUS.DISBURSED,
];

/**
 * Returns whether a status is terminal (no further progression).
 * @param {string} status
 * @returns {boolean}
 */
export function isTerminalStatus(status) {
  return TERMINAL_STATUSES.includes(status);
}
