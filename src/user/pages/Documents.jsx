import React, { useState, useMemo, useEffect } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { getDocReadinessForScheme, getOverallVaultScore } from "@utils/documentReadiness";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
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
  Grid,
  List,
  Search,
  Bot,
  Sparkles,
  CalendarX,
  Zap,
  MessageSquare,
} from "lucide-react";

const STATUS_META = {
  verified: {
    label: "Verified",
    badge: "bg-emerald-50 text-emerald-800 border-emerald-200",
    icon: CheckCircle,
    iconColor: "text-emerald-500",
    dot: "bg-emerald-500",
  },
  uploaded: {
    label: "Uploaded",
    badge: "bg-blue-50 text-blue-800 border-blue-200",
    icon: UploadCloud,
    iconColor: "text-blue-500",
    dot: "bg-blue-400",
  },
  pending_review: {
    label: "Pending Review",
    badge: "bg-amber-50 text-amber-800 border-amber-200",
    icon: Clock,
    iconColor: "text-amber-500",
    dot: "bg-amber-400",
  },
  rejected: {
    label: "Rejected",
    badge: "bg-rose-50 text-rose-800 border-rose-200",
    icon: XCircle,
    iconColor: "text-rose-500",
    dot: "bg-rose-400",
  },
  unlinked: {
    label: "Not Linked",
    badge: "bg-slate-100 text-slate-600 border-slate-200",
    icon: AlertCircle,
    iconColor: "text-slate-400",
    dot: "bg-slate-300",
  },
};

const TYPE_ICON_COLOR = {
  "Identity Proof": "bg-indigo-50 text-indigo-600",
  "Financial Proof": "bg-emerald-50 text-emerald-600",
  "Category Proof": "bg-amber-50 text-amber-600",
  "Property Proof": "bg-violet-50 text-violet-600",
  "Academic Proof": "bg-blue-50 text-blue-600",
  "Domicile Proof": "bg-rose-50 text-rose-500",
};

const SOURCE_BADGE = {
  DigiLocker: "bg-emerald-50 text-emerald-700 border-emerald-200",
  "State e-District Portal": "bg-blue-50 text-blue-700 border-blue-200",
  "Manual Upload": "bg-slate-100 text-slate-600 border-slate-200",
  "Government API": "bg-indigo-50 text-indigo-700 border-indigo-200",
  "Officer Verified": "bg-purple-50 text-purple-700 border-purple-200",
};

function isExpired(expiryDate) {
  if (!expiryDate || expiryDate === "No Expiration") {
return false;
}
  return new Date(expiryDate) < new Date();
}

