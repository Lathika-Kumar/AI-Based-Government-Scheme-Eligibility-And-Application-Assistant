import React, { useState, useEffect, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { getDocReadinessForScheme } from "@utils/documentReadiness";
import { TimelineSkeleton } from "@components/ui/LoadingSkeleton";
import EmptyState from "@components/ui/EmptyState";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import {
  ClipboardList,
  CheckCircle2,
  Clock,
  AlertCircle,
  ArrowRight,
  CalendarDays,
  Sparkles,
  BookmarkPlus,
  FileText,
  ChevronUp,
  ChevronDown,
  CircleDot,
  Circle,
  MessageSquareWarning,
  Send,
  Search,
  RefreshCw,
  Download,
  Share2,
  HelpCircle,
  User,
  CreditCard,
  Building,
  Check,
  PhoneCall,
  BrainCircuit,
  ShieldCheck,
  ShieldAlert,
  ArrowUpRight,
  Plus
} from "lucide-react";

// Overall stage mapping to indices
const STAGE_INDICES = {
  "Saved": 0,
  "Preparing Documents": 1,
  "Ready to Apply": 2,
  "Submitted": 3,
  "Under Review": 4,
  "Approved": 5,
  "Rejected": 5
};

const TIMELINE_STAGES = [
  { key: "Saved", label: "Interested", description: "Scheme saved to profile dashboard for evaluation." },
  { key: "Preparing Documents", label: "Preparing Documents", description: "Checklist socio-economic document collection." },
  { key: "Ready to Apply", label: "Ready to Apply", description: "All required documents verified in vault." },
  { key: "Submitted", label: "Applied", description: "Application successfully submitted online." },
  { key: "Under Review", label: "Under Review", description: "Awaiting authority registry check and verification." },
  { key: "Approved", label: "Approved / Rejected", description: "Final decision completed and mandate generated." }
];

// Returns overall project progress percentage
function getOverallProgress(stage) {
  switch (stage) {
    case "Saved": return 15;
    case "Preparing Documents": return 30;
    case "Ready to Apply": return 50;
    case "Submitted": return 70;
    case "Under Review": return 85;
    case "Approved": return 100;
    case "Rejected": return 100;
    default: return 15;
  }
}

// Get dynamic color scheme for ministry category
function getMinistryBgColor(ministry = "") {
  const m = ministry.toLowerCase();
  if (m.includes("finance")) {
return "bg-indigo-50 text-indigo-700 border-indigo-150";
}
  if (m.includes("agriculture")) {
return "bg-emerald-50 text-emerald-700 border-emerald-150";
}
  if (m.includes("housing")) {
return "bg-amber-50 text-amber-700 border-amber-150";
}
  if (m.includes("women")) {
return "bg-rose-50 text-rose-700 border-rose-150";
}
  return "bg-slate-50 text-slate-700 border-slate-150";
}

export default function Tracker() {
  const {
    applications,
    savedSchemes,
    schemes,
    documents,
    profile,
    submitGrievance,
    language,
    t
  } = useApp();

  const navigate = useNavigate();

  // Page States
  const [selectedId, setSelectedId] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [sortBy, setSortBy] = useState("latest");
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Chat/AI States
  const [aiChatOpen, setAiChatOpen] = useState(false);
  const [aiInitialQuery, setAiInitialQuery] = useState("");

  // Grievance panel per selected application
  const [showGrievance, setShowGrievance] = useState(false);
  const [grievanceData, setGrievanceData] = useState({ category: "Delay in Processing", description: "" });
  const [grievanceSuccess, setGrievanceSuccess] = useState(false);

  // Simulation loading
  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 800);
    return () => clearTimeout(timer);
  }, []);

  const handleRefresh = () => {
    setIsRefreshing(true);
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setIsRefreshing(false);
    }, 600);
  };

  // Combine applications and savedSchemes into a unified list
  const allEntries = useMemo(() => {
    return [
      ...applications.map((a) => ({ ...a, _type: "applied" })),
      ...savedSchemes.map((s) => ({ ...s, currentStage: s.stage, _type: "saved" })),
    ];
  }, [applications, savedSchemes]);

  // Filter entries
  const filteredEntries = useMemo(() => {
    return allEntries.filter((entry) => {
      // 1. Status Filter
      const stage = entry.currentStage || entry.stage || "Saved";
      if (statusFilter !== "all" && stage !== statusFilter) {
        return false;
      }
      // 2. Search Query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesName = entry.schemeName.toLowerCase().includes(q);
        const matchesId = (entry.id || "").toLowerCase().includes(q);
        const matchesRef = (entry.referenceNo || "").toLowerCase().includes(q);
        const matchesMinistry = (entry.ministry || "").toLowerCase().includes(q);
        return matchesName || matchesId || matchesRef || matchesMinistry;
      }
      return true;
    });
  }, [allEntries, statusFilter, searchQuery]);

  // Sort entries
  const sortedEntries = useMemo(() => {
    const list = [...filteredEntries];
    if (sortBy === "latest") {
      list.sort((a, b) => {
        const dateA = new Date(a.appliedDate || a.savedDate || 0);
        const dateB = new Date(b.appliedDate || b.savedDate || 0);
        return dateB - dateA;
      });
    } else if (sortBy === "oldest") {
      list.sort((a, b) => {
        const dateA = new Date(a.appliedDate || a.savedDate || 0);
        const dateB = new Date(b.appliedDate || b.savedDate || 0);
        return dateA - dateB;
      });
    } else if (sortBy === "status") {
      list.sort((a, b) => {
        const stageA = a.currentStage || a.stage || "";
        const stageB = b.currentStage || b.stage || "";
        return stageA.localeCompare(stageB);
      });
    } else if (sortBy === "deadline") {
      // Sort alphabetically as fallback
      list.sort((a, b) => a.schemeName.localeCompare(b.schemeName));
    }
    return list;
  }, [filteredEntries, sortBy]);

  // Selected Entry Selection
  const selectedEntry = useMemo(() => {
    if (sortedEntries.length === 0) {
return null;
}
    const found = sortedEntries.find((e) => (e.id || e.schemeId) === selectedId);
    return found || sortedEntries[0];
  }, [sortedEntries, selectedId]);

  // Calculate readiness of the selected entry
  const selectedReadiness = useMemo(() => {
    if (!selectedEntry) {
return null;
}
    const scheme = schemes.find((s) => s.id === selectedEntry.schemeId);
    return scheme
      ? getDocReadinessForScheme(scheme.requiredDocuments, documents)
      : { readinessScore: 0, missingDocs: [] };
  }, [selectedEntry, schemes, documents]);

  // Overall counts for summary headers
  const stats = useMemo(() => {
    return {
      total: allEntries.length,
      applied: applications.length,
      review: applications.filter((a) => a.currentStage === "Under Review").length,
      approved: applications.filter((a) => a.currentStage === "Approved").length,
    };
  }, [allEntries, applications]);

  // Grievance Submit Handler
  const handleGrievanceSubmit = (e) => {
    e.preventDefault();
    if (!grievanceData.description.trim() || !selectedEntry) {
return;
}

    submitGrievance({
      phone: profile?.phone || "9876543210",
      email: profile?.email || "rajesh.patel@gmail.com",
      relatedScheme: selectedEntry.schemeName,
      category: grievanceData.category,
      description: grievanceData.description,
    });

    setGrievanceSuccess(true);
    setGrievanceData({ category: "Delay in Processing", description: "" });
    setTimeout(() => {
      setGrievanceSuccess(false);
      setShowGrievance(false);
    }, 2000);
  };

  // Get status color mappings
  const getStatusBadge = (stage) => {
    switch (stage) {
      case "Saved":
        return { label: "Draft", style: "bg-slate-100 text-slate-700 border-slate-200" };
      case "Preparing Documents":
        return { label: "Preparing", style: "bg-amber-50 text-amber-700 border-amber-250 border-amber-200" };
      case "Ready to Apply":
        return { label: "Ready", style: "bg-blue-50 text-blue-700 border-blue-200" };
      case "Submitted":
        return { label: "Applied", style: "bg-indigo-50 text-indigo-700 border-indigo-200" };
      case "Under Review":
        return { label: "Under Review", style: "bg-blue-50 text-blue-800 border-blue-200" };
      case "Approved":
        return { label: "Approved", style: "bg-emerald-50 text-emerald-800 border-emerald-200" };
      case "Rejected":
        return { label: "Rejected", style: "bg-rose-50 text-rose-800 border-rose-200" };
      default:
        return { label: stage, style: "bg-slate-100 text-slate-700 border-slate-200" };
    }
  };

  // Simulated expected decision and benefits mapping
  const getSchemeAdditions = (schemeId) => {
    switch (schemeId) {
      case "atal-pension-yojana":
        return { benefit: "₹1,000 - ₹5,000 / month pension", officer: "Shri V. K. Naidu (Joint Director)", remarks: "Aadhaar mandate linked to State Bank of India. Verification verified.", expectedDate: "15 Nov 2026" };
      case "pm-kisan":
        return { benefit: "₹6,000 / year (₹2,000 per installment)", officer: "Shri Alok Mishra (Assistant Commissioner)", remarks: "Verified land ownership registry sync. DBT transfer approved.", expectedDate: "Completed" };
      case "pm-awas-yojana":
        return { benefit: "₹2.67 Lakh housing credit subsidy", officer: "Smt. Neera Tandon (Audit Officer)", remarks: "Application rejected due to income details mismatch with ITR state records.", expectedDate: "Declined" };
      default:
        return { benefit: "Direct Subsidy / Benefit Transfer", officer: "Pre-check desk officer", remarks: "Automatic state registry scan completed successfully.", expectedDate: "20 Nov 2026" };
    }
  };

  const currentStage = selectedEntry?.currentStage || selectedEntry?.stage || "Saved";
  const stageHistory = selectedEntry?.stageHistory || [];
  const currentStageIdx = STAGE_INDICES[currentStage] !== undefined ? STAGE_INDICES[currentStage] : 0;
  const isApplied = selectedEntry?._type === "applied";
  const schemeAdditions = selectedEntry ? getSchemeAdditions(selectedEntry.schemeId) : null;

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-1 sm:px-4">
      {/* Centralized Stat Summary cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: t("track_total_apps") || "Total Applications", val: stats.total, color: "border-b-slate-400 text-slate-900" },
          { label: t("track_submitted_apps") || "Submitted Applications", val: stats.applied, color: "border-b-indigo-500 text-indigo-700" },
          { label: t("track_under_review") || "Under Review Desk", val: stats.review, color: "border-b-blue-500 text-blue-700" },
          { label: t("track_approved_mandates") || "Approved Mandates", val: stats.approved, color: "border-b-emerald-500 text-emerald-700" }
        ].map((sItem, index) => (
          <div key={index} className={`bg-white border border-slate-200 border-b-4 ${sItem.color} rounded-2xl p-4 shadow-sm flex flex-col justify-between`}>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest leading-none">{sItem.label}</span>
            <span className="text-3xl font-black mt-2 leading-none">{sItem.val}</span>
          </div>
        ))}
      </div>

      {/* Main Two-Column Structure */}
      {isLoading ? (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-5 space-y-4">
            <div className="bg-white border border-slate-200 p-4 rounded-2xl animate-pulse space-y-3">
              <div className="h-9 w-full bg-slate-200 rounded-xl" />
              <div className="h-10 w-full bg-slate-100 rounded-xl" />
            </div>
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-white border border-slate-200 p-5 rounded-2xl animate-pulse space-y-3">
                <div className="h-4 w-3/4 bg-slate-200 rounded" />
                <div className="h-3 w-1/2 bg-slate-100 rounded" />
                <div className="h-2 w-full bg-slate-100 rounded" />
              </div>
            ))}
          </div>
          <div className="lg:col-span-7 space-y-6">
            <TimelineSkeleton />
          </div>
        </div>
      ) : allEntries.length === 0 ? (
        <EmptyState
          icon={ClipboardList}
          title={t("tracker_empty_title") || "Nothing tracked yet"}
          description={t("tracker_empty_desc") || "Browse scheme recommendations and save or apply to schemes to track them here."}
          action={{
            label: t("tracker_find_schemes") || "Find Matching Schemes",
            onClick: () => navigate("/recommendations"),
            icon: Sparkles
          }}
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

          {/* ================= LEFT COLUMN: LIST PANEL ================= */}
          <div className="lg:col-span-5 space-y-4">

            {/* Top Toolbar card */}
            <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm space-y-3">
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder={t("track_search_placeholder") || "Search by scheme name, ID or ministry..."}
                    className="w-full text-xs pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500 text-slate-800"
                  />
                </div>
                <button
                  onClick={handleRefresh}
                  className={`p-2 border border-slate-200 rounded-xl text-slate-500 hover:text-slate-800 bg-white hover:bg-slate-50 transition shrink-0 ${isRefreshing ? "animate-spin" : ""}`}
                  title="Refresh List"
                >
                  <RefreshCw className="h-4.5 w-4.5" />
                </button>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[9px] font-bold text-slate-400 uppercase tracking-widest block mb-1">{t("gen_filter") || "Status Filter"}</label>
                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="w-full text-xs p-2 border border-slate-200 rounded-xl bg-slate-50 text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  >
                    <option value="all">{t("tracker_tab_all") || "All Statuses"}</option>
                    <option value="Saved">{t("tracker_stage_saved") || "Draft / Saved"}</option>
                    <option value="Preparing Documents">{t("tracker_stage_preparing") || "Preparing"}</option>
                    <option value="Ready to Apply">{t("tracker_stage_ready") || "Ready to Apply"}</option>
                    <option value="Submitted">{t("tracker_stage_submitted") || "Submitted / Applied"}</option>
                    <option value="Under Review">{t("tracker_stage_review") || "Under Review"}</option>
                    <option value="Approved">{t("tracker_stage_approved") || "Approved"}</option>
                    <option value="Rejected">{t("tracker_stage_rejected") || "Rejected"}</option>
                  </select>
                </div>

                <div>
                  <label className="text-[9px] font-bold text-slate-400 uppercase tracking-widest block mb-1">{t("track_sort_by") || "Sort By"}</label>
                  <select
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                    className="w-full text-xs p-2 border border-slate-200 rounded-xl bg-slate-50 text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  >
                    <option value="latest">{t("track_sort_latest") || "Latest Updates"}</option>
                    <option value="oldest">{t("track_sort_oldest") || "Oldest Updates"}</option>
                    <option value="status">{t("track_sort_status") || "Status Order"}</option>
                    <option value="deadline">{t("track_sort_name") || "Alphabetical"}</option>
                  </select>
                </div>
              </div>
            </div>

            {/* List Stack */}
            <div className="space-y-3 max-h-[640px] overflow-y-auto pr-1">
              {sortedEntries.length === 0 ? (
                <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center text-slate-400 text-xs">
                  No applications match your active search filter criteria.
                </div>
              ) : (
                sortedEntries.map((entry) => {
                  const isSelected = selectedEntry && (entry.id || entry.schemeId) === (selectedEntry.id || selectedEntry.schemeId);
                  const badge = getStatusBadge(entry.currentStage || entry.stage);
                  const scheme = schemes.find((s) => s.id === entry.schemeId);
                  const docReadiness = scheme
                    ? getDocReadinessForScheme(scheme.requiredDocuments, documents)
                    : { readinessScore: 0 };
                  const progressPct = getOverallProgress(entry.currentStage || entry.stage);

                  // Dynamically build clean initials/logo label
                  const initials = entry.schemeName
                    .replace("Pradhan Mantri", "PM")
                    .split(" ")
                    .slice(0, 3)
                    .map((w) => w[0])
                    .join("")
                    .toUpperCase();

                  return (
                    <button
                      key={entry.id || entry.schemeId}
                      onClick={() => {
                        setSelectedId(entry.id || entry.schemeId);
                        setShowGrievance(false);
                      }}
                      className={`w-full text-left p-4 rounded-2xl border transition duration-200 flex flex-col gap-3 hover:-translate-y-0.5 hover:shadow-md ${
                        isSelected
                          ? "bg-indigo-50/40 border-indigo-500 ring-1 ring-indigo-500 shadow-sm"
                          : "bg-white border-slate-200 hover:border-slate-350"
                      }`}
                    >
                      <div className="flex items-start gap-3 w-full">
                        {/* Scheme Logo Box */}
                        <div className={`h-10 w-10 shrink-0 rounded-xl flex items-center justify-center font-black text-xs border ${getMinistryBgColor(entry.ministry)}`}>
                          {initials.slice(0, 3)}
                        </div>

                        {/* Title & Status */}
                        <div className="flex-1 min-w-0">
                          <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider truncate">
                            {entry.ministry.split(" / ")[0]}
                          </p>
                          <h3 className="text-xs font-bold text-slate-800 truncate mt-0.5">
                            {entry.schemeName}
                          </h3>
                          <p className="text-[10px] text-slate-400 font-mono mt-0.5">
                            ID: {entry.id || entry.referenceNo || "Draft Entry"}
                          </p>
                        </div>

                        {/* Badge */}
                        <span className={`text-[10px] px-2 py-0.5 border rounded-full font-bold uppercase ${badge.style} shrink-0`}>
                          {badge.label}
                        </span>
                      </div>

                      {/* Progress Metrics */}
                      <div className="space-y-2 pt-1 w-full border-t border-slate-100/60">
                        {/* Document readiness */}
                        <div>
                          <div className="flex justify-between text-[9px] text-slate-400 font-semibold mb-1">
                            <span>{t("rec_doc_readiness") || "Docs Upload Readiness"}</span>
                            <span className="text-slate-600 font-bold">{docReadiness.readinessScore}%</span>
                          </div>
                          <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                            <div className="h-full bg-emerald-500 transition-all duration-300" style={{ width: `${docReadiness.readinessScore}%` }} />
                          </div>
                        </div>

                        {/* Overall application step progress */}
                        <div>
                          <div className="flex justify-between text-[9px] text-slate-400 font-semibold mb-1">
                            <span>{t("tracker_current_stage") || "Lifecycle Pipeline Progress"}</span>
                            <span className="text-indigo-600 font-bold">{progressPct}%</span>
                          </div>
                          <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                            <div className="h-full bg-indigo-600 transition-all duration-300" style={{ width: `${progressPct}%` }} />
                          </div>
                        </div>
                      </div>

                      {/* Card Footer */}
                      <div className="flex justify-between items-center text-[10px] text-slate-400 font-medium pt-1">
                        <span>{entry._type === "applied" ? (t("tracker_applied_on") || "Applied: ") : (t("tracker_saved_on") || "Saved: ")}{entry.appliedDate || entry.savedDate}</span>
                        {entry._type === "saved" && (
                          <span className="text-[9px] text-slate-500 font-bold italic">{t("tracker_stage_saved") || "Draft saved"}</span>
                        )}
                      </div>
                    </button>
                  );
                })
              )}
            </div>

            {/* Bottom New Application Call */}
            <Link
              to="/recommendations"
              className="w-full flex items-center justify-center gap-2 py-3 bg-slate-50 hover:bg-slate-100 border border-dashed border-slate-300 rounded-xl text-xs font-bold text-slate-700 hover:text-indigo-700 transition"
            >
              <Plus className="h-4 w-4 text-slate-500" />
              <span>{t("tracker_find_schemes") || "Start New Application"}</span>
            </Link>

          </div>

          {/* ================= RIGHT COLUMN: DETAIL PANEL ================= */}
          <div className="lg:col-span-7 space-y-6">
            {selectedEntry ? (
              <div className="space-y-6">
                <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm">
                  <div className="flex flex-col sm:flex-row justify-between items-start gap-4">
                    {/* Header Info */}
                    <div className="flex gap-3">
                      <div className={`h-12 w-12 rounded-2xl flex items-center justify-center font-black text-sm border shrink-0 ${getMinistryBgColor(selectedEntry.ministry)}`}>
                        {selectedEntry.schemeName.split(" ").slice(0, 3).map((w) => w[0]).join("").toUpperCase()}
                      </div>
                      <div>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                          {selectedEntry.ministry}
                        </p>
                        <h2 className="text-base font-black text-slate-800 mt-0.5">
                          {selectedEntry.schemeName}
                        </h2>
                        <div className="flex flex-wrap gap-x-4 text-[10px] text-slate-400 mt-2 font-medium">
                          <span>{t("gen_id") || "ID"}: <strong className="font-mono text-slate-600">{selectedEntry.id || selectedEntry.referenceNo || "Draft"}</strong></span>
                          <span>{t("gen_applied") || "Submission"}: <strong className="text-slate-600">{selectedEntry.appliedDate || "Not Submitted"}</strong></span>
                        </div>
                      </div>
                    </div>

                    {/* Premium Status summary panel */}
                    <div className="bg-slate-50 border border-slate-150 rounded-xl p-3 text-right self-stretch sm:self-auto flex sm:flex-col justify-between sm:justify-start items-center sm:items-end gap-2 shrink-0">
                      <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">{t("tracker_current_stage") || "Application Status"}</span>
                      <span className={`text-xs px-2.5 py-0.5 border rounded-full font-bold uppercase ${getStatusBadge(currentStage).style}`}>
                        {getStatusBadge(currentStage).label}
                      </span>
                      <span className="text-[9px] text-slate-400 mt-1">
                        {selectedEntry.appliedDate ? `${t("tracker_applied_on") || "As of: "}${selectedEntry.appliedDate}` : `${t("tracker_saved_on") || "Saved: "}${selectedEntry.savedDate}`}
                      </span>
                    </div>
                  </div>
                </div>

                {/* 2. TIMELINE PANEL */}
                <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm">
                  <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-5">
                    <h3 className="text-xs font-bold text-slate-800 uppercase tracking-widest flex items-center gap-1.5">
                      <CalendarDays className="h-4 w-4 text-indigo-600" />
                      {t("track_stage_logs") || "Application Lifecycle Pipeline"}
                    </h3>
                    <span className="text-[10px] text-slate-400 font-medium">Auto-synced with State Portal</span>
                  </div>

                  <div className="relative pl-6 border-l-2 border-slate-100 space-y-6">
                    {/* Render timeline steps */}
                    {TIMELINE_STAGES.map((step, idx) => {
                      const historyEntry = stageHistory.find((h) => h.stage === step.key) ||
                        (step.key === "Saved" ? { date: selectedEntry.savedDate } : null) ||
                        (step.key === "Submitted" ? { date: selectedEntry.appliedDate } : null);

                      const isCompleted = idx < currentStageIdx;
                      const isCurrent = idx === currentStageIdx;
                      const isFuture = idx > currentStageIdx;
                      const isRejectedState = currentStage === "Rejected" && step.key === "Approved";

                      // Icon styling
                      let iconColor = "bg-slate-100 border-slate-300 text-slate-400";
                      let linePulse = false;

                      if (isCompleted) {
                        iconColor = "bg-emerald-500 border-emerald-500 text-white";
                      } else if (isCurrent) {
                        iconColor = "bg-indigo-650 bg-indigo-600 border-indigo-600 text-white";
                        linePulse = true;
                      }

                      const getStepTranslation = (k) => {
                        switch (k) {
                          case "Saved": return { label: t("tracker_stage_saved"), desc: t("tracker_saved_hint") || step.description };
                          case "Preparing Documents": return { label: t("tracker_stage_preparing"), desc: step.description };
                          case "Ready to Apply": return { label: t("tracker_stage_ready"), desc: step.description };
                          case "Submitted": return { label: t("tracker_stage_submitted"), desc: step.description };
                          case "Under Review": return { label: t("tracker_stage_review"), desc: step.description };
                          case "Approved": return { label: t("tracker_stage_approved"), desc: step.description };
                          case "Rejected": return { label: t("tracker_stage_rejected"), desc: step.description };
                          default: return { label: k, desc: step.description };
                        }
                      };
                      const stepTrans = getStepTranslation(step.key);

                      return (
                        <div key={idx} className="relative group">
                          {/* Pulsing indicator if current step */}
                          <div className={`absolute -left-[35px] top-1.5 h-6 w-6 rounded-full flex items-center justify-center border-2 transition-all ${iconColor}`}>
                            {isCompleted ? (
                              <Check className="h-3.5 w-3.5" />
                            ) : isCurrent ? (
                              <span className="relative flex h-2 w-2">
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-white opacity-75"></span>
                                <span className="relative inline-flex rounded-full h-2 w-2 bg-white"></span>
                              </span>
                            ) : (
                              <Circle className="h-2 w-2 text-slate-300" />
                            )}
                          </div>

                          <div className="space-y-0.5">
                            <div className="flex items-center justify-between gap-2 flex-wrap">
                              <h4 className={`text-xs font-bold ${
                                isCompleted ? "text-emerald-700" : isCurrent ? "text-indigo-700 font-extrabold" : "text-slate-400"
                              }`}>
                                {isRejectedState ? (t("tracker_stage_rejected") || "Rejected") : (stepTrans.label || step.label)}
                              </h4>
                              {historyEntry && (
                                <span className="text-[10px] text-slate-400 font-semibold">{historyEntry.date}</span>
                              )}
                            </div>
                            <p className="text-[11px] text-slate-500 max-w-lg leading-relaxed">
                              {step.key === "Approved" && currentStage === "Rejected"
                                ? "Application declined verification checks. Please address the remarks."
                                : stepTrans.desc}
                            </p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* 3. APPLICATION SUMMARY CARD */}
                <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4">
                  <h3 className="text-xs font-bold text-slate-800 uppercase tracking-widest flex items-center gap-1.5 pb-2 border-b border-slate-100">
                    <FileText className="h-4 w-4 text-indigo-600" />
                    {t("track_disbursement_details") || "Benefit Mandate Details"}
                  </h3>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                    {[
                      { label: t("det_implemented_by") || "Sponsoring Department", val: selectedEntry.ministry, icon: Building },
                            { label: t("gen_ref") || "Reference Number", val: selectedEntry.referenceNo || "Awaiting Submission", icon: ClipboardList, mono: true },
                      { label: t("profile_full_name") || "Applicant Name", val: profile?.name || "Rajesh Patel", icon: User },
                      { label: t("track_benefit_label") || "Monthly / Direct Benefit", val: schemeAdditions?.benefit || "Direct Bank Mandate", icon: CreditCard },
                      { label: t("track_expected_disbursement") || "Target Expected Decision", val: schemeAdditions?.expectedDate || "Pending Review", icon: CalendarDays },
                      { label: t("track_disbursement_details") || "Disbursement Mode", val: "Direct Benefit Transfer (DBT)", icon: ArrowUpRight }
                    ].map((item, index) => (
                      <div key={index} className="flex gap-2.5 items-start p-2 bg-slate-50 rounded-xl border border-slate-100">
                        <item.icon className="h-4.5 w-4.5 text-slate-400 shrink-0 mt-0.5" />
                        <div>
                          <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">{item.label}</p>
                          <p className={`text-slate-800 font-bold mt-0.5 ${item.mono ? "font-mono text-[11px]" : ""}`}>{item.val}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* 4. OFFICER REMARKS CARD */}
                <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-3">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
                    <h3 className="text-xs font-bold text-slate-800 uppercase tracking-widest flex items-center gap-1.5">
                      <User className="h-4 w-4 text-indigo-600" />
                      {t("track_officer_remarks") || "Verification Authority Remarks"}
                    </h3>
                    <span className={`text-[10px] px-2 py-0.5 border rounded-full font-bold flex items-center gap-1 ${
                      currentStage === "Approved"
                        ? "bg-emerald-50 text-emerald-700 border-emerald-150"
                        : currentStage === "Rejected"
                        ? "bg-rose-50 text-rose-700 border-rose-150"
                        : "bg-amber-50 text-amber-700 border-amber-150"
                    }`}>
                      {currentStage === "Approved" ? (
                        <>
                          <ShieldCheck className="h-3.5 w-3.5" /> Checked
                        </>
                      ) : currentStage === "Rejected" ? (
                        <>
                          <ShieldAlert className="h-3.5 w-3.5" /> Action Required
                        </>
                      ) : (
                        <>
                          <Clock className="h-3.5 w-3.5" /> Under Review
                        </>
                      )}
                    </span>
                  </div>

                  <div className="space-y-3 text-xs">
                    <div className="flex justify-between items-start">
                      <div>
                        <p className="font-bold text-slate-700">{schemeAdditions?.officer || "Automated Desk Officer"}</p>
                        <p className="text-[9px] text-slate-400">Department Verification Desk</p>
                      </div>
                      <span className="text-[10px] text-slate-400 font-semibold">{selectedEntry.appliedDate || selectedEntry.savedDate}</span>
                    </div>

                    <p className="text-slate-600 italic bg-slate-50 p-3 rounded-xl border border-slate-100 leading-relaxed">
                      "{schemeAdditions?.remarks || "Automatic state socio-demographic records checks passed. Initial application verified."}"
                    </p>
                  </div>
                </div>

                {/* 5. NEXT STEP ACTION CARD */}
                <div className={`p-4 rounded-2xl border flex items-start gap-3 shadow-xs ${
                  currentStage === "Rejected"
                    ? "bg-rose-50 border-rose-200 text-rose-800"
                    : currentStage === "Approved"
                    ? "bg-emerald-50 border-emerald-250 border-emerald-200 text-emerald-800"
                    : "bg-amber-50 border-amber-250 border-amber-200 text-amber-800"
                }`}>
                  {currentStage === "Rejected" ? (
                    <ShieldAlert className="h-5 w-5 text-rose-500 shrink-0 mt-0.5" />
                  ) : currentStage === "Approved" ? (
                    <ShieldCheck className="h-5 w-5 text-emerald-500 shrink-0 mt-0.5" />
                  ) : (
                    <Clock className="h-5 w-5 text-amber-500 shrink-0 mt-0.5" />
                  )}
                  <div className="space-y-1">
                    <p className="text-xs font-bold uppercase tracking-wider">{t("tracker_next") || "Citizen Next Action Required"}</p>
                    <p className="text-xs leading-relaxed font-semibold">
                      {currentStage === "Rejected"
                        ? "ITR mismatch detected. Complete document reconciliation or upload correct income certificate via vault."
                        : currentStage === "Approved"
                        ? "No actions required. First direct transfer installment scheduled for the next state DBT release cycle."
                        : selectedEntry.nextAction || "Await official desk verification. Expected completion date is listed on summary."}
                    </p>
                  </div>
                </div>

                {/* 6. AI INSIGHT CARD */}
                <div className="bg-gradient-to-br from-indigo-900 to-slate-900 border border-indigo-950 rounded-2xl p-5 shadow-md text-white space-y-2">
                  <div className="flex items-center gap-1.5">
                    <BrainCircuit className="h-4.5 w-4.5 text-indigo-300 shrink-0" />
                    <span className="text-xs font-black tracking-widest text-indigo-300 uppercase">{t("dash_insights_title") || "AI Insight Summary"}</span>
                  </div>
                  <p className="text-xs font-medium text-slate-200 leading-relaxed">
                    {currentStage === "Approved"
                      ? "Congratulations! Your application has been approved. The direct benefit amount is expected to be credited within 7–10 working days."
                      : currentStage === "Rejected"
                      ? "Your income certificate parameters mismatch with state registries. Uploading a revised income certificate showing OBC ceiling category may clear reconciliation desk."
                      : selectedReadiness?.missingDocs?.length > 0
                      ? `Your vault is missing ${selectedReadiness.missingDocs.slice(0, 1).join("")}. Uploading it will trigger automated state routing checks.`
                      : "Aadhaar e-KYC verified correctly. Pre-screening scores indicate high match success. Awaiting state desk official seal."}
                  </p>
                </div>

                {/* 7. QUICK ACTIONS */}
                <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-3">
                  <h3 className="text-xs font-bold text-slate-800 uppercase tracking-widest pb-1 border-b border-slate-100">
                    {t("dash_nudges_title") || "Quick Citizen Actions"}
                  </h3>
                  <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 text-[10px] font-bold">
                    <button
                      onClick={() => alert("Downloading official submission receipt mandate...")}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-slate-200 rounded-xl bg-slate-50 hover:bg-slate-100 hover:border-slate-300 text-slate-600 transition"
                    >
                      <Download className="h-4 w-4" /> {t("track_download") || "Download Receipt"}
                    </button>

                    <button
                      onClick={() => navigate("/documents")}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-slate-200 rounded-xl bg-slate-50 hover:bg-slate-100 hover:border-slate-300 text-slate-600 transition"
                    >
                      <FileText className="h-4 w-4" /> {t("prof_manage_vault") || "View Documents"}
                    </button>

                    <button
                      onClick={() => navigate("/help")}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-slate-200 rounded-xl bg-slate-50 hover:bg-slate-100 hover:border-slate-300 text-slate-600 transition"
                    >
                      <PhoneCall className="h-4 w-4" /> {t("err_btn_contact_support") || "Contact Support"}
                    </button>

                    <button
                      onClick={() => setShowGrievance(!showGrievance)}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-slate-200 rounded-xl bg-slate-50 hover:bg-slate-100 hover:border-slate-300 text-slate-600 transition"
                    >
                      <MessageSquareWarning className="h-4 w-4" /> {t("track_raise_grievance_btn") || "File Grievance"}
                    </button>

                    <button
                      onClick={() => {
                        navigator.clipboard.writeText(window.location.href);
                        alert("Application reference URL link copied to clipboard!");
                      }}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-slate-200 rounded-xl bg-slate-50 hover:bg-slate-100 hover:border-slate-300 text-slate-600 transition col-span-2 sm:col-span-1"
                    >
                      <Share2 className="h-4 w-4" /> {t("gen_share") || "Share App"}
                    </button>
                  </div>
                </div>

                {/* Grievance Quick Submit Drawer/Panel */}
                {showGrievance && (
                  <div className="bg-white border border-rose-250 border-rose-200 rounded-2xl p-5 shadow-sm space-y-3 animate-in fade-in slide-in-from-top-2 duration-200">
                    <h4 className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                      <MessageSquareWarning className="h-4 w-4 text-rose-500" />
                      {t("track_grievance_form_title") || "File a Grievance"} for {selectedEntry.schemeName}
                    </h4>

                    {grievanceSuccess ? (
                      <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 p-4 rounded-xl text-xs font-bold flex items-center gap-2">
                        <Check className="h-4 w-4 text-emerald-500" />
                        {t("track_grievance_success") || "Grievance ticket registered successfully. Syncing portal logs."}
                      </div>
                    ) : (
                      <form onSubmit={handleGrievanceSubmit} className="space-y-3">
                        <div className="space-y-2">
                          <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block">{t("help_label_category") || "Grievance Category"}</label>
                          <select
                            value={grievanceData.category}
                            onChange={(e) => setGrievanceData({ ...grievanceData, category: e.target.value })}
                            className="w-full text-xs p-2 border border-slate-200 rounded-xl focus:outline-none focus:ring-1 focus:ring-rose-500 bg-slate-50"
                          >
                            <option value="Delay in Processing">{t("help_cat_payment") || "Delay in Processing"}</option>
                            <option value="Document Rejected Unfairly">{t("help_cat_doc_block") || "Document Rejected Unfairly"}</option>
                            <option value="Technical Error">{t("help_cat_link_broken") || "Technical Error"}</option>
                            <option value="Other">{t("help_cat_general") || "Other Category"}</option>
                          </select>

                          <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block">{t("help_label_desc") || "Detailed Description"}</label>
                          <textarea
                            value={grievanceData.description}
                            onChange={(e) => setGrievanceData({ ...grievanceData, description: e.target.value })}
                            placeholder={t("track_grievance_desc_placeholder") || "Detail your complaint. Mention registry records and references..."}
                            rows={3}
                            required
                            className="w-full text-xs p-2 border border-slate-200 rounded-xl focus:outline-none focus:ring-1 focus:ring-rose-500 bg-slate-50"
                          />
                        </div>
                        <div className="flex justify-end gap-2 text-xs">
                          <button
                            type="button"
                            onClick={() => setShowGrievance(false)}
                            className="px-3 py-2 font-bold text-slate-500 hover:text-slate-800"
                          >
                            {t("gen_cancel") || "Cancel"}
                          </button>
                          <button
                            type="submit"
                            className="px-4 py-2 font-black bg-rose-600 hover:bg-rose-700 text-white rounded-xl flex items-center gap-1.5 shadow-sm transition"
                          >
                            <Send className="h-3.5 w-3.5" /> {t("track_grievance_form_title") || "Submit Grievance"}
                          </button>
                        </div>
                      </form>
                    )}
                  </div>
                )}

              </div>
            ) : (
              <div className="h-full bg-white border border-slate-200 rounded-2xl p-8 flex flex-col items-center justify-center text-center text-slate-400 text-xs">
                {t("tracker_empty_title") || "Select an application from the Left Panel list view to display details."}
              </div>
            )}
          </div>

        </div>
      )}

      {/* Floating SchemeBridge AI Assistant Widget */}
      <SchemeAIChatWidget
        isOpen={aiChatOpen}
        initialQuery={aiInitialQuery}
        pageContext={window.location.pathname}
        contextData={selectedEntry ? {
          page: "tracker",
          selectedApplication: selectedEntry,
          currentStatus: currentStage,
          missingDocuments: selectedReadiness?.missingDocs || [],
          timelineStage: currentStage
        } : null}
        onClose={() => {
          setAiChatOpen(false);
          setAiInitialQuery("");
        }}
      />

      {/* Floating Bot trigger button */}
      <button
        onClick={() => setAiChatOpen(true)}
        className="fixed bottom-6 right-6 h-12 w-12 rounded-full bg-indigo-650 bg-indigo-600 hover:bg-indigo-700 text-white flex items-center justify-center shadow-xl border border-indigo-500 hover:scale-105 transition z-40"
        title="Ask SchemeAI"
        aria-label="Open SchemeAI Assistant"
      >
        <BrainCircuit className="h-6 w-6" />
      </button>

    </div>
  );
}
