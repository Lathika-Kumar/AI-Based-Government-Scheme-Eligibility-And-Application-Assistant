import React, { useState, useRef } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { usePageMeta } from "@utils/usePageMeta";
import { useApplicationDetails } from "@user/hooks/useApplications";
import applicationService, { REAPPLYABLE_STATUSES } from "@services/applicationService";
import { useToast } from "@components/ui/ToastNotification";
import {
  ArrowLeft, RefreshCw, AlertCircle, CheckCircle2, XCircle, Clock,
  FileText, Upload, Download, Send, X, AlertTriangle, ChevronDown, ChevronUp,
  History, Sparkles, ShieldCheck, RotateCcw
} from "lucide-react";

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

const STATUS_COLORS = {
  DRAFT:                 "text-gray-600",
  DOCUMENTS_PENDING:     "text-amber-600",
  READY_FOR_SUBMISSION:  "text-blue-600",
  SUBMITTED:             "text-indigo-600",
  UNDER_REVIEW:          "text-purple-600",
  DOCUMENT_VERIFICATION: "text-purple-600",
  CORRECTION_REQUIRED:   "text-red-600",
  APPROVED:              "text-green-600",
  REJECTED:              "text-red-600",
  CANCELLED:             "text-gray-400",
};

const DETAILED_STATUS_CONFIG = {
  NOT_UPLOADED:                { label: "Not Uploaded",                   color: "text-gray-600 dark:text-slate-400",    bg: "bg-gray-100 dark:bg-slate-800",    border: "border-gray-300 dark:border-slate-700" },
  UPLOADED:                    { label: "Uploaded",                       color: "text-blue-700 dark:text-blue-300",    bg: "bg-blue-100 dark:bg-blue-950/60",   border: "border-blue-300 dark:border-blue-800" },
  PROCESSING:                  { label: "AI & OCR Processing",            color: "text-blue-700 dark:text-blue-300",    bg: "bg-blue-100 dark:bg-blue-950/60",   border: "border-blue-300 dark:border-blue-800" },
  OCR_PROCESSING:              { label: "OCR Processing",                 color: "text-amber-700 dark:text-amber-300",  bg: "bg-amber-100 dark:bg-amber-950/60", border: "border-amber-300 dark:border-amber-800" },
  OCR_COMPLETED:               { label: "OCR Completed",                  color: "text-indigo-700 dark:text-indigo-300", bg: "bg-indigo-100 dark:bg-indigo-950/60", border: "border-indigo-300 dark:border-indigo-800" },
  AI_VERIFIED:                 { label: "AI Verified (Officer Pending)",  color: "text-indigo-700 dark:text-indigo-300", bg: "bg-indigo-100 dark:bg-indigo-950/60", border: "border-indigo-300 dark:border-indigo-800" },
  AI_REVIEW_REQUIRED:          { label: "AI Review Flagged",              color: "text-amber-700 dark:text-amber-300",  bg: "bg-amber-100 dark:bg-amber-950/60", border: "border-amber-300 dark:border-amber-800" },
  AI_REJECTED:                 { label: "AI Rejected",                    color: "text-red-700 dark:text-red-300",     bg: "bg-red-100 dark:bg-red-950/60",     border: "border-red-300 dark:border-red-800" },
  PENDING_ADMIN_VERIFICATION:  { label: "Pending Officer Review",         color: "text-purple-700 dark:text-purple-300",bg: "bg-purple-100 dark:bg-purple-950/60", border: "border-purple-300 dark:border-purple-800" },
  ADMIN_VERIFIED:              { label: "Officer Verified",               color: "text-emerald-700 dark:text-emerald-300", bg: "bg-emerald-100 dark:bg-emerald-950/60", border: "border-emerald-300 dark:border-emerald-800" },
  VERIFIED:                    { label: "Officer Verified",               color: "text-emerald-700 dark:text-emerald-300", bg: "bg-emerald-100 dark:bg-emerald-950/60", border: "border-emerald-300 dark:border-emerald-800" },
  CORRECTION_REQUIRED:         { label: "Correction Required",             color: "text-orange-700 dark:text-orange-300", bg: "bg-orange-100 dark:bg-orange-950/60", border: "border-orange-300 dark:border-orange-800" },
  REJECTED:                    { label: "Rejected",                       color: "text-red-700 dark:text-red-300",     bg: "bg-red-100 dark:bg-red-950/60",     border: "border-red-300 dark:border-red-800" },
  REUPLOAD_REQUIRED:           { label: "Re-upload Required",             color: "text-orange-700 dark:text-orange-300", bg: "bg-orange-100 dark:bg-orange-950/60", border: "border-orange-300 dark:border-orange-800" },
};

function fmtDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function fmtSize(bytes) {
  if (!bytes) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
}

const EVENT_ICONS = {
  APPLICATION_CREATED:        { icon: FileText,      color: "text-blue-500",    bg: "bg-blue-100" },
  DOCUMENT_UPLOADED:          { icon: Upload,        color: "text-indigo-500",  bg: "bg-indigo-100" },
  DOCUMENTS_PENDING:          { icon: AlertTriangle, color: "text-amber-500",   bg: "bg-amber-100" },
  READY_FOR_SUBMISSION:       { icon: CheckCircle2,  color: "text-blue-500",    bg: "bg-blue-100" },
  APPLICATION_SUBMITTED:      { icon: Send,          color: "text-indigo-500",  bg: "bg-indigo-100" },
  UNDER_REVIEW:               { icon: Clock,         color: "text-purple-500",  bg: "bg-purple-100" },
  DOCUMENT_VERIFIED:          { icon: CheckCircle2,  color: "text-emerald-500", bg: "bg-emerald-100" },
  DOCUMENT_REJECTED:          { icon: XCircle,       color: "text-red-500",     bg: "bg-red-100" },
  DOCUMENT_STATUS_CHANGED:    { icon: RefreshCw,     color: "text-purple-500",  bg: "bg-purple-100" },
  DOCUMENT_REUPLOAD_REQUIRED: { icon: AlertTriangle, color: "text-orange-500",  bg: "bg-orange-100" },
  CORRECTION_REQUIRED:        { icon: AlertCircle,   color: "text-red-500",     bg: "bg-red-100" },
  APPROVED:                   { icon: CheckCircle2,  color: "text-green-500",   bg: "bg-green-100" },
  REJECTED:                   { icon: XCircle,       color: "text-red-500",     bg: "bg-red-100" },
  CANCELLED:                  { icon: X,             color: "text-gray-500",    bg: "bg-gray-100" },
};

function getEventIcon(type) {
  return EVENT_ICONS[type] || { icon: FileText, color: "text-gray-500", bg: "bg-gray-100" };
}