function DocumentCard({ doc, onRemove, onChangeStatus, linkedSchemes = [], viewMode = "list" }) {
  const { language, t } = useApp();
  const meta = STATUS_META[doc.status] || STATUS_META.unlinked;
  const StatusIcon = meta.icon;
  const typeColor = TYPE_ICON_COLOR[doc.type] || "bg-slate-100 text-slate-500";
  const expired = isExpired(doc.expiryDate);
  const sourceBadgeClass = SOURCE_BADGE[doc.source] || SOURCE_BADGE["Manual Upload"];

  const getStatusLabel = (status) => {
    switch (status) {
      case "verified": return t("vault_status_verified") || "Verified";
      case "uploaded": return t("vault_status_uploaded") || "Uploaded";
      case "pending_review": return t("vault_status_pending") || "Pending Review";
      case "rejected": return t("vault_status_rejected") || "Rejected";
      default: return t("vault_status_unlinked") || "Not Linked";
    }
  };

  const isGrid = viewMode === "grid";

  return (
    <div className={`bg-white border ${expired ? "border-rose-200 shadow-rose-50" : "border-slate-200"} rounded-2xl p-4 flex ${isGrid ? "flex-col" : "flex-col sm:flex-row sm:items-center"} gap-4 hover:shadow-sm transition relative`}>
      {expired && (
        <div className="absolute top-2.5 right-2.5 flex items-center gap-1 bg-rose-100 text-rose-700 text-[9px] font-extrabold px-2 py-0.5 rounded-full border border-rose-200 select-none">
          <CalendarX className="h-3 w-3" />
          {t("vault_expired") || "Expired"}
        </div>
      )}
      <div className={`flex items-start ${isGrid ? "justify-between" : "gap-4 w-full sm:w-auto flex-1 min-w-0"}`}>
        <div className={`flex ${isGrid ? "items-start" : "items-center"} gap-3 ${isGrid ? "" : "flex-1 min-w-0"}`}>
          <div className={`p-3 rounded-xl shrink-0 ${typeColor}`}>
            <FileText className="h-5 w-5" />
          </div>

          <div className="flex-1 min-w-0 space-y-1.5">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="font-bold text-slate-800 text-sm truncate">{doc.name}</h3>
              {doc.status === "verified" && !expired && (
                <span className="text-[9px] bg-emerald-100 text-emerald-800 px-1.5 py-0.5 rounded font-extrabold border border-emerald-200 select-none flex items-center gap-1">
                  ✓ {t("vault_status_verified_registry") || "Verified Registry"}
                </span>
              )}
            </div>

            {/* Verification Source Badge */}
            <div className="flex flex-wrap items-center gap-1.5">
              <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded border ${sourceBadgeClass}`}>
                {doc.source || "Manual Upload"}
              </span>
              <span className="text-[9px] font-bold px-1.5 py-0.5 rounded border bg-slate-50 text-slate-500 border-slate-200">
                OCR: {t("vault_ocr_soon") || "Coming Soon"}
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[10px] text-slate-400 font-medium">
              <span>{t("vault_label_issuer") || "Issuer: "}<strong className="text-slate-600">{doc.issuer || "Self-Uploaded"}</strong></span>
              <span>&bull;</span>
              <span className={expired ? "text-rose-500 font-bold" : ""}>
                {t("vault_label_expires") || "Expires: "}
                <strong className={expired ? "text-rose-600" : "text-slate-600"}>{doc.expiryDate || "No Expiration"}</strong>
              </span>
              {doc.date && (
                <>
                  <span>&bull;</span>
                  <span>{t("vault_label_uploaded") || "Uploaded: "}{doc.date}</span>
                </>
              )}
            </div>

            {/* Linked schemes */}
            {linkedSchemes.length > 0 && (
              <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wide">
                  {t("vault_label_linked_schemes") || "Linked Schemes:"}
                </span>
                {linkedSchemes.map((s) => (
                  <span key={s.id} className="bg-indigo-50 border border-indigo-100 text-indigo-700 px-1.5 py-0.2 rounded text-[9px] font-bold">
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
              <StatusIcon className={`h-3 w-3 ${meta.iconColor}`} />
            </span>
          </div>
        )}
      </div>

      <div className={`flex flex-col sm:flex-row sm:items-center ${isGrid ? "justify-between pt-3 border-t border-slate-100" : "gap-3 shrink-0"}`}>
        {!isGrid && (
          <div className="flex items-center gap-2">
            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-[10px] font-bold ${meta.badge}`}>
              <StatusIcon className={`h-3 w-3 ${meta.iconColor}`} />
              {getStatusLabel(doc.status)}
            </span>
            <select
              value={doc.status}
              onChange={(e) => onChangeStatus(doc.id, e.target.value)}
              className="text-[10px] border border-slate-200 rounded-lg px-2 py-0.5 text-slate-600 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-400"
              title="Change status (mock)"
            >
              <option value="verified">{t("vault_status_verified") || "Verified"}</option>
              <option value="uploaded">{t("vault_status_uploaded") || "Uploaded"}</option>
              <option value="pending_review">{t("vault_status_pending") || "Pending Review"}</option>
              <option value="rejected">{t("vault_status_rejected") || "Rejected"}</option>
            </select>
          </div>
        )}

        {isGrid && (
          <select
            value={doc.status}
            onChange={(e) => onChangeStatus(doc.id, e.target.value)}
            className="text-[10px] border border-slate-200 rounded-lg px-2 py-1.5 text-slate-600 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-400 w-28"
            title="Change status (mock)"
          >
            <option value="verified">{t("vault_status_verified") || "Verified"}</option>
            <option value="uploaded">{t("vault_status_uploaded") || "Uploaded"}</option>
            <option value="pending_review">{t("vault_status_pending") || "Pending Review"}</option>
            <option value="rejected">{t("vault_status_rejected") || "Rejected"}</option>
          </select>
        )}

        <div className="flex items-center gap-1">
          <button className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition" title="Preview">
            <Eye className="h-4 w-4" />
          </button>
          <button className="p-1.5 text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition" title="Replace">
            <RefreshCw className="h-4 w-4" />
          </button>
          <button className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition" title="Download">
            <Download className="h-4 w-4" />
          </button>
          <div className="w-px h-4 bg-slate-200 mx-1"></div>
          <button
            onClick={() => onRemove(doc.id)}
            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
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
  const { language, t } = useApp();
  const readiness = getDocReadinessForScheme(scheme.requiredDocuments, documents);
  const barColor =
    readiness.readinessLabel === "Ready"
      ? "bg-emerald-500"
      : readiness.readinessLabel === "Partially Ready"
      ? "bg-amber-400"
      : "bg-rose-400";

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4 space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[10px] text-slate-400 uppercase tracking-wide font-semibold">{scheme.ministry}</p>
          <h4 className="text-xs font-bold text-slate-800 mt-0.5 leading-snug">{scheme.name}</h4>
        </div>
        <span
          className={`shrink-0 text-[10px] font-bold px-2 py-0.5 rounded-full border ${
            readiness.readinessLabel === "Ready"
              ? "bg-emerald-100 text-emerald-700 border-emerald-200"
              : readiness.readinessLabel === "Partially Ready"
              ? "bg-amber-100 text-amber-700 border-amber-200"
              : "bg-rose-100 text-rose-700 border-rose-200"
          }`}
        >
          {readiness.readinessLabel === "Ready" ? (t("vault_ready") || "Ready") :
           readiness.readinessLabel === "Partially Ready" ? (t("vault_partially_ready") || "Partially Ready") :
           (t("vault_missing_docs") || "Missing Documents")}
        </span>
      </div>

      <div>
        <div className="flex justify-between mb-1">
          <span className="text-[10px] text-slate-400">
            {t("vault_docs_ready_count", { available: readiness.totalAvailable, required: readiness.totalRequired }) ||
             (language === "hi"
               ? `${readiness.totalAvailable}/${readiness.totalRequired} दस्तावेज़ तैयार`
               : `${readiness.totalAvailable}/${readiness.totalRequired} documents ready`)}
          </span>
          <span className="text-[10px] font-bold text-slate-600">{readiness.readinessScore}%</span>
        </div>
        <div className="h-1.5 bg-slate-100 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full ${barColor}`}
            style={{ width: `${readiness.readinessScore}%` }}
          />
        </div>
      </div>

      {readiness.missingDocs.length > 0 && (
        <div className="space-y-1">
          <p className="text-[10px] font-semibold text-rose-700 uppercase tracking-wide">
            {t("vault_missing_prefix") || "Missing:"}
          </p>
          {readiness.missingDocs.map((doc) => (
            <div key={doc} className="flex items-center gap-1.5 text-[11px] text-rose-600">
              <XCircle className="h-3 w-3 shrink-0" />
              {doc}
            </div>
          ))}
        </div>
      )}

      {readiness.availableDocs.length > 0 && (
        <div className="space-y-1">
          <p className="text-[10px] font-semibold text-emerald-700 uppercase tracking-wide">
            {t("vault_available_prefix") || "Available:"}
          </p>
          {readiness.availableDocs.map((doc) => (
            <div key={doc} className="flex items-center gap-1.5 text-[11px] text-emerald-600">
              <CheckCircle className="h-3 w-3 shrink-0" />
              {doc}
            </div>
          ))}
        </div>
      )}

      <Link
        to={`/scheme/${scheme.id}`}
        className="inline-flex items-center gap-1 text-indigo-600 hover:text-indigo-800 text-xs font-semibold"
      >
        {t("vault_view_scheme_details") || "View scheme details"}
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
}

