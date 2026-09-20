import React, { useState, useMemo, useEffect, useCallback } from "react";
import {
  PlusCircle,
  Trash2,
  ChevronDown,
  ChevronUp,
  AlertTriangle,
  Copy,
  X,
  FileWarning,
  FileCheck,
  Calendar,
  Award,
  Folder,
  BarChart3,
  Clock,
  Sparkles,
  Upload,
  FileText,
  Loader2,
  CheckCircle2,
  Info,
  RefreshCw
} from "lucide-react";
import SearchBar from "@components/ui/SearchBar";
import StatusBadge from "@components/ui/StatusBadge";
import EmptyState from "@components/ui/EmptyState";
import {
  getSchemes,
  searchSchemes,
  extractCircularScheme,
  createScheme,
  updateScheme,
  updateStatus,
  deleteScheme
} from "@services/schemeService";
import { getSchemeDocumentChecklist } from "@services/applicationService";
import adminService from "@services/adminService";

const CORE_CATEGORIES = [ 
  "Agriculture",
  "Education",
  "Healthcare",
  "Women & Child Welfare",
  "Employment",
  "Housing",
  "Entrepreneurship",
  "Pension",
  "Disability",
  "Social Welfare",
  "Financial Assistance",
  "Skill Development",
  "Minority Welfare",
  "Student Scholarships"
];

// Normalize real backend SchemeResponse into view-model fields without fabricating data
export function normalizeScheme(scheme) {
  if (!scheme || typeof scheme !== "object") return null;

  const id = scheme.id || scheme.schemeCode || "";
  const schemeCode = scheme.schemeCode || "—";
  const slug = scheme.slug || "—";

  // Title / Name
  const name =
    scheme.title?.english ||
    scheme.title?.en ||
    scheme.title?.hi ||
    scheme.title?.hindi ||
    (typeof scheme.title === "string" ? scheme.title : "") ||
    scheme.name ||
    schemeCode ||
    "Untitled Scheme";

  // Description / Short Description
  const description =
    scheme.shortDescription?.english ||
    scheme.shortDescription?.en ||
    scheme.description?.english ||
    scheme.description?.en ||
    (typeof scheme.shortDescription === "string" ? scheme.shortDescription : "") ||
    (typeof scheme.description === "string" ? scheme.description : "") ||
    scheme.description ||
    "—";

  // Category
  const category =
    scheme.category?.name ||
    scheme.category?.code ||
    scheme.categoryCode ||
    scheme.category ||
    "General";

  // Ministry & Department
  const ministry =
    scheme.ministry ||
    (scheme.schemeLevel === "CENTRAL"
      ? "Central Government"
      : scheme.stateOrUt
      ? `State Government (${scheme.stateOrUt})`
      : "State Government");

  const department = scheme.department || scheme.stateOrUt || "—";

  // Status (normalized: 'published', 'draft', 'inactive', 'archived')
  let status = "draft";
  if (scheme.status) {
    const raw = String(scheme.status).toUpperCase();
    if (raw === "ACTIVE" || raw === "PUBLISHED") status = "published";
    else if (raw === "DRAFT") status = "draft";
    else if (raw === "INACTIVE") status = "inactive";
    else if (raw === "ARCHIVED") status = "archived";
    else status = raw.toLowerCase();
  }

  // Official Link
  const officialLink =
    scheme.source?.sourceUrl ||
    scheme.applicationInfo?.applicationUrl ||
    scheme.officialSourceUrl ||
    scheme.applicationUrl ||
    scheme.applicationInfo?.officialPortalUrl ||
    scheme.officialUrl ||
    scheme.officialLink ||
    "—";

  // Source Type
  const sourceType = scheme.schemeLevel === "STATE" ? "State" : "Central";

  // Benefits
  let benefits = [];
  if (Array.isArray(scheme.benefits)) {
    benefits = scheme.benefits.map((b) => {
      if (typeof b === "string") return b;
      if (b.description?.english) return b.description.english;
      if (b.benefitType) return `${b.benefitType}${b.amount ? `: ₹${b.amount}` : ""}`;
      return JSON.stringify(b);
    });
  }

  // Required Documents
  let requiredDocuments = [];
  if (Array.isArray(scheme.requiredDocuments)) {
    requiredDocuments = scheme.requiredDocuments.map((d) => {
      if (typeof d === "string") return d;
      return d.documentName?.english || d.documentCode || d.name || String(d);
    });
  } else if (Array.isArray(scheme.documents)) {
    requiredDocuments = scheme.documents.map((d) => {
      if (typeof d === "string") return d;
      return d.documentCode || d.documentName || String(d);
    });
  }

  // Steps
  let steps = [];
  if (Array.isArray(scheme.steps)) {
    steps = scheme.steps.map((st) => {
      if (typeof st === "string") return st;
      return `${st.stepNumber ? `${st.stepNumber}. ` : ""}${st.title || ""}${st.description ? ` - ${st.description}` : ""}`.trim();
    });
  }

  // FAQs
  const faqs = Array.isArray(scheme.faqs) ? scheme.faqs : [];

  // Tags
  const tags = Array.isArray(scheme.tags)
    ? scheme.tags
    : [scheme.schemeType, scheme.beneficiaryType, scheme.schemeLevel].filter(Boolean);

  // Deadline
  let deadline = "—";
  const appInfo = scheme.applicationInfo;
  const rawDeadline =
    appInfo?.deadline ||
    appInfo?.applicationEndDate ||
    appInfo?.closingDate ||
    appInfo?.lastDate ||
    appInfo?.applicationDeadline ||
    scheme.deadline ||
    scheme.applicationDeadline ||
    scheme.closingDate ||
    scheme.lastDate;

  if (rawDeadline && rawDeadline !== "—") {
    try {
      const d = new Date(rawDeadline);
      if (!isNaN(d.getTime())) {
        deadline = d.toISOString().split("T")[0];
      } else {
        deadline = String(rawDeadline);
      }
    } catch {
      deadline = String(rawDeadline);
    }
  }

  // Applications count and Approval rate from backend (if present)
  const applicationsCount = typeof scheme.applicationsCount === "number"
    ? scheme.applicationsCount
    : (typeof scheme.appCount === "number" ? scheme.appCount : null);

  const approvalRate = typeof scheme.approvalRate === "number"
    ? scheme.approvalRate
    : null;

  // Last Updated
  const lastUpdated = scheme.updatedAt
    ? new Date(scheme.updatedAt).toISOString().split("T")[0]
    : scheme.createdAt
    ? new Date(scheme.createdAt).toISOString().split("T")[0]
    : "—";

  return {
    id,
    schemeCode,
    slug,
    name,
    description,
    category,
    ministry,
    department,
    status,
    officialLink,
    sourceType,
    benefits,
    requiredDocuments,
    steps,
    faqs,
    tags,
    deadline,
    applicationsCount,
    approvalRate,
    lastUpdated,
    rawBackendScheme: scheme,
  };
}

function getQualityWarnings(scheme) {
  const warnings = [];
  if (!scheme) return warnings;

  const description = scheme.description || "";
  if (description === "—" || description.length < 30) {
    warnings.push("Objective is too short or missing (< 30 chars).");
  }
  const rawScheme = scheme.rawBackendScheme || scheme;
  const hasRawDocs = Array.isArray(rawScheme.requiredDocuments) && rawScheme.requiredDocuments.length > 0;
  const hasParsedDocs = Array.isArray(scheme.requiredDocuments) && scheme.requiredDocuments.length > 0;
  const hasRules = !!(rawScheme.eligibilityRules?.conditions?.length || rawScheme.eligibilityRules?.groups?.length);
  const hasCategory = !!(rawScheme.category?.name || scheme.category);

  if (!hasRawDocs && !hasParsedDocs && !hasRules && !hasCategory) {
    warnings.push("No required documents listed.");
  }
  const benefits = scheme.benefits || [];
  if (!Array.isArray(benefits) || benefits.length === 0) {
    warnings.push("No benefits listed.");
  }
  const officialLink = scheme.officialLink || rawScheme.source?.sourceUrl || rawScheme.applicationInfo?.applicationUrl || rawScheme.officialSourceUrl || "";
  if (!officialLink || officialLink === "—" || !officialLink.startsWith("http")) {
    warnings.push("Official source link not indexed in catalog.");
  }
  return warnings;
}

