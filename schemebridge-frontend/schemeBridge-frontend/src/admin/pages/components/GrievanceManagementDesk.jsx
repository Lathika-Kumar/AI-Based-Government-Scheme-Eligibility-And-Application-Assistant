import React, { useState, useEffect, useCallback } from "react";
import { useToast } from "@components/ui/ToastNotification";
import {
  MessageSquare,
  AlertTriangle,
  Clock,
  Send,
  User,
  Paperclip,
  CheckCircle,
  TrendingUp,
  Search,
  ChevronDown,
  ChevronUp,
  Users,
  UserPlus,
  FileText,
  RefreshCw
} from "lucide-react";
import grievanceService from "@services/grievanceService";
import userAdminService from "@services/userAdminService";

const FALLBACK_OFFICERS = [
  "Verification Officer",
  "Priya Patel",
  "Amit Singh",
  "Sanjay Kumar",
  "Neha Sharma"
];

export default function GrievanceManagementDesk() {
  const { showToast } = useToast();
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("all");
  const [expandedId, setExpandedId] = useState(null);
  const [replyText, setReplyText] = useState("");
  const [grievancesList, setGrievancesList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [pagination, setPagination] = useState({ page: 0, totalPages: 1, totalElements: 0 });
  const [availableOfficers, setAvailableOfficers] = useState(FALLBACK_OFFICERS);

  // Load live verification officers from Oracle directory
  useEffect(() => {
    let isMounted = true;
    (async () => {
      try {
        const res = await userAdminService.getAdminUsers({ role: "ROLE_VERIFICATION_OFFICER", size: 20 });
        if (isMounted && !res.error && res.data?.content && res.data.content.length > 0) {
          const names = res.data.content.map(u => {
            const fullName = `${u.firstName || ""} ${u.lastName || ""}`.trim();
            return fullName || u.email;
          }).filter(Boolean);
          if (names.length > 0) {
            setAvailableOfficers(names);
          }
        }
      } catch (err) {
        // keep fallback
      }
    })();
    return () => { isMounted = false; };
  }, []);

  const fetchGrievances = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const statusParam = filter !== "all" ? filter.toUpperCase().replace(" ", "_") : undefined;
      const res = await grievanceService.getAdminGrievances({
        status: statusParam,
        search: search.trim() || undefined,
        page: currentPage - 1,
        size: pageSize,
      });

      if (!res.error && res.data) {
        setGrievancesList(res.data.content || []);
        setPagination({
          page: res.data.page ?? 0,
          totalPages: res.data.totalPages ?? Math.max(1, Math.ceil((res.data.totalElements || 0) / pageSize)),
          totalElements: res.data.totalElements ?? (res.data.content || []).length
        });
        setError(null);
      } else {
        const errMsg = res?.message || "Failed to load grievances queue.";
        setError(errMsg);
        showToast("error", "Failed to load grievances", errMsg);
      }
    } catch (err) {
      console.error("Grievance fetch error", err);
      const errMsg = err?.message || "Failed to load grievances queue.";
      setError(errMsg);
      showToast("error", "Failed to load grievances", errMsg);
    } finally {
      setLoading(false);
    }
  }, [filter, search, currentPage, pageSize, showToast]);

  useEffect(() => {
    setCurrentPage(1);
  }, [filter, search]);

  useEffect(() => {
    fetchGrievances();
  }, [fetchGrievances]);

  const handleReplySubmit = async (grievanceId) => {
    if (!replyText.trim()) return;
    try {
      const res = await grievanceService.adminReplyToGrievance(grievanceId, replyText);
      if (!res.error) {
        showToast("success", "Reply Sent", "Your response has been communicated to the citizen.");
        setReplyText("");
        fetchGrievances();
      } else {
        showToast("error", "Reply Failed", res.message);
      }
    } catch (err) {
      showToast("error", "Error", "Failed to send reply.");
    }
  };

  const handleResolve = async (grievanceId) => {
    try {
      const res = await grievanceService.resolveGrievance(grievanceId, "Grievance investigated and resolved by administrator.");
      if (!res.error) {
        showToast("success", "Grievance Resolved", `Ticket ${grievanceId} marked as RESOLVED.`);
        fetchGrievances();
      } else {
        showToast("error", "Resolution Failed", res.message);
      }
    } catch (err) {
      showToast("error", "Error", "Failed to resolve grievance.");
    }
  };

  const handleAssignOfficer = async (grievanceId, officerName) => {
    try {
      const res = await grievanceService.assignGrievance(grievanceId, officerName, `Assigned to ${officerName}`);
      if (!res.error) {
        showToast("success", "Officer Assigned", `Ticket assigned to ${officerName}.`);
        fetchGrievances();
      } else {
        showToast("error", "Assignment Failed", res.message);
      }
    } catch (err) {
      showToast("error", "Error", "Failed to assign officer.");
    }
  };

  const priorityStyles = {
    CRITICAL: "bg-rose-50 text-rose-700 border-rose-200 animate-pulse",
    HIGH: "bg-amber-50 text-amber-700 border-amber-200",
    MEDIUM: "bg-blue-50 text-blue-700 border-blue-200",
    LOW: "bg-slate-100 text-slate-600 border-slate-200"
  };

  const statusStyles = {
    OPEN: "bg-indigo-50 text-indigo-700 border-indigo-200",
    IN_PROGRESS: "bg-purple-50 text-purple-700 border-purple-200",
    RESOLVED: "bg-emerald-50 text-emerald-700 border-emerald-200",
    CLOSED: "bg-slate-50 text-slate-600 border-slate-200",
    REJECTED: "bg-rose-50 text-rose-700 border-rose-200"
  };

  return (
    <div className="space-y-4">
      {/* ── Search & Filter Panel ── */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-4 rounded-2xl shadow-sm flex flex-col md:flex-row gap-3 items-center justify-between">
        <div className="flex-1 relative w-full">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search tickets by ID, subject, or citizen ID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100/50 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-600 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500 transition"
          />
        </div>
        <div className="flex items-center gap-2">
          <div className="flex bg-slate-100 dark:bg-slate-800 p-1 rounded-xl select-none text-[11px] font-bold shrink-0">
            {["all", "open", "in_progress", "resolved"].map((f) => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`px-3 py-1.5 rounded-lg capitalize transition ${
                  filter === f ? "bg-white dark:bg-slate-700 text-slate-900 dark:text-slate-100 shadow-sm" : "text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200"
                }`}
              >
                {f.replace("_", " ")}
              </button>
            ))}
          </div>
          <button
            onClick={fetchGrievances}
            className="p-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-600 rounded-xl transition"
            title="Refresh"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* ── Ticket Listing Rows ── */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center justify-center gap-2">
            <RefreshCw className="h-4 w-4 animate-spin text-indigo-600" />
            Loading Live Grievances...
          </div>
        ) : error ? (
          <div className="p-16 flex flex-col items-center justify-center text-center space-y-3">
            <AlertTriangle className="h-10 w-10 text-rose-500" />
            <div>
              <p className="text-sm font-bold text-slate-800 dark:text-slate-200">Failed to Load Grievances</p>
              <p className="text-xs text-rose-500 mt-1 max-w-sm">{error}</p>
            </div>
            <button
              onClick={fetchGrievances}
              className="mt-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 transition flex items-center gap-1.5"
            >
              <RefreshCw className="h-3.5 w-3.5" /> Retry
            </button>
          </div>
        ) : grievancesList.length === 0 ? (
          <div className="p-16 flex flex-col items-center justify-center text-center space-y-3">
            <MessageSquare className="h-10 w-10 text-slate-400 dark:text-slate-500" />
            <div>
              <p className="text-sm font-bold text-slate-800 dark:text-slate-200">No Grievances Found</p>
              <p className="text-xs text-slate-400 dark:text-slate-400 leading-normal max-w-sm mt-0.5">
                All citizen grievance requests are sorted. No pending tickets matching filters.
              </p>
            </div>
          </div>
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {grievancesList.map((g) => {
              const isExpanded = expandedId === g.id;
              const pStyle = priorityStyles[g.priority] || priorityStyles.MEDIUM;
              const sStyle = statusStyles[g.status] || statusStyles.OPEN;

              return (
                <div key={g.id} className="transition duration-150">
                  <div
                    onClick={() => setExpandedId(isExpanded ? null : g.id)}
                    className="p-5 flex flex-col lg:flex-row lg:items-center justify-between gap-4 cursor-pointer hover:bg-slate-50/70 dark:hover:bg-slate-800/50"
                  >
                    <div className="flex items-start gap-4">
                      <div className="h-10 w-10 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-100 dark:border-indigo-800/60 flex items-center justify-center shrink-0">
                        <MessageSquare className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
                      </div>
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-slate-900 dark:text-slate-100">{g.grievanceNumber || g.id}</span>
                          <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold border ${pStyle}`}>
                            {g.priority}
                          </span>
                          <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold border ${sStyle}`}>
                            {g.status}
                          </span>
                        </div>
                        <div className="text-xs font-semibold text-slate-800 dark:text-slate-200">{g.subject}</div>
                        <div className="text-[11px] text-slate-500 dark:text-slate-400">
                          Citizen ID: {g.userId} | Category: {g.category}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-4 self-end lg:self-auto">
                      <div className="text-right">
                        <div className="text-[11px] font-bold text-slate-700 dark:text-slate-300">
                          {g.assignedTo ? `Assigned: ${g.assignedTo}` : "Unassigned"}
                        </div>
                        <div className="text-[10px] text-slate-400 dark:text-slate-400">
                          {g.createdAt ? new Date(g.createdAt).toLocaleDateString("en-IN") : ""}
                        </div>
                      </div>
                      {isExpanded ? (
                        <ChevronUp className="h-4 w-4 text-slate-400" />
                      ) : (
                        <ChevronDown className="h-4 w-4 text-slate-400" />
                      )}
                    </div>
                  </div>

                  {/* Expanded Detail Panel */}
                  {isExpanded && (
                    <div className="p-6 bg-slate-50/50 dark:bg-slate-800/30 border-t border-slate-100 dark:border-slate-800 space-y-6">
                      <div className="space-y-2">
                        <h4 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">Citizen Description</h4>
                        <div className="p-4 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-700 dark:text-slate-300 leading-relaxed">
                          {g.description}
                        </div>
                      </div>

                      {/* Timeline entries */}
                      {g.timeline && g.timeline.length > 0 && (
                        <div className="space-y-2">
                          <h4 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">Resolution History</h4>
                          <div className="space-y-2">
                            {g.timeline.map((t, i) => (
                              <div key={i} className="p-3 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs flex justify-between items-center">
                                <div>
                                  <span className="font-bold text-slate-800 dark:text-slate-200">{t.action}: </span>
                                  <span className="text-slate-600 dark:text-slate-300">{t.details}</span>
                                </div>
                                <span className="text-[10px] text-slate-400 dark:text-slate-400 font-bold">
                                  {t.performedBy} ({new Date(t.timestamp).toLocaleTimeString("en-IN")})
                                </span>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Actions */}
                      <div className="space-y-3">
                        <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">Respond to Citizen</h4>
                        <div className="flex gap-2">
                          <input
                            type="text"
                            placeholder="Type official reply to citizen..."
                            value={replyText}
                            onChange={(e) => setReplyText(e.target.value)}
                            className="flex-1 px-4 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-600 rounded-xl text-xs text-slate-800 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                          />
                          <button
                            onClick={() => handleReplySubmit(g.id)}
                            className="px-4 py-2 bg-indigo-600 text-white font-bold text-xs rounded-xl hover:bg-indigo-700 transition flex items-center gap-1"
                          >
                            <Send className="h-3.5 w-3.5" />
                            Send Reply
                          </button>
                        </div>

                        <div className="flex flex-wrap gap-2 pt-2 border-t border-slate-200">
                          {availableOfficers.map((officer) => (
                            <button
                              key={officer}
                              onClick={() => handleAssignOfficer(g.id, officer)}
                              className="px-3 py-1.5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-600 hover:border-indigo-300 dark:hover:border-indigo-500 text-slate-700 dark:text-slate-300 hover:text-indigo-700 dark:hover:text-indigo-400 text-xs font-semibold rounded-lg transition"
                            >
                              Assign to {officer}
                            </button>
                          ))}
                          {g.status !== "RESOLVED" && (
                            <button
                              onClick={() => handleResolve(g.id)}
                              className="px-4 py-1.5 bg-emerald-600 text-white text-xs font-bold rounded-lg hover:bg-emerald-700 transition ml-auto flex items-center gap-1"
                            >
                              <CheckCircle className="h-3.5 w-3.5" />
                              Mark Resolved
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Pagination Bar */}
        {!loading && !error && grievancesList.length > 0 && (
          <div className="px-5 py-3.5 border-t border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
            <span className="text-slate-500 dark:text-slate-400 font-semibold">
              Showing {pagination.totalElements === 0 ? 0 : (currentPage - 1) * pageSize + 1} to{" "}
              {Math.min(currentPage * pageSize, pagination.totalElements)} of {pagination.totalElements} grievances
            </span>
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1 mr-2">
                <span className="text-slate-400 font-bold text-[10px] uppercase">Per page:</span>
                <select
                  value={pageSize}
                  onChange={(e) => {
                    setPageSize(Number(e.target.value));
                    setCurrentPage(1);
                  }}
                  className="text-xs border border-slate-200 dark:border-slate-700 rounded-lg px-2 py-1 bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 font-bold focus:outline-none cursor-pointer"
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
              </div>
              <button
                disabled={currentPage <= 1}
                onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                className="px-3 py-1.5 font-bold border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-40 transition"
              >
                Previous
              </button>
              <span className="px-3 py-1 bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-100 dark:border-indigo-800/60 text-indigo-700 dark:text-indigo-300 font-bold rounded-lg text-xs">
                Page {currentPage} of {pagination.totalPages}
              </span>
              <button
                disabled={currentPage >= pagination.totalPages}
                onClick={() => setCurrentPage((prev) => Math.min(pagination.totalPages, prev + 1))}
                className="px-3 py-1.5 font-bold border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-40 transition"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
