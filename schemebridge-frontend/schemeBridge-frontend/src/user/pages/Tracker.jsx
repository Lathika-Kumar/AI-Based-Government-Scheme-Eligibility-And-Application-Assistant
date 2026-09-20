import React, { useState, useEffect, useMemo, useCallback } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { getMyApplications, getApplicationTimeline } from "@services/applicationService";
import { usePageMeta } from "@utils/usePageMeta";
import EmptyState from "@components/ui/EmptyState";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import { useToast } from "@components/ui/ToastNotification";
import {
  ClipboardList,
  CheckCircle2,
  Clock,
  AlertCircle,
  ArrowRight,
  CalendarDays,
  Sparkles,
  FileText,
  ChevronLeft,
  ChevronRight,
  CircleDot,
  Circle,
  MessageSquareWarning,
  Send,
  Search,
  RefreshCw,
  Building,
  Check,
  ShieldCheck,
  ShieldAlert,
  ArrowUpRight,
  HelpCircle,
} from "lucide-react";

const STAGE_MAP = {
  DOCUMENTS_PENDING: { label: "Preparing Documents", idx: 1, style: "bg-saffron/10 text-saffron-dark border-saffron/20" },
  READY_FOR_SUBMISSION: { label: "Ready to Apply", idx: 2, style: "bg-government-blue/10 text-government-blue border-government-blue/20" },
  SUBMITTED: { label: "Submitted", idx: 3, style: "bg-government-blue/10 text-government-blue border-government-blue/20" },
  UNDER_REVIEW: { label: "Under Review", idx: 4, style: "bg-government-blue/10 text-government-blue border-government-blue/20" },
  CORRECTION_REQUIRED: { label: "Correction Required", idx: 4, style: "bg-amber-50 text-amber-700 border-amber-300" },
  APPROVED: { label: "Approved", idx: 5, style: "bg-india-green/10 text-india-green border-india-green/20" },
  REJECTED: { label: "Rejected", idx: 5, style: "bg-red-50 text-red-700 border-red-200" },
  CANCELLED: { label: "Cancelled", idx: 5, style: "bg-gray-100 text-gray-700 border-gray-200" },
};

const TIMELINE_STAGES = [
  { key: "APPLICATION_CREATED", label: "Created", description: "Application record initiated on SchemeBridge." },
  { key: "DOCUMENTS_PENDING", label: "Preparing Documents", description: "Upload mandatory verification documents." },
  { key: "READY_FOR_SUBMISSION", label: "Ready to Submit", description: "All mandatory documents satisfied." },
  { key: "APPLICATION_SUBMITTED", label: "Submitted", description: "Application submitted to authorities." },
  { key: "UNDER_REVIEW", label: "Under Review", description: "Desk review and registry verification." },
  { key: "APPROVED", label: "Final Decision", description: "Approval / Rejection decision finalized." },
];

function getOverallProgress(status) {
  switch (status) {
    case "DOCUMENTS_PENDING": return 30;
    case "READY_FOR_SUBMISSION": return 50;
    case "SUBMITTED": return 70;
    case "UNDER_REVIEW": return 85;
    case "CORRECTION_REQUIRED": return 60;
    case "APPROVED": return 100;
    case "REJECTED": return 100;
    case "CANCELLED": return 100;
    default: return 20;
  }
}