export default function SchemeManagementConsole({
  applications = []
}) {
  const [backendSchemes, setBackendSchemes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [sortOrder, setSortOrder] = useState("latest");
  const [expandedId, setExpandedId] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // Sole authoritative source of truth: real database records from backend API
  const schemes = backendSchemes;

  // Live Applications State (authoritative review queue for metrics)
  const [liveApplications, setLiveApplications] = useState([]);

  const fetchLiveApplications = useCallback(async () => {
    try {
      const res = await adminService.getApplicationsQueue({ size: 500 });
      if (!res.error && res.data?.content) {
        setLiveApplications(res.data.content);
      }
    } catch (err) {
      console.warn("Could not fetch live applications for metrics:", err);
    }
  }, []);

  const fetchLiveSchemes = useCallback(async () => {
    setLoading(true);
    setError(null);
    fetchLiveApplications();
    try {
      const categoryParam = categoryFilter !== "all" ? categoryFilter : undefined;
      const statusParam = statusFilter !== "all" ? statusFilter.toUpperCase() : undefined;
      const sortField = (sortOrder === "latest" || sortOrder === "oldest") ? "updatedAt" : "schemeCode";
      const sortDir = sortOrder === "oldest" ? "asc" : "desc";

      const res = await searchSchemes({
        q: search.trim() || undefined,
        categoryCode: categoryParam,
        status: statusParam,
        page: currentPage - 1,
        size: pageSize,
        sort: sortField,
        direction: sortDir
      });

      if (!res.error && res.data) {
        const rawList = res.data.content || (Array.isArray(res.data) ? res.data : []);
        const normalized = rawList.map(normalizeScheme).filter(Boolean);
        setBackendSchemes(normalized);
        const total = res.data.totalElements ?? rawList.length;
        setTotalElements(total);
        setTotalPages(res.data.totalPages ?? Math.max(1, Math.ceil(total / pageSize)));
      } else {
        let msg = res.message || "Failed to load schemes from backend.";
        if (res.status === 401 || res.status === 403) {
          msg = "You are not authorized to view schemes. Please verify admin privileges.";
        } else if (msg.toLowerCase().includes("timed out") || msg.toLowerCase().includes("timeout")) {
          msg = "Scheme service request timed out. Please check your connection.";
        }
        setError(msg);
        setBackendSchemes([]);
        setTotalElements(0);
        setTotalPages(1);
      }
    } catch (err) {
      console.error("Failed to load schemes from backend", err);
      setError(err.message || "Network error while connecting to Scheme Service.");
      setBackendSchemes([]);
      setTotalElements(0);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  }, [search, statusFilter, categoryFilter, sortOrder, currentPage, pageSize, fetchLiveApplications]);

  useEffect(() => {
    fetchLiveSchemes();
  }, [fetchLiveSchemes]);

  // Form State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState("add");
  const [editingScheme, setEditingScheme] = useState(null);
  const [formSubmitting, setFormSubmitting] = useState(false);

  // Canonical Documents State (Issue 1)
  const [formCanonicalChecklist, setFormCanonicalChecklist] = useState(null);
  const [loadingDocs, setLoadingDocs] = useState(false);

  const [formSchemeCode, setFormSchemeCode] = useState("");
  const [formName, setFormName] = useState("");
  const [formMinistry, setFormMinistry] = useState("");
  const [formDepartment, setFormDepartment] = useState("");
  const [formCategory, setFormCategory] = useState("Social Welfare");
  const [formStatus, setFormStatus] = useState("draft");
  const [formDesc, setFormDesc] = useState("");
  const [formMinAge, setFormMinAge] = useState(18);
  const [formMaxAge, setFormMaxAge] = useState(100);
  const [formMaxIncome, setFormMaxIncome] = useState(300000);
  const [formOccupations, setFormOccupations] = useState([]);
  const [formCastes, setFormCastes] = useState([]);
  const [formGenders, setFormGenders] = useState([]);
  const [formDocs, setFormDocs] = useState("");
  const [formSteps, setFormSteps] = useState("");
  const [formBenefits, setFormBenefits] = useState("");
  const [formOfficialLink, setFormOfficialLink] = useState("");
  const [formSourceType, setFormSourceType] = useState("Central");
  const [formDeadline, setFormDeadline] = useState("");
  const [formApprovalRate, setFormApprovalRate] = useState("");
  const [formErrors, setFormErrors] = useState({});

  // AI Circular Ingestion State
  const [circularFile, setCircularFile] = useState(null);
  const [extracting, setExtracting] = useState(null); // null | 'uploading' | 'success' | 'manual_review' | 'error'
  const [extractionMeta, setExtractionMeta] = useState(null);
  const [extractError, setExtractError] = useState("");

  // Selection configurations
  const occupationsList = ["Farmer", "Student", "Self-Employed", "Unemployed", "Salaried"];
  const castesList = ["General", "OBC", "SC", "ST"];
  const gendersList = ["Male", "Female", "Other"];

  // Enrich schemes with real dynamic metrics (zero fabricated values)
  const enrichedSchemes = useMemo(() => {
    const effectiveApps = (liveApplications && liveApplications.length > 0)
      ? liveApplications
      : (applications && applications.length > 0 ? applications : []);

    return (schemes || []).map((s) => {
      // Find matching live applications for this scheme
      const schemeApps = effectiveApps.filter(
        (a) => a.schemeCode === s.schemeCode || a.schemeId === s.id || a.schemeCode === s.id
      );

      // 1. Applications count: prefer live queue count if available, else backend count
      let appCount = s.applicationsCount != null ? s.applicationsCount : 0;
      if (effectiveApps.length > 0) {
        appCount = schemeApps.length;
      }

      // 2. Authoritative dynamic approval rate calculation: approved / decided * 100 (zero fabrication)
      // Decided applications = APPROVED + REJECTED. Pending / Under review are excluded.
      let approvalRate = typeof s.approvalRate === "number" ? s.approvalRate : null;
      if (effectiveApps.length > 0) {
        const decidedApps = schemeApps.filter(
          (a) => a.status === "APPROVED" || a.status === "REJECTED"
        );
        if (decidedApps.length > 0) {
          const approvedCount = decidedApps.filter(
            (a) => a.status === "APPROVED" || a.status === "VERIFIED"
          ).length;
          approvalRate = Math.round((approvedCount / decidedApps.length) * 100);
        } else {
          approvalRate = null;
        }
      }

      const warnings = getQualityWarnings(s);

      return {
        ...s,
        appCount,
        approvalRate,
        warnings
      };
    });
  }, [schemes, applications, liveApplications]);

  const handleExtractPdf = async () => {
    if (!circularFile) return;
    setExtracting("uploading");
    setExtractError("");
    try {
      const res = await extractCircularScheme(circularFile);
      if (res.error) {
        setExtracting("error");
        setExtractError(res.message || "Failed to extract structured data from PDF.");
        return;
      }
      const draft = res.data;
      if (draft) {
        if (draft.name) setFormName(draft.name);
        if (draft.schemeCode) setFormSchemeCode(draft.schemeCode);
        if (draft.ministry) setFormMinistry(draft.ministry);
        if (draft.department) setFormDepartment(draft.department);
        if (draft.category) setFormCategory(draft.category);
        if (draft.description) setFormDesc(draft.description);
        if (draft.officialLink) setFormOfficialLink(draft.officialLink);
        if (draft.minAge != null) setFormMinAge(draft.minAge);
        if (draft.maxAge != null) setFormMaxAge(draft.maxAge);
        if (draft.maxIncome != null) setFormMaxIncome(draft.maxIncome);
        if (draft.occupations && draft.occupations.length > 0) setFormOccupations(draft.occupations);
        if (draft.castes && draft.castes.length > 0) setFormCastes(draft.castes);
        if (draft.genders && draft.genders.length > 0) setFormGenders(draft.genders);
        if (draft.benefits && draft.benefits.length > 0) setFormBenefits(draft.benefits.join(", "));
        if (draft.requiredDocuments && draft.requiredDocuments.length > 0) setFormDocs(draft.requiredDocuments.join(", "));
        if (draft.steps && draft.steps.length > 0) setFormSteps(draft.steps.join(", "));
        if (draft.deadline) setFormDeadline(draft.deadline);
        setFormStatus("draft");

        setExtractionMeta({
          sourceDocumentId: draft.sourceDocumentId,
          sourceFileName: draft.sourceFileName,
          uploadedBy: draft.uploadedBy,
          modelUsed: draft.modelUsed,
          extractedAt: draft.extractedAt,
          extractionStatus: draft.extractionStatus,
          rawSnippet: draft.rawSnippet
        });

        setExtracting(draft.extractionStatus === "SUCCESS" ? "success" : "manual_review");
      }
    } catch (err) {
      setExtracting("error");
      setExtractError(err.message || "An unexpected error occurred during circular extraction.");
    }
  };

  const handleOpenAdd = () => {
    setEditingScheme(null);
    setModalMode("add");
    setCircularFile(null);
    setExtracting(null);
    setExtractionMeta(null);
    setExtractError("");
    setFormSchemeCode(`SCH-${Date.now().toString(36).toUpperCase()}`);
    setFormName("");
    setFormMinistry("");
    setFormDepartment("");
    setFormCategory("Social Welfare");
    setFormStatus("draft");
    setFormDesc("");
    setFormMinAge(18);
    setFormMaxAge(100);
    setFormMaxIncome(300000);
    setFormOccupations([]);
    setFormCastes([]);
    setFormGenders([]);
    setFormDocs("");
    setFormSteps("");
    setFormBenefits("");
    setFormOfficialLink("");
    setFormSourceType("Central");
    setFormDeadline("");
    setFormApprovalRate("");
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenEdit = (scheme) => {
    setEditingScheme(scheme);
    setModalMode("edit");
    setCircularFile(null);
    setExtracting(null);
    setExtractionMeta(null);
    setExtractError("");
    setFormCanonicalChecklist(null);
    setLoadingDocs(true);

    const code = scheme.schemeCode || scheme.id || scheme.slug;
    if (code && code !== "—") {
      getSchemeDocumentChecklist(code)
        .then((res) => {
          setFormCanonicalChecklist(res?.data || res || null);
        })
        .catch((err) => {
          console.warn("Failed to fetch canonical checklist for admin editor:", err);
          setFormCanonicalChecklist(null);
        })
        .finally(() => {
          setLoadingDocs(false);
        });
    } else {
      setLoadingDocs(false);
    }

    setFormSchemeCode(scheme.schemeCode || scheme.id || "");
    setFormName(scheme.name || "");
    setFormMinistry(scheme.ministry || "");
    setFormDepartment(scheme.department || "");
    setFormCategory(scheme.category || "Social Welfare");
    setFormStatus(scheme.status || "draft");
    setFormDesc(scheme.description === "—" ? "" : (scheme.description || ""));
    setFormMinAge(scheme.eligibility?.minAge ?? 18);
    setFormMaxAge(scheme.eligibility?.maxAge ?? 100);
    setFormMaxIncome(scheme.eligibility?.maxIncome ?? 300000);
    setFormOccupations(scheme.eligibility?.occupations ?? []);
    setFormCastes(scheme.eligibility?.castes ?? []);
    setFormGenders(scheme.eligibility?.genders ?? []);
    setFormDocs(Array.isArray(scheme.requiredDocuments) ? scheme.requiredDocuments.join(", ") : "");
    setFormSteps(Array.isArray(scheme.steps) ? scheme.steps.join(", ") : "");
    setFormBenefits(Array.isArray(scheme.benefits) ? scheme.benefits.join(", ") : "");
    setFormOfficialLink(scheme.officialLink === "—" ? "" : (scheme.officialLink || ""));
    setFormSourceType(scheme.sourceType || "Central");
    setFormDeadline(scheme.deadline === "—" ? "" : (scheme.deadline || ""));
    setFormApprovalRate(typeof scheme.approvalRate === "number" ? scheme.approvalRate : "");
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleDuplicate = async (scheme) => {
    try {
      const copyCode = `SCH-${Date.now().toString(36).toUpperCase()}`;
      const payload = {
        schemeCode: copyCode,
        slug: `${(scheme.slug || "scheme").replace(/-copy.*$/, "")}-copy-${Date.now().toString(36)}`,
        title: {
          english: `${scheme.name} (Copy)`,
          hindi: `${scheme.name} (Copy)`,
          tamil: "",
          translations: {}
        },
        description: {
          english: scheme.description || "",
          hindi: scheme.description || "",
          tamil: "",
          translations: {}
        },
        shortDescription: {
          english: (scheme.description || "").slice(0, 160),
          hindi: (scheme.description || "").slice(0, 160),
          tamil: "",
          translations: {}
        },
        categoryCode: scheme.category || "General",
        ministry: scheme.ministry || "Central Government",
        department: scheme.department || "Government Board",
        schemeLevel: scheme.sourceType === "State" ? "STATE" : "CENTRAL",
        stateOrUt: scheme.sourceType === "State" ? "ALL" : null,
        beneficiaryType: "CITIZEN",
        schemeType: "SUBSIDY",
        status: "DRAFT",
        benefits: (scheme.benefits || []).map((b) => ({
          benefitType: "Financial Assistance",
          amount: 0,
          description: { english: b, hindi: b, tamil: "", translations: {} }
        })),
        requiredDocuments: (scheme.requiredDocuments || []).map((d) => ({
          documentCode: d.toUpperCase().replace(/[^A-Z0-9]/g, "_"),
          documentName: { english: d, hindi: d, tamil: "", translations: {} },
          isMandatory: true,
          verificationMode: "AUTOMATIC"
        })),
        steps: (scheme.steps || []).map((st, idx) => ({
          stepNumber: idx + 1,
          title: `Step ${idx + 1}`,
          description: st
        })),
        source: {
          sourceType: "OFFICIAL_NOTIFICATION",
          officialNotificationNumber: copyCode,
          issuingAuthority: scheme.ministry || "Administrative Authority"
        },
        tags: [scheme.category?.toLowerCase() || "general"],
        faqs: [],
        version: 1
      };

      const res = await createScheme(payload);
      if (!res.error) {
        await fetchLiveSchemes();
      } else {
        alert(res.message || "Failed to duplicate scheme.");
      }
    } catch (err) {
      console.error("Duplicate failed:", err);
      alert("Failed to duplicate scheme.");
    }
  };

  const handleSetStatus = async (scheme, nextStatus) => {
    try {
      const backendStatus =
        nextStatus.toUpperCase() === "PUBLISHED"
          ? "ACTIVE"
          : nextStatus.toUpperCase();
      const res = await updateStatus(scheme.id, backendStatus);
      if (!res.error) {
        await fetchLiveSchemes();
      } else {
        alert(res.message || "Failed to update scheme status.");
      }
    } catch (err) {
      console.error("Failed to update status:", err);
      alert("Failed to update status.");
    }
  };

  const handleDeleteScheme = async (schemeId) => {
    if (!window.confirm("Are you sure you want to delete this scheme permanently from MongoDB?")) {
      return;
    }
    try {
      const res = await deleteScheme(schemeId);
      if (!res.error) {
        await fetchLiveSchemes();
      } else {
        alert(res.message || "Failed to delete scheme.");
      }
    } catch (err) {
      console.error("Failed to delete scheme:", err);
      alert("Failed to delete scheme.");
    }
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();

    // Validation
    const errors = {};
    if (!formName.trim()) {
      errors.name = "Scheme title is required.";
    } else if (formName.trim().length < 5) {
      errors.name = "Scheme title must be at least 5 characters.";
    }

    if (!formMinistry.trim()) {
      errors.ministry = "Hosting ministry is required.";
    }

    if (!formDepartment.trim()) {
      errors.department = "Executing department is required.";
    }

    if (!formCategory || formCategory === "all") {
      errors.category = "Please select a valid category.";
    }

    if (!formDesc.trim()) {
      errors.description = "Objective goal description is required.";
    } else if (formDesc.trim().length < 30) {
      errors.description = "Description must be at least 30 characters.";
    }

    const benefitsArray = (formBenefits || "").split(",").map((s) => s.trim()).filter(Boolean);
    if (benefitsArray.length === 0) {
      errors.benefits = "At least one benefit is required.";
    }

    const docsArray = (formDocs || "").split(",").map((s) => s.trim()).filter(Boolean);
    if (modalMode === "add" && docsArray.length === 0) {
      errors.requiredDocuments = "At least one required document is required.";
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setFormErrors({});
    setFormSubmitting(true);

    try {
      const effectiveSchemeCode = formSchemeCode.trim() || `SCH-${Date.now().toString(36).toUpperCase()}`;
      const effectiveSlug = (formName.trim()).toLowerCase().replace(/[^a-z0-9]+/g, "-");
      const backendStatus = formStatus.toUpperCase() === "PUBLISHED" ? "ACTIVE" : formStatus.toUpperCase();

      const payload = {
        schemeCode: effectiveSchemeCode,
        slug: effectiveSlug,
        title: {
          english: formName.trim(),
          hindi: formName.trim(),
          tamil: "",
          translations: {}
        },
        description: {
          english: formDesc.trim(),
          hindi: formDesc.trim(),
          tamil: "",
          translations: {}
        },
        shortDescription: {
          english: formDesc.trim().slice(0, 160),
          hindi: formDesc.trim().slice(0, 160),
          tamil: "",
          translations: {}
        },
        categoryCode: formCategory,
        ministry: formMinistry.trim(),
        department: formDepartment.trim(),
        schemeLevel: formSourceType.toUpperCase() === "STATE" ? "STATE" : "CENTRAL",
        stateOrUt: formSourceType.toUpperCase() === "STATE" ? "ALL" : null,
        beneficiaryType: formOccupations[0]?.toUpperCase() || "CITIZEN",
        schemeType: "SUBSIDY",
        status: backendStatus,
        benefits: benefitsArray.map((b) => ({
          benefitType: "Financial Assistance",
          amount: 0,
          description: { english: b, hindi: b, tamil: "", translations: {} }
        })),
        requiredDocuments: modalMode === "edit" && editingScheme?.rawBackendScheme?.requiredDocuments?.length
          ? editingScheme.rawBackendScheme.requiredDocuments
          : docsArray.map((d) => ({
              documentCode: d.toUpperCase().replace(/[^A-Z0-9]/g, "_"),
              documentName: { english: d, hindi: d, tamil: "", translations: {} },
              isMandatory: true,
              verificationMode: "AUTOMATIC"
            })),
        steps: (formSteps || "").split(",").map((s, idx) => ({
          stepNumber: idx + 1,
          title: `Step ${idx + 1}`,
          description: s.trim()
        })).filter((st) => st.description),
        applicationInfo: {
          officialPortalUrl: formOfficialLink.trim() || null,
          applicationUrl: formOfficialLink.trim() || null,
          applicationMode: "ONLINE",
          deadline: formDeadline ? new Date(formDeadline).toISOString() : null,
          closingDate: formDeadline || null
        },
        source: {
          sourceType: "OFFICIAL_NOTIFICATION",
          officialNotificationNumber: effectiveSchemeCode,
          issuingAuthority: formMinistry.trim()
        },
        tags: [formCategory.toLowerCase(), ...formOccupations.map((o) => o.toLowerCase())],
        faqs: [],
        version: 1
      };

      let res;
      if (modalMode === "edit" && editingScheme?.id) {
        res = await updateScheme(editingScheme.id, payload);
      } else {
        res = await createScheme(payload);
      }

      if (!res.error) {
        setIsModalOpen(false);
        await fetchLiveSchemes();
      } else {
        alert(res.message || "Failed to persist scheme to database.");
      }
    } catch (err) {
      console.error("Save scheme failed:", err);
      alert("Failed to save scheme.");
    } finally {
      setFormSubmitting(false);
    }
  };

  // Readiness calculation
  const checkResults = useMemo(() => {
    const checks = [
      { id: "desc", name: "Objective Description Check", met: (formDesc || "").trim().length >= 30, tip: "Length must be ≥ 30 characters." },
      { id: "benefits", name: "Benefits Specified", met: (formBenefits || "").trim().split(",").filter(Boolean).length > 0, tip: "Detail at least one benefit." },
      { id: "criteria", name: "Eligibility Bounds Configured", met: Number(formMinAge) >= 0 && Number(formMaxIncome) > 0, tip: "Configure age boundaries & income limit." },
      { id: "docs", name: "Required Documents Setup", met: (formDocs || "").trim().split(",").filter(Boolean).length > 0, tip: "Require at least one verification file." },
      { id: "dept", name: "Executing Department", met: (formDepartment || "").trim().length > 0, tip: "Specify administrative department." },
      { id: "link", name: "Portal URL Setup", met: (formOfficialLink || "").trim().startsWith("http"), tip: "Include secure link starting with http/https." },
    ];
    const metCount = checks.filter(c => c.met).length;
    const score = Math.round((metCount / checks.length) * 100);
    return { checks, score };
  }, [formDesc, formBenefits, formMinAge, formMaxIncome, formDocs, formDepartment, formOfficialLink]);

  return (
    <div className="space-y-4">
      {/* ── Sub-Header & Trigger Bar ── */}
      <div className="flex flex-col lg:flex-row justify-between items-stretch lg:items-center gap-3 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-2xl shadow-sm">
        <div className="flex flex-col md:flex-row gap-3 w-full lg:flex-1">
          <SearchBar
            value={search}
            onChange={(val) => {
              setSearch(val);
              setCurrentPage(1);
            }}
            placeholder="Search schemes by title, scheme code, ministry, department..."
            className="flex-1"
          />

          <div className="flex flex-wrap gap-2 select-none">
            <select
              value={categoryFilter}
              onChange={(e) => {
                setCategoryFilter(e.target.value);
                setCurrentPage(1);
              }}
              className="text-xs border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-100/50 dark:hover:bg-slate-750 focus:outline-none transition cursor-pointer font-bold"
            >
              <option value="all">All Categories</option>
              {CORE_CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>

            <select
              value={sortOrder}
              onChange={(e) => {
                setSortOrder(e.target.value);
                setCurrentPage(1);
              }}
              className="text-xs border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-100/50 dark:hover:bg-slate-750 focus:outline-none transition cursor-pointer font-bold"
            >
              <option value="latest">Latest Updated</option>
              <option value="oldest">Oldest Updated</option>
            </select>
          </div>
        </div>

        <div className="flex items-center gap-2 w-full lg:w-auto justify-between shrink-0 border-t lg:border-t-0 border-slate-200 dark:border-slate-800 pt-3 lg:pt-0 select-none">
          <div className="flex bg-slate-100 dark:bg-slate-800 p-1 rounded-xl text-[11px] font-bold">
            {["all", "published", "draft", "inactive", "archived"].map((f) => (
              <button
                key={f}
                onClick={() => {
                  setStatusFilter(f);
                  setCurrentPage(1);
                }}
                className={`px-3 py-1.5 rounded-lg capitalize transition whitespace-nowrap ${
                  statusFilter === f ? "bg-white dark:bg-slate-700 text-slate-900 dark:text-slate-100 shadow-sm" : "text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200"
                }`}
              >
                {f}
              </button>
            ))}
          </div>
          <button
            onClick={fetchLiveSchemes}
            disabled={loading}
            title="Refresh from MongoDB"
            className="p-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 rounded-xl text-xs font-bold transition flex items-center shrink-0"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin text-indigo-600 dark:text-indigo-400" : ""}`} />
          </button>
          <button
            onClick={handleOpenAdd}
            className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center gap-1.5 shrink-0 animate-in fade-in"
          >
            <PlusCircle className="h-4 w-4" />
            <span>Add Scheme</span>
          </button>
        </div>
      </div>

      {/* ── Error Banner with Retry ── */}
      {error && (
        <div className="bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-900/50 p-5 rounded-2xl flex flex-col sm:flex-row items-center justify-between gap-4 text-rose-800 dark:text-rose-300 animate-in fade-in">
          <div className="flex items-center gap-3">
            <AlertTriangle className="h-6 w-6 text-rose-500 shrink-0" />
            <div>
              <p className="font-bold text-sm">Failed to load schemes</p>
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-0.5">{error}</p>
            </div>
          </div>
          <button
            onClick={fetchLiveSchemes}
            className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold shadow-sm transition shrink-0"
          >
            Retry
          </button>
        </div>
      )}

      {/* ── Loading Skeleton ── */}
      {loading ? (
        <div className="space-y-4 animate-pulse select-none">
          <div className="flex items-center justify-center py-6 text-slate-500 dark:text-slate-400 font-bold text-xs gap-2">
            <Loader2 className="h-4 w-4 animate-spin text-indigo-600 dark:text-indigo-400" />
            <span>Loading schemes from database...</span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="h-56 bg-slate-200 dark:bg-slate-800 rounded-2xl"></div>
            ))}
          </div>
        </div>
      ) : error ? (
        /* When error is present, do NOT show 'No Schemes in Database' empty state */
        null
      ) : enrichedSchemes.length === 0 ? (
        <EmptyState
          icon={FileWarning}
          title={
            (search || statusFilter !== "all" || categoryFilter !== "all")
              ? "No Matching Schemes Found"
              : "No Schemes in Database"
          }
          description={
            (search || statusFilter !== "all" || categoryFilter !== "all")
              ? "No schemes match your active search or filter criteria. Try resetting filters."
              : "MongoDB scheme database is currently empty. Click 'Add Scheme' to provision a government scheme."
          }
          action={{
            label: (search || statusFilter !== "all" || categoryFilter !== "all") ? "Reset Filters" : "Provision Scheme",
            variant: "secondary",
            onClick: () => {
              if (search || statusFilter !== "all" || categoryFilter !== "all") {
                setSearch("");
                setStatusFilter("all");
                setCategoryFilter("all");
                setSortOrder("latest");
                setCurrentPage(1);
              } else {
                handleOpenAdd();
              }
            }
          }}
          className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl"
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {enrichedSchemes.map((scheme) => {
            const isExpanded = expandedId === scheme.id;

            return (
              <div
                key={scheme.id}
                className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm hover:shadow-md transition duration-200 flex flex-col overflow-hidden relative group animate-in fade-in zoom-in-95 duration-150"
              >
                {/* Clickable Card Body */}
                <div
                  onClick={() => handleOpenEdit(scheme)}
                  className="p-5 flex-1 space-y-4 cursor-pointer hover:bg-slate-50/40 dark:hover:bg-slate-800/40 transition duration-150"
                  title="Click to edit scheme parameters"
                >
                  {/* Category Tag + Scheme Code + Status Badge Row */}
                  <div className="flex justify-between items-start gap-2 select-none">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 text-[9px] font-bold rounded-lg border border-slate-200 dark:border-slate-700 uppercase tracking-wider flex items-center gap-1">
                        <Folder className="h-3.5 w-3.5 text-slate-400" />
                        {scheme.category || "Social Welfare"}
                      </span>
                      {scheme.schemeCode && scheme.schemeCode !== "—" && (
                        <span className="px-2 py-0.5 bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 text-[9px] font-mono font-bold rounded-lg border border-indigo-100 dark:border-indigo-800/60">
                          {scheme.schemeCode}
                        </span>
                      )}
                    </div>
                    <StatusBadge status={scheme.status} variant="pill" size="xs" />
                  </div>

                  {/* Scheme Name, Ministry, Department */}
                  <div className="space-y-1">
                    <h3 className="text-sm font-black text-slate-900 dark:text-slate-100 tracking-tight leading-snug group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition">
                      {scheme.name}
                    </h3>
                    <div className="text-[10px] font-semibold text-slate-500 dark:text-slate-400 space-y-0.5">
                      <p className="flex items-center gap-1 font-bold text-slate-700 dark:text-slate-300">
                        <span className="text-[8px] text-slate-400 font-extrabold uppercase">Ministry:</span> {scheme.ministry}
                      </p>
                      <p className="flex items-center gap-1 font-bold text-slate-600 dark:text-slate-400">
                        <span className="text-[8px] text-slate-400 font-extrabold uppercase">Dept:</span> {scheme.department}
                      </p>
                    </div>
                  </div>

                  {/* Summary Demographics */}
                  <div className="grid grid-cols-3 gap-2.5 text-[10px] text-slate-500 dark:text-slate-400 font-semibold bg-slate-50 dark:bg-slate-800/60 p-3 rounded-xl border border-slate-100 dark:border-slate-800 select-none">
                    <div>
                      <span className="text-slate-400 block uppercase text-[8px] font-extrabold mb-0.5">Applications</span>
                      <span className="font-extrabold text-slate-800 dark:text-slate-200 text-xs flex items-center gap-1">
                        <BarChart3 className="h-3.5 w-3.5 text-indigo-500" />
                        {scheme.appCount}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block uppercase text-[8px] font-extrabold mb-0.5">Approval Rate</span>
                      <span className="font-extrabold text-slate-800 dark:text-slate-200 text-xs flex items-center gap-1">
                        <Award className="h-3.5 w-3.5 text-emerald-500" />
                        {scheme.approvalRate !== null ? `${scheme.approvalRate}%` : "—"}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block uppercase text-[8px] font-extrabold mb-0.5">Deadline</span>
                      <span className="font-extrabold text-slate-800 dark:text-slate-200 text-xs flex items-center gap-1">
                        <Calendar className="h-3.5 w-3.5 text-rose-500" />
                        {scheme.deadline}
                      </span>
                    </div>
                  </div>

                  {/* Quality Warnings Indicator */}
                  {(scheme.warnings || []).length > 0 && (
                    <div
                      className="bg-rose-50 dark:bg-rose-950/30 border border-rose-100 dark:border-rose-900/40 p-2.5 rounded-xl flex items-start gap-2 text-rose-700 dark:text-rose-300 text-[10px] leading-relaxed select-none"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <AlertTriangle className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" />
                      <div>
                        <span className="font-bold">Content Warnings ({scheme.warnings.length}):</span>
                        <ul className="list-disc list-inside mt-0.5 font-medium text-rose-600 dark:text-rose-400">
                          {(scheme.warnings || []).slice(0, 2).map((w, idx) => (
                            <li key={idx} className="truncate max-w-xs">{w}</li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  )}

                  {/* Action link & clock */}
                  <div className="flex justify-between items-center text-[9px] text-slate-400 font-bold border-t border-slate-100 dark:border-slate-800 pt-2.5 select-none">
                    <span className="flex items-center gap-1">
                      <Clock className="h-3.5 w-3.5 text-slate-400" />
                      Updated: {scheme.lastUpdated}
                    </span>
                    <span className="text-indigo-600 dark:text-indigo-400 group-hover:underline">
                      Detailed Editor &rarr;
                    </span>
                  </div>

                  {/* Expanded Detail Panel */}
                  {isExpanded && (
                    <div
                      className="border-t border-slate-100 dark:border-slate-800 pt-3 mt-3 text-xs text-slate-600 dark:text-slate-300 space-y-3 animate-in slide-in-from-top-2 duration-150"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div>
                        <span className="font-bold text-slate-700 dark:text-slate-200 block mb-0.5">Objective Goal Description</span>
                        <p className="leading-relaxed text-slate-500 dark:text-slate-400 font-medium">{scheme.description}</p>
                      </div>
                      <div>
                        <span className="font-bold text-slate-700 dark:text-slate-200 block mb-0.5">Benefits Overview</span>
                        <ul className="list-disc list-inside space-y-0.5 text-slate-500 dark:text-slate-400 font-medium">
                          {(scheme.benefits || []).length > 0 ? (
                            scheme.benefits.map((b, idx) => <li key={idx}>{b}</li>)
                          ) : (
                            <li className="text-slate-400">None specified</li>
                          )}
                        </ul>
                      </div>
                      <div>
                        <span className="font-bold text-slate-700 dark:text-slate-200 block mb-0.5">Required Files Checklist</span>
                        <div className="flex flex-wrap gap-1 mt-1">
                          {(scheme.requiredDocuments || []).length > 0 ? (
                            scheme.requiredDocuments.map((d) => (
                              <span key={d} className="px-2 py-0.5 bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 text-[9px] font-bold rounded-md">
                                {d}
                              </span>
                            ))
                          ) : (
                            <span className="text-slate-400 text-[10px]">No mandatory documents required</span>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Card Action Footer */}
                <div className="p-4 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-850/50 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 select-none">
                  {/* Status Toggle Buttons */}
                  <div className="flex items-center gap-1.5 justify-between">
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Status:</span>
                    <div className="flex bg-slate-100 dark:bg-slate-800 p-0.5 rounded-lg border border-slate-200 dark:border-slate-700">
                      {["draft", "published", "inactive", "archived"].map((st) => (
                        <button
                          key={st}
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleSetStatus(scheme, st);
                          }}
                          className={`px-2 py-1 rounded-md text-[9px] font-bold capitalize transition-all ${
                            scheme.status === st
                              ? st === "published"
                                ? "bg-indigo-600 text-white shadow-sm"
                                : st === "draft"
                                ? "bg-amber-500 text-white shadow-sm"
                                : "bg-slate-500 text-white shadow-sm"
                              : "text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200"
                          }`}
                        >
                          {st}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="flex items-center justify-between sm:justify-end gap-2">
                    <div className="flex items-center gap-1 text-slate-400">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDuplicate(scheme);
                        }}
                        className="p-2 hover:bg-slate-200 dark:hover:bg-slate-800 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg transition"
                        title="Duplicate scheme"
                      >
                        <Copy className="h-4 w-4" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteScheme(scheme.id);
                        }}
                        className="p-2 hover:bg-slate-200 dark:hover:bg-slate-800 hover:text-rose-600 dark:hover:text-rose-400 rounded-lg transition"
                        title="Delete Permanently"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setExpandedId(isExpanded ? null : scheme.id);
                      }}
                      className="px-3 py-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 rounded-lg text-[10px] font-bold flex items-center gap-1 transition"
                    >
                      <span>{isExpanded ? "Collapse" : "Quick View"}</span>
                      {isExpanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── Pagination Controls ── */}
      {!loading && !error && enrichedSchemes.length > 0 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-2xl shadow-sm text-xs select-none">
          <div className="text-slate-500 dark:text-slate-400 font-semibold">
            Showing <span className="font-bold text-slate-800 dark:text-slate-200">{(currentPage - 1) * pageSize + 1}</span> to{" "}
            <span className="font-bold text-slate-800 dark:text-slate-200">{Math.min(currentPage * pageSize, totalElements)}</span> of{" "}
            <span className="font-bold text-slate-800 dark:text-slate-200">{totalElements.toLocaleString()}</span> schemes
          </div>

          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1 mr-2">
              <span className="text-slate-400 font-bold text-[10px] uppercase">Per page:</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setCurrentPage(1);
                }}
                className="text-xs border border-slate-200 dark:border-slate-700 rounded-lg px-2 py-1 bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 font-bold focus:outline-none"
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </div>

            <button
              type="button"
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage <= 1}
              className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-700 dark:text-slate-300 font-bold rounded-lg transition"
            >
              Previous
            </button>

            <span className="px-3 py-1 bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-100 dark:border-indigo-800/60 text-indigo-700 dark:text-indigo-300 font-bold rounded-lg text-xs">
              Page {currentPage} of {totalPages}
            </span>

            <button
              type="button"
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage >= totalPages}
              className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-700 dark:text-slate-300 font-bold rounded-lg transition"
            >
              Next
            </button>
          </div>
        </div>
      )}

      {/* ── Scheme Modal Form (Add/Edit) with Quality Sidepanel ── */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm" onClick={() => setIsModalOpen(false)} />
          <div className="relative bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-2xl max-w-4xl w-full max-h-[85vh] flex flex-col overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-slate-900 dark:bg-slate-950 text-white p-5 flex items-center justify-between shrink-0 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <FileCheck className="h-5 w-5 text-indigo-400" />
                <h3 className="font-bold text-sm">
                  {modalMode === "edit" ? "Modify Scheme Parameters" : "Provision New Scheme Portal"}
                </h3>
              </div>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white p-1 rounded-lg transition">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="flex-1 flex overflow-hidden">
              <form onSubmit={handleFormSubmit} className="flex-1 overflow-y-auto p-6 space-y-4 text-xs">
                {/* ── AI Government Circular Ingestion Box (Available when Adding New Scheme) ── */}
                {modalMode === "add" && (
                  <div className="border border-indigo-200 dark:border-indigo-800/60 bg-gradient-to-br from-indigo-50/70 via-white to-purple-50/50 dark:from-indigo-950/40 dark:via-slate-900 dark:to-purple-950/30 rounded-xl p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="p-1.5 bg-indigo-600 text-white rounded-lg">
                          <Sparkles className="h-4 w-4" />
                        </div>
                        <div>
                          <h4 className="font-bold text-slate-800 dark:text-slate-100 text-xs">AI Government Circular Ingestion</h4>
                          <p className="text-[10px] text-slate-500 dark:text-slate-400">Upload official PDF notification to auto-extract structured scheme draft via Gemini AI</p>
                        </div>
                      </div>
                      <span className="text-[9px] font-bold px-2 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 uppercase tracking-wider">
                        PDFBox + Gemini 1.5
                      </span>
                    </div>

                    <div className="flex flex-col sm:flex-row items-center gap-3">
                      <label className="flex-1 w-full flex items-center gap-2 px-3 py-2 border-2 border-dashed border-indigo-300 dark:border-indigo-700 hover:border-indigo-500 dark:hover:border-indigo-400 rounded-xl bg-white/80 dark:bg-slate-800/80 cursor-pointer transition">
                        <Upload className="h-4 w-4 text-indigo-600 dark:text-indigo-400 shrink-0" />
                        <span className="text-xs text-slate-600 dark:text-slate-300 truncate font-medium">
                          {circularFile ? circularFile.name : "Select or drag Government Circular PDF..."}
                        </span>
                        <input
                          type="file"
                          accept=".pdf,application/pdf"
                          onChange={(e) => {
                            if (e.target.files && e.target.files[0]) {
                              setCircularFile(e.target.files[0]);
                              setExtracting(null);
                              setExtractError("");
                            }
                          }}
                          className="hidden"
                        />
                      </label>

                      <button
                        type="button"
                        onClick={handleExtractPdf}
                        disabled={!circularFile || extracting === "uploading"}
                        className="w-full sm:w-auto px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 dark:disabled:bg-slate-800 text-white font-bold rounded-xl transition flex items-center justify-center gap-2 shadow-xs shrink-0"
                      >
                        {extracting === "uploading" ? (
                          <>
                            <Loader2 className="h-4 w-4 animate-spin" />
                            <span>Extracting AI Draft...</span>
                          </>
                        ) : (
                          <>
                            <Sparkles className="h-4 w-4" />
                            <span>Extract Scheme Metadata</span>
                          </>
                        )}
                      </button>
                    </div>

                    {/* Status & Traceability Alerts */}
                    {extracting === "success" && (
                      <div className="p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/60 rounded-xl space-y-1 text-[11px]">
                        <div className="flex items-center gap-1.5 font-bold text-emerald-800 dark:text-emerald-300">
                          <CheckCircle2 className="h-4 w-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
                          <span>Gemini AI extraction completed successfully! Form pre-filled below.</span>
                        </div>
                        <div className="grid grid-cols-2 gap-x-4 text-[10px] text-emerald-700 dark:text-emerald-400 pt-1">
                          <p><span className="font-semibold">Source File:</span> {extractionMeta?.sourceFileName}</p>
                          <p><span className="font-semibold">Document ID:</span> {extractionMeta?.sourceDocumentId}</p>
                          <p><span className="font-semibold">AI Model:</span> {extractionMeta?.modelUsed}</p>
                          <p><span className="font-semibold">State:</span> Saved strictly as DRAFT requiring human review.</p>
                        </div>
                      </div>
                    )}

                    {extracting === "manual_review" && (
                      <div className="p-3 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 rounded-xl space-y-1 text-[11px]">
                        <div className="flex items-center gap-1.5 font-bold text-amber-800 dark:text-amber-300">
                          <Info className="h-4 w-4 text-amber-600 dark:text-amber-400 shrink-0" />
                          <span>AI Fallback Mode: Circular PDF stored for Manual Review (Zero-Fabrication).</span>
                        </div>
                        <p className="text-[10px] text-amber-700 dark:text-amber-400">
                          No fake criteria were hallucinated. The original PDF is indexed under Document ID <code className="bg-amber-100 dark:bg-amber-900/50 px-1 py-0.5 rounded font-mono">{extractionMeta?.sourceDocumentId}</code>.
                        </p>
                        {extractionMeta?.rawSnippet && (
                          <div className="mt-1.5 p-2 bg-white/80 dark:bg-slate-800 rounded border border-amber-200 dark:border-amber-800 text-[10px] text-slate-700 dark:text-slate-200 font-mono max-h-20 overflow-y-auto">
                            <span className="font-bold text-slate-500 dark:text-slate-400 block mb-0.5">Circular Text Preview:</span>
                            {extractionMeta.rawSnippet}
                          </div>
                        )}
                      </div>
                    )}

                    {extracting === "error" && (
                      <div className="p-3 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 rounded-xl flex items-center gap-2 text-[11px] text-rose-800 dark:text-rose-300">
                        <AlertTriangle className="h-4 w-4 text-rose-600 dark:text-rose-400 shrink-0" />
                        <span>{extractError}</span>
                      </div>
                    )}
                  </div>
                )}

                {/* Title and Category */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Scheme Title *</label>
                    <input
                      type="text"
                      placeholder="e.g. Atal Pension Yojana"
                      value={formName}
                      onChange={(e) => setFormName(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.name ? "border-rose-400 bg-rose-50/20 text-rose-800 dark:text-rose-300" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.name && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.name}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Core Category *</label>
                    <select
                      value={formCategory}
                      onChange={(e) => setFormCategory(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.category ? "border-rose-400 bg-rose-50/20" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold cursor-pointer`}
                    >
                      <option value="">Select Category</option>
                      {CORE_CATEGORIES.map((cat) => (
                        <option key={cat} value={cat}>
                          {cat}
                        </option>
                      ))}
                    </select>
                    {formErrors.category && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.category}</p>}
                  </div>
                </div>

                {/* Ministry, Department, Status */}
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Hosting Ministry *</label>
                    <input
                      type="text"
                      placeholder="e.g. Ministry of Finance"
                      value={formMinistry}
                      onChange={(e) => setFormMinistry(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.ministry ? "border-rose-400 bg-rose-50/20" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.ministry && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.ministry}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Executing Department *</label>
                    <input
                      type="text"
                      placeholder="e.g. PFRDA"
                      value={formDepartment}
                      onChange={(e) => setFormDepartment(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.department ? "border-rose-400 bg-rose-50/20" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.department && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.department}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Portal Visibility</label>
                    <select
                      value={formStatus}
                      onChange={(e) => setFormStatus(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold cursor-pointer"
                    >
                      <option value="draft">Draft (Private Nodal Review)</option>
                      <option value="published">Published (Live Citizen Registration)</option>
                      <option value="archived">Archived (Public Directory History)</option>
                    </select>
                  </div>
                </div>

                {/* Portal URL, Government Source */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Official Portal URL *</label>
                    <input
                      type="url"
                      placeholder="e.g. https://pmkisan.gov.in"
                      value={formOfficialLink}
                      onChange={(e) => setFormOfficialLink(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.officialLink ? "border-rose-400 bg-rose-50/20" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.officialLink && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.officialLink}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Government Source</label>
                    <select
                      value={formSourceType}
                      onChange={(e) => setFormSourceType(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold cursor-pointer"
                    >
                      <option value="Central">Central Government</option>
                      <option value="State">State Government</option>
                    </select>
                  </div>
                </div>

                {/* Deadline and Approval Rate */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Closing Deadline *</label>
                    <input
                      type="date"
                      value={formDeadline}
                      onChange={(e) => setFormDeadline(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.deadline ? "border-rose-400 bg-rose-50/20" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.deadline && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.deadline}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Avg. Approval Rate (%) *</label>
                    <input
                      type="number"
                      min="0"
                      max="100"
                      placeholder="e.g. 92"
                      value={formApprovalRate}
                      onChange={(e) => setFormApprovalRate(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.approvalRate ? "border-rose-400 bg-rose-50/20" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.approvalRate && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.approvalRate}</p>}
                  </div>
                </div>

                {/* Objective Goal Description */}
                <div>
                  <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Objective Goal Description *</label>
                  <textarea
                    placeholder="Describe the scheme objectives, funding models, and coverage..."
                    value={formDesc}
                    onChange={(e) => setFormDesc(e.target.value)}
                    rows="3"
                    className={`w-full px-3 py-2 border ${formErrors.description ? "border-rose-400 bg-rose-50/20 text-rose-800 dark:text-rose-300" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                  />
                  {formErrors.description && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.description}</p>}
                </div>

                {/* Eligibility Criteria Boundaries */}
                <div className="border border-slate-200 dark:border-slate-800 p-4 rounded-xl space-y-4 bg-slate-50/50 dark:bg-slate-850/50">
                  <span className="block font-bold text-slate-800 dark:text-slate-200 uppercase tracking-wider text-[10px]">Eligibility Criteria Parameters</span>
                  <div className="grid grid-cols-3 gap-3">
                    <div>
                      <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Min Age Limit</label>
                      <input
                        type="number"
                        value={formMinAge}
                        onChange={(e) => setFormMinAge(e.target.value)}
                        className={`w-full px-3 py-2 border ${formErrors.minAge ? "border-rose-400 text-rose-800" : "border-slate-200 dark:border-slate-700"} bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold`}
                      />
                      {formErrors.minAge && <p className="text-rose-500 text-[9px] mt-1 font-bold">{formErrors.minAge}</p>}
                    </div>
                    <div>
                      <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Max Age Limit</label>
                      <input
                        type="number"
                        value={formMaxAge}
                        onChange={(e) => setFormMaxAge(e.target.value)}
                        className={`w-full px-3 py-2 border ${formErrors.maxAge ? "border-rose-400 text-rose-800" : "border-slate-200 dark:border-slate-700"} bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold`}
                      />
                      {formErrors.maxAge && <p className="text-rose-500 text-[9px] mt-1 font-bold">{formErrors.maxAge}</p>}
                    </div>
                    <div>
                      <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Max Income Ceiling (₹)</label>
                      <input
                        type="number"
                        value={formMaxIncome}
                        onChange={(e) => setFormMaxIncome(e.target.value)}
                        className={`w-full px-3 py-2 border ${formErrors.maxIncome ? "border-rose-400 text-rose-800" : "border-slate-200 dark:border-slate-700"} bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold`}
                      />
                      {formErrors.maxIncome && <p className="text-rose-500 text-[9px] mt-1 font-bold">{formErrors.maxIncome}</p>}
                    </div>
                  </div>

                  <div className="space-y-3 pt-3 border-t border-slate-200 dark:border-slate-700">
                    {[
                      { label: "Target Occupations", list: occupationsList, state: formOccupations, setState: setFormOccupations },
                      { label: "Target Social Categories", list: castesList, state: formCastes, setState: setFormCastes },
                      { label: "Target Genders", list: gendersList, state: formGenders, setState: setFormGenders },
                    ].map(({ label, list, state, setState }) => (
                      <div key={label}>
                        <span className="block font-bold text-slate-600 dark:text-slate-300 mb-1.5">{label} <span className="text-slate-400 font-normal">(empty = open to all)</span></span>
                        <div className="flex flex-wrap gap-2">
                          {list.map((item) => {
                            const active = state.includes(item);
                            return (
                              <button key={item} type="button"
                                onClick={() => {
                                  setState(prev => prev.includes(item) ? prev.filter(x => x !== item) : [...prev, item]);
                                }}
                                className={`px-2.5 py-1 rounded-lg border text-[10px] font-bold transition ${active ? "bg-indigo-600 text-white border-indigo-600" : "bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700"}`}>
                                {item}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Benefits Overview */}
                <div>
                  <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Benefits (comma separated) *</label>
                  <input
                    type="text"
                    placeholder="Guaranteed pension, Corpus return to nominee"
                    value={formBenefits}
                    onChange={(e) => setFormBenefits(e.target.value)}
                    className={`w-full px-3 py-2 border ${formErrors.benefits ? "border-rose-400 bg-rose-50/20 text-rose-805" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                  />
                  {formErrors.benefits && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.benefits}</p>}
                </div>

                {/* Required Documents Section (Issue 1: Canonical Source of Truth) */}
                {modalMode === "edit" ? (
                  <div className="border border-slate-200 dark:border-slate-800 rounded-xl p-4 bg-slate-50/50 dark:bg-slate-850/50 space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="block font-bold text-slate-800 dark:text-slate-200 uppercase tracking-wider text-[10px]">
                        Required Documents (Canonical Checklist)
                      </span>
                      {loadingDocs && (
                        <span className="flex items-center gap-1 text-[10px] text-indigo-600 dark:text-indigo-400 font-semibold">
                          <Loader2 className="h-3 w-3 animate-spin" />
                          Fetching canonical requirements...
                        </span>
                      )}
                    </div>

                    {loadingDocs ? (
                      <div className="space-y-2">
                        <div className="h-8 bg-slate-200 dark:bg-slate-700 animate-pulse rounded-lg" />
                        <div className="h-8 bg-slate-200 dark:bg-slate-700 animate-pulse rounded-lg" />
                      </div>
                    ) : formCanonicalChecklist?.items && formCanonicalChecklist.items.length > 0 ? (
                      <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                        {formCanonicalChecklist.items.map((doc, idx) => {
                          const isOneOf = doc.alternativeGroupType === "ONE_OF" || (doc.alternatives && doc.alternatives.length > 0);
                          return (
                            <div key={idx} className="p-3 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl space-y-1.5 shadow-2xs">
                              <div className="flex items-start justify-between gap-2">
                                <div className="flex items-center gap-1.5 font-bold text-slate-800 dark:text-slate-200 text-xs">
                                  <FileText className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400 shrink-0" />
                                  <span>{doc.documentName || doc.documentCode}</span>
                                </div>
                                {isOneOf ? (
                                  <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-purple-100 dark:bg-purple-950/60 text-purple-700 dark:text-purple-300 border border-purple-200 dark:border-purple-800/60 shrink-0">
                                    Any ONE of group
                                  </span>
                                ) : doc.mandatory ? (
                                  <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/60 shrink-0">
                                    Mandatory
                                  </span>
                                ) : (
                                  <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-600 shrink-0">
                                    Optional
                                  </span>
                                )}
                              </div>

                              {isOneOf && doc.alternatives && doc.alternatives.length > 0 && (
                                <div className="mt-1 pl-4 border-l-2 border-purple-200 dark:border-purple-700 space-y-0.5 text-[11px] text-slate-600 dark:text-slate-300">
                                  <span className="text-[10px] font-bold text-purple-900 dark:text-purple-300 block">Accepted Alternatives:</span>
                                  {doc.alternatives.map((alt, aIdx) => (
                                    <div key={aIdx} className="flex items-center gap-1.5">
                                      <span className="h-1 w-1 rounded-full bg-purple-400" />
                                      <span>{alt}</span>
                                    </div>
                                  ))}
                                </div>
                              )}

                              {doc.issuingAuthority && (
                                <div className="text-[10px] text-slate-500 dark:text-slate-400 flex items-center gap-1">
                                  <span className="font-semibold text-slate-600 dark:text-slate-300">Issuing Authority:</span>
                                  <span>{doc.issuingAuthority}</span>
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    ) : (
                      <div className="p-3 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 rounded-xl text-[11px] text-amber-800 dark:text-amber-300 flex items-center gap-2">
                        <Info className="h-4 w-4 text-amber-600 dark:text-amber-400 shrink-0" />
                        <span>
                          {formCanonicalChecklist?.documentStatus === "DOCUMENT_REQUIREMENTS_NOT_MAPPED"
                            ? "No statutory document requirements mapped in official gazette/catalog for this scheme."
                            : "No mandatory document requirements recorded for this scheme."}
                        </span>
                      </div>
                    )}
                  </div>
                ) : (
                  <div>
                    <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Required Documents (comma separated) *</label>
                    <input
                      type="text"
                      placeholder="Aadhaar Card, Bank Passbook, Income Certificate"
                      value={formDocs}
                      onChange={(e) => setFormDocs(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.requiredDocuments ? "border-rose-400 bg-rose-50/20 text-rose-805" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.requiredDocuments && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.requiredDocuments}</p>}
                  </div>
                )}

                {/* Application Steps */}
                <div>
                  <label className="block font-bold text-slate-600 dark:text-slate-300 mb-1">Application Steps (comma separated)</label>
                  <input
                    type="text"
                    placeholder="Visit bank branch, Fill form, Submit nominee"
                    value={formSteps}
                    onChange={(e) => setFormSteps(e.target.value)}
                    className={`w-full px-3 py-2 border ${formErrors.steps ? "border-rose-400 bg-rose-50/20 text-rose-805" : "border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-100"} focus:bg-white dark:focus:bg-slate-800 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                  />
                  {formErrors.steps && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.steps}</p>}
                </div>

                {/* Submit Action Buttons */}
                <div className="flex items-center justify-between pt-4 border-t border-slate-100 dark:border-slate-800">
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">
                    {modalMode === "add" && (
                      <span className="flex items-center gap-1 text-slate-600 dark:text-slate-400">
                        <Info className="h-3.5 w-3.5 text-indigo-500 dark:text-indigo-400" />
                        AI Extraction creates a <strong className="font-semibold text-slate-800 dark:text-slate-200">Draft</strong> for administrative review.
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setIsModalOpen(false)}
                      className="px-4 py-2 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 rounded-xl font-bold transition"
                    >
                      Cancel
                    </button>
                    {modalMode === "add" && (
                      <button
                        type="button"
                        onClick={(e) => {
                          setFormStatus("draft");
                          setTimeout(() => {
                            const form = e.target.closest('form');
                            if (form?.requestSubmit) {
                              form.requestSubmit();
                            } else {
                              form?.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
                            }
                          }, 50);
                        }}
                        className="px-4 py-2 border border-indigo-200 dark:border-indigo-800 bg-indigo-50 dark:bg-indigo-950/40 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 rounded-xl font-bold transition"
                      >
                        Save as Draft
                      </button>
                    )}
                    <button
                      type="submit"
                      onClick={() => {
                        if (modalMode === "add" && formStatus === "draft") {
                          setFormStatus("published");
                        }
                      }}
                      className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold shadow-sm transition"
                    >
                      {modalMode === "edit" ? "Save Configurations" : "Publish Live Scheme"}
                    </button>
                  </div>
                </div>
              </form>

              {/* Side Quality Checker */}
              <div className="w-72 bg-slate-50 dark:bg-slate-850 border-l border-slate-200 dark:border-slate-800 p-5 overflow-y-auto shrink-0 space-y-4 select-none">
                <div>
                  <span className="block font-bold text-slate-800 dark:text-slate-200 uppercase tracking-wider text-[9px]">Portal Readiness Score</span>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-2xl font-black text-slate-900 dark:text-slate-100">{checkResults.score}%</span>
                    <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${
                      checkResults.score === 100
                        ? "bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800"
                        : checkResults.score >= 60
                        ? "bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 border-amber-200 dark:border-amber-800"
                        : "bg-rose-100 dark:bg-rose-950/60 text-rose-800 dark:text-rose-300 border-rose-200 dark:border-rose-800"
                    }`}>
                      {checkResults.score === 100 ? "Ready to Launch" : "Needs Triage"}
                    </span>
                  </div>
                  <div className="h-1.5 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden mt-1.5">
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        checkResults.score === 100 ? "bg-emerald-500" : checkResults.score >= 60 ? "bg-amber-400" : "bg-rose-500"
                      }`}
                      style={{ width: `${checkResults.score}%` }}
                    />
                  </div>
                </div>

                <div className="space-y-3 pt-3 border-t border-slate-200 dark:border-slate-700">
                  <span className="font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider text-[9px] block">Audit Checklists</span>
                  <div className="space-y-2">
                    {checkResults.checks.map((chk) => (
                      <div key={chk.id} className="space-y-0.5 text-[11px] font-medium">
                        <div className="flex items-center gap-1.5">
                          <span className={chk.met ? "text-emerald-600 dark:text-emerald-400 font-bold" : "text-rose-500 font-bold"}>
                            {chk.met ? "✓" : "✗"}
                          </span>
                          <span className={chk.met ? "text-slate-700 dark:text-slate-300" : "text-slate-500 dark:text-slate-400"}>{chk.name}</span>
                        </div>
                        {!chk.met && (
                          <p className="text-[10px] text-slate-400 dark:text-slate-500 pl-3.5 leading-tight">{chk.tip}</p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </div>

            </div>
          </div>
        </div>
      )}

    </div>
  );
}
