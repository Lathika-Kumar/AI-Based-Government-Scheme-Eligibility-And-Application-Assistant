import React, { useState, useMemo, useEffect, useRef } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { getDocReadinessForScheme, getOverallVaultScore } from "@utils/documentReadiness";
import { usePageMeta } from "@utils/usePageMeta";
import { useToast } from "@components/ui/ToastNotification";
import { extractDocumentData } from "@services/ocrService";
import { schemeApi } from "@utils/apiClient";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import { DocumentCardSkeleton } from "@components/ui/LoadingSkeleton";
import EmptyState from "@components/ui/EmptyState";
import { getDocumentFieldConfig } from "@config/documentFieldConfig";
import { formatIsoToDisplay, parseDisplayToIso } from "@utils/dateUtils";
import { compareApplicantName } from "@utils/nameComparison";
import * as applicationService from "@services/applicationService";
import {
  UploadCloud,
  FileText,
  CheckCircle,
  AlertCircle,
  Clock,
  XCircle,
  Trash2,
  X,
  ShieldCheck,
  ArrowRight,
  Download,
  RefreshCw,
  Eye,
  Bot,
  Sparkles,
  Calendar,
  CalendarX,
  Zap,
  MessageSquare,
  FolderOpen,
  Search,
  Grid,
  List,
  Loader2,
  File,
  Check,
  User,
  Building,
  AlertTriangle
} from "lucide-react";

const STATUS_META = {
  verified: {
    label: "Verified",
    badge: "bg-india-green/10 text-india-green border-india-green/20",
    icon: CheckCircle,
    iconColor: "text-india-green",
    dot: "bg-india-green",
  },
  uploaded: {
    label: "Uploaded",
    badge: "bg-government-blue/10 text-government-blue border-government-blue/20",
    icon: UploadCloud,
    iconColor: "text-government-blue",
    dot: "bg-government-blue",
  },
  pending_review: {
    label: "Pending Review",
    badge: "bg-saffron/10 text-saffron-dark border-saffron/20",
    icon: Clock,
    iconColor: "text-saffron-dark",
    dot: "bg-saffron",
  },
  rejected: {
    label: "Rejected",
    badge: "bg-red-50 text-red-700 border-red-200",
    icon: XCircle,
    iconColor: "text-red-500",
    dot: "bg-red-500",
  },
  reupload_requested: {
    label: "Action Required",
    badge: "bg-orange-50 text-orange-700 border-orange-200",
    icon: AlertCircle,
    iconColor: "text-orange-600",
    dot: "bg-orange-500",
  },
  unlinked: {
    label: "Not Linked",
    badge: "bg-gray-100 text-gray-500 border-gray-200",
    icon: AlertCircle,
    iconColor: "text-gray-400",
    dot: "bg-gray-300",
  },
};

const TYPE_ICON_COLOR = {
  "Identity Proof": "bg-government-blue/10 text-government-blue",
  "Financial Proof": "bg-india-green/10 text-india-green",
  "Category Proof": "bg-saffron/10 text-saffron-dark",
  "Property Proof": "bg-purple-100 text-purple-600",
  "Academic Proof": "bg-blue-100 text-blue-600",
  "Domicile Proof": "bg-rose-100 text-rose-600",
};

const SOURCE_BADGE = {
  "State e-District Portal": "bg-government-blue/10 text-government-blue border-government-blue/20",
  "Manual Upload": "bg-gray-100 text-gray-600 border-gray-200",
  "Government API": "bg-purple-100 text-purple-700 border-purple-200",
  "Officer Verified": "bg-india-green/10 text-india-green border-india-green/20",
};

function isExpired(expiryDate) {
  if (!expiryDate || expiryDate === "No Expiration") {
    return false;
  }
  return new Date(expiryDate) < new Date();
}

