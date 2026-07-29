/**
 * @file routes.js
 * @description Application route path constants.
 * Single source of truth for all internal navigation paths.
 *
 * Usage:
 *   import { ROUTES } from "../config/routes";
 *   <Link to={ROUTES.CITIZEN.DASHBOARD}>
 *   navigate(ROUTES.CITIZEN.SCHEME_DETAIL("pm-kisan"))
 */

/** All public route paths */
export const PUBLIC_ROUTES = Object.freeze({
  HOME: "/",
  LOGIN: "/login",
  SIGNUP: "/signup",
  ABOUT: "/about",
  HELP_SUPPORT: "/help-support",
  TERMS: "/terms",
  DISCLAIMER: "/disclaimer",
  UNAUTHORIZED: "/unauthorized",
  NOT_FOUND: "*",
  // Error pages
  ERROR_401: "/401",
  ERROR_403: "/403",
  ERROR_500: "/500",
});

/** All citizen portal route paths */
export const CITIZEN_ROUTES = Object.freeze({
  ONBOARDING: "/onboarding",
  DASHBOARD: "/dashboard",
  PROFILE: "/profile",
  RECOMMENDATIONS: "/recommendations",
  SCHEME_DETAIL: (id = ":id") => `/scheme/${id}`,
  DOCUMENTS: "/documents",
  TRACKER: "/tracker",
  NOTIFICATIONS: "/notifications",
  HELP: "/help",
});

/** All admin panel route paths */
export const ADMIN_ROUTES = Object.freeze({
  ROOT: "/admin",
  DASHBOARD: "/admin/dashboard",
  SCHEMES: "/admin/schemes",
  APPLICATIONS: "/admin/applications",
  DOCUMENTS: "/admin/documents",
  USERS: "/admin/users",
  GRIEVANCES: "/admin/grievances",
  ANALYTICS: "/admin/analytics",
  REPORTS: "/admin/reports",
  AUDIT: "/admin/audit",
});

/** Combined routes object for convenience */
export const ROUTES = Object.freeze({
  PUBLIC: PUBLIC_ROUTES,
  CITIZEN: CITIZEN_ROUTES,
  ADMIN: ADMIN_ROUTES,
});

export default ROUTES;
