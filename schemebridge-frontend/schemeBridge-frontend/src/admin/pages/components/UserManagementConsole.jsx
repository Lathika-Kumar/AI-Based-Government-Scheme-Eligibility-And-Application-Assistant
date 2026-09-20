import React, { useState, useEffect, useMemo, useCallback } from "react";
import { useToast } from "@components/ui/ToastNotification";
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
import userAdminService from "@services/userAdminService";

export default function UserManagementConsole() {
  const { showToast } = useToast();

  const [activeTab, setActiveTab] = useState("all");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedUser, setSelectedUser] = useState(null);
  const [isProfileDrawerOpen, setIsProfileDrawerOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [usersList, setUsersList] = useState([]);
  const [pagination, setPagination] = useState({ page: 0, totalPages: 1, totalElements: 0 });
  const [totalDirectoryUsers, setTotalDirectoryUsers] = useState(null);
  const [roleCounts, setRoleCounts] = useState({});
  const itemsPerPage = 15;

  const tabs = [
    { id: "all", label: "All Users", role: null },
    { id: "citizens", label: "Citizens", role: "ROLE_USER" },
    { id: "officers", label: "Verification Officers", role: "ROLE_VERIFICATION_OFFICER" },
    { id: "managers", label: "Scheme Managers", role: "ROLE_SCHEME_MANAGER" },
    { id: "admins", label: "Administrators", role: "ROLE_ADMIN" }
  ];

  const permissionsMatrix = {
    citizens: ["View Schemes", "Apply for Schemes", "Upload Documents", "Track Applications", "Submit Grievances"],
    officers: ["Verify Documents", "Review Applications", "Update Application Status", "View Citizen Profiles"],
    managers: ["Manage Schemes", "Assign Applications", "Generate Reports", "View Analytics"],
    admins: ["Full System Access", "User Management", "System Configuration", "Audit Logs"]
  };

  const normalizeRole = (r) => (r ? (r.startsWith("ROLE_") ? r : `ROLE_${r}`) : "");

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const activeTabObj = tabs.find(t => t.id === activeTab);
      const role = activeTabObj?.role || undefined;
      const status = statusFilter !== "all" ? statusFilter.toUpperCase() : undefined;

      const res = await userAdminService.getAdminUsers({
        search: search.trim() || undefined,
        role,
        status,
        page: currentPage - 1,
        size: itemsPerPage,
      });

      if (!res.error && res.data) {
        const paged = res.data;
        const mapped = (paged.content || []).map(u => ({
          id: u.id,
          name: `${u.firstName || ''} ${u.lastName || ''}`.trim() || u.email,
          email: u.email,
          phone: u.phoneNumber || "-",
          status: u.accountStatus === "ACTIVE" 
            ? "Active" 
            : u.accountStatus === "LOCKED" 
            ? "Locked" 
            : u.accountStatus === "PENDING_VERIFICATION"
            ? "Pending Verification"
            : "Inactive",
          rawStatus: u.accountStatus,
          roles: u.roles || [],
          state: u.state || "National",
          lastLogin: u.updatedAt ? new Date(u.updatedAt).toLocaleDateString("en-IN") : "Recent",
          createdAt: u.createdAt
        }));
        setUsersList(mapped);
        setPagination({
          page: paged.page,
          totalPages: paged.totalPages || 1,
          totalElements: paged.totalElements || 0
        });
        if (paged.totalUsers != null) {
          setTotalDirectoryUsers(paged.totalUsers);
        }
        if (paged.roleCounts) {
          setRoleCounts(paged.roleCounts);
        }
      } else {
        const errMsg = res.message || "Could not retrieve user directory.";
        setError(errMsg);
        showToast("error", "Failed to load users", errMsg);
      }
    } catch (err) {
      console.error("Error fetching admin users", err);
      const errMsg = err?.message || "Failed to load users from authentication service.";
      setError(errMsg);
      showToast("error", "Error", errMsg);
    } finally {
      setLoading(false);
    }
  }, [activeTab, search, statusFilter, currentPage, showToast]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleViewProfile = (user) => {
    setSelectedUser(user);
    setIsProfileDrawerOpen(true);
  };

  const handleToggleStatus = async (user) => {
    const newStatus = user.rawStatus === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      const res = await userAdminService.updateUserStatus(user.id, newStatus, "Status updated by admin");
      if (!res.error) {
        showToast("success", "Status Updated", `User ${user.name} is now ${newStatus}.`);
        fetchUsers();
      } else {
        showToast("error", "Update Failed", res.message || "Failed to update user status.");
      }
    } catch (err) {
      showToast("error", "Error", "An unexpected error occurred.");
    }
  };

  const handleUnlockUser = async (user) => {
    try {
      const res = await userAdminService.unlockUser(user.id);
      if (!res.error) {
        showToast("success", "Account Unlocked", `User account ${user.email} has been unlocked.`);
        fetchUsers();
      } else {
        showToast("error", "Unlock Failed", res.message || "Failed to unlock account.");
      }
    } catch (err) {
      showToast("error", "Error", "An unexpected error occurred.");
    }
  };

  const handleAssignRole = async (tabId) => {
    if (!selectedUser) return;
    const tab = tabs.find(t => t.id === tabId);
    if (!tab || !tab.role) return;
    const roleToAssign = tab.role;
    try {
      const res = await userAdminService.updateUserRoles(selectedUser.id, [roleToAssign], "Role updated by administrator");
      if (!res.error) {
        showToast("success", "Role Assigned", `Role updated to ${roleToAssign.replace("ROLE_", "")} for ${selectedUser.name}`);
        setIsProfileDrawerOpen(false);
        fetchUsers();
      } else {
        showToast("error", "Role Update Failed", res.message || "Could not update roles.");
      }
    } catch (err) {
      showToast("error", "Error", "Failed to assign role.");
    }
  };

  const exportToCSV = () => {
    const headers = ["ID", "Name", "Email", "Phone", "Status", "Roles", "Last Updated"];
    const csvContent = [
      headers.join(","),
      ...usersList.map((u) => [
        `"${u.id}"`,
        `"${u.name}"`,
        `"${u.email}"`,
        `"${u.phone || ''}"`,
        `"${u.status}"`,
        `"${u.roles.join(';')}"`,
        `"${u.lastLogin}"`
      ].join(","))
    ].join("\n");

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `users_directory_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Skeleton Row
  const SkeletonRow = () => (
    <tr className="animate-pulse">
      <td className="px-5 py-4"><div className="h-4 w-32 bg-slate-100 dark:bg-slate-800 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-48 bg-slate-100 dark:bg-slate-800 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-24 bg-slate-100 dark:bg-slate-800 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-16 bg-slate-100 dark:bg-slate-800 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-28 bg-slate-100 dark:bg-slate-800 rounded"></div></td>
      <td className="px-5 py-4"><div className="h-4 w-24 bg-slate-100 dark:bg-slate-800 rounded"></div></td>
    </tr>
  );

  return (
    <div className="space-y-6">
      {/* Header Section */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
        <div className="flex flex-col md:flex-row gap-4 justify-between items-start md:items-center mb-4">
          <div>
            <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <Users className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
              Identity & Access Management
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
              Live identity administration backed by Oracle 21c security directory ({totalDirectoryUsers != null ? totalDirectoryUsers : pagination.totalElements} users)
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={fetchUsers}
              className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 px-3 py-2 rounded-xl transition"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
            <button
              onClick={exportToCSV}
              className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 px-3 py-2 rounded-xl transition"
            >
              <Download className="h-4 w-4" />
              Export CSV
            </button>
          </div>
        </div>

        {/* Search and Filters */}
        <div className="flex flex-col lg:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search users by name, email, or phone..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100/50 dark:hover:bg-slate-700/50 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 transition"
            />
          </div>
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1 sm:w-48">
              <Filter className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setCurrentPage(1);
                }}
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100/50 dark:hover:bg-slate-700/50 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 dark:text-slate-200 transition appearance-none"
              >
                <option value="all">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive / Suspended</option>
                <option value="LOCKED">Locked</option>
                <option value="PENDING_VERIFICATION">Pending Verification</option>
              </select>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="mt-4 flex border-b border-slate-100 dark:border-slate-800 overflow-x-auto select-none pt-1">
          {tabs.map((tab) => {
            const count = roleCounts[tab.id];
            return (
              <button
                key={tab.id}
                onClick={() => {
                  setActiveTab(tab.id);
                  setCurrentPage(1);
                  setIsProfileDrawerOpen(false);
                }}
                className={`pb-2.5 px-4 text-xs font-bold border-b-2 transition whitespace-nowrap focus:outline-none flex items-center gap-1.5 ${
                  activeTab === tab.id
                    ? "border-indigo-600 text-indigo-700 dark:text-indigo-400"
                    : "border-transparent text-slate-400 hover:text-slate-600 dark:hover:text-slate-300"
                }`}
              >
                <span>{tab.label}</span>
                {count !== undefined && (
                  <span
                    className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                      activeTab === tab.id
                        ? "bg-indigo-100 dark:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300"
                        : "bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400"
                    }`}
                  >
                    {count}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Table Section */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-slate-50/70 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 uppercase tracking-wider font-bold text-[10px]">
              <tr>
                <th className="px-5 py-3">User</th>
                <th className="px-5 py-3">Email</th>
                <th className="px-5 py-3">Phone</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Assigned Roles</th>
                <th className="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800 font-semibold text-slate-700 dark:text-slate-300">
              {loading ? (
                <>
                  <SkeletonRow />
                  <SkeletonRow />
                  <SkeletonRow />
                </>
              ) : error ? (
                <tr>
                  <td colSpan="6" className="px-5 py-12 text-center">
                    <XCircle className="h-10 w-10 text-rose-500 dark:text-rose-400 mx-auto mb-3" />
                    <p className="text-sm font-bold text-slate-800 dark:text-slate-200">Failed to load users</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 mb-4">{error}</p>
                    <button
                      onClick={fetchUsers}
                      className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl text-xs transition inline-flex items-center gap-2"
                    >
                      <RefreshCw className="h-3.5 w-3.5" />
                      Retry Connection
                    </button>
                  </td>
                </tr>
              ) : usersList.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-5 py-12 text-center">
                    <Users className="h-10 w-10 text-slate-400 dark:text-slate-500 mx-auto mb-3" />
                    <p className="text-sm font-bold text-slate-800 dark:text-slate-200">No Users Found</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                      Try adjusting your search query or filters
                    </p>
                  </td>
                </tr>
              ) : (
                usersList.map((u, idx) => (
                  <tr key={u.id || idx} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition">
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-full bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-100 dark:border-indigo-800/60 flex items-center justify-center font-black text-xs uppercase">
                          {u.name ? u.name.charAt(0) : "U"}
                        </div>
                        <div>
                          <div className="font-bold text-slate-800 dark:text-slate-100">{u.name}</div>
                          <div className="text-[10px] text-slate-400 dark:text-slate-400">{u.state || "National"}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-4 text-slate-600 dark:text-slate-300">{u.email}</td>
                    <td className="px-5 py-4 text-slate-600 dark:text-slate-300">{u.phone || "-"}</td>
                    <td className="px-5 py-4">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                        u.rawStatus === "ACTIVE"
                          ? "bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-100 dark:border-emerald-800"
                          : u.rawStatus === "LOCKED"
                          ? "bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border-amber-100 dark:border-amber-800"
                          : u.rawStatus === "PENDING_VERIFICATION"
                          ? "bg-sky-50 dark:bg-sky-950/60 text-sky-700 dark:text-sky-300 border-sky-100 dark:border-sky-800"
                          : "bg-rose-50 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 border-rose-100 dark:border-rose-800"
                      }`}>
                        {u.status}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex flex-wrap gap-1">
                        {u.roles.map((r, i) => (
                          <span key={i} className="px-1.5 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 rounded text-[9px] font-bold">
                            {r.replace("ROLE_", "")}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-5 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {u.rawStatus === "LOCKED" && (
                          <button
                            onClick={() => handleUnlockUser(u)}
                            className="p-1.5 hover:bg-amber-100 dark:hover:bg-amber-900/50 text-amber-700 dark:text-amber-300 rounded-lg transition"
                            title="Unlock Account"
                          >
                            <Unlock className="h-4 w-4" />
                          </button>
                        )}
                        <button
                          onClick={() => handleToggleStatus(u)}
                          className={`p-1.5 rounded-lg transition ${
                            u.rawStatus === "ACTIVE"
                              ? "hover:bg-rose-50 dark:hover:bg-rose-900/50 text-rose-600 dark:text-rose-400"
                              : "hover:bg-emerald-50 dark:hover:bg-emerald-900/50 text-emerald-600 dark:text-emerald-400"
                          }`}
                          title={u.rawStatus === "ACTIVE" ? "Deactivate User" : "Activate User"}
                        >
                          {u.rawStatus === "ACTIVE" ? <Lock className="h-4 w-4" /> : <Unlock className="h-4 w-4" />}
                        </button>
                        <button
                          onClick={() => handleViewProfile(u)}
                          className="px-3 py-1.5 bg-indigo-50 dark:bg-indigo-950/60 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800/60 font-bold rounded-lg transition text-xs"
                        >
                          Manage
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {pagination.totalPages > 1 && (
          <div className="px-5 py-3.5 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
            <span className="text-xs text-slate-500 dark:text-slate-400 font-semibold">
              Showing page {currentPage} of {pagination.totalPages} ({pagination.totalElements} total records)
            </span>
            <div className="flex items-center gap-2">
              <button
                disabled={currentPage <= 1}
                onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                className="px-3 py-1.5 text-xs font-bold border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-40"
              >
                Previous
              </button>
              <button
                disabled={currentPage >= pagination.totalPages}
                onClick={() => setCurrentPage(prev => prev + 1)}
                className="px-3 py-1.5 text-xs font-bold border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* User Manage Drawer */}
      {isProfileDrawerOpen && selectedUser && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex justify-end">
          <div className="w-full max-w-md bg-white dark:bg-slate-900 border-l border-slate-200 dark:border-slate-800 h-full shadow-2xl p-6 overflow-y-auto space-y-6">
            <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-4">
              <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                <UserCog className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
                User Access Control
              </h4>
              <button
                onClick={() => setIsProfileDrawerOpen(false)}
                className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 rounded-lg"
              >
                <XCircle className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-400">User Details</label>
                <div className="p-3 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl space-y-1 mt-1">
                  <div className="text-xs font-bold text-slate-800 dark:text-slate-100">{selectedUser.name}</div>
                  <div className="text-xs text-slate-600 dark:text-slate-300">{selectedUser.email}</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">Phone: {selectedUser.phone}</div>
                </div>
              </div>

              <div>
                <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-400">Assign Role</label>
                <div className="grid grid-cols-2 gap-2 mt-1">
                  {tabs.filter(t => t.role).map((tab) => (
                    <button
                      key={tab.id}
                      onClick={() => handleAssignRole(tab.id)}
                      className={`p-2.5 rounded-xl border text-xs font-bold text-left transition ${
                        selectedUser.roles.map(normalizeRole).includes(normalizeRole(tab.role))
                          ? "bg-indigo-50 dark:bg-indigo-950/60 border-indigo-300 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300"
                          : "bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700"
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-400">Account Actions</label>
                <div className="space-y-2 mt-1">
                  <button
                    onClick={() => handleToggleStatus(selectedUser)}
                    className={`w-full py-2.5 px-4 rounded-xl text-xs font-bold transition flex items-center justify-center gap-2 ${
                      selectedUser.rawStatus === "ACTIVE"
                        ? "bg-rose-50 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 border border-rose-200 dark:border-rose-800 hover:bg-rose-100 dark:hover:bg-rose-900/60"
                        : "bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800 hover:bg-emerald-100 dark:hover:bg-emerald-900/60"
                    }`}
                  >
                    {selectedUser.rawStatus === "ACTIVE" ? <Lock className="h-4 w-4" /> : <Unlock className="h-4 w-4" />}
                    {selectedUser.rawStatus === "ACTIVE" ? "Deactivate User Account" : "Activate User Account"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
