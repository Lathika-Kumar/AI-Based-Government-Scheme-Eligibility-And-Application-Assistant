import React, { useState, useEffect, useMemo, useCallback } from "react";
import {
  ShieldAlert,
  Search,
  Download,
  AlertTriangle,
  CheckCircle,
  FileText,
  Calendar,
  Clock,
  User,
  Users,
  Settings,
  FileCheck,
  ChevronDown,
  ChevronUp,
  RefreshCw
} from "lucide-react";
import adminService from "@services/adminService";

export default function AuditLogsConsole() {
  const [search, setSearch] = useState("");
  const [filterModule, setFilterModule] = useState("all");
  const [filterSeverity, setFilterSeverity] = useState("all");
  const [auditLogsList, setAuditLogsList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [pagination, setPagination] = useState({ page: 0, totalPages: 1, totalElements: 0 });

  const fetchAuditLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await adminService.getAdminAuditLogs({
        entityType: filterModule !== "all" ? filterModule.toUpperCase() : undefined,
        page: currentPage - 1,
        size: pageSize,
      });

      if (!res.error && res.data) {
        const raw = res.data.content || [];
        const enriched = raw.map((log) => ({
          id: log.id,
          timestamp: log.timestamp || new Date().toISOString(),
          actionType: log.action || "OPERATION",
          entityType: log.entityType || "SYSTEM",
          entityName: log.entityId || "N/A",
          actor: log.actorId || "System Vault",
          role: log.actorRole || "Administrator",
          detail: log.details ? JSON.stringify(log.details) : `Action ${log.action} executed on ${log.entityType}`,
          ip: log.ipAddress || "10.0.0.1",
          device: log.userAgent || "Authorized Admin Console",
          severity: log.action.includes("REJECT") || log.action.includes("DELETE") ? "Warning" : "Success",
          securityFlag: log.action.includes("DELETE") || log.action.includes("LOCKED")
        }));
        setAuditLogsList(enriched);
        setPagination({
          page: res.data.page ?? 0,
          totalPages: res.data.totalPages ?? Math.max(1, Math.ceil((res.data.totalElements || 0) / pageSize)),
          totalElements: res.data.totalElements ?? enriched.length
        });
        setError(null);
      } else {
        setError(res?.message || "Failed to load audit registry.");
      }
    } catch (err) {
      console.error("Failed to load audit logs", err);
      setError(err?.message || "Failed to load audit registry.");
    } finally {
      setLoading(false);
    }
  }, [filterModule, currentPage, pageSize]);

  useEffect(() => {
    setCurrentPage(1);
  }, [filterModule]);

  useEffect(() => {
    fetchAuditLogs();
  }, [fetchAuditLogs]);

  const filteredLogs = useMemo(() => {
    return auditLogsList.filter((log) => {
      const matchesSearch =
        log.actor.toLowerCase().includes(search.toLowerCase()) ||
        log.entityName.toLowerCase().includes(search.toLowerCase()) ||
        log.detail.toLowerCase().includes(search.toLowerCase()) ||
        log.actionType.toLowerCase().includes(search.toLowerCase());

      let matchesSeverity = true;
      if (filterSeverity === "flagged") {
        matchesSeverity = log.securityFlag;
      } else if (filterSeverity !== "all") {
        matchesSeverity = log.severity.toLowerCase() === filterSeverity.toLowerCase();
      }

      return matchesSearch && matchesSeverity;
    });
  }, [auditLogsList, search, filterSeverity]);

  const handleExport = () => {
    const headers = "Timestamp,Actor,Role,Action,Module,Entity,Detail,IP Address,Device,Security Anomaly\n";
    const rows = filteredLogs
      .map(
        (l) =>
          `"${l.timestamp}","${l.actor}","${l.role}","${l.actionType}","${l.entityType}","${l.entityName}","${l.detail}","${l.ip}","${l.device}","${l.securityFlag ? "ANOMALY" : "NORMAL"}"`
      )
      .join("\n");
    const blob = new Blob([headers + rows], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `Security_Audit_Logs_${new Date().toISOString().split("T")[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const severityColors = {
    Success: "bg-emerald-50 text-emerald-700 border-emerald-200",
    Info: "bg-blue-50 text-blue-700 border-blue-200",
    Warning: "bg-amber-50 text-amber-700 border-amber-200",
    Critical: "bg-rose-50 text-rose-700 border-rose-200"
  };

  const getEntityIcon = (entityType) => {
    switch (entityType?.toLowerCase()) {
      case "scheme":
        return <Settings className="h-4 w-4" />;
      case "application":
        return <FileCheck className="h-4 w-4" />;
      case "application_document":
      case "document":
        return <FileText className="h-4 w-4" />;
      case "grievance":
        return <AlertTriangle className="h-4 w-4" />;
      case "user":
        return <User className="h-4 w-4" />;
      default:
        return <Clock className="h-4 w-4" />;
    }
  };

  return (
    <div className="space-y-4">
      {/* ── Filter / Control Bar ── */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-2xl shadow-sm space-y-3">
        <div className="flex flex-col md:flex-row gap-3 items-start md:items-center justify-between">
          <div className="flex flex-col md:flex-row gap-3 flex-1 w-full">
            <div className="relative flex-1">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search audit trail by actor, action, or entity ID..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full pl-10 pr-4 py-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100/50 dark:hover:bg-slate-700/50 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 transition"
              />
            </div>

            <select
              value={filterModule}
              onChange={(e) => setFilterModule(e.target.value)}
              className="px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="all">All Modules</option>
              <option value="application">Applications</option>
              <option value="application_document">Documents</option>
              <option value="scheme">Schemes</option>
              <option value="grievance">Grievances</option>
              <option value="user">User Security</option>
            </select>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={fetchAuditLogs}
              className="p-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 rounded-xl transition"
              title="Refresh Audit Trail"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            <button
              onClick={handleExport}
              className="px-3 py-2 bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 border border-indigo-200 dark:border-indigo-800/60 rounded-xl text-xs font-bold transition flex items-center gap-1.5"
            >
              <Download className="h-3.5 w-3.5" />
              Export Audit Trail
            </button>
          </div>
        </div>
      </div>

      {/* ── Table View ── */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50/70 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider text-[10px]">
              <tr>
                <th className="px-5 py-3">Timestamp</th>
                <th className="px-5 py-3">Actor & Role</th>
                <th className="px-5 py-3">Action</th>
                <th className="px-5 py-3">Module</th>
                <th className="px-5 py-3">Entity Reference</th>
                <th className="px-5 py-3">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800 font-semibold text-slate-700 dark:text-slate-300">
              {loading ? (
                <tr>
                  <td colSpan="6" className="p-12 text-center text-slate-500 dark:text-slate-400">
                    Loading Live Immutable Audit Trail...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="6" className="p-12 text-center text-rose-500">
                    <p className="font-bold">Failed to load audit records</p>
                    <p className="text-xs text-rose-500 mt-1">{error}</p>
                    <button
                      onClick={fetchAuditLogs}
                      className="mt-2 px-3 py-1.5 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 transition"
                    >
                      Retry
                    </button>
                  </td>
                </tr>
              ) : filteredLogs.length === 0 ? (
                <tr>
                  <td colSpan="6" className="p-12 text-center text-slate-500 dark:text-slate-400">
                    No matching audit records found.
                  </td>
                </tr>
              ) : (
                filteredLogs.map((l) => {
                  const sColor = severityColors[l.severity] || severityColors.Success;
                  return (
                    <tr key={l.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition">
                      <td className="px-5 py-3 text-[11px] text-slate-500 dark:text-slate-400 whitespace-nowrap">
                        {new Date(l.timestamp).toLocaleString("en-IN")}
                      </td>
                      <td className="px-5 py-3">
                        <div className="font-bold text-slate-800 dark:text-slate-100">{l.actor}</div>
                        <div className="text-[10px] text-slate-400 dark:text-slate-400">{l.role}</div>
                      </td>
                      <td className="px-5 py-3">
                        <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold border ${sColor}`}>
                          {l.actionType}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-1.5 text-slate-700 dark:text-slate-300">
                          {getEntityIcon(l.entityType)}
                          <span className="capitalize">{l.entityType}</span>
                        </div>
                      </td>
                      <td className="px-5 py-3 font-mono text-[11px] text-slate-600 dark:text-slate-400">
                        {l.entityName}
                      </td>
                      <td className="px-5 py-3 text-slate-600 dark:text-slate-400 max-w-xs truncate" title={l.detail}>
                        {l.detail}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {!loading && !error && auditLogsList.length > 0 && (
          <div className="px-5 py-3.5 border-t border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
            <span className="text-slate-500 dark:text-slate-400 font-semibold">
              Showing {pagination.totalElements === 0 ? 0 : (currentPage - 1) * pageSize + 1} to{" "}
              {Math.min(currentPage * pageSize, pagination.totalElements)} of {pagination.totalElements} audit entries
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
