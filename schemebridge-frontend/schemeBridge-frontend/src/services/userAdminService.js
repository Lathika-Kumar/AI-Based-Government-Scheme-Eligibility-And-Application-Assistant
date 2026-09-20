/**
 * @file userAdminService.js
 * @description Administrative User Management Service — connected to Auth Service (port 8080 / Oracle 21c).
 *
 * Authorization: ROLE_ADMIN strictly required (enforced by Auth Service).
 */

import { authApi } from "@utils/apiClient";

/**
 * Get paginated list of users with search, role, and status filters.
 */
export async function getAdminUsers({
  search,
  role,
  status,
  page = 0,
  size = 20,
  sort = "createdAt",
  direction = "DESC",
} = {}) {
  const params = { page, size, sort, direction };
  if (search) params.search = search;
  if (role && role !== "ALL") params.role = role;
  if (status && status !== "ALL") params.status = status;
  return authApi.get("/api/admin/users", { params });
}

/**
 * Get single user by ID.
 */
export async function getAdminUserById(userId) {
  return authApi.get(`/api/admin/users/${encodeURIComponent(userId)}`);
}

/**
 * Update user status (ACTIVE, INACTIVE, LOCKED).
 */
export async function updateUserStatus(userId, status, reason = "") {
  return authApi.patch(`/api/admin/users/${encodeURIComponent(userId)}/status`, {
    status,
    reason,
  });
}

/**
 * Update user assigned roles.
 */
export async function updateUserRoles(userId, roles, reason = "") {
  return authApi.patch(`/api/admin/users/${encodeURIComponent(userId)}/roles`, {
    roles,
    reason,
  });
}

/**
 * Unlock locked user account.
 */
export async function unlockUser(userId) {
  return authApi.post(`/api/admin/users/${encodeURIComponent(userId)}/unlock`, {});
}

const userAdminService = {
  getAdminUsers,
  getAdminUserById,
  updateUserStatus,
  updateUserRoles,
  unlockUser,
};

export default userAdminService;
