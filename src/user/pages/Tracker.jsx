import React, { useState, useEffect, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { getDocReadinessForScheme } from "@utils/documentReadiness";
import { ListRowSkeleton } from "@components/ui/LoadingSkeleton";
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

function getMinistryBgColor(ministry = "") {
  const m = ministry.toLowerCase();
  if (m.includes("finance")) return "bg-government-blue/10 text-government-blue border-government-blue/20";
  if (m.includes("agriculture")) return "bg-india-green/10 text-india-green border-india-green/20";
  if (m.includes("housing")) return "bg-saffron/10 text-saffron-dark border-saffron/20";
  if (m.includes("women")) return "bg-rose-50 text-rose-700 border-rose-200";
  return "bg-gray-50 text-gray-700 border-gray-200";
}

export default function Tracker() {
  const {
    applications,
    savedSchemes,
    schemes,
    documents,
    profile,
    submitGrievance
  } = useApp();

  const navigate = useNavigate();

  const [selectedId, setSelectedId] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [sortBy, setSortBy] = useState("latest");
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5;

  const [aiChatOpen, setAiChatOpen] = useState(false);
  const [aiInitialQuery, setAiInitialQuery] = useState("");

  const [showGrievance, setShowGrievance] = useState(false);
  const [grievanceData, setGrievanceData] = useState({ category: "Delay in Processing", description: "" });
  const [grievanceSuccess, setGrievanceSuccess] = useState(false);

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

  const allEntries = useMemo(() => {
    return [
      ...applications.map((a) => ({ ...a, _type: "applied" })),
      ...savedSchemes.map((s) => ({ ...s, currentStage: s.stage, _type: "saved" })),
    ];
  }, [applications, savedSchemes]);

  const filteredEntries = useMemo(() => {
    return allEntries.filter((entry) => {
      const stage = entry.currentStage || entry.stage || "Saved";
      if (statusFilter !== "all" && stage !== statusFilter) {
        return false;
      }
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

  useEffect(() => {
    setCurrentPage(1);
  }, [filteredEntries]);

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
    }
    return list;
  }, [filteredEntries, sortBy]);

  const paginatedEntries = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return sortedEntries.slice(start, start + itemsPerPage);
  }, [sortedEntries, currentPage, itemsPerPage]);

  const totalPages = Math.ceil(sortedEntries.length / itemsPerPage);

  const selectedEntry = useMemo(() => {
    if (sortedEntries.length === 0) return null;
    const found = sortedEntries.find((e) => (e.id || e.schemeId) === selectedId);
    return found || sortedEntries[0];
  }, [sortedEntries, selectedId]);

  const selectedReadiness = useMemo(() => {
    if (!selectedEntry) return null;
    const scheme = schemes.find((s) => s.id === selectedEntry.schemeId);
    return scheme ? getDocReadinessForScheme(scheme.requiredDocuments, documents) : { readinessScore: 0, missingDocs: [] };
  }, [selectedEntry, schemes, documents]);

  const stats = useMemo(() => {
    return {
      total: allEntries.length,
      applied: applications.length,
      review: applications.filter((a) => a.currentStage === "Under Review").length,
      approved: applications.filter((a) => a.currentStage === "Approved").length,
    };
  }, [allEntries, applications]);

  const handleGrievanceSubmit = (e) => {
    e.preventDefault();
    if (!grievanceData.description.trim() || !selectedEntry) return;

    submitGrievance({
      phone: profile?.phone || "9876543210",
      email: profile?.email || "citizen@schemebridge.gov.in",
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

  const getStatusBadge = (stage) => {
    switch (stage) {
      case "Saved":
        return { label: "Draft", style: "bg-gray-100 text-gray-700 border-gray-200" };
      case "Preparing Documents":
        return { label: "Preparing", style: "bg-saffron/10 text-saffron-dark border-saffron/20" };
      case "Ready to Apply":
        return { label: "Ready", style: "bg-government-blue/10 text-government-blue border-government-blue/20" };
      case "Submitted":
        return { label: "Applied", style: "bg-government-blue/10 text-government-blue border-government-blue/20" };
      case "Under Review":
        return { label: "Under Review", style: "bg-government-blue/10 text-government-blue border-government-blue/20" };
      case "Approved":
        return { label: "Approved", style: "bg-india-green/10 text-india-green border-india-green/20" };
      case "Rejected":
        return { label: "Rejected", style: "bg-red-50 text-red-700 border-red-200" };
      default:
        return { label: stage, style: "bg-gray-100 text-gray-700 border-gray-200" };
    }
  };

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
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: "Total Applications", val: stats.total, color: "border-b-gray-400 text-gray-900" },
          { label: "Submitted Applications", val: stats.applied, color: "border-b-government-blue text-government-blue" },
          { label: "Under Review", val: stats.review, color: "border-b-saffron text-saffron-dark" },
          { label: "Approved Mandates", val: stats.approved, color: "border-b-india-green text-india-green" }
        ].map((sItem, index) => (
          <div key={index} className={`bg-white border border-gray-200 border-b-4 ${sItem.color} rounded-2xl p-4 shadow-sm flex flex-col justify-between`}>
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest leading-none">{sItem.label}</span>
            <span className="text-3xl font-bold mt-2 leading-none">{sItem.val}</span>
          </div>
        ))}
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-5 space-y-4">
            <div className="bg-white border border-gray-200 p-4 rounded-2xl animate-pulse space-y-3">
              <div className="h-9 w-full bg-gray-200 rounded-xl" />
              <div className="h-10 w-full bg-gray-100 rounded-xl" />
            </div>
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-white border border-gray-200 p-5 rounded-2xl animate-pulse space-y-3">
                <div className="h-4 w-3/4 bg-gray-200 rounded" />
                <div className="h-3 w-1/2 bg-gray-100 rounded" />
                <div className="h-2 w-full bg-gray-100 rounded" />
              </div>
            ))}
          </div>
          <div className="lg:col-span-7 space-y-6">
            <ListRowSkeleton />
          </div>
        </div>
      ) : allEntries.length === 0 ? (
        <EmptyState
          icon={ClipboardList}
          title="Nothing tracked yet"
          description="Browse scheme recommendations and save or apply to schemes to track them here."
          action={{
            label: "Find Matching Schemes",
            onClick: () => navigate("/recommendations"),
            icon: Sparkles
          }}
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-5 space-y-4">
            <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-sm space-y-3">
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Search className="absolute left-2.5 top-2 h-4 w-4 text-gray-400" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search by scheme name, ID or ministry..."
                    className="w-full text-sm pl-8 pr-3 py-2 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-government-blue text-gray-800"
                  />
                </div>
                <button
                  onClick={handleRefresh}
                  className={`p-2 border border-gray-200 rounded-xl text-gray-500 hover:text-gray-800 bg-white hover:bg-gray-50 transition shrink-0 ${isRefreshing ? "animate-spin" : ""}`}
                  title="Refresh List"
                >
                  <RefreshCw className="h-4.5 w-4.5" />
                </button>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[10px] font-bold text-gray-400 uppercase tracking-widest block mb-1">Status Filter</label>
                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="w-full text-sm p-2 border border-gray-200 rounded-xl bg-gray-50 text-gray-700 focus:outline-none focus:ring-2 focus:ring-government-blue"
                  >
                    <option value="all">All Statuses</option>
                    <option value="Saved">Draft / Saved</option>
                    <option value="Preparing Documents">Preparing</option>
                    <option value="Ready to Apply">Ready to Apply</option>
                    <option value="Submitted">Submitted / Applied</option>
                    <option value="Under Review">Under Review</option>
                    <option value="Approved">Approved</option>
                    <option value="Rejected">Rejected</option>
                  </select>
                </div>

                <div>
                  <label className="text-[10px] font-bold text-gray-400 uppercase tracking-widest block mb-1">Sort By</label>
                  <select
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                    className="w-full text-sm p-2 border border-gray-200 rounded-xl bg-gray-50 text-gray-700 focus:outline-none focus:ring-2 focus:ring-government-blue"
                  >
                    <option value="latest">Latest Updates</option>
                    <option value="oldest">Oldest Updates</option>
                    <option value="status">Status Order</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="space-y-3 max-h-[560px] overflow-y-auto pr-1">
              {sortedEntries.length === 0 ? (
                <div className="bg-white border border-gray-200 rounded-2xl p-10 text-center text-gray-400 text-sm">
                  No applications match your active search filter criteria.
                </div>
              ) : (
                paginatedEntries.map((entry) => {
                  const isSelected = selectedEntry && (entry.id || entry.schemeId) === (selectedEntry.id || selectedEntry.schemeId);
                  const badge = getStatusBadge(entry.currentStage || entry.stage);
                  const scheme = schemes.find((s) => s.id === entry.schemeId);
                  const docReadiness = scheme ? getDocReadinessForScheme(scheme.requiredDocuments, documents) : { readinessScore: 0 };
                  const progressPct = getOverallProgress(entry.currentStage || entry.stage);

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
                          ? "bg-government-blue/5 border-government-blue ring-1 ring-government-blue/30 shadow-sm"
                          : "bg-white border-gray-200 hover:border-gray-300"
                      }`}
                    >
                      <div className="flex items-start gap-3 w-full">
                        <div className={`h-10 w-10 shrink-0 rounded-xl flex items-center justify-center font-bold text-sm border ${getMinistryBgColor(entry.ministry)}`}>
                          {initials.slice(0, 3)}
                        </div>

                        <div className="flex-1 min-w-0">
                          <p className="text-[10px] text-gray-400 font-bold uppercase tracking-wider truncate">
                            {entry.ministry.split(" / ")[0]}
                          </p>
                          <h3 className="text-sm font-bold text-gray-800 truncate mt-0.5">
                            {entry.schemeName}
                          </h3>
                          <p className="text-[10px] text-gray-400 font-mono mt-0.5">
                            ID: {entry.id || entry.referenceNo || "Draft Entry"}
                          </p>
                        </div>

                        <span className={`text-[10px] px-2 py-0.5 border rounded-full font-bold uppercase ${badge.style} shrink-0`}>
                          {badge.label}
                        </span>
                      </div>

                      <div className="space-y-2 pt-1 w-full border-t border-gray-100/60">
                        <div>
                          <div className="flex justify-between text-[10px] text-gray-400 font-semibold mb-1">
                            <span>Document Readiness</span>
                            <span className="text-gray-600 font-bold">{docReadiness.readinessScore}%</span>
                          </div>
                          <div className="h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
                            <div className="h-full bg-india-green transition-all duration-300" style={{ width: `${docReadiness.readinessScore}%` }} />
                          </div>
                        </div>

                        <div>
                          <div className="flex justify-between text-[10px] text-gray-400 font-semibold mb-1">
                            <span>Lifecycle Pipeline Progress</span>
                            <span className="text-government-blue font-bold">{progressPct}%</span>
                          </div>
                          <div className="h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
                            <div className="h-full bg-government-blue transition-all duration-300" style={{ width: `${progressPct}%` }} />
                          </div>
                        </div>
                      </div>

                      <div className="flex justify-between items-center text-[10px] text-gray-400 font-medium pt-1">
                        <span>{entry._type === "applied" ? "Applied: " : "Saved: "}{entry.appliedDate || entry.savedDate}</span>
                        {entry._type === "saved" && (
                          <span className="text-[9px] text-gray-500 font-bold italic">Draft saved</span>
                        )}
                      </div>
                    </button>
                  );
                })
              )}
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-between bg-white border border-gray-200 rounded-2xl p-3 text-sm">
                <span className="text-gray-500 font-semibold">
                  Page <span className="text-gray-800 font-bold">{currentPage}</span> of <span className="text-gray-800 font-bold">{totalPages}</span>
                </span>
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                    disabled={currentPage === 1}
                    className="px-2.5 py-1 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
                  >
                    Prev
                  </button>
                  {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                    <button
                      key={page}
                      onClick={() => setCurrentPage(page)}
                      className={`px-2.5 py-1 rounded-lg text-sm font-bold transition ${
                        page === currentPage
                          ? "bg-government-blue text-white shadow-sm"
                          : "border border-gray-200 text-gray-600 hover:bg-gray-50"
                      }`}
                    >
                      {page}
                    </button>
                  ))}
                  <button
                    onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                    disabled={currentPage === totalPages}
                    className="px-2.5 py-1 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}

            <Link
              to="/recommendations"
              className="w-full flex items-center justify-center gap-2 py-3 bg-gray-50 hover:bg-gray-100 border border-dashed border-gray-300 rounded-xl text-sm font-bold text-gray-700 hover:text-government-blue transition"
            >
              <Plus className="h-4 w-4 text-gray-500" />
              <span>Start New Application</span>
            </Link>
          </div>

          <div className="lg:col-span-7 space-y-6">
            {selectedEntry ? (
              <div className="space-y-6">
                <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
                  <div className="flex flex-col sm:flex-row justify-between items-start gap-4">
                    <div className="flex gap-3">
                      <div className={`h-12 w-12 rounded-2xl flex items-center justify-center font-bold text-base border shrink-0 ${getMinistryBgColor(selectedEntry.ministry)}`}>
                        {selectedEntry.schemeName.split(" ").slice(0, 3).map((w) => w[0]).join("").toUpperCase()}
                      </div>
                      <div>
                        <p className="text-[10px] text-gray-400 font-bold uppercase tracking-wider">
                          {selectedEntry.ministry}
                        </p>
                        <h2 className="text-lg font-bold text-gray-800 mt-0.5">
                          {selectedEntry.schemeName}
                        </h2>
                        <div className="flex flex-wrap gap-x-4 text-[10px] text-gray-400 mt-2 font-medium">
                          <span>ID: <strong className="font-mono text-gray-600">{selectedEntry.id || selectedEntry.referenceNo || "Draft"}</strong></span>
                          <span>Submission: <strong className="text-gray-600">{selectedEntry.appliedDate || "Not Submitted"}</strong></span>
                        </div>
                      </div>
                    </div>

                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-3 text-right self-stretch sm:self-auto flex sm:flex-col justify-between sm:justify-start items-center sm:items-end gap-2 shrink-0">
                      <span className="text-[9px] font-bold text-gray-400 uppercase tracking-widest">Application Status</span>
                      <span className={`text-sm px-2.5 py-0.5 border rounded-full font-bold uppercase ${getStatusBadge(currentStage).style}`}>
                        {getStatusBadge(currentStage).label}
                      </span>
                      <span className="text-[10px] text-gray-400 mt-1">
                        {selectedEntry.appliedDate ? `As of: ${selectedEntry.appliedDate}` : `Saved: ${selectedEntry.savedDate}`}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-sm">
                  <div className="flex items-center justify-between pb-3 border-b border-gray-100 mb-5">
                    <h3 className="text-sm font-bold text-gray-800 uppercase tracking-widest flex items-center gap-1.5">
                      <CalendarDays className="h-4 w-4 text-government-blue" />
                      Application Lifecycle Pipeline
                    </h3>
                    <span className="text-[10px] text-gray-400 font-medium">Auto-synced with State Portal</span>
                  </div>

                  <div className="relative pl-6 border-l-2 border-gray-100 space-y-6">
                    {TIMELINE_STAGES.map((stage, idx) => {
                      const historyEntry = stageHistory.find((h) => h.stage === stage.key) ||
                        (stage.key === "Saved" ? { date: selectedEntry.savedDate } : null) ||
                        (stage.key === "Submitted" ? { date: selectedEntry.appliedDate } : null);

                      const isCompleted = idx < currentStageIdx;
                      const isCurrent = idx === currentStageIdx;
                      const isFuture = idx > currentStageIdx;
                      const isRejectedState = currentStage === "Rejected" && stage.key === "Approved";

                      let iconColor = "bg-gray-100 border-gray-300 text-gray-400";
                      let linePulse = false;

                      if (isCompleted) {
                        iconColor = "bg-india-green border-india-green text-white";
                      } else if (isCurrent) {
                        iconColor = "bg-government-blue border-government-blue text-white";
                        linePulse = true;
                      }

                      return (
                        <div key={idx} className="relative group">
                          <div className={`absolute -left-[35px] top-1.5 h-6 w-6 rounded-full flex items-center justify-center border-2 transition-all ${iconColor}`}>
                            {isCompleted ? (
                              <Check className="h-3.5 w-3.5" />
                            ) : isCurrent ? (
                              <span className="relative flex h-2 w-2">
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-white opacity-75"></span>
                                <span className="relative inline-flex rounded-full h-2 w-2 bg-white"></span>
                              </span>
                            ) : (
                              <Circle className="h-2 w-2 text-gray-300" />
                            )}
                          </div>

                          <div className="space-y-0.5">
                            <div className="flex items-center justify-between gap-2 flex-wrap">
                              <h4 className={`text-sm font-bold ${
                                isCompleted ? "text-india-green" : isCurrent ? "text-government-blue font-extrabold" : "text-gray-400"
                              }`}>
                                {isRejectedState ? "Rejected" : stage.label}
                              </h4>
                              {historyEntry && (
                                <span className="text-[10px] text-gray-400 font-semibold">{historyEntry.date}</span>
                              )}
                            </div>
                            <p className="text-xs text-gray-500 max-w-lg leading-relaxed">
                              {stage.key === "Approved" && currentStage === "Rejected"
                                ? "Application declined verification checks. Please address the remarks."
                                : stage.description}
                            </p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm space-y-4">
                  <h3 className="text-sm font-bold text-gray-800 uppercase tracking-widest flex items-center gap-1.5 pb-2 border-b border-gray-100">
                    <FileText className="h-4 w-4 text-government-blue" />
                    Benefit Mandate Details
                  </h3>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                    {[
                      { label: "Sponsoring Department", val: selectedEntry.ministry, icon: Building },
                      { label: "Reference Number", val: selectedEntry.referenceNo || "Awaiting Submission", icon: ClipboardList, mono: true },
                      { label: "Applicant Name", val: profile?.name || "Rajesh Kumar", icon: User },
                      { label: "Monthly / Direct Benefit", val: schemeAdditions?.benefit || "Direct Bank Mandate", icon: CreditCard },
                      { label: "Target Expected Decision", val: schemeAdditions?.expectedDate || "Pending Review", icon: CalendarDays },
                      { label: "Disbursement Mode", val: "Direct Benefit Transfer (DBT)", icon: ArrowUpRight }
                    ].map((item, index) => (
                      <div key={index} className="flex gap-2.5 items-start p-2 bg-gray-50 rounded-xl border border-gray-100">
                        <item.icon className="h-4.5 w-4.5 text-gray-400 shrink-0 mt-0.5" />
                        <div>
                          <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{item.label}</p>
                          <p className={`text-gray-800 font-bold mt-0.5 ${item.mono ? "font-mono text-sm" : ""}`}>{item.val}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm space-y-3">
                  <div className="flex items-center justify-between border-b border-gray-100 pb-2.5">
                    <h3 className="text-sm font-bold text-gray-800 uppercase tracking-widest flex items-center gap-1.5">
                      <User className="h-4 w-4 text-government-blue" />
                      Verification Authority Remarks
                    </h3>
                    <span className={`text-[10px] px-2 py-0.5 border rounded-full font-bold flex items-center gap-1 ${
                      currentStage === "Approved"
                        ? "bg-india-green/10 text-india-green border-india-green/20"
                        : currentStage === "Rejected"
                        ? "bg-red-50 text-red-700 border-red-200"
                        : "bg-saffron/10 text-saffron-dark border-saffron/20"
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
                        <p className="font-bold text-gray-700">{schemeAdditions?.officer || "Automated Desk Officer"}</p>
                        <p className="text-[10px] text-gray-400">Department Verification Desk</p>
                      </div>
                      <span className="text-[10px] text-gray-400 font-semibold">{selectedEntry.appliedDate || selectedEntry.savedDate}</span>
                    </div>

                    <p className="text-gray-600 italic bg-gray-50 p-3 rounded-xl border border-gray-100 leading-relaxed">
                      "{schemeAdditions?.remarks || "Automatic state socio-demographic records checks passed. Initial application verified."}"
                    </p>
                  </div>
                </div>

                <div className={`p-4 rounded-2xl border flex items-start gap-3 shadow-sm ${
                  currentStage === "Rejected"
                    ? "bg-red-50 border-red-200 text-red-800"
                    : currentStage === "Approved"
                    ? "bg-india-green/5 border-india-green/20 text-india-green-dark"
                    : "bg-saffron/10 border-saffron/20 text-saffron-dark"
                }`}>
                  {currentStage === "Rejected" ? (
                    <ShieldAlert className="h-5 w-5 text-red-500 shrink-0 mt-0.5" />
                  ) : currentStage === "Approved" ? (
                    <ShieldCheck className="h-5 w-5 text-india-green shrink-0 mt-0.5" />
                  ) : (
                    <Clock className="h-5 w-5 text-saffron-dark shrink-0 mt-0.5" />
                  )}
                  <div className="space-y-1">
                    <p className="text-xs font-bold uppercase tracking-wider">Citizen Next Action Required</p>
                    <p className="text-xs leading-relaxed font-medium">
                      {currentStage === "Rejected"
                        ? "ITR mismatch detected. Complete document reconciliation or upload revised income certificate via vault."
                        : currentStage === "Approved"
                        ? "No actions required. First direct transfer installment scheduled for next state DBT release cycle."
                        : selectedEntry.nextAction || "Awaiting official desk verification. Expected completion date listed on summary."}
                    </p>
                  </div>
                </div>

                <div className="bg-gradient-to-br from-government-blue to-government-blue-dark border border-government-blue/20 rounded-2xl p-5 shadow-md text-white space-y-2">
                  <div className="flex items-center gap-1.5">
                    <BrainCircuit className="h-4.5 w-4.5 text-saffron shrink-0" />
                    <span className="text-xs font-bold tracking-widest text-saffron uppercase">AI Insight Summary</span>
                  </div>
                  <p className="text-xs font-medium text-white/90 leading-relaxed">
                    {currentStage === "Rejected"
                      ? "Your income certificate parameters mismatch with state registries. Uploading a revised income certificate showing OBC ceiling category may clear reconciliation desk."
                      : selectedReadiness?.missingDocs?.length > 0
                      ? `Your vault is missing ${selectedReadiness.missingDocs.slice(0, 1).join("")}. Uploading it will trigger automated state routing checks.`
                      : "Aadhaar e-KYC verified correctly. Pre-screening scores indicate high match success. Awaiting state desk official seal."}
                  </p>
                </div>

                <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm space-y-3">
                  <h3 className="text-xs font-bold text-gray-800 uppercase tracking-widest pb-1 border-b border-gray-100">
                    Quick Citizen Actions
                  </h3>
                  <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 text-[10px] font-bold">
                    <button
                      onClick={() => alert("Downloading official submission receipt mandate...")}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-gray-200 rounded-xl bg-gray-50 hover:bg-gray-100 hover:border-gray-300 text-gray-600 transition"
                    >
                      <Download className="h-4 w-4" /> Download Receipt
                    </button>

                    <button
                      onClick={() => navigate("/documents")}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-gray-200 rounded-xl bg-gray-50 hover:bg-gray-100 hover:border-gray-300 text-gray-600 transition"
                    >
                      <FileText className="h-4 w-4" /> View Documents
                    </button>

                    <button
                      onClick={() => navigate("/help")}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-gray-200 rounded-xl bg-gray-50 hover:bg-gray-100 hover:border-gray-300 text-gray-600 transition"
                    >
                      <PhoneCall className="h-4 w-4" /> Contact Support
                    </button>

                    <button
                      onClick={() => setShowGrievance(!showGrievance)}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-gray-200 rounded-xl bg-gray-50 hover:bg-gray-100 hover:border-gray-300 text-gray-600 transition"
                    >
                      <MessageSquareWarning className="h-4 w-4" /> File Grievance
                    </button>

                    <button
                      onClick={() => {
                        navigator.clipboard.writeText(window.location.href);
                        alert("Application reference URL link copied to clipboard!");
                      }}
                      className="flex flex-col items-center justify-center gap-1.5 p-2.5 border border-gray-200 rounded-xl bg-gray-50 hover:bg-gray-100 hover:border-gray-300 text-gray-600 transition col-span-2 sm:col-span-1"
                    >
                      <Share2 className="h-4 w-4" /> Share App
                    </button>
                  </div>
                </div>

                {showGrievance && (
                  <div className="bg-white border border-red-200 rounded-2xl p-5 shadow-sm space-y-3 animate-in fade-in slide-in-from-top-2 duration-200">
                    <h4 className="text-sm font-bold text-gray-800 flex items-center gap-1.5">
                      <MessageSquareWarning className="h-4 w-4 text-red-500" />
                      File a Grievance for {selectedEntry.schemeName}
                    </h4>

                    {grievanceSuccess ? (
                      <div className="bg-india-green/5 border border-india-green/20 text-india-green p-4 rounded-xl text-xs font-bold flex items-center gap-2">
                        <Check className="h-4 w-4 text-india-green" />
                        Grievance ticket registered successfully. Syncing portal logs.
                      </div>
                    ) : (
                      <form onSubmit={handleGrievanceSubmit} className="space-y-3">
                        <div className="space-y-2">
                          <label className="text-[10px] font-bold text-gray-400 uppercase tracking-widest block">Grievance Category</label>
                          <select
                            value={grievanceData.category}
                            onChange={(e) => setGrievanceData({ ...grievanceData, category: e.target.value })}
                            className="w-full text-sm p-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500 bg-gray-50"
                          >
                            <option value="Delay in Processing">Delay in Processing</option>
                            <option value="Document Rejected Unfairly">Document Rejected Unfairly</option>
                            <option value="Technical Error">Technical Error</option>
                            <option value="Other">Other Category</option>
                          </select>

                          <label className="text-[10px] font-bold text-gray-400 uppercase tracking-widest block">Detailed Description</label>
                          <textarea
                            value={grievanceData.description}
                            onChange={(e) => setGrievanceData({ ...grievanceData, description: e.target.value })}
                            placeholder="Detail your complaint. Mention registry records and references..."
                            rows={3}
                            required
                            className="w-full text-sm p-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500 bg-gray-50"
                          />
                        </div>
                        <div className="flex justify-end gap-2 text-sm">
                          <button
                            type="button"
                            onClick={() => setShowGrievance(false)}
                            className="px-3 py-2 font-bold text-gray-500 hover:text-gray-800"
                          >
                            Cancel
                          </button>
                          <button
                            type="submit"
                            className="px-4 py-2 font-extrabold bg-red-600 hover:bg-red-700 text-white rounded-xl flex items-center gap-1.5 shadow-sm transition"
                          >
                            <Send className="h-3.5 w-3.5" /> Submit Grievance
                          </button>
                        </div>
                      </form>
                    )}
                  </div>
                )}

              </div>
            ) : (
              <div className="h-full bg-white border border-gray-200 rounded-2xl p-8 flex flex-col items-center justify-center text-center text-gray-400 text-sm">
                Select an application from the Left Panel list view to display details.
              </div>
            )}
          </div>
        </div>
      )}

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

      <button
        onClick={() => setAiChatOpen(true)}
        className="fixed bottom-6 right-6 h-12 w-12 rounded-full bg-government-blue hover:bg-government-blue-dark text-white flex items-center justify-center shadow-xl border border-government-blue-light hover:scale-105 transition z-40"
        title="Ask SchemeAI"
        aria-label="Open SchemeAI Assistant"
      >
        <BrainCircuit className="h-6 w-6" />
      </button>

    </div>
  );
}
