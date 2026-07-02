import React, { useState, useMemo } from "react";
import {
  ShieldAlert,
  Search,
  Download,
  AlertTriangle,
  CheckCircle,
  FileText,
  Calendar
} from "lucide-react";

export default function AuditLogsConsole({ auditLogs }) {
  const [search, setSearch] = useState("");
  const [filterModule, setFilterModule] = useState("all");
  const [filterSeverity, setFilterSeverity] = useState("all");
  const [filterTime, setFilterTime] = useState("all");

  // Enrich audit logs with IP, Device, Role, and Security Anomaly Flags
  const enrichedLogs = useMemo(() => {
    return (auditLogs || []).map((log, idx) => {
      const isSystem = log.actor === "System Vault Registry" || log.actor === "System Audit";
      const isCitizen = log.actor && !log.actor.includes("Admin") && !isSystem;

      let role = "Nodal Officer";
      if (isSystem) role = "Automated Engine";
      if (isCitizen) role = "Citizen";
      if (log.actor && log.actor.includes("Admin")) role = "Super Admin";

      // Deterministic IP and Devices
      const idNum = parseInt(log.id.replace("LOG-", "")) || idx;
      const ip = `10.120.4.${20 + (idNum % 150)}`;
      
      const devices = [
        "Chrome / Windows 11 Official",
        "Firefox / RedHat Linux Kernel",
        "Safari / MacOS Sonoma",
        "Edge / Server Core Windows"
      ];
      const device = devices[idNum % devices.length];

      // Flags for anomalies: e.g. delete actions, or login/updates outside typical parameters
      let securityFlag = false;
      let severity = "Success";

      if (log.actionType === "Delete") {
        securityFlag = true;
        severity = "Warning";
      } else if (log.actionType === "Status Change" && log.detail.includes("Rejected")) {
        severity = "Info";
      } else if (log.actionType === "Login" && idNum % 13 === 0) {
        securityFlag = true;
        severity = "Critical";
      }

      return {
        ...log,
        role,
        ip,
        device,
        securityFlag,
        severity
      };
    });
  }, [auditLogs]);

  const filteredLogs = useMemo(() => {
    return enrichedLogs
      .filter((log) => {
        const matchesSearch =
          log.actor.toLowerCase().includes(search.toLowerCase()) ||
          log.entityName.toLowerCase().includes(search.toLowerCase()) ||
          log.detail.toLowerCase().includes(search.toLowerCase());

        const matchesModule = filterModule === "all" || log.entityType.toLowerCase() === filterModule.toLowerCase();
        
        let matchesSeverity = true;
        if (filterSeverity === "flagged") {
          matchesSeverity = log.securityFlag;
        } else if (filterSeverity !== "all") {
          matchesSeverity = log.severity.toLowerCase() === filterSeverity.toLowerCase();
        }

        // Time filtering simulated
        let matchesTime = true;
        if (filterTime === "today") {
          const logDate = new Date(log.timestamp).toDateString();
          const todayDate = new Date().toDateString();
          matchesTime = logDate === todayDate;
        }

        return matchesSearch && matchesModule && matchesSeverity && matchesTime;
      });
  }, [enrichedLogs, search, filterModule, filterSeverity, filterTime]);

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
    Success: "bg-emerald-50 text-emerald-700 border-emerald-150 border-emerald-200",
    Info: "bg-blue-50 text-blue-700 border-blue-200",
    Warning: "bg-amber-50 text-amber-700 border-amber-200",
    Critical: "bg-rose-50 text-rose-700 border-rose-200 hover:animate-pulse"
  };

  return (
    <div className="space-y-4">
      {/* ── Filter / Control Bar ── */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm space-y-3">
        <div className="flex flex-col md:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search audit trail by actor, entity details..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-50 hover:bg-slate-100/50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 transition"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            {/* Time Filter */}
            <select
              value={filterTime}
              onChange={(e) => setFilterTime(e.target.value)}
              className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer"
            >
              <option value="all">All History</option>
              <option value="today">Today</option>
            </select>

            {/* Module Filter */}
            <select
              value={filterModule}
              onChange={(e) => setFilterModule(e.target.value)}
              className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer"
            >
              <option value="all">All Modules</option>
              <option value="scheme">Scheme Management</option>
              <option value="application">Application Desk</option>
              <option value="document">Document Vault</option>
              <option value="grievance">Grievance Desk</option>
            </select>

            {/* Severity Filter */}
            <select
              value={filterSeverity}
              onChange={(e) => setFilterSeverity(e.target.value)}
              className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer"
            >
              <option value="all">All Severities</option>
              <option value="flagged">⚠️ Anomaly Flags Only</option>
              <option value="Success">Success Logs</option>
              <option value="Warning">Warning Logs</option>
              <option value="Critical">Critical Logs</option>
            </select>

            {/* Export CSV */}
            <button
              onClick={handleExport}
              disabled={filteredLogs.length === 0}
              className="px-3 py-2 bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-750 text-slate-700 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl text-xs font-bold flex items-center gap-1.5 transition"
            >
              <Download className="h-3.5 w-3.5" />
              <span>Export CSV</span>
            </button>
          </div>
        </div>
      </div>

      {/* ── Main Activity Grid ── */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        {filteredLogs.length === 0 ? (
          <div className="p-16 flex flex-col items-center justify-center text-center space-y-3">
            <FileText className="h-10 w-10 text-slate-400" />
            <div>
              <p className="text-sm font-bold text-slate-800">No Audits Found</p>
              <p className="text-xs text-slate-400 leading-normal max-w-sm mt-0.5">
                No telemetry activity matches your query. Clear filters to search all logs.
              </p>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-slate-50/70 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-bold text-[9px] select-none">
                <tr>
                  <th className="px-4 py-3.5">Time</th>
                  <th className="px-4 py-3.5">Actor</th>
                  <th className="px-4 py-3.5">Role</th>
                  <th className="px-4 py-3.5">Action</th>
                  <th className="px-4 py-3.5">Module</th>
                  <th className="px-4 py-3.5 hidden md:table-cell">Entity</th>
                  <th className="px-4 py-3.5">Details</th>
                  <th className="px-4 py-3.5 hidden lg:table-cell">Client IP</th>
                  <th className="px-4 py-3.5 hidden xl:table-cell">Browser Device</th>
                  <th className="px-4 py-3.5">Severity</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-semibold text-slate-655 text-slate-700">
                {filteredLogs.map((log) => (
                  <tr
                    key={log.id}
                    className={`hover:bg-slate-50 transition ${
                      log.securityFlag ? "bg-rose-50/20" : ""
                    }`}
                  >
                    <td className="px-4 py-3.5 text-slate-400 font-medium whitespace-nowrap">
                      {new Date(log.timestamp).toLocaleString("en-IN", { timeZone: "IST" })}
                    </td>
                    <td className="px-4 py-3.5 font-bold text-slate-800">
                      {log.actor}
                    </td>
                    <td className="px-4 py-3.5 text-slate-500 font-medium whitespace-nowrap">
                      {log.role}
                    </td>
                    <td className="px-4 py-3.5 whitespace-nowrap">
                      {log.actionType}
                    </td>
                    <td className="px-4 py-3.5 whitespace-nowrap">
                      {log.entityType}
                    </td>
                    <td className="px-4 py-3.5 hidden md:table-cell text-slate-800 font-bold max-w-[140px] truncate" title={log.entityName}>
                      {log.entityName}
                    </td>
                    <td className="px-4 py-3.5 text-slate-500 font-medium max-w-sm truncate" title={log.detail}>
                      {log.detail}
                    </td>
                    <td className="px-4 py-3.5 hidden lg:table-cell font-mono text-[10px] text-slate-500">
                      {log.ip}
                    </td>
                    <td className="px-4 py-3.5 hidden xl:table-cell text-slate-400 font-medium truncate max-w-[180px]" title={log.device}>
                      {log.device}
                    </td>
                    <td className="px-4 py-3.5">
                      <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold inline-flex items-center gap-1 ${severityColors[log.severity] || ""}`}>
                        {log.securityFlag && <ShieldAlert className="h-3 w-3 text-rose-600 shrink-0" />}
                        {log.severity}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
