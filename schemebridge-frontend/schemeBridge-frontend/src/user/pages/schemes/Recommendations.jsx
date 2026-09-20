import React, { useState, useMemo, useEffect, useRef, useCallback } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { getRecommendations, getPersonalizedRecommendations, trackRecommendationEvent } from "@services/schemeService";
import { getDocReadinessForScheme } from "@utils/documentReadiness";
import { renderFormattedText } from "@utils/textFormatter";
import { resolveApplicationAction } from "@utils/applicationStateMapping";
import { usePageMeta } from "@utils/usePageMeta";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import EmptyState from "@components/ui/EmptyState";
import Pagination from "@components/ui/Pagination";
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
  MessageSquare,
  ExternalLink,
  ChevronDown,
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
  "Student Scholarships",
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
  usePageMeta("Matching Schemes", "AI-recommended public welfare schemes based on your profile");
  const navigate = useNavigate();
  const { user: authUser } = useAuth();
  const { profile: appProfile, user: appUser, documents, saveScheme, isSaved, hasApplied, applications = [] } = useApp();
  const activeUser = authUser || appUser;
  const profile = appProfile || {};

  // Backend recommendations state
  const [backendItems, setBackendItems] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState(null);

  // Page-level UI states
  const [searchQuery, setSearchQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState("all"); // eligible, possibly_eligible, not_eligible, all
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [schemeType, setSchemeType] = useState("all"); // all, Central, State
  const [minMatchScore, setMinMatchScore] = useState(0);
  const [minDocReadiness, setMinDocReadiness] = useState(0);
  const [selectedDeadlineRange, setSelectedDeadlineRange] = useState("all"); // all, 7, 30
  const [sortBy, setSortBy] = useState("match_score");
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);

  // Mobile drawer state
  const [isMobileFilterOpen, setIsMobileFilterOpen] = useState(false);

  // Chat widget state
  const [aiChatOpen, setAiChatOpen] = useState(false);
  const [aiInitialQuery, setAiInitialQuery] = useState("");

  // Expandable "Why this scheme is recommended" state
  const [expandedReasons, setExpandedReasons] = useState({});
  const toggleReasons = (schemeId, defaultOpen) => {
    setExpandedReasons((prev) => ({
      ...prev,
      [schemeId]: !(prev[schemeId] !== undefined ? prev[schemeId] : defaultOpen),
    }));
  };

  // Fetch real backend recommendations for the current authenticated citizen
  const loadRecommendations = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    setBackendItems([]); // Clear stale items from previous user session
    try {
      // 1. Try authenticated current-user recommendations endpoint (/api/recommendations)
      let res = await getPersonalizedRecommendations({ page: 0, size: 50 });

      // 2. If needed, fallback to POST /api/schemes/recommendations with authentic profile attributes only (no fake defaults)
      if (res?.error || !res?.data?.recommendations) {
        const rawAge = profile?.age !== undefined && profile?.age !== "" ? Number(profile.age) : (activeUser?.age !== undefined && activeUser?.age !== "" ? Number(activeUser.age) : undefined);
        const rawGender = profile?.gender || activeUser?.gender || undefined;
        const rawIncome = profile?.annualIncome !== undefined && profile?.annualIncome !== "" ? Number(profile.annualIncome) : (activeUser?.annualIncome !== undefined && activeUser?.annualIncome !== "" ? Number(activeUser.annualIncome) : undefined);
        const rawOccupation = profile?.occupation || activeUser?.occupation || undefined;
        const rawState = profile?.state || activeUser?.state || undefined;
        const rawCategory = profile?.socialCategory || profile?.caste || activeUser?.socialCategory || activeUser?.caste || undefined;
        const rawDisability = profile?.disabilityStatus !== undefined ? Boolean(profile.disabilityStatus) : (activeUser?.disabilityStatus !== undefined ? Boolean(activeUser.disabilityStatus) : undefined);

        const eligibilityProfile = {};
        if (rawAge !== undefined && !isNaN(rawAge)) eligibilityProfile.age = rawAge;
        if (rawGender !== undefined) eligibilityProfile.gender = String(rawGender).toUpperCase();
        if (rawIncome !== undefined && !isNaN(rawIncome)) eligibilityProfile.annualIncome = rawIncome;
        if (rawOccupation !== undefined) eligibilityProfile.occupation = String(rawOccupation).toUpperCase();
        if (rawState !== undefined) eligibilityProfile.state = String(rawState).toUpperCase();
        if (rawCategory !== undefined) eligibilityProfile.socialCategory = String(rawCategory).toUpperCase();
        if (rawDisability !== undefined) eligibilityProfile.disabilityStatus = rawDisability;

        // Populate structured citizen attributes
        const occUpper = String(rawOccupation || "").toUpperCase();
        if (profile?.isFarmer !== undefined || activeUser?.isFarmer !== undefined || occUpper === "FARMER") {
          eligibilityProfile.isFarmer = Boolean(profile?.isFarmer ?? activeUser?.isFarmer ?? (occUpper === "FARMER"));
        }
        if (profile?.isStudent !== undefined || activeUser?.isStudent !== undefined || occUpper === "STUDENT") {
          eligibilityProfile.isStudent = Boolean(profile?.isStudent ?? activeUser?.isStudent ?? (occUpper === "STUDENT"));
        }
        if (profile?.bplStatus !== undefined || activeUser?.bplStatus !== undefined || profile?.bplCardHolder !== undefined || activeUser?.bplCardHolder !== undefined) {
          eligibilityProfile.bplStatus = Boolean(profile?.bplStatus ?? activeUser?.bplStatus ?? profile?.bplCardHolder ?? activeUser?.bplCardHolder);
        }
        if (profile?.district || activeUser?.district) {
          eligibilityProfile.district = profile?.district || activeUser?.district;
        }
        if (profile?.maritalStatus || activeUser?.maritalStatus) {
          eligibilityProfile.maritalStatus = profile?.maritalStatus || activeUser?.maritalStatus;
        }
        if (profile?.employmentStatus || activeUser?.employmentStatus) {
          eligibilityProfile.employmentStatus = profile?.employmentStatus || activeUser?.employmentStatus;
        }
        if (profile?.landholdingArea !== undefined || activeUser?.landholdingArea !== undefined) {
          eligibilityProfile.landholdingArea = Number(profile?.landholdingArea ?? activeUser?.landholdingArea);
        }

        const attrs = {};
        if (eligibilityProfile.isStudent !== undefined) attrs.isStudent = eligibilityProfile.isStudent;
        if (eligibilityProfile.isFarmer !== undefined) attrs.isFarmer = eligibilityProfile.isFarmer;
        if (profile?.ownsLand !== undefined || activeUser?.ownsLand !== undefined) {
          attrs.ownsLand = Boolean(profile?.ownsLand ?? activeUser?.ownsLand);
        }
        if (eligibilityProfile.bplStatus !== undefined) attrs.bplCardHolder = eligibilityProfile.bplStatus;
        if (Object.keys(attrs).length > 0) {
          eligibilityProfile.attributes = attrs;
        }

        res = await getRecommendations(eligibilityProfile, { page: 0, size: 50 });
      }

      if (!res?.error && res?.data?.recommendations) {
        setBackendItems(res.data.recommendations);
      } else if (res?.error) {
        setErrorMessage(res.message || "Unable to fetch recommendations. Please try again later.");
      }
    } catch (err) {
      console.error("Failed to load recommendations from Scheme Service:", err);
      setErrorMessage("Failed to load recommendations. Please verify network connectivity and try again.");
    } finally {
      setIsLoading(false);
    }
  }, [profile, activeUser]);

  useEffect(() => {
    loadRecommendations();
  }, [loadRecommendations]);

  // Telemetry: track impressions and interactions without duplicate re-render flooding
  const trackedImpressionsRef = useRef(new Set());

  useEffect(() => {
    if (!backendItems || backendItems.length === 0) return;
    const sessionId = (typeof window !== "undefined" && window.sessionStorage)
      ? window.sessionStorage.getItem("sb_telemetry_session") || ("sess_" + Date.now())
      : "sess_" + Date.now();
    if (typeof window !== "undefined" && window.sessionStorage) {
      window.sessionStorage.setItem("sb_telemetry_session", sessionId);
    }

    backendItems.slice(0, 5).forEach((item, index) => {
      const code = item.schemeCode;
      if (code && !trackedImpressionsRef.current.has(code)) {
        trackedImpressionsRef.current.add(code);
        trackRecommendationEvent({
          schemeCode: code,
          eventType: "RECOMMENDATION_SHOWN",
          recommendationRank: item.rank || (index + 1),
          recommendationScore: item.recommendationScore || (item.matchScore ? item.matchScore / 100.0 : 0.8),
          sessionId,
          metadata: { surface: "recommendations_feed" },
        });
      }
    });
  }, [backendItems]);

  const handleSchemeInteraction = (schemeCode, eventType, score, rank) => {
    if (!schemeCode) return;
    const sessionId = (typeof window !== "undefined" && window.sessionStorage)
      ? window.sessionStorage.getItem("sb_telemetry_session") || ("sess_" + Date.now())
      : "sess_" + Date.now();
    trackRecommendationEvent({
      schemeCode,
      eventType,
      recommendationRank: rank || 1,
      recommendationScore: score ? score / 100.0 : 0.8,
      sessionId,
      metadata: { source: "citizen_portal" },
    });
  };

  // Map backend recommendation items into presentation models
  const evaluatedSchemes = useMemo(() => {
    return backendItems.map((item, originalIndex) => {
      const title = item.schemeTitle || item.title?.english || item.title?.hindi || item.name || item.schemeCode;
      const desc = item.shortDescription?.english || (typeof item.shortDescription === 'string' ? item.shortDescription : "") || item.title?.hindi || "";
      const isCentral = item.schemeLevel !== "STATE";
      const tags = [item.schemeType, item.beneficiaryType, item.stateOrUt, item.categoryName].filter(Boolean);

      // Backend status: "ELIGIBLE" -> "eligible", "INDETERMINATE" -> "possibly_eligible", "NOT_ELIGIBLE" -> "not_eligible"
      let statusKey = "possibly_eligible";
      if (item.eligibilityStatus === "ELIGIBLE") statusKey = "eligible";
      else if (item.eligibilityStatus === "NOT_ELIGIBLE") statusKey = "not_eligible";

      const score = Math.round(
        item.recommendationScore !== undefined ? item.recommendationScore * 100 : (item.matchScore || (statusKey === "eligible" ? 100 : 50))
      );

      const schemeObj = {
        id: item.schemeId || item.slug || item.schemeCode,
        schemeCode: item.schemeCode,
        slug: item.slug,
        name: title,
        description: desc,
        sourceType: isCentral ? "Central" : "State",
        ministry: item.beneficiaryType ? `Ministry of ${item.beneficiaryType}` : "Government of India",
        department: item.stateOrUt || (isCentral ? "Central Department" : "State Department"),
        tags,
      };

      const evaluation = {
        status: statusKey,
        matchScore: score,
        matchedConditions: item.passedConditions || item.matchedConditions || [],
        failedConditions: item.failedConditions || [],
        missingInformation: item.missingInformation || [],
      };

      const checklistItems = item.checklist || [];
      const requiredDocs = item.requiredDocuments || [];
      const appSteps = item.applicationSteps || [];
      const appUrl = item.applicationUrl || null;
      const benefitsList = item.benefits || [];

      // Authoritative document requirements from scheme data
      const rawRequirements = checklistItems.length > 0 ? checklistItems : requiredDocs;
      const readiness = getDocReadinessForScheme(rawRequirements, documents);

      let formattedDeadline = "—";
      if (item.deadline) {
        try {
          const d = new Date(item.deadline);
          if (!isNaN(d.getTime())) {
            formattedDeadline = d.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
          }
        } catch (e) {
          formattedDeadline = "—";
        }
      }

      // Grounded recommendation reasons formatting
      const rawReasons = item.reasons && item.reasons.length > 0
        ? item.reasons
        : item.passedConditions && item.passedConditions.length > 0
        ? item.passedConditions
        : [];

      let statutoryNotice = "Statutory eligibility was confirmed by the EligibilityEngine; ranking model prioritizes eligible schemes.";
      const groundedReasons = [];

      for (const r of rawReasons) {
        if (!r || typeof r !== "string") continue;
        if (r.toLowerCase().includes("statutory eligibility was confirmed") || r.toLowerCase().includes("eligibilityengine")) {
          statutoryNotice = r;
        } else {
          let clean = r.replace(/^Recommended because this scheme is specifically /i, "")
                       .replace(/^Recommended because the scheme /i, "")
                       .replace(/^Recommended because /i, "")
                       .replace(/^Recommended as a /i, "")
                       .trim();
          // Capitalize first letter
          if (clean.length > 0) {
            clean = clean.charAt(0).toUpperCase() + clean.slice(1);
          }
          if (clean && !groundedReasons.includes(clean)) {
            groundedReasons.push(clean);
          }
        }
      }

      if (groundedReasons.length === 0) {
        if (item.explanation?.matchedProfileFactors && item.explanation.matchedProfileFactors.length > 0) {
          for (const f of item.explanation.matchedProfileFactors) {
            groundedReasons.push(`Matches your ${f}`);
          }
        } else {
          groundedReasons.push("You satisfy all statutory eligibility criteria for this scheme.");
        }
      }

      const backendRank = item.rank || (originalIndex + 1);

      const recDetails = {
        backendRank,
        opportunityScore: score,
        opportunityScoreExplanation: item.explanation?.primaryReason || "Calculated authoritatively by SchemeBridge Eligibility Engine based on your verified attributes.",
        statutoryNotice,
        aiBadgeText: statusKey === "eligible" ? "Statutory Match: Eligible" : statusKey === "possibly_eligible" ? "Possibly Eligible" : "Low Match",
        benefitSummary: benefitsList.length > 0 ? benefitsList[0] : (desc || "Direct Benefit Transfer & Government welfare assistance under Central/State guidelines."),
        benefits: benefitsList,
        whyYouMatch: groundedReasons,
        requiredDocuments: requiredDocs,
        checklist: checklistItems,
        applicationSteps: appSteps,
        applicationUrl: appUrl,
        deadlineFormatted: formattedDeadline,
        deadlineDays: 30,
      };

      return { scheme: schemeObj, evaluation, readiness, recDetails, backendRank, originalIndex };
    });
  }, [backendItems, documents]);

  const handleClearFilters = () => {
    setSearchQuery("");
    setActiveFilter("all");
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
        if (!matchesText) return false;
      }

      // 2. Government Scheme Type
      if (schemeType === "Central" && scheme.sourceType !== "Central") return false;
      if (schemeType === "State" && scheme.sourceType !== "State") return false;

      // 3. Core Category (Tags)
      if (selectedCategory !== "all") {
        const allowedTags = mapCategoryToTags(selectedCategory);
        const matchesCategory = scheme.tags.some((tag) => allowedTags.includes(tag.toLowerCase()));
        if (!matchesCategory) return false;
      }

      // 4. Match Status Tab
      if (activeFilter !== "all" && evaluation.status !== activeFilter) {
        return false;
      }

      // 5. Match Score Slider
      if (evaluation.matchScore < minMatchScore) {
        return false;
      }

      // 6. Doc Readiness Slider
      if (readiness.readinessScore < minDocReadiness) {
        return false;
      }

      // 7. Deadline Range
      if (selectedDeadlineRange !== "all") {
        const days = recDetails.deadlineDays;
        if (selectedDeadlineRange === "7" && days > 7) return false;
        if (selectedDeadlineRange === "30" && days > 30) return false;
      }

      return true;
    });
  }, [evaluatedSchemes, searchQuery, selectedCategory, schemeType, activeFilter, minMatchScore, minDocReadiness, selectedDeadlineRange]);

  // Reset to page 1 when filters, search query, sorting, or page size change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, selectedCategory, schemeType, activeFilter, minMatchScore, minDocReadiness, selectedDeadlineRange, sortBy, itemsPerPage]);

  // Sort logic: Invariant: Default "match_score" strictly preserves backend ranking order (#1, #2, ..., #10)
  const sortedSchemes = useMemo(() => {
    const copy = [...filteredSchemes];
    if (sortBy === "match_score") {
      return copy.sort((a, b) => a.backendRank - b.backendRank);
    }
    if (sortBy === "doc_readiness") {
      return copy.sort((a, b) => b.readiness.readinessScore - a.readiness.readinessScore);
    }
    if (sortBy === "alpha") {
      return copy.sort((a, b) => a.scheme.name.localeCompare(b.scheme.name));
    }
    return copy;
  }, [filteredSchemes, sortBy]);

  const totalPages = Math.max(1, Math.ceil(sortedSchemes.length / itemsPerPage));

  // If currentPage exceeds totalPages, clamp it safely
  useEffect(() => {
    if (currentPage > totalPages && totalPages > 0) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  // Paginated items: single authoritative client-side slicing
  const paginatedSchemes = useMemo(() => {
    const safePage = Math.min(Math.max(1, currentPage), totalPages);
    const startIndex = (safePage - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, sortedSchemes.length);
    return sortedSchemes.slice(startIndex, endIndex);
  }, [sortedSchemes, currentPage, totalPages, itemsPerPage]);

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
      {/* ── PAGE HEADER ── */}
      <div className="space-y-4">
        <div className="bg-gradient-to-r from-government-blue via-government-blue-light to-government-blue text-white p-6 rounded-xl shadow-lg">
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="bg-white/10 p-2 rounded-lg">
                  <Sparkles className="h-5 w-5 text-saffron" />
                </div>
                <span className="text-xs font-semibold text-white/80 uppercase tracking-wider">
                  Authoritative Scheme Recommendations
                </span>
              </div>
              <h1 className="text-2xl font-bold tracking-tight">
                Government Scheme Recommendations
              </h1>
              <p className="text-sm text-white/90 leading-relaxed max-w-2xl">
                Evaluated in real-time by the SchemeBridge Eligibility Engine against your verified
                income, category, state, and demographic criteria.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Primary Layout Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">

        {/* ── STICKY FILTER SIDEBAR (Desktop) ── */}
        <aside className="hidden lg:block lg:col-span-1 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 p-5 rounded-xl shadow-sm space-y-5 lg:sticky lg:top-0 lg:self-start lg:max-h-[calc(100vh-8rem)] lg:overflow-y-auto">
          <div className="flex items-center justify-between border-b border-gray-100 dark:border-slate-800 pb-3">
            <h2 className="text-sm font-bold text-gray-900 dark:text-slate-100 flex items-center gap-1.5">
              <SlidersHorizontal className="h-4 w-4 text-government-blue dark:text-indigo-400" />
              Filter Schemes
            </h2>
            <button
              onClick={handleClearFilters}
              aria-label="Reset all filters"
              className="text-xs text-government-blue dark:text-indigo-400 font-semibold hover:underline"
            >
              Reset Filters
            </button>
          </div>

          {/* Residence State — Read-Only from Profile */}
          <div className="space-y-1.5 p-3.5 bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl">
            <span className="text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider block">Residence State</span>
            <div className="flex items-center gap-1.5 font-semibold text-sm text-gray-800 dark:text-slate-100 mt-1">
              <span>📍</span>
              <span>{profile?.state || activeUser?.state || "Not specified"}</span>
            </div>
            <div className="flex justify-between items-center text-[11px] text-gray-500 dark:text-slate-400 mt-2 border-t border-gray-200/60 dark:border-slate-700/60 pt-1.5">
              <span>Evaluated against backend</span>
              <Link to="/profile" className="text-government-blue dark:text-indigo-400 hover:underline font-semibold flex items-center gap-0.5 transition">
                Profile <ArrowRight className="h-3 w-3" />
              </Link>
            </div>
          </div>

          {/* Scheme Category Filter */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider block">Scheme Category</label>
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              aria-label="Filter by scheme category"
              className="w-full text-sm border border-gray-300 dark:border-slate-700 rounded-xl px-3 py-2.5 bg-gray-50 dark:bg-slate-800 text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400 transition cursor-pointer"
            >
              <option value="all">All Categories</option>
              {CORE_CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>

          {/* Scheme Type (Central / State) */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider block">Scheme Type</label>
            <div className="space-y-1.5 text-sm text-gray-700 dark:text-slate-300">
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="schemeType" value="all" checked={schemeType === "all"}
                  onChange={() => setSchemeType("all")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300 dark:border-slate-600" />
                <span>All Schemes</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="schemeType" value="Central" checked={schemeType === "Central"}
                  onChange={() => setSchemeType("Central")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300 dark:border-slate-600" />
                <span>Central Government</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="schemeType" value="State" checked={schemeType === "State"}
                  onChange={() => setSchemeType("State")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300 dark:border-slate-600" />
                <span>State Government</span>
              </label>
            </div>
          </div>

          {/* Application Deadline Filter */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider block">Application Deadline</label>
            <div className="space-y-1.5 text-sm text-gray-700 dark:text-slate-300">
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="deadlineRange" value="all" checked={selectedDeadlineRange === "all"}
                  onChange={() => setSelectedDeadlineRange("all")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300 dark:border-slate-600" />
                <span>All Deadlines</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="deadlineRange" value="7" checked={selectedDeadlineRange === "7"}
                  onChange={() => setSelectedDeadlineRange("7")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300 dark:border-slate-600" />
                <span>Closing Soon (&lt; 7 Days)</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium">
                <input type="radio" name="deadlineRange" value="30" checked={selectedDeadlineRange === "30"}
                  onChange={() => setSelectedDeadlineRange("30")}
                  className="rounded text-government-blue focus:ring-government-blue border-gray-300 dark:border-slate-600" />
                <span>Closing This Month (&lt; 30 Days)</span>
              </label>
            </div>
          </div>

          {/* Minimum Eligibility Score Slider */}
          <div className="space-y-2 border-t border-gray-100 dark:border-slate-800 pt-4">
            <div className="flex justify-between items-center">
              <label className="text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider">Minimum Match Score</label>
              <span className="text-sm font-bold text-government-blue dark:text-indigo-400">{minMatchScore}%+</span>
            </div>
            <input
              type="range" min="0" max="100" step="5"
              value={minMatchScore}
              onChange={(e) => setMinMatchScore(Number(e.target.value))}
              aria-label="Minimum eligibility score filter"
              className="w-full accent-government-blue dark:accent-indigo-400 h-2 bg-gray-200 dark:bg-slate-700 rounded-lg cursor-pointer"
            />
          </div>

          {/* Sort Schemes By */}
          <div className="space-y-1.5 border-t border-gray-100 dark:border-slate-800 pt-4">
            <label className="text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider block">Sort Schemes By</label>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              aria-label="Sort schemes"
              className="w-full text-sm border border-gray-300 dark:border-slate-700 rounded-xl px-3 py-2.5 bg-gray-50 dark:bg-slate-800 text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400 transition cursor-pointer"
            >
              <option value="match_score">Match Score (High to Low)</option>
              <option value="alpha">Scheme Name (A to Z)</option>
            </select>
          </div>
        </aside>

        {/* FEED SECTION (Right Columns) */}
        <div className="col-span-1 lg:col-span-3 space-y-5">

          {/* Search + Controls */}
          <div className="space-y-3">
            <div className="flex gap-3">
              <div className="relative flex-1">
                <span className="absolute inset-y-0 left-3.5 flex items-center pointer-events-none">
                  <Search className="h-4 w-4 text-gray-400 dark:text-slate-400" />
                </span>
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search by Scheme Name, Ministry, Department, or Category"
                  aria-label="Search government schemes"
                  className="w-full text-sm pl-10 pr-10 py-3 bg-white dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-xl shadow-sm text-gray-800 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400 transition"
                />
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery("")}
                    aria-label="Clear search"
                    className="absolute inset-y-0 right-3.5 flex items-center text-gray-400 dark:text-slate-400 hover:text-gray-600 dark:hover:text-slate-200"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>
              <button
                onClick={() => setIsMobileFilterOpen(true)}
                aria-label="Open filter panel"
                className="lg:hidden flex items-center justify-center gap-1.5 bg-white dark:bg-slate-800 border border-gray-300 dark:border-slate-700 text-gray-800 dark:text-slate-200 font-semibold px-4 py-3 rounded-xl shadow-sm text-sm hover:bg-gray-50 dark:hover:bg-slate-700 transition shrink-0"
              >
                <SlidersHorizontal className="h-4 w-4" />
                Filters
              </button>
            </div>

            {/* Tab Bar */}
            <div className="flex bg-gray-100 dark:bg-slate-900 p-1 rounded-xl overflow-x-auto scrollbar-none border border-gray-200 dark:border-slate-800">
              {[
                { key: "all", label: "All Schemes", count: counts.all },
                { key: "eligible", label: "Recommended", count: counts.eligible },
                { key: "possibly_eligible", label: "Eligible / Review", count: counts.possibly_eligible },
                { key: "not_eligible", label: "Not Eligible", count: counts.not_eligible },
              ].map(({ key, label, count }) => (
                <button
                  key={key}
                  onClick={() => setActiveFilter(key)}
                  aria-pressed={activeFilter === key}
                  className={`flex-1 min-w-[90px] text-center px-4 py-2 rounded-lg text-sm font-semibold whitespace-nowrap transition ${
                    activeFilter === key ? "bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 shadow-sm" : "text-gray-600 dark:text-slate-400 hover:text-gray-900 dark:hover:text-slate-100"
                  }`}
                >
                  {label} <span className="font-bold">({count})</span>
                </button>
              ))}
            </div>

            <div className="text-xs text-gray-500 dark:text-slate-400 font-semibold tracking-wide px-1">
              Showing {sortedSchemes.length} matching government {sortedSchemes.length === 1 ? "scheme" : "schemes"}
            </div>
          </div>

          {/* Error / Information Notices */}
          {errorMessage && (
            <div className="p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-xl text-red-800 dark:text-red-300 text-sm flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <AlertCircle className="h-5 w-5 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold">Recommendations are temporarily unavailable.</p>
                  <p className="text-xs text-red-700 dark:text-red-400 mt-0.5">{errorMessage}</p>
                </div>
              </div>
              <button
                onClick={loadRecommendations}
                className="text-xs font-semibold px-3 py-1.5 bg-white dark:bg-slate-800 border border-red-300 dark:border-red-800 rounded-lg hover:bg-red-50 dark:hover:bg-slate-700 transition cursor-pointer shrink-0"
              >
                Retry
              </button>
            </div>
          )}

          {!isLoading && !errorMessage && (!profile?.state && !activeUser?.state && !profile?.age && !activeUser?.age) && (
            <div className="p-4 bg-blue-50 dark:bg-indigo-950/40 border border-blue-200 dark:border-indigo-900 rounded-xl text-government-blue dark:text-indigo-300 text-sm flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <AlertCircle className="h-5 w-5 text-government-blue dark:text-indigo-400 shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold">Complete Your Profile for Better Matching</p>
                  <p className="text-xs text-slate-600 dark:text-slate-400 mt-0.5">
                    Your profile currently lacks state or age details. Providing your residence state, age, annual income, occupation, and social category in your profile helps discover targeted statutory schemes.
                  </p>
                </div>
              </div>
              <Link
                to="/profile"
                className="text-xs font-semibold px-3 py-1.5 bg-government-blue text-white rounded-lg hover:bg-government-blue-dark transition shrink-0"
              >
                Update Profile
              </Link>
            </div>
          )}

          {/* Cards Stack */}
          <div className="space-y-5">
            {isLoading ? (
              <div className="space-y-4">
                <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl p-8 text-center space-y-3">
                  <div className="animate-spin inline-block w-8 h-8 border-4 border-government-blue dark:border-indigo-400 border-t-transparent rounded-full"></div>
                  <p className="text-base font-bold text-gray-800 dark:text-slate-200">
                    Finding schemes that match your profile...
                  </p>
                  <p className="text-xs text-gray-500 dark:text-slate-400">
                    Evaluating statutory eligibility criteria and computing personalized relevance against active welfare schemes.
                  </p>
                </div>
                {[1, 2, 3].map((i) => (
                  <div key={i} className="rounded-xl bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 p-6 animate-pulse space-y-3">
                    <div className="h-4 bg-gray-200 dark:bg-slate-800 rounded w-1/4"></div>
                    <div className="h-6 bg-gray-200 dark:bg-slate-800 rounded w-3/4"></div>
                    <div className="h-4 bg-gray-200 dark:bg-slate-800 rounded w-full"></div>
                  </div>
                ))}
              </div>
            ) : sortedSchemes.length > 0 ? (
              paginatedSchemes.map(({ scheme, evaluation, readiness, recDetails }) => {
                const meta = STATUS_META[evaluation.status] || STATUS_META.possibly_eligible;
                const StatusIcon = meta.icon;
                const appAction = resolveApplicationAction(scheme, applications);
                const isSavedCheck = isSaved(scheme.id);
                const isAppliedCheck = appAction.hasExistingApp;

                return (
                  <div
                    key={scheme.id}
                    className={`bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 border-l-4 ${meta.border} rounded-xl shadow-sm hover:shadow-md transition-shadow overflow-hidden flex flex-col md:flex-row`}
                  >
                    {/* Left main content */}
                    <div className="p-5 flex-1 space-y-4">

                      {/* Source, Department & AI Score Row */}
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-xs bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 px-2.5 py-1 rounded-full font-extrabold tracking-wide flex items-center gap-1 shadow-sm">
                          Rank #{recDetails.backendRank}
                        </span>
                        {scheme.schemeCode && (
                          <span className="text-xs font-mono font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 px-2 py-0.5 rounded border border-slate-200 dark:border-slate-700">
                            {scheme.schemeCode}
                          </span>
                        )}
                        <span className="text-xs bg-gray-100 dark:bg-slate-800 border border-gray-200 dark:border-slate-700 text-gray-700 dark:text-slate-300 px-2 py-1 rounded font-semibold uppercase tracking-wider">
                          {scheme.sourceType === "Central" ? "Central Government" : "State Government"}
                        </span>
                        {scheme.department && (
                          <span className="text-xs text-gray-600 dark:text-slate-400 font-semibold uppercase tracking-wider flex items-center gap-1">
                            <Building2 className="h-3.5 w-3.5" />
                            {scheme.department}
                          </span>
                        )}

                        <span className="text-xs bg-government-blue/10 dark:bg-indigo-950/60 border border-government-blue/20 dark:border-indigo-900 text-government-blue dark:text-indigo-300 px-2.5 py-1 rounded-full font-semibold flex items-center gap-1">
                          Recommendation Match: {recDetails.opportunityScore}%
                        </span>

                        <span className="text-xs bg-saffron text-government-blue-dark px-2.5 py-1 rounded-full font-bold tracking-wide flex items-center gap-1 select-none shadow-sm">
                          <Sparkles className="h-3 w-3" />
                          {recDetails.aiBadgeText}
                        </span>
                      </div>

                      {/* Scheme Name & Ministry */}
                      <div className="space-y-0.5">
                        <h2 className="text-lg font-bold text-gray-900 dark:text-slate-100 leading-snug tracking-tight">
                          {scheme.name}
                        </h2>
                        {scheme.ministry && (
                          <p className="text-xs text-gray-500 dark:text-slate-400 font-semibold">{scheme.ministry}</p>
                        )}
                      </div>

                      {/* Scheme Benefits */}
                      <div className="space-y-1 border-t border-gray-100 dark:border-slate-800 pt-3">
                        <span className="text-xs font-bold text-gray-500 dark:text-slate-400 uppercase tracking-widest block">
                          Scheme Description & Benefits
                        </span>
                        <p className="text-sm text-gray-700 dark:text-slate-300 leading-relaxed">
                          {renderFormattedText(recDetails.benefitSummary)}
                        </p>
                      </div>

                      {/* Status Badges Row */}
                      <div className="flex flex-wrap items-center gap-2 border-t border-gray-100 dark:border-slate-800 pt-3">
                        {appAction.hasExistingApp && (
                          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 text-xs font-semibold">
                            <CheckCircle className="h-3.5 w-3.5 shrink-0 text-indigo-600 dark:text-indigo-400" />
                            <span>{appAction.badgeText}</span>
                          </div>
                        )}
                        <div className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full border text-xs font-semibold ${meta.badge}`}>
                          <StatusIcon className="h-3.5 w-3.5 shrink-0" />
                          <span>
                            {evaluation.status === "eligible" ? "Authoritative Match: Eligible"
                              : evaluation.status === "possibly_eligible" ? "Indeterminate: Additional Data Needed"
                              : "Authoritative Match: Not Eligible"}
                          </span>
                        </div>

                        <div className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full border text-xs font-semibold ${
                          readiness.readinessScore === 100
                            ? "bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300"
                            : readiness.readinessScore >= 50
                            ? "bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800 text-amber-700 dark:text-amber-300"
                            : "bg-red-50 dark:bg-red-950/40 border-red-200 dark:border-red-800 text-red-700 dark:text-red-300"
                        }`}>
                          <ClipboardCheck className="h-3.5 w-3.5 shrink-0" />
                          <span>Document Readiness: {readiness.readinessLabel} ({readiness.totalAvailable}/{readiness.totalRequired})</span>
                        </div>

                        <div className="flex items-center gap-1 px-2.5 py-1.5 rounded-full bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 text-xs font-semibold">
                          <Clock className="h-3.5 w-3.5 shrink-0 text-slate-500 dark:text-slate-400" />
                          <span>Deadline: {recDetails.deadlineFormatted}</span>
                        </div>
                      </div>

                      {/* Grounded Recommendation Reasons & Eligibility Assessment (Expandable) */}
                      <div className="bg-gray-50 dark:bg-slate-800/70 p-4 border border-gray-100 dark:border-slate-700 rounded-xl space-y-3">
                        <button
                          type="button"
                          onClick={() => toggleReasons(scheme.id, recDetails.backendRank <= 3)}
                          aria-expanded={expandedReasons[scheme.id] !== undefined ? expandedReasons[scheme.id] : recDetails.backendRank <= 3}
                          className="w-full flex items-center justify-between text-left cursor-pointer focus:outline-none"
                        >
                          <h4 className="text-xs font-bold text-gray-700 dark:text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                            <CheckCircle2 className="h-4 w-4 text-india-green shrink-0" />
                            Why this scheme is recommended ({recDetails.whyYouMatch.length})
                          </h4>
                          <ChevronDown className={`h-4 w-4 text-gray-500 dark:text-slate-400 transition-transform duration-200 ${(expandedReasons[scheme.id] !== undefined ? expandedReasons[scheme.id] : recDetails.backendRank <= 3) ? "rotate-180" : ""}`} />
                        </button>

                        {(expandedReasons[scheme.id] !== undefined ? expandedReasons[scheme.id] : recDetails.backendRank <= 3) && (
                          <div className="space-y-2 pt-2 border-t border-gray-200/60 dark:border-slate-700/60">
                            <p className="text-[11px] text-gray-500 dark:text-slate-400 italic">
                              {recDetails.statutoryNotice}
                            </p>
                            <ul className="grid sm:grid-cols-2 gap-2">
                              {recDetails.whyYouMatch.map((reason, idx) => (
                                <li key={idx} className="flex items-start gap-2 text-xs text-gray-700 dark:text-slate-300 leading-snug">
                                  <span className="text-india-green font-bold shrink-0 mt-0.5">✓</span>
                                  <span>{reason}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </div>

                      {/* Scheme Document Checklist Preview */}
                      <div className="space-y-2 border-t border-gray-100 dark:border-slate-800 pt-3">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-gray-600 dark:text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                            <FileText className="h-3.5 w-3.5 text-government-blue dark:text-indigo-400" />
                            Required Documents ({readiness.totalRequired})
                          </span>
                          {readiness.totalRequired > 0 && (
                            <span className={`text-[11px] font-semibold ${
                              readiness.readinessScore === 100
                                ? "text-india-green"
                                : readiness.readinessScore >= 50
                                ? "text-saffron-dark dark:text-saffron"
                                : "text-red-500"
                            }`}>
                              {readiness.totalAvailable}/{readiness.totalRequired} Ready ({readiness.readinessScore}%)
                            </span>
                          )}
                        </div>
                        {readiness.evaluatedItems && readiness.evaluatedItems.length > 0 ? (
                          <div className="grid sm:grid-cols-2 gap-2">
                            {readiness.evaluatedItems.map((doc, dIdx) => {
                              const isVer = doc.availabilityStatus === "VERIFIED";
                              const isPend = doc.availabilityStatus === "PENDING";
                              const isUnavail = doc.availabilityStatus === "UNAVAILABLE";
                              return (
                                <div
                                  key={dIdx}
                                  className={`p-2.5 rounded-lg text-xs space-y-1 border transition ${
                                    isVer
                                      ? "bg-emerald-50/70 dark:bg-emerald-950/30 border-emerald-200 dark:border-emerald-800/60"
                                      : isPend
                                      ? "bg-amber-50/70 dark:bg-amber-950/30 border-amber-200 dark:border-amber-800/60"
                                      : isUnavail
                                      ? "bg-gray-50/70 dark:bg-slate-800/50 border-gray-200 dark:border-slate-700/60"
                                      : "bg-red-50/70 dark:bg-red-950/30 border-red-200 dark:border-red-900/60"
                                  }`}
                                >
                                  <div className="flex items-start justify-between gap-1.5">
                                    <div className="flex items-start gap-1.5 min-w-0">
                                      <span
                                        className={`font-bold text-sm leading-none shrink-0 mt-0.5 ${
                                          isVer
                                            ? "text-india-green"
                                            : isPend
                                            ? "text-saffron-dark dark:text-saffron"
                                            : isUnavail
                                            ? "text-gray-400 dark:text-slate-500"
                                            : "text-red-500"
                                        }`}
                                      >
                                        {doc.statusIcon || (isVer ? "✓" : isPend ? "✓" : isUnavail ? "—" : "✗")}
                                      </span>
                                      <div className="min-w-0">
                                        <span className="font-semibold text-gray-800 dark:text-slate-200 block truncate">
                                          {doc.documentName}
                                        </span>
                                        <span
                                          className={`text-[10px] font-bold block ${
                                            isVer
                                              ? "text-india-green"
                                              : isPend
                                              ? "text-saffron-dark dark:text-saffron"
                                              : isUnavail
                                              ? "text-gray-500 dark:text-slate-400"
                                              : "text-red-500"
                                          }`}
                                        >
                                          {doc.availabilityLabel || doc.statusLabel}
                                        </span>
                                      </div>
                                    </div>
                                    <span
                                      className={`text-[10px] px-1.5 py-0.5 rounded font-bold uppercase shrink-0 ${
                                        doc.mandatory
                                          ? "bg-red-100 dark:bg-red-950/60 text-red-700 dark:text-red-300"
                                          : "bg-gray-200 dark:bg-slate-700 text-gray-600 dark:text-slate-300"
                                      }`}
                                    >
                                      {doc.mandatory ? "Mandatory" : "Optional"}
                                    </span>
                                  </div>
                                  {doc.matchedVaultDoc && (
                                    <p className="text-[11px] text-gray-500 dark:text-slate-400 truncate pl-3.5">
                                      Vault: {doc.matchedVaultDoc.name || doc.matchedVaultDoc.documentName}
                                    </p>
                                  )}
                                  {doc.issuingAuthority && (
                                    <p className="text-[11px] text-gray-500 dark:text-slate-400 truncate pl-3.5">
                                      Authority: {doc.issuingAuthority}
                                    </p>
                                  )}
                                  {doc.alternatives && doc.alternatives.length > 0 && (
                                    <p className="text-[10px] text-government-blue dark:text-indigo-400 pl-3.5">
                                      Any one of: {doc.alternatives.join(", ")}
                                    </p>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        ) : (
                          <p className="text-xs text-gray-500 dark:text-slate-400 italic">
                            Official document requirements will be verified during application submission.
                          </p>
                        )}
                      </div>

                      {/* Application Steps Preview */}
                      {recDetails.applicationSteps && recDetails.applicationSteps.length > 0 && (
                        <div className="space-y-1.5 border-t border-gray-100 dark:border-slate-800 pt-3">
                          <span className="text-xs font-bold text-gray-600 dark:text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                            <ClipboardCheck className="h-3.5 w-3.5 text-india-green" />
                            Application Procedure
                          </span>
                          <ol className="space-y-1 pl-4 list-decimal text-xs text-gray-700 dark:text-slate-300">
                            {recDetails.applicationSteps.slice(0, 3).map((step, sIdx) => (
                              <li key={sIdx} className="leading-snug">{step}</li>
                            ))}
                          </ol>
                        </div>
                      )}
                    </div>

                    {/* Right: Scores & Actions Panel */}
                    <div className="bg-gray-50 dark:bg-slate-800/80 border-t md:border-t-0 md:border-l border-gray-200 dark:border-slate-700 p-5 w-full md:w-56 shrink-0 flex flex-col justify-between gap-4">
                      <div className="space-y-4">
                        <div>
                          <div className="flex justify-between items-center mb-1.5">
                            <span className="text-xs text-gray-500 dark:text-slate-400 font-semibold uppercase tracking-wide">Recommendation Match</span>
                            <span className="text-sm font-bold text-government-blue dark:text-indigo-400">{recDetails.opportunityScore}%</span>
                          </div>
                          <div className="h-2 bg-gray-200 dark:bg-slate-700 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-gradient-to-r from-government-blue to-government-blue-light dark:from-indigo-600 dark:to-indigo-400 rounded-full transition-all duration-500"
                              style={{ width: `${recDetails.opportunityScore}%` }}
                              role="progressbar"
                              aria-valuenow={recDetails.opportunityScore}
                              aria-valuemin={0}
                              aria-valuemax={100}
                            />
                          </div>
                          <p className="text-xs text-gray-500 dark:text-slate-400 mt-1.5 leading-normal italic">
                            {recDetails.opportunityScoreExplanation}
                          </p>
                        </div>
                      </div>

                      <div className="space-y-2 pt-2 border-t border-gray-200 dark:border-slate-700">
                        <Link
                          to={appAction.targetRoute}
                          onClick={() => handleSchemeInteraction(scheme.schemeCode || scheme.id, "SCHEME_VIEWED", recDetails.opportunityScore)}
                          aria-label={`${appAction.buttonText} for ${scheme.name}`}
                          className={`w-full inline-flex items-center justify-center gap-1.5 py-2.5 rounded-lg text-sm font-semibold shadow-sm transition ${
                            appAction.action === "CONTINUE_APPLICATION"
                              ? "bg-saffron hover:bg-saffron-dark text-government-blue-dark font-bold"
                              : "bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 text-white"
                          }`}
                        >
                          <span>{appAction.buttonText}</span>
                          <ArrowRight className="h-4 w-4" />
                        </Link>

                        {/* View Details CTA */}
                        <Link
                          to={`/scheme/${scheme.slug || scheme.schemeCode || scheme.id}`}
                          onClick={() => handleSchemeInteraction(scheme.schemeCode || scheme.id, "SCHEME_VIEWED", recDetails.opportunityScore)}
                          className="w-full inline-flex items-center justify-center gap-1.5 bg-white dark:bg-slate-800 hover:bg-gray-50 dark:hover:bg-slate-700 text-gray-700 dark:text-slate-200 border border-gray-300 dark:border-slate-700 py-2 rounded-lg text-xs font-semibold transition"
                        >
                          <span>View Details</span>
                        </Link>

                        {recDetails.applicationUrl && (
                          <a
                            href={recDetails.applicationUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="w-full inline-flex items-center justify-center gap-1.5 bg-emerald-50 dark:bg-emerald-950/50 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800 py-2 rounded-lg text-xs font-semibold transition"
                          >
                            <span>Official Portal</span>
                            <ExternalLink className="h-3 w-3" />
                          </a>
                        )}

                        <button
                          onClick={() => {
                            saveScheme(scheme);
                            handleSchemeInteraction(scheme.schemeCode || scheme.id, "SCHEME_SAVED", recDetails.opportunityScore);
                          }}
                          disabled={isSavedCheck || isAppliedCheck}
                          aria-label={isAppliedCheck ? "Application submitted" : isSavedCheck ? "Scheme saved" : `Save ${scheme.name}`}
                          className={`w-full inline-flex items-center justify-center gap-1.5 border py-2.5 rounded-lg text-sm font-semibold transition ${
                            isSavedCheck || isAppliedCheck
                              ? "bg-gray-100 dark:bg-slate-800 border-gray-200 dark:border-slate-700 text-gray-400 dark:text-slate-500 cursor-not-allowed"
                              : "bg-white dark:bg-slate-800 border-gray-300 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-700 text-gray-700 dark:text-slate-200"
                          }`}
                        >
                          {isAppliedCheck ? (
                            <><ClipboardCheck className="h-4 w-4 text-gray-400 dark:text-slate-500" /><span>Applied</span></>
                          ) : isSavedCheck ? (
                            <><BookmarkCheck className="h-4 w-4 text-india-green" /><span>Saved</span></>
                          ) : (
                            <><BookmarkPlus className="h-4 w-4 text-gray-500 dark:text-slate-400" /><span>Save Scheme</span></>
                          )}
                        </button>

                        <button
                          onClick={() => handleAskAI(`I want to know more about ${scheme.name}. Can you explain how this scheme works and what benefits I will get?`)}
                          aria-label={`Open AI Assistant for ${scheme.name}`}
                          className="w-full inline-flex items-center justify-center gap-1.5 bg-government-blue/10 dark:bg-indigo-950/60 hover:bg-government-blue/20 dark:hover:bg-indigo-900/80 text-government-blue dark:text-indigo-300 border border-government-blue/20 dark:border-indigo-900/60 py-2.5 rounded-lg text-sm font-semibold transition"
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
                title={backendItems.length === 0 ? "No schemes currently match your eligibility profile." : "No schemes match your current filters"}
                description={backendItems.length === 0
                  ? "Based on your verified citizen attributes, the statutory eligibility engine did not find active schemes where all conditions are satisfied. You may update your profile or browse the catalog."
                  : "Try adjusting your search query, changing the scheme category, or clearing active filters to discover more matching government programmes."}
                action={backendItems.length === 0
                  ? { label: "Update Profile", onClick: () => navigate("/profile"), variant: "primary" }
                  : { label: "Clear All Filters", onClick: handleClearFilters, variant: "primary" }}
              />
            )}
          </div>

          {/* Pagination Controls */}
          {sortedSchemes.length > 0 && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
              itemsPerPage={itemsPerPage}
              onItemsPerPageChange={(newSize) => {
                setItemsPerPage(newSize);
                setCurrentPage(1);
              }}
              totalItems={sortedSchemes.length}
            />
          )}

        </div>
      </div>

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
