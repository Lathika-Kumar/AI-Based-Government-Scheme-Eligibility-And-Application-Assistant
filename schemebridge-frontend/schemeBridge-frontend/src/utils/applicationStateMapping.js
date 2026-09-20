/**
 * @file applicationStateMapping.js
 * @description Authoritative application state resolution and button action mapping.
 * 
 * Enforces strict project state machine:
 * - NO APPLICATION:            [File Application]
 * - DRAFT / DOCUMENTS_PENDING: [Continue Application]
 * - SUBMITTED / UNDER_REVIEW:  [View Application]
 * - APPROVED:                  [View Application]
 * - REJECTED:                  [View Application]
 */

export const ACTION_TYPES = Object.freeze({
  FILE_APPLICATION: "FILE_APPLICATION",
  CONTINUE_APPLICATION: "CONTINUE_APPLICATION",
  VIEW_APPLICATION: "VIEW_APPLICATION",
});

/**
 * Finds matching application record for a scheme.
 * Compares against schemeId, schemeCode, slug, and code.
 */
export function findApplicationForScheme(schemeOrCode, applications = []) {
  if (!schemeOrCode || !Array.isArray(applications) || applications.length === 0) {
    return null;
  }

  const targets = [];
  if (typeof schemeOrCode === "object") {
    if (schemeOrCode.schemeCode) targets.push(String(schemeOrCode.schemeCode).trim().toLowerCase());
    if (schemeOrCode.code) targets.push(String(schemeOrCode.code).trim().toLowerCase());
    if (schemeOrCode.id) targets.push(String(schemeOrCode.id).trim().toLowerCase());
    if (schemeOrCode._id) targets.push(String(schemeOrCode._id).trim().toLowerCase());
    if (schemeOrCode.slug) targets.push(String(schemeOrCode.slug).trim().toLowerCase());
  } else {
    targets.push(String(schemeOrCode).trim().toLowerCase());
  }

  return applications.find((app) => {
    const aId = app.schemeId ? String(app.schemeId).trim().toLowerCase() : "";
    const aCode = app.schemeCode ? String(app.schemeCode).trim().toLowerCase() : "";
    const aNum = app.applicationNumber ? String(app.applicationNumber).trim().toLowerCase() : "";
    const aRef = app.referenceNo ? String(app.referenceNo).trim().toLowerCase() : "";

    return targets.some((t) => t && (t === aId || t === aCode || t === aNum || t === aRef));
  }) || null;
}

/**
 * Resolves application status and returns canonical button action, label, badge, and navigation route.
 */
export function resolveApplicationAction(scheme, applications = []) {
  const app = findApplicationForScheme(scheme, applications);

  if (!app) {
    const schemeSlug = scheme?.slug || scheme?.schemeCode || scheme?.id || "";
    return {
      hasApplication: false,
      hasExistingApp: false,
      application: null,
      action: ACTION_TYPES.FILE_APPLICATION,
      label: "File Application",
      buttonText: "File Application",
      badgeText: null,
      badgeColor: null,
      targetRoute: `/scheme/${schemeSlug}`,
      status: null,
      canApplyAgain: true,
    };
  }

  const rawStatus = String(app.status || app.currentStage || "").toUpperCase().replace(/\s+/g, "_");
  const appId = app.id || app.applicationNumber || app.referenceNo || "";

  // 1. Incomplete / Draft Applications
  if (
    rawStatus === "DRAFT" ||
    rawStatus === "DOCUMENTS_PENDING" ||
    rawStatus === "READY_FOR_SUBMISSION" ||
    rawStatus === "PREPARING" ||
    rawStatus === "PREPARING_DOCUMENTS"
  ) {
    return {
      hasApplication: true,
      hasExistingApp: true,
      application: app,
      action: ACTION_TYPES.CONTINUE_APPLICATION,
      label: "Continue Application",
      buttonText: "Continue Application",
      badgeText: "Application in Progress",
      badgeColor: "bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 border-amber-200 dark:border-amber-800",
      targetRoute: appId ? `/applications/${appId}` : "/applications",
      status: rawStatus,
      canApplyAgain: false,
    };
  }

  // 2. Submitted / In Review Applications
  if (
    rawStatus === "SUBMITTED" ||
    rawStatus === "UNDER_REVIEW" ||
    rawStatus === "READY_TO_APPLY"
  ) {
    return {
      hasApplication: true,
      hasExistingApp: true,
      application: app,
      action: ACTION_TYPES.VIEW_APPLICATION,
      label: "View Application",
      buttonText: "View Application",
      badgeText: rawStatus === "UNDER_REVIEW" ? "Under Department Review" : "Application Submitted",
      badgeColor: "bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800",
      targetRoute: "/applications",
      status: rawStatus,
      canApplyAgain: false,
    };
  }

  // 3. Approved Applications
  if (rawStatus === "APPROVED" || rawStatus === "DISBURSED") {
    return {
      hasApplication: true,
      hasExistingApp: true,
      application: app,
      action: ACTION_TYPES.VIEW_APPLICATION,
      label: "View Application",
      buttonText: "View Application",
      badgeText: rawStatus === "DISBURSED" ? "Benefit Disbursed" : "Application Approved",
      badgeColor: "bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800",
      targetRoute: "/applications",
      status: rawStatus,
      canApplyAgain: false,
    };
  }

  // 4. Rejected / Cancelled Applications
  if (rawStatus === "REJECTED" || rawStatus === "CANCELLED") {
    return {
      hasApplication: true,
      hasExistingApp: true,
      application: app,
      action: ACTION_TYPES.VIEW_APPLICATION,
      label: "View Application",
      buttonText: "View Application",
      badgeText: rawStatus === "CANCELLED" ? "Application Cancelled" : "Application Rejected",
      badgeColor: "bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300 border-rose-200 dark:border-rose-800",
      targetRoute: "/applications",
      status: rawStatus,
      canApplyAgain: true,
    };
  }

  // Fallback default
  return {
    hasApplication: true,
    application: app,
    action: ACTION_TYPES.VIEW_APPLICATION,
    label: "View Application",
    buttonText: "View Application",
    badgeText: `Status: ${rawStatus.replace(/_/g, " ")}`,
    badgeColor: "bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700",
    targetRoute: `/applications/${appId}`,
    status: rawStatus,
    canApplyAgain: false,
  };
}
