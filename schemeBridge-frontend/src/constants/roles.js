/**
 * @file roles.js
 * @description User role constants for role-based access control.
 * Backend integration: replace values with server-returned role strings if they differ.
 */

/** Canonical role identifiers */
export const ROLES = Object.freeze({
  GUEST: "guest",
  CITIZEN: "citizen",
  SUPER_ADMIN: "super_admin",
  VERIFICATION_OFFICER: "verification_officer",
  SCHEME_MANAGER: "scheme_manager",
});

/** Human-readable display labels for each role */
export const ROLE_LABELS = Object.freeze({
  [ROLES.GUEST]: "Guest",
  [ROLES.CITIZEN]: "Citizen",
  [ROLES.SUPER_ADMIN]: "Super Administrator",
  [ROLES.VERIFICATION_OFFICER]: "Verification Officer",
  [ROLES.SCHEME_MANAGER]: "Scheme Manager",
});

/** Roles that have access to the citizen portal */
export const CITIZEN_ROLES = [ROLES.CITIZEN];

/** Roles that have access to the admin panel */
export const ADMIN_ROLES = [ROLES.SUPER_ADMIN, ROLES.VERIFICATION_OFFICER, ROLES.SCHEME_MANAGER];

/**
 * Check if a given role has admin access.
 * @param {string} role
 * @returns {boolean}
 */
export function isAdminRole(role) {
  return ADMIN_ROLES.includes(role);
}

/**
 * Check if a given role has citizen access.
 * @param {string} role
 * @returns {boolean}
 */
export function isCitizenRole(role) {
  return CITIZEN_ROLES.includes(role);
}

/**
 * Role-based permissions configuration
 * Defines which tabs/features each admin role can access
 */
export const ROLE_PERMISSIONS = Object.freeze({
  [ROLES.SUPER_ADMIN]: [
    "overview",
    "analytics",
    "applications",
    "documents",
    "schemes",
    "users",
    "grievances",
    "audits",
    "notifications",
    "settings"
  ],
  [ROLES.VERIFICATION_OFFICER]: [
    "overview",
    "applications",
    "documents",
    "grievances",
    "notifications"
  ],
  [ROLES.SCHEME_MANAGER]: [
    "overview",
    "schemes",
    "applications",
    "grievances",
    "notifications"
  ]
});

/**
 * Check if a role has permission to access a specific tab
 * @param {string} role
 * @param {string} tab
 * @returns {boolean}
 */
export function hasPermission(role, tab) {
  const permissions = ROLE_PERMISSIONS[role] || [];
  return permissions.includes(tab);
}
