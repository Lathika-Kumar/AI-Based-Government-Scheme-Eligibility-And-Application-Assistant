import React, { useState, useMemo, useEffect, useRef } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { getDocReadinessForScheme, getOverallVaultScore } from "@utils/documentReadiness";
import { usePageMeta } from "@utils/usePageMeta";
import { useToast } from "@components/ui/ToastNotification";
import { extractDocumentData } from "@services/ocrService";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import DigiLockerModal from "@components/DigiLockerModal";
import { DocumentCardSkeleton } from "@components/ui/LoadingSkeleton";
import EmptyState from "@components/ui/EmptyState";
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
  Building
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
  DigiLocker: "bg-india-green/10 text-india-green border-india-green/20",
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

function DocumentCard({ doc, onRemove, onPreview, onDownload, onChangeStatus, linkedSchemes = [], viewMode = "list" }) {
  const meta = STATUS_META[doc.status] || STATUS_META.unlinked;
  const StatusIcon = meta.icon;
  const typeColor = TYPE_ICON_COLOR[doc.type] || "bg-gray-100 text-gray-500";
  const expired = isExpired(doc.expiryDate);
  const sourceBadgeClass = SOURCE_BADGE[doc.source] || SOURCE_BADGE["Manual Upload"];

  const isGrid = viewMode === "grid";

  return (
    <div className={`bg-white border rounded-2xl p-5 flex ${isGrid ? "flex-col" : "flex-col sm:flex-row sm:items-center"} gap-4 ${expired ? "border-red-200" : "border-gray-200"} shadow-sm hover:shadow-md transition relative`}>
      {expired && (
        <div className="absolute top-2.5 right-2.5 flex items-center gap-1 bg-red-50 text-red-700 text-[10px] font-extrabold px-2 py-0.5 rounded-full border border-red-200 select-none">
          <CalendarX className="h-3 w-3" />
          Expired
        </div>
      )}
      <div className={`flex items-start ${isGrid ? "justify-between" : "gap-4 w-full sm:w-auto flex-1 min-w-0"}`}>
        <div className={`flex ${isGrid ? "items-start" : "items-center"} gap-3 ${isGrid ? "" : "flex-1 min-w-0"}`}>
          <div className={`p-3 rounded-xl shrink-0 ${typeColor}`}>
            <FileText className="h-5 w-5" />
          </div>

          <div className="flex-1 min-w-0 space-y-1.5">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="text-xs font-bold text-gray-900 truncate">{doc.name}</h3>
              {doc.status === "verified" && !expired && (
                <span className="text-[10px] bg-india-green/10 text-india-green px-1.5 py-0.5 rounded font-extrabold border border-india-green/20 select-none flex items-center gap-1">
                  <ShieldCheck className="h-3 w-3" /> Verified Registry
                </span>
              )}
            </div>

            {/* Document Details & Metadata */}
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[10px] text-gray-500 font-medium">
              <span>Holder: <strong className="text-gray-700">{doc.holderName || "Rajesh Patel"}</strong></span>
              <span>&bull;</span>
              <span>Issuer: <strong className="text-gray-700">{doc.issuer || "Self-Uploaded"}</strong></span>
              <span>&bull;</span>
              <span>Size: <strong className="text-gray-700">{doc.filesize || doc.fileSize || "1.4 MB"}</strong></span>
              <span>&bull;</span>
              <span className={expired ? "text-red-500 font-bold" : ""}>
                Expires: <strong className={expired ? "text-red-600" : "text-gray-700"}>{doc.expiryDate || "No Expiration"}</strong>
              </span>
              {doc.date && (
                <>
                  <span>&bull;</span>
                  <span>Uploaded: {doc.date}</span>
                </>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-1.5">
              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded border ${sourceBadgeClass}`}>
                {doc.source || "Manual Upload"}
              </span>
              <span className="text-[10px] font-bold px-1.5 py-0.5 rounded border bg-emerald-50 text-emerald-700 border-emerald-200 flex items-center gap-1">
                <Sparkles className="h-2.5 w-2.5 text-emerald-600" />
                OCR: {doc.aiConfidence || 98}% Confidence
              </span>
            </div>

            {/* Linked schemes */}
            {linkedSchemes.length > 0 && (
              <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">
                  Linked Schemes:
                </span>
                {linkedSchemes.map((s) => (
                  <span key={s.id} className="bg-government-blue/10 border border-government-blue/20 text-government-blue px-1.5 py-0.2 rounded text-[10px] font-bold">
                    {s.name.split(" (")[0]}
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>

        {isGrid && (
          <div className="flex items-center gap-2">
            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-[10px] font-bold ${meta.badge}`}>
              <StatusIcon className="h-3 w-3" />
              {meta.label}
            </span>
          </div>
        )}
      </div>

      <div className={`flex flex-col sm:flex-row sm:items-center ${isGrid ? "justify-between pt-3 border-t border-gray-100" : "gap-3 shrink-0"}`}>
        {!isGrid && (
          <div className="flex items-center gap-2">
            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-[10px] font-bold ${meta.badge}`}>
              <StatusIcon className="h-3 w-3" />
              {meta.label}
            </span>
          </div>
        )}

        <div className="flex items-center gap-1">
          <button onClick={() => onPreview(doc)} className="p-1.5 text-gray-400 hover:text-government-blue hover:bg-government-blue/10 rounded-lg transition" title="Preview Document Details">
            <Eye className="h-4 w-4" />
          </button>
          <button onClick={() => onDownload(doc)} className="p-1.5 text-gray-400 hover:text-india-green hover:bg-india-green/10 rounded-lg transition" title="Download Document">
            <Download className="h-4 w-4" />
          </button>
          <div className="w-px h-4 bg-gray-200 mx-1"></div>
          <button
            onClick={() => onRemove(doc.id)}
            className="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition"
            title="Remove document"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>
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
    <div className="bg-white border border-gray-200 rounded-2xl p-5 space-y-3 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[10px] text-gray-400 uppercase tracking-wider font-semibold">{scheme.ministry}</p>
          <h4 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{scheme.name}</h4>
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
          <span className="text-[10px] text-gray-400">
            {readiness.totalAvailable}/{readiness.totalRequired} documents ready
          </span>
          <span className="text-[10px] font-bold text-gray-600">{readiness.readinessScore}%</span>
        </div>
        <div className="h-2.5 bg-gray-100 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-300 ${barColor}`}
            style={{ width: `${readiness.readinessScore}%` }}
          />
        </div>
      </div>

      {readiness.missingDocs.length > 0 && (
        <div className="space-y-1">
          <p className="text-[10px] font-semibold text-red-700 uppercase tracking-wider">
            Missing:
          </p>
          {readiness.missingDocs.map((doc) => (
            <div key={doc} className="flex items-center gap-1.5 text-[11px] text-red-600">
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
        className="inline-flex items-center gap-1 text-government-blue hover:text-government-blue-dark text-xs font-semibold"
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
  const [holderName, setHolderName] = useState(profile?.name || "Rajesh Patel");
  const [docNumber, setDocNumber] = useState("");

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
  const [isDigiLockerModalOpen, setIsDigiLockerModalOpen] = useState(false);
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
    setHolderName(profile?.name || "Rajesh Patel");
    setDocNumber("");
    setIsModalOpen(false);
  };

  const handleDigiLockerImport = () => {
    setIsDigiLockerModalOpen(true);
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

    try {
      const ocrRes = await extractDocumentData({
        file,
        fileName: file.name,
        profileName: profile?.name || "Rajesh Patel"
      });

      if (ocrRes.success && ocrRes.data) {
        const data = ocrRes.data;
        setOcrExtractedData(data);
        setNewDocName(data.documentName || file.name.replace(/\.[^/.]+$/, ""));
        setNewDocIssuer(data.issuer || "Self-Uploaded");
        setNewDocType(data.category || "Identity Proof");
        setNewDocExpiry(data.expiry || "No Expiration");
        setHolderName(data.holderName || profile?.name || "Rajesh Patel");
        setDocNumber(data.documentNumber || `XXXX XXXX ${Math.floor(1000 + Math.random() * 9000)}`);
        setNewDocStatus("pending_review");
        showToast("success", "AI OCR Extraction Complete", `Extracted attributes for ${data.documentName} with 98% confidence.`);
      }
    } catch (err) {
      showToast("error", "OCR Scan Failed", "Manual form entry required.");
    } finally {
      setIsAnalyzingOCR(false);
    }
  };

  const handleUploadSubmit = (e) => {
    e.preventDefault();
    if (!newDocName.trim()) {
      showToast("error", "Validation Error", "Document Name is required.");
      return;
    }

    const genIdNumber = Math.floor(1000 + Math.random() * 9000);
    const docPayload = {
      id: `DOC-${genIdNumber}`,
      name: newDocName.trim(),
      filename: selectedFile ? selectedFile.name : `${newDocName.trim().toLowerCase().replace(/\s+/g, "_")}.pdf`,
      filesize: selectedFile ? `${(selectedFile.size / (1024 * 1024)).toFixed(1)} MB` : "1.4 MB",
      type: newDocType,
      status: "pending_review",
      issuer: newDocIssuer.trim() || "Self-Uploaded",
      expiryDate: newDocExpiry.trim() || "No Expiration",
      holderName: holderName.trim() || profile?.name || "Rajesh Patel",
      docNumber: docNumber.trim() || `XXXX XXXX ${genIdNumber}`,
      ocrStatus: "Success",
      aiConfidence: 98,
      uploadDate: new Date().toISOString().split("T")[0],
      source: "Manual Upload"
    };

    addDocument(docPayload);
    resetUploadModalState();
  };

  const handlePreviewDoc = (doc) => {
    setPreviewDocModal(doc);
  };

  const handleDownloadDoc = (doc) => {
    showToast("info", "Downloading File", `Downloading ${doc.filename || `${doc.name}.pdf`}...`);
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
            onClick={handleDigiLockerImport}
            disabled={isImporting}
            className="inline-flex items-center gap-1.5 bg-india-green hover:bg-india-green-dark text-white px-4 py-2 rounded-xl text-xs font-semibold shadow transition shrink-0 disabled:opacity-70"
          >
            <RefreshCw className={`h-4 w-4 ${isImporting ? "animate-spin" : ""}`} />
            Import from DigiLocker
          </button>
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
        <div className="bg-white border border-gray-200 rounded-2xl p-5 space-y-3 shadow-sm">
          <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
            <div className="bg-saffron/10 p-1.5 rounded-lg text-saffron-dark">
              <Sparkles className="h-4 w-4" />
            </div>
            <div>
              <h2 className="text-xs font-bold text-gray-800 uppercase tracking-wider">
                AI Document Priority Recommendations
              </h2>
              <p className="text-gray-400 text-[11px]">
                Recommended uploads to maximize scheme match rate
              </p>
            </div>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {aiSuggestions.map((sug) => (
              <div key={sug.id} className={`border rounded-xl p-3.5 space-y-2 text-xs ${PRIORITY_STYLE[sug.priority]}`}>
                <div className="flex items-center justify-between">
                  <span className="font-bold">{sug.missingDoc}</span>
                  <span className="text-[10px] uppercase tracking-wider font-extrabold px-2 py-0.5 rounded-full bg-white/70 border border-current">
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
        <div className="bg-white border border-gray-200 rounded-2xl p-5 space-y-3 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-government-blue" />
              <h2 className="text-xs font-bold text-gray-800 uppercase tracking-wider">
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
          <div className="h-2.5 bg-gray-100 rounded-full overflow-hidden">
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
          <p className="text-[11px] text-gray-500 mt-2">
            Based on {totalDocs} document{totalDocs !== 1 ? "s" : ""} in your vault across {trackedSchemes.length} tracked scheme{trackedSchemes.length !== 1 ? "s" : ""}.
          </p>
        </div>
      )}

      {/* Document Vault */}
      {documents.length === 0 ? (
        <div className="py-16 flex flex-col items-center text-center text-gray-400">
          <FolderOpen className="h-12 w-12 opacity-40 mb-4 text-government-blue" />
          <h3 className="text-sm font-bold text-gray-600">Your Digital Locker is Empty</h3>
          <p className="text-xs mt-1 max-w-xs">Upload your identity & financial documents to calculate scheme readiness.</p>
          <div className="mt-5 flex items-center gap-3">
            <button
              onClick={() => openUploadModalWithPrefill()}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold bg-government-blue hover:bg-government-blue-dark text-white shadow-sm transition"
            >
              <UploadCloud className="h-4 w-4" />
              Add Document
            </button>
            <button
              onClick={handleDigiLockerImport}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold bg-gray-100 text-gray-700 hover:bg-gray-200 border border-gray-200 transition"
            >
              <RefreshCw className="h-4 w-4" />
              Connect DigiLocker
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-gray-200 pb-3">
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-bold text-gray-700">
                Your Documents ({filteredDocs.length})
              </h2>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <div className="relative">
                <Search className="absolute left-2.5 top-1.5 h-3.5 w-3.5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search documents..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8 pr-3 py-1.5 text-[11px] border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue bg-gray-50 w-32 sm:w-48 transition"
                />
              </div>

              <select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
                className="px-2 py-1.5 text-[11px] border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue bg-white"
              >
                <option value="all">All Categories</option>
                <option value="Identity Proof">Identity Proof</option>
                <option value="Financial Proof">Financial Proof</option>
                <option value="Category Proof">Category Proof</option>
                <option value="Property Proof">Property Proof</option>
                <option value="Academic Proof">Academic Proof</option>
                <option value="Domicile Proof">Domicile Proof</option>
              </select>

              <div className="flex gap-1 bg-gray-100 p-1 rounded-xl shrink-0 select-none">
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
                      statusFilter === f.id ? "bg-white text-gray-900 shadow-sm" : "text-gray-500"
                    }`}
                  >
                    {f.label}
                  </button>
                ))}
              </div>

              <div className="flex gap-1 bg-gray-100 p-1 rounded-xl shrink-0 ml-1">
                <button
                  onClick={() => setViewMode("list")}
                  className={`p-1 rounded-lg transition ${viewMode === "list" ? "bg-white shadow-sm text-gray-900" : "text-gray-400"}`}
                  title="List View"
                >
                  <List className="h-3.5 w-3.5" />
                </button>
                <button
                  onClick={() => setViewMode("grid")}
                  className={`p-1 rounded-lg transition ${viewMode === "grid" ? "bg-white shadow-sm text-gray-900" : "text-gray-400"}`}
                  title="Grid View"
                >
                  <Grid className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </div>

          {filteredDocs.length === 0 ? (
            <div className="bg-white border border-gray-200 rounded-2xl p-10 text-center space-y-2">
              <FileText className="h-10 w-10 text-gray-300 mx-auto" />
              <p className="font-semibold text-gray-600 text-sm">
                No documents found
              </p>
              <p className="text-gray-400 text-xs">
                No records correspond to the selected filter criteria.
              </p>
            </div>
          ) : Object.keys(docsByType).length > 0 ? (
            Object.entries(docsByType).map(([type, docs]) => (
              <div key={type} className="space-y-2">
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider px-1">
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
          <h2 className="text-sm font-bold text-gray-700">
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
        <div className="bg-white border border-dashed border-gray-200 rounded-2xl p-8 text-center space-y-2">
          <p className="text-xs text-gray-400">
            Save or apply to schemes to see document readiness here.
          </p>
          <Link
            to="/recommendations"
            className="text-government-blue text-xs font-semibold inline-flex items-center gap-1 hover:text-government-blue-dark"
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
          <div className="relative bg-white rounded-2xl border border-gray-200 shadow-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto z-10 animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-gradient-to-r from-government-blue to-government-blue-dark text-white p-5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <UploadCloud className="h-5 w-5 text-saffron" />
                <h3 className="font-bold text-sm">
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
              <div className="bg-government-blue/5 border-b border-government-blue/20 px-6 py-3 flex items-center gap-2 text-xs text-government-blue font-semibold">
                <Sparkles className="h-3.5 w-3.5 text-government-blue" />
                AI Suggestion: <span className="font-black text-government-blue-dark">{prefillDocName}</span>
              </div>
            )}

            <div className="p-6 space-y-5 text-xs">
              {/* Drag & Drop File Upload Area */}
              <div>
                <label className="block font-bold text-gray-700 mb-2">
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
                        : "border-gray-300 hover:border-government-blue hover:bg-gray-50/80"
                    }`}
                  >
                    <div className="p-3 bg-government-blue/10 rounded-2xl text-government-blue">
                      <UploadCloud className="h-7 w-7" />
                    </div>
                    <div>
                      <p className="font-bold text-gray-800 text-sm">
                        Click to browse or drag & drop file here
                      </p>
                      <p className="text-gray-400 text-[11px] mt-0.5">
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
                  <div className="bg-gray-50 border border-gray-200 rounded-2xl p-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="p-2.5 bg-government-blue/10 text-government-blue rounded-xl">
                        <FileText className="h-6 w-6" />
                      </div>
                      <div>
                        <p className="font-bold text-gray-900 text-xs">{selectedFile.name}</p>
                        <p className="text-[10px] text-gray-400 font-medium mt-0.5">
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
                      className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
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
                  <p className="text-[10px] text-gray-500">Extracting holder name, document numbers, and authority seals...</p>
                </div>
              )}

              {/* OCR Completion Banner */}
              {ocrExtractedData && !isAnalyzingOCR && (
                <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-3.5 flex items-start gap-2 text-emerald-800">
                  <Sparkles className="h-4 w-4 text-emerald-600 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-bold text-xs">AI OCR Extraction Complete (98% Confidence)</p>
                    <p className="text-[11px] text-emerald-700/80 mt-0.5">
                      Extracted attributes have auto-populated the form fields below. You can review and edit them before saving.
                    </p>
                  </div>
                </div>
              )}

              {/* Auto-Filled Editable Form */}
              <form onSubmit={handleUploadSubmit} className="space-y-4">
                <div>
                  <label htmlFor="newDocName" className="block font-semibold text-gray-700 mb-1">
                    2. Document / Certificate Name *
                  </label>
                  <input
                    id="newDocName"
                    type="text"
                    required
                    placeholder="e.g. Aadhaar Card, Income Certificate"
                    value={newDocName}
                    onChange={(e) => setNewDocName(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label htmlFor="holderName" className="block font-semibold text-gray-700 mb-1">
                      Holder Name
                    </label>
                    <input
                      id="holderName"
                      type="text"
                      placeholder="e.g. Rajesh Patel"
                      value={holderName}
                      onChange={(e) => setHolderName(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                    />
                  </div>
                  <div>
                    <label htmlFor="docNumber" className="block font-semibold text-gray-700 mb-1">
                      Document Number
                    </label>
                    <input
                      id="docNumber"
                      type="text"
                      placeholder="e.g. XXXX XXXX 5489"
                      value={docNumber}
                      onChange={(e) => setDocNumber(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label htmlFor="newDocIssuer" className="block font-semibold text-gray-700 mb-1">
                      Issuing Authority *
                    </label>
                    <input
                      id="newDocIssuer"
                      type="text"
                      required
                      placeholder="e.g. UIDAI, Revenue Dept"
                      value={newDocIssuer}
                      onChange={(e) => setNewDocIssuer(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                    />
                  </div>
                  <div>
                    <label htmlFor="newDocExpiry" className="block font-semibold text-gray-700 mb-1">
                      Expiry Date
                    </label>
                    <input
                      id="newDocExpiry"
                      type="text"
                      placeholder="e.g. 2028-12-31, No Expiration"
                      value={newDocExpiry}
                      onChange={(e) => setNewDocExpiry(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label htmlFor="newDocType" className="block font-semibold text-gray-700 mb-1">
                      Document Category
                    </label>
                    <select
                      id="newDocType"
                      value={newDocType}
                      onChange={(e) => setNewDocType(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
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
                    <label htmlFor="newDocStatus" className="block font-semibold text-gray-700 mb-1">
                      Verification Status
                    </label>
                    <select
                      id="newDocStatus"
                      value={newDocStatus}
                      onChange={(e) => setNewDocStatus(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl transition"
                    >
                      <option value="pending_review">Pending Verification</option>
                      <option value="uploaded">Uploaded</option>
                      <option value="verified">Verified</option>
                    </select>
                  </div>
                </div>

                <div className="flex justify-end gap-2 pt-4 border-t border-gray-100">
                  <button
                    type="button"
                    onClick={resetUploadModalState}
                    className="px-4 py-2 border border-gray-200 hover:bg-gray-50 text-gray-600 rounded-xl font-semibold transition"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isAnalyzingOCR}
                    className="px-5 py-2 bg-government-blue hover:bg-government-blue-dark text-white rounded-xl font-bold shadow-sm transition flex items-center gap-1.5 disabled:opacity-50"
                  >
                    <CheckCircle className="h-4 w-4" />
                    <span>Add to Vault</span>
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
          <div className="relative bg-white rounded-2xl border border-gray-200 shadow-2xl max-w-md w-full p-6 z-10 space-y-4 animate-in fade-in zoom-in-95 duration-150 text-xs">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <FileText className="h-5 w-5 text-government-blue" />
                <h3 className="font-bold text-sm text-gray-900">{previewDocModal.name}</h3>
              </div>
              <button onClick={() => setPreviewDocModal(null)} className="text-gray-400 hover:text-gray-700 p-1 rounded-lg">
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="space-y-3 bg-gray-50 p-4 rounded-xl border border-gray-200">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 uppercase">Document ID</span>
                  <p className="font-mono font-bold text-gray-800">{previewDocModal.id}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 uppercase">Holder Name</span>
                  <p className="font-bold text-gray-800">{previewDocModal.holderName || "Rajesh Patel"}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 uppercase">Document Number</span>
                  <p className="font-mono font-bold text-gray-800">{previewDocModal.docNumber || "XXXX XXXX 5489"}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 uppercase">Issuer</span>
                  <p className="font-bold text-gray-800">{previewDocModal.issuer || "Self-Uploaded"}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 uppercase">Category</span>
                  <p className="font-bold text-gray-800">{previewDocModal.type}</p>
                </div>
                <div>
                  <span className="text-[10px] font-semibold text-gray-400 uppercase">Expiry Date</span>
                  <p className="font-bold text-gray-800">{previewDocModal.expiryDate || "No Expiration"}</p>
                </div>
              </div>

              <div className="pt-2 border-t border-gray-200 flex items-center justify-between text-[11px]">
                <span className="text-gray-500">OCR Confidence: <strong className="text-emerald-700">{previewDocModal.aiConfidence || 98}%</strong></span>
                <span className="text-gray-500">File: <strong>{previewDocModal.filename || `${previewDocModal.name}.pdf`} ({previewDocModal.filesize || "1.4 MB"})</strong></span>
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

      {/* DigiLocker Interactive Import Modal */}
      {isDigiLockerModalOpen && (
        <DigiLockerModal
          onImport={() => {
            importDocuments();
            setIsDigiLockerModalOpen(false);
          }}
          onClose={() => setIsDigiLockerModalOpen(false)}
        />
      )}
    </div>
  );
}