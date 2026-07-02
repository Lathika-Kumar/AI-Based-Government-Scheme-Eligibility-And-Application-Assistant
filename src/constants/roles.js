/**
 * @file roles.js
 * @description User role constants for role-based access control.
 * Backend integration: replace values with server-returned role strings if they differ.
 */

/** Canonical role identifiers */
export const ROLES = Object.freeze({
  GUEST: "guest",
  CITIZEN: "citizen",
  ADMIN: "admin",
  SUPER_ADMIN: "super_admin",
});

/** Human-readable display labels for each role */
export const ROLE_LABELS = Object.freeze({
  [ROLES.GUEST]: "Guest",
  [ROLES.CITIZEN]: "Citizen",
  [ROLES.ADMIN]: "Administrator",
  [ROLES.SUPER_ADMIN]: "Super Administrator",
});

/** Roles that have access to the citizen portal */
export const CITIZEN_ROLES = [ROLES.CITIZEN];

/** Roles that have access to the admin panel */
export const ADMIN_ROLES = [ROLES.ADMIN, ROLES.SUPER_ADMIN];

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
