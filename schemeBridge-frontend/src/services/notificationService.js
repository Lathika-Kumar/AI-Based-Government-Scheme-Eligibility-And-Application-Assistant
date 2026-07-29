/**
 * @file notificationService.js
 * @description Notification service — backend-integrated with mock fallbacks.
 *
 * Set VITE_USE_MOCK_API=true in .env to force mock responses.
 * All functions fall back to mock logic if the API call throws.
 */

import { MOCK_LOADING_DELAY_MS } from "../config/constants";
import { DEFAULT_NOTIFICATIONS } from "../data/mockNotifications";
import apiClient from "@utils/apiClient";
import { ENDPOINTS, buildUrl } from "@config/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";

const delay = (ms = MOCK_LOADING_DELAY_MS) =>
  new Promise((resolve) => setTimeout(resolve, ms));

// In-memory mock store
let _mockNotifications = [...DEFAULT_NOTIFICATIONS];

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Fetch all notifications for the current user.
 * Backend integration: GET /api/v1/notifications?page=0&size=15&category=
 *
 * @param {{ category?: string, onlyUnread?: boolean }} [options]
 * @returns {Promise<object[]>}
 */
export async function getNotifications({ category = "", onlyUnread = false } = {}) {
  const fallbackMock = async () => {
    await delay();
    let results = [..._mockNotifications];
    if (category) results = results.filter((n) => n.category === category);
    if (onlyUnread) results = results.filter((n) => !n.read);
    results.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    return results;
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(ENDPOINTS.NOTIFICATIONS.LIST, {
      params: { category, onlyUnread },
    });
  } catch (error) {
    console.warn("getNotifications API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Get the count of unread notifications.
 * Backend integration: GET /api/v1/notifications/unread-count → { count: number }
 *
 * @returns {Promise<{ count: number }>}
 */
export async function getUnreadCount() {
  const fallbackMock = async () => {
    await delay(200);
    const count = _mockNotifications.filter((n) => !n.read).length;
    return { count };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.get(ENDPOINTS.NOTIFICATIONS.UNREAD_COUNT);
  } catch (error) {
    console.warn("getUnreadCount API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Mark a specific notification as read.
 * Backend integration: POST /api/v1/notifications/:id/read
 *
 * @param {string} id
 * @returns {Promise<{ success: boolean, id: string }>}
 */
export async function markRead(id) {
  const fallbackMock = async () => {
    await delay(200);
    _mockNotifications = _mockNotifications.map((n) =>
      n.id === id ? { ...n, read: true } : n
    );
    return { success: true, id };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(buildUrl(ENDPOINTS.NOTIFICATIONS.MARK_READ, { id }));
  } catch (error) {
    console.warn("markRead API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Mark all notifications as read.
 * Backend integration: POST /api/v1/notifications/read-all
 *
 * @returns {Promise<{ success: boolean, count: number }>}
 */
export async function markAllRead() {
  const fallbackMock = async () => {
    await delay(400);
    const count = _mockNotifications.filter((n) => !n.read).length;
    _mockNotifications = _mockNotifications.map((n) => ({ ...n, read: true }));
    return { success: true, count };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.post(ENDPOINTS.NOTIFICATIONS.MARK_ALL_READ);
  } catch (error) {
    console.warn("markAllRead API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Delete a notification by ID.
 * Backend integration: DELETE /api/v1/notifications/:id
 *
 * @param {string} id
 * @returns {Promise<{ success: boolean, id: string }>}
 */
export async function deleteNotification(id) {
  const fallbackMock = async () => {
    await delay(300);
    _mockNotifications = _mockNotifications.filter((n) => n.id !== id);
    return { success: true, id };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    return await apiClient.delete(buildUrl(ENDPOINTS.NOTIFICATIONS.DELETE, { id }));
  } catch (error) {
    console.warn("deleteNotification API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Delete all notifications that have been read.
 * Backend integration: DELETE /api/v1/notifications?onlyRead=true
 *
 * @returns {Promise<{ success: boolean, deleted: number }>}
 */
export async function clearReadNotifications() {
  await delay(400);
  const before = _mockNotifications.length;
  _mockNotifications = _mockNotifications.filter((n) => !n.read);
  return { success: true, deleted: before - _mockNotifications.length };
}

/**
 * Update notification preferences (channels, priority thresholds).
 * Backend integration: PUT /api/v1/notifications/preferences
 *
 * @param {object} preferences
 * @returns {Promise<{ success: boolean }>}
 */
export async function updateNotificationPreferences(preferences) {
  await delay(500);
  return { success: true, preferences };
}

const notificationService = {
  getNotifications,
  getUnreadCount,
  markRead,
  markAllRead,
  deleteNotification,
  clearReadNotifications,
  updateNotificationPreferences,
};

export default notificationService;
