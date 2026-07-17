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

function DocumentCard({ doc, onRemove, onChangeStatus, linkedSchemes = [], viewMode = "list" }) {
  const meta = STATUS_META[doc.status] || STATUS_META.unlinked;
  const StatusIcon = meta.icon;
  const typeColor = TYPE_ICON_COLOR[doc.type] || "bg-gray-100 text-gray-500";
  const expired = isExpired(doc.expiryDate);
  const sourceBadgeClass = SOURCE_BADGE[doc.source] || SOURCE_BADGE["Manual Upload"];

  const isGrid = viewMode === "grid";

  return (
    <div className={`bg-white border ${expired ? "border-red-200 shadow-sm" : "border-gray-200"} rounded-2xl p-4 flex ${isGrid ? "flex-col" : "flex-col sm:flex-row sm:items-center"} gap-4 hover:shadow-md transition relative`}>
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
              <h3 className="font-bold text-gray-800 text-sm truncate">{doc.name}</h3>
              {doc.status === "verified" && !expired && (
                <span className="text-[10px] bg-india-green/10 text-india-green px-1.5 py-0.5 rounded font-extrabold border border-india-green/20 select-none flex items-center gap-1">
                  <ShieldCheck className="h-3 w-3" /> Verified Registry
                </span>
              )}
            </div>

            {/* Verification Source Badge */}
            <div className="flex flex-wrap items-center gap-1.5">
              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded border ${sourceBadgeClass}`}>
                {doc.source || "Manual Upload"}
              </span>
              <span className="text-[10px] font-bold px-1.5 py-0.5 rounded border bg-gray-50 text-gray-500 border-gray-200">
                OCR: Coming Soon
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[10px] text-gray-400 font-medium">
              <span>Issuer: <strong className="text-gray-600">{doc.issuer || "Self-Uploaded"}</strong></span>
              <span>&bull;</span>
              <span className={expired ? "text-red-500 font-bold" : ""}>
                Expires: <strong className={expired ? "text-red-600" : "text-gray-600"}>{doc.expiryDate || "No Expiration"}</strong>
              </span>
              {doc.date && (
                <>
                  <span>&bull;</span>
                  <span>Uploaded: {doc.date}</span>
                </>
              )}
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
            <select
              value={doc.status}
              onChange={(e) => onChangeStatus(doc.id, e.target.value)}
              className="text-[10px] border border-gray-200 rounded-lg px-2 py-0.5 text-gray-600 bg-white focus:outline-none focus:ring-2 focus:ring-government-blue"
              title="Change status (mock)"
            >
              <option value="verified">Verified</option>
              <option value="uploaded">Uploaded</option>
              <option value="pending_review">Pending Review</option>
              <option value="rejected">Rejected</option>
            </select>
          </div>
        )}

        {isGrid && (
          <select
            value={doc.status}
            onChange={(e) => onChangeStatus(doc.id, e.target.value)}
            className="text-[10px] border border-gray-200 rounded-lg px-2 py-1.5 text-gray-600 bg-white focus:outline-none focus:ring-2 focus:ring-government-blue w-28"
            title="Change status (mock)"
          >
            <option value="verified">Verified</option>
            <option value="uploaded">Uploaded</option>
            <option value="pending_review">Pending Review</option>
            <option value="rejected">Rejected</option>
          </select>
        )}

        <div className="flex items-center gap-1">
          <button className="p-1.5 text-gray-400 hover:text-government-blue hover:bg-government-blue/10 rounded-lg transition" title="Preview">
            <Eye className="h-4 w-4" />
          </button>
          <button className="p-1.5 text-gray-400 hover:text-india-green hover:bg-india-green/10 rounded-lg transition" title="Replace">
            <RefreshCw className="h-4 w-4" />
          </button>
          <button className="p-1.5 text-gray-400 hover:text-government-blue hover:bg-government-blue/10 rounded-lg transition" title="Download">
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
    <div className="bg-white border border-gray-200 rounded-2xl p-4 space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[10px] text-gray-400 uppercase tracking-wider font-semibold">{scheme.ministry}</p>
          <h4 className="text-xs font-bold text-gray-800 mt-0.5 leading-snug">{scheme.name}</h4>
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
    High: "bg-red-50 border-red-200 text-red-700",
    Medium: "bg-saffron/10 border-saffron/20 text-saffron-dark",
    Low: "bg-gray-50 border-gray-200 text-gray-600",
  };

  // ── Skeleton loading guard ────────────────────────────────────────────────
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
          <p className="text-gray-500 text-xs mt-1">Manage your documents and certificates</p>
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
          <button
            onClick={() => setAiChatOpen(true)}
            className="inline-flex items-center gap-1.5 bg-gray-900 hover:bg-gray-800 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow transition shrink-0"
          >
            <Bot className="h-4 w-4 text-government-blue-light" />
            SchemeAI
          </button>
        </div>
      </div>

      {/* === AI Document Intelligence Dashboard === */}
      <div className="bg-gradient-to-br from-government-blue to-government-blue-dark border border-government-blue/20 rounded-2xl p-5 space-y-4 shadow-md">
        <div className="flex items-center gap-2.5">
          <div className="bg-white/10 p-2 rounded-xl">
            <Sparkles className="h-5 w-5 text-saffron" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white">AI Document Intelligence</h2>
            <p className="text-saffron/80 text-[10px] uppercase tracking-widest font-semibold">Real-time vault analysis</p>
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
            {aiSuggestions.length > 0 ? (
              <p>
                <span className="text-india-green font-bold">AI Insight:</span> Upload {aiSuggestions[0].missingDoc} to unlock {aiSuggestions[0].schemesUnlocked.length} scheme{aiSuggestions[0].schemesUnlocked.length > 1 ? "s" : ""}. Your vault readiness will increase by an estimated 15–20%.
              </p>
            ) : (
              <p>
                <span className="text-india-green font-bold">All Clear:</span> Your document vault is fully prepared for all tracked schemes. Review the readiness scores below.
              </p>
            )}
          </div>
        </div>
      </div>

      {/* === AI Suggestions === */}
      {aiSuggestions.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <Zap className="h-4 w-4 text-saffron-dark" />
            <h2 className="text-sm font-bold text-gray-800">AI Suggestions ({aiSuggestions.length})</h2>
          </div>
          <div className="grid sm:grid-cols-2 gap-3">
            {aiSuggestions.map((sug) => (
              <div
                key={sug.id}
                className="bg-white border border-gray-200 rounded-2xl p-4 space-y-3 hover:shadow-md transition"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <div className="bg-saffron/10 p-2 rounded-lg border border-saffron/20">
                      <FileText className="h-4 w-4 text-saffron-dark" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-gray-800 leading-tight">{sug.missingDoc}</p>
                      <p className="text-[10px] text-gray-400 mt-0.5">Missing document</p>
                    </div>
                  </div>
                  <span className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full border ${PRIORITY_STYLE[sug.priority]}`}>
                    {sug.priority} Priority
                  </span>
                </div>

                <div className="space-y-1.5 text-[10px]">
                  <div className="flex items-start gap-1.5 text-india-green">
                    <Sparkles className="h-3 w-3 shrink-0 mt-0.5" />
                    <span><strong>Unlocks:</strong> {sug.schemesUnlocked.join(", ")}</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-gray-500">
                    <Clock className="h-3 w-3 shrink-0" />
                    <span>Est. completion: {sug.eta}</span>
                  </div>
                </div>

                <button
                  onClick={() => openUploadModalWithPrefill(sug.missingDoc)}
                  className="w-full inline-flex items-center justify-center gap-1.5 bg-government-blue hover:bg-government-blue-dark text-white py-2 rounded-xl text-[10px] font-bold shadow-sm transition"
                >
                  <UploadCloud className="h-3.5 w-3.5" />
                  Upload {sug.missingDoc.split(" ")[0]}
                  <ArrowRight className="h-3 w-3" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* DigiLocker Trust Card */}
      <div className="bg-gray-900 border border-gray-800 p-5 rounded-2xl text-gray-300 flex items-start gap-3.5 shadow-sm">
        <ShieldCheck className="h-5 w-5 text-government-blue-light shrink-0 mt-0.5" />
        <div className="text-xs space-y-1.5 leading-relaxed">
          <h3 className="font-bold text-white text-xs">
            Simulated Digital Locker Registry Integrity
          </h3>
          <p>
            SchemeBridge emulates national registry linkages (Aadhaar, PAN, Caste Registry). Documents bearing the Verified Registry tag are cryptographically matched. Uploaded documents remain in demo-only sandboxed storage.
          </p>
          <div className="flex flex-wrap gap-2 pt-1">
            {["DigiLocker", "State e-District Portal", "Manual Upload", "Government API (Future)", "Officer Verified (Future)"].map((src) => (
              <span key={src} className="text-[10px] font-bold px-2 py-0.5 rounded bg-gray-800 text-gray-300 border border-gray-700">
                {src}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Vault Completeness Score */}
      {trackedSchemes.length > 0 && (
        <div className="bg-white border border-gray-200 rounded-2xl p-5">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-government-blue" />
              <h2 className="text-sm font-bold text-gray-800">
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
              {vaultScore.score}% · {vaultScore.label}
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
            Based on {documents.length} document{documents.length !== 1 ? "s" : ""} in your vault across {trackedSchemes.length} tracked scheme{trackedSchemes.length !== 1 ? "s" : ""}.
          </p>
        </div>
      )}

      {/* Document Vault */}
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

      {/* Upload Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-gray-900/60 backdrop-blur-sm"
            onClick={() => setIsModalOpen(false)}
          />
          <div className="relative bg-white rounded-2xl border border-gray-200 shadow-2xl max-w-md w-full overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-gradient-to-r from-government-blue to-government-blue-dark text-white p-5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <UploadCloud className="h-5 w-5 text-saffron" />
                <h3 className="font-bold text-sm">
                  Add Document to Digital Locker
                </h3>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
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

            <form onSubmit={handleUploadSubmit} className="p-6 space-y-4 text-xs">
              <div>
                <label htmlFor="newDocName" className="block font-semibold text-gray-600 mb-1.5">
                  Document / Certificate Name *
                </label>
                <input
                  id="newDocName"
                  type="text"
                  required
                  placeholder="e.g. Income Certificate 2026"
                  value={newDocName}
                  onChange={(e) => setNewDocName(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-lg transition"
                />
              </div>

              <div>
                <label htmlFor="newDocIssuer" className="block font-semibold text-gray-600 mb-1.5">
                  Issuing Authority (Issuer) *
                </label>
                <input
                  id="newDocIssuer"
                  type="text"
                  required
                  placeholder="e.g. UIDAI, State Revenue Board"
                  value={newDocIssuer}
                  onChange={(e) => setNewDocIssuer(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-lg transition"
                />
              </div>

              <div>
                <label htmlFor="newDocExpiry" className="block font-semibold text-gray-600 mb-1.5">
                  Expiration Date (or "No Expiration")
                </label>
                <input
                  id="newDocExpiry"
                  type="text"
                  placeholder="e.g. 2028-12-31, No Expiration"
                  value={newDocExpiry}
                  onChange={(e) => setNewDocExpiry(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-lg transition"
                />
              </div>

              <div>
                <label htmlFor="newDocType" className="block font-semibold text-gray-600 mb-1.5">
                  Document Category
                </label>
                <select
                  id="newDocType"
                  value={newDocType}
                  onChange={(e) => setNewDocType(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-lg transition"
                >
                  <option value="Identity Proof">Identity Proof (Aadhaar, Voter ID)</option>
                  <option value="Financial Proof">Financial Proof (Income, Bank Passbook)</option>
                  <option value="Category Proof">Category Proof (Caste Certificate)</option>
                  <option value="Property Proof">Property Proof (Khatauni, Land Records)</option>
                  <option value="Academic Proof">Academic Proof (Marksheet, College Receipt)</option>
                  <option value="Domicile Proof">Domicile Proof (Ration Card, Address Proof)</option>
                </select>
              </div>

              <div>
                <label htmlFor="newDocStatus" className="block font-semibold text-gray-600 mb-1.5">
                  Verification Status (mock)
                </label>
                <select
                  id="newDocStatus"
                  value={newDocStatus}
                  onChange={(e) => setNewDocStatus(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-lg transition"
                >
                  <option value="uploaded">Uploaded (pending verification)</option>
                  <option value="verified">Verified</option>
                  <option value="pending_review">Pending Review</option>
                  <option value="rejected">Rejected</option>
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 border border-gray-200 hover:bg-gray-50 text-gray-600 rounded-xl font-semibold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-government-blue hover:bg-government-blue-dark text-white rounded-xl font-bold shadow-sm transition"
                >
                  Add to Vault
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
