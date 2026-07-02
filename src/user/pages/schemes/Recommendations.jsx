import React, { useState, useMemo, useEffect } from "react";
import { Link } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { checkEligibility } from "@utils/eligibilityEngine";
import { getDocReadinessForScheme } from "@utils/documentReadiness";
import { getSchemeRecommendationDetails } from "@data/mockRecommendations";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import { CardSkeleton } from "@components/ui/LoadingSkeleton";
import EmptyState from "@components/ui/EmptyState";
import {
  CheckCircle,
  XCircle,
  AlertCircle,
  ArrowRight,
  FileText,
  SlidersHorizontal,
  BookmarkPlus,
  BookmarkCheck,
  Search,
  X,
  CheckCircle2,
  HelpCircle,
  ClipboardCheck,
  Sparkles,
  Clock,
  Building2,
  MessageSquare
} from "lucide-react";

const STATUS_META = {
  eligible: {
    badge: "bg-emerald-50 text-emerald-805 text-emerald-800 border-emerald-250 border-emerald-200",
    border: "border-l-emerald-600",
    icon: CheckCircle,
    iconColor: "text-emerald-600",
  },
  possibly_eligible: {
    badge: "bg-amber-50 text-amber-805 text-amber-800 border-amber-250 border-amber-200",
    border: "border-l-amber-500",
    icon: AlertCircle,
    iconColor: "text-amber-500",
  },
  not_eligible: {
    badge: "bg-rose-50 text-rose-805 text-rose-800 border-rose-250 border-rose-200",
    border: "border-l-rose-500",
    icon: XCircle,
    iconColor: "text-rose-500",
  },
};

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

function mapCategoryToTags(cat) {
  const c = cat.toLowerCase();
  switch (c) {
    case "agriculture": return ["agriculture", "farmer", "rural", "crop"];
    case "education": return ["education", "student", "students", "school", "college"];
    case "healthcare": return ["healthcare", "health", "medical", "hospital"];
    case "women & child welfare": return ["women", "child", "girl", "mother"];
    case "employment": return ["employment", "job", "work", "unorganized-sector"];
    case "housing": return ["housing", "urban", "rural", "house"];
    case "entrepreneurship": return ["entrepreneurship", "business", "self-employed", "shg"];
    case "pension": return ["pension", "retirement", "elderly"];
    case "disability": return ["disability", "disabled", "special-needs"];
    case "social welfare": return ["social-welfare", "welfare", "sc", "st", "obc", "bpl"];
    case "financial assistance": return ["financial-assistance", "subsidy", "income-support", "savings"];
    case "skill development": return ["skill-development", "skill", "skills", "training"];
    case "minority welfare": return ["minority-welfare", "minority", "sc", "st", "obc"];
    case "student scholarships": return ["scholarship", "students", "education"];
    default: return [c];
  }
}