export default function Tracker() {
  usePageMeta("Application Tracker", "Track status of submitted schemes & applications");
  const { profile, submitGrievance } = useApp();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [realApps, setRealApps] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [timelineEvents, setTimelineEvents] = useState([]);
  const [timelineLoading, setTimelineLoading] = useState(false);

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

  const fetchApplications = useCallback(async () => {
    try {
      const res = await getMyApplications();
      if (!res.error && Array.isArray(res.data)) {
        setRealApps(res.data);
      }
    } catch (err) {
      console.error("Failed to load applications from Scheme Service:", err);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchApplications();
  }, [fetchApplications]);

  const handleRefresh = () => {
    setIsRefreshing(true);
    fetchApplications();
  };

  // Map backend ApplicationResponse to tracker presentation models
  const allEntries = useMemo(() => {
    return realApps.map((app) => {
      const title = app.schemeTitle?.english || app.schemeTitle?.hindi || app.schemeCode;
      const stageInfo = STAGE_MAP[app.status] || { label: app.status, idx: 0, style: "bg-gray-100 text-gray-700" };
      const appliedDate = app.submittedAt
        ? app.submittedAt.split("T")[0]
        : app.createdAt
        ? app.createdAt.split("T")[0]
        : "Recent";

      return {
        id: app.id,
        applicationNumber: app.applicationNumber,
        schemeCode: app.schemeCode,
        schemeName: title,
        ministry: "National Welfare Gateway",
        status: app.status,
        currentStage: stageInfo.label,
        stageStyle: stageInfo.style,
        stageIdx: stageInfo.idx,
        appliedDate,
        submittedAt: app.submittedAt,
        createdAt: app.createdAt,
        documentReadiness: app.documentReadiness || { total: 0, uploaded: 0, percentage: 100 },
        documents: app.documents || [],
      };
    });
  }, [realApps]);

  const filteredEntries = useMemo(() => {
    return allEntries.filter((entry) => {
      if (statusFilter !== "all" && entry.status !== statusFilter && entry.currentStage !== statusFilter) {
        return false;
      }
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesName = entry.schemeName.toLowerCase().includes(q);
        const matchesId = (entry.id || "").toLowerCase().includes(q);
        const matchesRef = (entry.applicationNumber || "").toLowerCase().includes(q);
        const matchesCode = (entry.schemeCode || "").toLowerCase().includes(q);
        return matchesName || matchesId || matchesRef || matchesCode;
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
      list.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
    } else if (sortBy === "oldest") {
      list.sort((a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0));
    } else if (sortBy === "status") {
      list.sort((a, b) => a.status.localeCompare(b.status));
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
    const found = sortedEntries.find((e) => e.id === selectedId);
    return found || sortedEntries[0];
  }, [sortedEntries, selectedId]);

  // Load timeline for selected application
  useEffect(() => {
    let isMounted = true;
    if (selectedEntry?.id) {
      setTimelineLoading(true);
      getApplicationTimeline(selectedEntry.id)
        .then((res) => {
          if (isMounted && !res.error && res.data?.events) {
            setTimelineEvents(res.data.events);
          } else if (isMounted) {
            setTimelineEvents([]);
          }
        })
        .catch(() => {
          if (isMounted) setTimelineEvents([]);
        })
        .finally(() => {
          if (isMounted) setTimelineLoading(false);
        });
    } else {
      setTimelineEvents([]);
    }
    return () => { isMounted = false; };
  }, [selectedEntry?.id]);

  const stats = useMemo(() => {
    return {
      total: allEntries.length,
      pending: allEntries.filter((a) => a.status === "DOCUMENTS_PENDING" || a.status === "READY_FOR_SUBMISSION").length,
      review: allEntries.filter((a) => a.status === "UNDER_REVIEW" || a.status === "SUBMITTED").length,
      approved: allEntries.filter((a) => a.status === "APPROVED").length,
    };
  }, [allEntries]);

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
    showToast("success", "Grievance Filed", "Your grievance has been lodged successfully.");
    setTimeout(() => {
      setGrievanceSuccess(false);
      setShowGrievance(false);
    }, 2000);
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-1 sm:px-4">
      {/* Top Stat Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: "All Applications", val: stats.total, color: "border-b-gray-400 dark:border-b-slate-500 text-gray-900 dark:text-slate-100" },
          { label: "Pending Uploads", val: stats.pending, color: "border-b-saffron text-saffron-dark dark:text-amber-400" },
          { label: "Under Review", val: stats.review, color: "border-b-government-blue dark:border-b-indigo-500 text-government-blue dark:text-indigo-400" },
          { label: "Approved", val: stats.approved, color: "border-b-india-green text-india-green dark:text-emerald-400" },
        ].map((sItem, index) => (
          <div key={index} className={`bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 border-b-4 ${sItem.color} rounded-2xl p-4 shadow-sm flex flex-col justify-between`}>
            <span className="text-[10px] font-bold text-gray-400 dark:text-slate-400 uppercase tracking-widest leading-none">{sItem.label}</span>
            <span className="text-3xl font-bold mt-2 leading-none">{sItem.val}</span>
          </div>
        ))}
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="rounded-2xl bg-gray-100 dark:bg-slate-800 animate-pulse h-32" />
          ))}
        </div>
      ) : allEntries.length === 0 ? (
        <div className="py-16 flex flex-col items-center text-center text-gray-400 dark:text-slate-500">
          <ClipboardList className="h-12 w-12 opacity-40 mb-4" />
          <h3 className="text-sm font-bold text-gray-600 dark:text-slate-300">No Applications Found</h3>
          <p className="text-xs mt-1 max-w-xs">You haven't submitted any government scheme applications yet.</p>
          <Link
            to="/schemes"
            className="mt-5 inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold bg-slate-900 dark:bg-indigo-600 text-white hover:bg-slate-800 dark:hover:bg-indigo-700 shadow-xs transition"
          >
            <Sparkles className="h-4 w-4" />
            Find & Apply for Schemes
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left Column: Applications List */}
          <div className="lg:col-span-5 space-y-4">
            <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-4 shadow-sm space-y-3">
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-gray-400" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search by scheme name or number..."
                    className="w-full text-sm pl-8 pr-3 py-2 bg-gray-50 dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400 text-gray-800 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500"
                  />
                </div>
                <button
                  onClick={handleRefresh}
                  className={`p-2 border border-gray-200 dark:border-slate-700 rounded-xl text-gray-500 dark:text-slate-400 hover:text-gray-800 dark:hover:text-slate-200 bg-white dark:bg-slate-800 hover:bg-gray-50 dark:hover:bg-slate-700 transition shrink-0 ${isRefreshing ? "animate-spin" : ""}`}
                  title="Refresh Applications"
                >
                  <RefreshCw className="h-4.5 w-4.5" />
                </button>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[10px] font-bold text-gray-400 dark:text-slate-400 uppercase tracking-widest block mb-1">Filter Status</label>
                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="w-full text-sm p-2 border border-gray-200 dark:border-slate-700 rounded-xl bg-gray-50 dark:bg-slate-800 text-gray-700 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400"
                  >
                    <option value="all">All Statuses</option>
                    <option value="DOCUMENTS_PENDING">Documents Pending</option>
                    <option value="READY_FOR_SUBMISSION">Ready to Apply</option>
                    <option value="SUBMITTED">Submitted</option>
                    <option value="UNDER_REVIEW">Under Review</option>
                    <option value="CORRECTION_REQUIRED">Correction Required</option>
                    <option value="APPROVED">Approved</option>
                    <option value="REJECTED">Rejected</option>
                  </select>
                </div>

                <div>
                  <label className="text-[10px] font-bold text-gray-400 dark:text-slate-400 uppercase tracking-widest block mb-1">Sort By</label>
                  <select
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                    className="w-full text-sm p-2 border border-gray-200 dark:border-slate-700 rounded-xl bg-gray-50 dark:bg-slate-800 text-gray-700 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400"
                  >
                    <option value="latest">Latest Updates</option>
                    <option value="oldest">Oldest Updates</option>
                    <option value="status">Status Order</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="space-y-3 max-h-[60vh] overflow-y-auto pr-1" role="list">
              {sortedEntries.length === 0 ? (
                <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-10 text-center text-gray-400 dark:text-slate-500 text-sm">
                  No applications match your filter criteria.
                </div>
              ) : (
                paginatedEntries.map((entry) => {
                  const isSelected = selectedEntry && entry.id === selectedEntry.id;
                  const progressPct = getOverallProgress(entry.status);

                  return (
                    <div
                      key={entry.id}
                      onClick={() => setSelectedId(entry.id)}
                      className={`bg-white dark:bg-slate-900 border rounded-2xl p-4 transition cursor-pointer relative ${
                        isSelected
                          ? "border-government-blue dark:border-indigo-500 ring-2 ring-government-blue/20 dark:ring-indigo-500/20 shadow-md"
                          : "border-gray-200 dark:border-slate-800 hover:border-gray-300 dark:hover:border-slate-700 hover:shadow-xs"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-start gap-3 min-w-0">
                          <div className="h-10 w-10 rounded-xl flex items-center justify-center font-bold text-xs border shrink-0 bg-government-blue/10 dark:bg-indigo-950/50 text-government-blue dark:text-indigo-400 border-government-blue/20 dark:border-indigo-800">
                            SB
                          </div>
                          <div className="min-w-0">
                            <h4 className="text-xs font-bold text-gray-900 dark:text-slate-100 truncate">
                              {entry.schemeName}
                            </h4>
                            <p className="text-[10px] text-gray-400 dark:text-slate-400 font-medium truncate mt-0.5">
                              {entry.applicationNumber}
                            </p>
                          </div>
                        </div>

                        <span className={`text-[9px] px-2 py-0.5 border rounded-full font-bold uppercase shrink-0 ${entry.stageStyle}`}>
                          {entry.currentStage}
                        </span>
                      </div>

                      <div className="mt-3 pt-3 border-t border-gray-100 dark:border-slate-800 flex items-center justify-between text-[10px] text-gray-400 dark:text-slate-500">
                        <span>Created: {entry.appliedDate}</span>
                        <span className="font-semibold text-gray-600 dark:text-slate-300">{progressPct}% Complete</span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-3 shadow-xs">
                <span className="text-xs text-gray-500 dark:text-slate-400 font-medium">
                  Page {currentPage} of {totalPages}
                </span>
                <div className="flex gap-2">
                  <button
                    disabled={currentPage === 1}
                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                    className="p-1.5 border border-gray-200 dark:border-slate-700 rounded-xl text-gray-600 dark:text-slate-300 hover:bg-gray-50 dark:hover:bg-slate-800 disabled:opacity-30 transition"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                  <button
                    disabled={currentPage === totalPages}
                    onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                    className="p-1.5 border border-gray-200 dark:border-slate-700 rounded-xl text-gray-600 dark:text-slate-300 hover:bg-gray-50 dark:hover:bg-slate-800 disabled:opacity-30 transition"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Right Column: Detailed Application View */}
          <div className="lg:col-span-7">
            {selectedEntry ? (
              <div className="space-y-6">
                {/* Header Card */}
                <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
                  <div className="flex flex-col sm:flex-row justify-between items-start gap-4">
                    <div className="flex gap-3">
                      <div className="h-12 w-12 rounded-2xl flex items-center justify-center font-bold text-base border shrink-0 bg-government-blue text-white">
                        SB
                      </div>
                      <div>
                        <p className="text-[10px] text-gray-400 dark:text-slate-400 font-bold uppercase tracking-wider">
                          Official Application Record
                        </p>
                        <h2 className="text-lg font-bold text-gray-800 dark:text-slate-100 mt-0.5">
                          {selectedEntry.schemeName}
                        </h2>
                        <div className="flex flex-wrap gap-x-4 text-[10px] text-gray-400 dark:text-slate-400 mt-2 font-medium">
                          <span>Application No: <strong className="font-mono text-gray-700 dark:text-slate-200">{selectedEntry.applicationNumber}</strong></span>
                          <span>Scheme Code: <strong className="font-mono text-gray-700 dark:text-slate-200">{selectedEntry.schemeCode}</strong></span>
                        </div>
                      </div>
                    </div>

                    <div className="bg-gray-50 dark:bg-slate-800/60 border border-gray-100 dark:border-slate-700 rounded-xl p-3 text-right self-stretch sm:self-auto flex sm:flex-col justify-between sm:justify-start items-center sm:items-end gap-2 shrink-0">
                      <span className="text-[9px] font-bold text-gray-400 dark:text-slate-400 uppercase tracking-widest">Status</span>
                      <span className={`text-sm px-2.5 py-0.5 border rounded-full font-bold uppercase ${selectedEntry.stageStyle}`}>
                        {selectedEntry.currentStage}
                      </span>
                      <span className="text-[10px] text-gray-400 dark:text-slate-400 mt-1">
                        Readiness: {selectedEntry.documentReadiness?.percentage || 0}%
                      </span>
                    </div>
                  </div>

                  {/* Manage Application Button */}
                  <div className="mt-4 pt-4 border-t border-gray-100 dark:border-slate-800 flex items-center justify-between">
                    <Link
                      to={`/applications/${selectedEntry.id}`}
                      className="inline-flex items-center gap-1.5 px-4 py-2 bg-government-blue hover:bg-government-blue-dark dark:bg-indigo-600 dark:hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition shadow-xs"
                    >
                      <span>Manage Documents & Actions</span>
                      <ArrowRight className="h-3.5 w-3.5" />
                    </Link>

                    <button
                      onClick={() => setShowGrievance(true)}
                      className="text-xs font-semibold text-gray-500 dark:text-slate-400 hover:text-government-blue dark:hover:text-indigo-400 transition flex items-center gap-1"
                    >
                      <MessageSquareWarning className="h-3.5 w-3.5" />
                      Lodge Grievance
                    </button>
                  </div>
                </div>

                {/* Real Event Timeline Card */}
                <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm">
                  <div className="flex items-center justify-between pb-3 border-b border-gray-100 dark:border-slate-800 mb-5">
                    <h3 className="text-sm font-bold text-gray-800 dark:text-slate-100 uppercase tracking-widest flex items-center gap-1.5">
                      <CalendarDays className="h-4 w-4 text-government-blue dark:text-indigo-400" />
                      Audit Event Timeline
                    </h3>
                    <span className="text-[10px] text-gray-400 dark:text-slate-500 font-medium">Authoritative Immutable Audit Log</span>
                  </div>

                  {timelineLoading ? (
                    <div className="space-y-3">
                      {[1, 2, 3].map((i) => (
                        <div key={i} className="h-10 bg-gray-100 dark:bg-slate-800 rounded-xl animate-pulse" />
                      ))}
                    </div>
                  ) : timelineEvents.length > 0 ? (
                    <div className="relative pl-6 border-l-2 border-government-blue/20 dark:border-indigo-500/30 space-y-6">
                      {timelineEvents.map((evt, idx) => (
                        <div key={idx} className="relative">
                          <div className="absolute -left-[31px] top-1 h-3.5 w-3.5 rounded-full bg-government-blue dark:bg-indigo-500 border-2 border-white dark:border-slate-900 shadow-xs" />
                          <div>
                            <div className="flex items-center justify-between">
                              <span className="text-xs font-bold text-gray-900 dark:text-slate-100 uppercase tracking-wider">
                                {evt.eventType?.replace(/_/g, " ")}
                              </span>
                              <span className="text-[10px] text-gray-400 dark:text-slate-500">
                                {evt.createdAt ? new Date(evt.createdAt).toLocaleString("en-IN") : "Recent"}
                              </span>
                            </div>
                            <p className="text-xs text-gray-600 dark:text-slate-300 mt-1 leading-relaxed">
                              {evt.message}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="relative pl-6 border-l-2 border-gray-100 dark:border-slate-800 space-y-4">
                      {TIMELINE_STAGES.map((st, idx) => (
                        <div key={idx} className="relative">
                          <div className={`absolute -left-[31px] top-1 h-3.5 w-3.5 rounded-full border-2 border-white dark:border-slate-900 shadow-xs ${idx <= selectedEntry.stageIdx ? "bg-government-blue dark:bg-indigo-500" : "bg-gray-300 dark:bg-slate-700"}`} />
                          <span className="text-xs font-bold text-gray-800 dark:text-slate-200">{st.label}</span>
                          <p className="text-[11px] text-gray-500 dark:text-slate-400">{st.description}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Documents Checklist Card */}
                {selectedEntry.documents && selectedEntry.documents.length > 0 && (
                  <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm space-y-3">
                    <h3 className="text-xs font-bold text-gray-800 dark:text-slate-100 uppercase tracking-widest flex items-center gap-1.5">
                      <FileText className="h-4 w-4 text-government-blue dark:text-indigo-400" />
                      Required Verification Documents
                    </h3>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {selectedEntry.documents.map((doc, idx) => (
                        <div key={idx} className="p-3 bg-gray-50 dark:bg-slate-800/60 border border-gray-200 dark:border-slate-700 rounded-xl flex items-center justify-between text-xs">
                          <div>
                            <span className="font-semibold text-gray-800 dark:text-slate-200 block">{doc.documentName || doc.documentCode}</span>
                            <span className="text-[10px] text-gray-400 dark:text-slate-500">{doc.mandatory ? "Mandatory" : "Optional"}</span>
                          </div>

                          <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold uppercase ${
                            doc.verificationStatus === "VERIFIED"
                              ? "bg-india-green/10 text-india-green dark:text-emerald-400"
                              : doc.verificationStatus === "REJECTED"
                              ? "bg-red-50 dark:bg-red-950/50 text-red-700 dark:text-red-400"
                              : doc.uploaded
                              ? "bg-blue-50 dark:bg-blue-950/50 text-blue-700 dark:text-blue-400"
                              : "bg-gray-200 dark:bg-slate-700 text-gray-600 dark:text-slate-300"
                          }`}>
                            {doc.verificationStatus || (doc.uploaded ? "Uploaded" : "Pending")}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ) : null}
          </div>
        </div>
      )}

      {/* Grievance Modal */}
      {showGrievance && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50 backdrop-blur-xs p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 max-w-md w-full shadow-2xl border border-gray-200 dark:border-slate-800 space-y-4">
            <h3 className="text-base font-bold text-gray-900 dark:text-slate-100 flex items-center gap-2">
              <MessageSquareWarning className="h-5 w-5 text-government-blue dark:text-indigo-400" />
              Lodge Scheme Grievance
            </h3>
            <p className="text-xs text-gray-500 dark:text-slate-400">
              Submit an official inquiry regarding {selectedEntry?.schemeName}.
            </p>

            {grievanceSuccess ? (
              <div className="p-4 bg-green-50 dark:bg-emerald-950/40 text-green-700 dark:text-emerald-300 border border-green-200 dark:border-emerald-800 rounded-xl text-xs font-semibold text-center">
                ✓ Grievance lodged successfully. Tracking reference generated.
              </div>
            ) : (
              <form onSubmit={handleGrievanceSubmit} className="space-y-3">
                <div>
                  <label className="text-[11px] font-bold text-gray-600 dark:text-slate-300 block mb-1">Issue Category</label>
                  <select
                    value={grievanceData.category}
                    onChange={(e) => setGrievanceData({ ...grievanceData, category: e.target.value })}
                    className="w-full text-xs p-2.5 border border-gray-300 dark:border-slate-700 rounded-xl bg-gray-50 dark:bg-slate-800 text-gray-800 dark:text-slate-200 focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400"
                  >
                    <option>Delay in Processing</option>
                    <option>Document Verification Issue</option>
                    <option>Rejection Clarification</option>
                    <option>Other Grievance</option>
                  </select>
                </div>

                <div>
                  <label className="text-[11px] font-bold text-gray-600 dark:text-slate-300 block mb-1">Description</label>
                  <textarea
                    rows={4}
                    value={grievanceData.description}
                    onChange={(e) => setGrievanceData({ ...grievanceData, description: e.target.value })}
                    placeholder="Provide specific details about your query..."
                    className="w-full text-xs p-2.5 border border-gray-300 dark:border-slate-700 rounded-xl bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500 focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400"
                    required
                  />
                </div>

                <div className="flex gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowGrievance(false)}
                    className="flex-1 py-2 border border-gray-300 dark:border-slate-700 rounded-xl text-xs font-semibold text-gray-600 dark:text-slate-300 hover:bg-gray-50 dark:hover:bg-slate-800 transition"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="flex-1 py-2 bg-government-blue hover:bg-government-blue-dark dark:bg-indigo-600 dark:hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition"
                  >
                    Submit Grievance
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* AI Assistant */}
      <SchemeAIChatWidget
        isOpen={aiChatOpen}
        initialQuery={aiInitialQuery}
        pageContext={window.location.pathname}
        onClose={() => {
          setAiChatOpen(false);
          setAiInitialQuery("");
        }}
      />
    </div>
  );
}
