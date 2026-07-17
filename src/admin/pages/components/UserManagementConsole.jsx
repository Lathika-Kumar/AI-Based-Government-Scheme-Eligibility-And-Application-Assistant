import React, { useState, useMemo } from "react";
import { useApp } from "@context/AppContext";
import {
  Users,
  Search,
  Filter,
  Download,
  User,
  UserCog,
  Lock,
  Unlock,
  ShieldCheck,
  XCircle,
  ArrowLeftRight,
  RefreshCw,
  History,
  MoreVertical
} from "lucide-react";

export default function UserManagementConsole() {
  const { 
    usersRegistry, 
    toggleRegistryUserStatus,
    updateRegistryUser,
    resetUserPassword,
    assignUserRole,
    addAuditLog
  } = useApp();

  const [activeTab, setActiveTab] = useState("citizens");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedUser, setSelectedUser] = useState(null);
  const [isProfileDrawerOpen, setIsProfileDrawerOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const itemsPerPage = 10;

  // Simulate loading state
  React.useEffect(() => {
    const timer = setTimeout(() => {
      setLoading(false);
    }, 800);
    return () => clearTimeout(timer);
  }, []);

  const tabs = [
    { id: "citizens", label: "Citizens" },
    { id: "officers", label: "Verification Officers" },
    { id: "managers", label: "Scheme Managers" },
    { id: "support", label: "Support Officers" },
    { id: "admins", label: "Super Admins" }
  ];

  const permissionsMatrix = {
    citizens: ["View Schemes", "Apply for Schemes", "Upload Documents", "Track Applications", "Submit Grievances"],
    officers: ["Verify Documents", "Review Applications", "Update Application Status", "View Citizen Profiles"],
    managers: ["Manage Schemes", "Assign Applications", "Generate Reports", "View Analytics"],
    support: ["View Grievances", "Respond to Citizens", "Escalate Tickets", "Update Grievance Status"],
    admins: ["Full System Access", "User Management", "System Configuration", "Audit Logs"]
  };

  const activityHistory = [
    { id: 1, timestamp: "2026-07-03 14:30:00", action: "Login", description: "User logged in via OTP" },
    { id: 2, timestamp: "2026-07-03 12:15:00", action: "Application Submitted", description: "Submitted PM-KISAN application" },
    { id: 3, timestamp: "2026-07-02 18:45:00", action: "Document Uploaded", description: "Aadhaar Card uploaded" },
    { id: 4, timestamp: "2026-07-02 10:20:00", action: "Profile Updated", description: "Changed contact number" }
  ];

  const currentList = useMemo(() => {
    const list = usersRegistry?.[activeTab] || [];
    let filtered = list;

    if (search) {
      filtered = filtered.filter(
        (u) =>
          u.name.toLowerCase().includes(search.toLowerCase()) ||
          u.email.toLowerCase().includes(search.toLowerCase()) ||
          (u.phone && u.phone.includes(search))
      );
    }

    if (statusFilter !== "all") {
      filtered = filtered.filter((u) => u.status === statusFilter);
    }

    return filtered;
  }, [usersRegistry, activeTab, search, statusFilter]);

  const paginatedList = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    const end = start + itemsPerPage;
    return currentList.slice(start, end);
  }, [currentList, currentPage]);

  const totalPages = Math.ceil(currentList.length / itemsPerPage);

  const handleViewProfile = (user) => {
    setSelectedUser(user);
    setIsProfileDrawerOpen(true);
  };

  const handleToggleStatus = (user) => {
    toggleRegistryUserStatus(activeTab, user.email);
    addAuditLog("Status Change", "User", user.name, `User ${user.name} status toggled.`);
  };

  const handleResetPassword = () => {
    if (selectedUser) {
      resetUserPassword(selectedUser.email);
      alert(`Temporary password sent to ${selectedUser.email}`);
    }
  };

  const handleAssignRole = (newRole) => {
    if (selectedUser && newRole !== activeTab) {
      assignUserRole(selectedUser.email, activeTab, newRole);
      setIsProfileDrawerOpen(false);
      setActiveTab(newRole);
    }
  };

  const handleEditProfile = (updates) => {
    if (selectedUser) {
      updateRegistryUser(activeTab, selectedUser.email, updates);
      setSelectedUser({ ...selectedUser, ...updates });
    }
  };

  const exportToCSV = () => {
    const headers = ["Name", "Email", "Phone", "Status", "Last Login"];
    const csvContent = [
      headers.join(","),
      ...currentList.map((u) => [
        u.name,
        u.email,
        u.phone || "",
        u.status,
        u.lastLogin
      ].join(","))
    ].join("\n");

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `${activeTab}_users_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Skeleton Loader
  const SkeletonRow = () => (
    <tr className="animate-pulse">
      <td className="px-5 py-4"><div className="h-4 w-32 bg-slate-100 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-48 bg-slate-100 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-24 bg-slate-100 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-16 bg-slate-100 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-28 bg-slate-100 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-24 bg-slate-100 rounded"></div></td>
    </tr>
  );

  return (
    <div className="space-y-6">
      {/* Header Section */}
      <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm">
        <div className="flex flex-col md:flex-row gap-4 justify-between items-start md:items-center mb-4">
          <div>
            <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <Users className="h-5 w-5 text-indigo-600" />
              Identity & Access Management
            </h3>
            <p className="text-xs text-slate-500 mt-1">
              Manage citizen and government officer identities, roles, and permissions
            </p>
          </div>
          <button
            onClick={exportToCSV}
            className="flex items-center gap-2 text-xs font-semibold text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 px-3 py-2 rounded-xl transition"
          >
            <Download className="h-4 w-4" />
            Export CSV
          </button>
        </div>

        {/* Search and Filters */}
        <div className="flex flex-col lg:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search users by name, email, or phone..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 hover:bg-slate-100/50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 transition"
            />
          </div>
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1 sm:w-48">
              <Filter className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 hover:bg-slate-100/50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 transition appearance-none"
              >
                <option value="all">All Statuses</option>
                <option value="Active">Active</option>
                <option value="Suspended">Suspended</option>
              </select>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="mt-4 flex border-b border-slate-100 overflow-x-auto select-none pt-1">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => {
                setActiveTab(tab.id);
                setSearch("");
                setStatusFilter("all");
                setCurrentPage(1);
                setIsProfileDrawerOpen(false);
              }}
              className={`pb-2.5 px-5 text-xs font-bold border-b-2 transition whitespace-nowrap focus:outline-none ${
                activeTab === tab.id
                  ? "border-indigo-600 text-indigo-700"
                  : "border-transparent text-slate-400 hover:text-slate-600"
              }`}
            >
              {tab.label}
              <span className="ml-2 bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded-full text-[10px]">
                {usersRegistry?.[tab.id]?.length || 0}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Table Section */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-slate-50/70 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-bold text-[10px]">
              <tr>
                <th className="px-5 py-3">User</th>
                <th className="px-5 py-3">Email</th>
                <th className="px-5 py-3">Phone</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Last Login</th>
                <th className="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-semibold text-slate-700">
              {loading ? (
                <>
                  <SkeletonRow />
                  <SkeletonRow />
                  <SkeletonRow />
                  <SkeletonRow />
                  <SkeletonRow />
                </>
              ) : paginatedList.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-5 py-12 text-center">
                    <Users className="h-10 w-10 text-slate-400 mx-auto mb-3" />
                    <p className="text-sm font-bold text-slate-800">No Users Found</p>
                    <p className="text-xs text-slate-500 mt-1">
                      {search || statusFilter !== "all"
                        ? "Try adjusting your search or filters"
                        : `No ${tabs.find(t => t.id === activeTab)?.label || "users"} in the system`}
                    </p>
                  </td>
                </tr>
              ) : (
                paginatedList.map((user, idx) => (
                  <tr key={idx} className="hover:bg-slate-50 transition">
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-100 flex items-center justify-center font-black text-xs">
                          {user.name.split(" ").map((x) => x[0]).join("")}
                        </div>
                        <div>
                          <div className="font-bold text-slate-800">{user.name}</div>
                          <div className="text-[10px] text-slate-400">{user.dept || user.occupation}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-4 text-slate-600">{user.email}</td>
                    <td className="px-5 py-4 text-slate-600">{user.phone || "-"}</td>
                    <td className="px-5 py-4">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                        user.status === "Active"
                          ? "bg-emerald-50 text-emerald-700 border-emerald-100"
                          : "bg-rose-50 text-rose-700 border-rose-100"
                      }`}>
                        {user.status}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-slate-600">{user.lastLogin}</td>
                    <td className="px-5 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleViewProfile(user)}
                          className="p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                          title="View Profile"
                        >
                          <User className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => handleToggleStatus(user)}
                          className={`p-1.5 rounded-lg transition ${
                            user.status === "Active"
                              ? "text-rose-500 hover:text-rose-600 hover:bg-rose-50"
                              : "text-emerald-500 hover:text-emerald-600 hover:bg-emerald-50"
                          }`}
                          title={user.status === "Active" ? "Suspend Account" : "Activate Account"}
                        >
                          {user.status === "Active" ? <Lock className="h-4 w-4" /> : <Unlock className="h-4 w-4" />}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {!loading && totalPages > 1 && (
          <div className="border-t border-slate-200 px-5 py-4 flex items-center justify-between">
            <div className="text-xs text-slate-500 font-semibold">
              Showing {((currentPage - 1) * itemsPerPage) + 1} to {Math.min(currentPage * itemsPerPage, currentList.length)} of {currentList.length} users
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                disabled={currentPage === 1}
                className="px-3 py-1.5 text-xs font-bold border border-slate-200 rounded-lg hover:bg-slate-50 transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                let pageNum;
                if (totalPages <= 5) {
                  pageNum = i + 1;
                } else if (currentPage <= 3) {
                  pageNum = i + 1;
                } else if (currentPage >= totalPages - 2) {
                  pageNum = totalPages - 4 + i;
                } else {
                  pageNum = currentPage - 2 + i;
                }
                return (
                  <button
                    key={pageNum}
                    onClick={() => setCurrentPage(pageNum)}
                    className={`px-3 py-1.5 text-xs font-bold rounded-lg transition ${
                      currentPage === pageNum
                        ? "bg-indigo-600 text-white"
                        : "border border-slate-200 hover:bg-slate-50"
                    }`}
                  >
                    {pageNum}
                  </button>
                );
              })}
              <button
                onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
                disabled={currentPage === totalPages}
                className="px-3 py-1.5 text-xs font-bold border border-slate-200 rounded-lg hover:bg-slate-50 transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Profile Drawer */}
      {isProfileDrawerOpen && selectedUser && (
        <div className="fixed inset-0 z-50 flex justify-end">
          {/* Backdrop */}
          <div
            className="absolute inset-0 bg-slate-900/30 backdrop-blur-sm"
            onClick={() => setIsProfileDrawerOpen(false)}
          />

          {/* Drawer Content */}
          <div className="relative w-full max-w-lg bg-white border-l border-slate-200 shadow-2xl overflow-y-auto">
            {/* Drawer Header */}
            <div className="sticky top-0 bg-white border-b border-slate-200 p-5 z-10">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="text-sm font-bold text-slate-900">User Profile</h4>
                  <p className="text-[10px] text-slate-500 mt-1">Manage user details and permissions</p>
                </div>
                <button
                  onClick={() => setIsProfileDrawerOpen(false)}
                  className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition"
                >
                  <XCircle className="h-5 w-5" />
                </button>
              </div>
            </div>

            <div className="p-5 space-y-6">
              {/* User Info */}
              <div className="flex items-center gap-4">
                <div className="h-16 w-16 rounded-2xl bg-indigo-50 text-indigo-700 border border-indigo-100 flex items-center justify-center font-black text-xl">
                  {selectedUser.name.split(" ").map((x) => x[0]).join("")}
                </div>
                <div>
                  <h5 className="text-base font-bold text-slate-900">{selectedUser.name}</h5>
                  <p className="text-xs text-slate-500">{selectedUser.email}</p>
                  <span className={`inline-block mt-2 px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                    selectedUser.status === "Active"
                      ? "bg-emerald-50 text-emerald-700 border-emerald-100"
                      : "bg-rose-50 text-rose-700 border-rose-100"
                  }`}>
                    {selectedUser.status}
                  </span>
                </div>
              </div>

              {/* Quick Actions */}
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={handleResetPassword}
                  className="flex items-center justify-center gap-2 text-xs font-bold bg-slate-50 hover:bg-slate-100 border border-slate-200 text-slate-700 px-4 py-3 rounded-xl transition"
                >
                  <RefreshCw className="h-4 w-4" />
                  Reset Password
                </button>
                <button
                  onClick={() => handleToggleStatus(selectedUser)}
                  className={`flex items-center justify-center gap-2 text-xs font-bold border px-4 py-3 rounded-xl transition ${
                    selectedUser.status === "Active"
                      ? "bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100"
                      : "bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100"
                  }`}
                >
                  {selectedUser.status === "Active" ? <Lock className="h-4 w-4" /> : <Unlock className="h-4 w-4" />}
                  {selectedUser.status === "Active" ? "Suspend" : "Activate"}
                </button>
              </div>

              {/* Edit Profile */}
              <div className="border border-slate-200 rounded-2xl p-4">
                <h6 className="text-xs font-bold text-slate-800 mb-4 flex items-center gap-2">
                  <UserCog className="h-4 w-4 text-slate-500" />
                  Edit Profile
                </h6>
                <div className="space-y-3">
                  <div>
                    <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1">Name</label>
                    <input
                      type="text"
                      defaultValue={selectedUser.name}
                      onBlur={(e) => handleEditProfile({ name: e.target.value })}
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1">Phone</label>
                    <input
                      type="text"
                      defaultValue={selectedUser.phone || ""}
                      onBlur={(e) => handleEditProfile({ phone: e.target.value })}
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                  {selectedUser.dept && (
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1">Department</label>
                      <input
                        type="text"
                        defaultValue={selectedUser.dept}
                        onBlur={(e) => handleEditProfile({ dept: e.target.value })}
                        className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      />
                    </div>
                  )}
                </div>
              </div>

              {/* Role Assignment */}
              <div className="border border-slate-200 rounded-2xl p-4">
                <h6 className="text-xs font-bold text-slate-800 mb-4 flex items-center gap-2">
                  <ArrowLeftRight className="h-4 w-4 text-slate-500" />
                  Assign Role
                </h6>
                <p className="text-[10px] text-slate-500 mb-3">
                  Current role: <span className="font-bold text-slate-700">{tabs.find(t => t.id === activeTab)?.label}</span>
                </p>
                <div className="grid grid-cols-1 gap-2">
                  {tabs.filter(t => t.id !== activeTab).map(tab => (
                    <button
                      key={tab.id}
                      onClick={() => handleAssignRole(tab.id)}
                      className="flex items-center justify-between px-3 py-2.5 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold text-slate-700 transition"
                    >
                      {tab.label}
                      <ArrowLeftRight className="h-3.5 w-3.5 text-slate-400" />
                    </button>
                  ))}
                </div>
              </div>

              {/* Permissions Matrix */}
              <div className="border border-slate-200 rounded-2xl p-4">
                <h6 className="text-xs font-bold text-slate-800 mb-4 flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-slate-500" />
                  Permissions
                </h6>
                <div className="space-y-2">
                  {permissionsMatrix[activeTab]?.map((perm, idx) => (
                    <div key={idx} className="flex items-center gap-2">
                      <div className="h-2 w-2 rounded-full bg-emerald-500" />
                      <span className="text-xs text-slate-600">{perm}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Activity History */}
              <div className="border border-slate-200 rounded-2xl p-4">
                <h6 className="text-xs font-bold text-slate-800 mb-4 flex items-center gap-2">
                  <History className="h-4 w-4 text-slate-500" />
                  Activity History
                </h6>
                <div className="space-y-3">
                  {activityHistory.map(activity => (
                    <div key={activity.id} className="flex gap-3">
                      <div className="flex flex-col items-center">
                        <div className="h-2 w-2 rounded-full bg-indigo-500 mt-1" />
                        <div className="w-0.5 h-full bg-slate-200" />
                      </div>
                      <div className="flex-1 pb-3">
                        <div className="flex justify-between items-start">
                          <p className="text-xs font-bold text-slate-800">{activity.action}</p>
                          <p className="text-[10px] text-slate-400">{activity.timestamp}</p>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5">{activity.description}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
