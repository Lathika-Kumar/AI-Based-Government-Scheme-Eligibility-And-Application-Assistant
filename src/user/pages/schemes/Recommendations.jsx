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
    badge: "bg-india-green/10 text-india-green border border-india-green/20",
    border: "border-l-india-green",
    icon: CheckCircle,
    iconColor: "text-india-green",
  },
  possibly_eligible: {
    badge: "bg-saffron/10 text-saffron-dark border border-saffron/20",
    border: "border-l-saffron",
    icon: AlertCircle,
    iconColor: "text-saffron-dark",
  },
  not_eligible: {
    badge: "bg-red-50 text-red-700 border border-red-200",
    border: "border-l-red-500",
    icon: XCircle,
    iconColor: "text-red-500",
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
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 6;

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

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [filteredSchemes]);

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

  // Paginated items
  const paginatedSchemes = useMemo(() => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    return sortedSchemes.slice(startIndex, startIndex + itemsPerPage);
  }, [sortedSchemes, currentPage, itemsPerPage]);

  // Pagination helpers
  const totalPages = Math.ceil(sortedSchemes.length / itemsPerPage);

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
      {/* ── Page Header ── */}
      <div className="bg-gradient-to-r from-government-blue via-government-blue-light to-government-blue text-white p-6 rounded-xl shadow-lg">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <div className="bg-white/10 p-2 rounded-lg">
                <Sparkles className="h-5 w-5 text-saffron" />
              </div>
              <span className="text-xs font-semibold text-white/80 uppercase tracking-wider">
                AI-Powered Scheme Discovery
              </span>
            </div>
            <h1 className="text-2xl font-bold tracking-tight">
              Government Scheme Recommendations
            </h1>
            <p className="text-sm text-white/90 leading-relaxed max-w-2xl">
              Discover Central and State Government welfare schemes tailored to your verified profile,
              eligibility, income, occupation, and location.
            </p>
          </div>
        </div>
      </div>

      {/* Primary Layout Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">

        {/* ── STICKY FILTER SIDEBAR (Desktop) ── */}
        <aside className="hidden lg:block lg:col-span-1 bg-white border border-gray-200 p-5 rounded-xl shadow-sm space-y-5 lg:sticky lg:top-24 lg:self-start lg:max-h-[calc(100vh-120px)] lg:overflow-y-auto">
          <div className="flex items-center justify-between border-b border-gray-100 pb-3">
            <h2 className="text-sm font-bold text-gray-900 flex items-center gap-1.5">
              <SlidersHorizontal className="h-4 w-4 text-government-blue" />
              Filter Schemes
            </h2>
            <button
              onClick={handleClearFilters}
              aria-label="Reset all filters"
              className="text-xs text-government-blue font-semibold hover:underline"
            >
              Reset Filters
            </button>
          </div>

          {/* Residence State — Read-Only from Profile */}
          <div className="space-y-1.5 p-3.5 bg-gray-50 border border-gray-200 rounded-xl">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Residence State</span>
            <div className="flex items-center gap-1.5 font-semibold text-sm text-gray-800 mt-1">
              <span>📍</span>
              <span>{profile?.state || user?.state || "Tamil Nadu"}</span>
            </div>
            <div className="flex justify-between items-center text-[11px] text-gray-500 mt-2 border-t border-gray-200/60 pt-1.5">
              <span>Sourced from your profile</span>
              <Link to="/profile" className="text-government-blue hover:text-government-blue-dark font-semibold flex items-center gap-0.5 transition" aria-label="Edit your profile to update state">
                Edit Profile <ArrowRight className="h-3 w-3" />
              </Link>
            </div>
          </div>

          {/* Scheme Category Filter */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Scheme Category</label>
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              aria-label="Filter by scheme category"
              className="w-full text-sm border border-gray-300 rounded-xl px-3 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-government-blue transition cursor-pointer"
            >
              <option value="all">All Categories</option>
              {CORE_CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>

          {/* Scheme Type (Central / State) */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Scheme Type</label>
            <div className="space-y-1.5 text-sm text-gray-700">
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="schemeType" value="all" checked={schemeType === "all"}
                  onChange={() => setSchemeType("all")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                <span>All Schemes</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="schemeType" value="Central" checked={schemeType === "Central"}
                  onChange={() => setSchemeType("Central")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                <span>Central Government</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="schemeType" value="State" checked={schemeType === "State"}
                  onChange={() => setSchemeType("State")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                <span>State Government</span>
              </label>
            </div>
          </div>

          {/* Application Deadline Filter */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Application Deadline</label>
            <div className="space-y-1.5 text-sm text-gray-700">
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="deadlineRange" value="all" checked={selectedDeadlineRange === "all"}
                  onChange={() => setSelectedDeadlineRange("all")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                <span>All Deadlines</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="deadlineRange" value="7" checked={selectedDeadlineRange === "7"}
                  onChange={() => setSelectedDeadlineRange("7")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                <span>Application Closing Soon (&lt; 7 Days)</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="deadlineRange" value="30" checked={selectedDeadlineRange === "30"}
                  onChange={() => setSelectedDeadlineRange("30")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                <span>Closing This Month (&lt; 30 Days)</span>
              </label>
            </div>
          </div>

          {/* Minimum Eligibility Score Slider */}
          <div className="space-y-2 border-t border-gray-100 pt-4">
            <div className="flex justify-between items-center">
              <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Minimum Eligibility Score</label>
              <span className="text-sm font-bold text-government-blue">{minMatchScore}%+</span>
            </div>
            <input
              type="range" min="0" max="100" step="5"
              value={minMatchScore}
              onChange={(e) => setMinMatchScore(Number(e.target.value))}
              aria-label="Minimum eligibility score filter"
              className="w-full accent-government-blue h-2 bg-gray-200 rounded-lg cursor-pointer"
            />
          </div>

          {/* Minimum Document Readiness Slider */}
          <div className="space-y-2">
            <div className="flex justify-between items-center">
              <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Document Readiness</label>
              <span className="text-sm font-bold text-india-green">{minDocReadiness}%+</span>
            </div>
            <input
              type="range" min="0" max="100" step="5"
              value={minDocReadiness}
              onChange={(e) => setMinDocReadiness(Number(e.target.value))}
              aria-label="Minimum document readiness filter"
              className="w-full accent-india-green h-2 bg-gray-200 rounded-lg cursor-pointer"
            />
          </div>

          {/* Sort Schemes By */}
          <div className="space-y-1.5 border-t border-gray-100 pt-4">
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Sort Schemes By</label>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              aria-label="Sort schemes"
              className="w-full text-sm border border-gray-300 rounded-xl px-3 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-government-blue transition cursor-pointer"
            >
              <option value="match_score">Eligibility Score (High to Low)</option>
              <option value="doc_readiness">Document Readiness (High to Low)</option>
              <option value="alpha">Scheme Name (A to Z)</option>
            </select>
          </div>
        </aside>

        {/* FEED SECTION (Right Columns) */}
        <div className="col-span-1 lg:col-span-3 space-y-5">

          {/* Search Bar & Mobile Filter Trigger */}
          <div className="flex gap-3">
            <div className="relative flex-1">
              <span className="absolute inset-y-0 left-3.5 flex items-center pointer-events-none">
                <Search className="h-4 w-4 text-gray-400" />
              </span>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search by Scheme Name, Ministry, Department, or Category"
                aria-label="Search government schemes"
                className="w-full text-sm pl-10 pr-10 py-3 bg-white border border-gray-300 rounded-xl shadow-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-government-blue transition"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery("")}
                  aria-label="Clear search"
                  className="absolute inset-y-0 right-3.5 flex items-center text-gray-400 hover:text-gray-600"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
            <button
              onClick={() => setIsMobileFilterOpen(true)}
              aria-label="Open filter panel"
              className="lg:hidden flex items-center justify-center gap-1.5 bg-white border border-gray-300 text-gray-800 font-semibold px-4 py-3 rounded-xl shadow-sm text-sm hover:bg-gray-50 transition shrink-0"
            >
              <SlidersHorizontal className="h-4 w-4" />
              Filters
            </button>
          </div>

          {/* Tab Bar */}
          <div className="flex bg-gray-100 p-1 rounded-xl overflow-x-auto scrollbar-none border border-gray-200">
            {[
              { key: "all", label: "All Schemes", count: counts.all },
              { key: "eligible", label: "Recommended", count: counts.eligible },
              { key: "possibly_eligible", label: "Eligible", count: counts.possibly_eligible },
              { key: "not_eligible", label: "Not Eligible", count: counts.not_eligible },
            ].map(({ key, label, count }) => (
              <button
                key={key}
                onClick={() => setActiveFilter(key)}
                aria-pressed={activeFilter === key}
                className={`flex-1 min-w-[90px] text-center px-4 py-2 rounded-lg text-sm font-semibold whitespace-nowrap transition ${
                  activeFilter === key ? "bg-white text-gray-900 shadow-sm" : "text-gray-600 hover:text-gray-900"
                }`}
              >
                {label} <span className="font-bold">({count})</span>
              </button>
            ))}
          </div>

          <div className="text-xs text-gray-500 font-semibold tracking-wide px-1">
            Showing {sortedSchemes.length} matching government {sortedSchemes.length === 1 ? "scheme" : "schemes"}
          </div>

          {/* Cards Stack */}
          <div className="space-y-5">
            {sortedSchemes.length > 0 ? (
              paginatedSchemes.map(({ scheme, evaluation, readiness, recDetails }) => {
                const meta = STATUS_META[evaluation.status] || STATUS_META.possibly_eligible;
                const StatusIcon = meta.icon;
                const isSavedCheck = isSaved(scheme.id);
                const isAppliedCheck = hasApplied(scheme.id);

                return (
                  <div
                    key={scheme.id}
                    className={`bg-white border border-gray-200 border-l-4 ${meta.border} rounded-xl shadow-sm hover:shadow-md transition-shadow overflow-hidden flex flex-col md:flex-row`}
                  >
                    {/* ── Left main content ── */}
                    <div className="p-5 flex-1 space-y-4">

                      {/* Source, Department & AI Score Row */}
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-xs bg-gray-100 border border-gray-200 text-gray-700 px-2 py-1 rounded font-semibold uppercase tracking-wider">
                          {scheme.sourceType === "Central" ? "Central Government" : "State Government"}
                        </span>
                        {scheme.department && (
                          <span className="text-xs text-gray-600 font-semibold uppercase tracking-wider flex items-center gap-1">
                            <Building2 className="h-3.5 w-3.5" />
                            {scheme.department}
                          </span>
                        )}

                        {/* AI Eligibility Score Badge */}
                        <span className="text-xs bg-government-blue/10 border border-government-blue/20 text-government-blue px-2.5 py-1 rounded-full font-semibold flex items-center gap-1">
                          Eligibility Score: {recDetails.opportunityScore}%
                        </span>

                        {/* AI Recommendation Badge */}
                        <span className="text-xs bg-saffron text-government-blue-dark px-2.5 py-1 rounded-full font-bold tracking-wide flex items-center gap-1 select-none shadow-sm">
                          <Sparkles className="h-3 w-3" />
                          {recDetails.aiBadgeText === "High Fit"
                            ? "Highly Recommended"
                            : recDetails.aiBadgeText === "Possibly Eligible"
                            ? "Eligibility Under Review"
                            : recDetails.aiBadgeText}
                        </span>
                      </div>

                      {/* Scheme Name & Ministry */}
                      <div className="space-y-0.5">
                        <h2 className="text-lg font-bold text-gray-900 leading-snug tracking-tight">
                          {scheme.name}
                        </h2>
                        {scheme.ministry && (
                          <p className="text-xs text-gray-500 font-semibold">{scheme.ministry}</p>
                        )}
                      </div>

                      {/* Scheme Benefits */}
                      <div className="space-y-1 border-t border-gray-100 pt-3">
                        <span className="text-xs font-bold text-gray-500 uppercase tracking-widest block">
                          Scheme Benefits
                        </span>
                        <p className="text-sm text-gray-700 leading-relaxed">
                          {recDetails.benefitSummary}
                        </p>
                      </div>

                      {/* Status Badges Row */}
                      <div className="flex flex-wrap items-center gap-2 border-t border-gray-100 pt-3">
                        <div className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full border text-xs font-semibold ${meta.badge}`}>
                          <StatusIcon className="h-3.5 w-3.5 shrink-0" />
                          <span>
                            {evaluation.status === "eligible" ? "Recommended"
                              : evaluation.status === "possibly_eligible" ? "Eligibility Under Review"
                              : "Not Eligible"}
                          </span>
                        </div>

                        <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-gray-50 border border-gray-200 text-gray-700 text-xs font-semibold">
                          <FileText className="h-3.5 w-3.5 text-gray-500 shrink-0" />
                          <span>
                            Document Verification: {readiness.totalAvailable} of {readiness.totalRequired} Verified &middot; {readiness.readinessScore}% Ready
                          </span>
                        </div>

                        <div className="flex items-center gap-1 px-2.5 py-1.5 rounded-full bg-red-50 border border-red-200 text-red-700 text-xs font-semibold">
                          <Clock className="h-3.5 w-3.5 shrink-0" />
                          <span>Application Closing in {recDetails.deadlineDays} Days</span>
                        </div>
                      </div>

                      {/* Eligibility Assessment & Required Documents Grid */}
                      <div className="grid sm:grid-cols-2 gap-4 bg-gray-50 p-4 border border-gray-100 rounded-xl">

                        {/* Eligibility Assessment */}
                        <div className="space-y-2">
                          <h4 className="text-xs font-bold text-gray-500 uppercase tracking-widest flex items-center gap-1">
                            <CheckCircle2 className="h-3.5 w-3.5 text-india-green" />
                            Eligibility Assessment
                          </h4>
                          <ul className="space-y-1.5">
                            {recDetails.whyYouMatch.map((reason, idx) => (
                              <li key={idx} className="flex items-start gap-1.5 text-sm text-gray-700 leading-snug">
                                <span className="text-india-green font-bold shrink-0 mt-0.5">✓</span>
                                <span>{reason}</span>
                              </li>
                            ))}
                          </ul>
                        </div>

                        {/* Required Documents */}
                        <div className="space-y-2">
                          <h4 className="text-xs font-bold text-gray-500 uppercase tracking-widest flex items-center gap-1">
                            <FileText className="h-3.5 w-3.5 text-gray-500" />
                            Required Documents
                          </h4>
                          {readiness.missingDocs.length > 0 ? (
                            <ul className="space-y-1.5">
                              {readiness.missingDocs.map((doc, idx) => (
                                <li key={idx} className="flex items-start gap-1.5 text-sm text-red-700 font-semibold leading-snug">
                                  <span className="text-red-400 font-bold shrink-0 mt-0.5">✗</span>
                                  <span>{doc}</span>
                                </li>
                              ))}
                            </ul>
                          ) : (
                            <div className="flex items-center gap-1.5 text-sm text-india-green font-semibold">
                              <CheckCircle className="h-3.5 w-3.5 text-india-green" />
                              All required documents are verified.
                            </div>
                          )}
                        </div>
                      </div>

                      {/* AI Advisory Panel */}
                      <div className="bg-government-blue rounded-xl p-3.5 flex items-start gap-2.5">
                        <svg className="h-4 w-4 text-saffron shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.346.9A2 2 0 0116 19H8a2 2 0 01-1.89-1.357l-.346-.9z" />
                        </svg>
                        <p className="text-xs text-white leading-relaxed">
                          {readiness.readinessScore === 100
                            ? `✅ Your document vault is complete. You may now apply and secure your benefits before the application deadline.`
                            : readiness.missingDocs.length > 0
                            ? `⚡ Complete your document verification to improve your eligibility score and accelerate the application review process. Missing: ${readiness.missingDocs[0]}.`
                            : `📋 Complete your document vault to apply for this scheme.`}
                        </p>
                      </div>

                    </div>

                    {/* ── Right: Scores & Actions Panel ── */}
                    <div className="bg-gray-50 border-t md:border-t-0 md:border-l border-gray-200 p-5 w-full md:w-56 shrink-0 flex flex-col justify-between gap-4">
                      <div className="space-y-4">

                        {/* Application Priority */}
                        <div>
                          <div className="flex justify-between items-center mb-1.5">
                            <span className="text-xs text-gray-500 font-semibold uppercase tracking-wide">Application Priority</span>
                            <span className="text-sm font-bold text-government-blue">{recDetails.opportunityScore}%</span>
                          </div>
                          <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-gradient-to-r from-government-blue to-government-blue-light rounded-full transition-all duration-500"
                              style={{ width: `${recDetails.opportunityScore}%` }}
                              role="progressbar"
                              aria-valuenow={recDetails.opportunityScore}
                              aria-valuemin={0}
                              aria-valuemax={100}
                            />
                          </div>
                          <p className="text-xs text-gray-500 mt-1.5 leading-normal italic">
                            {recDetails.opportunityScoreExplanation}
                          </p>
                        </div>

                        {/* Document Readiness */}
                        <div>
                          <div className="flex justify-between items-center mb-1.5">
                            <span className="text-xs text-gray-500 font-semibold uppercase tracking-wide">Document Readiness</span>
                            <span className="text-sm font-bold text-gray-900">{readiness.readinessScore}%</span>
                          </div>
                          <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                            <div
                              className={`h-full rounded-full transition-all duration-500 ${
                                readiness.readinessLabel === "Ready" ? "bg-india-green"
                                : readiness.readinessLabel === "Partially Ready" ? "bg-saffron"
                                : "bg-red-500"
                              }`}
                              style={{ width: `${readiness.readinessScore}%` }}
                              role="progressbar"
                              aria-valuenow={readiness.readinessScore}
                              aria-valuemin={0}
                              aria-valuemax={100}
                            />
                          </div>
                        </div>

                      </div>

                      {/* Action Buttons */}
                      <div className="space-y-2 pt-2 border-t border-gray-200">

                        {/* View Scheme Details */}
                        <Link
                          to={`/scheme/${scheme.id}`}
                          aria-label={`View details for ${scheme.name}`}
                          className="w-full inline-flex items-center justify-center gap-1.5 bg-government-blue hover:bg-government-blue-dark text-white py-2.5 rounded-lg text-sm font-semibold shadow-sm transition"
                        >
                          <span>View Scheme Details</span>
                          <ArrowRight className="h-4 w-4" />
                        </Link>

                        {/* Save to tracker */}
                        <button
                          onClick={() => saveScheme(scheme)}
                          disabled={isSavedCheck || isAppliedCheck}
                          aria-label={isAppliedCheck ? "Application submitted" : isSavedCheck ? "Scheme saved" : `Save ${scheme.name} to tracker`}
                          className={`w-full inline-flex items-center justify-center gap-1.5 border py-2.5 rounded-lg text-sm font-semibold transition ${
                            isSavedCheck || isAppliedCheck
                              ? "bg-gray-100 border-gray-200 text-gray-400 cursor-not-allowed"
                              : "bg-white border-gray-300 hover:bg-gray-50 text-gray-700"
                          }`}
                        >
                          {isAppliedCheck ? (
                            <><ClipboardCheck className="h-4 w-4 text-gray-400" /><span>Application Submitted</span></>
                          ) : isSavedCheck ? (
                            <><BookmarkCheck className="h-4 w-4 text-india-green" /><span>Saved to Tracker</span></>
                          ) : (
                            <><BookmarkPlus className="h-4 w-4 text-gray-500" /><span>Save to Tracker</span></>
                          )}
                        </button>

                        {/* AI Assistant Button */}
                        <button
                          onClick={() => handleAskAI(`I want to ask about ${scheme.name}. Can you explain my eligibility parameters and if I have any missing documents for it?`)}
                          aria-label={`Open AI Assistant for ${scheme.name}`}
                          className="w-full inline-flex items-center justify-center gap-1.5 bg-government-blue/10 hover:bg-government-blue/20 text-government-blue border border-government-blue/20 py-2.5 rounded-lg text-sm font-semibold transition"
                        >
                          <MessageSquare className="h-4 w-4 shrink-0" />
                          <span>AI Assistant</span>
                        </button>

                      </div>
                    </div>

                  </div>
                );
              })
            ) : (
              <EmptyState
                icon={HelpCircle}
                title="No schemes match your current filters"
                description="Try adjusting your search query, changing the scheme category, or clearing active filters to discover more matching government programmes."
                action={{ label: "Clear All Filters", onClick: handleClearFilters, variant: "primary" }}
              />
            )}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-gray-200 pt-4 mt-4">
              <div className="text-sm text-gray-600">
                Page <span className="font-bold text-gray-900">{currentPage}</span> of <span className="font-bold text-gray-900">{totalPages}</span>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                  disabled={currentPage === 1}
                  className="px-3 py-2 text-sm font-semibold rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  Previous
                </button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map(pageNum => (
                  <button
                    key={pageNum}
                    onClick={() => setCurrentPage(pageNum)}
                    className={`px-3 py-2 text-sm font-semibold rounded-lg transition ${
                      pageNum === currentPage
                        ? "bg-government-blue text-white shadow-sm"
                        : "text-gray-700 hover:bg-gray-50 border border-gray-300"
                    }`}
                  >
                    {pageNum}
                  </button>
                ))}
                <button
                  onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                  disabled={currentPage === totalPages}
                  className="px-3 py-2 text-sm font-semibold rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  Next
                </button>
              </div>
            </div>
          )}

        </div>
      </div>

      {/* ── MOBILE FILTER DRAWER ── */}
      {isMobileFilterOpen && (
        <div className="fixed inset-0 z-50 flex lg:hidden" role="dialog" aria-modal="true" aria-label="Filter panel">
          <div className="fixed inset-0 bg-gray-900/60 backdrop-blur-sm" onClick={() => setIsMobileFilterOpen(false)} />

          <div className="relative flex flex-col w-full max-w-sm bg-white ml-auto h-full shadow-2xl">
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <h2 className="text-sm font-bold text-gray-900 flex items-center gap-1.5">
                <SlidersHorizontal className="h-4 w-4 text-government-blue" />
                Filter Schemes
              </h2>
              <button onClick={() => setIsMobileFilterOpen(false)} aria-label="Close filter panel" className="text-gray-400 hover:text-gray-900 p-1 rounded-md">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-5 space-y-6">
              {/* Residence State — Read-Only */}
              <div className="space-y-1.5 p-3.5 bg-gray-50 border border-gray-200 rounded-xl">
                <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Residence State</span>
                <div className="flex items-center gap-1.5 font-semibold text-sm text-gray-800 mt-1">
                  <span>📍</span>
                  <span>{profile?.state || user?.state || "Tamil Nadu"}</span>
                </div>
                <div className="flex justify-between items-center text-[11px] text-gray-500 mt-2 border-t border-gray-200/60 pt-1.5">
                  <span>Sourced from your profile</span>
                  <Link to="/profile" className="text-government-blue hover:text-government-blue-dark font-semibold flex items-center gap-0.5 transition">
                    Edit Profile <ArrowRight className="h-3 w-3" />
                  </Link>
                </div>
              </div>

              {/* Scheme Category */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Scheme Category</label>
                <select
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                  aria-label="Filter by scheme category"
                  className="w-full text-sm border border-gray-300 rounded-xl px-3 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-government-blue transition cursor-pointer"
                >
                  <option value="all">All Categories</option>
                  {CORE_CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>

              {/* Scheme Type */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Scheme Type</label>
                <div className="space-y-1.5 text-sm text-gray-700">
                  <label className="flex items-center gap-2 cursor-pointer font-medium">
                    <input type="radio" name="mobSchemeType" value="all" checked={schemeType === "all"}
                      onChange={() => setSchemeType("all")}
                      className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                    <span>All Schemes</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer font-medium">
                    <input type="radio" name="mobSchemeType" value="Central" checked={schemeType === "Central"}
                      onChange={() => setSchemeType("Central")}
                      className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                    <span>Central Government</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer font-medium">
                    <input type="radio" name="mobSchemeType" value="State" checked={schemeType === "State"}
                      onChange={() => setSchemeType("State")}
                      className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                    <span>State Government</span>
                  </label>
                </div>
              </div>

              {/* Application Deadline */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Application Deadline</label>
                <div className="space-y-1.5 text-sm text-gray-700">
                  <label className="flex items-center gap-2 cursor-pointer font-medium">
                    <input type="radio" name="mobDeadlineRange" value="all" checked={selectedDeadlineRange === "all"}
                      onChange={() => setSelectedDeadlineRange("all")}
                      className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                    <span>All Deadlines</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer font-medium">
                    <input type="radio" name="mobDeadlineRange" value="7" checked={selectedDeadlineRange === "7"}
                      onChange={() => setSelectedDeadlineRange("7")}
                      className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                    <span>Application Closing Soon (&lt; 7 Days)</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer font-medium">
                    <input type="radio" name="mobDeadlineRange" value="30" checked={selectedDeadlineRange === "30"}
                      onChange={() => setSelectedDeadlineRange("30")}
                      className="rounded text-government-blue focus:ring-government-blue border-gray-300" />
                    <span>Closing This Month (&lt; 30 Days)</span>
                  </label>
                </div>
              </div>

              {/* Minimum Eligibility Score */}
              <div className="space-y-2 border-t border-gray-100 pt-4">
                <div className="flex justify-between items-center">
                  <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Minimum Eligibility Score</label>
                  <span className="text-sm font-bold text-government-blue">{minMatchScore}%+</span>
                </div>
                <input type="range" min="0" max="100" step="5" value={minMatchScore}
                  onChange={(e) => setMinMatchScore(Number(e.target.value))}
                  aria-label="Minimum eligibility score"
                  className="w-full accent-government-blue h-2 bg-gray-200 rounded-lg cursor-pointer"
                />
              </div>

              {/* Document Readiness */}
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Document Readiness</label>
                  <span className="text-sm font-bold text-india-green">{minDocReadiness}%+</span>
                </div>
                <input type="range" min="0" max="100" step="5" value={minDocReadiness}
                  onChange={(e) => setMinDocReadiness(Number(e.target.value))}
                  aria-label="Minimum document readiness"
                  className="w-full accent-india-green h-2 bg-gray-200 rounded-lg cursor-pointer"
                />
              </div>

              {/* Sort Schemes By */}
              <div className="space-y-1.5 border-t border-gray-100 pt-4">
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider block">Sort Schemes By</label>
                <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}
                  aria-label="Sort schemes by"
                  className="w-full text-sm border border-gray-300 rounded-xl px-3 py-2.5 bg-gray-50 text-gray-700 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-government-blue transition cursor-pointer"
                >
                  <option value="match_score">Eligibility Score (High to Low)</option>
                  <option value="doc_readiness">Document Readiness (High to Low)</option>
                  <option value="alpha">Scheme Name (A to Z)</option>
                </select>
              </div>
            </div>

            <div className="px-5 py-4 border-t border-gray-100 flex gap-3 bg-white shrink-0">
              <button
                onClick={handleClearFilters}
                className="flex-1 bg-white border border-gray-300 text-gray-700 text-sm font-semibold py-2.5 rounded-xl hover:bg-gray-50 transition"
              >
                Reset Filters
              </button>
              <button
                onClick={() => setIsMobileFilterOpen(false)}
                className="flex-1 bg-government-blue text-white text-sm font-semibold py-2.5 rounded-xl hover:bg-government-blue-dark transition"
              >
                Apply Filters
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