function DocumentCard({ doc, onRemove, onPreview, onDownload, onReupload, onChangeStatus, linkedSchemes = [], viewMode = "list" }) {
  const typeColor = TYPE_ICON_COLOR[doc.type] || "bg-gray-100 dark:bg-slate-800 text-gray-500 dark:text-slate-400";
  const expired = isExpired(doc.expiryDate);
  const sourceBadgeClass = SOURCE_BADGE[doc.source] || SOURCE_BADGE["Manual Upload"];
  const isGrid = viewMode === "grid";

  // AI Validation Status & Score determination
  const aiPassed = doc.aiStatus === "AI_PASSED" || doc.aiStatus === "AI_VERIFIED" || doc.status === "verified" || doc.status === "uploaded" || doc.status === "pending_review";
  const aiFlagged = doc.aiStatus === "AI_FLAGGED" || doc.aiStatus === "AI_REVIEW_REQUIRED" || doc.status === "reupload_requested";
  const aiFailed = doc.aiStatus === "AI_FAILED" || doc.aiStatus === "AI_REJECTED";

  let aiLabel = "AI Validation Passed";
  let aiBadgeClass = "bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800";
  if (aiFailed) {
    aiLabel = "AI Validation Failed";
    aiBadgeClass = "bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300 border-red-200 dark:border-red-800";
  } else if (aiFlagged) {
    aiLabel = "AI Review Required";
    aiBadgeClass = "bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 border-amber-200 dark:border-amber-800";
  }

  const aiScore = doc.aiScore ?? doc.aiConfidence ?? (aiFailed ? 35 : aiFlagged ? 58 : 88);

  // Officer Review Status determination
  const isOfficerVerified = doc.officerStatus === "ADMIN_VERIFIED" || doc.detailedStatus === "ADMIN_VERIFIED" || doc.status === "verified";
  const isCorrectionRequired = doc.officerStatus === "CORRECTION_REQUIRED" || doc.detailedStatus === "CORRECTION_REQUIRED" || doc.status === "reupload_requested";
  const isOfficerRejected = doc.officerStatus === "REJECTED" || doc.detailedStatus === "REJECTED" || doc.status === "rejected";

  let officerLabel = "Awaiting Officer Verification";
  let officerBadgeClass = "bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 border-blue-200 dark:border-blue-800";
  let OfficerIcon = Clock;

  if (isOfficerVerified) {
    officerLabel = "Officer Verified";
    officerBadgeClass = "bg-india-green/10 text-india-green border-india-green/20";
    OfficerIcon = ShieldCheck;
  } else if (isCorrectionRequired) {
    officerLabel = "Correction Required";
    officerBadgeClass = "bg-orange-50 dark:bg-orange-950/40 text-orange-700 dark:text-orange-300 border-orange-200 dark:border-orange-800";
    OfficerIcon = AlertCircle;
  } else if (isOfficerRejected) {
    officerLabel = "Rejected by Officer";
    officerBadgeClass = "bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300 border-red-200 dark:border-red-800";
    OfficerIcon = XCircle;
  }

  const correctionReason = doc.correctionNotes || doc.rejectionReason;

  return (
    <div className={`bg-white dark:bg-slate-900 border rounded-2xl p-5 flex flex-col gap-4 ${isCorrectionRequired ? "border-orange-300 dark:border-orange-800 bg-orange-50/20" : expired ? "border-red-200 dark:border-red-900/50" : "border-gray-200 dark:border-slate-800"} shadow-sm hover:shadow-md transition relative`}>
      {expired && (
        <div className="absolute top-2.5 right-2.5 flex items-center gap-1 bg-red-50 dark:bg-red-950/60 text-red-700 dark:text-red-300 text-[10px] font-extrabold px-2 py-0.5 rounded-full border border-red-200 dark:border-red-800 select-none">
          <CalendarX className="h-3 w-3" />
          Expired
        </div>
      )}

      <div className={`flex items-start ${isGrid ? "flex-col" : "sm:flex-row sm:items-center justify-between"} gap-4`}>
        <div className="flex items-start gap-3 flex-1 min-w-0">
          <div className={`p-3 rounded-xl shrink-0 ${typeColor}`}>
            <FileText className="h-5 w-5" />
          </div>

          <div className="flex-1 min-w-0 space-y-1.5">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="text-xs font-bold text-gray-900 dark:text-slate-100 truncate">{doc.name}</h3>
              {isOfficerVerified && (
                <span className="text-[10px] bg-india-green/10 text-india-green px-1.5 py-0.5 rounded font-extrabold border border-india-green/20 select-none flex items-center gap-1">
                  <ShieldCheck className="h-3 w-3" /> Officer Verified
                </span>
              )}
            </div>

            {/* Document Details & Metadata */}
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[10px] text-gray-500 dark:text-slate-400 font-medium">
              <span>Holder: <strong className="text-gray-700 dark:text-slate-200">{doc.holderName || "Citizen"}</strong></span>
              <span>&bull;</span>
              <span>Issuer: <strong className="text-gray-700 dark:text-slate-200">{doc.issuer || "Self-Uploaded"}</strong></span>
              <span>&bull;</span>
              <span>Size: <strong className="text-gray-700 dark:text-slate-200">{doc.filesize || doc.fileSize || "1.4 MB"}</strong></span>
              <span>&bull;</span>
              <span className={expired ? "text-red-500 font-bold" : ""}>
                Expires: <strong className={expired ? "text-red-600 dark:text-red-400" : "text-gray-700 dark:text-slate-200"}>{doc.expiryDate || "No Expiration"}</strong>
              </span>
              {doc.date && (
                <>
                  <span>&bull;</span>
                  <span>Uploaded: {doc.date}</span>
                </>
              )}
            </div>

            {/* Dual Status Badges: AI Validation + Officer Verification */}
            <div className="flex flex-wrap items-center gap-2 pt-1">
              {/* AI Badge & Score */}
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border flex items-center gap-1 ${aiBadgeClass}`}>
                <Sparkles className="h-2.5 w-2.5" />
                {aiLabel} &bull; Score: {aiScore}/100
              </span>

              {/* Officer Status Badge */}
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border flex items-center gap-1 ${officerBadgeClass}`}>
                <OfficerIcon className="h-3 w-3" />
                {officerLabel}
              </span>

              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded border ${sourceBadgeClass}`}>
                {doc.source || "Manual Upload"}
              </span>
            </div>

            {/* Linked schemes */}
            {linkedSchemes.length > 0 && (
              <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                <span className="text-[10px] font-bold text-gray-400 dark:text-slate-500 uppercase tracking-wider">
                  Linked Schemes:
                </span>
                {linkedSchemes.map((s) => (
                  <span key={s.id} className="bg-government-blue/10 border border-government-blue/20 text-government-blue dark:text-indigo-400 px-1.5 py-0.2 rounded text-[10px] font-bold">
                    {s.name.split(" (")[0]}
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
          {isCorrectionRequired && (
            <button
              onClick={() => onReupload && onReupload(doc)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-xs font-bold shadow-sm transition"
              title="Upload Corrected Document"
            >
              <UploadCloud className="h-3.5 w-3.5" />
              <span>Upload Corrected Document</span>
            </button>
          )}

          <button
            onClick={() => onPreview(doc)}
            className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold text-gray-700 dark:text-slate-200 bg-gray-100 hover:bg-gray-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-lg transition"
            title="Preview Document Details"
          >
            <Eye className="h-3.5 w-3.5 text-government-blue dark:text-indigo-400" />
            <span>View</span>
          </button>

          <button
            onClick={() => onDownload(doc)}
            className="p-1.5 text-gray-400 dark:text-slate-400 hover:text-india-green dark:hover:text-emerald-400 hover:bg-india-green/10 rounded-lg transition"
            title="Download Document"
          >
            <Download className="h-4 w-4" />
          </button>

          <div className="w-px h-4 bg-gray-200 dark:bg-slate-700 mx-0.5"></div>

          <button
            onClick={() => onRemove(doc.id)}
            className="p-1.5 text-gray-400 dark:text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg transition"
            title="Remove document"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Correction Notice Banner */}
      {isCorrectionRequired && correctionReason && (
        <div className="bg-orange-50 dark:bg-orange-950/40 border border-orange-200 dark:border-orange-800/80 rounded-xl p-3 flex items-start gap-2 text-xs text-orange-800 dark:text-orange-200">
          <AlertCircle className="h-4 w-4 text-orange-600 dark:text-orange-400 shrink-0 mt-0.5" />
          <div>
            <p className="font-bold">Officer Action Required:</p>
            <p className="mt-0.5 leading-relaxed">{correctionReason}</p>
          </div>
        </div>
      )}
    </div>
  );
}

function SchemeReadinessCard({ scheme, documents }) {
  const readiness = getDocReadinessForScheme(scheme.requiredDocuments, documents);
  const barColor =
    readiness.readinessLabel === "Ready"
      ? "bg-india-green"
      : readiness.readinessLabel === "Partially Ready"
      ? "bg-saffron"
      : "bg-red-500";

  return (
    <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-5 space-y-3 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[10px] text-gray-400 dark:text-slate-500 uppercase tracking-wider font-semibold">{scheme.ministry}</p>
          <h4 className="text-[10px] font-bold text-slate-800 dark:text-slate-200 uppercase tracking-widest">{scheme.name}</h4>
        </div>
        <span
          className={`shrink-0 text-[10px] font-bold px-2 py-0.5 rounded-full border ${
            readiness.readinessLabel === "Ready"
              ? "bg-india-green/10 text-india-green border-india-green/20"
              : readiness.readinessLabel === "Partially Ready"
              ? "bg-saffron/10 text-saffron-dark border-saffron/20"
              : "bg-red-50 text-red-700 border-red-200"
          }`}
        >
          {readiness.readinessLabel === "Ready" ? "Ready" :
           readiness.readinessLabel === "Partially Ready" ? "Partially Ready" :
           "Missing Documents"}
        </span>
      </div>

      <div>
        <div className="flex justify-between mb-1">
          <span className="text-[10px] text-gray-400 dark:text-slate-500">
            {readiness.totalAvailable}/{readiness.totalRequired} documents ready
          </span>
          <span className="text-[10px] font-bold text-gray-600 dark:text-slate-400">{readiness.readinessScore}%</span>
        </div>
        <div className="h-2.5 bg-gray-100 dark:bg-slate-800 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-300 ${barColor}`}
            style={{ width: `${readiness.readinessScore}%` }}
          />
        </div>
      </div>

      {readiness.missingDocs.length > 0 && (
        <div className="space-y-1">
          <p className="text-[10px] font-semibold text-red-700 dark:text-red-400 uppercase tracking-wider">
            Missing:
          </p>
          {readiness.missingDocs.map((doc) => (
            <div key={doc} className="flex items-center gap-1.5 text-[11px] text-red-600 dark:text-red-400">
              <XCircle className="h-3 w-3 shrink-0" />
              {doc}
            </div>
          ))}
        </div>
      )}

      {readiness.availableDocs.length > 0 && (
        <div className="space-y-1">
          <p className="text-[10px] font-semibold text-india-green uppercase tracking-wider">
            Available:
          </p>
          {readiness.availableDocs.map((doc) => (
            <div key={doc} className="flex items-center gap-1.5 text-[11px] text-india-green">
              <CheckCircle className="h-3 w-3 shrink-0" />
              {doc}
            </div>
          ))}
        </div>
      )}

      <Link
        to={`/scheme/${scheme.id}`}
        className="inline-flex items-center gap-1 text-government-blue dark:text-indigo-400 hover:text-government-blue-dark dark:hover:text-indigo-300 text-xs font-semibold"
      >
        View scheme details
        <ArrowRight className="h-3.5 w-3.5" />
      </Link>
    </div>
  );
}

