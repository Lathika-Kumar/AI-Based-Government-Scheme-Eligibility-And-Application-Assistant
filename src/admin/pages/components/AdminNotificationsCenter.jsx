import React, { useState } from "react";
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
  Smartphone
} from "lucide-react";

export default function AdminNotificationsCenter() {
  const [activeTab, setActiveTab] = useState("internal");
  const [selectedRecipient, setSelectedRecipient] = useState("all");

  // Internal Notifications (existing data)
  const [internalNotifications, setInternalNotifications] = useState([
    { id: "admin-notif-1", category: "application", title: "12 Applications Approaching SLA Bounds", body: "Pending applications for Post Matric SC Scholarship are within 24 hours of their SLA target.", time: "10 mins ago", read: false, priority: "high" },
    { id: "admin-notif-2", category: "security", title: "⚠️ Multiple Login Anomaly Flagged", body: "Credential audit flagged duplicate administrative sessions from IP 10.120.4.155.", time: "1 hour ago", read: false, priority: "critical" },
    { id: "admin-notif-3", category: "document", title: "Nodal Verification Batches Success", body: "DigiLocker OCR daemon successfully batch verified 142 citizen identity certificates.", time: "2 hours ago", read: true, priority: "normal" },
    { id: "admin-notif-4", category: "user", title: "New Citizen Registration Spike", body: "Gujarat region logged an 8% rise in farmer registrations over the past 4 hours.", time: "4 hours ago", read: true, priority: "normal" },
    { id: "admin-notif-5", category: "system", title: "API Gateway Integration Active", body: "UCSC evolution and conservation services connected. Latency: 42ms.", time: "1 day ago", read: true, priority: "normal" }
  ]);

  // Broadcast Notifications
  const [broadcasts, setBroadcasts] = useState([
    { id: "broadcast-1", title: "National Scholarship Portal Maintenance", message: "The portal will undergo scheduled maintenance on Sunday from 2 AM to 6 AM IST.", recipients: "All Citizens", channel: "Email + SMS", status: "sent", sentAt: "2026-07-02 10:30" },
    { id: "broadcast-2", title: "Last Date Extended for PM-KISAN", message: "The application deadline for PM-KISAN Yojana has been extended to July 31st.", recipients: "Farmers", channel: "Email", status: "scheduled", sentAt: "2026-07-05 09:00" }
  ]);

  // Announcement Banners
  const [announcements, setAnnouncements] = useState([
    { id: "banner-1", title: "Welcome to SchemeBridge 2.0", content: "New features: AI scheme recommendations, document verification, and faster processing.", status: "active", placement: "top" },
    { id: "banner-2", title: "SLA Performance Award", content: "Congratulations to the Gujarat team for achieving 98% SLA compliance!", status: "inactive", placement: "dashboard" }
  ]);

  // Delivery History
  const [deliveryHistory, setDeliveryHistory] = useState([
    { id: "delivery-1", type: "Email", recipient: "rajesh.kumar@example.com", subject: "Application Status Update", status: "delivered", timestamp: "2026-07-03 09:15" },
    { id: "delivery-2", type: "SMS", recipient: "+91 98765 43210", subject: "OTP Verification", status: "delivered", timestamp: "2026-07-03 08:45" },
    { id: "delivery-3", type: "Email", recipient: "anita.patel@example.com", subject: "Document Request", status: "failed", timestamp: "2026-07-02 16:20" }
  ]);

  // New Broadcast Form
  const [newBroadcast, setNewBroadcast] = useState({
    title: "",
    message: "",
    recipients: "all",
    channels: ["email"],
    scheduledDate: "",
    scheduledTime: ""
  });

  const unreadCount = internalNotifications.filter((n) => !n.read).length;

  const markAllRead = () => {
    setInternalNotifications(internalNotifications.map((n) => ({ ...n, read: true })));
  };

  const handleDismiss = (id) => {
    setInternalNotifications(internalNotifications.filter((n) => n.id !== id));
  };

  const handleSendBroadcast = (e) => {
    e.preventDefault();
    alert("Broadcast scheduled successfully!");
    setNewBroadcast({
      title: "",
      message: "",
      recipients: "all",
      channels: ["email"],
      scheduledDate: "",
      scheduledTime: ""
    });
  };

  const priorityPill = {
    critical: "bg-rose-50 text-rose-700 border-rose-200 animate-pulse",
    high: "bg-amber-50 text-amber-700 border-amber-200",
    normal: "bg-slate-100 text-slate-600 border-slate-200"
  };

  const categoryIcons = {
    application: FileText,
    security: AlertTriangle,
    document: ShieldCheck,
    user: Users,
    system: CheckCircle
  };

  const statusPill = {
    sent: "bg-emerald-50 text-emerald-700 border-emerald-200",
    scheduled: "bg-amber-50 text-amber-700 border-amber-200",
    failed: "bg-rose-50 text-rose-700 border-rose-200",
    active: "bg-indigo-50 text-indigo-700 border-indigo-200",
    inactive: "bg-slate-100 text-slate-600 border-slate-200"
  };

  const tabs = [
    { id: "internal", label: "Internal Notifications", icon: Inbox },
    { id: "broadcast", label: "Broadcast Notifications", icon: Megaphone },
    { id: "announcements", label: "Announcement Banners", icon: Layout },
    { id: "templates", label: "Communication Templates", icon: FileText },
    { id: "history", label: "Delivery History", icon: History }
  ];

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-indigo-50 rounded-xl text-indigo-600">
              <Bell className="h-4.5 w-4.5" />
            </div>
            <div>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Government Communication Center</h3>
              <p className="text-[10px] text-slate-400 font-semibold mt-0.5">
                Manage internal notifications, broadcasts, and citizen communications
              </p>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex mt-4 border-b border-slate-100 overflow-x-auto">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`pb-2.5 px-4 text-xs font-bold border-b-2 transition whitespace-nowrap ${
                  activeTab === tab.id
                    ? "border-indigo-600 text-indigo-700"
                    : "border-transparent text-slate-500 hover:text-slate-700"
                }`}
              >
                <div className="flex items-center gap-2">
                  <Icon className="h-4 w-4" />
                  {tab.label}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Internal Notifications */}
      {activeTab === "internal" && (
        <div className="space-y-4">
          <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm flex items-center justify-between gap-4">
            <div>
              <p className="text-[10px] text-slate-400 font-semibold">
                {unreadCount} unread administrative logs require action
              </p>
            </div>
            {unreadCount > 0 && (
              <button
                onClick={markAllRead}
                className="px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 border border-indigo-100 text-indigo-700 rounded-xl text-xs font-bold transition"
              >
                Mark All as Read
              </button>
            )}
          </div>

          <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden divide-y divide-slate-100">
            {internalNotifications.length === 0 ? (
              <div className="p-16 flex flex-col items-center justify-center text-center space-y-3">
                <Inbox className="h-10 w-10 text-slate-400" />
                <div>
                  <p className="text-sm font-bold text-slate-800">Clear Operations Log</p>
                  <p className="text-xs text-slate-400 leading-normal max-w-sm mt-0.5">
                    All administrative notifications cleared. No alert triggers currently active.
                  </p>
                </div>
              </div>
            ) : (
              internalNotifications.map((notif) => {
                const Icon = categoryIcons[notif.category] || Bell;
                return (
                  <div
                    key={notif.id}
                    className={`p-4 flex gap-4 items-start hover:bg-slate-50/50 transition ${
                      !notif.read ? "bg-indigo-50/20" : ""
                    }`}
                  >
                    <div className="p-2.5 rounded-xl border bg-white shadow-sm shrink-0">
                      <Icon className="h-4.5 w-4.5 text-slate-500" />
                    </div>
                    <div className="space-y-1 flex-1 text-xs">
                      <div className="flex justify-between items-start gap-2 flex-wrap">
                        <div className="flex items-center gap-2">
                          <h4 className="font-bold text-slate-800 text-xs">{notif.title}</h4>
                          <span className={`px-2 py-0.5 rounded-full border text-[8px] font-bold ${priorityPill[notif.priority] || ""}`}>
                            {notif.priority}
                          </span>
                        </div>
                        <span className="text-[10px] text-slate-400 font-semibold">{notif.time}</span>
                      </div>
                      <p className="text-slate-500 font-medium leading-relaxed max-w-2xl">{notif.body}</p>
                    </div>
                    <button
                      onClick={() => handleDismiss(notif.id)}
                      className="p-1 hover:bg-slate-100 rounded text-slate-400 hover:text-rose-600 transition shrink-0 self-start mt-0.5"
                      title="Dismiss alert"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* Broadcast Notifications */}
      {activeTab === "broadcast" && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <div className="lg:col-span-2 space-y-4">
            <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <div className="p-4 border-b border-slate-100">
                <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Recent Broadcasts</h4>
              </div>
              <div className="divide-y divide-slate-100">
                {broadcasts.map((broadcast) => (
                  <div key={broadcast.id} className="p-4 hover:bg-slate-50/50 transition">
                    <div className="flex justify-between items-start gap-4 mb-2">
                      <div>
                        <h5 className="font-bold text-slate-800 text-xs">{broadcast.title}</h5>
                        <p className="text-[10px] text-slate-500 font-medium mt-0.5">{broadcast.recipients}</p>
                      </div>
                      <div className="flex flex-col items-end gap-1">
                        <span className={`px-2 py-0.5 rounded-full border text-[8px] font-bold ${statusPill[broadcast.status] || ""}`}>
                          {broadcast.status}
                        </span>
                        <span className="text-[9px] text-slate-400 font-medium">{broadcast.sentAt}</span>
                      </div>
                    </div>
                    <p className="text-xs text-slate-600 font-medium mb-2">{broadcast.message}</p>
                    <div className="flex items-center gap-2 text-[10px] text-slate-400 font-medium">
                      <span className="flex items-center gap-1">
                        {broadcast.channel.includes("Email") && <Mail className="h-3 w-3" />}
                        {broadcast.channel.includes("SMS") && <Smartphone className="h-3 w-3" />}
                        {broadcast.channel}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <form onSubmit={handleSendBroadcast} className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
              <div className="border-b border-slate-100 pb-2 mb-4">
                <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2">
                  <Send className="h-4 w-4" />
                  New Broadcast
                </h4>
              </div>
              <div className="space-y-3">
                <div>
                  <label className="block text-slate-500 text-[10px] mb-1 font-semibold">Title</label>
                  <input
                    type="text"
                    value={newBroadcast.title}
                    onChange={(e) => setNewBroadcast({ ...newBroadcast, title: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl text-xs font-semibold focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    placeholder="Broadcast title..."
                  />
                </div>

                <div>
                  <label className="block text-slate-500 text-[10px] mb-1 font-semibold">Message</label>
                  <textarea
                    value={newBroadcast.message}
                    onChange={(e) => setNewBroadcast({ ...newBroadcast, message: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl text-xs font-semibold focus:outline-none focus:ring-1 focus:ring-indigo-500 min-h-[100px]"
                    placeholder="Enter your broadcast message here..."
                  />
                </div>

                <div>
                  <label className="block text-slate-500 text-[10px] mb-1 font-semibold">Recipients</label>
                  <select
                    value={newBroadcast.recipients}
                    onChange={(e) => setNewBroadcast({ ...newBroadcast, recipients: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl text-xs font-semibold focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  >
                    <option value="all">All Citizens</option>
                    <option value="farmers">Farmers</option>
                    <option value="students">Students</option>
                    <option value="seniors">Senior Citizens</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-500 text-[10px] mb-1 font-semibold">Channels</label>
                  <div className="flex gap-2">
                    <label className="flex items-center gap-1 text-[10px] font-semibold text-slate-600 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={newBroadcast.channels.includes("email")}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setNewBroadcast({ ...newBroadcast, channels: [...newBroadcast.channels, "email"] });
                          } else {
                            setNewBroadcast({ ...newBroadcast, channels: newBroadcast.channels.filter(c => c !== "email") });
                          }
                        }}
                      />
                      <Mail className="h-3 w-3" />
                      Email
                    </label>
                    <label className="flex items-center gap-1 text-[10px] font-semibold text-slate-600 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={newBroadcast.channels.includes("sms")}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setNewBroadcast({ ...newBroadcast, channels: [...newBroadcast.channels, "sms"] });
                          } else {
                            setNewBroadcast({ ...newBroadcast, channels: newBroadcast.channels.filter(c => c !== "sms") });
                          }
                        }}
                      />
                      <Smartphone className="h-3 w-3" />
                      SMS
                    </label>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-slate-500 text-[10px] mb-1 font-semibold">Date</label>
                    <input
                      type="date"
                      value={newBroadcast.scheduledDate}
                      onChange={(e) => setNewBroadcast({ ...newBroadcast, scheduledDate: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl text-xs font-semibold focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-500 text-[10px] mb-1 font-semibold">Time</label>
                    <input
                      type="time"
                      value={newBroadcast.scheduledTime}
                      onChange={(e) => setNewBroadcast({ ...newBroadcast, scheduledTime: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl text-xs font-semibold focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  className="w-full px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1.5"
                >
                  <Send className="h-4 w-4" />
                  {newBroadcast.scheduledDate && newBroadcast.scheduledTime ? "Schedule Broadcast" : "Send Broadcast"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Announcement Banners */}
      {activeTab === "announcements" && (
        <div className="space-y-4">
          <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100">
              <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Banner Management</h4>
            </div>
            <div className="divide-y divide-slate-100">
              {announcements.map((banner) => (
                <div key={banner.id} className="p-4 hover:bg-slate-50/50 transition">
                  <div className="flex justify-between items-start gap-4 mb-2">
                    <div>
                      <h5 className="font-bold text-slate-800 text-xs">{banner.title}</h5>
                      <p className="text-[10px] text-slate-500 font-medium mt-0.5">Placement: {banner.placement}</p>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full border text-[8px] font-bold ${statusPill[banner.status] || ""}`}>
                      {banner.status}
                    </span>
                  </div>
                  <p className="text-xs text-slate-600 font-medium mb-2">{banner.content}</p>
                  <div className="flex gap-2">
                    <button className="px-3 py-1 bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 rounded-lg text-[10px] font-bold transition">
                      <Eye className="h-3 w-3 inline mr-1" />
                      Preview
                    </button>
                    <button className="px-3 py-1 bg-indigo-50 hover:bg-indigo-100 border border-indigo-100 text-indigo-700 rounded-lg text-[10px] font-bold transition">
                      Edit
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Communication Templates */}
      {activeTab === "templates" && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
            <div className="border-b border-slate-100 pb-2 mb-4">
              <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2">
                <Mail className="h-4 w-4" />
                Email Templates
              </h4>
            </div>
            <div className="space-y-3">
              <div className="p-3 border border-slate-200 bg-slate-50 rounded-lg">
                <h5 className="font-bold text-slate-800 text-xs">Application Approved</h5>
                <p className="text-[10px] text-slate-500 mt-1">Notifies citizen of successful application</p>
                <button className="mt-2 text-[10px] text-indigo-600 font-bold hover:text-indigo-800">Edit Template</button>
              </div>
              <div className="p-3 border border-slate-200 bg-slate-50 rounded-lg">
                <h5 className="font-bold text-slate-800 text-xs">Application Rejected</h5>
                <p className="text-[10px] text-slate-500 mt-1">Notifies citizen of rejection with reasons</p>
                <button className="mt-2 text-[10px] text-indigo-600 font-bold hover:text-indigo-800">Edit Template</button>
              </div>
              <div className="p-3 border border-slate-200 bg-slate-50 rounded-lg">
                <h5 className="font-bold text-slate-800 text-xs">Document Request</h5>
                <p className="text-[10px] text-slate-500 mt-1">Requests additional documentation</p>
                <button className="mt-2 text-[10px] text-indigo-600 font-bold hover:text-indigo-800">Edit Template</button>
              </div>
            </div>
          </div>

          <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
            <div className="border-b border-slate-100 pb-2 mb-4">
              <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2">
                <Smartphone className="h-4 w-4" />
                SMS Templates (Placeholders)
              </h4>
            </div>
            <div className="space-y-3">
              <div className="p-3 border border-slate-200 bg-slate-50 rounded-lg">
                <h5 className="font-bold text-slate-800 text-xs">OTP Verification</h5>
                <p className="text-[10px] text-slate-500 mt-1">Your OTP for SchemeBridge is: [OTP]</p>
                <button className="mt-2 text-[10px] text-indigo-600 font-bold hover:text-indigo-800">Edit Template</button>
              </div>
              <div className="p-3 border border-slate-200 bg-slate-50 rounded-lg">
                <h5 className="font-bold text-slate-800 text-xs">Status Update</h5>
                <p className="text-[10px] text-slate-500 mt-1">Your [Scheme] application status has been updated</p>
                <button className="mt-2 text-[10px] text-indigo-600 font-bold hover:text-indigo-800">Edit Template</button>
              </div>
              <div className="p-3 border border-slate-200 bg-slate-50 rounded-lg">
                <h5 className="font-bold text-slate-800 text-xs">Payment Received</h5>
                <p className="text-[10px] text-slate-500 mt-1">₹[Amount] has been credited to your account</p>
                <button className="mt-2 text-[10px] text-indigo-600 font-bold hover:text-indigo-800">Edit Template</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Delivery History */}
      {activeTab === "history" && (
        <div className="space-y-4">
          <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 flex justify-between items-center">
              <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Delivery History</h4>
              <button className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 rounded-lg text-[10px] font-bold transition">
                Export CSV
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-xs">
                <thead className="bg-slate-50/70 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-bold text-[9px]">
                  <tr>
                    <th className="px-4 py-3 text-left">Timestamp</th>
                    <th className="px-4 py-3 text-left">Type</th>
                    <th className="px-4 py-3 text-left">Recipient</th>
                    <th className="px-4 py-3 text-left">Subject</th>
                    <th className="px-4 py-3 text-left">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 font-semibold text-slate-700">
                  {deliveryHistory.map((item) => (
                    <tr key={item.id} className="hover:bg-slate-50 transition">
                      <td className="px-4 py-3 text-[10px] text-slate-500">{item.timestamp}</td>
                      <td className="px-4 py-3">
                        {item.type === "Email" ? <Mail className="h-3 w-3 text-slate-500" /> : <Smartphone className="h-3 w-3 text-slate-500" />}
                        <span className="ml-2">{item.type}</span>
                      </td>
                      <td className="px-4 py-3">{item.recipient}</td>
                      <td className="px-4 py-3 max-w-[200px] truncate">{item.subject}</td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-0.5 rounded-full border text-[8px] font-bold ${statusPill[item.status] || ""}`}>
                          {item.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
