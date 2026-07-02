/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect } from "react";
import { DEFAULT_NOTIFICATIONS } from "../data/mockNotifications";

const NotificationContext = createContext(null);

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error("useNotifications must be used within a NotificationProvider");
  }
  return context;
};

export const NotificationProvider = ({ children }) => {
  const [notifications, setNotifications] = useState(() => {
    const saved = localStorage.getItem("schemebridge_notifications");
    return saved ? JSON.parse(saved) : DEFAULT_NOTIFICATIONS;
  });

  // Persist notifications to localStorage
  useEffect(() => {
    localStorage.setItem("schemebridge_notifications", JSON.stringify(notifications));
  }, [notifications]);

  // ── Notification Actions ──────────────────────────────────────────────────

  const markNotificationRead = (id) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const markAllNotificationsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const dismissNotification = (id) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  const addNotification = (notif) => {
    const newNotif = {
      id: `NOTIF-${  Date.now()}`,
      timestamp: new Date().toISOString(),
      read: false,
      priority: "normal",
      ...notif,
    };
    setNotifications((prev) => [newNotif, ...prev]);
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  const getUnreadNotifications = () => {
    return notifications.filter((n) => !n.read);
  };

  const getNotificationsByPriority = (priority) => {
    return notifications.filter((n) => n.priority === priority);
  };

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        markNotificationRead,
        markAllNotificationsRead,
        dismissNotification,
        addNotification,
        getUnreadNotifications,
        getNotificationsByPriority,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};
