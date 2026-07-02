import React, { useState, useMemo } from "react";
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
  Clock
} from "lucide-react";
import SearchBar from "@components/ui/SearchBar";
import StatusBadge from "@components/ui/StatusBadge";
import EmptyState from "@components/ui/EmptyState";

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

// Validate and normalize scheme object to ensure all required fields exist
function validateScheme(scheme) {
  if (!scheme || typeof scheme !== 'object') {
    return null;
  }

  return {
    id: scheme.id || `scheme-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    name: scheme.name || "Untitled Scheme",
    ministry: scheme.ministry || "Unknown Ministry",
    department: scheme.department || "Unknown Department",
    category: scheme.category || "Social Welfare",
    status: scheme.status || "draft",
    description: scheme.description || "",
    officialLink: scheme.officialLink || "",
    sourceType: scheme.sourceType || "Central",
    benefits: Array.isArray(scheme.benefits) ? scheme.benefits : [],
    requiredDocuments: Array.isArray(scheme.requiredDocuments) ? scheme.requiredDocuments : [],
    steps: Array.isArray(scheme.steps) ? scheme.steps : [],
    eligibility: {
      minAge: scheme.eligibility?.minAge ?? 0,
      maxAge: scheme.eligibility?.maxAge ?? 100,
      maxIncome: scheme.eligibility?.maxIncome ?? 0,
      occupations: Array.isArray(scheme.eligibility?.occupations) ? scheme.eligibility.occupations : [],
      castes: Array.isArray(scheme.eligibility?.castes) ? scheme.eligibility.castes : [],
      genders: Array.isArray(scheme.eligibility?.genders) ? scheme.eligibility.genders : [],
      states: Array.isArray(scheme.eligibility?.states) ? scheme.eligibility.states : [],
    },
    tags: Array.isArray(scheme.tags) ? scheme.tags : [],
    faqs: Array.isArray(scheme.faqs) ? scheme.faqs : [],
    deadline: scheme.deadline || new Date(Date.now() + 180 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    approvalRate: typeof scheme.approvalRate === 'number' ? scheme.approvalRate : 85,
    lastUpdated: scheme.lastUpdated || new Date().toISOString().split("T")[0]
  };
}

function getQualityWarnings(scheme) {
  const warnings = [];
  if (!scheme) {
    return warnings;
  }
  const description = scheme.description || "";
  if (description.length < 30) {
    warnings.push("Objective is too short (< 30 chars).");
  }
  if (!scheme.eligibility?.maxIncome) {
    warnings.push("No annual household income ceiling defined.");
  }
  const requiredDocuments = scheme.requiredDocuments || [];
  if (!Array.isArray(requiredDocuments) || requiredDocuments.length === 0) {
    warnings.push("No required documents listed.");
  }
  const benefits = scheme.benefits || [];
  if (!Array.isArray(benefits) || benefits.length === 0) {
    warnings.push("No benefits listed.");
  }
  const officialLink = scheme.officialLink || "";
  if (!officialLink.startsWith("http")) {
    warnings.push("Missing official external link.");
  }
  return warnings;
}

export default function SchemeManagementConsole({
  schemes,
  addScheme,
  editScheme,
  deleteScheme,
  applications
}) {
  // Debug: Log incoming schemes prop
  if (import.meta.env.DEV) {
    console.warn("[SMC] Incoming schemes:", schemes?.length || 0);
  }

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [sortOrder, setSortOrder] = useState("latest");
  const [expandedId, setExpandedId] = useState(null);

  // Form State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState("add");
  const [editingScheme, setEditingScheme] = useState(null);

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
  const [formApprovalRate, setFormApprovalRate] = useState(85);
  const [formErrors, setFormErrors] = useState({});

  // Selection configurations
  const occupationsList = ["Farmer", "Student", "Self-Employed", "Unemployed", "Salaried"];
  const castesList = ["General", "OBC", "SC", "ST"];
  const gendersList = ["Male", "Female", "Other"];

  // Enrich schemes with dynamic metrics
  const enrichedSchemes = useMemo(() => {
    return (schemes || []).map((s) => {
      // Calculate real dynamic applications count for this scheme from AppContext
      const appCount = (applications || []).filter((a) => a.schemeId === s.id).length;

      // Use actual values from scheme object if they exist, otherwise dynamic fallbacks
      const valCode = (s.id || s.name || "").length;
      const approvalRate = s.approvalRate ?? (85 + (valCode % 11));
      const popularity = s.popularity ?? (70 + (valCode % 25));
      const performanceScore = s.performanceScore ?? (80 + (valCode % 18));
      const deadline = s.deadline ?? `2026-12-${10 + (valCode % 15)}`;
      const category = s.category ?? "Social Welfare";

      const warnings = getQualityWarnings(s);

      return {
        ...s,
        category,
        appCount,
        approvalRate,
        popularity,
        performanceScore,
        deadline,
        warnings
      };
    });
  }, [schemes, applications]);

  const filteredSchemes = useMemo(() => {
    const searchLower = (search || "").toLowerCase();
    let result = enrichedSchemes;

    // Status Filter
    if (statusFilter !== "all") {
      result = result.filter((s) => s.status === statusFilter);
    }

    // Category Filter
    if (categoryFilter !== "all") {
      result = result.filter((s) => s.category === categoryFilter);
    }

    // Search query
    if (searchLower) {
      result = result.filter(
        (s) =>
          (s.name || "").toLowerCase().includes(searchLower) ||
          (s.ministry || "").toLowerCase().includes(searchLower) ||
          (s.department || "").toLowerCase().includes(searchLower) ||
          (s.category || "").toLowerCase().includes(searchLower) ||
          (s.description || "").toLowerCase().includes(searchLower)
      );
    }

    // Sorting by lastUpdated
    result = [...result].sort((a, b) => {
      const dateA = new Date(a.lastUpdated || "1970-01-01");
      const dateB = new Date(b.lastUpdated || "1970-01-01");
      return sortOrder === "latest" ? dateB - dateA : dateA - dateB;
    });

    if (import.meta.env.DEV) {
      console.warn("[SMC] Filtered schemes count:", result.length);
    }

    return result;
  }, [enrichedSchemes, search, statusFilter, categoryFilter, sortOrder]);

  const handleOpenAdd = () => {
    setEditingScheme(null);
    setModalMode("add");
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
    setFormDeadline(new Date(Date.now() + 180 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]);
    setFormApprovalRate(85);
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenEdit = (scheme) => {
    setEditingScheme(scheme);
    setModalMode("edit");
    setFormName(scheme.name || "");
    setFormMinistry(scheme.ministry || "");
    setFormDepartment(scheme.department || "");
    setFormCategory(scheme.category || "Social Welfare");
    setFormStatus(scheme.status || "draft");
    setFormDesc(scheme.description || "");
    setFormMinAge(scheme.eligibility?.minAge ?? 18);
    setFormMaxAge(scheme.eligibility?.maxAge ?? 100);
    setFormMaxIncome(scheme.eligibility?.maxIncome ?? 300000);
    setFormOccupations(scheme.eligibility?.occupations ?? []);
    setFormCastes(scheme.eligibility?.castes ?? []);
    setFormGenders(scheme.eligibility?.genders ?? []);
    setFormDocs(Array.isArray(scheme.requiredDocuments) ? scheme.requiredDocuments.join(", ") : "");
    setFormSteps(Array.isArray(scheme.steps) ? scheme.steps.join(", ") : "");
    setFormBenefits(Array.isArray(scheme.benefits) ? scheme.benefits.join(", ") : "");
    setFormOfficialLink(scheme.officialLink || "");
    setFormSourceType(scheme.sourceType || "Central");
    setFormDeadline(scheme.deadline || "");
    setFormApprovalRate(scheme.approvalRate ?? 85);
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleDuplicate = (scheme) => {
    const validated = validateScheme(scheme);
    if (!validated) {
      console.error("Cannot duplicate invalid scheme:", scheme);
      return;
    }
    const duplicated = validateScheme({
      ...validated,
      id: undefined, // Let context dynamically resolve collision-free ID
      name: `${validated.name} (Copy)`,
      status: "draft"
    });
    if (duplicated) {
      addScheme(duplicated);
    }
  };

  const handleSetStatus = (scheme, nextStatus) => {
    editScheme({ ...scheme, status: nextStatus });
  };

  const handleFormSubmit = (e) => {
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

    if (!formOfficialLink.trim()) {
      errors.officialLink = "Official portal URL is required.";
    } else {
      try {
        new URL(formOfficialLink);
        if (!formOfficialLink.startsWith("http://") && !formOfficialLink.startsWith("https://")) {
          errors.officialLink = "URL must start with http:// or https://.";
        }
      } catch (_err) {
        errors.officialLink = "Please enter a valid URL.";
      }
    }

    const minAgeNum = Number(formMinAge);
    const maxAgeNum = Number(formMaxAge);
    if (isNaN(minAgeNum) || minAgeNum < 0 || minAgeNum > 100) {
      errors.minAge = "Min age must be between 0 and 100.";
    }
    if (isNaN(maxAgeNum) || maxAgeNum < 0 || maxAgeNum > 100) {
      errors.maxAge = "Max age must be between 0 and 100.";
    } else if (maxAgeNum < minAgeNum) {
      errors.maxAge = "Max age must be greater than or equal to min age.";
    }

    const maxIncomeNum = Number(formMaxIncome);
    if (isNaN(maxIncomeNum) || maxIncomeNum < 0) {
      errors.maxIncome = "Max income ceiling must be a positive number.";
    }

    const benefitsArray = (formBenefits || "").split(",").map((s) => s.trim()).filter(Boolean);
    if (benefitsArray.length === 0) {
      errors.benefits = "At least one benefit is required.";
    }

    const docsArray = (formDocs || "").split(",").map((s) => s.trim()).filter(Boolean);
    if (docsArray.length === 0) {
      errors.requiredDocuments = "At least one required document is required.";
    }

    if (!formDeadline) {
      errors.deadline = "Closing deadline date is required.";
    }

    const approvalRateNum = Number(formApprovalRate);
    if (isNaN(approvalRateNum) || approvalRateNum < 0 || approvalRateNum > 100) {
      errors.approvalRate = "Avg. approval rate must be a percentage between 0 and 100.";
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setFormErrors({});

    const schemeData = validateScheme({
      id: modalMode === "edit" ? editingScheme?.id : undefined,
      name: formName.trim(),
      ministry: formMinistry.trim(),
      department: formDepartment.trim(),
      category: formCategory,
      status: formStatus,
      description: formDesc.trim(),
      officialLink: formOfficialLink.trim(),
      sourceType: formSourceType,
      benefits: benefitsArray,
      requiredDocuments: docsArray,
      steps: (formSteps || "").split(",").map((s) => s.trim()).filter(Boolean),
      eligibility: {
        minAge: minAgeNum,
        maxAge: maxAgeNum,
        maxIncome: maxIncomeNum,
        occupations: formOccupations,
        castes: formCastes,
        genders: formGenders,
        states: editingScheme?.eligibility?.states || [],
      },
      tags: editingScheme?.tags || [formCategory.toLowerCase(), ...formOccupations.map(o => o.toLowerCase())],
      faqs: modalMode === "edit" ? (editingScheme?.faqs || []) : [
        { question: "How do I check application status?", answer: "Go to your Application Tracker under the citizen portal." }
      ],
      deadline: formDeadline,
      approvalRate: approvalRateNum
    });

    if (!schemeData) {
      console.error("Failed to validate scheme data");
      return;
    }

    if (modalMode === "edit") {
      editScheme(schemeData);
    } else {
      addScheme(schemeData);
    }
    setIsModalOpen(false);
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
      <div className="flex flex-col lg:flex-row justify-between items-stretch lg:items-center gap-3 bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
        <div className="flex flex-col md:flex-row gap-3 w-full lg:flex-1">
          <SearchBar
            value={search}
            onChange={setSearch}
            placeholder="Search schemes by title, ministry, department, or category..."
            className="flex-1"
          />

          <div className="flex flex-wrap gap-2 select-none">
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="text-xs border border-slate-200 rounded-xl px-3 py-2 bg-slate-50 text-slate-700 hover:bg-slate-100/50 focus:outline-none transition cursor-pointer font-bold"
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
              onChange={(e) => setSortOrder(e.target.value)}
              className="text-xs border border-slate-200 rounded-xl px-3 py-2 bg-slate-50 text-slate-700 hover:bg-slate-100/50 focus:outline-none transition cursor-pointer font-bold"
            >
              <option value="latest">Latest Updated</option>
              <option value="oldest">Oldest Updated</option>
            </select>
          </div>
        </div>

        <div className="flex items-center gap-2 w-full lg:w-auto justify-between shrink-0 border-t lg:border-t-0 pt-3 lg:pt-0 select-none">
          <div className="flex bg-slate-100 p-1 rounded-xl text-[11px] font-bold">
            {["all", "published", "draft", "archived"].map((f) => (
              <button
                key={f}
                onClick={() => setStatusFilter(f)}
                className={`px-3 py-1.5 rounded-lg capitalize transition whitespace-nowrap ${
                  statusFilter === f ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
                }`}
              >
                {f}
              </button>
            ))}
          </div>
          <button
            onClick={handleOpenAdd}
            className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center gap-1.5 shrink-0 animate-in fade-in"
          >
            <PlusCircle className="h-4 w-4" />
            <span>Add Scheme</span>
          </button>
        </div>
      </div>

      {/* ── Cards Grid ── */}
      {filteredSchemes.length === 0 ? (
        <EmptyState
          icon={FileWarning}
          title="No Schemes Configured"
          description="No matching live, draft, or archived schemes found. Try adjusting filters or search query."
          action={{
            label: "Reset Filters",
            variant: "secondary",
            onClick: () => {
              setSearch("");
              setStatusFilter("all");
              setCategoryFilter("all");
              setSortOrder("latest");
            }
          }}
          className="bg-white border border-slate-200 rounded-2xl"
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {filteredSchemes.map((scheme) => {
            const isExpanded = expandedId === scheme.id;

            return (
              <div
                key={scheme.id}
                className="bg-white border border-slate-200 rounded-2xl shadow-sm hover:shadow-md transition duration-200 flex flex-col overflow-hidden relative group animate-in fade-in zoom-in-95 duration-150"
              >
                {/* Clickable Card Body (detailed editor launcher) */}
                <div
                  onClick={() => handleOpenEdit(scheme)}
                  className="p-5 flex-1 space-y-4 cursor-pointer hover:bg-slate-50/40 transition duration-150"
                  title="Click anywhere to edit details"
                >
                  {/* Category Tag + Status Badge Row */}
                  <div className="flex justify-between items-start gap-2 select-none">
                    <span className="px-2 py-0.5 bg-slate-100 text-slate-600 text-[9px] font-bold rounded-lg border border-slate-200 uppercase tracking-wider flex items-center gap-1">
                      <Folder className="h-3.5 w-3.5 text-slate-400" />
                      {scheme.category || "Social Welfare"}
                    </span>
                    <StatusBadge status={scheme.status} variant="pill" size="xs" />
                  </div>

                  {/* Scheme Name, Ministry, Department */}
                  <div className="space-y-1">
                    <h3 className="text-sm font-black text-slate-900 tracking-tight leading-snug group-hover:text-indigo-600 transition">
                      {scheme.name}
                    </h3>
                    <div className="text-[10px] font-semibold text-slate-500 space-y-0.5">
                      <p className="flex items-center gap-1 font-bold text-slate-700">
                        <span className="text-[8px] text-slate-400 font-extrabold uppercase">Ministry:</span> {scheme.ministry}
                      </p>
                      <p className="flex items-center gap-1 font-bold text-slate-600">
                        <span className="text-[8px] text-slate-400 font-extrabold uppercase">Dept:</span> {scheme.department}
                      </p>
                    </div>
                  </div>

                  {/* Summary Demographics */}
                  <div className="grid grid-cols-3 gap-2.5 text-[10px] text-slate-500 font-semibold bg-slate-50 p-3 rounded-xl border border-slate-100 select-none">
                    <div>
                      <span className="text-slate-400 block uppercase text-[8px] font-extrabold mb-0.5">Applications</span>
                      <span className="font-extrabold text-slate-800 text-xs flex items-center gap-1">
                        <BarChart3 className="h-3.5 w-3.5 text-indigo-500" />
                        {scheme.appCount}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block uppercase text-[8px] font-extrabold mb-0.5">Approval Rate</span>
                      <span className="font-extrabold text-slate-800 text-xs flex items-center gap-1">
                        <Award className="h-3.5 w-3.5 text-emerald-500" />
                        {scheme.approvalRate || 85}%
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block uppercase text-[8px] font-extrabold mb-0.5">Deadline</span>
                      <span className="font-extrabold text-slate-800 text-xs flex items-center gap-1">
                        <Calendar className="h-3.5 w-3.5 text-rose-500" />
                        {scheme.deadline}
                      </span>
                    </div>
                  </div>

                  {/* Quality Warnings Indicator */}
                  {(scheme.warnings || []).length > 0 && (
                    <div
                      className="bg-rose-50 border border-rose-100 p-2.5 rounded-xl flex items-start gap-2 text-rose-700 text-[10px] leading-relaxed select-none"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <AlertTriangle className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" />
                      <div>
                        <span className="font-bold">Content Warnings ({scheme.warnings.length}):</span>
                        <ul className="list-disc list-inside mt-0.5 font-medium text-rose-600">
                          {(scheme.warnings || []).slice(0, 2).map((w, idx) => (
                            <li key={idx} className="truncate max-w-xs">{w}</li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  )}

                  {/* Action link & clock */}
                  <div className="flex justify-between items-center text-[9px] text-slate-400 font-bold border-t border-slate-100 pt-2.5 select-none">
                    <span className="flex items-center gap-1">
                      <Clock className="h-3.5 w-3.5 text-slate-400" />
                      Updated: {scheme.lastUpdated}
                    </span>
                    <span className="text-indigo-650 text-indigo-600 group-hover:underline">
                      Detailed Editor &rarr;
                    </span>
                  </div>

                  {/* Expanded Detail Panel */}
                  {isExpanded && (
                    <div
                      className="border-t border-slate-100 pt-3 mt-3 text-xs text-slate-655 space-y-3 animate-in slide-in-from-top-2 duration-150"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div>
                        <span className="font-bold text-slate-700 block mb-0.5">Objective Goal Description</span>
                        <p className="leading-relaxed text-slate-500 font-medium">{scheme.description}</p>
                      </div>
                      <div>
                        <span className="font-bold text-slate-700 block mb-0.5">Eligibility Parameters</span>
                        <div className="flex flex-wrap gap-1.5 pt-0.5">
                          <span className="px-2 py-0.5 bg-slate-100 border border-slate-200 text-slate-600 text-[9px] font-bold rounded-lg">
                            Age: {scheme.eligibility?.minAge} - {scheme.eligibility?.maxAge} Yrs
                          </span>
                          <span className="px-2 py-0.5 bg-slate-100 border border-slate-200 text-slate-600 text-[9px] font-bold rounded-lg">
                            Max Income: ₹{scheme.eligibility?.maxIncome?.toLocaleString("en-IN")}
                          </span>
                          {scheme.eligibility?.occupations?.map((occ) => (
                            <span key={occ} className="px-2 py-0.5 bg-indigo-50 border border-indigo-100 text-indigo-700 text-[9px] font-bold rounded-lg">
                              {occ}
                            </span>
                          ))}
                        </div>
                      </div>
                      <div>
                        <span className="font-bold text-slate-700 block mb-0.5">Benefits Overview</span>
                        <ul className="list-disc list-inside space-y-0.5 text-slate-500 font-medium">
                          {scheme.benefits?.map((b, idx) => <li key={idx}>{b}</li>)}
                        </ul>
                      </div>
                      <div>
                        <span className="font-bold text-slate-700 block mb-0.5">Required Files checklist</span>
                        <div className="flex flex-wrap gap-1 mt-1">
                          {scheme.requiredDocuments?.map((d) => (
                            <span key={d} className="px-2 py-0.5 bg-slate-100 border border-slate-200 text-slate-600 text-[9px] font-bold rounded-md">
                              {d}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Card Action footer */}
                <div className="p-4 border-t border-slate-100 bg-slate-50/50 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 select-none">
                  {/* Status Toggle Buttons */}
                  <div className="flex items-center gap-1.5 justify-between">
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Status:</span>
                    <div className="flex bg-slate-100 p-0.5 rounded-lg border border-slate-200">
                      {["draft", "published", "archived"].map((st) => (
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
                              : "text-slate-500 hover:text-slate-800"
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
                        className="p-2 hover:bg-slate-200 hover:text-slate-700 rounded-lg transition"
                        title="Duplicate scheme"
                      >
                        <Copy className="h-4 w-4" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          deleteScheme(scheme.id);
                        }}
                        className="p-2 hover:bg-slate-200 hover:text-rose-600 rounded-lg transition"
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
                      className="px-3 py-1 bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-lg text-[10px] font-bold flex items-center gap-1 transition"
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

      {/* ── Scheme Modal Form (Add/Edit) with Quality Sidepanel ── */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm" onClick={() => setIsModalOpen(false)} />
          <div className="relative bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-4xl w-full max-h-[85vh] flex flex-col overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-150">
            <div className="bg-slate-900 text-white p-5 flex items-center justify-between shrink-0">
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
                {/* Title and Category */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block font-bold text-slate-600 mb-1">Scheme Title *</label>
                    <input
                      type="text"
                      placeholder="e.g. Atal Pension Yojana"
                      value={formName}
                      onChange={(e) => setFormName(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.name ? "border-rose-400 bg-rose-50/20 text-rose-800" : "border-slate-200 bg-slate-50"} focus:bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.name && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.name}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 mb-1">Core Category *</label>
                    <select
                      value={formCategory}
                      onChange={(e) => setFormCategory(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.category ? "border-rose-400 bg-rose-50/20" : "border-slate-200 bg-slate-50"} rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold cursor-pointer`}
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
                    <label className="block font-bold text-slate-600 mb-1">Hosting Ministry *</label>
                    <input
                      type="text"
                      placeholder="e.g. Ministry of Finance"
                      value={formMinistry}
                      onChange={(e) => setFormMinistry(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.ministry ? "border-rose-400 bg-rose-50/20" : "border-slate-200 bg-slate-50"} focus:bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.ministry && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.ministry}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 mb-1">Executing Department *</label>
                    <input
                      type="text"
                      placeholder="e.g. PFRDA"
                      value={formDepartment}
                      onChange={(e) => setFormDepartment(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.department ? "border-rose-400 bg-rose-50/20" : "border-slate-200 bg-slate-50"} focus:bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.department && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.department}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 mb-1">Portal Visibility</label>
                    <select
                      value={formStatus}
                      onChange={(e) => setFormStatus(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold cursor-pointer"
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
                    <label className="block font-bold text-slate-600 mb-1">Official Portal URL *</label>
                    <input
                      type="url"
                      placeholder="e.g. https://pmkisan.gov.in"
                      value={formOfficialLink}
                      onChange={(e) => setFormOfficialLink(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.officialLink ? "border-rose-400 bg-rose-50/20" : "border-slate-200 bg-slate-50"} focus:bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.officialLink && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.officialLink}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 mb-1">Government Source</label>
                    <select
                      value={formSourceType}
                      onChange={(e) => setFormSourceType(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold cursor-pointer"
                    >
                      <option value="Central">Central Government</option>
                      <option value="State">State Government</option>
                    </select>
                  </div>
                </div>

                {/* Deadline and Approval Rate */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block font-bold text-slate-600 mb-1">Closing Deadline *</label>
                    <input
                      type="date"
                      value={formDeadline}
                      onChange={(e) => setFormDeadline(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.deadline ? "border-rose-400 bg-rose-50/20" : "border-slate-200 bg-slate-50"} focus:bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.deadline && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.deadline}</p>}
                  </div>
                  <div>
                    <label className="block font-bold text-slate-600 mb-1">Avg. Approval Rate (%) *</label>
                    <input
                      type="number"
                      min="0"
                      max="100"
                      placeholder="e.g. 92"
                      value={formApprovalRate}
                      onChange={(e) => setFormApprovalRate(e.target.value)}
                      className={`w-full px-3 py-2 border ${formErrors.approvalRate ? "border-rose-400 bg-rose-50/20" : "border-slate-200 bg-slate-50"} focus:bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                    />
                    {formErrors.approvalRate && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.approvalRate}</p>}
                  </div>
                </div>

                {/* Objective Goal Description */}
                <div>
                  <label className="block font-bold text-slate-600 mb-1">Objective Goal Description *</label>
                  <textarea
                    placeholder="Describe the scheme objectives, funding models, and coverage..."
                    value={formDesc}
                    onChange={(e) => setFormDesc(e.target.value)}
                    rows="3"
                    className={`w-full px-3 py-2 border ${formErrors.description ? "border-rose-400 bg-rose-50/20 text-rose-800" : "border-slate-200 bg-slate-50"} focus:bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                  />
                  {formErrors.description && <p className="text-rose-500 text-[10px] mt-1 font-bold">{formErrors.description}</p>}
                </div>

                {/* Eligibility Criteria Boundaries */}
                <div className="border border-slate-200 p-4 rounded-xl space-y-4 bg-slate-50/50">
                  <span className="block font-bold text-slate-800 uppercase tracking-wider text-[10px]">Eligibility Criteria Parameters</span>
                  <div className="grid grid-cols-3 gap-3">
                    <div>
                      <label className="block font-bold text-slate-600 mb-1">Min Age Limit</label>
                      <input
                        type="number"
                        value={formMinAge}
                        onChange={(e) => setFormMinAge(e.target.value)}
                        className={`w-full px-3 py-2 border ${formErrors.minAge ? "border-rose-400 text-rose-800" : "border-slate-200"} bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold`}
                      />
                      {formErrors.minAge && <p className="text-rose-500 text-[9px] mt-1 font-bold">{formErrors.minAge}</p>}
                    </div>
                    <div>
                      <label className="block font-bold text-slate-600 mb-1">Max Age Limit</label>
                      <input
                        type="number"
                        value={formMaxAge}
                        onChange={(e) => setFormMaxAge(e.target.value)}
                        className={`w-full px-3 py-2 border ${formErrors.maxAge ? "border-rose-400 text-rose-800" : "border-slate-200"} bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold`}
                      />
                      {formErrors.maxAge && <p className="text-rose-500 text-[9px] mt-1 font-bold">{formErrors.maxAge}</p>}
                    </div>
                    <div>
                      <label className="block font-bold text-slate-600 mb-1">Max Income Ceiling (₹)</label>
                      <input
                        type="number"
                        value={formMaxIncome}
                        onChange={(e) => setFormMaxIncome(e.target.value)}
                        className={`w-full px-3 py-2 border ${formErrors.maxIncome ? "border-rose-400 text-rose-800" : "border-slate-200"} bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-semibold`}
                      />
                      {formErrors.maxIncome && <p className="text-rose-500 text-[9px] mt-1 font-bold">{formErrors.maxIncome}</p>}
                    </div>
                  </div>

                  <div className="space-y-3 pt-3 border-t border-slate-200">
                    {[
                      { label: "Target Occupations", list: occupationsList, state: formOccupations, setState: setFormOccupations },
                      { label: "Target Social Categories", list: castesList, state: formCastes, setState: setFormCastes },
                      { label: "Target Genders", list: gendersList, state: formGenders, setState: setFormGenders },
                    ].map(({ label, list, state, setState }) => (
                      <div key={label}>
                        <span className="block font-bold text-slate-600 mb-1.5">{label} <span className="text-slate-400 font-normal">(empty = open to all)</span></span>
                        <div className="flex flex-wrap gap-2">
                          {list.map((item) => {
                            const active = state.includes(item);
                            return (
                              <button key={item} type="button"
                                onClick={() => {
                                  setState(prev => prev.includes(item) ? prev.filter(x => x !== item) : [...prev, item]);
                                }}
                                className={`px-2.5 py-1 rounded-lg border text-[10px] font-bold transition ${active ? "bg-indigo-600 text-white border-indigo-600" : "bg-white text-slate-600 border-slate-200 hover:bg-slate-50"}`}>
                                {item}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Benefits, steps, docs inputs */}
                <div className="space-y-3">
                  {[
                    { label: "Benefits (comma separated) *", placeholder: "Guaranteed pension, Corpus return to nominee", value: formBenefits, set: setFormBenefits, error: formErrors.benefits },
                    { label: "Required Documents (comma separated) *", placeholder: "Aadhaar Card, Bank Passbook, Income Certificate", value: formDocs, set: setFormDocs, error: formErrors.requiredDocuments },
                    { label: "Application Steps (comma separated)", placeholder: "Visit bank branch, Fill form, Submit nominee", value: formSteps, set: setFormSteps, error: formErrors.steps },
                  ].map(({ label, placeholder, value, set, error }) => (
                    <div key={label}>
                      <label className="block font-bold text-slate-600 mb-1">{label}</label>
                      <input
                        type="text"
                        placeholder={placeholder}
                        value={value}
                        onChange={(e) => set(e.target.value)}
                        className={`w-full px-3 py-2 border ${error ? "border-rose-400 bg-rose-50/20 text-rose-805" : "border-slate-200 bg-slate-50"} focus:bg-white rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium`}
                      />
                      {error && <p className="text-rose-500 text-[10px] mt-1 font-bold">{error}</p>}
                    </div>
                  ))}
                </div>

                {/* Submit Action Buttons */}
                <div className="flex justify-end gap-2 pt-4 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => setIsModalOpen(false)}
                    className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl font-bold transition"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold shadow-sm transition"
                  >
                    {modalMode === "edit" ? "Save Configurations" : "Provision Scheme"}
                  </button>
                </div>
              </form>

              {/* Side Quality Checker */}
              <div className="w-72 bg-slate-50 border-l border-slate-200 p-5 overflow-y-auto shrink-0 space-y-4 select-none">
                <div>
                  <span className="block font-bold text-slate-800 uppercase tracking-wider text-[9px]">Portal Readiness Score</span>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-2xl font-black text-slate-900">{checkResults.score}%</span>
                    <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${
                      checkResults.score === 100
                        ? "bg-emerald-100 text-emerald-800 border-emerald-200"
                        : checkResults.score >= 60
                        ? "bg-amber-100 text-amber-800 border-amber-200"
                        : "bg-rose-100 text-rose-800 border-rose-200"
                    }`}>
                      {checkResults.score === 100 ? "Ready to Launch" : "Needs Triage"}
                    </span>
                  </div>
                  <div className="h-1.5 bg-slate-200 rounded-full overflow-hidden mt-1.5">
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        checkResults.score === 100 ? "bg-emerald-500" : checkResults.score >= 60 ? "bg-amber-400" : "bg-rose-500"
                      }`}
                      style={{ width: `${checkResults.score}%` }}
                    />
                  </div>
                </div>

                <div className="space-y-3 pt-3 border-t border-slate-200">
                  <span className="font-bold text-slate-700 uppercase tracking-wider text-[9px] block">Audit Checklists</span>
                  <div className="space-y-2">
                    {checkResults.checks.map((chk) => (
                      <div key={chk.id} className="space-y-0.5 text-[11px] font-medium">
                        <div className="flex items-center gap-1.5">
                          <span className={chk.met ? "text-emerald-600 font-bold" : "text-rose-500 font-bold"}>
                            {chk.met ? "✓" : "✗"}
                          </span>
                          <span className={chk.met ? "text-slate-700" : "text-slate-500"}>{chk.name}</span>
                        </div>
                        {!chk.met && (
                          <p className="text-[10px] text-slate-400 pl-3.5 leading-tight">{chk.tip}</p>
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
