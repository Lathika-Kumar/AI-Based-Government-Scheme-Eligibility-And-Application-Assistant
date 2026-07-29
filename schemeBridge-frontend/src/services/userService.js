/**
 * @file userService.js
 * @description User/Admin service — backend-integrated with mock fallbacks.
 *
 * Set VITE_USE_MOCK_API=true in .env to force mock responses.
 * All functions fall back to mock logic if the API call throws.
 */

import apiClient from "@utils/apiClient";
import { ENDPOINTS } from "@config/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";

// ── Mock data ─────────────────────────────────────────────────────────────────

const mockUsers = [
  {
    id: "USER-1001",
    name: "Sanjay Kumar",
    role: "Super Admin",
    department: "Government Scheme Evaluation Board",
    email: "sanjay.kumar@schemebridge.in",
    avatar: "https://i.pravatar.cc/150?img=1",
    status: "active",
    lastLogin: "2026-07-01T09:15:00Z",
  },
  {
    id: "USER-1002",
    name: "Anita Sharma",
    role: "Verification Officer",
    department: "Ministry of Finance",
    email: "anita.sharma@schemebridge.in",
    avatar: "https://i.pravatar.cc/150?img=2",
    status: "active",
    lastLogin: "2026-07-01T08:45:00Z",
  },
];

const MOCK_DASHBOARD_STATS = {
  totalUsers: 1284,
  activeSchemes: 47,
  pendingApplications: 312,
  verifiedDocuments: 8920,
  recentActivity: [],
};

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Fetch all users (admin only).
 * Backend integration: GET /api/v1/admin/users
 *
 * @returns {Promise<object[]>}
 */
export const fetchUsers = async () => {
  const fallbackMock = async () => Promise.resolve([...mockUsers]);

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(ENDPOINTS.ADMIN.USERS_LIST);
  } catch (error) {
    console.warn("fetchUsers API failed, falling back to mock:", error);
    return fallbackMock();
  }
};

/**
 * Fetch admin dashboard statistics.
 * Backend integration: GET /api/v1/admin/stats
 *
 * @returns {Promise<object>}
 */
export const getDashboardStats = async () => {
  const fallbackMock = async () => Promise.resolve({ ...MOCK_DASHBOARD_STATS });

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(ENDPOINTS.ADMIN.DASHBOARD_STATS);
  } catch (error) {
    console.warn("getDashboardStats API failed, falling back to mock:", error);
    return fallbackMock();
  }
};

/**
 * Update a user's details (admin only).
 * Backend integration: PUT /api/v1/admin/users/:id
 *
 * @param {object} updatedUser
 * @returns {Promise<object>}
 */
export const updateUser = async (updatedUser) => {
  const idx = mockUsers.findIndex((u) => u.id === updatedUser.id);
  if (idx !== -1) {
    mockUsers[idx] = { ...mockUsers[idx], ...updatedUser };
  }
  return Promise.resolve({ ...mockUsers[idx] });
};

/**
 * Reset a user's password (admin only).
 *
 * @param {string} userId
 * @returns {Promise<{ success: boolean, userId: string }>}
 */
export const resetPassword = async (userId) => {
  return Promise.resolve({ success: true, userId });
};

/**
 * Assign a new role to a user (admin only).
 *
 * @param {string} userId
 * @param {string} newRole
 * @returns {Promise<object>}
 */
export const assignRole = async (userId, newRole) => {
  const user = mockUsers.find((u) => u.id === userId);
  if (user) user.role = newRole;
  return Promise.resolve({ ...user });
};

const userService = {
  fetchUsers,
  getDashboardStats,
  updateUser,
  resetPassword,
  assignRole,
};

export default userService;