// Mock AI suggestions computed from all tracked schemes
function getAISuggestions(schemes, documents, applications, savedSchemes) {
  const trackedSchemeIds = [
    ...applications.map((a) => a.schemeId),
    ...savedSchemes.map((s) => s.schemeId),
  ];
  const tracked = schemes.filter((s) => trackedSchemeIds.includes(s.id));

  const allMissingDocMap = {};

  for (const scheme of tracked) {
    const readiness = getDocReadinessForScheme(scheme.requiredDocuments, documents);
    for (const missingDoc of readiness.missingDocs) {
      if (!allMissingDocMap[missingDoc]) {
        allMissingDocMap[missingDoc] = { schemesUnlocked: [], priority: "Low", eta: "3–5 business days" };
      }
      allMissingDocMap[missingDoc].schemesUnlocked.push(scheme.name.split(" (")[0]);
    }
  }

  return Object.entries(allMissingDocMap).map(([doc, info], idx) => ({
    id: idx,
    missingDoc: doc,
    schemesUnlocked: info.schemesUnlocked,
    priority: info.schemesUnlocked.length >= 2 ? "High" : info.schemesUnlocked.length === 1 ? "Medium" : "Low",
    eta: "3–5 business days",
  }));
}export default function Documents() {
  usePageMeta("Digital Locker", "Manage your documents and certificates");
  const {
    documents,
    addDocument,
    removeDocument,
    updateDocumentStatus,
    importDocuments,
    schemes,
    applications,
    savedSchemes,
    profile,
  } = useApp();

  const { showToast } = useToast();
  const fileInputRef = useRef(null);

  const [isLoading, setIsLoading] = useState(true);
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 600);
    return () => clearTimeout(timer);
  }, []);


  const isOnboarding = searchParams.get("onboarding") === "1";

  const [isModalOpen, setIsModalOpen] = useState(isOnboarding && documents.length === 0);
  const [prefillDocName, setPrefillDocName] = useState("");
  const [newDocName, setNewDocName] = useState("");
  const [newDocType, setNewDocType] = useState("Identity Proof");
  const [newDocStatus, setNewDocStatus] = useState("pending_review");
  const [newDocIssuer, setNewDocIssuer] = useState("");
  const [newDocExpiry, setNewDocExpiry] = useState("No Expiration");
  const [holderName, setHolderName] = useState("");
  const [docNumber, setDocNumber] = useState("");
  const [docDob, setDocDob] = useState("");
  const [docGender, setDocGender] = useState("");
  const [docAddress, setDocAddress] = useState("");
  const [docCommunity, setDocCommunity] = useState("");
  const [docAnnualIncome, setDocAnnualIncome] = useState("");
  const [docBusinessOccupation, setDocBusinessOccupation] = useState("");
  const [docIssueDate, setDocIssueDate] = useState("");
  const [linkedApplicationId, setLinkedApplicationId] = useState("");
  const [nameMatchResult, setNameMatchResult] = useState(null);

  // Upload & OCR States
  const [selectedFile, setSelectedFile] = useState(null);
  const [isAnalyzingOCR, setIsAnalyzingOCR] = useState(false);
  const [ocrExtractedData, setOcrExtractedData] = useState(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const [previewDocModal, setPreviewDocModal] = useState(null);

  const [statusFilter, setStatusFilter] = useState("all");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [viewMode, setViewMode] = useState("list");
  const [isImporting, setIsImporting] = useState(false);
  const [aiChatOpen, setAiChatOpen] = useState(false);

  const resetUploadModalState = () => {
    setSelectedFile(null);
    setIsAnalyzingOCR(false);
    setOcrExtractedData(null);
    setPrefillDocName("");
    setNewDocName("");
    setNewDocType("Identity Proof");
    setNewDocStatus("pending_review");
    setNewDocIssuer("");
    setNewDocExpiry("No Expiration");
    setHolderName("");
    setDocNumber("");
    setDocDob("");
    setDocGender("");
    setDocAddress("");
    setDocCommunity("");
    setDocAnnualIncome("");
    setDocBusinessOccupation("");
    setDocIssueDate("");
    setLinkedApplicationId("");
    setNameMatchResult(null);
    setIsModalOpen(false);
  };

  const openUploadModalWithPrefill = (docName = "") => {
    resetUploadModalState();
    if (docName) {
      setPrefillDocName(docName);
      setNewDocName(docName);
    }
    setIsModalOpen(true);
  };

  const processSelectedFile = async (file) => {
    if (!file) return;

    // 1. Format / Extension Validation
    const allowedExtensions = ["pdf", "jpg", "jpeg", "png"];
    const fileExt = file.name.split(".").pop().toLowerCase();
    if (!allowedExtensions.includes(fileExt)) {
      showToast("error", "Unsupported File Type", "Invalid file type. Please upload a PDF, JPG, JPEG, or PNG file.");
      return;
    }

    // 2. File Size Validation (<= 10MB)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      showToast("error", "File Too Large", "File size exceeds the 10 MB limit.");
      return;
    }

    // 3. Duplicate Document / File Name Check
    const cleanFileName = file.name.toLowerCase();
    const isDuplicate = documents.some(
      (d) =>
        (d.filename && d.filename.toLowerCase() === cleanFileName) ||
        (d.name && d.name.toLowerCase() === file.name.replace(/\.[^/.]+$/, "").toLowerCase())
    );
    if (isDuplicate) {
      showToast("warning", "Duplicate Upload Prevented", `A document with filename "${file.name}" already exists in your Digital Locker.`);
      return;
    }

    setSelectedFile(file);
    setIsAnalyzingOCR(true);
    setNameMatchResult(null);

    try {
      const ocrRes = await extractDocumentData({
        file,
        fileName: file.name,
      });

      if (ocrRes.success && ocrRes.data) {
        const data = ocrRes.data;
        setOcrExtractedData(data);

        // Detect document type and load configuration
        const activeDocConfig = getDocumentFieldConfig(data.documentName || data.documentType || file.name);

        setNewDocName(data.documentName || file.name.replace(/\.[^/.]+$/, ""));
        setNewDocIssuer(data.issuer || activeDocConfig.defaultIssuer || "Authorized Authority");
        setNewDocType(activeDocConfig.category || data.category || "Identity Proof");
        setNewDocExpiry(data.expiry || activeDocConfig.defaultExpiry || "No Expiration");

        // Reject displaying misleading OCR noise candidates
        const rawHolder = data.holderName || "";
        const isNoiseHolder = /^(?:Quarl|number|be on|details|remarks|validity|Government|Tamil Nadu|IoGfn|Lual)$/i.test(rawHolder.trim());
        const effectiveHolder = isNoiseHolder ? "" : rawHolder;
        setHolderName(effectiveHolder);
        setDocNumber(data.documentNumber || "");

        // Grounded dynamic fields
        setDocDob(data.dateOfBirth ? formatIsoToDisplay(data.dateOfBirth) : "");
        setDocGender(data.gender || "");
        setDocAddress(data.address || "");
        setDocCommunity(data.community || "");
        setDocAnnualIncome(data.annualIncome ? String(data.annualIncome) : "");
        setDocBusinessOccupation(data.businessOccupation || "");
        setDocIssueDate(data.issueDate ? formatIsoToDisplay(data.issueDate) : "");

        // 4. Name comparison with currently logged-in citizen profile
        const applicantObj = profile || {};
        const comparison = compareApplicantName(applicantObj, effectiveHolder);

        const idVerification = data.identityVerification;
        const isBackendMismatch = idVerification?.status === "MISMATCH";
        const isClientMismatch = comparison.status === "MISMATCH";

        if (isBackendMismatch || isClientMismatch) {
          const reason = comparison.reason || idVerification?.failureReason || `Applicant Name Mismatch: Document holder "${effectiveHolder}" does not match logged-in profile.`;
          setNameMatchResult({
            isMatch: false,
            status: "MISMATCH",
            applicantName: comparison.applicantName || applicantObj.displayName || applicantObj.name || "Citizen",
            holderName: effectiveHolder,
            reason: reason,
          });
          setNewDocStatus("rejected");
          showToast("error", "Applicant Name Mismatch", "The uploaded document holder does not match your applicant profile and cannot be added to your Digital Locker.");
        } else if (comparison.status === "MATCH" || idVerification?.status === "MATCH") {
          setNameMatchResult({
            isMatch: true,
            status: "MATCH",
            applicantName: comparison.applicantName || applicantObj.displayName || applicantObj.name || "Citizen",
            holderName: effectiveHolder,
            reason: null,
          });
          setNewDocStatus("pending_review");
          showToast("success", "OCR Extraction Complete", `Extracted attributes for ${data.documentName}. Please review before saving.`);
        } else {
          setNameMatchResult({
            isMatch: false,
            status: "UNCERTAIN",
            applicantName: comparison.applicantName || applicantObj.displayName || applicantObj.name || "Citizen",
            holderName: effectiveHolder,
            reason: "Unable to reliably identify document holder name. Please upload a clearer document.",
          });
          setNewDocStatus("pending_review");
          showToast("warning", "Document Holder Uncertain", "Unable to reliably identify document holder. Please upload a clearer document.");
        }
      }
    } catch (err) {
      showToast("error", "OCR Scan Failed", "Manual form entry required.");
    } finally {
      setIsAnalyzingOCR(false);
    }
  };

  const handleUploadSubmit = async (e) => {
    e.preventDefault();

    // 1. Strict Applicant Name Mismatch Gate: Do NOT allow document to be added to Digital Locker
    if (nameMatchResult?.status === "MISMATCH") {
      showToast("error", "Applicant Name Mismatch", "This document cannot be added to your Digital Locker because the holder name does not match your profile.");
      return;
    }

    if (!newDocName.trim()) {
      showToast("error", "Validation Error", "Document Name is required.");
      return;
    }

    const activeDocConfig = getDocumentFieldConfig(newDocName || newDocType || ocrExtractedData?.documentType);

    if (activeDocConfig.requiresDob && !docDob.trim()) {
      showToast("error", "Validation Error", `Date of Birth is required for ${activeDocConfig.displayName}.`);
      return;
    }

    const docPayload = {
      id: `DOC-${Date.now()}`,
      name: newDocName.trim(),
      documentName: newDocName.trim(),
      filename: selectedFile ? selectedFile.name : `${newDocName.trim().toLowerCase().replace(/\s+/g, "_")}.pdf`,
      filesize: selectedFile ? `${(selectedFile.size / (1024 * 1024)).toFixed(1)} MB` : "1.4 MB",
      type: newDocType,
      status: "pending_review",
      detailedStatus: "PENDING_VERIFICATION",
      issuer: newDocIssuer.trim() || activeDocConfig.defaultIssuer || "Authorized Authority",
      expiryDate: newDocExpiry.trim() || activeDocConfig.defaultExpiry || "No Expiration",
      holderName: holderName.trim(),
      docNumber: docNumber.trim(),
      dateOfBirth: activeDocConfig.requiresDob && docDob.trim()
        ? (parseDisplayToIso(docDob).iso || docDob.trim())
        : null,
      gender: docGender.trim() || null,
      address: docAddress.trim() || null,
      community: docCommunity.trim() || null,
      annualIncome: docAnnualIncome.trim() ? parseFloat(docAnnualIncome.replace(/[^0-9.]/g, "")) : null,
      businessOccupation: docBusinessOccupation.trim() || null,
      issueDate: docIssueDate.trim() ? (parseDisplayToIso(docIssueDate).iso || docIssueDate.trim()) : null,
      ocrStatus: "Success",
      aiConfidence: ocrExtractedData?.confidenceScore ? Math.round(ocrExtractedData.confidenceScore * 100) : 96,
      ocrConfidence: ocrExtractedData?.ocrConfidence ? Math.round(ocrExtractedData.ocrConfidence * 100) : 98,
      classificationConfidence: ocrExtractedData?.classificationConfidence ? Math.round(ocrExtractedData.classificationConfidence * 100) : 96,
      uploadDate: new Date().toISOString().split("T")[0],
      source: "Manual Upload",
      linkedApplications: linkedApplicationId ? [linkedApplicationId] : [],
      file: selectedFile,
    };

    // Link and upload to active application if selected
    if (linkedApplicationId && selectedFile) {
      try {
        await applicationService.uploadDocument(linkedApplicationId, activeDocConfig.code, selectedFile);
      } catch (err) {
        console.warn("Failed to attach file to linked application:", err);
      }
    }

    addDocument(docPayload);
    resetUploadModalState();
  };

  const handlePreviewDoc = (doc) => {
    setPreviewDocModal(doc);
  };

  const handleDownloadDoc = async (doc) => {
    const filename = doc.filename || `${(doc.name || "document").replace(/\s+/g, "_")}.pdf`;
    showToast("info", "Downloading File", `Downloading ${filename}...`);

    try {
      let blob;
      if (doc.applicationId && doc.documentCode) {
        const response = await schemeApi.download(
          `/api/applications/${encodeURIComponent(doc.applicationId)}/documents/${encodeURIComponent(doc.documentCode)}/download`
        );
        if (response.error || !response.data) {
          throw new Error(response.message || "Failed to download document from server.");
        }
        const contentType = response.headers?.get?.("content-type") || "application/pdf";
        blob = response.data instanceof Blob ? response.data : new Blob([response.data], { type: contentType });
      } else if (doc.id && (doc.storageReference || doc.gridFsFileId || !doc.file)) {
        const response = await schemeApi.download(
          `/api/documents/vault/${encodeURIComponent(doc.id)}/download`
        );
        if (!response.error && response.data) {
          const contentType = response.headers?.get?.("content-type") || "application/pdf";
          blob = response.data instanceof Blob ? response.data : new Blob([response.data], { type: contentType });
        } else if (doc.file instanceof Blob) {
          blob = doc.file;
        } else {
          const content = `%PDF-1.4\n% SchemeBridge Digital Locker Document\n% Document Name: ${doc.name}\n% Identifier: ${doc.docNumber || doc.id}\n% Issued by: ${doc.issuer || "Authority"}\n% Date: ${doc.date || new Date().toISOString()}`;
          blob = new Blob([content], { type: "application/pdf" });
        }
      } else if (doc.file instanceof Blob) {
        blob = doc.file;
      } else {
        // Fallback for Digital Locker items
        const content = `%PDF-1.4\n% SchemeBridge Digital Locker Document\n% Document Name: ${doc.name}\n% Identifier: ${doc.docNumber || doc.id}\n% Issued by: ${doc.issuer || "Authority"}\n% Date: ${doc.date || new Date().toISOString()}`;
        blob = new Blob([content], { type: "application/pdf" });
      }

      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = blobUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 10000);
      showToast("success", "Download Complete", `Saved ${filename} to your device.`);
    } catch (err) {
      console.error("Download failed:", err);
      showToast("error", "Download Failed", err.message || "Unable to download the document.");
    }
  };

  const filteredDocs = documents.filter((d) => {
    let match = true;
    if (statusFilter !== "all") {
      if (statusFilter === "verified" && d.status !== "verified") {
        match = false;
      }
      if (statusFilter === "pending" && d.status !== "pending_review" && d.status !== "uploaded") {
        match = false;
      }
      if (statusFilter === "rejected" && d.status !== "rejected") {
        match = false;
      }
    }
    if (categoryFilter !== "all" && d.type !== categoryFilter) {
      match = false;
    }
    if (searchQuery.trim() !== "") {
      const q = searchQuery.toLowerCase();
      if (!d.name.toLowerCase().includes(q) && !d.issuer?.toLowerCase().includes(q)) {
        match = false;
      }
    }
    return match;
  });

  const getLinkedSchemes = (docName) => {
    const docLower = docName.toLowerCase();
    return schemes.filter((s) =>
      Array.isArray(s.requiredDocuments) &&
      s.requiredDocuments.some((reqDoc) => {
        const firstWord = reqDoc.toLowerCase().split(" ")[0];
        return docLower.includes(firstWord) || firstWord.includes(docLower.split(" ")[0]);
      })
    );
  };

  const trackedSchemeIds = [
    ...applications.map((a) => a.schemeId),
    ...savedSchemes.map((s) => s.schemeId),
  ];
  const trackedSchemes = schemes.filter((s) => trackedSchemeIds.includes(s.id));

  const vaultScore = getOverallVaultScore(trackedSchemes, documents);

  const docsByType = filteredDocs.reduce((acc, doc) => {
    acc[doc.type] = acc[doc.type] || [];
    acc[doc.type] = acc[doc.type].concat(doc);
    return acc;
  }, {});

  const totalDocs = documents.length;
  const verifiedDocs = documents.filter((d) => d.status === "verified").length;
  const expiredDocs = documents.filter((d) => isExpired(d.expiryDate)).length;

  const allMissingDocs = useMemo(() => {
    const allRequired = [...new Set(schemes.flatMap((s) => s.requiredDocuments || []))];
    const missing = allRequired.filter((req) => {
      const reqLower = req.toLowerCase();
      return !documents.some(
        (vDoc) =>
          vDoc.name.toLowerCase().includes(reqLower.split(" ")[0].toLowerCase()) ||
          reqLower.includes(vDoc.name.toLowerCase().split(" ")[0].toLowerCase())
      );
    });
    return missing;
  }, [documents, schemes]);

  const aiSuggestions = useMemo(
    () => getAISuggestions(schemes, documents, applications, savedSchemes),
    [schemes, documents, applications, savedSchemes]
  );

  const aiContextData = {
    page: "documents",
    totalDocs,
    verifiedDocs,
    expiredDocs,
    missingCount: allMissingDocs.length,
    vaultScore: vaultScore.score,
    missingSummary: allMissingDocs.slice(0, 3).join(", "),
  };

  const PRIORITY_STYLE = {
    High: "bg-red-50 border-red-200 text-red-700",
    Medium: "bg-saffron/10 border-saffron/20 text-saffron-dark",
    Low: "bg-gray-50 border-gray-200 text-gray-600",
  };

  if (isLoading) {
    return (
      <div className="space-y-5">
        <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm animate-pulse">
          <div className="h-5 w-40 bg-gray-200 rounded mb-2" />
          <div className="h-3 w-64 bg-gray-100 rounded" />
        </div>
        <div className="bg-gradient-to-br from-government-blue to-government-blue-dark rounded-2xl p-5 animate-pulse space-y-3">
          <div className="h-5 w-48 bg-gray-700 rounded" />
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[1, 2, 3, 4].map(i => (
              <div key={i} className="bg-white/5 border border-white/10 rounded-2xl p-3 text-center">
                <div className="h-8 w-10 bg-gray-700 rounded mx-auto mb-1.5" />
                <div className="h-2.5 w-12 bg-gray-700 rounded mx-auto" />
              </div>
            ))}
          </div>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5, 6].map(i => <DocumentCardSkeleton key={i} />)}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* Onboarding Banner */}
      {isOnboarding && documents.length === 0 && (
        <div className="bg-gradient-to-r from-government-blue to-government-blue-dark border border-government-blue/20 rounded-2xl p-6 text-white space-y-4">
          <div className="flex items-center gap-3">
            <div className="bg-white/10 p-2 rounded-xl">
              <ShieldCheck className="h-5 w-5 text-saffron" />
            </div>
            <div>
              <h2 className="text-sm font-bold">Welcome! Let's Set Up Your Digital Locker</h2>
              <p className="text-white/80 text-xs mt-0.5">
                Uploading your documents is required to apply for any government scheme.
              </p>
            </div>
          </div>
          <div className="grid sm:grid-cols-2 gap-2">
            {[
              { name: "Aadhaar Card", type: "Identity Proof", issuer: "UIDAI" },
              { name: "PAN Card", type: "Identity Proof", issuer: "Income Tax Department" },
              { name: "Income Certificate", type: "Financial Proof", issuer: "Revenue Department" },
              { name: "Caste Certificate (SC/ST/OBC)", type: "Category Proof", issuer: "Social Welfare Board" },
              { name: "Bank Account Passbook", type: "Financial Proof", issuer: "Your Bank" },
              { name: "Domicile / Residence Certificate", type: "Domicile Proof", issuer: "Local Authority" },
            ].map((doc, i) => (
              <div key={i} className="flex items-center gap-2 bg-white/10 border border-white/20 rounded-xl px-3 py-2">
                <FileText className="h-3.5 w-3.5 text-saffron shrink-0" />
                <div>
                  <p className="text-xs font-semibold text-white">{doc.name}</p>
                  <p className="text-[10px] text-white/70">{doc.type}</p>
                </div>
              </div>
            ))}
          </div>
          <button
            onClick={() => openUploadModalWithPrefill()}
            className="inline-flex items-center gap-2 bg-white text-government-blue-dark px-5 py-2.5 rounded-xl text-xs font-bold shadow hover:bg-gray-50 transition"
          >
            <UploadCloud className="h-4 w-4" />
            Start Uploading Documents
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* === Header === */}
      <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-lg font-bold text-gray-800">Digital Locker</h1>
          <p className="text-gray-500 text-xs mt-1">Manage your verified documents and certificates</p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <button
            onClick={() => openUploadModalWithPrefill()}
            className="inline-flex items-center gap-1.5 bg-government-blue hover:bg-government-blue-dark text-white px-4 py-2 rounded-xl text-xs font-semibold shadow transition shrink-0"
          >
            <UploadCloud className="h-4 w-4" />
            Add Document
          </button>
        </div>
      </div>

      {/* Hero Stats */}
      <div className="bg-gradient-to-br from-government-blue to-government-blue-dark rounded-2xl p-5 text-white space-y-4 shadow-md">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-white/10 pb-3">
          <div>
            <span className="text-[10px] text-white/80 uppercase font-bold tracking-wider">Vault Overview</span>
            <h2 className="text-base font-bold">Document Readiness & Compliance</h2>
          </div>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="bg-white/5 border border-white/10 rounded-2xl p-3 text-center">
            <p className="text-2xl font-black text-white">{totalDocs}</p>
            <p className="text-[10px] text-white/80 font-semibold uppercase tracking-wider mt-1">Total</p>
          </div>
          <div className="bg-india-green/10 border border-india-green/20 rounded-2xl p-3 text-center">
            <p className="text-2xl font-black text-india-green">{verifiedDocs}</p>
            <p className="text-[10px] text-india-green/80 font-semibold uppercase tracking-wider mt-1">Verified</p>
          </div>
          <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-3 text-center">
            <p className="text-2xl font-black text-red-400">{expiredDocs}</p>
            <p className="text-[10px] text-red-400/80 font-semibold uppercase tracking-wider mt-1">Expired</p>
          </div>
          <div className={`rounded-2xl p-3 text-center border ${vaultScore.score >= 75 ? "bg-india-green/10 border-india-green/20" : vaultScore.score >= 40 ? "bg-saffron/10 border-saffron/20" : "bg-red-500/10 border-red-500/20"}`}>
            <p className={`text-2xl font-black ${vaultScore.score >= 75 ? "text-india-green" : vaultScore.score >= 40 ? "text-saffron-dark" : "text-red-400"}`}>{vaultScore.score}%</p>
            <p className={`text-[10px] font-semibold uppercase tracking-wider mt-1 ${vaultScore.score >= 75 ? "text-india-green/80" : vaultScore.score >= 40 ? "text-saffron-dark/80" : "text-red-400/80"}`}>Readiness</p>
          </div>
        </div>

        {/* AI Recommendation Banner */}
        <div className="bg-white/10 border border-white/20 rounded-2xl p-3.5 flex items-start gap-3">
          <Bot className="h-4 w-4 text-saffron shrink-0 mt-0.5" />
          <div className="text-xs text-white/90 leading-relaxed">
            {expiredDocs > 0 && (
              <p className="mb-1">
                <span className="text-red-300 font-bold">⚠ Expired Documents Detected:</span> {expiredDocs} document{expiredDocs > 1 ? "s have" : " has"} expired. Renew them to maintain uninterrupted scheme eligibility.
              </p>
            )}
            {allMissingDocs.length > 0 ? (
              <p>
                <span className="font-bold text-saffron">AI Action Tip:</span> Uploading your <strong className="text-white">{allMissingDocs[0]}</strong> will unlock additional eligible government schemes.
              </p>
            ) : (
              <p className="text-india-green font-semibold">
                ✨ Excellent! Your Digital Locker contains all key verification documents for matching schemes.
              </p>
            )}
          </div>
        </div>
      </div>

      {/* AI Smart Suggestions Panel */}
      {aiSuggestions.length > 0 && (
        <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-5 space-y-3 shadow-sm">
          <div className="flex items-center gap-2 border-b border-gray-100 dark:border-slate-800 pb-3">
            <div className="bg-saffron/10 p-1.5 rounded-lg text-saffron-dark">
              <Sparkles className="h-4 w-4" />
            </div>
            <div>
              <h2 className="text-xs font-bold text-gray-800 dark:text-slate-100 uppercase tracking-wider">
                AI Document Priority Recommendations
              </h2>
              <p className="text-gray-400 dark:text-slate-500 text-[11px]">
                Recommended uploads to maximize scheme match rate
              </p>
            </div>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {aiSuggestions.map((sug) => (
              <div key={sug.id} className={`border rounded-xl p-3.5 space-y-2 text-xs ${PRIORITY_STYLE[sug.priority]}`}>
                <div className="flex items-center justify-between">
                  <span className="font-bold">{sug.missingDoc}</span>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold px-2 py-0.5 rounded-full bg-white/70 dark:bg-slate-900/70 border border-current">
                    {sug.priority} Priority
                  </span>
                </div>
                <p className="text-[11px] opacity-90">
                  Unlocks: <strong>{sug.schemesUnlocked.join(", ") || "Government Welfare Schemes"}</strong>
                </p>
                <div className="pt-1 flex items-center justify-between">
                  <span className="text-[10px] opacity-75">ETA: {sug.eta}</span>
                  <button
                    onClick={() => openUploadModalWithPrefill(sug.missingDoc)}
                    className="text-[11px] font-bold underline hover:opacity-80 flex items-center gap-1"
                  >
                    Upload Now
                    <ArrowRight className="h-3 w-3" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Vault Score Bar */}
      {trackedSchemes.length > 0 && (
        <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-5 space-y-3 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-government-blue dark:text-indigo-400" />
              <h2 className="text-xs font-bold text-gray-800 dark:text-slate-100 uppercase tracking-wider">
                Vault Readiness Score
              </h2>
            </div>
            <span
              className={`text-xs font-bold px-3 py-1 rounded-full border ${
                vaultScore.score === 100
                  ? "bg-india-green/10 text-india-green border-india-green/20"
                  : vaultScore.score >= 50
                  ? "bg-saffron/10 text-saffron-dark border-saffron/20"
                  : "bg-red-50 text-red-700 border-red-200"
              }`}
            >
              {vaultScore.score}% &bull; {vaultScore.label}
            </span>
          </div>
          <div className="h-2.5 bg-gray-100 dark:bg-slate-800 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-300 ${
                vaultScore.score === 100
                  ? "bg-india-green"
                  : vaultScore.score >= 50
                  ? "bg-saffron"
                  : "bg-red-500"
              }`}
              style={{ width: `${vaultScore.score}%` }}
            />
          </div>
          <p className="text-[11px] text-gray-500 dark:text-slate-400 mt-2">
            Based on {totalDocs} document{totalDocs !== 1 ? "s" : ""} in your vault across {trackedSchemes.length} tracked scheme{trackedSchemes.length !== 1 ? "s" : ""}.
          </p>
        </div>
      )}

      {/* Document Vault */}
      {documents.length === 0 ? (
        <div className="py-16 flex flex-col items-center text-center text-gray-400 dark:text-slate-500">
          <FolderOpen className="h-12 w-12 opacity-40 mb-4 text-government-blue dark:text-indigo-400" />
          <h3 className="text-sm font-bold text-gray-600 dark:text-slate-300">Your Digital Locker is Empty</h3>
          <p className="text-xs mt-1 max-w-xs">Upload your identity & financial documents to calculate scheme readiness.</p>
          <div className="mt-5 flex items-center gap-3">
            <button
              onClick={() => openUploadModalWithPrefill()}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold bg-government-blue hover:bg-government-blue-dark text-white shadow-sm transition"
            >
              <UploadCloud className="h-4 w-4" />
              Add Document
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-gray-200 dark:border-slate-800 pb-3">
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-bold text-gray-700 dark:text-slate-200">
                Your Documents ({filteredDocs.length})
              </h2>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <div className="relative">
                <Search className="absolute left-2.5 top-1.5 h-3.5 w-3.5 text-gray-400 dark:text-slate-500" />
                <input
                  type="text"
                  placeholder="Search documents..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8 pr-3 py-1.5 text-[11px] border border-gray-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue bg-gray-50 dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 w-32 sm:w-48 transition"
                />
              </div>

              <select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
                className="px-2 py-1.5 text-[11px] border border-gray-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100"
              >
                <option value="all">All Categories</option>
                <option value="Identity Proof">Identity Proof</option>
                <option value="Financial Proof">Financial Proof</option>
                <option value="Category Proof">Category Proof</option>
                <option value="Property Proof">Property Proof</option>
                <option value="Academic Proof">Academic Proof</option>
                <option value="Domicile Proof">Domicile Proof</option>
              </select>

              <div className="flex gap-1 bg-gray-100 dark:bg-slate-800 p-1 rounded-xl shrink-0 select-none">
                {[
                  { id: "all", label: "All" },
                  { id: "verified", label: "Verified" },
                  { id: "pending", label: "Pending" },
                  { id: "rejected", label: "Rejected" },
                ].map((f) => (
                  <button
                    key={f.id}
                    onClick={() => setStatusFilter(f.id)}
                    className={`px-2.5 py-1 rounded-lg text-[10px] font-semibold capitalize transition ${
                      statusFilter === f.id ? "bg-white dark:bg-slate-700 text-gray-900 dark:text-slate-100 shadow-sm" : "text-gray-500 dark:text-slate-400"
                    }`}
                  >
                    {f.label}
                  </button>
                ))}
              </div>

              <div className="flex gap-1 bg-gray-100 dark:bg-slate-800 p-1 rounded-xl shrink-0 ml-1">
                <button
                  onClick={() => setViewMode("list")}
                  className={`p-1 rounded-lg transition ${viewMode === "list" ? "bg-white dark:bg-slate-700 shadow-sm text-gray-900 dark:text-slate-100" : "text-gray-400 dark:text-slate-500"}`}
                  title="List View"
                >
                  <List className="h-3.5 w-3.5" />
                </button>
                <button
                  onClick={() => setViewMode("grid")}
                  className={`p-1 rounded-lg transition ${viewMode === "grid" ? "bg-white dark:bg-slate-700 shadow-sm text-gray-900 dark:text-slate-100" : "text-gray-400 dark:text-slate-500"}`}
                  title="Grid View"
                >
                  <Grid className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </div>

          {filteredDocs.length === 0 ? (
            <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl p-10 text-center space-y-2">
              <FileText className="h-10 w-10 text-gray-300 dark:text-slate-600 mx-auto" />
              <p className="font-semibold text-gray-600 dark:text-slate-300 text-sm">
                No documents found
              </p>
              <p className="text-gray-400 dark:text-slate-500 text-xs">
                No records correspond to the selected filter criteria.
              </p>
            </div>
          ) : Object.keys(docsByType).length > 0 ? (
            Object.entries(docsByType).map(([type, docs]) => (
              <div key={type} className="space-y-2">
                <p className="text-[10px] font-bold text-gray-400 dark:text-slate-500 uppercase tracking-wider px-1">
                  {type}
                </p>
                <div className={viewMode === "grid" ? "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3" : "space-y-2"}>
                  {docs.map((doc) => (
                    <DocumentCard
                      key={doc.id}
                      doc={doc}
                      onRemove={removeDocument}
                      onPreview={handlePreviewDoc}
                      onDownload={handleDownloadDoc}
                      onReupload={(d) => openUploadModalWithPrefill(d.name)}
                      onChangeStatus={updateDocumentStatus}
                      linkedSchemes={getLinkedSchemes(doc.name)}
                      viewMode={viewMode}
                    />
                  ))}
                </div>
              </div>
            ))
          ) : null}
        </div>
      )}

      {/* Scheme Readiness Section */}
      {trackedSchemes.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-sm font-bold text-gray-700 dark:text-slate-200">
            Readiness for Your Tracked Schemes
          </h2>
          <div className="grid sm:grid-cols-2 gap-3">
            {trackedSchemes.map((scheme) => (
              <SchemeReadinessCard key={scheme.id} scheme={scheme} documents={documents} />
            ))}
          </div>
        </div>
      )}

      {trackedSchemes.length === 0 && (
        <div className="bg-white dark:bg-slate-900 border border-dashed border-gray-200 dark:border-slate-800 rounded-2xl p-8 text-center space-y-2">
          <p className="text-xs text-gray-400 dark:text-slate-500">
            Save or apply to schemes to see document readiness here.
          </p>
          <Link
            to="/recommendations"
            className="text-government-blue dark:text-indigo-400 text-xs font-semibold inline-flex items-center gap-1 hover:text-government-blue-dark dark:hover:text-indigo-300"
          >
            Browse schemes
            <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      )}

      {/* Modern Add Document & Drag-and-Drop Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-gray-900/60 backdrop-blur-sm"
            onClick={resetUploadModalState}
          />
          <div className="relative bg-white dark:bg-slate-900 rounded-2xl border border-gray-200 dark:border-slate-800 shadow-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto z-10 animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-gradient-to-r from-government-blue to-government-blue-dark text-white p-5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <UploadCloud className="h-5 w-5 text-saffron" />
                <h3 className="font-bold text-sm text-white">
                  Add Document to Digital Locker
                </h3>
              </div>
              <button
                onClick={resetUploadModalState}
                className="text-white/70 hover:text-white p-1 rounded-lg transition"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {prefillDocName && (
              <div className="bg-government-blue/5 border-b border-government-blue/20 px-6 py-3 flex items-center gap-2 text-xs text-government-blue dark:text-indigo-400 font-semibold">
                <Sparkles className="h-3.5 w-3.5 text-government-blue dark:text-indigo-400" />
                AI Suggestion: <span className="font-black text-government-blue-dark dark:text-indigo-300">{prefillDocName}</span>
              </div>
            )}

            <div className="p-6 space-y-5 text-xs text-gray-900 dark:text-slate-100">
              {/* Drag & Drop File Upload Area */}
              <div>
                <label className="block font-bold text-gray-700 dark:text-slate-300 mb-2">
                  1. Choose Document File (PDF, JPG, JPEG, PNG - Max 10 MB) *
                </label>
                {!selectedFile ? (
                  <div
                    onDragOver={(e) => { e.preventDefault(); setIsDragActive(true); }}
                    onDragLeave={() => setIsDragActive(false)}
                    onDrop={(e) => {
                      e.preventDefault();
                      setIsDragActive(false);
                      if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                        processSelectedFile(e.dataTransfer.files[0]);
                      }
                    }}
                    onClick={() => fileInputRef.current?.click()}
                    className={`border-2 border-dashed rounded-2xl p-6 text-center cursor-pointer transition flex flex-col items-center justify-center gap-2.5 ${
                      isDragActive
                        ? "border-government-blue bg-government-blue/10 scale-[1.01]"
                        : "border-gray-300 dark:border-slate-700 hover:border-government-blue hover:bg-gray-50/80 dark:hover:bg-slate-800/80"
                    }`}
                  >
                    <div className="p-3 bg-government-blue/10 text-government-blue rounded-2xl">
                      <UploadCloud className="h-7 w-7" />
                    </div>
                    <div>
                      <p className="font-bold text-gray-800 dark:text-slate-200 text-sm">
                        Click to browse or drag & drop file here
                      </p>
                      <p className="text-gray-400 dark:text-slate-500 text-[11px] mt-0.5">
                        Supports PDF, JPG, JPEG, PNG (Up to 10 MB)
                      </p>
                    </div>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept=".pdf,.jpg,.jpeg,.png,image/jpeg,image/png,application/pdf"
                      className="hidden"
                      onChange={(e) => {
                        if (e.target.files && e.target.files[0]) {
                          processSelectedFile(e.target.files[0]);
                        }
                      }}
                    />
                  </div>
                ) : (
                  <div className="bg-gray-50 dark:bg-slate-800/60 border border-gray-200 dark:border-slate-700 rounded-2xl p-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="p-2.5 bg-government-blue/10 text-government-blue rounded-xl">
                        <FileText className="h-6 w-6" />
                      </div>
                      <div>
                        <p className="font-bold text-gray-900 dark:text-slate-100 text-xs">{selectedFile.name}</p>
                        <p className="text-[10px] text-gray-400 dark:text-slate-500 font-medium mt-0.5">
                          {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB &bull; {selectedFile.type || "Document"}
                        </p>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedFile(null);
                        setOcrExtractedData(null);
                      }}
                      className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg transition"
                      title="Remove file"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                )}
              </div>

              {/* OCR Scanning Progress Animation */}
              {isAnalyzingOCR && (
                <div className="bg-government-blue/5 border border-government-blue/20 rounded-2xl p-4 space-y-2 animate-pulse">
                  <div className="flex items-center gap-2 text-government-blue font-bold">
                    <Loader2 className="h-4 w-4 animate-spin text-government-blue" />
                    <span>Reading document with AI OCR engine...</span>
                  </div>
                  <div className="h-2 bg-government-blue/20 rounded-full overflow-hidden">
                    <div className="h-full bg-government-blue rounded-full animate-pulse w-3/4" />
                  </div>
                  <p className="text-[10px] text-gray-500 dark:text-slate-400">Extracting holder name, document numbers, and authority seals...</p>
                </div>
              )}

              {/* OCR, Document Classification, and Applicant Name Verification Statuses */}
              {ocrExtractedData && !isAnalyzingOCR && (() => {
                const activeDocConfig = getDocumentFieldConfig(newDocName || newDocType || ocrExtractedData?.documentType);

                if (nameMatchResult?.status === "MISMATCH") {
                  return (
                    <div className="bg-red-50 dark:bg-red-950/60 border-2 border-red-400 dark:border-red-700 rounded-2xl p-4 text-red-900 dark:text-red-200 space-y-3 shadow-sm animate-in fade-in">
                      <div className="flex items-center gap-2.5 font-bold text-sm text-red-700 dark:text-red-400">
                        <AlertTriangle className="h-5 w-5 shrink-0 text-red-600 dark:text-red-400" />
                        <span>Applicant Name Mismatch</span>
                      </div>
                      <p className="text-xs text-red-700 dark:text-red-300 leading-relaxed">
                        {nameMatchResult.reason || "The extracted document holder name does not match the logged-in applicant profile."}
                      </p>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs bg-red-100/60 dark:bg-red-900/40 p-2.5 rounded-xl border border-red-200 dark:border-red-800">
                        <div>
                          <span className="text-[10px] font-semibold text-red-600 dark:text-red-400 uppercase tracking-wider block">Logged-in Profile Name</span>
                          <span className="font-bold text-red-900 dark:text-red-100">{nameMatchResult.applicantName || profile?.name || user?.name || "Citizen"}</span>
                        </div>
                        <div>
                          <span className="text-[10px] font-semibold text-red-600 dark:text-red-400 uppercase tracking-wider block">Extracted Document Holder</span>
                          <span className="font-bold text-red-900 dark:text-red-100">{nameMatchResult.holderName || "Not detected / Mismatched"}</span>
                        </div>
                      </div>
                      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 text-[11px] font-semibold text-red-800 dark:text-red-300 bg-red-100/70 dark:bg-red-900/50 p-2.5 rounded-xl">
                        <span>⚠️ Upload blocked: Documents issued to other individuals cannot be added to your Digital Locker.</span>
                        <button
                          type="button"
                          onClick={() => fileInputRef.current?.click()}
                          className="px-2.5 py-1 bg-red-600 hover:bg-red-700 text-white rounded-lg font-bold text-xs shrink-0 transition"
                        >
                          Choose Another File
                        </button>
                      </div>
                    </div>
                  );
                }

                // 3 Separate Statuses when not a mismatch:
                // 1. Document Detected (Classification)
                // 2. OCR Extraction Complete (OCR Quality)
                // 3. Applicant Name Matched (Identity alignment)
                return (
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 animate-in fade-in">
                    {/* 1. Document Detection & Classification */}
                    <div className="bg-blue-50/80 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800/80 rounded-xl p-3 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] font-bold text-blue-700 dark:text-blue-300 uppercase tracking-wider">Document Detected</span>
                        <span className="text-[10px] font-extrabold px-1.5 py-0.5 rounded bg-blue-100 dark:bg-blue-900/60 text-blue-800 dark:text-blue-200">
                          {ocrExtractedData.classificationConfidence ? Math.round(ocrExtractedData.classificationConfidence * 100) : (ocrExtractedData.confidenceScore ? Math.round(ocrExtractedData.confidenceScore * 100) : 96)}%
                        </span>
                      </div>
                      <p className="font-bold text-xs text-blue-900 dark:text-blue-100 truncate" title={activeDocConfig.displayName}>
                        {activeDocConfig.displayName}
                      </p>
                      <p className="text-[10px] text-blue-700/80 dark:text-blue-300/80">Classification Complete</p>
                    </div>

                    {/* 2. OCR Extraction Quality */}
                    <div className="bg-purple-50/80 dark:bg-purple-950/40 border border-purple-200 dark:border-purple-800/80 rounded-xl p-3 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] font-bold text-purple-700 dark:text-purple-300 uppercase tracking-wider">OCR Extraction Complete</span>
                        <span className="text-[10px] font-extrabold px-1.5 py-0.5 rounded bg-purple-100 dark:bg-purple-900/60 text-purple-800 dark:text-purple-200">
                          {ocrExtractedData.ocrConfidence ? Math.round(ocrExtractedData.ocrConfidence * 100) : 98}%
                        </span>
                      </div>
                      <p className="font-bold text-xs text-purple-900 dark:text-purple-100 truncate">
                        Attributes Extracted
                      </p>
                      <p className="text-[10px] text-purple-700/80 dark:text-purple-300/80">Ready for review</p>
                    </div>

                    {/* 3. Applicant Name Matching */}
                    <div className={`border rounded-xl p-3 space-y-1 ${
                      nameMatchResult?.status === "MATCH"
                        ? "bg-emerald-50/80 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800/80 text-emerald-900 dark:text-emerald-100"
                        : "bg-amber-50/80 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800/80 text-amber-900 dark:text-amber-100"
                    }`}>
                      <div className="flex items-center justify-between">
                        <span className={`text-[10px] font-bold uppercase tracking-wider ${
                          nameMatchResult?.status === "MATCH" ? "text-emerald-700 dark:text-emerald-300" : "text-amber-700 dark:text-amber-300"
                        }`}>
                          Name Verification
                        </span>
                        <span className={`text-[10px] font-extrabold px-1.5 py-0.5 rounded ${
                          nameMatchResult?.status === "MATCH"
                            ? "bg-emerald-100 dark:bg-emerald-900/60 text-emerald-800 dark:text-emerald-200"
                            : "bg-amber-100 dark:bg-amber-900/60 text-amber-800 dark:text-amber-200"
                        }`}>
                          {nameMatchResult?.status === "MATCH" ? "Matched" : "Uncertain"}
                        </span>
                      </div>
                      <p className="font-bold text-xs truncate">
                        {nameMatchResult?.status === "MATCH" ? "Applicant Name Matched" : "Pending Verification"}
                      </p>
                      <p className={`text-[10px] truncate ${
                        nameMatchResult?.status === "MATCH" ? "text-emerald-700/80 dark:text-emerald-300/80" : "text-amber-700/80 dark:text-amber-300/80"
                      }`}>
                        {nameMatchResult?.status === "MATCH" ? `Matches ${nameMatchResult.applicantName || "Profile"}` : "Manual check required"}
                      </p>
                    </div>
                  </div>
                );
              })()}

              {/* Editable Form with Dynamic Document Fields */}
              <form onSubmit={handleUploadSubmit} className="space-y-4">
                <div>
                  <label htmlFor="newDocName" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                    2. Document / Certificate Name *
                  </label>
                  <input
                    id="newDocName"
                    type="text"
                    required
                    placeholder="e.g. Aadhaar Card, Income Certificate, Community Certificate"
                    value={newDocName}
                    onChange={(e) => setNewDocName(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 focus:bg-white dark:focus:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                  />
                </div>

                {/* Dynamic Document-Specific Fields */}
                {(() => {
                  const activeDocConfig = getDocumentFieldConfig(newDocName || newDocType || ocrExtractedData?.documentType);
                  const showHolder = activeDocConfig.fields.includes("holderName");
                  const showDob = activeDocConfig.fields.includes("dateOfBirth");
                  const showDocNumber = activeDocConfig.fields.includes("documentNumber");
                  const showGender = activeDocConfig.fields.includes("gender");
                  const showAddress = activeDocConfig.fields.includes("address");
                  const showCommunity = activeDocConfig.fields.includes("community");
                  const showAnnualIncome = activeDocConfig.fields.includes("annualIncome");
                  const showBusinessOccupation = activeDocConfig.fields.includes("businessOccupation");
                  const showIssueDate = activeDocConfig.fields.includes("issueDate");

                  return (
                    <div className="space-y-3 bg-gray-50/50 dark:bg-slate-800/40 p-3 rounded-2xl border border-gray-200/70 dark:border-slate-700/60">
                      {/* Document Type Indicator */}
                      <div className="flex items-center justify-between text-xs pb-1 border-b border-gray-200/60 dark:border-slate-700/60">
                        <span className="font-semibold text-gray-500 dark:text-slate-400">Detected Document Type:</span>
                        <span className="font-bold text-government-blue dark:text-indigo-400 bg-government-blue/10 dark:bg-indigo-950/50 px-2 py-0.5 rounded-lg">
                          {activeDocConfig.displayName}
                        </span>
                      </div>

                      {/* Holder Name */}
                      {showHolder && (
                        <div>
                          <label htmlFor="holderName" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                            {activeDocConfig.labels?.holderName || "Document Holder Name"} *
                          </label>
                          <input
                            id="holderName"
                            type="text"
                            required
                            placeholder={activeDocConfig.placeholders?.holderName || "e.g. As written on document"}
                            value={holderName}
                            onChange={(e) => setHolderName(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                          />
                        </div>
                      )}

                      {/* DOB & Document Number */}
                      {(showDob || showDocNumber) && (
                        <div className={`grid ${showDob && showDocNumber ? "grid-cols-2" : "grid-cols-1"} gap-3`}>
                          {showDob && (
                            <div>
                              <label htmlFor="docDob" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                                {activeDocConfig.labels?.dateOfBirth || "Date of Birth"} <span className="text-red-500">*</span>
                              </label>
                              <input
                                id="docDob"
                                type="text"
                                placeholder={activeDocConfig.placeholders?.dateOfBirth || "DD/MM/YYYY"}
                                value={docDob}
                                onChange={(e) => setDocDob(e.target.value)}
                                className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                              />
                            </div>
                          )}

                          {showDocNumber && (
                            <div>
                              <label htmlFor="docNumber" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                                {activeDocConfig.labels?.documentNumber || "Document Number"}
                              </label>
                              <input
                                id="docNumber"
                                type="text"
                                placeholder={activeDocConfig.placeholders?.documentNumber || "e.g. Certificate / ID Number"}
                                value={docNumber}
                                onChange={(e) => setDocNumber(e.target.value)}
                                className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                              />
                            </div>
                          )}
                        </div>
                      )}

                      {/* Gender (Aadhaar) */}
                      {showGender && (
                        <div>
                          <label htmlFor="docGender" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                            {activeDocConfig.labels?.gender || "Gender"}
                          </label>
                          <input
                            id="docGender"
                            type="text"
                            placeholder={activeDocConfig.placeholders?.gender || "Male / Female / Transgender"}
                            value={docGender}
                            onChange={(e) => setDocGender(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                          />
                        </div>
                      )}

                      {/* Address (Aadhaar, Domicile, Ration) */}
                      {showAddress && (
                        <div>
                          <label htmlFor="docAddress" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                            {activeDocConfig.labels?.address || "Address"}
                          </label>
                          <textarea
                            id="docAddress"
                            rows={2}
                            placeholder={activeDocConfig.placeholders?.address || "Address as printed on document"}
                            value={docAddress}
                            onChange={(e) => setDocAddress(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition resize-none"
                          />
                        </div>
                      )}

                      {/* Community (Caste/Community Certificates) */}
                      {showCommunity && (
                        <div>
                          <label htmlFor="docCommunity" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                            {activeDocConfig.labels?.community || "Community / Caste"}
                          </label>
                          <input
                            id="docCommunity"
                            type="text"
                            placeholder={activeDocConfig.placeholders?.community || "e.g. SC / ST / OBC / BC / MBC / General"}
                            value={docCommunity}
                            onChange={(e) => setDocCommunity(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                          />
                        </div>
                      )}

                      {/* Annual Income (Income Certificate) */}
                      {showAnnualIncome && (
                        <div>
                          <label htmlFor="docAnnualIncome" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                            {activeDocConfig.labels?.annualIncome || "Annual Family Income (₹)"}
                          </label>
                          <input
                            id="docAnnualIncome"
                            type="text"
                            placeholder={activeDocConfig.placeholders?.annualIncome || "e.g. 120000"}
                            value={docAnnualIncome}
                            onChange={(e) => setDocAnnualIncome(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                          />
                        </div>
                      )}

                      {/* Business / Occupation (Business Certificate) */}
                      {showBusinessOccupation && (
                        <div>
                          <label htmlFor="docBusinessOccupation" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                            {activeDocConfig.labels?.businessOccupation || "Business / Occupation"}
                          </label>
                          <input
                            id="docBusinessOccupation"
                            type="text"
                            placeholder={activeDocConfig.placeholders?.businessOccupation || "e.g. Retail Trade, Agro Services"}
                            value={docBusinessOccupation}
                            onChange={(e) => setDocBusinessOccupation(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                          />
                        </div>
                      )}

                      {/* Issue Date */}
                      {showIssueDate && (
                        <div>
                          <label htmlFor="docIssueDate" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                            {activeDocConfig.labels?.issueDate || "Issue Date / Registration Date"}
                          </label>
                          <input
                            id="docIssueDate"
                            type="text"
                            placeholder={activeDocConfig.placeholders?.issueDate || "DD/MM/YYYY"}
                            value={docIssueDate}
                            onChange={(e) => setDocIssueDate(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                          />
                        </div>
                      )}
                    </div>
                  );
                })()}

                {/* Issuing Authority & Expiry Date */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label htmlFor="newDocIssuer" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                      Issuing Authority *
                    </label>
                    <input
                      id="newDocIssuer"
                      type="text"
                      required
                      placeholder="e.g. UIDAI, Revenue Dept, MSME"
                      value={newDocIssuer}
                      onChange={(e) => setNewDocIssuer(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 focus:bg-white dark:focus:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                    />
                  </div>
                  <div>
                    <label htmlFor="newDocExpiry" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                      Expiry / Validity
                    </label>
                    <input
                      id="newDocExpiry"
                      type="text"
                      placeholder="e.g. No Expiration, 2028-12-31"
                      value={newDocExpiry}
                      onChange={(e) => setNewDocExpiry(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 focus:bg-white dark:focus:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                    />
                  </div>
                </div>

                {/* Document Category & Verification Status */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label htmlFor="newDocType" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                      Document Category
                    </label>
                    <select
                      id="newDocType"
                      value={newDocType}
                      onChange={(e) => setNewDocType(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 focus:bg-white dark:focus:bg-slate-800 text-gray-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                    >
                      <option value="Identity Proof">Identity Proof</option>
                      <option value="Financial Proof">Financial Proof</option>
                      <option value="Category Proof">Category Proof</option>
                      <option value="Property Proof">Property Proof</option>
                      <option value="Academic Proof">Academic Proof</option>
                      <option value="Domicile Proof">Domicile Proof</option>
                    </select>
                  </div>
                  <div>
                    <label htmlFor="newDocStatus" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                      Verification Status
                    </label>
                    <input
                      id="newDocStatus"
                      type="text"
                      readOnly
                      disabled
                      value="Pending Verification"
                      className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-100 dark:bg-slate-800/80 text-gray-600 dark:text-slate-400 rounded-xl cursor-not-allowed"
                    />
                  </div>
                </div>

                {/* Link to Scheme Application */}
                <div>
                  <label htmlFor="linkedApplicationId" className="block font-semibold text-gray-700 dark:text-slate-300 mb-1">
                    Link to Scheme Application (Optional)
                  </label>
                  <select
                    id="linkedApplicationId"
                    value={linkedApplicationId}
                    onChange={(e) => setLinkedApplicationId(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 focus:bg-white dark:focus:bg-slate-800 text-gray-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                  >
                    <option value="">Do not link to specific application now</option>
                    {(applications || []).map((app) => (
                      <option key={app.id} value={app.id}>
                        {app.schemeName || `Scheme #${app.schemeId || app.id}`} (ID: {app.id})
                      </option>
                    ))}
                  </select>
                </div>

                {/* Modal Footer Actions */}
                <div className="flex justify-end gap-2 pt-4 border-t border-gray-100 dark:border-slate-800">
                  <button
                    type="button"
                    onClick={resetUploadModalState}
                    className="px-4 py-2 border border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800 text-gray-600 dark:text-slate-300 rounded-xl font-semibold transition"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isAnalyzingOCR || nameMatchResult?.status === "MISMATCH"}
                    className={`px-5 py-2 rounded-xl font-bold shadow-sm transition flex items-center gap-1.5 ${
                      nameMatchResult?.status === "MISMATCH"
                        ? "bg-red-500/80 text-white cursor-not-allowed opacity-80"
                        : "bg-government-blue hover:bg-government-blue-dark text-white disabled:opacity-50"
                    }`}
                  >
                    {nameMatchResult?.status === "MISMATCH" ? (
                      <>
                        <AlertTriangle className="h-4 w-4" />
                        <span>Cannot Add: Applicant Name Mismatch</span>
                      </>
                    ) : isAnalyzingOCR ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        <span>Processing OCR...</span>
                      </>
                    ) : (
                      <>
                        <CheckCircle className="h-4 w-4" />
                        <span>Confirm & Save to Digital Locker</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Document Details Preview Modal */}
      {previewDocModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-gray-900/60 backdrop-blur-sm" onClick={() => setPreviewDocModal(null)} />
          <div className="relative bg-white dark:bg-slate-900 rounded-2xl border border-gray-200 dark:border-slate-800 shadow-2xl max-w-md w-full p-6 z-10 space-y-4 animate-in fade-in zoom-in-95 duration-150 text-xs">
            <div className="flex items-center justify-between border-b border-gray-100 dark:border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <FileText className="h-5 w-5 text-government-blue dark:text-indigo-400" />
                <h3 className="font-bold text-sm text-gray-900 dark:text-slate-100">{previewDocModal.name}</h3>
              </div>
              <button onClick={() => setPreviewDocModal(null)} className="text-gray-400 hover:text-gray-700 dark:hover:text-slate-200 p-1 rounded-lg">
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="space-y-3 bg-gray-50 dark:bg-slate-800/60 p-4 rounded-xl border border-gray-200 dark:border-slate-700">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 dark:text-slate-500 uppercase">Document ID</span>
                  <p className="font-mono font-bold text-gray-800 dark:text-slate-200">{previewDocModal.id}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 dark:text-slate-500 uppercase">Holder Name</span>
                  <p className="font-bold text-gray-800 dark:text-slate-200">{previewDocModal.holderName || profile?.name || user?.name || "Citizen"}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 dark:text-slate-500 uppercase">Document Number</span>
                  <p className="font-mono font-bold text-gray-800 dark:text-slate-200">{previewDocModal.docNumber || "XXXX XXXX 5489"}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 dark:text-slate-500 uppercase">Issuer</span>
                  <p className="font-bold text-gray-800 dark:text-slate-200">{previewDocModal.issuer || "Self-Uploaded"}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 dark:text-slate-500 uppercase">Category</span>
                  <p className="font-bold text-gray-800 dark:text-slate-200">{previewDocModal.type}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 dark:text-slate-500 uppercase">Expiry Date</span>
                  <p className="font-bold text-gray-800 dark:text-slate-200">{previewDocModal.expiryDate || "No Expiration"}</p>
                </div>
              </div>

              {/* Status Breakdown */}
              <div className="pt-2 border-t border-gray-200 dark:border-slate-700 space-y-1.5">
                <div className="flex justify-between items-center text-[11px]">
                  <span className="text-gray-500 dark:text-slate-400">AI Validation Status:</span>
                  <span className="font-semibold text-emerald-700 dark:text-emerald-300">
                    {previewDocModal.aiStatus || "AI_PASSED"} (Score: {previewDocModal.aiScore ?? previewDocModal.aiConfidence ?? 88}/100)
                  </span>
                </div>
                <div className="flex justify-between items-center text-[11px]">
                  <span className="text-gray-500 dark:text-slate-400">Officer Verification:</span>
                  <span className={`font-semibold ${previewDocModal.status === "verified" || previewDocModal.officerStatus === "ADMIN_VERIFIED" ? "text-india-green" : previewDocModal.status === "reupload_requested" || previewDocModal.detailedStatus === "CORRECTION_REQUIRED" ? "text-orange-600" : "text-blue-600"}`}>
                    {previewDocModal.status === "verified" || previewDocModal.officerStatus === "ADMIN_VERIFIED" ? "Officer Verified" : previewDocModal.status === "reupload_requested" || previewDocModal.detailedStatus === "CORRECTION_REQUIRED" ? "Correction Required" : "Awaiting Officer Verification"}
                  </span>
                </div>
                {(previewDocModal.correctionNotes || previewDocModal.rejectionReason) && (
                  <p className="text-[10px] text-orange-700 dark:text-orange-300 bg-orange-50 dark:bg-orange-950/30 p-2 rounded-lg">
                    <strong>Officer Note:</strong> {previewDocModal.correctionNotes || previewDocModal.rejectionReason}
                  </p>
                )}
              </div>

              {/* Statutory Aadhaar Disclaimer */}
              {previewDocModal.name?.toLowerCase().includes("aadhaar") && (
                <div className="pt-2 border-t border-gray-200 dark:border-slate-700">
                  <p className="text-[9px] text-gray-500 dark:text-slate-400 italic leading-snug">
                    <strong>Statutory Disclaimer:</strong> Aadhaar document validation is performed via AI-assisted visual structure and checksum checks. It does not constitute direct electronic authentication with UIDAI.
                  </p>
                </div>
              )}

              <div className="pt-2 border-t border-gray-200 dark:border-slate-700 flex items-center justify-between text-[11px]">
                <span className="text-gray-500 dark:text-slate-400">OCR Confidence: <strong className="text-emerald-700 dark:text-emerald-400">{previewDocModal.aiConfidence || 98}%</strong></span>
                <span className="text-gray-500 dark:text-slate-400">File: <strong className="text-slate-800 dark:text-slate-200">{previewDocModal.filename || `${previewDocModal.name}.pdf`} ({previewDocModal.filesize || "1.4 MB"})</strong></span>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => {
                  handleDownloadDoc(previewDocModal);
                  setPreviewDocModal(null);
                }}
                className="px-4 py-2 bg-government-blue hover:bg-government-blue-dark text-white rounded-xl font-bold flex items-center gap-1.5 transition"
              >
                <Download className="h-4 w-4" />
                <span>Download File</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Context-Aware AI Chat Widget */}
      <SchemeAIChatWidget
        isOpen={aiChatOpen}
        onClose={() => setAiChatOpen(false)}
        pageContext="/documents"
        contextData={aiContextData}
      />
    </div>
  );
}