import React, { useState, useEffect, useMemo, useCallback } from "react";
import { useToast } from "@components/ui/ToastNotification";
import {
  Search,
  ChevronDown,
  ChevronUp,
  Download,
  CheckCircle,
  XCircle,
  UserCheck,
  Filter,
  CheckSquare,
  Square,
  ArrowUpDown,
  AlertTriangle,
  Sparkles,
  RefreshCw,
  FolderOpen
} from "lucide-react";
import adminService from "@services/adminService";
import { useApp } from "@context/AppContext";

export default function ApplicationsManagement({ applications = [], updateApplicationStatus, onSelectApplication }) {
  const { showToast } = useToast();
  const { refreshApplications } = useApp();
  const [search, setSearch] = useState("");
  const [stageFilter, setStageFilter] = useState("all");
  const [initialLoading, setInitialLoading] = useState(() => {
    return !applications || applications.length === 0;
  });
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [selectedIds, setSelectedIds] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const mapApplication = useCallback((app) => ({
    id: app.applicationNumber || app.id,
    applicationId: app.id || app.applicationId,
    applicationNumber: app.applicationNumber || app.id,
    applicantName: app.applicantName || app.userName || (app.userId ? `Citizen #${app.userId}` : "Citizen"),
    applicantState: app.state || app.applicantState || "National",
    applicantIncome: app.income ? `₹${Number(app.income).toLocaleString("en-IN")}` : (app.applicantIncome || "Declared"),
    applicantCaste: app.casteCategory || app.caste || app.applicantCaste || "General",
    schemeId: app.schemeId,
    schemeCode: app.schemeCode,
    schemeName: (typeof app.schemeTitle === "string" ? app.schemeTitle : (app.schemeTitle?.english || app.schemeTitle?.hindi)) || app.schemeName || app.schemeCode || "Government Scheme",
    ministry: app.ministry || "Government of India",
    currentStage: app.status ? String(app.status).replace(/_/g, " ") : (app.currentStage || "SUBMITTED"),
    status: app.status ? String(app.status).toUpperCase().replace(/ /g, "_") : "SUBMITTED",
    appliedDate: app.submittedAt ? new Date(app.submittedAt).toLocaleDateString("en-IN") : (app.createdAt ? new Date(app.createdAt).toLocaleDateString("en-IN") : (app.appliedDate || "Recent")),
    officer: app.assignedOfficer || app.officer || "Pending Assignment",
    rawApp: app
  }), []);

  // Initial load check - only run ONCE on mount if applications are not loaded
  useEffect(() => {
    let isMounted = true;
    if (!applications || applications.length === 0) {
      if (typeof refreshApplications === "function") {
        refreshApplications()
          .catch((err) => {
            console.warn("Initial load applications error:", err);
            if (isMounted) setError("Failed to load applications queue.");
          })
          .finally(() => {
            if (isMounted) setInitialLoading(false);
          });
      } else {
        setInitialLoading(false);
      }
    } else {
      setInitialLoading(false);
    }
    return () => { isMounted = false; };
  }, []); // Run ONLY once on mount

  // Clear initialLoading when applications prop is received from parent
  useEffect(() => {
    if (Array.isArray(applications)) {
      setInitialLoading(false);
      setError(null);
    }
  }, [applications]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    setError(null);
    try {
      if (typeof refreshApplications === "function") {
        await refreshApplications();
      }
    } catch (err) {
      console.error("Refresh applications error:", err);
      showToast("error", "Refresh Failed", "Could not refresh applications queue.");
      setError("Failed to refresh applications queue.");
    } finally {
      setIsRefreshing(false);
    }
  };

  const allMappedApps = useMemo(() => {
    if (!Array.isArray(applications)) return [];
    return applications.map(mapApplication);
  }, [applications, mapApplication]);

  const stageFilteredApps = useMemo(() => {
    if (stageFilter === "all") return allMappedApps;
    const target = stageFilter.toUpperCase().replace(/ /g, "_");
    return allMappedApps.filter((app) => {
      const st = String(app.status || app.currentStage || "").toUpperCase().replace(/ /g, "_");
      return st === target;
    });
  }, [allMappedApps, stageFilter]);

  const filteredApps = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return stageFilteredApps;
    return stageFilteredApps.filter((app) =>
      app.applicantName?.toLowerCase().includes(q) ||
      app.id?.toLowerCase().includes(q) ||
      app.applicationNumber?.toLowerCase().includes(q) ||
      app.schemeName?.toLowerCase().includes(q) ||
      app.schemeCode?.toLowerCase().includes(q)
    );
  }, [stageFilteredApps, search]);

  const totalElements = filteredApps.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));

  const paginatedApps = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredApps.slice(start, start + pageSize);
  }, [filteredApps, currentPage, pageSize]);

  useEffect(() => {
    setCurrentPage(1);
  }, [stageFilter, search, pageSize]);

  const handleSelectAll = () => {
    if (selectedIds.length === filteredApps.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(filteredApps.map((a) => a.id));
    }
  };

  const handleSelectOne = (id) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter((x) => x !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };

  const handleExport = async () => {
    try {
      const csv = await adminService.downloadAdminReport("applications");
      if (csv) {
        const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute("download", `applications_queue_${new Date().toISOString().split("T")[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showToast("success", "Export Complete", "Official application review queue exported.");
      }
    } catch (err) {
      showToast("error", "Export Error", "Failed to download applications CSV.");
    }
  };

  const statusColors = {
    SUBMITTED: "bg-indigo-50 text-indigo-700 border-indigo-200",
    "UNDER REVIEW": "bg-purple-50 text-purple-700 border-purple-200",
    APPROVED: "bg-emerald-50 text-emerald-700 border-emerald-200",
    REJECTED: "bg-rose-50 text-rose-700 border-rose-200",
    "CORRECTION REQUIRED": "bg-amber-50 text-amber-700 border-amber-200",
    "DOCUMENTS PENDING": "bg-slate-100 text-slate-700 border-slate-200",
    "READY FOR SUBMISSION": "bg-blue-50 text-blue-700 border-blue-200",
    "READY TO SUBMIT": "bg-blue-50 text-blue-700 border-blue-200",
    "DOCUMENT VERIFICATION": "bg-purple-50 text-purple-700 border-purple-200"
  };

  return (
    <div className="space-y-4">
      {/* Filter / Search Bar */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-2xl shadow-sm space-y-3">
        <div className="flex flex-col md:flex-row gap-3 items-center justify-between">
          <div className="flex-1 relative w-full">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search applications by ID, citizen name, or scheme..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100/50 dark:hover:bg-slate-700/50 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 transition"
            />
          </div>

          <div className="flex flex-wrap gap-2 items-center">
            <select
              value={stageFilter}
              onChange={(e) => setStageFilter(e.target.value)}
              className="px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-600 cursor-pointer"
            >
              <option value="all">All Stages</option>
              <option value="documents_pending">Documents Pending</option>
              <option value="document_verification">Document Verification</option>
              <option value="ready_for_submission">Ready to Submit</option>
              <option value="submitted">Submitted</option>
              <option value="under_review">Under Review</option>
              <option value="approved">Approved</option>
              <option value="rejected">Rejected</option>
              <option value="correction_required">Correction Required</option>
            </select>

            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="p-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 rounded-xl transition disabled:opacity-50"
              title="Refresh Applications"
            >
              <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            </button>

            <button
              onClick={handleExport}
              className="flex items-center gap-1.5 px-3 py-2 bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 border border-indigo-200 dark:border-indigo-800/60 rounded-xl text-xs font-bold transition"
            >
              <Download className="h-3.5 w-3.5" />
              Export CSV
            </button>
          </div>
        </div>
      </div>

      {/* Applications Table */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
        {initialLoading ? (
          <div className="p-12 text-center text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center justify-center gap-2">
            <RefreshCw className="h-4 w-4 animate-spin text-indigo-600" />
            Loading Live Applications Review Queue...
          </div>
        ) : error && filteredApps.length === 0 ? (
          <div className="p-16 flex flex-col items-center justify-center text-center space-y-3">
            <AlertTriangle className="h-10 w-10 text-rose-500" />
            <div>
              <p className="text-sm font-bold text-slate-800 dark:text-slate-200">Failed to Load Applications</p>
              <p className="text-xs text-rose-500 dark:text-rose-400 mt-1 max-w-sm">
                {error}
              </p>
            </div>
            <button
              onClick={handleRefresh}
              className="mt-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 transition flex items-center gap-1.5"
            >
              <RefreshCw className="h-3.5 w-3.5" /> Retry
            </button>
          </div>
        ) : filteredApps.length === 0 ? (
          <div className="p-16 flex flex-col items-center justify-center text-center space-y-3">
            <FolderOpen className="h-10 w-10 text-slate-400 dark:text-slate-500" />
            <div>
              <p className="text-sm font-bold text-slate-800 dark:text-slate-200">No Applications Found</p>
              <p className="text-xs text-slate-400 dark:text-slate-400 mt-1 max-w-sm">
                No citizen applications in the database match your current filters.
              </p>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50/70 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider text-[10px]">
                <tr>
                  <th className="px-5 py-3.5">Application ID</th>
                  <th className="px-5 py-3.5">Citizen Profile</th>
                  <th className="px-5 py-3.5">Scheme Details</th>
                  <th className="px-5 py-3.5">Submission Date</th>
                  <th className="px-5 py-3.5">Lifecycle Stage</th>
                  <th className="px-5 py-3.5 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800 font-semibold text-slate-700 dark:text-slate-300">
                {paginatedApps.map((app) => {
                  const sColor = statusColors[app.currentStage] || statusColors.SUBMITTED;
                  return (
                    <tr key={app.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/60 transition duration-150">
                      <td className="px-5 py-3.5 font-bold text-slate-900 dark:text-slate-100 font-mono">
                        {app.id}
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="font-bold text-slate-800 dark:text-slate-100">{app.applicantName}</div>
                        <div className="text-[10px] text-slate-400 dark:text-slate-400 font-semibold">
                          {app.applicantState} • {app.applicantCaste}
                        </div>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="font-bold text-slate-800 dark:text-slate-100 line-clamp-1">{app.schemeName}</div>
                        <div className="text-[10px] text-slate-400 dark:text-slate-400">{app.schemeCode}</div>
                      </td>
                      <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400 whitespace-nowrap text-[11px]">
                        {app.appliedDate}
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border uppercase ${sColor}`}>
                          {app.currentStage}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        <button
                          onClick={() => onSelectApplication && onSelectApplication({
                            ...(app.rawApp || {}),
                            ...app,
                            id: app.applicationId || app.rawApp?.id || app.id,
                            applicationId: app.applicationId || app.rawApp?.id || app.id,
                            applicationNumber: app.applicationNumber || app.rawApp?.applicationNumber
                          })}
                          className="px-3 py-1.5 bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 border border-indigo-200 dark:border-indigo-800/60 rounded-xl text-xs font-bold transition"
                        >
                          Review
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {!initialLoading && filteredApps.length > 0 && (
          <div className="px-5 py-3.5 border-t border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
            <span className="text-slate-500 dark:text-slate-400 font-semibold">
              Showing {totalElements === 0 ? 0 : (currentPage - 1) * pageSize + 1} to{" "}
              {Math.min(currentPage * pageSize, totalElements)} of {totalElements} applications
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
                Page {currentPage} of {totalPages}
              </span>
              <button
                disabled={currentPage >= totalPages}
                onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
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
