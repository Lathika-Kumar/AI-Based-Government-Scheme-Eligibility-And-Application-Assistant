import React, { useState, useEffect, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useToast } from "@components/ui/ToastNotification";
import {
  Bell,
  CheckCircle,
  AlertTriangle,
  Users,
  ShieldCheck,
  FileText,
  Trash2,
  Inbox,
  Send,
  Calendar,
  Clock,
  Mail,
  MessageSquare,
  Megaphone,
  Eye,
  History,
  Layout,
  Smartphone,
  RefreshCw,
  ArrowRight
} from "lucide-react";
import notificationService from "@services/notificationService";

export default function AdminNotificationsCenter() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [activeTab, setActiveTab] = useState("internal");
  const [notificationsList, setNotificationsList] = useState([]);
  const [loading, setLoading] = useState(true);

  // New Broadcast Form
  const [newBroadcast, setNewBroadcast] = useState({
    title: "",
    message: "",
    recipientRole: "ROLE_USER",
    type: "SYSTEM_ALERT",
    channel: "IN_APP"
  });

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const res = await notificationService.getNotifications({ page: 0, size: 50 });
      if (!res.error && res.data) {
        setNotificationsList(res.data.content || []);
      }
    } catch (err) {
      console.error("Failed to load notifications", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchNotifications();

    // Subscribe to SSE stream
    const unsubscribe = notificationService.subscribeToNotificationStream(
      (newAlert) => {
        showToast("info", "New Real-time Alert", newAlert.title || "Admin notification received");
        setNotificationsList((prev) => [newAlert, ...prev]);
      },
      (err) => {
        console.warn("SSE stream alert error", err);
      }
    );

    return () => unsubscribe();
  }, [fetchNotifications, showToast]);

  const unreadCount = notificationsList.filter((n) => !n.read).length;

  const markAllRead = async () => {
    try {
      await notificationService.markAllNotificationsRead();
      setNotificationsList(notificationsList.map((n) => ({ ...n, read: true })));
      showToast("success", "Marked as Read", "All notifications marked as read.");
    } catch (err) {
      showToast("error", "Error", "Failed to mark all as read.");
    }
  };

  const handleMarkRead = async (id) => {
    try {
      await notificationService.markNotificationRead(id);
      setNotificationsList(notificationsList.map((n) => n.id === id ? { ...n, read: true } : n));
    } catch (err) {
      console.error(err);
    }
  };

  const handleSendBroadcast = async (e) => {
    e.preventDefault();
    if (!newBroadcast.title.trim() || !newBroadcast.message.trim()) {
      showToast("error", "Validation Error", "Title and message are required.");
      return;
    }

    try {
      const payload = {
        recipientRole: newBroadcast.recipientRole || null,
        type: newBroadcast.type || "SYSTEM_ALERT",
        title: newBroadcast.title,
        message: newBroadcast.message,
        channel: newBroadcast.channel || "IN_APP"
      };

      const res = await notificationService.sendAdminNotification(payload);
      if (!res.error) {
        showToast("success", "Broadcast Dispatched", "Notification successfully sent to target audience.");
        setNewBroadcast({
          title: "",
          message: "",
          recipientRole: "ROLE_USER",
          type: "SYSTEM_ALERT",
          channel: "IN_APP"
        });
        fetchNotifications();
      } else {
        showToast("error", "Broadcast Failed", res.message);
      }
    } catch (err) {
      showToast("error", "Error", "Failed to dispatch broadcast.");
    }
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
        <div className="flex flex-col md:flex-row justify-between md:items-center gap-4">
          <div>
            <h3 className="text-lg font-black text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <Bell className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
              Administrative Alert Center & Live SSE Stream
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 font-semibold mt-1">
              Real-time operational alerts, citizen push communications, and system broadcast console
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={fetchNotifications}
              className="p-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 rounded-xl transition"
              title="Refresh"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            {unreadCount > 0 && (
              <button
                onClick={markAllRead}
                className="px-3 py-2 bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 border border-indigo-200 dark:border-indigo-800/60 rounded-xl text-xs font-bold transition flex items-center gap-1.5"
              >
                <CheckCircle className="h-3.5 w-3.5" />
                Mark All Read ({unreadCount})
              </button>
            )}
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex gap-2 mt-5 border-t border-slate-100 dark:border-slate-800 pt-4">
          <button
            onClick={() => setActiveTab("internal")}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
              activeTab === "internal"
                ? "bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300"
                : "bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
            }`}
          >
            <Inbox className="h-4 w-4" />
            Live In-App Alerts ({notificationsList.length})
          </button>
          <button
            onClick={() => setActiveTab("broadcast")}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
              activeTab === "broadcast"
                ? "bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300"
                : "bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
            }`}
          >
            <Send className="h-4 w-4" />
            Broadcast Notification
          </button>
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === "internal" ? (
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
          {loading ? (
            <div className="p-12 text-center text-xs font-semibold text-slate-500 dark:text-slate-400">
              Loading Live Notifications...
            </div>
          ) : notificationsList.length === 0 ? (
            <div className="p-16 flex flex-col items-center justify-center text-center space-y-3">
              <Bell className="h-10 w-10 text-slate-400 dark:text-slate-500" />
              <div>
                <p className="text-sm font-bold text-slate-800 dark:text-slate-200">No Notifications</p>
                <p className="text-xs text-slate-400 dark:text-slate-400 mt-1">All administrative alerts and citizen updates are caught up.</p>
              </div>
            </div>
          ) : (
            <div className="divide-y divide-slate-100 dark:divide-slate-800">
              {notificationsList.map((n) => (
                <div
                  key={n.id}
                  onClick={() => !n.read && handleMarkRead(n.id)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      if (!n.read) handleMarkRead(n.id);
                    }
                  }}
                  className={`p-5 flex items-start justify-between gap-4 transition cursor-pointer hover:bg-slate-50/70 dark:hover:bg-slate-800/50 ${
                    !n.read ? "bg-indigo-50/20 dark:bg-indigo-950/20" : ""
                  }`}
                >
                  <div className="flex items-start gap-4">
                    <div className="h-9 w-9 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-100 dark:border-indigo-800/60 flex items-center justify-center shrink-0">
                      <Bell className="h-4.5 w-4.5 text-indigo-600 dark:text-indigo-400" />
                    </div>
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900 dark:text-slate-100">{n.title}</span>
                        <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 uppercase">
                          {n.type?.replace(/_/g, " ")}
                        </span>
                        {!n.read && (
                          <span className="h-2 w-2 rounded-full bg-indigo-600"></span>
                        )}
                      </div>
                      <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">{n.message}</p>
                      <div className="text-[10px] text-slate-400 dark:text-slate-400 font-semibold">
                        {n.createdAt ? new Date(n.createdAt).toLocaleString("en-IN") : "Recent"}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    {(n.relatedEntityId || n.applicationId) && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          if (!n.read) handleMarkRead(n.id);
                          navigate(`/admin/review/${n.relatedEntityId || n.applicationId}`);
                        }}
                        className="text-xs font-bold text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300 px-3 py-1 rounded-lg hover:bg-indigo-50 dark:hover:bg-indigo-950/60 transition flex items-center gap-1"
                      >
                        <span>View Details</span>
                        <ArrowRight className="h-3 w-3" />
                      </button>
                    )}

                    {!n.read && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMarkRead(n.id);
                        }}
                        className="text-xs font-bold text-slate-600 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200 px-3 py-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition shrink-0"
                      >
                        Mark Read
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 rounded-2xl shadow-sm space-y-6">
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-800 dark:text-slate-200 flex items-center gap-2">
            <Megaphone className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
            Dispatch Broadcast Communication
          </h4>

          <form onSubmit={handleSendBroadcast} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">Target Audience</label>
                <select
                  value={newBroadcast.recipientRole}
                  onChange={(e) => setNewBroadcast({ ...newBroadcast, recipientRole: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-200"
                >
                  <option value="ROLE_USER">All Registered Citizens</option>
                  <option value="ROLE_VERIFICATION_OFFICER">Verification Officers</option>
                  <option value="ROLE_SCHEME_MANAGER">Scheme Managers</option>
                  <option value="ROLE_ADMIN">Super Administrators</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">Alert Category</label>
                <select
                  value={newBroadcast.type}
                  onChange={(e) => setNewBroadcast({ ...newBroadcast, type: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-200"
                >
                  <option value="SYSTEM_ALERT">System Advisory</option>
                  <option value="APPLICATION_SUBMITTED">Deadline Notification</option>
                  <option value="DOCUMENT_VERIFIED">Verification Directive</option>
                </select>
              </div>
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">Broadcast Title</label>
              <input
                type="text"
                placeholder="e.g. National Scholarship Portal Maintenance Notice"
                value={newBroadcast.title}
                onChange={(e) => setNewBroadcast({ ...newBroadcast, title: e.target.value })}
                className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-1">Message Content</label>
              <textarea
                rows={4}
                placeholder="Type official notification message..."
                value={newBroadcast.message}
                onChange={(e) => setNewBroadcast({ ...newBroadcast, message: e.target.value })}
                className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>

            <button
              type="submit"
              className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-sm transition flex items-center gap-2"
            >
              <Send className="h-4 w-4" />
              Dispatch Live Notification
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