export default function Recommendations() {
  const { profile, user, schemes, documents, saveScheme, isSaved, hasApplied, t } = useApp();

  // Page-level UI states
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState("all"); // eligible, possibly_eligible, not_eligible, all
  const [selectedState, setSelectedState] = useState("all");
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [schemeType, setSchemeType] = useState("all"); // all, Central, State
  const [minMatchScore, setMinMatchScore] = useState(0);
  const [minDocReadiness, setMinDocReadiness] = useState(0);
  const [selectedDeadlineRange, setSelectedDeadlineRange] = useState("all"); // all, 7, 30
  const [sortBy, setSortBy] = useState("match_score");

  // Mobile drawer state
  const [isMobileFilterOpen, setIsMobileFilterOpen] = useState(false);

  // Chat widget state
  const [aiChatOpen, setAiChatOpen] = useState(false);
  const [aiInitialQuery, setAiInitialQuery] = useState("");

  // Skeleton loading state
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 900);
    return () => clearTimeout(timer);
  }, []);

  // Evaluate schemes with eligibility and recommendations data
  const evaluatedSchemes = useMemo(() => {
    return schemes
      .filter((s) => s.status !== "archived")
      .map((scheme) => {
        const evaluation = checkEligibility(profile, scheme, documents);
        const readiness = getDocReadinessForScheme(scheme.requiredDocuments, documents);
        const recDetails = getSchemeRecommendationDetails(scheme.id, scheme.name);
        return { scheme, evaluation, readiness, recDetails };
      });
  }, [schemes, profile, documents]);

  // States & Tags extraction
  const availableStates = useMemo(() => {
    const statesSet = new Set();
    schemes.forEach((s) => {
      if (s.eligibility?.states && s.eligibility.states.length > 0) {
        s.eligibility.states.forEach((st) => statesSet.add(st));
      }
    });
    return Array.from(statesSet).sort();
  }, [schemes]);

  const availableCategories = useMemo(() => {
    const tagsSet = new Set();
    schemes.forEach((s) => {
      if (s.tags && s.tags.length > 0) {
        s.tags.forEach((tag) => tagsSet.add(tag));
      }
    });
    return Array.from(tagsSet).sort();
  }, [schemes]);

  const handleClearFilters = () => {
    setSearchQuery("");
    setActiveFilter("all");
    setSelectedState("all");
    setSelectedCategory("all");
    setSchemeType("all");
    setMinMatchScore(0);
    setMinDocReadiness(0);
    setSelectedDeadlineRange("all");
    setSortBy("match_score");
  };

  const handleAskAI = (queryText) => {
    setAiInitialQuery(queryText);
    setAiChatOpen(true);
  };

  // Filter logic
  const filteredSchemes = useMemo(() => {
    return evaluatedSchemes.filter(({ scheme, evaluation, readiness, recDetails }) => {
      // 1. Search Query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesText =
          scheme.name.toLowerCase().includes(q) ||
          scheme.description.toLowerCase().includes(q) ||
          scheme.ministry.toLowerCase().includes(q) ||
          (scheme.department && scheme.department.toLowerCase().includes(q)) ||
          scheme.tags.some((t) => t.toLowerCase().includes(q));
        if (!matchesText) {
return false;
}
      }

      // 2. Government Scheme Type & Residence State Logic
      const citizenState = profile?.state || user?.state || "Tamil Nadu";
      if (schemeType === "Central") {
        if (scheme.sourceType !== "Central") {
return false;
}
      } else if (schemeType === "State") {
        if (scheme.sourceType !== "State") {
return false;
}
        const targetStates = scheme.eligibility?.states || [];
        if (targetStates.length > 0 && !targetStates.includes(citizenState)) {
          return false;
        }
      } else {
        // Both (Default)
        if (scheme.sourceType === "State") {
          const targetStates = scheme.eligibility?.states || [];
          if (targetStates.length > 0 && !targetStates.includes(citizenState)) {
            return false;
          }
        }
      }

      // 3. Core Category (Tags)
      if (selectedCategory !== "all") {
        const allowedTags = mapCategoryToTags(selectedCategory);
        const matchesCategory = scheme.tags.some(tag => allowedTags.includes(tag.toLowerCase()));
        if (!matchesCategory) {
return false;
}
      }

      // 5. Match Status Tab
      if (activeFilter !== "all" && evaluation.status !== activeFilter) {
        return false;
      }

      // 6. Match Score Slider
      if (evaluation.matchScore < minMatchScore) {
        return false;
      }

      // 7. Doc Readiness Slider
      if (readiness.readinessScore < minDocReadiness) {
        return false;
      }

      // 8. Deadline Range
      if (selectedDeadlineRange !== "all") {
        const days = recDetails.deadlineDays;
        if (selectedDeadlineRange === "7" && days > 7) {
return false;
}
        if (selectedDeadlineRange === "30" && days > 30) {
return false;
}
      }

      return true;
    });
  }, [evaluatedSchemes, searchQuery, selectedCategory, schemeType, activeFilter, minMatchScore, minDocReadiness, selectedDeadlineRange, profile, user]);

  // Sort logic
  const sortedSchemes = useMemo(() => {
    const copy = [...filteredSchemes];
    if (sortBy === "match_score") {
      return copy.sort((a, b) => b.evaluation.matchScore - a.evaluation.matchScore);
    }
    if (sortBy === "doc_readiness") {
      return copy.sort((a, b) => b.readiness.readinessScore - a.readiness.readinessScore);
    }
    if (sortBy === "alpha") {
      return copy.sort((a, b) => a.scheme.name.localeCompare(b.scheme.name));
    }
    return copy;
  }, [filteredSchemes, sortBy]);

  const counts = useMemo(() => {
    return {
      all: evaluatedSchemes.length,
      eligible: evaluatedSchemes.filter((i) => i.evaluation.status === "eligible").length,
      possibly_eligible: evaluatedSchemes.filter((i) => i.evaluation.status === "possibly_eligible").length,
      not_eligible: evaluatedSchemes.filter((i) => i.evaluation.status === "not_eligible").length,
    };
  }, [evaluatedSchemes]);

  return (
    <div className="space-y-6">
      {/* Header Info */}
      <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-xs space-y-1">
        <h1 className="text-xl font-black text-slate-900 tracking-tight">{t("rec_title") || "Recommended Schemes"}</h1>
        <p className="text-xs text-slate-500 leading-normal">
          {t("rec_subtitle") || "Explore e-governance benefit programs mapped dynamically to your socio-economic criteria."}
        </p>
      </div>

      {/* Primary Layout Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">

        {/* STICKY FILTER SIDEBAR (Desktop) */}
        <aside className="hidden lg:block lg:col-span-1 bg-white border border-slate-200 p-5 rounded-2xl shadow-xs space-y-5 lg:sticky lg:top-24 lg:self-start lg:max-h-[calc(100vh-120px)] lg:overflow-y-auto">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h2 className="text-xs font-black text-slate-900 uppercase tracking-widest flex items-center gap-1.5">
              <SlidersHorizontal className="h-4 w-4 text-slate-500" />
              {t("rec_filter_title") || "Filter Schemes"}
            </h2>
            <button
              onClick={handleClearFilters}
              className="text-[10px] text-indigo-650 text-indigo-600 font-extrabold hover:underline"
            >
              {t("rec_reset_btn") || "Reset"}
            </button>
          </div>

          {/* Residence State Filter (Read-Only Card style) */}
          <div className="space-y-1.5 p-3.5 bg-slate-50 border border-slate-200 rounded-2xl">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">{t("rec_residence_state") || "Residence State"}</span>
            <div className="flex items-center gap-1.5 font-bold text-xs text-slate-800 mt-1">
              <span>📍</span>
              <span>{profile?.state || user?.state || "Tamil Nadu"}</span>
            </div>
            <div className="flex justify-between items-center text-[10px] text-slate-400 mt-2 border-t border-slate-200/60 pt-1.5">
              <span>{t("rec_using_profile") || "Using your profile"}</span>
              <Link to="/profile" className="text-indigo-650 text-indigo-600 hover:text-indigo-805 font-extrabold flex items-center gap-0.5 transition">
                {t("rec_edit_profile") || "Edit Profile"} <ArrowRight className="h-3 w-3" />
              </Link>
            </div>
          </div>

          {/* Category Filter */}
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">{t("rec_core_category") || "Core Category"}</label>
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="w-full text-xs border border-slate-200 rounded-xl px-3 py-2 bg-slate-50 text-slate-700 hover:bg-slate-100/50 focus:outline-none transition cursor-pointer"
            >
              <option value="all">{t("rec_all_categories") || "All Categories"}</option>
              {CORE_CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </div>

          {/* Government Scheme Type (Central / State) */}
          <div className="space-y-2">
            <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">{t("rec_scheme_type") || "Government Scheme Type"}</label>
            <div className="space-y-1.5 text-xs text-slate-700">
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input
                  type="radio"
                  name="schemeType"
                  value="all"
                  checked={schemeType === "all"}
                  onChange={() => setSchemeType("all")}
                  className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                />
                <span>{t("rec_type_both") || "Both (Default)"}</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input
                  type="radio"
                  name="schemeType"
                  value="Central"
                  checked={schemeType === "Central"}
                  onChange={() => setSchemeType("Central")}
                  className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                />
                <span>{t("rec_type_central") || "Central Government"}</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input
                  type="radio"
                  name="schemeType"
                  value="State"
                  checked={schemeType === "State"}
                  onChange={() => setSchemeType("State")}
                  className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                />
                <span>{t("rec_type_state") || "State Government"}</span>
              </label>
            </div>
          </div>

          {/* NEW: Deadline Filter */}
          <div className="space-y-2">
            <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">{t("rec_closing_date") || "Closing Date"}</label>
            <div className="space-y-1.5 text-xs text-slate-700">
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input
                  type="radio"
                  name="deadlineRange"
                  value="all"
                  checked={selectedDeadlineRange === "all"}
                  onChange={() => setSelectedDeadlineRange("all")}
                  className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                />
                <span>{t("rec_all_deadlines") || "All Deadlines"}</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input
                  type="radio"
                  name="deadlineRange"
                  value="7"
                  checked={selectedDeadlineRange === "7"}
                  onChange={() => setSelectedDeadlineRange("7")}
                  className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                />
                <span>{t("rec_closing_soon") || "Closing Soon (< 7 Days)"}</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input
                  type="radio"
                  name="deadlineRange"
                  value="30"
                  checked={selectedDeadlineRange === "30"}
                  onChange={() => setSelectedDeadlineRange("30")}
                  className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                />
                <span>{t("rec_closing_month") || "Closing This Month (< 30 Days)"}</span>
              </label>
            </div>
          </div>

          {/* Match Score Range Slider */}
          <div className="space-y-2 border-t border-slate-100 pt-4">
            <div className="flex justify-between items-center">
              <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{t("rec_min_match") || "Min Match Score"}</label>
              <span className="text-xs font-bold text-indigo-600">{minMatchScore}%+</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              step="5"
              value={minMatchScore}
              onChange={(e) => setMinMatchScore(Number(e.target.value))}
              className="w-full accent-slate-900 h-1 bg-slate-100 rounded-lg cursor-pointer"
            />
          </div>

          {/* Document Readiness Range Slider */}
          <div className="space-y-2">
            <div className="flex justify-between items-center">
              <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{t("rec_min_doc") || "Min Doc Readiness"}</label>
              <span className="text-xs font-bold text-emerald-600">{minDocReadiness}%+</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              step="5"
              value={minDocReadiness}
              onChange={(e) => setMinDocReadiness(Number(e.target.value))}
              className="w-full accent-slate-900 h-1 bg-slate-100 rounded-lg cursor-pointer"
            />
          </div>

          {/* Sort Controller */}
          <div className="space-y-1.5 border-t border-slate-100 pt-4">
            <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">{t("rec_sort_by") || "Sort Results By"}</label>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="w-full text-xs border border-slate-200 rounded-xl px-3 py-2 bg-slate-50 text-slate-700 hover:bg-slate-100/50 focus:outline-none transition cursor-pointer"
            >
              <option value="match_score">{t("rec_sort_match") || "Match Score (High to Low)"}</option>
              <option value="doc_readiness">{t("rec_sort_readiness") || "Document Readiness (High to Low)"}</option>
              <option value="alpha">{t("rec_sort_alpha") || "Alphabetical Order"}</option>
            </select>
          </div>
        </aside>

        {/* FEED SECTION (Right Columns) */}
        <div className="col-span-1 lg:col-span-3 space-y-5">

          {/* Search Bar & Mobile Filter Trigger */}
          <div className="flex gap-3">
            <div className="relative flex-1">
              <span className="absolute inset-y-0 left-3.5 flex items-center pointer-events-none">
                <Search className="h-4.5 w-4.5 text-slate-400" />
              </span>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={t("rec_search_placeholder") || "Search matching schemes, departments, or ministries..."}
                className="w-full text-xs pl-10 pr-4 py-3 bg-white border border-slate-200 rounded-xl shadow-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-slate-900"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery("")}
                  className="absolute inset-y-0 right-3.5 flex items-center text-slate-400 hover:text-slate-600"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>

            <button
              onClick={() => setIsMobileFilterOpen(true)}
              className="lg:hidden flex items-center justify-center gap-1.5 bg-white border border-slate-200 text-slate-800 font-bold px-4 py-3 rounded-xl shadow-xs text-xs hover:bg-slate-50 transition shrink-0"
            >
              <SlidersHorizontal className="h-4 w-4" />
              {t("rec_filters_btn") || "Filters"}
            </button>
          </div>

          {/* Quick Stats Filter Tags Bar */}
          <div className="flex bg-slate-200/50 p-1 rounded-2xl overflow-x-auto scrollbar-none shadow-inner border border-slate-200/20">
            <button
              onClick={() => setActiveFilter("all")}
              className={`flex-1 min-w-[90px] text-center px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition ${
                activeFilter === "all" ? "bg-white text-slate-900 shadow-xs" : "text-slate-500 hover:text-slate-900"
              }`}
            >
              {t("rec_all_matches") || "All Matches"} ({counts.all})
            </button>
            <button
              onClick={() => setActiveFilter("eligible")}
              className={`flex-1 min-w-[90px] text-center px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition ${
                activeFilter === "eligible" ? "bg-white text-slate-900 shadow-xs" : "text-slate-500 hover:text-slate-900"
              }`}
            >
              {t("rec_eligible") || "Eligible"} ({counts.eligible})
            </button>
            <button
              onClick={() => setActiveFilter("possibly_eligible")}
              className={`flex-1 min-w-[90px] text-center px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition ${
                activeFilter === "possibly_eligible" ? "bg-white text-slate-900 shadow-xs" : "text-slate-500 hover:text-slate-900"
              }`}
            >
              {t("rec_possibly") || "Possibly"} ({counts.possibly_eligible})
            </button>
            <button
              onClick={() => setActiveFilter("not_eligible")}
              className={`flex-1 min-w-[90px] text-center px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition ${
                activeFilter === "not_eligible" ? "bg-white text-slate-900 shadow-xs" : "text-slate-500 hover:text-slate-900"
              }`}
            >
              {t("rec_not_eligible") || "Ineligible"} ({counts.not_eligible})
            </button>
          </div>

          <div className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider px-1">
            {t("rec_found_matches", { count: sortedSchemes.length }) || `Found ${sortedSchemes.length} matching programs`}
          </div>

          {/* Cards Stack */}
          <div className="space-y-5">
            {sortedSchemes.length > 0 ? (
              sortedSchemes.map(({ scheme, evaluation, readiness, recDetails }) => {
                const meta = STATUS_META[evaluation.status] || STATUS_META.possibly_eligible;
                const StatusIcon = meta.icon;
                const isSavedCheck = isSaved(scheme.id);
                const isAppliedCheck = hasApplied(scheme.id);

                return (
                  <div
                    key={scheme.id}
                    className={`bg-white border border-slate-205 border-slate-200 border-l-4 ${meta.border} rounded-2xl shadow-xs hover:shadow-md transition overflow-hidden flex flex-col md:flex-row`}
                  >
                    {/* Left main data container */}
                    <div className="p-5 flex-1 space-y-4">

                      {/* Department and Tags header */}
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-[9px] bg-slate-100 border border-slate-200 text-slate-500 px-2 py-0.5 rounded font-bold uppercase tracking-wider">
                          {scheme.sourceType}
                        </span>
                        {scheme.department && (
                          <span className="text-[9px] text-slate-450 text-slate-450 font-bold uppercase tracking-wider flex items-center gap-1">
                            <Building2 className="h-3 w-3" />
                            {scheme.department}
                          </span>
                        )}

                        {/* Opportunity Score Indicator */}
                        <span className="text-[9px] bg-indigo-50 border border-indigo-150 text-indigo-700 px-2 py-0.5 rounded font-bold uppercase tracking-wider">
                          Opportunity: {recDetails.opportunityScore}/100
                        </span>

                        {/* AI Recommendation Badge */}
                        <span className="text-[9px] bg-indigo-650 bg-indigo-600 text-white px-2 py-0.5 rounded-full font-black tracking-wide flex items-center gap-1 select-none shadow-xs">
                          <Sparkles className="h-2.5 w-2.5" />
                          {recDetails.aiBadgeText}
                        </span>
                      </div>

                      {/* Scheme Name */}
                      <div className="space-y-1">
                        <h2 className="text-base font-black text-slate-905 text-slate-900 leading-snug tracking-tight">
                          {scheme.name}
                        </h2>
                        {scheme.ministry && (
                          <p className="text-[10px] text-slate-400 font-semibold">{scheme.ministry}</p>
                        )}
                      </div>

                      {/* Benefit Summary */}
                      <div className="space-y-1 border-t border-slate-50 pt-3">
                        <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest block">
                          {t("rec_benefit_label") || "Core Scheme Benefit"}
                        </span>
                        <p className="text-xs text-slate-700 leading-relaxed font-medium">
                          {recDetails.benefitSummary}
                        </p>
                      </div>

                      {/* Dynamic status badges row */}
                      <div className="flex flex-wrap items-center gap-2 border-t border-slate-50 pt-3">
                        <div className={`flex items-center gap-1.5 px-3 py-1 rounded-full border text-[10px] font-bold ${meta.badge}`}>
                          <StatusIcon className="h-3.5 w-3.5 shrink-0" />
                          <span>{t(evaluation.status === "eligible" ? "rec_eligible" : evaluation.status === "possibly_eligible" ? "rec_possibly" : "rec_not")}</span>
                        </div>

                        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-50 border border-slate-200 text-slate-700 text-[10px] font-bold">
                          <FileText className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                          <span>
                            {readiness.totalAvailable}/{readiness.totalRequired} Docs verified · {readiness.readinessScore}% Ready
                          </span>
                        </div>

                        <div className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-rose-50 border border-rose-200 text-rose-700 text-[10px] font-bold">
                          <Clock className="h-3.5 w-3.5 text-rose-450 shrink-0" />
                          <span>{t("rec_closing_soon", { days: recDetails.deadlineDays }) || `Closing in ${recDetails.deadlineDays} Days`}</span>
                        </div>
                      </div>

                      {/* Core Differentiator: Match details list */}
                      <div className="grid sm:grid-cols-2 gap-4 bg-slate-50 p-4 border border-slate-100 rounded-xl">
                        {/* Why You Match */}
                        <div className="space-y-2">
                          <h4 className="text-[9px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1">
                            <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                            {t("rec_why_match") || "Why You Match"}
                          </h4>
                          <ul className="space-y-1">
                            {recDetails.whyYouMatch.map((reason, idx) => (
                              <li key={idx} className="flex items-start gap-1 text-xs text-slate-700 font-medium">
                                <span className="text-emerald-500 font-bold mr-1">✓</span>
                                <span className="leading-tight">{reason}</span>
                              </li>
                            ))}
                          </ul>
                        </div>

                        {/* Missing Documents */}
                        <div className="space-y-2">
                          <h4 className="text-[9px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1">
                            <FileText className="h-3.5 w-3.5 text-slate-400" />
                            {t("rec_doc_checklist") || "Required Document Checks"}
                          </h4>
                          {readiness.missingDocs.length > 0 ? (
                            <ul className="space-y-1">
                              {readiness.missingDocs.map((doc, idx) => (
                                <li key={idx} className="flex items-start gap-1 text-xs text-rose-700 font-semibold">
                                  <span className="text-rose-400 font-bold mr-1">✗</span>
                                  <span className="leading-tight">{doc}</span>
                                </li>
                              ))}
                            </ul>
                          ) : (
                            <div className="flex items-center gap-1 text-xs text-emerald-700 font-bold">
                              <CheckCircle className="h-3.5 w-3.5 text-emerald-500 mr-1" />
                              {t("rec_all_docs_verified") || "All documents matched and verified"}
                            </div>
                          )}
                        </div>
                      </div>

                      {/* AI Recommendation */}
                      <div className="bg-slate-900 rounded-xl p-3 flex items-start gap-2">
                        <svg className="h-3.5 w-3.5 text-indigo-400 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.346.9A2 2 0 0116 19H8a2 2 0 01-1.89-1.357l-.346-.9z" /></svg>
                        <p className="text-[10px] text-slate-300 leading-relaxed">
                          {readiness.readinessScore === 100
                            ? `✅ Your vault is ready. Apply now to secure benefits before the deadline.`
                            : readiness.missingDocs.length > 0
                            ? `⚡ Upload ${readiness.missingDocs[0]} to improve your readiness for this scheme. Check your Document Vault for quick upload options.`
                            : `📋 Complete your document vault to apply for this scheme.`}
                        </p>
                      </div>

                    </div>

                    {/* Right side opportunity panel */}
                    <div className="bg-slate-50/50 border-t md:border-t-0 md:border-l border-slate-200 p-5 w-full md:w-56 shrink-0 flex flex-col justify-between gap-4">
                      <div className="space-y-3.5">

                        {/* Opportunity Score Indicator */}
                        <div>
                          <div className="flex justify-between items-center mb-1">
                            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wide">
                              {t("rec_opportunity_priority") || "Opportunity Priority"}
                            </span>
                            <span className="text-xs font-black text-indigo-700">{recDetails.opportunityScore}%</span>
                          </div>
                          <div className="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-indigo-600 rounded-full"
                              style={{ width: `${recDetails.opportunityScore}%` }}
                            />
                          </div>
                          <p className="text-[9px] text-slate-400 mt-1 leading-normal italic">
                            {recDetails.opportunityScoreExplanation}
                          </p>
                        </div>

                        {/* Document Readiness Score */}
                        <div>
                          <div className="flex justify-between items-center mb-1">
                            <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wide">
                              {t("rec_min_doc") || "Doc Readiness"}
                            </span>
                            <span className="text-xs font-black text-slate-800">{readiness.readinessScore}%</span>
                          </div>
                          <div className="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                            <div
                              className={`h-full rounded-full ${
                                readiness.readinessLabel === "Ready" ? "bg-emerald-500" : readiness.readinessLabel === "Partially Ready" ? "bg-amber-400" : "bg-rose-500"
                              }`}
                              style={{ width: `${readiness.readinessScore}%` }}
                            />
                          </div>
                        </div>

                      </div>

                      {/* Main page triggers */}
                      <div className="space-y-2 pt-2 border-t border-slate-100">

                        {/* View Details Page */}
                        <Link
                          to={`/scheme/${scheme.id}`}
                          className="w-full inline-flex items-center justify-center gap-1.5 bg-slate-900 hover:bg-slate-800 text-white py-2 rounded-xl text-xs font-bold shadow-xs transition"
                        >
                          <span>{t("rec_view_details") || "View Details"}</span>
                          <ArrowRight className="h-3.5 w-3.5" />
                        </Link>

                        {/* Save to tracker */}
                        <button
                          onClick={() => saveScheme(scheme)}
                          disabled={isSavedCheck || isAppliedCheck}
                          className={`w-full inline-flex items-center justify-center gap-1.5 border py-2 rounded-xl text-xs font-bold transition ${
                            isSavedCheck || isAppliedCheck
                              ? "bg-slate-100 border-slate-200 text-slate-400 cursor-not-allowed"
                              : "bg-white border-slate-300 hover:bg-slate-50 text-slate-700"
                          }`}
                        >
                          {isAppliedCheck ? (
                            <>
                              <ClipboardCheck className="h-3.5 w-3.5 text-slate-400" />
                              <span>{t("rec_applied") || "Applied"}</span>
                            </>
                          ) : isSavedCheck ? (
                            <>
                              <BookmarkCheck className="h-3.5 w-3.5 text-emerald-600" />
                              <span>{t("rec_saved") || "Saved"}</span>
                            </>
                          ) : (
                            <>
                              <BookmarkPlus className="h-3.5 w-3.5 text-slate-500" />
                              <span>{t("rec_save_tracker") || "Save to Tracker"}</span>
                            </>
                          )}
                        </button>

                        {/* NEW: Ask AI Button */}
                        <button
                          onClick={() => handleAskAI(`I want to ask about ${scheme.name}. Can you explain my eligibility parameters and if I have any missing documents for it?`)}
                          className="w-full inline-flex items-center justify-center gap-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 border border-indigo-200/50 py-2 rounded-xl text-xs font-bold transition"
                        >
                          <MessageSquare className="h-3.5 w-3.5 shrink-0" />
                          <span>{t("rec_ask_ai") || "Ask SchemeAI"}</span>
                        </button>

                      </div>
                    </div>

                  </div>
                );
              })
            ) : (
              <EmptyState
                icon={HelpCircle}
                title={t("rec_empty_title") || "No schemes match your filters"}
                description={t("rec_empty_desc") || "Try adjusting your search query or clearing active filters to see more matching schemes."}
                action={{ label: t("rec_clear_filters") || "Clear All Filters", onClick: handleClearFilters, variant: "primary" }}
              />
            )}
          </div>

        </div>
      </div>

      {/* MOBILE FILTER SHEET PANEL */}
      {isMobileFilterOpen && (
        <div className="fixed inset-0 z-50 flex lg:hidden">
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs" onClick={() => setIsMobileFilterOpen(false)} />

          <div className="relative flex flex-col w-full max-w-sm bg-white ml-auto h-full shadow-2xl transition duration-300">
            <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
                <SlidersHorizontal className="h-4 w-4 text-slate-800" />
                {t("rec_filter_title") || "Filter Directory"}
              </h2>
              <button onClick={() => setIsMobileFilterOpen(false)} className="text-slate-400 hover:text-slate-900 p-1 rounded-md">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-5 space-y-6">
              {/* Residence State Read-Only Info */}
              <div className="space-y-1.5 p-3.5 bg-slate-50 border border-slate-200 rounded-2xl">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">{t("rec_residence_state") || "Residence State"}</span>
                <div className="flex items-center gap-1.5 font-bold text-xs text-slate-800 mt-1">
                  <span>📍</span>
                  <span>{profile?.state || user?.state || "Tamil Nadu"}</span>
                </div>
                <div className="flex justify-between items-center text-[10px] text-slate-400 mt-2 border-t border-slate-200/60 pt-1.5">
                  <span>{t("rec_using_profile") || "Using your profile"}</span>
                  <Link to="/profile" className="text-indigo-600 hover:text-indigo-800 font-extrabold flex items-center gap-0.5 transition">
                    {t("rec_edit_profile") || "Edit Profile"} <ArrowRight className="h-3 w-3" />
                  </Link>
                </div>
              </div>

              {/* Core Category dropdown */}
              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-700 block">{t("rec_core_category") || "Core Category"}</label>
                <select
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                  className="w-full text-xs border border-slate-200 rounded-xl px-3 py-2.5 bg-slate-50 text-slate-800 focus:outline-none"
                >
                  <option value="all">{t("rec_all_categories") || "All Categories"}</option>
                  {CORE_CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>
                      {cat}
                    </option>
                  ))}
                </select>
              </div>

              {/* Government Scheme Type */}
              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-700 block">{t("rec_scheme_type") || "Government Scheme Type"}</label>
                <div className="space-y-2 text-xs text-slate-700">
                  <label className="flex items-center gap-2 cursor-pointer font-medium">
                    <input
                      type="radio"
                      name="mobSchemeType"
                      value="all"
                      checked={schemeType === "all"}
                      onChange={() => setSchemeType("all")}
                      className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                    />
                    <span>{t("rec_type_both") || "Both (Default)"}</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer font-medium">
                    <input
                      type="radio"
                      name="mobSchemeType"
                      value="Central"
                      checked={schemeType === "Central"}
                      onChange={() => setSchemeType("Central")}
                      className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                    />
                    <span>{t("rec_type_central") || "Central Government"}</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer font-medium">
                    <input
                      type="radio"
                      name="mobSchemeType"
                      value="State"
                      checked={schemeType === "State"}
                      onChange={() => setSchemeType("State")}
                      className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                    />
                    <span>{t("rec_type_state") || "State Government"}</span>
                  </label>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-700 block">{t("rec_closing_date") || "Deadline Closing"}</label>
                <div className="space-y-2 text-xs text-slate-700">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="mobDeadlineRange"
                      value="all"
                      checked={selectedDeadlineRange === "all"}
                      onChange={() => setSelectedDeadlineRange("all")}
                      className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                    />
                    <span>{t("rec_all_deadlines") || "All Deadlines"}</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="mobDeadlineRange"
                      value="7"
                      checked={selectedDeadlineRange === "7"}
                      onChange={() => setSelectedDeadlineRange("7")}
                      className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                    />
                    <span>{t("rec_closing_soon") || "Closing Soon (< 7 Days)"}</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="mobDeadlineRange"
                      value="30"
                      checked={selectedDeadlineRange === "30"}
                      onChange={() => setSelectedDeadlineRange("30")}
                      className="rounded text-slate-900 focus:ring-slate-900 border-slate-300"
                    />
                    <span>{t("rec_closing_month") || "Closing This Month (< 30 Days)"}</span>
                  </label>
                </div>
              </div>

              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <label className="text-xs font-bold text-slate-700">{t("rec_min_match") || "Min Match Score"}</label>
                  <span className="text-xs font-bold text-indigo-700">{minMatchScore}%+</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  step="5"
                  value={minMatchScore}
                  onChange={(e) => setMinMatchScore(Number(e.target.value))}
                  className="w-full accent-slate-900 h-1 bg-slate-100 rounded-lg cursor-pointer"
                />
              </div>

              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <label className="text-xs font-bold text-slate-700">{t("rec_min_doc") || "Min Doc Readiness"}</label>
                  <span className="text-xs font-bold text-emerald-700">{minDocReadiness}%+</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  step="5"
                  value={minDocReadiness}
                  onChange={(e) => setMinDocReadiness(Number(e.target.value))}
                  className="w-full accent-slate-900 h-1 bg-slate-100 rounded-lg cursor-pointer"
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-700 block">{t("rec_sort_by") || "Sort By"}</label>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="w-full text-xs border border-slate-200 rounded-xl px-3 py-2.5 bg-slate-50 text-slate-800"
                >
                  <option value="match_score">{t("rec_sort_match") || "Match Score"}</option>
                  <option value="doc_readiness">{t("rec_sort_readiness") || "Doc Readiness"}</option>
                  <option value="alpha">{t("rec_sort_alpha") || "Alphabetical"}</option>
                </select>
              </div>
            </div>

            <div className="px-5 py-4 border-t border-slate-150 flex gap-3 bg-slate-50 shrink-0">
              <button
                onClick={handleClearFilters}
                className="flex-1 bg-white border border-slate-200 text-slate-700 text-xs font-bold py-2.5 rounded-xl"
              >
                {t("rec_reset_btn") || "Clear All"}
              </button>
              <button
                onClick={() => setIsMobileFilterOpen(false)}
                className="flex-1 bg-slate-900 text-white text-xs font-bold py-2.5 rounded-xl"
              >
                {t("rec_filters_btn") || "Apply Filters"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Floating Assistant Widget */}
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
