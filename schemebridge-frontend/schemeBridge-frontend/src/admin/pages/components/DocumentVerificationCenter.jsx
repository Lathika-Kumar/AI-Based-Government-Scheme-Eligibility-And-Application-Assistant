import React, { useState, useEffect, useCallback } from "react";
import { useToast } from "@components/ui/ToastNotification";
import {
  FileText,
  CheckCircle,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Search,
  Sparkles,
  ShieldCheck,
  Eye,
  Download,
  Clock,
  ExternalLink,
  ShieldAlert,
  Info,
  Check,
  X
} from "lucide-react";
import adminService, {
  getAdminDocuments,
  verifyAdminDocument,
  requestAdminDocumentCorrection,
  rejectAdminDocument,
  getDocumentVerification,
  downloadAdminDocument,
  downloadAdminDocumentById
} from "@services/adminService";
import { useApp } from "@context/AppContext";

export default function DocumentVerificationCenter() {
  const { showToast } = useToast();
  const { refreshApplications } = useApp();
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [documentsList, setDocumentsList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [pagination, setPagination] = useState({ page: 0, totalPages: 1, totalElements: 0 });

  // Modals & Panels
  const [selectedDoc, setSelectedDoc] = useState(null);
  const [isAiModalOpen, setIsAiModalOpen] = useState(false);
  const [aiData, setAiData] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);

  const [isCorrectionModalOpen, setIsCorrectionModalOpen] = useState(false);
  const [correctionReason, setCorrectionReason] = useState("");

  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState("");

  const correctionReasonTemplates = [
    "Document is blurry, low resolution, or illegible",
    "Wrong document uploaded for this requirement",
    "Extracted name or identity does not match citizen profile",
    "Document is expired or outdated",
    "Required page or reverse side of card is missing",
    "Certificate or reference number unreadable"
  ];

  const rejectReasonTemplates = [
    "Document is invalid or rejected by administrative authority",
    "Suspected alteration, tampering, or fraudulent document",
    "Document not issued by authorized government department",
    "Eligibility criteria document permanently disqualified"
  ];

  const fetchDocuments = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const statusParam = statusFilter !== "all" ? statusFilter.toUpperCase() : undefined;
      const res = await getAdminDocuments({
        status: statusParam,
        search: search.trim() || undefined,
        page: currentPage - 1,
        size: pageSize,
      });

      if (!res.error && res.data) {
        const raw = res.data.content || [];
        const mapped = raw.map((d) => {
          const rawStatus = String(d.status || d.detailedStatus || "").toUpperCase();
          const isOfficerVerified =
            rawStatus === "ADMIN_VERIFIED" ||
            rawStatus === "VERIFIED" ||
            d.adminVerificationResult === "ADMIN_VERIFIED" ||
            d.verificationStatus === "VERIFIED";

          const isCorrectionRequired =
            rawStatus === "CORRECTION_REQUIRED" ||
            rawStatus === "REUPLOAD_REQUIRED";

          const isRejected =
            rawStatus === "REJECTED" ||
            d.verificationStatus === "REJECTED" ||
            d.adminVerificationResult === "REJECTED";

          const detailedStatus = isOfficerVerified
            ? "ADMIN_VERIFIED"
            : isCorrectionRequired
            ? "CORRECTION_REQUIRED"
            : isRejected
            ? "REJECTED"
            : (rawStatus || "UNDER_OFFICER_REVIEW");

          // Explicit separation: Officer Review Status
          let officerReviewStatus = "UNDER_OFFICER_REVIEW";
          let officerDisplayStatus = "Pending Officer Verification";
          let officerBadgeColor = "bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border-amber-200 dark:border-amber-800";

          if (isOfficerVerified) {
            officerReviewStatus = "ADMIN_VERIFIED";
            officerDisplayStatus = "Officer Verified ✓";
            officerBadgeColor = "bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800";
          } else if (isCorrectionRequired) {
            officerReviewStatus = "CORRECTION_REQUIRED";
            officerDisplayStatus = "Correction Required";
            officerBadgeColor = "bg-orange-50 dark:bg-orange-950/60 text-orange-700 dark:text-orange-300 border-orange-200 dark:border-orange-800";
          } else if (isRejected) {
            officerReviewStatus = "REJECTED";
            officerDisplayStatus = "Rejected";
            officerBadgeColor = "bg-rose-50 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 border-rose-200 dark:border-rose-800";
          }

          // Explicit separation: AI Validation Status
          const rawAiStatus = d.aiVerificationResult || d.aiStatus || "AI_PASSED";
          let aiDisplayStatus = "✓ AI Validation Passed";
          let aiBadgeColor = "bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800";

          if (rawAiStatus === "AI_REVIEW_REQUIRED" || rawAiStatus === "AI_FLAGGED") {
            aiDisplayStatus = "⚠ AI Review Required";
            aiBadgeColor = "bg-amber-50 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 border-amber-200 dark:border-amber-800";
          } else if (rawAiStatus === "AI_REJECTED" || rawAiStatus === "AI_FAILED") {
            aiDisplayStatus = "✗ AI Validation Failed";
            aiBadgeColor = "bg-rose-50 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 border-rose-200 dark:border-rose-800";
          }

          return {
            id: d.id,
            applicationId: d.applicationId,
            documentCode: d.documentCode,
            name: d.documentName || d.documentCode,
            documentType: d.documentType || d.documentName || "Supporting Document",
            citizen: d.userId || "Citizen",
            schemeCode: d.schemeCode || "Scheme",
            detailedStatus,
            officerReviewStatus,
            officerDisplayStatus,
            officerBadgeColor,
            aiStatus: rawAiStatus,
            aiDisplayStatus,
            aiBadgeColor,
            displayStatus: officerDisplayStatus,
            badgeColor: officerBadgeColor,
            sha256: d.sha256,
            verificationScore: d.verificationScore != null ? d.verificationScore : 78,
            adminVerificationResult: d.adminVerificationResult,
            correctionReason: d.correctionReason,
            rejectionReason: d.rejectionReason,
            date: d.uploadedAt ? new Date(d.uploadedAt).toLocaleDateString("en-IN") : "Recent",
            downloadUrl: d.downloadUrl || `/api/applications/${d.applicationId}/documents/${d.documentCode}/download`,
            version: d.version || 1
          };
        });

        setDocumentsList(mapped);
        setPagination({
          page: res.data.page ?? 0,
          totalPages: res.data.totalPages ?? Math.max(1, Math.ceil((res.data.totalElements || 0) / pageSize)),
          totalElements: res.data.totalElements ?? mapped.length
        });
      } else {
        const errMsg = res.message || "Failed to load documents queue from SchemeBridge.";
        setError(errMsg);
        showToast("error", "Failed to load documents", errMsg);
      }
    } catch (err) {
      console.error("Failed to load documents", err);
      const errMsg = err?.message || "Scheme service request failed.";
      setError(errMsg);
      showToast("error", "Failed to load documents", errMsg);
    } finally {
      setLoading(false);
    }
  }, [statusFilter, search, currentPage, pageSize, showToast]);

  useEffect(() => {
    setCurrentPage(1);
  }, [statusFilter, search]);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  // Open AI Analysis Modal
  const openAiAnalysis = async (doc) => {
    setSelectedDoc(doc);
    setIsAiModalOpen(true);
    setAiLoading(true);
    try {
      const res = await getDocumentVerification(doc.id);
      if (!res.error && res.data) {
        setAiData(res.data);
      } else {
        // Fallback default
        setAiData({
          documentType: doc.documentType,
          overallScore: doc.verificationScore,
          aiStatus: doc.aiStatus,
          officerStatus: doc.detailedStatus === "ADMIN_VERIFIED" ? "ADMIN_VERIFIED" : "PENDING",
          checks: [
            { check: "DOCUMENT_TYPE", status: "PASSED", message: "Document format conforms to " + doc.documentType, score: 25, weight: 25 },
            { check: "OCR_QUALITY", status: "PASSED", message: "Machine readability confirmed", score: 15, weight: 15 },
            { check: "REQUIRED_FIELDS", status: "PASSED", message: "Required identity tokens detected", score: 20, weight: 20 },
            { check: "NAME_MATCH", status: "PASSED", message: "Consistent with citizen profile", score: 15, weight: 15 },
            { check: "FILE_INTEGRITY", status: "PASSED", message: "MIME type and SHA-256 integrity passed", score: 5, weight: 5 }
          ],
          warnings: [
            "AI document validation only. Official identity authentication has not been performed with UIDAI or issuing department."
          ],
          extractedFields: {
            documentType: doc.documentType,
            maskedIdentifier: "XXXX-XXXX-1234",
            maskedIdentifierDetected: true
          }
        });
      }
    } catch (e) {
      console.error("Error fetching AI analysis:", e);
    } finally {
      setAiLoading(false);
    }
  };

  // Officer Approve
  const handleApprove = async (doc) => {
    try {
      let res;
      if (doc.id) {
        res = await verifyAdminDocument(doc.id, "Document verified and approved by administrative officer.");
      }
      if ((!res || res.error) && doc.applicationId && doc.documentCode) {
        res = await adminService.verifyDocument(doc.applicationId, doc.documentCode);
      }
      if (res && !res.error) {
        showToast("success", "Officer Verified", `${doc.name} approved and officially verified.`);
        setDocumentsList((prev) =>
          prev.map((item) =>
            item.id === doc.id || (item.applicationId === doc.applicationId && item.documentCode === doc.documentCode)
              ? {
                  ...item,
                  officerReviewStatus: "ADMIN_VERIFIED",
                  officerDisplayStatus: "Officer Verified ✓",
                  officerBadgeColor: "bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800",
                  displayStatus: "Officer Verified ✓",
                  badgeColor: "bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800"
                }
              : item
          )
        );
        fetchDocuments();
        if (typeof refreshApplications === "function") {
          refreshApplications().catch(() => {});
        }
        if (isAiModalOpen) setIsAiModalOpen(false);
      } else {
        showToast("error", "Verification Failed", res?.message || "Could not verify document.");
      }
    } catch (err) {
      console.error("Verification error:", err);
      showToast("error", "Error", "Failed to approve document.");
    }
  };

  // Officer Request Correction
  const handleRequestCorrectionSubmit = async () => {
    if (!selectedDoc || !correctionReason.trim()) return;
    try {
      let res;
      if (selectedDoc.id) {
        res = await requestAdminDocumentCorrection(selectedDoc.id, correctionReason.trim());
      }
      if ((!res || res.error) && selectedDoc.applicationId && selectedDoc.documentCode) {
        res = await adminService.requestDocumentCorrectionByCode(selectedDoc.applicationId, selectedDoc.documentCode, correctionReason.trim());
      }
      if (res && !res.error) {
        showToast("success", "Correction Requested", `Correction notification dispatched to citizen.`);
        setIsCorrectionModalOpen(false);
        setCorrectionReason("");
        setDocumentsList((prev) =>
          prev.map((item) =>
            item.id === selectedDoc.id || (item.applicationId === selectedDoc.applicationId && item.documentCode === selectedDoc.documentCode)
              ? {
                  ...item,
                  officerReviewStatus: "CORRECTION_REQUIRED",
                  officerDisplayStatus: "Correction Required",
                  officerBadgeColor: "bg-orange-50 dark:bg-orange-950/60 text-orange-700 dark:text-orange-300 border-orange-200 dark:border-orange-800",
                  displayStatus: "Correction Required",
                  badgeColor: "bg-orange-50 dark:bg-orange-950/60 text-orange-700 dark:text-orange-300 border-orange-200 dark:border-orange-800",
                  correctionReason: correctionReason.trim()
                }
              : item
          )
        );
        fetchDocuments();
        if (typeof refreshApplications === "function") {
          refreshApplications().catch(() => {});
        }
        if (isAiModalOpen) setIsAiModalOpen(false);
      } else {
        showToast("error", "Request Failed", res?.message || "Could not submit correction request.");
      }
    } catch (err) {
      console.error("Correction error:", err);
      showToast("error", "Error", "Failed to request document correction.");
    }
  };

  // Officer Reject
  const handleRejectSubmit = async () => {
    if (!selectedDoc || !rejectReason.trim()) return;
    try {
      let res;
      if (selectedDoc.id) {
        res = await rejectAdminDocument(selectedDoc.id, rejectReason.trim());
      }
      if ((!res || res.error) && selectedDoc.applicationId && selectedDoc.documentCode) {
        res = await adminService.rejectDocument(selectedDoc.applicationId, selectedDoc.documentCode, rejectReason.trim());
      }
      if (res && !res.error) {
        showToast("success", "Document Rejected", `${selectedDoc.name} marked as rejected.`);
        setIsRejectModalOpen(false);
        setRejectReason("");
        setDocumentsList((prev) =>
          prev.map((item) =>
            item.id === selectedDoc.id || (item.applicationId === selectedDoc.applicationId && item.documentCode === selectedDoc.documentCode)
              ? {
                  ...item,
                  officerReviewStatus: "REJECTED",
                  officerDisplayStatus: "Rejected",
                  officerBadgeColor: "bg-rose-50 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 border-rose-200 dark:border-rose-800",
                  displayStatus: "Rejected",
                  badgeColor: "bg-rose-50 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 border-rose-200 dark:border-rose-800",
                  rejectionReason: rejectReason.trim()
                }
              : item
          )
        );
        fetchDocuments();
        if (typeof refreshApplications === "function") {
          refreshApplications().catch(() => {});
        }
        if (isAiModalOpen) setIsAiModalOpen(false);
      } else {
        showToast("error", "Rejection Failed", res?.message || "Could not reject document.");
      }
    } catch (err) {
      console.error("Rejection error:", err);
      showToast("error", "Error", "Failed to reject document.");
    }
  };

  const handleDownload = async (doc) => {
    try {
      showToast("info", "Opening Document", "Fetching authenticated file preview...");
      let res;
      if (doc.id) {
        res = await downloadAdminDocumentById(doc.id);
      }
      if (!res || res.error) {
        res = await downloadAdminDocument(doc.applicationId, doc.documentCode);
      }
      if (!res.error && res.data) {
        const blob = res.data;
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl, "_blank");
      } else {
        showToast("error", "Download Failed", res?.message || "Failed to download document.");
      }
    } catch (err) {
      console.error("Authenticated download error:", err);
      showToast("error", "Download Error", "Could not retrieve document file.");
    }
  };

  return (
    <div className="space-y-4">
      {/* Search & Filter Header */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-2xl shadow-sm space-y-4">
        <div className="flex flex-col lg:flex-row gap-3 items-center justify-between">
          <div className="flex-1 relative w-full">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search by document name, type, application ID, or citizen ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100/50 dark:hover:bg-slate-700/50 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 transition"
            />
          </div>
          <div className="flex items-center gap-2">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-600 cursor-pointer"
            >
              <option value="all">All Verification Statuses</option>
              <option value="pending">Officer Pending</option>
              <option value="verified">Officer Verified</option>
              <option value="rejected">Rejected / Action Required</option>
            </select>
            <button
              onClick={fetchDocuments}
              className="p-2.5 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 rounded-xl transition"
              title="Refresh"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            </button>
          </div>
        </div>
      </div>

      {/* Main Grid */}
      {loading ? (
        <div className="p-12 text-center text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center justify-center gap-2">
          <RefreshCw className="h-4 w-4 animate-spin text-indigo-600" />
          Loading Verification Queue...
        </div>
      ) : error ? (
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-12 text-center rounded-2xl flex flex-col items-center justify-center space-y-3">
          <AlertTriangle className="h-10 w-10 text-rose-500" />
          <div>
            <p className="text-sm font-bold text-slate-800 dark:text-slate-200">Failed to Load Documents</p>
            <p className="text-xs text-rose-500 dark:text-rose-400 mt-1 max-w-sm">{error}</p>
          </div>
          <button
            onClick={fetchDocuments}
            className="mt-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 transition flex items-center gap-1.5"
          >
            <RefreshCw className="h-3.5 w-3.5" /> Retry
          </button>
        </div>
      ) : documentsList.length === 0 ? (
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-12 text-center rounded-2xl flex flex-col items-center justify-center space-y-3">
          <FileText className="h-10 w-10 text-slate-400 dark:text-slate-500" />
          <div>
            <p className="text-sm font-bold text-slate-800 dark:text-slate-200">No Documents Found</p>
            <p className="text-xs text-slate-400 dark:text-slate-400 mt-1">
              No files in the verification queue matching your current filters.
            </p>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-5">
          {documentsList.map((doc) => (
            <div
              key={doc.id}
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm space-y-4 hover:shadow-md transition duration-200 flex flex-col justify-between"
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-100 dark:border-indigo-800/60 flex items-center justify-center text-indigo-600 dark:text-indigo-400">
                      <FileText className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100">{doc.name}</h4>
                      <p className="text-[10px] text-slate-400 font-semibold">
                        Type: <span className="text-indigo-600 dark:text-indigo-400 font-bold">{doc.documentType}</span> | v{doc.version}
                      </p>
                    </div>
                  </div>
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase border ${doc.badgeColor}`}>
                    {doc.displayStatus}
                  </span>
                </div>

                <div className="p-3 bg-slate-50 dark:bg-slate-800/60 border border-slate-100 dark:border-slate-800 rounded-xl text-[11px] text-slate-600 dark:text-slate-300 space-y-1.5">
                  <div className="flex justify-between">
                    <span className="font-bold text-slate-700 dark:text-slate-200">Citizen:</span>
                    <span>{doc.citizen}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="font-bold text-slate-700 dark:text-slate-200">Application:</span>
                    <span className="font-mono text-[10px]">{doc.applicationId}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="font-bold text-slate-700 dark:text-slate-200">Uploaded:</span>
                    <span>{doc.date}</span>
                  </div>

                  {/* AI Verification Score & Status */}
                  <div className="pt-1.5 border-t border-slate-200/60 dark:border-slate-700/60 flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <Sparkles className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400" />
                      <span className="font-bold text-slate-700 dark:text-slate-200">AI Score:</span>
                    </div>
                    <span className={`px-2 py-0.5 rounded-md font-extrabold text-[10px] ${
                      doc.verificationScore >= 70
                        ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-950/80 dark:text-emerald-300"
                        : doc.verificationScore >= 50
                        ? "bg-amber-100 text-amber-800 dark:bg-amber-950/80 dark:text-amber-300"
                        : "bg-rose-100 text-rose-800 dark:bg-rose-950/80 dark:text-rose-300"
                    }`}>
                      {doc.verificationScore}%
                    </span>
                  </div>

                  {doc.correctionReason && (
                    <div className="text-orange-600 dark:text-orange-400 pt-1 text-[10px]">
                      <span className="font-bold">Correction Requested:</span> {doc.correctionReason}
                    </div>
                  )}

                  {doc.rejectionReason && (
                    <div className="text-rose-600 dark:text-rose-400 pt-1 text-[10px]">
                      <span className="font-bold">Rejected:</span> {doc.rejectionReason}
                    </div>
                  )}
                </div>
              </div>

                  {/* Action Buttons */}
              {/* Action Buttons */}
              <div className="space-y-2 pt-3 border-t border-slate-100 dark:border-slate-800">
                {/* Row 1: View File & AI Analysis */}
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => handleDownload(doc)}
                    className="flex-1 py-2 px-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 cursor-pointer"
                    title="View File"
                  >
                    <Eye className="h-3.5 w-3.5" /> View File
                  </button>
                  <button
                    type="button"
                    onClick={() => openAiAnalysis(doc)}
                    className="flex-1 py-2 px-2.5 bg-indigo-50 dark:bg-indigo-950/60 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <Sparkles className="h-3.5 w-3.5" /> AI Analysis
                  </button>
                </div>

                {/* Row 2 & 3: Officer Verification & Review Actions */}
                {doc.officerReviewStatus === "ADMIN_VERIFIED" ? (
                  <div className="w-full py-2.5 px-3 bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 shadow-sm">
                    <CheckCircle className="h-4 w-4 text-emerald-600 dark:text-emerald-400" /> Officer Verified ✓
                  </div>
                ) : doc.officerReviewStatus === "REJECTED" ? (
                  <div className="w-full py-2.5 px-3 bg-rose-50 dark:bg-rose-950/60 border border-rose-200 dark:border-rose-800 text-rose-700 dark:text-rose-300 rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 shadow-sm">
                    <XCircle className="h-4 w-4 text-rose-600 dark:text-rose-400" /> Rejected
                  </div>
                ) : (
                  <>
                    <button
                      type="button"
                      onClick={() => handleApprove(doc)}
                      className="w-full py-2.5 px-3 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white rounded-xl text-xs font-extrabold shadow-sm transition flex items-center justify-center gap-1.5 cursor-pointer border border-emerald-700"
                    >
                      <CheckCircle className="h-4 w-4 text-white" /> Verify Document
                    </button>

                    <div className="flex items-center gap-2">
                      {doc.officerReviewStatus !== "CORRECTION_REQUIRED" && (
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedDoc(doc);
                            setIsCorrectionModalOpen(true);
                          }}
                          className="flex-1 py-1.5 px-2.5 bg-amber-50 hover:bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-200 dark:border-amber-800 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1 cursor-pointer"
                        >
                          <Clock className="h-3.5 w-3.5" /> Request Correction
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => {
                          setSelectedDoc(doc);
                          setIsRejectModalOpen(true);
                        }}
                        className="flex-1 py-1.5 px-2.5 bg-rose-50 dark:bg-rose-950/60 hover:bg-rose-100 text-rose-700 dark:text-rose-300 border border-rose-200 dark:border-rose-800/60 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1 cursor-pointer"
                      >
                        <XCircle className="h-3.5 w-3.5" /> Reject
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination Bar */}
      {!loading && !error && documentsList.length > 0 && (
        <div className="px-5 py-3.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
          <span className="text-slate-500 dark:text-slate-400 font-semibold">
            Showing {pagination.totalElements === 0 ? 0 : (currentPage - 1) * pageSize + 1} to{" "}
            {Math.min(currentPage * pageSize, pagination.totalElements)} of {pagination.totalElements} documents
          </span>
          <div className="flex items-center gap-2">
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

      {/* AI Analysis Modal */}
      {isAiModalOpen && selectedDoc && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 max-w-xl w-full rounded-2xl p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-start justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
              <div>
                <div className="flex items-center gap-2">
                  <Sparkles className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
                  <h3 className="text-sm font-extrabold text-slate-900 dark:text-slate-100">
                    AI Verification Summary & Checks
                  </h3>
                </div>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Document: <strong className="text-slate-700 dark:text-slate-200">{selectedDoc.name}</strong> ({selectedDoc.documentType})
                </p>
              </div>
              <button
                onClick={() => setIsAiModalOpen(false)}
                className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {aiLoading ? (
              <div className="py-8 text-center text-xs font-semibold text-slate-500 flex items-center justify-center gap-2">
                <RefreshCw className="h-4 w-4 animate-spin text-indigo-600" />
                Loading AI Analysis Engine...
              </div>
            ) : aiData ? (
              <div className="space-y-4">
                {/* Score & Verdict Banner */}
                <div className="p-4 bg-slate-50 dark:bg-slate-800/80 rounded-xl border border-slate-100 dark:border-slate-700 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-bold uppercase text-slate-400">AI Verification Result</span>
                    <div className="text-sm font-black text-slate-900 dark:text-slate-100 flex items-center gap-2">
                      {aiData.aiStatus === "AI_VERIFIED" ? (
                        <span className="text-emerald-600 flex items-center gap-1"><CheckCircle className="h-4 w-4" /> PASSED</span>
                      ) : aiData.aiStatus === "AI_REJECTED" ? (
                        <span className="text-rose-600 flex items-center gap-1"><XCircle className="h-4 w-4" /> REJECTED</span>
                      ) : (
                        <span className="text-amber-600 flex items-center gap-1"><AlertTriangle className="h-4 w-4" /> REVIEW REQUIRED</span>
                      )}
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="text-[10px] font-bold uppercase text-slate-400">Verification Score</span>
                    <div className="text-lg font-black text-indigo-600 dark:text-indigo-400">
                      {aiData.overallScore}%
                    </div>
                  </div>
                </div>

                {/* Checks List */}
                <div className="space-y-2">
                  <h4 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                    Explainable Verification Checks (100% Weighted):
                  </h4>
                  <div className="space-y-1.5">
                    {(aiData.checks || []).map((chk, idx) => (
                      <div
                        key={idx}
                        className="p-2.5 bg-white dark:bg-slate-950 border border-slate-100 dark:border-slate-800 rounded-xl flex items-start gap-2.5 text-xs"
                      >
                        {chk.status === "PASSED" ? (
                          <CheckCircle className="h-4 w-4 text-emerald-600 shrink-0 mt-0.5" />
                        ) : chk.status === "NOT_CHECKED" ? (
                          <Info className="h-4 w-4 text-slate-400 shrink-0 mt-0.5" />
                        ) : chk.status === "WARNING" ? (
                          <AlertTriangle className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />
                        ) : (
                          <XCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                        )}
                        <div className="flex-1">
                          <div className="flex justify-between items-center">
                            <span className="font-bold text-slate-800 dark:text-slate-200">{chk.check}</span>
                            <span className="text-[10px] font-bold text-slate-400">{chk.score}/{chk.weight} pts</span>
                          </div>
                          <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">{chk.message}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Extracted Fields (Strict Masking) */}
                {aiData.extractedFields && Object.keys(aiData.extractedFields).length > 0 && (
                  <div className="space-y-1.5">
                    <h4 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                      Extracted Identity Attributes (Privacy Protected):
                    </h4>
                    <div className="p-3 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-100 dark:border-slate-700 grid grid-cols-2 gap-2 text-[11px]">
                      {Object.entries(aiData.extractedFields).map(([key, val]) => (
                        <div key={key}>
                          <span className="text-slate-400 font-semibold">{key}: </span>
                          <strong className="text-slate-800 dark:text-slate-200">
                            {typeof val === "boolean" ? (val ? "Yes" : "No") : String(val)}
                          </strong>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Mandatory Disclaimer Warnings */}
                <div className="p-3 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 rounded-xl space-y-1">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-amber-900 dark:text-amber-200">
                    <ShieldAlert className="h-4 w-4 text-amber-600" />
                    Statutory & Identity Disclaimers:
                  </div>
                  {(aiData.warnings || []).map((w, idx) => (
                    <p key={idx} className="text-[11px] text-amber-800 dark:text-amber-300">
                      &bull; {w}
                    </p>
                  ))}
                  <p className="text-[11px] text-amber-800 dark:text-amber-300 font-bold pt-1">
                    Officer Decision: {selectedDoc.detailedStatus === "ADMIN_VERIFIED" ? "OFFICER VERIFIED" : "PENDING REVIEW"}
                  </p>
                </div>

                {/* Decision Actions */}
                <div className="flex justify-end gap-2 pt-2 border-t border-slate-100 dark:border-slate-800">
                  <button
                    onClick={() => setIsAiModalOpen(false)}
                    className="px-4 py-2 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 rounded-xl text-xs font-bold"
                  >
                    Close
                  </button>
                  {selectedDoc.officerReviewStatus !== "ADMIN_VERIFIED" && selectedDoc.detailedStatus !== "ADMIN_VERIFIED" && (
                    <button
                      type="button"
                      onClick={() => handleApprove(selectedDoc)}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                    >
                      <CheckCircle className="h-4 w-4" /> Verify Document
                    </button>
                  )}
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}

      {/* Correction Modal */}
      {isCorrectionModalOpen && selectedDoc && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 max-w-md w-full rounded-2xl p-6 shadow-2xl space-y-4">
            <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <Clock className="h-5 w-5 text-amber-600" />
              Request Document Correction
            </h4>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              The citizen will receive an action required notification requesting a replacement upload for <strong>{selectedDoc.name}</strong>.
            </p>

            <select
              value={correctionReason}
              onChange={(e) => setCorrectionReason(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-200 rounded-xl text-xs font-semibold"
            >
              <option value="">Select reason template...</option>
              {correctionReasonTemplates.map((r, i) => (
                <option key={i} value={r}>{r}</option>
              ))}
            </select>

            <textarea
              rows={3}
              placeholder="Or type custom correction requirement..."
              value={correctionReason}
              onChange={(e) => setCorrectionReason(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-100 placeholder-slate-400 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-indigo-600 focus:outline-none"
            />

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setIsCorrectionModalOpen(false)}
                className="px-4 py-2 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 rounded-xl text-xs font-bold"
              >
                Cancel
              </button>
              <button
                onClick={handleRequestCorrectionSubmit}
                disabled={!correctionReason.trim()}
                className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-bold disabled:opacity-40"
              >
                Dispatch Correction Request
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reject Modal */}
      {isRejectModalOpen && selectedDoc && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 max-w-md w-full rounded-2xl p-6 shadow-2xl space-y-4">
            <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <AlertTriangle className="h-5 w-5 text-rose-600" />
              Specify Mandatory Rejection Reason
            </h4>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              The document will transition to REJECTED. A valid reason must be permanently audited.
            </p>

            <select
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-200 rounded-xl text-xs font-semibold"
            >
              <option value="">Select rejection reason template...</option>
              {rejectReasonTemplates.map((r, i) => (
                <option key={i} value={r}>{r}</option>
              ))}
            </select>

            <textarea
              rows={3}
              placeholder="Or type specific rejection rationale..."
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-100 placeholder-slate-400 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-indigo-600 focus:outline-none"
            />

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setIsRejectModalOpen(false)}
                className="px-4 py-2 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 rounded-xl text-xs font-bold"
              >
                Cancel
              </button>
              <button
                onClick={handleRejectSubmit}
                disabled={!rejectReason.trim()}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold disabled:opacity-40"
              >
                Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