export default function ApplicationDetail() {
  const { id } = useParams();
  usePageMeta("Application Details", "View and manage your scheme application");
  const showToast = useToast();
  const navigate = useNavigate();

  const { application, timeline, loading, error, refetch } = useApplicationDetails(id);
  const [uploading, setUploading] = useState({}); // { [docCode]: loading }
  const [submitting, setSubmitting] = useState(false);
  const [showTimeline, setShowTimeline] = useState(true);
  const [expandedHistory, setExpandedHistory] = useState({});
  const [showReapplyModal, setShowReapplyModal] = useState(false);
  const [reapplying, setReapplying] = useState(false);
  const fileInputRefs = useRef({});

  const handleConfirmReapply = async () => {
    if (!application) return;
    setReapplying(true);
    const result = await applicationService.reapplyApplication(application.id);
    setReapplying(false);
    if (result.error) {
      showToast(result.message || "Failed to reapply for scheme.", "error");
    } else {
      const newApp = result.data;
      showToast(`New application ${newApp.applicationNumber} created! Reused saved documents from your Document Vault.`, "success");
      setShowReapplyModal(false);
      navigate(`/applications/${newApp.id}`);
    }
  };

  const handleUpload = async (docCode, file) => {
    if (!file) return;
    if (file.size > MAX_FILE_SIZE) {
      showToast("File must be smaller than 5MB.", "error");
      return;
    }
    if (!["application/pdf","image/jpeg","image/jpg","image/png"].includes(file.type)) {
      showToast("Only PDF, JPG, or PNG files are accepted.", "error");
      return;
    }
    setUploading((prev) => ({ ...prev, [docCode]: true }));
    const result = await applicationService.uploadDocument(id, docCode, file);
    setUploading((prev) => ({ ...prev, [docCode]: false }));
    if (result.error) {
      showToast(result.message || "Upload failed.", "error");
    } else {
      showToast(`Document uploaded successfully.`, "success");
      refetch();
    }
  };

  const handleDownload = async (docCode, fileName) => {
    try {
      showToast("Downloading document...", "info");
      const res = await applicationService.downloadDocument(id, docCode);
      if (res.error || !res.data) {
        showToast(res.message || "Failed to download document.", "error");
        return;
      }
      const blob = res.data instanceof Blob ? res.data : new Blob([res.data], {
        type: res.headers?.get?.("content-type") || "application/octet-stream"
      });
      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = blobUrl;
      link.download = fileName || `${docCode}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 10000);
      showToast("Document downloaded successfully.", "success");
    } catch (err) {
      console.error("Download error:", err);
      showToast("Failed to download document.", "error");
    }
  };

  const handleSubmit = async () => {
    if (!window.confirm("Submit this application? Make sure all required documents are uploaded.")) return;
    setSubmitting(true);
    const result = await applicationService.submitApplication(id);
    setSubmitting(false);
    if (result.error) {
      showToast(result.message || "Submission failed.", "error");
    } else {
      showToast("Application submitted successfully!", "success");
      refetch();
    }
  };

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="bg-gray-100 animate-pulse rounded-xl h-32" />
        <div className="bg-gray-100 animate-pulse rounded-xl h-64" />
      </div>
    );
  }

  if (error || !application) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-xl p-8 text-center">
        <AlertCircle className="h-10 w-10 text-red-400 mx-auto mb-3" />
        <p className="font-semibold text-red-800">Application not found</p>
        <p className="text-sm text-red-600 mt-1">{error}</p>
        <Link to="/applications" className="mt-4 inline-flex items-center gap-1.5 text-government-blue text-sm font-semibold hover:underline">
          <ArrowLeft className="h-4 w-4" /> Back to Applications
        </Link>
      </div>
    );
  }

  const statusColor = STATUS_COLORS[application.status] || "text-gray-600";
  const canSubmit = application.status === "READY_FOR_SUBMISSION";
  const canUpload = !["APPROVED", "REJECTED", "CANCELLED"].includes(application.status);
  const readinessPercent = application.documentReadiness?.percentage ?? application.documentReadiness ?? 0;

  return (
    <div className="space-y-6">
      {/* Back */}
      <Link to="/applications" className="inline-flex items-center gap-1.5 text-government-blue dark:text-indigo-400 text-sm font-semibold hover:underline">
        <ArrowLeft className="h-4 w-4" /> My Applications
      </Link>

      {/* Application Header */}
      <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div>
            <p className="text-xs text-gray-400 dark:text-slate-500 font-mono mb-1">{application.applicationNumber}</p>
            <h1 className="text-xl font-bold text-gray-900 dark:text-slate-100">
              {typeof application.schemeTitle === "string" ? application.schemeTitle : (application.schemeTitle?.english || application.schemeName || application.schemeCode)}
            </h1>
            <p className="text-sm text-gray-500 dark:text-slate-400 mt-0.5">{application.schemeCode}</p>
            <div className="flex items-center gap-2 mt-2">
              <span className={`text-sm font-bold ${statusColor}`}>● {application.status?.replace(/_/g, " ")}</span>
            </div>
          </div>
          <div className="text-right text-xs text-gray-500 dark:text-slate-400 space-y-1">
            <p>Created: {fmtDate(application.createdAt)}</p>
            {application.submittedAt && <p>Submitted: {fmtDate(application.submittedAt)}</p>}
            {application.updatedAt && <p>Updated: {fmtDate(application.updatedAt)}</p>}
          </div>
        </div>

        {/* Document Readiness */}
        <div className="mt-4">
          <div className="flex justify-between text-xs text-gray-500 dark:text-slate-400 mb-1">
            <span>Document Readiness</span>
            <span className="font-bold text-gray-800 dark:text-slate-200">{readinessPercent}%</span>
          </div>
          <div className="h-2 bg-gray-100 dark:bg-slate-800 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all ${readinessPercent === 100 ? "bg-green-500" : readinessPercent >= 50 ? "bg-amber-400" : "bg-red-400"}`}
              style={{ width: `${readinessPercent}%` }}
            />
          </div>
        </div>

        {/* Submit button */}
        {canSubmit && (
          <button onClick={handleSubmit} disabled={submitting}
            className="mt-4 flex items-center gap-2 bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white px-6 py-2.5 rounded-lg text-sm font-bold transition">
            {submitting ? <><RefreshCw className="h-4 w-4 animate-spin" /> Submitting...</> : <><Send className="h-4 w-4" /> Submit Application</>}
          </button>
        )}

        {/* Reapply Banner for Terminal Applications */}
        {REAPPLYABLE_STATUSES.includes(application.status) && (
          <div className="mt-4 p-4 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 rounded-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div>
              <p className="font-bold text-sm text-amber-900 dark:text-amber-200 flex items-center gap-1.5">
                <RotateCcw className="h-4 w-4 text-amber-600 shrink-0" />
                This application is {application.status.toLowerCase()}. You can reapply for this scheme.
              </p>
              <p className="text-xs text-amber-700 dark:text-amber-300/80 mt-0.5">
                Your previous application will remain in your application history. Existing documents from your Document Vault will be automatically reused.
              </p>
            </div>
            <button
              onClick={() => setShowReapplyModal(true)}
              className="shrink-0 flex items-center gap-2 bg-amber-600 hover:bg-amber-700 active:bg-amber-800 text-white px-5 py-2.5 rounded-lg text-xs font-bold transition shadow-sm">
              <RotateCcw className="h-4 w-4" /> Reapply for Scheme
            </button>
          </div>
        )}

        {/* Correction warning */}
        {application.status === "CORRECTION_REQUIRED" && (
          <div className="mt-4 flex items-start gap-2 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 text-sm p-4 rounded-xl">
            <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
            <div>
              <p className="font-bold">Correction Required</p>
              <p className="text-xs text-red-600 dark:text-red-400 mt-0.5">A verification officer has returned one or more documents for correction. Please review the reason(s) below and upload a replacement document.</p>
            </div>
          </div>
        )}
      </div>

      {/* Documents Section */}
      {application.documents?.length > 0 && (
        <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl shadow-sm">
          <div className="p-5 border-b border-gray-100 dark:border-slate-800">
            <h2 className="font-bold text-gray-900 dark:text-slate-100 flex items-center gap-2">
              <FileText className="h-4 w-4 text-government-blue dark:text-indigo-400" /> Required Documents & Readiness Status
            </h2>
          </div>
          <div className="divide-y divide-gray-100 dark:divide-slate-800">
            {application.documents.map((doc) => {
              const statusKey = doc.status || (doc.uploaded ? (doc.verificationStatus === "VERIFIED" ? "VERIFIED" : (doc.verificationStatus === "REJECTED" ? "REJECTED" : "PENDING_ADMIN_VERIFICATION")) : "NOT_UPLOADED");
              const sCfg = DETAILED_STATUS_CONFIG[statusKey] || DETAILED_STATUS_CONFIG.NOT_UPLOADED;
              const hasHistory = Array.isArray(doc.versionHistory) && doc.versionHistory.length > 0;
              const isHistoryExpanded = expandedHistory[doc.documentCode];

              return (
                <div key={doc.documentCode} className="p-5">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-sm text-gray-900 dark:text-slate-100">{doc.documentName || doc.documentCode}</span>
                        {doc.mandatory && (
                          <span className="text-xs bg-red-100 dark:bg-red-950/60 text-red-700 dark:text-red-300 px-1.5 py-0.5 rounded font-semibold">Required</span>
                        )}
                        <span className="text-xs bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 px-1.5 py-0.5 rounded font-mono font-bold">
                          v{doc.version || 1}
                        </span>
                        {(doc.source === "DOCUMENT_VAULT" || doc.vaultDocumentId) && (
                          <span className="text-xs bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800 px-2 py-0.5 rounded-full font-bold flex items-center gap-1">
                            <CheckCircle2 className="h-3 w-3 text-emerald-600 dark:text-emerald-400" />
                            ✓ Available from Document Vault
                          </span>
                        )}
                        {doc.verificationScore != null && (
                          <span className="text-[10px] bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800/60 px-1.5 py-0.5 rounded font-bold flex items-center gap-1">
                            <Sparkles className="h-3 w-3 text-indigo-500" />
                            AI Validation Passed: {doc.verificationScore}/100
                          </span>
                        )}
                        {doc.identityMatchStatus && (
                          <span className={`text-[10px] border px-1.5 py-0.5 rounded font-bold flex items-center gap-1 ${
                            doc.identityMatchStatus === "MATCH"
                              ? "bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800/60"
                              : "bg-red-50 dark:bg-red-950/60 text-red-700 dark:text-red-300 border-red-200 dark:border-red-800/60"
                          }`}>
                            <ShieldCheck className="h-3 w-3 text-emerald-600" />
                            Identity: {doc.identityMatchStatus === "MATCH" ? "Verified (MATCH)" : doc.identityMatchStatus}
                          </span>
                        )}
                        <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${sCfg.bg} ${sCfg.color} ${sCfg.border}`}>
                          {sCfg.label}
                        </span>
                      </div>
                      {doc.issuingAuthority && (
                        <p className="text-xs text-gray-500 dark:text-slate-400 mt-0.5">Issued by: {doc.issuingAuthority}</p>
                      )}

                      {/* Aadhaar UIDAI statutory notice */}
                      {(doc.documentCode?.toUpperCase().includes("AADHAAR") || doc.documentName?.toLowerCase().includes("aadhaar")) && (
                        <p className="text-[11px] text-slate-500 dark:text-slate-400 italic mt-1">
                          * AI document validation only. Official identity authentication has not been performed with UIDAI. Awaiting officer verification.
                        </p>
                      )}

                      {/* ONE_OF Alternative Group indicator */}
                      {doc.alternativeGroupType === "ONE_OF" && doc.alternatives && doc.alternatives.length > 0 && (
                        <div className="mt-2 pl-2.5 py-1.5 bg-saffron/5 dark:bg-amber-950/30 border-l-2 border-saffron/50 dark:border-amber-400/50 rounded-r text-xs text-gray-700 dark:text-slate-300">
                          <span className="font-bold text-saffron-dark dark:text-amber-400 uppercase text-[10px] block">Satisfied by any ONE of:</span>
                          <span className="text-gray-600 dark:text-slate-400 font-medium">{doc.alternatives.join(" · ")}</span>
                        </div>
                      )}

                      {doc.uploaded && doc.fileName && (
                        <p className="text-xs text-gray-500 dark:text-slate-400 mt-1 flex items-center gap-1">
                          <FileText className="h-3 w-3" />
                          {doc.fileName} {doc.fileSize && `· ${fmtSize(doc.fileSize)}`}
                          {doc.uploadedAt && ` · ${fmtDate(doc.uploadedAt)}`}
                        </p>
                      )}

                      {/* Correction Reason Alert */}
                      {doc.correctionReason && (
                        <div className="mt-2 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900 rounded-lg p-2.5 flex items-start gap-2 text-xs text-amber-800 dark:text-amber-300">
                          <AlertTriangle className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />
                          <div>
                            <span className="font-bold">Correction Required by Officer:</span> {doc.correctionReason}
                          </div>
                        </div>
                      )}

                      {/* Rejection Alert */}
                      {doc.rejectionReason && (
                        <div className="mt-2 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg p-2.5 flex items-start gap-2 text-xs text-red-800 dark:text-red-300">
                          <AlertCircle className="h-4 w-4 text-red-500 shrink-0 mt-0.5" />
                          <div>
                            <span className="font-bold">Rejection Reason:</span> {doc.rejectionReason}
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Actions */}
                    <div className="flex items-center gap-2 shrink-0">
                      {hasHistory && (
                        <button
                          type="button"
                          onClick={() => setExpandedHistory(prev => ({ ...prev, [doc.documentCode]: !prev[doc.documentCode] }))}
                          className="flex items-center gap-1 bg-gray-50 dark:bg-slate-800 hover:bg-gray-100 dark:hover:bg-slate-700 text-gray-600 dark:text-slate-300 px-2.5 py-1.5 rounded-lg text-xs font-semibold border border-gray-200 dark:border-slate-700 transition"
                        >
                          <History className="h-3.5 w-3.5" />
                          History ({doc.versionHistory.length})
                        </button>
                      )}

                      {/* Primary Action when document is uploaded / available from vault */}
                      {doc.uploaded && (
                        <button
                          type="button"
                          onClick={() => handleDownload(doc.documentCode, doc.fileName)}
                          className="flex items-center gap-1.5 bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 text-white px-3.5 py-2 rounded-lg text-xs font-bold transition shadow-xs"
                          title="View Document"
                        >
                          <Download className="h-3.5 w-3.5" /> View Document
                        </button>
                      )}

                      {/* Secondary / Upload Actions */}
                      {canUpload && doc.status !== "VERIFIED" && doc.status !== "ADMIN_VERIFIED" && doc.verificationStatus !== "VERIFIED" && (
                        <div>
                          <input
                            ref={(el) => (fileInputRefs.current[doc.documentCode] = el)}
                            type="file"
                            accept=".pdf,.jpg,.jpeg,.png"
                            className="hidden"
                            onChange={(e) => handleUpload(doc.documentCode, e.target.files[0])}
                          />
                          <button
                            onClick={() => fileInputRefs.current[doc.documentCode]?.click()}
                            disabled={uploading[doc.documentCode]}
                            className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-semibold transition disabled:opacity-60 ${
                              doc.uploaded
                                ? "bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-300 dark:border-slate-600"
                                : doc.status === "REJECTED" || doc.status === "REUPLOAD_REQUIRED" || doc.status === "CORRECTION_REQUIRED"
                                ? "bg-amber-600 hover:bg-amber-700 text-white font-bold"
                                : "bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 text-white font-bold"
                            }`}
                          >
                            {uploading[doc.documentCode] ? (
                              <><RefreshCw className="h-3.5 w-3.5 animate-spin" /> {doc.uploaded ? "Replacing..." : "Uploading..."}</>
                            ) : doc.uploaded ? (
                              <><Upload className="h-3.5 w-3.5" /> Replace Document</>
                            ) : (
                              <><Upload className="h-3.5 w-3.5" /> {doc.status === "CORRECTION_REQUIRED" ? "Upload Corrected Document" : "Upload"}</>
                            )}
                          </button>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Version History Drawer */}
                  {isHistoryExpanded && hasHistory && (
                    <div className="mt-3 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-lg p-3 space-y-2">
                      <p className="text-xs font-bold text-slate-700 dark:text-slate-200">Previous Versions History</p>
                      <div className="divide-y divide-slate-200 dark:divide-slate-700">
                        {doc.versionHistory.map((hist, hIdx) => (
                          <div key={hIdx} className="py-1.5 flex items-center justify-between text-xs text-slate-600 dark:text-slate-300">
                            <div>
                              <span className="font-mono font-bold text-slate-800 dark:text-slate-100">v{hist.version}</span>: {hist.fileName || "document"} ({fmtSize(hist.fileSize)})
                              {hist.uploadedAt && <span className="text-slate-400 dark:text-slate-500 ml-1.5">{fmtDate(hist.uploadedAt)}</span>}
                            </div>
                            <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${hist.status === "REJECTED" ? "bg-red-100 dark:bg-red-950/60 text-red-700 dark:text-red-300" : "bg-gray-100 dark:bg-slate-700 text-gray-600 dark:text-slate-300"}`}>
                              {hist.status || hist.verificationStatus}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Timeline */}
      {timeline && (
        <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl shadow-sm">
          <button
            onClick={() => setShowTimeline(!showTimeline)}
            className="w-full p-5 flex items-center justify-between text-left border-b border-gray-100 dark:border-slate-800"
          >
            <h2 className="font-bold text-gray-900 dark:text-slate-100 flex items-center gap-2">
              <Clock className="h-4 w-4 text-government-blue dark:text-indigo-400" /> Application Timeline
              <span className="text-xs text-gray-400 dark:text-slate-500 font-normal">({timeline.events?.length ?? 0} events)</span>
            </h2>
            {showTimeline ? <ChevronUp className="h-4 w-4 text-gray-400 dark:text-slate-400" /> : <ChevronDown className="h-4 w-4 text-gray-400 dark:text-slate-400" />}
          </button>

          {showTimeline && (
            <div className="p-5">
              <ol className="relative border-l border-gray-200 dark:border-slate-800 space-y-6 ml-3">
                {(timeline.events ?? []).map((event, idx) => {
                  const eCfg = getEventIcon(event.eventType);
                  const EIcon = eCfg.icon;
                  const isLatest = idx === 0;
                  return (
                    <li key={idx} className="ml-6">
                      <span className={`absolute -left-3.5 flex items-center justify-center w-7 h-7 rounded-full ${eCfg.bg} ring-4 ring-white dark:ring-slate-900`}>
                        <EIcon className={`h-3.5 w-3.5 ${eCfg.color}`} />
                      </span>
                      <div className={`p-4 rounded-xl border ${isLatest ? "bg-government-blue/5 dark:bg-indigo-950/40 border-government-blue/20 dark:border-indigo-900" : "bg-gray-50 dark:bg-slate-800/70 border-gray-200 dark:border-slate-700"}`}>
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <p className="text-sm font-bold text-gray-900 dark:text-slate-100">
                              {event.eventType?.replace(/_/g, " ")}
                            </p>
                            {event.message && (
                              <p className="text-xs text-gray-600 dark:text-slate-300 mt-0.5 leading-relaxed">{event.message}</p>
                            )}
                            {event.fromStatus && event.toStatus && event.fromStatus !== event.toStatus && (
                              <p className="text-xs text-gray-400 dark:text-slate-500 mt-0.5">
                                {event.fromStatus} → {event.toStatus}
                              </p>
                            )}
                          </div>
                          <span className="text-[10px] text-gray-400 dark:text-slate-500 font-mono shrink-0">
                            {fmtDate(event.timestamp)}
                          </span>
                        </div>
                      </div>
                    </li>
                  );
                })}
              </ol>
            </div>
          )}
        </div>
      )}

      {/* Reapply Confirmation Modal */}
      {showReapplyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-fade-in">
          <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl max-w-md w-full shadow-2xl p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100 dark:border-slate-800">
              <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400 font-bold text-base">
                <RotateCcw className="h-5 w-5" />
                <span>Reapply for Scheme</span>
              </div>
              <button
                onClick={() => !reapplying && setShowReapplyModal(false)}
                disabled={reapplying}
                className="text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 p-1 rounded-lg">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-3 text-sm text-gray-600 dark:text-slate-300">
              <p className="font-semibold text-gray-900 dark:text-slate-100 text-base">
                {typeof application.schemeTitle === "string"
                  ? application.schemeTitle
                  : (application.schemeTitle?.english || application.schemeCode)}
              </p>
              <p className="text-xs text-gray-500 dark:text-slate-400 font-mono">
                Previous Application: {application.applicationNumber} ({application.status})
              </p>

              <div className="p-3.5 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 rounded-xl text-xs text-amber-900 dark:text-amber-200 space-y-1.5">
                <p className="font-semibold flex items-center gap-1.5 text-amber-800 dark:text-amber-300">
                  <CheckCircle2 className="h-4 w-4 text-amber-600 shrink-0" />
                  Reapply for this scheme?
                </p>
                <p>
                  Your previous application will remain in your application history. Your saved documents will be reused where applicable.
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={reapplying}
                onClick={() => setShowReapplyModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-semibold border border-gray-300 dark:border-slate-700 text-gray-700 dark:text-slate-300 hover:bg-gray-100 dark:hover:bg-slate-800 transition disabled:opacity-50">
                Cancel
              </button>
              <button
                type="button"
                disabled={reapplying}
                onClick={handleConfirmReapply}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-amber-600 hover:bg-amber-700 active:bg-amber-800 text-white shadow-sm flex items-center gap-2 transition disabled:opacity-50">
                {reapplying ? (
                  <>
                    <RefreshCw className="h-4 w-4 animate-spin" />
                    Creating Application...
                  </>
                ) : (
                  <>
                    <RotateCcw className="h-4 w-4" />
                    Continue Reapplication
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