export default function Documents() {
  const {
    documents,
    addDocument,
    removeDocument,
    updateDocumentStatus,
    importDocuments,
    schemes,
    applications,
    savedSchemes,
    language,
    t,
  } = useApp();

  const [isLoading, setIsLoading] = useState(true);
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 800);
    return () => clearTimeout(timer);
  }, []);

  const isOnboarding = searchParams.get("onboarding") === "1";

  const [isModalOpen, setIsModalOpen] = useState(isOnboarding && documents.length === 0);
  const [prefillDocName, setPrefillDocName] = useState("");
  const [newDocName, setNewDocName] = useState("");
  const [newDocType, setNewDocType] = useState("Identity Proof");
  const [newDocStatus, setNewDocStatus] = useState("uploaded");
  const [newDocIssuer, setNewDocIssuer] = useState("");
  const [newDocExpiry, setNewDocExpiry] = useState("No Expiration");

  const [statusFilter, setStatusFilter] = useState("all");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [viewMode, setViewMode] = useState("list");
  const [isImporting, setIsImporting] = useState(false);
  const [aiChatOpen, setAiChatOpen] = useState(false);

  const handleDigiLockerImport = () => {
    setIsImporting(true);
    setTimeout(() => {
      importDocuments();
      setIsImporting(false);
    }, 1500);
  };

  const openUploadModalWithPrefill = (docName = "") => {
    setPrefillDocName(docName);
    setNewDocName(docName);
    setIsModalOpen(true);
  };

  const handleUploadSubmit = (e) => {
    e.preventDefault();
    if (!newDocName.trim()) {
return;
}
    addDocument(
      newDocName.trim(),
      newDocType,
      newDocIssuer.trim() || "Self-Uploaded",
      newDocExpiry.trim() || "No Expiration",
      newDocStatus
    );
    setNewDocName("");
    setNewDocIssuer("");
    setNewDocExpiry("No Expiration");
    setPrefillDocName("");
    setIsModalOpen(false);
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
    acc[doc.type].push(doc);
    return acc;
  }, {});

  // === AI Intelligence Dashboard Metrics ===
  const totalDocs = documents.length;
  const verifiedDocs = documents.filter((d) => d.status === "verified").length;
  const expiredDocs = documents.filter((d) => isExpired(d.expiryDate)).length;

  // Compute all unique missing docs across ALL schemes
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

  // Build context string for AI chat
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
    High: "bg-rose-50 border-rose-200 text-rose-700",
    Medium: "bg-amber-50 border-amber-200 text-amber-700",
    Low: "bg-slate-50 border-slate-200 text-slate-600",
  };

  // ── Skeleton loading guard ────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="space-y-5">
        <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm animate-pulse">
          <div className="h-5 w-40 bg-slate-200 rounded mb-2" />
          <div className="h-3 w-64 bg-slate-100 rounded" />
        </div>
        <div className="bg-slate-900 rounded-2xl p-5 animate-pulse space-y-3">
          <div className="h-5 w-48 bg-slate-700 rounded" />
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[1,2,3,4].map(i => (
              <div key={i} className="bg-white/5 border border-white/10 rounded-xl p-3 text-center">
                <div className="h-8 w-10 bg-slate-700 rounded mx-auto mb-1.5" />
                <div className="h-2.5 w-12 bg-slate-700 rounded mx-auto" />
              </div>
            ))}
          </div>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1,2,3,4,5,6].map(i => <DocumentCardSkeleton key={i} />)}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* Onboarding Banner */}
      {isOnboarding && documents.length === 0 && (
        <div className="bg-indigo-900 border border-indigo-700 rounded-2xl p-6 text-white space-y-4">
          <div className="flex items-center gap-3">
            <div className="bg-indigo-600 p-2 rounded-xl">
              <ShieldCheck className="h-5 w-5 text-white" />
            </div>
            <div>
              <h2 className="text-sm font-bold">Welcome! Let's Set Up Your Document Vault</h2>
              <p className="text-indigo-300 text-xs mt-0.5">
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
              <div key={i} className="flex items-center gap-2 bg-indigo-800/50 border border-indigo-700 rounded-xl px-3 py-2">
                <FileText className="h-3.5 w-3.5 text-indigo-300 shrink-0" />
                <div>
                  <p className="text-xs font-semibold text-white">{doc.name}</p>
                  <p className="text-[10px] text-indigo-300">{doc.type}</p>
                </div>
              </div>
            ))}
          </div>
          <button
            onClick={() => openUploadModalWithPrefill()}
            className="inline-flex items-center gap-2 bg-white text-indigo-900 px-5 py-2.5 rounded-xl text-xs font-bold shadow hover:bg-indigo-50 transition"
          >
            <UploadCloud className="h-4 w-4" />
            Start Uploading Documents
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* === Header === */}
      <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-lg font-bold text-slate-900">{t("vault_title")}</h1>
          <p className="text-slate-500 text-xs mt-1">{t("vault_desc")}</p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <button
            onClick={handleDigiLockerImport}
            disabled={isImporting}
            className="inline-flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow transition shrink-0 disabled:opacity-70"
          >
            <RefreshCw className={`h-4 w-4 ${isImporting ? "animate-spin" : ""}`} />
            {t("vault_btn_import") || "Import from DigiLocker"}
          </button>
          <button
            onClick={() => openUploadModalWithPrefill()}
            className="inline-flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow transition shrink-0"
          >
            <UploadCloud className="h-4 w-4" />
            {t("vault_btn_add") || "Add Document"}
          </button>
          <button
            onClick={() => setAiChatOpen(true)}
            className="inline-flex items-center gap-1.5 bg-slate-900 hover:bg-slate-800 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow transition shrink-0"
          >
            <Bot className="h-4 w-4 text-indigo-400" />
            {t("vault_btn_ai") || "SchemeAI"}
          </button>
        </div>
      </div>

      {/* === AI Document Intelligence Dashboard === */}
      <div className="bg-gradient-to-br from-slate-900 to-indigo-950 border border-indigo-900/50 rounded-2xl p-5 space-y-4 shadow-lg">
        <div className="flex items-center gap-2.5">
          <div className="bg-indigo-500/20 p-2 rounded-xl border border-indigo-400/20">
            <Sparkles className="h-5 w-5 text-indigo-400" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white">{t("vault_ai_doc_intel") || "AI Document Intelligence"}</h2>
            <p className="text-indigo-300 text-[10px] uppercase tracking-wider font-semibold">{t("vault_realtime_analysis") || "Real-time vault analysis"}</p>
          </div>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="bg-white/5 border border-white/10 rounded-xl p-3 text-center">
            <p className="text-2xl font-black text-white">{totalDocs}</p>
            <p className="text-[10px] text-indigo-300 font-semibold uppercase tracking-wide mt-1">{t("vault_total") || "Total"}</p>
          </div>
          <div className="bg-emerald-500/10 border border-emerald-400/20 rounded-xl p-3 text-center">
            <p className="text-2xl font-black text-emerald-400">{verifiedDocs}</p>
            <p className="text-[10px] text-emerald-300 font-semibold uppercase tracking-wide mt-1">{t("vault_status_verified") || "Verified"}</p>
          </div>
          <div className="bg-rose-500/10 border border-rose-400/20 rounded-xl p-3 text-center">
            <p className="text-2xl font-black text-rose-400">{expiredDocs}</p>
            <p className="text-[10px] text-rose-300 font-semibold uppercase tracking-wide mt-1">{t("vault_status_expired") || "Expired"}</p>
          </div>
          <div className={`rounded-xl p-3 text-center border ${vaultScore.score >= 75 ? "bg-emerald-500/10 border-emerald-400/20" : vaultScore.score >= 40 ? "bg-amber-500/10 border-amber-400/20" : "bg-rose-500/10 border-rose-400/20"}`}>
            <p className={`text-2xl font-black ${vaultScore.score >= 75 ? "text-emerald-400" : vaultScore.score >= 40 ? "text-amber-400" : "text-rose-400"}`}>{vaultScore.score}%</p>
            <p className={`text-[10px] font-semibold uppercase tracking-wide mt-1 ${vaultScore.score >= 75 ? "text-emerald-300" : vaultScore.score >= 40 ? "text-amber-300" : "text-rose-300"}`}>{t("vault_readiness") || "Readiness"}</p>
          </div>
        </div>

        {/* AI Recommendation Banner */}
        <div className="bg-indigo-600/20 border border-indigo-400/30 rounded-xl p-3.5 flex items-start gap-3">
          <Bot className="h-4 w-4 text-indigo-400 shrink-0 mt-0.5" />
          <div className="text-xs text-indigo-200 leading-relaxed">
            {expiredDocs > 0 && (
              <p className="mb-1">
                <span className="text-rose-300 font-bold">{t("vault_expired_detected") || "⚠ Expired Documents Detected:"}</span>{" "}
                {t("vault_expired_desc", { count: expiredDocs }) || `${expiredDocs} document${expiredDocs > 1 ? "s have" : " has"} expired. Renew them to maintain uninterrupted scheme eligibility.`}
              </p>
            )}
            {aiSuggestions.length > 0 ? (
              <p>
                <span className="text-emerald-300 font-bold">{t("vault_ai_insight") || "AI Insight:"}</span>{" "}
                {t("vault_ai_insight_desc", { doc: aiSuggestions[0].missingDoc, count: aiSuggestions[0].schemesUnlocked.length }) || `Upload ${aiSuggestions[0].missingDoc} to unlock ${aiSuggestions[0].schemesUnlocked.length} scheme${aiSuggestions[0].schemesUnlocked.length > 1 ? "s" : ""}. Your vault readiness will increase by an estimated 15–20%.`}
              </p>
            ) : (
              <p>
                <span className="text-emerald-300 font-bold">{t("vault_all_clear") || "All Clear:"}</span> {t("vault_all_clear_desc") || "Your document vault is fully prepared for all tracked schemes. Review the readiness scores below."}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* === AI Suggestions === */}
      {aiSuggestions.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <Zap className="h-4 w-4 text-amber-500" />
            <h2 className="text-sm font-bold text-slate-800">
              {t("vault_ai_suggestions_title") || (language === "hi" ? "AI अनुशंसाएं" : "AI Suggestions")} ({aiSuggestions.length})
            </h2>
          </div>
          <div className="grid sm:grid-cols-2 gap-3">
            {aiSuggestions.map((sug) => (
              <div
                key={sug.id}
                className="bg-white border border-slate-200 rounded-2xl p-4 space-y-3 hover:shadow-sm transition"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <div className="bg-amber-50 p-2 rounded-lg border border-amber-100">
                      <FileText className="h-4 w-4 text-amber-600" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-slate-800 leading-tight">{sug.missingDoc}</p>
                      <p className="text-[10px] text-slate-400 mt-0.5">{t("vault_missing_doc") || "Missing document"}</p>
                    </div>
                  </div>
                  <span className={`text-[9px] font-extrabold px-2 py-0.5 rounded-full border ${PRIORITY_STYLE[sug.priority]}`}>
                    {t(`vault_priority_${sug.priority.toLowerCase()}`) || sug.priority} {t("vault_priority_label") || "Priority"}
                  </span>
                </div>

                <div className="space-y-1.5 text-[10px]">
                  <div className="flex items-start gap-1.5 text-emerald-700">
                    <Sparkles className="h-3 w-3 shrink-0 mt-0.5" />
                    <span><strong>{t("vault_unlocks") || "Unlocks"}:</strong> {sug.schemesUnlocked.join(", ")}</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-slate-500">
                    <Clock className="h-3 w-3 shrink-0" />
                    <span>{t("vault_est_completion") || "Est. completion"}: {sug.eta}</span>
                  </div>
                </div>

                <button
                  onClick={() => openUploadModalWithPrefill(sug.missingDoc)}
                  className="w-full inline-flex items-center justify-center gap-1.5 bg-indigo-600 hover:bg-indigo-700 text-white py-2 rounded-xl text-[10px] font-bold shadow-sm transition"
                >
                  <UploadCloud className="h-3.5 w-3.5" />
                  {t("vault_btn_upload_specific", { docName: sug.missingDoc.split(" ")[0] }) || `Upload ${sug.missingDoc.split(" ")[0]}`}
                  <ArrowRight className="h-3 w-3" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* DigiLocker Trust Card */}
      <div className="bg-slate-900 border border-slate-800 p-5 rounded-2xl text-slate-300 flex items-start gap-3.5 shadow-sm">
        <ShieldCheck className="h-5 w-5 text-indigo-400 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1.5 leading-relaxed">
          <h3 className="font-bold text-white text-xs">
            {t("vault_trust_card_title") || "Simulated Digital Locker Registry Integrity"}
          </h3>
          <p>
            {t("vault_trust_card_desc") || "SchemeBridge emulates national registry linkages (Aadhaar, PAN, Caste Registry). Documents bearing the Verified Registry tag are cryptographically matched. Uploaded documents remain in demo-only sandboxed storage."}
          </p>
          <div className="flex flex-wrap gap-2 pt-1">
            {["DigiLocker", "State e-District Portal", "Manual Upload", "Government API (Future)", "Officer Verified (Future)"].map((src) => (
              <span key={src} className="text-[9px] font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                {src}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Vault Completeness Score */}
      {trackedSchemes.length > 0 && (
        <div className="bg-white border border-slate-200 rounded-2xl p-5">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-indigo-500" />
              <h2 className="text-sm font-bold text-slate-800">
                {t("vault_score_title") || "Vault Readiness Score"}
              </h2>
            </div>
            <span
              className={`text-xs font-bold px-3 py-1 rounded-full border ${
                vaultScore.score === 100
                  ? "bg-emerald-100 text-emerald-700 border-emerald-200"
                  : vaultScore.score >= 50
                  ? "bg-amber-100 text-amber-700 border-amber-200"
                  : "bg-rose-100 text-rose-700 border-rose-200"
              }`}
            >
              {vaultScore.score}% · {vaultScore.label}
            </span>
          </div>
          <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all ${
                vaultScore.score === 100
                  ? "bg-emerald-500"
                  : vaultScore.score >= 50
                  ? "bg-amber-400"
                  : "bg-rose-400"
              }`}
              style={{ width: `${vaultScore.score}%` }}
            />
          </div>
          <p className="text-[11px] text-slate-400 mt-2">
            {t("vault_score_desc", { docs: documents.length, schemes: trackedSchemes.length }) ||
              (language === "hi"
                ? `आपकी तिजोरी में ${documents.length} दस्तावेज़ और ${trackedSchemes.length} ट्रैक की गई योजनाएं हैं।`
                : `Based on ${documents.length} document${documents.length !== 1 ? "s" : ""} in your vault across ${trackedSchemes.length} tracked scheme${trackedSchemes.length !== 1 ? "s" : ""}.`)}
          </p>
        </div>
      )}

      {/* Document Vault */}
      <div className="space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 pb-3">
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-bold text-slate-700">
              {t("vault_your_docs") || "Your Documents"} ({filteredDocs.length})
            </h2>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="absolute left-2.5 top-1.5 h-3.5 w-3.5 text-slate-400" />
              <input
                type="text"
                placeholder={t("vault_search_placeholder") || "Search..."}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8 pr-3 py-1.5 text-[11px] border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 w-32 sm:w-48 transition"
              />
            </div>

            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="px-2 py-1.5 text-[11px] border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 bg-white"
            >
              <option value="all">{t("vault_cat_all") || "All Categories"}</option>
              <option value="Identity Proof">{t("vault_cat_identity") || "Identity Proof"}</option>
              <option value="Financial Proof">{t("vault_cat_financial") || "Financial Proof"}</option>
              <option value="Category Proof">{t("vault_cat_category") || "Category Proof"}</option>
              <option value="Property Proof">{t("vault_cat_property") || "Property Proof"}</option>
              <option value="Academic Proof">{t("vault_cat_academic") || "Academic Proof"}</option>
              <option value="Domicile Proof">{t("vault_cat_domicile") || "Domicile Proof"}</option>
            </select>

            <div className="flex gap-1 bg-slate-100 p-1 rounded-xl shrink-0 select-none">
              {[
                { id: "all", label: t("vault_status_all") },
                { id: "verified", label: t("vault_status_verified") },
                { id: "pending", label: t("vault_status_pending") },
                { id: "rejected", label: t("vault_status_rejected") },
              ].map((f) => (
                <button
                  key={f.id}
                  onClick={() => setStatusFilter(f.id)}
                  className={`px-2.5 py-1 rounded-lg text-[10px] font-semibold capitalize transition ${
                    statusFilter === f.id ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
                  }`}
                >
                  {f.label}
                </button>
              ))}
            </div>

            <div className="flex gap-1 bg-slate-100 p-1 rounded-xl shrink-0 ml-1">
              <button
                onClick={() => setViewMode("list")}
                className={`p-1 rounded-lg transition ${viewMode === "list" ? "bg-white shadow-sm text-slate-900" : "text-slate-400"}`}
                title="List View"
              >
                <List className="h-3.5 w-3.5" />
              </button>
              <button
                onClick={() => setViewMode("grid")}
                className={`p-1 rounded-lg transition ${viewMode === "grid" ? "bg-white shadow-sm text-slate-900" : "text-slate-400"}`}
                title="Grid View"
              >
                <Grid className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        </div>

        {filteredDocs.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-2xl p-10 text-center space-y-2">
            <FileText className="h-10 w-10 text-slate-300 mx-auto" />
            <p className="font-semibold text-slate-600 text-sm">
              {t("vault_empty_title") || "No documents found"}
            </p>
            <p className="text-slate-400 text-xs">
              {t("vault_empty_desc") || "No records correspond to the selected filter criteria."}
            </p>
          </div>
        ) : Object.keys(docsByType).length > 0 ? (
          Object.entries(docsByType).map(([type, docs]) => (
            <div key={type} className="space-y-2">
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider px-1">
                {type}
              </p>
              <div className={viewMode === "grid" ? "grid sm:grid-cols-2 gap-3" : "space-y-2"}>
                {docs.map((doc) => (
                  <DocumentCard
                    key={doc.id}
                    doc={doc}
                    onRemove={removeDocument}
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

      {/* Scheme Readiness Section */}
      {trackedSchemes.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-sm font-bold text-slate-700">
            {t("vault_readiness_title") || "Readiness for Your Tracked Schemes"}
          </h2>
          <div className="grid sm:grid-cols-2 gap-3">
            {trackedSchemes.map((scheme) => (
              <SchemeReadinessCard key={scheme.id} scheme={scheme} documents={documents} />
            ))}
          </div>
        </div>
      )}

      {trackedSchemes.length === 0 && (
        <div className="bg-white border border-dashed border-slate-200 rounded-2xl p-8 text-center space-y-2">
          <p className="text-xs text-slate-400">
            {t("vault_no_tracked_desc") || "Save or apply to schemes to see document readiness here."}
          </p>
          <Link
            to="/recommendations"
            className="text-indigo-600 text-xs font-semibold inline-flex items-center gap-1 hover:text-indigo-800"
          >
            {t("vault_browse_schemes") || "Browse schemes"}
            <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      )}

      {/* Upload Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm"
            onClick={() => setIsModalOpen(false)}
          />
          <div className="relative bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-md w-full overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <UploadCloud className="h-5 w-5 text-indigo-400" />
                <h3 className="font-bold text-sm">
                  {t("vault_modal_title") || "Add Document to Vault"}
                </h3>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-white p-1 rounded-lg transition"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {prefillDocName && (
              <div className="bg-indigo-50 border-b border-indigo-100 px-6 py-3 flex items-center gap-2 text-xs text-indigo-700 font-semibold">
                <Sparkles className="h-3.5 w-3.5 text-indigo-500" />
                AI Suggestion: <span className="font-black text-indigo-900">{prefillDocName}</span>
              </div>
            )}

            <form onSubmit={handleUploadSubmit} className="p-6 space-y-4 text-xs">
              <div>
                <label htmlFor="newDocName" className="block font-semibold text-slate-600 mb-1.5">
                  {t("vault_modal_doc_name") || "Document / Certificate Name *"}
                </label>
                <input
                  id="newDocName"
                  type="text"
                  required
                  placeholder="e.g. Income Certificate 2026"
                  value={newDocName}
                  onChange={(e) => setNewDocName(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 rounded-lg transition"
                />
              </div>

              <div>
                <label htmlFor="newDocIssuer" className="block font-semibold text-slate-600 mb-1.5">
                  {t("vault_modal_issuer") || "Issuing Authority (Issuer) *"}
                </label>
                <input
                  id="newDocIssuer"
                  type="text"
                  required
                  placeholder="e.g. UIDAI, State Revenue Board"
                  value={newDocIssuer}
                  onChange={(e) => setNewDocIssuer(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 rounded-lg transition"
                />
              </div>

              <div>
                <label htmlFor="newDocExpiry" className="block font-semibold text-slate-600 mb-1.5">
                  {t("vault_modal_expiry") || "Expiration Date (or \"No Expiration\")"}
                </label>
                <input
                  id="newDocExpiry"
                  type="text"
                  placeholder="e.g. 2028-12-31, No Expiration"
                  value={newDocExpiry}
                  onChange={(e) => setNewDocExpiry(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 rounded-lg transition"
                />
              </div>

              <div>
                <label htmlFor="newDocType" className="block font-semibold text-slate-600 mb-1.5">
                  {t("vault_modal_category") || "Document Category"}
                </label>
                <select
                  id="newDocType"
                  value={newDocType}
                  onChange={(e) => setNewDocType(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 rounded-lg transition"
                >
                  <option value="Identity Proof">{t("vault_cat_identity_full") || "Identity Proof (Aadhaar, Voter ID)"}</option>
                  <option value="Financial Proof">{t("vault_cat_financial_full") || "Financial Proof (Income, Bank Passbook)"}</option>
                  <option value="Category Proof">{t("vault_cat_category_full") || "Category Proof (Caste Certificate)"}</option>
                  <option value="Property Proof">{t("vault_cat_property_full") || "Property Proof (Khatauni, Land Records)"}</option>
                  <option value="Academic Proof">{t("vault_cat_academic_full") || "Academic Proof (Marksheet, College Receipt)"}</option>
                  <option value="Domicile Proof">{t("vault_cat_domicile_full") || "Domicile Proof (Ration Card, Address Proof)"}</option>
                </select>
              </div>

              <div>
                <label htmlFor="newDocStatus" className="block font-semibold text-slate-600 mb-1.5">
                  {t("vault_modal_status_label") || "Verification Status (mock)"}
                </label>
                <select
                  id="newDocStatus"
                  value={newDocStatus}
                  onChange={(e) => setNewDocStatus(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 rounded-lg transition"
                >
                  <option value="uploaded">{t("vault_modal_status_uploaded") || "Uploaded (pending verification)"}</option>
                  <option value="verified">{t("vault_status_verified") || "Verified"}</option>
                  <option value="pending_review">{t("vault_status_pending") || "Pending Review"}</option>
                  <option value="rejected">{t("vault_status_rejected") || "Rejected"}</option>
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl font-semibold transition"
                >
                  {t("gen_cancel") || "Cancel"}
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold shadow-sm transition"
                >
                  {t("vault_modal_submit") || "Add to Vault"}
                </button>
              </div>
            </form>
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
