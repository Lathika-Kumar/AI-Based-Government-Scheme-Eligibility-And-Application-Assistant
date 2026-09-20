import React, { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { usePageMeta } from "@utils/usePageMeta";
import { useApp } from "@context/AppContext";
import adminService from "@services/adminService";
import { useToast } from "@components/ui/ToastNotification";
import {
  ClipboardList, RefreshCw, AlertCircle, Search, CheckCircle2,
  XCircle, Clock, ArrowRight, ChevronLeft, ChevronRight, FileText,
  ShieldCheck, ShieldAlert, Check, X, Eye, AlertTriangle, Sparkles,
  Download, FileCheck, Layers
} from "lucide-react";

const STATUS_CONFIG = {
  DOCUMENTS_PENDING:     { label: "Documents Pending",     color: "bg-amber-100 text-amber-800 border-amber-300" },
  READY_FOR_SUBMISSION:  { label: "Ready to Submit",       color: "bg-blue-100 text-blue-800 border-blue-300" },
  SUBMITTED:             { label: "Submitted",             color: "bg-indigo-100 text-indigo-700 border-indigo-300" },
  UNDER_REVIEW:          { label: "Under Review",          color: "bg-purple-100 text-purple-700 border-purple-300" },
  DOCUMENT_VERIFICATION: { label: "Doc Verification",      color: "bg-purple-100 text-purple-700 border-purple-300" },
  CORRECTION_REQUIRED:   { label: "Correction Required",   color: "bg-red-100 text-red-700 border-red-300" },
  APPROVED:              { label: "Approved",              color: "bg-green-100 text-green-700 border-green-300" },
  REJECTED:              { label: "Rejected",              color: "bg-red-100 text-red-700 border-red-300" },
};

const DETAILED_DOC_STATUS_CONFIG = {
  // ── Pre-upload ──────────────────────────────────────────────────────────
  NOT_UPLOADED:               { label: "Not Uploaded",             color: "bg-gray-100 text-gray-700 border-gray-300" },
  UPLOADING:                  { label: "Uploading…",               color: "bg-sky-100 text-sky-700 border-sky-300" },
  UPLOADED:                   { label: "Uploaded",                 color: "bg-blue-100 text-blue-700 border-blue-300" },
  // ── AI pipeline ─────────────────────────────────────────────────────────
  PROCESSING:                 { label: "Processing",               color: "bg-amber-100 text-amber-700 border-amber-300" },
  OCR_PROCESSING:             { label: "OCR Processing",           color: "bg-amber-100 text-amber-700 border-amber-300" },
  OCR_COMPLETED:              { label: "OCR Completed",            color: "bg-indigo-100 text-indigo-700 border-indigo-300" },
  AI_VERIFIED:                { label: "AI Verified",              color: "bg-teal-100 text-teal-700 border-teal-300" },
  AI_PASSED:                  { label: "AI Verified",              color: "bg-teal-100 text-teal-700 border-teal-300" },
  AI_REVIEW_REQUIRED:         { label: "AI — Needs Review",        color: "bg-yellow-100 text-yellow-700 border-yellow-300" },
  AI_FLAGGED:                 { label: "AI — Flagged",             color: "bg-yellow-100 text-yellow-700 border-yellow-300" },
  AI_REJECTED:                { label: "AI Rejected",              color: "bg-red-100 text-red-700 border-red-300" },
  AI_FAILED:                  { label: "AI Failed",                color: "bg-red-100 text-red-700 border-red-300" },
  // ── Officer queue ────────────────────────────────────────────────────────
  PENDING_ADMIN_VERIFICATION: { label: "Pending Officer Review",   color: "bg-purple-100 text-purple-700 border-purple-300" },
  UNDER_OFFICER_REVIEW:       { label: "Under Officer Review",     color: "bg-violet-100 text-violet-700 border-violet-300" },
  // ── Terminal ─────────────────────────────────────────────────────────────
  ADMIN_VERIFIED:             { label: "Officer Verified ✓",       color: "bg-emerald-100 text-emerald-700 border-emerald-300" },
  VERIFIED:                   { label: "Verified ✓",               color: "bg-emerald-100 text-emerald-700 border-emerald-300" },
  REJECTED:                   { label: "Rejected",                 color: "bg-red-100 text-red-700 border-red-300" },
  CORRECTION_REQUIRED:        { label: "Correction Required",      color: "bg-orange-100 text-orange-700 border-orange-300" },
  REUPLOAD_REQUIRED:          { label: "Re-upload Required",       color: "bg-orange-100 text-orange-700 border-orange-300" },
};

function fmtDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

export default function AdminApplicationReview() {
  usePageMeta("Application Review", "Administrative review queue for scheme applications");
  const showToast = useToast();
  const { refreshApplications } = useApp();
  const { applicationId: routeAppId } = useParams();
  const navigate = useNavigate();

  const [applications, setApplications] = useState([]);
  const [pagination, setPagination]     = useState({ page: 0, totalPages: 1, totalElements: 0 });
  const [loading, setLoading]           = useState(false);
  const [error, setError]               = useState(null);

  const [statusFilter, setStatusFilter] = useState("");
  const [schemeCodeFilter, setSchemeCodeFilter] = useState("");
  const [page, setPage] = useState(0);

  const [selectedApp, setSelectedApp]   = useState(null);
  const [appDetails, setAppDetails]     = useState(null);
  const [loadingDetails, setLoadingDetails] = useState(false);

  const [actionLoading, setActionLoading] = useState({});
  const [remarks, setRemarks]           = useState("");
  const [docRejectReason, setDocRejectReason] = useState("");
  const [rejectDocCode, setRejectDocCode] = useState(null);
  const [docCorrectionReason, setDocCorrectionReason] = useState("");
  const [correctionDocCode, setCorrectionDocCode] = useState(null);

  // OCR state for document inspection
  const [ocrData, setOcrData]           = useState({});
  const [loadingOcr, setLoadingOcr]     = useState({});
  const [expandedOcr, setExpandedOcr]   = useState({});

  const closeModal = useCallback(() => {
    setSelectedApp(null);
    setAppDetails(null);
    setRemarks("");
    setDocRejectReason("");
    setRejectDocCode(null);
    setDocCorrectionReason("");
    setCorrectionDocCode(null);
    if (routeAppId) {
      navigate("/admin/review");
    }
  }, [routeAppId, navigate]);

  useEffect(() => {
    if (!routeAppId) return;
    let cancelled = false;
    async function loadExactApplication() {
      setLoadingDetails(true);
      const res = await adminService.getAdminApplicationById(routeAppId);
      if (cancelled) return;
      if (res.data) {
        setSelectedApp(res.data);
        setAppDetails(res.data);
      } else {
        showToast(res.message || "Unable to locate application.", "error");
      }
      setLoadingDetails(false);
    }
    loadExactApplication();
    return () => { cancelled = true; };
  }, [routeAppId, showToast]);

  const handleViewDocument = async (appId, docCode) => {
    try {
      showToast("Fetching document preview...", "info");
      const res = await adminService.downloadAdminDocument(appId, docCode);
      if (res.error || !res.data) {
        showToast(res.message || "Failed to download document.", "error");
        return;
      }
      const contentType = res.headers?.get?.("content-type") || "application/pdf";
      const blob = res.data instanceof Blob ? res.data : new Blob([res.data], { type: contentType });
      const objectUrl = window.URL.createObjectURL(blob);
      window.open(objectUrl, "_blank", "noopener,noreferrer");
      setTimeout(() => window.URL.revokeObjectURL(objectUrl), 60000);
    } catch (err) {
      console.error("View document error:", err);
      showToast("Unable to open document file.", "error");
    }
  };

  const fetchQueue = useCallback(async () => {
    setLoading(true);
    setError(null);
    const result = await adminService.getApplicationsQueue({
      status: statusFilter || undefined,
      schemeCode: schemeCodeFilter || undefined,
      page, size: 15,
    });
    if (result.error) {
      setError(result.message || "Unable to load review queue.");
    } else {
      const pageData = result.data;
      setApplications(pageData.content ?? []);
      setPagination({
        page: pageData.page ?? 0,
        totalPages: pageData.totalPages ?? 1,
        totalElements: pageData.totalElements ?? 0,
      });
    }
    setLoading(false);
  }, [statusFilter, schemeCodeFilter, page]);

  useEffect(() => { fetchQueue(); }, [fetchQueue]);

  const openApp = async (app) => {
    setSelectedApp(app);
    setLoadingDetails(true);
    setAppDetails(app);
    setOcrData({});
    setExpandedOcr({});
    setLoadingDetails(false);
  };

  const toggleInspectOcr = async (appId, docCode) => {
    if (expandedOcr[docCode]) {
      setExpandedOcr((prev) => ({ ...prev, [docCode]: false }));
      return;
    }
    setExpandedOcr((prev) => ({ ...prev, [docCode]: true }));

    if (!ocrData[docCode]) {
      setLoadingOcr((prev) => ({ ...prev, [docCode]: true }));
      try {
        const res = await adminService.getDocumentOcr(appId, docCode);
        if (res?.data) {
          setOcrData((prev) => ({ ...prev, [docCode]: res.data }));
        } else if (res?.error) {
          showToast(res.message || "Unable to retrieve OCR extraction.", "error");
        }
      } catch (e) {
        showToast("Error processing OCR: " + e.message, "error");
      } finally {
        setLoadingOcr((prev) => ({ ...prev, [docCode]: false }));
      }
    }
  };

  const doAction = async (action, ...args) => {
    setActionLoading((prev) => ({ ...prev, [action]: true }));
    let result;
    try {
      switch (action) {
        case "start":   result = await adminService.startReview(selectedApp.id); break;
        case "approve": result = await adminService.approveApplication(selectedApp.id, remarks || "Approved"); break;
        case "reject":  result = await adminService.rejectApplication(selectedApp.id, remarks); break;
        case `verify_${args[0]}`:  result = await adminService.verifyDocument(selectedApp.id, args[0]); break;
        case `reject_doc_${args[0]}`: result = await adminService.rejectDocument(selectedApp.id, args[0], docRejectReason); break;
        case `correct_doc_${args[0]}`: result = await adminService.requestDocumentCorrectionByCode(selectedApp.id, args[0], docCorrectionReason); break;
        default: result = { error: true, message: "Unknown action" };
      }
      if (result.error) {
        showToast(result.message || `Action ${action} failed.`, "error");
      } else {
        showToast(`Action completed successfully.`, "success");
        if (action === "approve" || action === "reject") {
          closeModal();
        } else if (result.data) {
          setSelectedApp(result.data);
          setAppDetails(result.data);
        }
        fetchQueue();
        if (typeof refreshApplications === "function") {
          refreshApplications().catch(() => {});
        }
      }
    } finally {
      setActionLoading((prev) => ({ ...prev, [action]: false }));
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-r from-government-blue via-government-blue-light to-government-blue text-white p-6 rounded-xl shadow-lg">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <ClipboardList className="h-5 w-5 text-saffron" />
              <span className="text-xs font-semibold text-white/80 uppercase tracking-wider">Admin Portal</span>
            </div>
            <h1 className="text-2xl font-bold">Application Review Queue</h1>
            <p className="text-sm text-white/90 mt-1">
              {pagination.totalElements} applications · {statusFilter || "All statuses"}
            </p>
          </div>
          <button onClick={fetchQueue} disabled={loading}
            className="flex items-center gap-2 bg-white/10 hover:bg-white/20 text-white text-sm font-semibold px-4 py-2 rounded-lg border border-white/20 transition">
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          className="text-sm border border-slate-200 dark:border-slate-700 rounded-lg px-3 py-2.5 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue">
          <option value="">All Statuses</option>
          {Object.entries(STATUS_CONFIG).map(([k, v]) => (
            <option key={k} value={k}>{v.label}</option>
          ))}
        </select>
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input type="text" value={schemeCodeFilter} onChange={(e) => { setSchemeCodeFilter(e.target.value); setPage(0); }}
            placeholder="Filter by scheme code..."
            className="w-full pl-9 pr-4 py-2.5 text-sm border border-slate-200 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue" />
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-3 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900/50 text-red-700 dark:text-red-300 p-4 rounded-xl text-sm">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <p>{error}</p>
          <button onClick={fetchQueue} className="ml-auto text-xs font-bold hover:underline">Retry</button>
        </div>
      )}

      {loading && (
        <div className="space-y-3">
          {[1,2,3].map(i => <div key={i} className="bg-slate-100 dark:bg-slate-800 rounded-xl animate-pulse h-24" />)}
        </div>
      )}

      {/* Application list */}
      {!loading && (
        <div className="space-y-3">
          {applications.map((app) => {
            const cfg = STATUS_CONFIG[app.status] || {};
            return (
              <div key={app.id} className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow-sm flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap mb-1">
                    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${cfg.color}`}>
                      {cfg.label || app.status}
                    </span>
                    <span className="text-xs text-slate-400 dark:text-slate-500 font-mono">{app.applicationNumber}</span>
                  </div>
                  <p className="text-sm font-bold text-slate-900 dark:text-slate-100 truncate">
                    {(typeof app.schemeTitle === "string" ? app.schemeTitle : (app.schemeTitle?.english || app.schemeTitle?.en || app.schemeName)) || app.schemeCode}
                  </p>
                  <p className="text-xs text-slate-600 dark:text-slate-400">{app.schemeCode} · User: {app.userId}</p>
                  <p className="text-xs text-slate-400 dark:text-slate-500 mt-0.5">Created: {fmtDate(app.createdAt)}</p>
                </div>
                <button onClick={() => openApp(app)}
                  className="flex items-center gap-1.5 bg-government-blue hover:bg-government-blue-dark text-white px-4 py-2 rounded-lg text-xs font-bold transition">
                  <Eye className="h-3.5 w-3.5" /> Review
                </button>
              </div>
            );
          })}
          {applications.length === 0 && !loading && !routeAppId && (
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-12 text-center text-slate-400">
              <ClipboardList className="h-10 w-10 mx-auto mb-3 opacity-30" />
              <p className="font-semibold text-slate-600 dark:text-slate-400">No applications in queue</p>
            </div>
          )}
        </div>
      )}

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            className="flex items-center gap-1 px-4 py-2 text-sm font-semibold border border-slate-200 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-750 disabled:opacity-40 disabled:cursor-not-allowed transition">
            <ChevronLeft className="h-4 w-4" /> Previous
          </button>
          <span className="text-sm text-slate-600 dark:text-slate-400">Page {page + 1} of {pagination.totalPages}</span>
          <button onClick={() => setPage(p => Math.min(pagination.totalPages - 1, p + 1))} disabled={page >= pagination.totalPages - 1}
            className="flex items-center gap-1 px-4 py-2 text-sm font-semibold border border-slate-200 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-750 disabled:opacity-40 disabled:cursor-not-allowed transition">
            Next <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Review Modal */}
      {selectedApp && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-6 py-4 flex items-center justify-between z-10">
              <div>
                <p className="font-bold text-slate-900 dark:text-slate-100 text-sm">{selectedApp.applicationNumber}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400">{selectedApp.schemeCode} · {STATUS_CONFIG[selectedApp.status]?.label || selectedApp.status}</p>
              </div>
              <button onClick={closeModal}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="p-6 space-y-5">
              {/* Start Review */}
              {["SUBMITTED", "DOCUMENTS_PENDING", "CORRECTION_REQUIRED"].includes(selectedApp.status) && (
                <button onClick={() => doAction("start")} disabled={actionLoading["start"]}
                  className="w-full flex items-center justify-center gap-2 bg-purple-600 hover:bg-purple-700 disabled:opacity-60 text-white py-3 rounded-xl text-sm font-bold transition">
                  {actionLoading["start"] ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Eye className="h-4 w-4" />}
                  Start Review ({selectedApp.status} → UNDER_REVIEW)
                </button>
              )}

              {/* Document verification */}
              {["UNDER_REVIEW","DOCUMENT_VERIFICATION","CORRECTION_REQUIRED"].includes(selectedApp.status) && selectedApp.documents?.length > 0 && (
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-slate-100 text-sm mb-3 flex items-center gap-2">
                    <FileText className="h-4 w-4 text-government-blue" /> Document Verification & OCR Intelligence
                  </h3>
                  <div className="space-y-3">
                    {selectedApp.documents.map((doc) => {
                      const ocr = ocrData[doc.documentCode];
                      const isOcrOpen = expandedOcr[doc.documentCode];
                      const isOcrLoading = loadingOcr[doc.documentCode];
                      // Prefer the fine-grained detailedStatus from the backend (mapped to `status` field)
                      // then fall back to coarse verificationStatus, then upload state.
                      const rawStatus = doc.status || doc.detailedStatus || null;
                      const statusKey = rawStatus
                        ? String(rawStatus).toUpperCase()
                        : (doc.uploaded
                          ? (doc.verificationStatus === "VERIFIED" ? "ADMIN_VERIFIED"
                            : doc.verificationStatus === "REJECTED" ? "REJECTED"
                            : "PENDING_ADMIN_VERIFICATION")
                          : "NOT_UPLOADED");
                      const docStatusCfg = DETAILED_DOC_STATUS_CONFIG[statusKey] || { label: statusKey, color: "bg-gray-100 text-gray-700 border-gray-300" };

                      return (
                        <div key={doc.documentCode} className="border border-slate-200 dark:border-slate-800 rounded-xl p-4 bg-slate-50/50 dark:bg-slate-850/50">
                          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                            <div>
                              <div className="flex items-center gap-2 flex-wrap">
                                <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">{doc.documentName || doc.documentCode}</p>
                                {doc.mandatory && (
                                  <span className="text-[10px] uppercase font-bold bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-800 px-2 py-0.5 rounded">
                                    Mandatory
                                  </span>
                                )}
                                <span className="text-[10px] font-mono font-bold bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200 px-1.5 py-0.5 rounded">
                                  v{doc.version || 1}
                                </span>
                                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${docStatusCfg.color}`}>
                                  {docStatusCfg.label}
                                </span>
                              </div>
                              <p className="text-xs text-slate-400 font-mono mt-0.5">{doc.documentCode}</p>

                              {/* ONE_OF Alternative Group indicator */}
                              {doc.alternativeGroupType === "ONE_OF" && doc.alternatives && doc.alternatives.length > 0 && (
                                <div className="mt-1.5 pl-2.5 py-1 bg-blue-50 dark:bg-blue-950/40 border-l-2 border-government-blue rounded-r text-xs text-slate-700 dark:text-slate-300">
                                  <span className="font-bold text-slate-700 dark:text-slate-300 uppercase text-[10px] block">Satisfied by any ONE of:</span>
                                  <span className="text-slate-600 dark:text-slate-400 font-medium">{doc.alternatives.join(" · ")}</span>
                                </div>
                              )}

                              {doc.uploaded && doc.fileName && (
                                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">File: {doc.fileName} (v{doc.version || 1})</p>
                              )}
                              {doc.rejectionReason && (
                                <p className="text-xs text-red-600 dark:text-red-400 mt-1 font-medium">Rejection Reason: {doc.rejectionReason}</p>
                              )}
                            </div>

                            <div className="flex items-center gap-2 flex-wrap shrink-0">
                              {/* Authenticated View Document */}
                              {doc.uploaded && (
                                <button
                                  type="button"
                                  onClick={() => handleViewDocument(selectedApp.id, doc.documentCode)}
                                  className="flex items-center gap-1 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 px-3 py-1.5 rounded-lg text-xs font-semibold border border-slate-300 dark:border-slate-700 transition"
                                >
                                  <Eye className="h-3 w-3" /> View File
                                </button>
                              )}

                              {/* Inspect OCR Trigger */}
                              {doc.uploaded && (
                                <button
                                  type="button"
                                  onClick={() => toggleInspectOcr(selectedApp.id, doc.documentCode)}
                                  className="flex items-center gap-1 bg-indigo-50 dark:bg-indigo-950/40 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 px-3 py-1.5 rounded-lg text-xs font-semibold border border-indigo-200 dark:border-indigo-800/60 transition"
                                >
                                  {isOcrLoading ? (
                                    <RefreshCw className="h-3 w-3 animate-spin" />
                                  ) : (
                                    <Sparkles className="h-3 w-3 text-indigo-600 dark:text-indigo-400" />
                                  )}
                                  {isOcrOpen ? "Hide OCR" : "Inspect OCR"}
                                </button>
                              )}

                              {/* Verify/Reject/Correction buttons: visible only while document is NOT yet officer-verified/rejected */}
                              {(() => {
                                const st = String(doc.status || doc.detailedStatus || "").toUpperCase();
                                const isTerminal = ["ADMIN_VERIFIED", "VERIFIED", "REJECTED"].includes(st) ||
                                  doc.verificationStatus === "VERIFIED";
                                return doc.uploaded && !isTerminal;
                              })() && (
                                <>
                                  <button onClick={() => doAction(`verify_${doc.documentCode}`, doc.documentCode)}
                                    disabled={actionLoading[`verify_${doc.documentCode}`]}
                                    className="flex items-center gap-1 bg-green-600 hover:bg-green-700 text-white px-3 py-1.5 rounded-lg text-xs font-semibold transition disabled:opacity-50">
                                    <Check className="h-3 w-3" /> Verify
                                  </button>
                                  <button onClick={() => { setCorrectionDocCode(doc.documentCode); setRejectDocCode(null); }}
                                    className="flex items-center gap-1 bg-amber-100 dark:bg-amber-950/40 hover:bg-amber-200 dark:hover:bg-amber-900/50 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800 px-3 py-1.5 rounded-lg text-xs font-semibold transition">
                                    <AlertTriangle className="h-3 w-3" /> Request Correction
                                  </button>
                                  <button onClick={() => { setRejectDocCode(doc.documentCode); setCorrectionDocCode(null); }}
                                    className="flex items-center gap-1 bg-red-100 dark:bg-red-950/40 hover:bg-red-200 dark:hover:bg-red-900/50 text-red-700 dark:text-red-300 border border-red-200 dark:border-red-800 px-3 py-1.5 rounded-lg text-xs font-semibold transition">
                                    <X className="h-3 w-3" /> Reject
                                  </button>
                                </>
                              )}
                              {(["ADMIN_VERIFIED", "VERIFIED"].includes(String(doc.status || doc.detailedStatus || "").toUpperCase()) ||
                                doc.verificationStatus === "VERIFIED") && (
                                <span className="text-emerald-600 dark:text-emerald-400 text-xs font-bold flex items-center gap-1">
                                  <CheckCircle2 className="h-4 w-4" /> Officer Verified
                                </span>
                              )}
                            </div>
                          </div>

                          {/* Correction Form Drawer */}
                          {correctionDocCode === doc.documentCode && (
                            <div className="mt-3 space-y-2 bg-white dark:bg-slate-800 p-3 rounded-lg border border-amber-200 dark:border-amber-800">
                              <input type="text" value={docCorrectionReason} onChange={(e) => setDocCorrectionReason(e.target.value)}
                                placeholder="Specific correction required (e.g., re-upload clearer scan)"
                                className="w-full px-3 py-2 text-sm border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-800 dark:text-slate-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-400" />
                              <div className="flex gap-2">
                                <button onClick={() => doAction(`correct_doc_${doc.documentCode}`, doc.documentCode)}
                                  disabled={!docCorrectionReason.trim() || actionLoading[`correct_doc_${doc.documentCode}`]}
                                  className="flex-1 bg-amber-600 hover:bg-amber-700 disabled:opacity-50 text-white py-2 rounded-lg text-xs font-bold transition">
                                  Confirm Correction Request
                                </button>
                                <button onClick={() => setCorrectionDocCode(null)} className="flex-1 border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 py-2 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-300">
                                  Cancel
                                </button>
                              </div>
                            </div>
                          )}

                          {/* Rejection Form Drawer */}
                          {rejectDocCode === doc.documentCode && (
                            <div className="mt-3 space-y-2 bg-white dark:bg-slate-800 p-3 rounded-lg border border-red-200 dark:border-red-800">
                              <input type="text" value={docRejectReason} onChange={(e) => setDocRejectReason(e.target.value)}
                                placeholder="Reason for rejection (required)"
                                className="w-full px-3 py-2 text-sm border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-800 dark:text-slate-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-400" />
                              <div className="flex gap-2">
                                <button onClick={() => doAction(`reject_doc_${doc.documentCode}`, doc.documentCode)}
                                  disabled={!docRejectReason.trim() || actionLoading[`reject_doc_${doc.documentCode}`]}
                                  className="flex-1 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white py-2 rounded-lg text-xs font-bold transition">
                                  Confirm Reject
                                </button>
                                <button onClick={() => setRejectDocCode(null)} className="flex-1 border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 py-2 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-300">
                                  Cancel
                                </button>
                              </div>
                            </div>
                          )}

                          {/* OCR Extraction Details Panel */}
                          {isOcrOpen && ocr && (
                            <div className="mt-3 bg-white dark:bg-slate-800 p-4 rounded-xl border border-indigo-100 dark:border-indigo-900/50 shadow-sm space-y-3">
                              <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-700 pb-2">
                                <div className="flex items-center gap-2">
                                  <Layers className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
                                  <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
                                    Detected: {ocr.detectedDocumentType || "Document"}
                                  </span>
                                  <span className="text-[10px] text-slate-500 dark:text-slate-400">({ocr.provider})</span>
                                </div>
                                <div className="flex items-center gap-1.5">
                                  <span className="text-xs font-semibold text-slate-600 dark:text-slate-300">Confidence:</span>
                                  <span className="text-xs font-bold px-2 py-0.5 rounded bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300">
                                    {Math.round((ocr.overallConfidence || 0) * 100)}%
                                  </span>
                                </div>
                              </div>

                              {/* Extracted Fields Table */}
                              {ocr.extractedFields && Object.keys(ocr.extractedFields).length > 0 ? (
                                <div className="overflow-x-auto">
                                  <table className="w-full text-left text-xs border-collapse">
                                    <thead>
                                      <tr className="border-b border-slate-100 dark:border-slate-700 text-slate-400">
                                        <th className="py-1.5 font-medium">Field</th>
                                        <th className="py-1.5 font-medium">Extracted Value</th>
                                        <th className="py-1.5 font-medium">Confidence</th>
                                        <th className="py-1.5 font-medium">Source</th>
                                      </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-50 dark:divide-slate-700/50 text-slate-700 dark:text-slate-300">
                                      {Object.entries(ocr.extractedFields).map(([key, field]) => (
                                        <tr key={key}>
                                          <td className="py-1.5 font-semibold text-slate-900 dark:text-slate-100">{key}</td>
                                          <td className="py-1.5 text-slate-800 dark:text-slate-200 font-mono">
                                            {typeof field.value === "number" && key.toLowerCase().includes("income")
                                              ? `₹${field.value.toLocaleString("en-IN")}`
                                              : String(field.value)}
                                          </td>
                                          <td className="py-1.5">
                                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-green-50 dark:bg-green-950/60 text-green-700 dark:text-green-300">
                                              {Math.round((field.confidence || 0) * 100)}%
                                            </span>
                                          </td>
                                          <td className="py-1.5 text-[10px] text-slate-500 dark:text-slate-400 font-mono">{field.source}</td>
                                        </tr>
                                      ))}
                                    </tbody>
                                  </table>
                                </div>
                              ) : (
                                <p className="text-xs text-slate-400 italic">No structured fields extracted from document stream.</p>
                              )}

                              {/* Raw Text Preview */}
                              {ocr.rawText && (
                                <div className="mt-2 text-[11px] text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-900 p-2.5 rounded-lg max-h-24 overflow-y-auto font-mono whitespace-pre-wrap">
                                  {ocr.rawText}
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Approve / Reject Application: visible for all approval-capable applications */}
              {!["APPROVED","REJECTED","CANCELLED"].includes(selectedApp.status) && (() => {
                const officerVerifiedStatuses = ["ADMIN_VERIFIED", "VERIFIED"];
                const mandatoryDocuments = (selectedApp?.documents || []).filter((d) => d.mandatory !== false);
                const unverifiedMandatory = mandatoryDocuments.filter((d) => {
                  const st = String(d.verificationStatus || d.status || d.detailedStatus || (d.verified ? "VERIFIED" : "")).toUpperCase();
                  return !officerVerifiedStatuses.includes(st);
                });
                const allMandatoryOfficerVerified =
                  mandatoryDocuments.length > 0 &&
                  mandatoryDocuments.every((doc) =>
                    officerVerifiedStatuses.includes(
                      String(doc.verificationStatus || doc.status || doc.detailedStatus || (doc.verified ? "VERIFIED" : "")).toUpperCase()
                    )
                  );
                const canApprove = allMandatoryOfficerVerified;

                return (
                  <div className="border-t border-slate-100 dark:border-slate-800 pt-5 space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="font-bold text-slate-900 dark:text-slate-100 text-sm">Final Decision</h3>
                      <span className={`text-[10px] font-bold px-2.5 py-0.5 rounded-full border uppercase tracking-wider ${
                        canApprove
                          ? "bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800"
                          : "bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border-amber-200 dark:border-amber-800"
                      }`}>
                        {canApprove ? "Ready for Approval" : "Officer Verification Pending"}
                      </span>
                    </div>

                    {!canApprove && (
                      <div className="p-3.5 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/80 rounded-xl flex items-start gap-2.5 text-xs text-amber-900 dark:text-amber-200 shadow-xs">
                        <AlertTriangle className="h-4 w-4 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
                        <div className="space-y-1">
                          <p className="font-bold">Application Approval Blocked</p>
                          <p className="text-[11px] text-amber-800 dark:text-amber-300 leading-relaxed font-medium">
                            Application cannot be approved because one or more required documents have not completed officer verification.
                          </p>
                          {unverifiedMandatory.length > 0 && (
                            <p className="text-[10px] text-amber-700 dark:text-amber-400 font-semibold">
                              Pending verification: {unverifiedMandatory.map(d => d.documentName || d.documentCode).join(", ")}
                            </p>
                          )}
                        </div>
                      </div>
                    )}

                    <div className="space-y-1">
                      <label className="block text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                        Decision Remarks
                      </label>
                      <textarea
                        value={remarks}
                        onChange={(e) => setRemarks(e.target.value)}
                        placeholder="Add decision remarks (required for rejection)..."
                        rows={2}
                        className="w-full px-3.5 py-2.5 text-sm border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-600 transition resize-none font-medium"
                      />
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
                      <button
                        type="button"
                        onClick={() => doAction("approve")}
                        disabled={actionLoading["approve"] || !canApprove}
                        title={!canApprove ? "Application cannot be approved because one or more required documents have not completed officer verification." : "Approve Application"}
                        className={`inline-flex items-center justify-center gap-2 min-h-[48px] w-full rounded-xl border px-5 py-3 font-bold transition select-none ${
                          canApprove && !actionLoading["approve"]
                            ? "border-emerald-700 bg-emerald-600 hover:bg-emerald-700 text-white shadow-md focus:outline-none focus:ring-2 focus:ring-emerald-500 cursor-pointer"
                            : "border-slate-300 dark:border-slate-700 bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 opacity-100 cursor-not-allowed"
                        }`}
                        style={
                          canApprove && !actionLoading["approve"]
                            ? { backgroundColor: "#059669", color: "#ffffff", borderColor: "#047857" }
                            : undefined
                        }
                      >
                        {actionLoading["approve"] ? (
                          <RefreshCw className="h-5 w-5 animate-spin text-white shrink-0" />
                        ) : (
                          <CheckCircle2 className={`h-5 w-5 shrink-0 ${canApprove ? "text-white" : "text-slate-600 dark:text-slate-400"}`} />
                        )}
                        <span className="text-sm font-bold">{actionLoading["approve"] ? "Processing..." : "✓ Approve Application"}</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => doAction("reject")}
                        disabled={!remarks.trim() || actionLoading["reject"]}
                        title={!remarks.trim() ? "Decision remarks are required to reject an application." : "Reject Application"}
                        className={`inline-flex items-center justify-center gap-2 min-h-[48px] w-full rounded-xl border px-5 py-3 font-bold transition select-none ${
                          remarks.trim() && !actionLoading["reject"]
                            ? "border-rose-700 bg-rose-600 hover:bg-rose-700 text-white shadow-md focus:outline-none focus:ring-2 focus:ring-rose-500 cursor-pointer"
                            : "border-slate-300 dark:border-slate-700 bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 opacity-100 cursor-not-allowed"
                        }`}
                      >
                        {actionLoading["reject"] ? (
                          <RefreshCw className="h-4 w-4 animate-spin text-current shrink-0" />
                        ) : (
                          <XCircle className="h-4 w-4 text-current shrink-0" />
                        )}
                        <span className="text-xs sm:text-sm font-bold">{actionLoading["reject"] ? "Processing..." : "✕ Reject Application"}</span>
                      </button>
                    </div>

                    {!canApprove && (
                      <p className="text-[11px] text-slate-600 dark:text-slate-400 text-center font-bold italic pt-1">
                        Application cannot be approved because one or more required documents have not completed officer verification.
                      </p>
                    )}
                  </div>
                );
              })()}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
