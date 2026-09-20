/**
 * @file notificationService.js
 * @description In-app notifications & SSE real-time streaming service — connected to Scheme Service (port 8081).
 */

import { schemeApi, storage } from "@utils/apiClient";

const SCHEME_BASE = import.meta.env.VITE_SCHEME_API_URL || "http://localhost:8081";

/**
 * Fetch notifications for the current authenticated user (Citizen or Admin).
 */
export async function getNotifications({ unreadOnly = false, page = 0, size = 20 } = {}) {
  const params = { unreadOnly, page, size };
  return schemeApi.get("/api/notifications", { params });
}

/**
 * Get count of unread notifications for current user.
 */
export async function getUnreadCount() {
  return schemeApi.get("/api/notifications/unread-count");
}

/**
 * Mark a single notification as read.
 * Backend: POST /api/notifications/{id}/read
 */
export async function markNotificationRead(id) {
  return schemeApi.post(`/api/notifications/${encodeURIComponent(id)}/read`, {});
}

/**
 * Mark all notifications as read for current user.
 * Backend: POST /api/notifications/read-all
 */
export async function markAllNotificationsRead() {
  return schemeApi.post("/api/notifications/read-all", {});
}

/**
 * Admin: Broadcast or target a direct operational notification.
 */
export async function sendAdminNotification(payload) {
  return schemeApi.post("/api/admin/notifications/send", payload);
}

/**
 * Real-time SSE alert streaming for Admin Console.
 * Automatically handles token authentication and reconnection.
 *
 * @param {(data: object) => void} onMessage
 * @param {(err: any) => void} onError
 * @returns {() => void} cleanup unsubscribe function
 */
export function subscribeToNotificationStream(onMessage, onError) {
  const token = storage.getToken();
  if (!token) {
    if (onError) onError(new Error("No auth token available for SSE stream"));
    return () => {};
  }

  const sseUrl = `${SCHEME_BASE}/api/admin/notifications/stream?token=${encodeURIComponent(token)}`;
  let eventSource = null;
  let isClosed = false;

  try {
    eventSource = new EventSource(sseUrl);

    eventSource.addEventListener("notification", (event) => {
      try {
        const parsed = JSON.parse(event.data);
        if (onMessage) onMessage(parsed);
      } catch (err) {
        console.error("Failed to parse SSE notification payload", err);
      }
    });

    eventSource.addEventListener("CONNECTED", (event) => {
      console.log("SSE Admin Stream Connected:", event.data);
    });

    eventSource.onerror = (err) => {
      if (!isClosed) {
        console.warn("SSE connection encountered an error:", err);
        if (onError) onError(err);
      }
    };
  } catch (err) {
    console.error("Failed to initialize SSE EventSource:", err);
    if (onError) onError(err);
  }

  return () => {
    isClosed = true;
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
  };
}

const notificationService = {
  getNotifications,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
  sendAdminNotification,
  subscribeToNotificationStream,
};

export default notificationService;
