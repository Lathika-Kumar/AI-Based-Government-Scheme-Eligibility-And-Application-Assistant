import React, { useState, useMemo } from "react";
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

export default function ApplicationsManagement({
  applications,
  updateApplicationStatus,
  onSelectApplication
}) {
  const [search, setSearch] = useState("");
  const [stageFilter, setStageFilter] = useState("all");
  const [riskFilter, setRiskFilter] = useState("all");
  const [priorityFilter, setPriorityFilter] = useState("all");
  const [stateFilter, setStateFilter] = useState("all");
  const [sortField, setSortField] = useState("appliedDate");
  const [sortOrder, setSortOrder] = useState("desc");

  // Selection state for bulk actions
  const [selectedIds, setSelectedIds] = useState([]);

  // Mock enrichment helper for applications
  const enrichedApplications = useMemo(() => {
    return (applications || []).map((app, idx) => {
      // Deterministic mock generation based on app properties
      const idCode = app.id.replace("APP-", "");
      const isApproved = app.currentStage === "Approved";
      const isRejected = app.currentStage === "Rejected";
      
      let riskLevel = "Low";
      if (idCode % 3 === 0) riskLevel = "Medium";
      if (idCode % 7 === 0 && !isApproved) riskLevel = "High";

      let aiPriority = "Medium";
      if (riskLevel === "High" || app.currentStage === "Under Review") aiPriority = "High";
      if (isApproved || isRejected) aiPriority = "Low";

      let docReadiness = 100;
      if (app.currentStage === "Submitted") docReadiness = 75;
      if (app.currentStage === "Under Review") docReadiness = 85;

      const submissionDate = new Date(app.appliedDate);
      const expectedComp = new Date(submissionDate.getTime() + 7 * 24 * 60 * 60 * 1000)
        .toISOString()
        .split("T")[0];

      const officers = ["Amit Singh", "Neha Sharma", "Sanjay Kumar", "Priya Patel"];
      const officer = officers[parseInt(idCode) % officers.length];

      const departments = [
        "Department of Social Justice",
        "Department of Agriculture & Farmers Welfare",
        "Department of Financial Services",
        "Department of Housing & Urban Affairs"
      ];
      const dept = app.ministry ? app.ministry.split("/")[1] || app.ministry.split("-")[1] || app.ministry : departments[parseInt(idCode) % departments.length];

      return {
        ...app,
        dept: dept.trim(),
        riskLevel,
        aiPriority,
        docReadiness,
        expectedComp,
        officer
      };
    });
  }, [applications]);

  // Handle Sort
  const handleSort = (field) => {
    if (sortField === field) {
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortOrder("asc");
    }
  };

  // Filter & Search application logic
  const filteredApps = useMemo(() => {
    return enrichedApplications
      .filter((app) => {
        const matchesSearch =
          app.applicantName.toLowerCase().includes(search.toLowerCase()) ||
          app.id.toLowerCase().includes(search.toLowerCase()) ||
          app.schemeName.toLowerCase().includes(search.toLowerCase());

        const matchesStage = stageFilter === "all" || app.currentStage === stageFilter;
        const matchesRisk = riskFilter === "all" || app.riskLevel === riskFilter;
        const matchesPriority = priorityFilter === "all" || app.aiPriority === priorityFilter;
        const matchesState = stateFilter === "all" || app.applicantState === stateFilter;

        return matchesSearch && matchesStage && matchesRisk && matchesPriority && matchesState;
      })
      .sort((a, b) => {
        let valA = a[sortField];
        let valB = b[sortField];

        // Custom prioritization mapping
        if (sortField === "aiPriority") {
          const priorityWeight = { High: 3, Medium: 2, Low: 1 };
          valA = priorityWeight[a.aiPriority] || 0;
          valB = priorityWeight[b.aiPriority] || 0;
        } else if (sortField === "riskLevel") {
          const riskWeight = { High: 3, Medium: 2, Low: 1 };
          valA = riskWeight[a.riskLevel] || 0;
          valB = riskWeight[b.riskLevel] || 0;
        }

        if (valA < valB) return sortOrder === "asc" ? -1 : 1;
        if (valA > valB) return sortOrder === "asc" ? 1 : -1;
        return 0;
      });
  }, [enrichedApplications, search, stageFilter, riskFilter, priorityFilter, stateFilter, sortField, sortOrder]);

  // Selection Handlers
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

  // Bulk Actions
  const handleBulkApprove = () => {
    selectedIds.forEach((id) => updateApplicationStatus(id, "Approved"));
    setSelectedIds([]);
  };

  const handleBulkReject = () => {
    selectedIds.forEach((id) => updateApplicationStatus(id, "Rejected"));
    setSelectedIds([]);
  };

  const handleExport = () => {
    // Generate CSV contents
    const headers = "Application ID,Applicant Name,Scheme Name,Submission Date,Status,AI Priority,Risk Level,Officer\n";
    const rows = filteredApps
      .map(
        (a) =>
          `"${a.id}","${a.applicantName}","${a.schemeName}","${a.appliedDate}","${a.currentStage}","${a.aiPriority}","${a.riskLevel}","${a.officer}"`
      )
      .join("\n");
    const blob = new Blob([headers + rows], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `SchemeBridge_Applications_Export_${new Date().toISOString().split("T")[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Style helper mapping for status pills - GoI colors
  const statusColors = {
    Submitted: "bg-government-blue/10 text-government-blue border-government-blue/20",
    "Under Review": "bg-saffron/10 text-saffron-dark border-saffron/20",
    Approved: "bg-india-green/10 text-india-green border-india-green/20",
    Rejected: "bg-red-50 text-red-700 border-red-200"
  };

  const riskColors = {
    Low: "bg-india-green/10 text-india-green border-india-green/20",
    Medium: "bg-saffron/10 text-saffron-dark border-saffron/20",
    High: "bg-red-50 text-red-700 border-red-200 hover:animate-pulse"
  };

  const priorityColors = {
    Low: "bg-gray-100 text-gray-600 border-gray-200",
    Medium: "bg-government-blue/10 text-government-blue border-government-blue/20",
    High: "bg-saffron/10 text-saffron-dark border-saffron/20"
  };

  return (
    <div className="space-y-4">
      {/* ── Filter / Command Bar ── */}
      <div className="bg-white border border-gray-200 p-4 rounded-2xl shadow-sm space-y-3">
        <div className="flex flex-col md:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search by ID, citizen name, or scheme..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-gray-50 hover:bg-gray-100/50 border border-gray-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-government-blue text-gray-700 transition"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            {/* Stage Filter */}
            <select
              value={stageFilter}
              onChange={(e) => setStageFilter(e.target.value)}
              className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-government-blue cursor-pointer"
            >
              <option value="all">All Stages</option>
              <option value="Submitted">Submitted</option>
              <option value="Under Review">Under Review</option>
              <option value="Approved">Approved</option>
              <option value="Rejected">Rejected</option>
            </select>

            {/* Risk Filter */}
            <select
              value={riskFilter}
              onChange={(e) => setRiskFilter(e.target.value)}
              className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-government-blue cursor-pointer"
            >
              <option value="all">All Risks</option>
              <option value="Low">Low Risk</option>
              <option value="Medium">Medium Risk</option>
              <option value="High">High Risk</option>
            </select>

            {/* AI Priority Filter */}
            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
              className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-government-blue cursor-pointer"
            >
              <option value="all">All Priorities</option>
              <option value="High">High Priority</option>
              <option value="Medium">Medium Priority</option>
              <option value="Low">Low Priority</option>
            </select>

            {/* State Filter */}
            <select
              value={stateFilter}
              onChange={(e) => setStateFilter(e.target.value)}
              className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-government-blue cursor-pointer"
            >
              <option value="all">All States</option>
              <option value="Gujarat">Gujarat</option>
              <option value="Maharashtra">Maharashtra</option>
              <option value="Tamil Nadu">Tamil Nadu</option>
              <option value="Uttar Pradesh">Uttar Pradesh</option>
              <option value="Karnataka">Karnataka</option>
            </select>

            {/* Export */}
            <button
              onClick={handleExport}
              disabled={filteredApps.length === 0}
              className="px-3 py-2 bg-gray-100 hover:bg-gray-200 border border-gray-200 text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl text-xs font-bold flex items-center gap-1.5 transition"
            >
              <Download className="h-3.5 w-3.5" />
              <span>Export</span>
            </button>
          </div>
        </div>

        {/* ── Bulk Actions Bar ── */}
        {selectedIds.length > 0 && (
          <div className="bg-government-blue/10 border border-government-blue/20 p-3 rounded-xl flex items-center justify-between animate-in slide-in-from-top-2 duration-200">
            <span className="text-xs font-bold text-government-blue-dark">
              {selectedIds.length} application(s) selected
            </span>
            <div className="flex gap-2">
              <button
                onClick={handleBulkApprove}
                className="px-3 py-1.5 bg-india-green hover:bg-india-green-dark text-white rounded-lg text-xs font-bold flex items-center gap-1 transition"
              >
                <CheckCircle className="h-3.5 w-3.5" />
                <span>Approve Selected</span>
              </button>
              <button
                onClick={handleBulkReject}
                className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-bold flex items-center gap-1 transition"
              >
                <XCircle className="h-3.5 w-3.5" />
                <span>Reject Selected</span>
              </button>
              <button
                onClick={() => setSelectedIds([])}
                className="px-3 py-1.5 bg-white border border-gray-200 text-gray-600 hover:bg-gray-50 rounded-lg text-xs font-bold transition"
              >
                Clear Selection
              </button>
            </div>
          </div>
        )}
      </div>

      {/* ── Main Data Table ── */}
      <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">
        {filteredApps.length === 0 ? (
          <div className="p-16 flex flex-col items-center justify-center text-center space-y-4">
            <div className="p-4 bg-gray-50 rounded-full border border-gray-100">
              <FolderOpen className="h-10 w-10 text-gray-400" />
            </div>
            <div className="space-y-1">
              <p className="text-sm font-bold text-gray-800">No Applications Found</p>
              <p className="text-xs text-gray-400 leading-normal max-w-sm">
                No active records match the current filters. Adjust your search or filters to locate applications.
              </p>
            </div>
            <button
              onClick={() => {
                setSearch("");
                setStageFilter("all");
                setRiskFilter("all");
                setPriorityFilter("all");
                setStateFilter("all");
              }}
              className="px-4 py-2 bg-government-blue hover:bg-government-blue-dark text-white rounded-xl text-xs font-bold shadow-sm transition"
            >
              Clear All Filters
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-gray-50/70 border-b border-gray-200 text-gray-500 uppercase tracking-wider font-bold text-[9px] select-none">
                <tr>
                  <th className="px-4 py-3.5 w-10 text-center">
                    <button
                      onClick={handleSelectAll}
                      className="text-gray-400 hover:text-gray-600 focus:outline-none transition"
                    >
                      {selectedIds.length === filteredApps.length ? (
                        <CheckSquare className="h-4 w-4 text-government-blue" />
                      ) : (
                        <Square className="h-4 w-4" />
                      )}
                    </button>
                  </th>
                  <th className="px-4 py-3.5 cursor-pointer hover:bg-gray-100" onClick={() => handleSort("id")}>
                    <div className="flex items-center gap-1">
                      <span>App ID</span>
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </th>
                  <th className="px-4 py-3.5 cursor-pointer hover:bg-gray-100" onClick={() => handleSort("applicantName")}>
                    <div className="flex items-center gap-1">
                      <span>Citizen</span>
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </th>
                  <th className="px-4 py-3.5 cursor-pointer hover:bg-gray-100" onClick={() => handleSort("schemeName")}>
                    <div className="flex items-center gap-1">
                      <span>Scheme</span>
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </th>
                  <th className="px-4 py-3.5 hidden lg:table-cell cursor-pointer hover:bg-gray-100" onClick={() => handleSort("dept")}>
                    <div className="flex items-center gap-1">
                      <span>Department</span>
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </th>
                  <th className="px-4 py-3.5 cursor-pointer hover:bg-gray-100" onClick={() => handleSort("appliedDate")}>
                    <div className="flex items-center gap-1">
                      <span>Submission</span>
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </th>
                  <th className="px-4 py-3.5">
                    <span>Readiness</span>
                  </th>
                  <th className="px-4 py-3.5 cursor-pointer hover:bg-gray-100" onClick={() => handleSort("aiPriority")}>
                    <div className="flex items-center gap-1">
                      <span>AI Priority</span>
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </th>
                  <th className="px-4 py-3.5 cursor-pointer hover:bg-gray-100" onClick={() => handleSort("riskLevel")}>
                    <div className="flex items-center gap-1">
                      <span>Risk</span>
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </th>
                  <th className="px-4 py-3.5 cursor-pointer hover:bg-gray-100" onClick={() => handleSort("currentStage")}>
                    <div className="flex items-center gap-1">
                      <span>Stage</span>
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </th>
                  <th className="px-4 py-3.5 hidden xl:table-cell">Officer</th>
                  <th className="px-4 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredApps.map((app) => {
                  const isChecked = selectedIds.includes(app.id);
                  return (
                    <tr
                      key={app.id}
                      className={`hover:bg-gray-50 transition cursor-pointer ${isChecked ? "bg-government-blue/5" : ""}`}
                      onClick={() => onSelectApplication(app)}
                    >
                      {/* Checkbox */}
                      <td
                        className="px-4 py-3.5 text-center"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleSelectOne(app.id);
                        }}
                      >
                        <button className="text-gray-400 hover:text-government-blue focus:outline-none transition">
                          {isChecked ? (
                            <CheckSquare className="h-4 w-4 text-government-blue" />
                          ) : (
                            <Square className="h-4 w-4" />
                          )}
                        </button>
                      </td>

                      {/* ID */}
                      <td className="px-4 py-3.5 font-mono text-gray-500 font-bold text-[10px]">
                        {app.id}
                      </td>

                      {/* Applicant Name */}
                      <td className="px-4 py-3.5 font-bold text-gray-800">
                        <div className="flex flex-col">
                          <span>{app.applicantName}</span>
                          <span className="text-[9px] text-gray-400 font-medium font-sans">
                            {app.applicantState} • {app.applicantCaste}
                          </span>
                        </div>
                      </td>

                      {/* Scheme */}
                      <td className="px-4 py-3.5 font-semibold text-gray-700 max-w-[160px] truncate" title={app.schemeName}>
                        {app.schemeName}
                      </td>

                      {/* Department */}
                      <td className="px-4 py-3.5 hidden lg:table-cell text-gray-500 font-medium truncate max-w-[160px]" title={app.dept}>
                        {app.dept}
                      </td>

                      {/* Submitted Date */}
                      <td className="px-4 py-3.5 text-gray-500 font-semibold whitespace-nowrap">
                        {app.appliedDate}
                      </td>

                      {/* Document Readiness Progress */}
                      <td className="px-4 py-3.5">
                        <div className="flex items-center gap-2">
                          <div className="w-12 h-1.5 bg-gray-100 rounded-full overflow-hidden shrink-0">
                            <div
                              className={`h-full rounded-full transition-all ${
                                app.docReadiness === 100
                                  ? "bg-india-green"
                                  : app.docReadiness >= 75
                                  ? "bg-government-blue"
                                  : "bg-saffron"
                              }`}
                              style={{ width: `${app.docReadiness}%` }}
                            />
                          </div>
                          <span className="text-[10px] font-bold text-gray-600">{app.docReadiness}%</span>
                        </div>
                      </td>

                      {/* AI Priority */}
                      <td className="px-4 py-3.5">
                        <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold inline-flex items-center gap-1 ${priorityColors[app.aiPriority] || ""}`}>
                          {app.aiPriority === "High" && <Sparkles className="h-2.5 w-2.5 text-saffron-dark" />}
                          {app.aiPriority}
                        </span>
                      </td>

                      {/* Risk Level */}
                      <td className="px-4 py-3.5">
                        <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold inline-flex items-center gap-1 ${riskColors[app.riskLevel] || ""}`}>
                          {app.riskLevel === "High" && <AlertTriangle className="h-2.5 w-2.5 text-red-600" />}
                          {app.riskLevel}
                        </span>
                      </td>

                      {/* Stage Badge */}
                      <td className="px-4 py-3.5">
                        <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold whitespace-nowrap ${statusColors[app.currentStage] || ""}`}>
                          {app.currentStage}
                        </span>
                      </td>

                      {/* Officer */}
                      <td className="px-4 py-3.5 hidden xl:table-cell text-gray-500 font-semibold">
                        {app.officer}
                      </td>

                      {/* Actions */}
                      <td className="px-4 py-3.5 text-right" onClick={(e) => e.stopPropagation()}>
                        <button
                          onClick={() => onSelectApplication(app)}
                          className="px-2.5 py-1 bg-government-blue/10 hover:bg-government-blue/20 text-government-blue hover:text-government-blue-dark rounded-lg text-[10px] font-bold transition"
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
      </div>
    </div>
  );
}
