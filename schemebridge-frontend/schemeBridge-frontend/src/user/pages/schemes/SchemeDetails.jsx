import React, { useState, useEffect, useMemo } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { checkEligibility } from "@utils/eligibilityEngine";
import { getDocReadinessForScheme } from "@utils/documentReadiness";
import { renderFormattedText } from "@utils/textFormatter";
import { resolveApplicationAction } from "@utils/applicationStateMapping";
import { usePageMeta } from "@utils/usePageMeta";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import { getSchemeById, getSchemeByCode, trackRecommendationEvent } from "@services/schemeService";
import { getSchemeDocumentChecklist } from "@services/applicationService";
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle,
  AlertCircle,
  XCircle,
  ShieldCheck,
  Building2,
  Clock,
  Briefcase,
  FileText,
  IndianRupee,
  Calendar,
  BookmarkCheck,
  BookmarkPlus,
  MessageSquare,
  ExternalLink,
  ChevronDown,
  ChevronUp,
} from "lucide-react";

function FAQAccordion({ faqs = [] }) {
  const [openIdx, setOpenIdx] = useState(null);
  if (!faqs || faqs.length === 0) {
    return <p className="text-sm text-gray-500">No frequently asked questions available for this scheme.</p>;
  }
  return (
    <div className="space-y-3">
      {faqs.map((faq, idx) => {
        const q = faq.question?.english || faq.question || `Question ${idx + 1}`;
        const a = faq.answer?.english || faq.answer || "Please refer to official guidelines.";
        const isOpen = openIdx === idx;
        return (
          <div key={idx} className="border border-gray-200 rounded-xl overflow-hidden bg-white">
            <button
              onClick={() => setOpenIdx(isOpen ? null : idx)}
              className="w-full text-left p-4 flex items-center justify-between gap-3 hover:bg-gray-50 transition"
            >
              <span className="text-sm font-bold text-gray-800">{q}</span>
              {isOpen ? <ChevronUp className="h-4 w-4 text-gray-500 shrink-0" /> : <ChevronDown className="h-4 w-4 text-gray-500 shrink-0" />}
            </button>
            {isOpen && (
              <div className="p-4 pt-0 text-sm text-gray-600 leading-relaxed border-t border-gray-100 bg-gray-50/50">
                {a}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

function normalizeBackendScheme(data) {
  if (!data) return null;
  const id = data.id || data.schemeCode || "";
  const schemeCode = data.schemeCode || "";
  const slug = data.slug || "";
  const name =
    data.title?.english ||
    data.title?.hindi ||
    (typeof data.title === "string" ? data.title : "") ||
    data.name ||
    schemeCode;
  const description =
    data.description?.english ||
    data.description?.hindi ||
    (typeof data.description === "string" ? data.description : "") ||
    "";
  const shortDescription =
    data.shortDescription?.english ||
    data.shortDescription?.hindi ||
    (typeof data.shortDescription === "string" ? data.shortDescription : "") ||
    "";
  const category =
    data.category?.name ||
    data.category?.code ||
    data.categoryCode ||
    data.category ||
    "General";
  const ministry =
    data.ministry ||
    (data.schemeLevel === "CENTRAL" ? "Central Government" : "State Government");
  const department = data.department || "Government Board";
  const sourceType = data.schemeLevel === "STATE" ? "State" : "Central";

  let benefits = [];
  if (Array.isArray(data.benefits)) {
    benefits = data.benefits.map((b) => {
      if (typeof b === "string") return b;
      if (b.description?.english) return b.description.english;
      if (b.benefitType) return `${b.benefitType}${b.amount ? `: ₹${b.amount}` : ""}`;
      return JSON.stringify(b);
    });
  }

  let requiredDocuments = [];
  if (Array.isArray(data.requiredDocuments)) {
    requiredDocuments = data.requiredDocuments.map((d) => {
      if (typeof d === "string") return d;
      return d.documentName?.english || d.documentCode || d.name || String(d);
    });
  }

  return {
    id,
    code: schemeCode,
    schemeCode,
    slug,
    name,
    description,
    shortDescription,
    category,
    ministry,
    department,
    sourceType,
    benefits,
    requiredDocuments,
    eligibilityCriteria: data.eligibilityRules?.conditions?.map((c) => (c.field ? `${c.field} ${c.operator || "=="} ${c.value || ""}` : "")) || [],
    applicationSteps: data.steps?.map((s) => s.description || s.title || "") || [
      "Submit official registration via SchemeBridge portal.",
      "Upload verified canonical identity and income credentials.",
      "Track nodal officer review and verification updates."
    ],
    faqs: data.faqs || [],
    applicationInfo: data.applicationInfo,
    source: data.source,
    tags: data.tags || [],
    rawBackendScheme: data
  };
}

export default function SchemeDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { schemes = [], applyToScheme, hasApplied, saveScheme, isSaved, profile, documents, applications = [] } = useApp();

  const [loadedScheme, setLoadedScheme] = useState(null);
  const [loading, setLoading] = useState(true);
  const [canonicalChecklist, setCanonicalChecklist] = useState(null);

  useEffect(() => {
    let isMounted = true;
    setLoading(true);

    // 1. Check local cache first
    const cached = (schemes || []).find(
      (s) => s.id === id || s.schemeCode === id || s.slug === id || s.schemeCode?.toLowerCase() === id?.toLowerCase() || s.slug?.toLowerCase() === id?.toLowerCase()
    );
    if (cached) {
      setLoadedScheme(cached);
      setLoading(false);
      const code = cached.code || cached.schemeCode || cached.slug || cached.id;
      getSchemeDocumentChecklist(code)
        .then((res) => { if (isMounted) setCanonicalChecklist(res?.data || res); })
        .catch(() => { if (isMounted) setCanonicalChecklist(null); });
      return;
    }

    // 2. Fetch fresh from backend API (by ID or schemeCode or slug)
    async function fetchFromApi() {
      try {
        let res = await getSchemeById(id);
        if (res.error || !res.data) {
          res = await getSchemeByCode(id);
        }
        if (isMounted) {
          if (!res.error && res.data) {
            const normalized = normalizeBackendScheme(res.data);
            setLoadedScheme(normalized);
            const effectiveCode = normalized.schemeCode || normalized.slug || normalized.id;
            getSchemeDocumentChecklist(effectiveCode)
              .then((cRes) => { if (isMounted) setCanonicalChecklist(cRes?.data || cRes); })
              .catch(() => { if (isMounted) setCanonicalChecklist(null); });
          } else {
            setLoadedScheme(null);
          }
        }
      } catch (err) {
        console.error("Failed to load scheme details:", err);
        if (isMounted) setLoadedScheme(null);
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    fetchFromApi();
    return () => { isMounted = false; };
  }, [id, schemes]);

  const scheme = loadedScheme;
  usePageMeta(scheme?.name || "Scheme Details", scheme?.description || "Government scheme details and eligibility criteria");

  const [activeTab, setActiveTab] = useState("overview");
  const [isApplying, setIsApplying] = useState(false);
  const [appliedSuccess, setAppliedSuccess] = useState(false);

  // AI chat states
  const [aiChatOpen, setAiChatOpen] = useState(false);
  const [aiInitialQuery, setAiInitialQuery] = useState("");

  const appAction = useMemo(() => {
    return resolveApplicationAction(scheme, applications);
  }, [scheme, applications]);

  const alreadyApplied = appAction.hasExistingApp;
  const alreadySaved = scheme ? isSaved(scheme.id) : false;

  const effectiveRequirements = useMemo(() => {
    if (canonicalChecklist?.items && canonicalChecklist.items.length > 0) {
      return canonicalChecklist.items;
    }
    return scheme?.requiredDocuments || [];
  }, [canonicalChecklist, scheme]);

  const evaluation = useMemo(() => (scheme ? checkEligibility(profile, scheme, documents) : null), [scheme, profile, documents]);
  const readiness = useMemo(() => (scheme ? getDocReadinessForScheme(effectiveRequirements, documents) : null), [scheme, effectiveRequirements, documents]);
  const recDetails = useMemo(() => {
    if (!scheme) return null;
    return {
      benefitSummary: scheme.shortDescription || scheme.description?.english || scheme.description || "Direct financial assistance and institutional support under government mandate.",
      opportunityScore: evaluation?.matchScore ?? 85,
      opportunityScoreExplanation: evaluation?.qualifyingReasons?.[0] || "Profile matches high-priority demographic and occupational focus.",
      deadlineDays: scheme.applicationDeadline ? Math.max(1, Math.ceil((new Date(scheme.applicationDeadline) - new Date()) / (1000 * 60 * 60 * 24))) : null,
    };
  }, [scheme, evaluation]);

  // Phase 26 Telemetry: Record SCHEME_VIEWED on load (deduplicated)
  useEffect(() => {
    if (!scheme) return;
    const effectiveCode = scheme.schemeCode || scheme.code || scheme.id;
    if (!effectiveCode) return;
    const sessionId = (typeof window !== "undefined" && window.sessionStorage)
      ? window.sessionStorage.getItem("sb_telemetry_session") || ("sess_" + Date.now())
      : "sess_" + Date.now();

    trackRecommendationEvent({
      schemeCode: effectiveCode,
      eventType: "SCHEME_VIEWED",
      recommendationRank: 1,
      recommendationScore: evaluation?.matchScore ? evaluation.matchScore / 100.0 : 0.8,
      sessionId,
      metadata: { surface: "scheme_details" }
    });
  }, [scheme?.id, scheme?.schemeCode, evaluation?.matchScore]);

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto py-12 px-4 space-y-6 animate-pulse">
        <div className="h-6 w-32 bg-slate-200 rounded-lg" />
        <div className="h-32 bg-slate-200 rounded-2xl" />
        <div className="grid grid-cols-3 gap-4">
          <div className="h-24 bg-slate-200 rounded-xl" />
          <div className="h-24 bg-slate-200 rounded-xl" />
          <div className="h-24 bg-slate-200 rounded-xl" />
        </div>
        <div className="h-64 bg-slate-200 rounded-2xl" />
      </div>
    );
  }

  if (!scheme) {
    return (
      <div className="max-w-md mx-auto my-16 text-center space-y-4">
        <h2 className="text-xl font-bold text-gray-800">Scheme Not Found</h2>
        <p className="text-gray-500 text-sm">
          The scheme ID requested does not exist or may have been archived.
        </p>
        <Link to="/recommendations" className="inline-flex items-center gap-1.5 text-government-blue font-semibold underline text-sm">
          <ArrowLeft className="h-4 w-4" />
          Back to Recommendations
        </Link>
      </div>
    );
  }

  const handleApply = async () => {
    setIsApplying(true);
    try {
      const effectiveCode = scheme.schemeCode || scheme.code || scheme.id;
      const sessionId = (typeof window !== "undefined" && window.sessionStorage)
        ? window.sessionStorage.getItem("sb_telemetry_session") || ("sess_" + Date.now())
        : "sess_" + Date.now();

      trackRecommendationEvent({
        schemeCode: effectiveCode,
        eventType: "APPLICATION_STARTED",
        recommendationRank: 1,
        recommendationScore: evaluation?.matchScore ? evaluation.matchScore / 100.0 : 0.8,
        sessionId,
        metadata: { surface: "scheme_details_apply_button" }
      });

      const success = await applyToScheme(scheme);
      if (success) {
        setAppliedSuccess(true);
        trackRecommendationEvent({
          schemeCode: effectiveCode,
          eventType: "SCHEME_APPLIED",
          recommendationRank: 1,
          recommendationScore: evaluation?.matchScore ? evaluation.matchScore / 100.0 : 0.8,
          sessionId,
          metadata: { surface: "scheme_details_applied" }
        });
      }
    } catch (err) {
      console.error("Apply error:", err);
    } finally {
      setIsApplying(false);
    }
  };

  const handleAskAI = (queryText) => {
    setAiInitialQuery(queryText);
    setAiChatOpen(true);
  };

  const statusStyleMap = {
    eligible: { color: "text-india-green bg-india-green/10 border-india-green/20", icon: CheckCircle },
    possibly_eligible: { color: "text-saffron-dark bg-saffron/10 border-saffron/20", icon: AlertCircle },
    not_eligible: { color: "text-red-700 bg-red-50 border-red-200", icon: XCircle },
  };
  const sStyle = evaluation ? statusStyleMap[evaluation.status] : statusStyleMap.not_eligible;
  const StatusIcon = sStyle.icon;

  const tabs = [
    { id: "overview", label: "Overview" },
    { id: "benefits", label: "Benefits" },
    { id: "eligibility", label: "Eligibility" },
    { id: "documents", label: "Documents" },
    { id: "faqs", label: "FAQs" },
    { id: "process", label: "Application Process" },
    { id: "website", label: "Official Website" },
  ];

  return (
    <div className="space-y-6">

      {/* Back breadcrumb */}
      <Link
        to="/recommendations"
        className="inline-flex items-center gap-1.5 text-gray-600 dark:text-slate-400 hover:text-gray-900 dark:hover:text-slate-100 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-government-blue rounded px-1"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to Schemes
      </Link>

      {/* Government Hero Banner */}
      <div className="bg-gradient-to-r from-government-blue via-government-blue-light to-government-blue text-white p-6 sm:p-8 rounded-2xl shadow-lg space-y-4 relative overflow-hidden">
        {/* Decorative elements */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-saffron/10 rounded-full -mr-32 -mt-32" />
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-india-green/10 rounded-full -ml-24 -mb-24" />

        <div className="relative space-y-3">
          <div className="flex flex-wrap gap-2 items-center">
            <span className="text-xs bg-white/20 text-white border border-white/30 px-3 py-1 rounded-full font-bold uppercase tracking-wider">
              {scheme.sourceType === "Central" ? "Central Scheme" : "State Scheme"}
            </span>
            <span className="text-xs bg-india-green/20 text-india-green border border-india-green/30 px-3 py-1 rounded-full font-bold uppercase tracking-wider flex items-center gap-1">
              <ShieldCheck className="h-3.5 w-3.5" />
              Verified Source
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight leading-snug text-white">
            {scheme.name}
          </h1>

          <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-white/90 font-medium">
            <span className="flex items-center gap-2">
              <Building2 className="h-4 w-4" />
              {scheme.ministry}
            </span>
            <span className="flex items-center gap-2">
              <Clock className="h-4 w-4" />
              Last Updated: {scheme.lastUpdated}
            </span>
            {scheme.department && (
              <span className="flex items-center gap-2">
                <Briefcase className="h-4 w-4" />
                {scheme.department}
              </span>
            )}
          </div>
        </div>
      </div>

      {appliedSuccess && (
        <div className="bg-india-green/10 border border-india-green/20 text-india-green text-sm p-4 rounded-xl flex items-center gap-2 shadow-sm">
          <CheckCircle className="h-5 w-5 shrink-0" />
          <span>Application submitted successfully!</span>
        </div>
      )}

      {/* Main Grid Content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">

        {/* Left Side: Dynamic Tabbed Layout */}
        <div className="lg:col-span-2 space-y-6">

          {/* Tab Selection Bar */}
          <div className="flex bg-gray-100 dark:bg-slate-900 p-1 rounded-2xl overflow-x-auto scrollbar-none shadow-inner border border-gray-200 dark:border-slate-800">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex-1 min-w-[100px] text-center px-4 py-2.5 rounded-xl text-sm font-semibold whitespace-nowrap transition ${
                  activeTab === tab.id
                    ? "bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100 shadow-sm border border-gray-200 dark:border-slate-700"
                    : "text-gray-600 dark:text-slate-400 hover:text-gray-900 dark:hover:text-slate-100"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Tab Content Panel */}
          <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 p-6 rounded-2xl shadow-sm min-h-[300px]">

            {/* 1. Overview Tab */}
            {activeTab === "overview" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 dark:text-slate-100 border-l-4 border-government-blue dark:border-indigo-400 pl-4">
                    Scheme Overview
                  </h3>
                  <p className="text-sm text-gray-700 dark:text-slate-300 leading-relaxed mt-4">
                    {renderFormattedText(scheme.description)}
                  </p>
                </div>

                <div className="grid sm:grid-cols-2 gap-4">
                  <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 p-5 rounded-xl">
                    <div className="flex items-center gap-2 mb-3">
                      <Building2 className="h-5 w-5 text-government-blue dark:text-indigo-400" />
                      <h4 className="text-sm font-bold text-gray-900 dark:text-slate-100">Implementing Department</h4>
                    </div>
                    <p className="text-gray-700 dark:text-slate-300 font-semibold">{scheme.department || "State Directorate"}</p>
                  </div>

                  <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 p-5 rounded-xl">
                    <div className="flex items-center gap-2 mb-3">
                      <FileText className="h-5 w-5 text-india-green" />
                      <h4 className="text-sm font-bold text-gray-900 dark:text-slate-100">Funding Authority</h4>
                    </div>
                    <p className="text-gray-700 dark:text-slate-300 font-semibold">{scheme.sourceType === "Central" ? "Central Government" : "State Government"}</p>
                  </div>
                </div>

                {recDetails && (
                  <div className="bg-government-blue/5 dark:bg-indigo-950/40 border border-government-blue/20 dark:border-indigo-900/60 p-5 rounded-xl">
                    <h4 className="text-sm font-bold text-government-blue dark:text-indigo-300 mb-2">AI Recommendation Rationale</h4>
                    <p className="text-sm text-gray-700 dark:text-slate-300">{renderFormattedText(recDetails.benefitSummary)}</p>
                  </div>
                )}
              </div>
            )}

            {/* 2. Benefits Tab */}
            {activeTab === "benefits" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 dark:text-slate-100 border-l-4 border-india-green pl-4">
                    Financial & Welfare Benefits
                  </h3>
                </div>

                <ul className="space-y-4">
                  {Array.isArray(scheme.benefits) ? (
                    scheme.benefits.map((benefit, idx) => (
                      <li key={idx} className="flex items-start gap-3 p-4 bg-gray-50 dark:bg-slate-800/70 rounded-xl border border-gray-200 dark:border-slate-700">
                        <div className="bg-india-green/10 p-2 rounded-full shrink-0 mt-0.5">
                          <IndianRupee className="h-4 w-4 text-india-green" />
                        </div>
                        <span className="text-sm text-gray-700 dark:text-slate-200 leading-relaxed font-medium">{benefit}</span>
                      </li>
                    ))
                  ) : (
                    <li className="text-sm text-gray-500 dark:text-slate-400">No benefit details listed.</li>
                  )}
                </ul>
              </div>
            )}

            {/* 3. Eligibility Tab */}
            {activeTab === "eligibility" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 dark:text-slate-100 border-l-4 border-saffron pl-4">
                    Eligibility Criteria
                  </h3>
                </div>

                <div className={`p-5 rounded-xl border flex items-start gap-3 ${sStyle.color}`}>
                  <StatusIcon className="h-6 w-6 shrink-0 mt-0.5" />
                  <div>
                    <h4 className="font-bold text-sm">
                      {evaluation?.status === "eligible" 
                        ? "You are eligible for this scheme" 
                        : evaluation?.status === "possibly_eligible" 
                        ? "You may be eligible for this scheme" 
                        : "You do not meet all criteria"}
                    </h4>
                    <p className="text-xs mt-1 opacity-90 leading-relaxed font-medium">
                      Based on official criteria and your profile demographics.
                    </p>
                  </div>
                </div>

                <div className="space-y-4">
                  {/* Criteria Match Details */}
                  {evaluation?.qualifyingReasons.length > 0 && (
                    <div className="bg-india-green/5 dark:bg-emerald-950/40 border border-india-green/20 dark:border-emerald-800/50 rounded-xl p-5 space-y-3">
                      <h4 className="text-xs font-bold text-india-green uppercase tracking-widest">Satisfied Criteria</h4>
                      <ul className="space-y-2">
                        {evaluation.qualifyingReasons.map((reason, idx) => (
                          <li key={idx} className="flex items-start gap-2 text-sm text-india-green font-medium">
                            <CheckCircle className="h-4 w-4 shrink-0 mt-0.5" />
                            <span>{reason}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {evaluation?.unmetConditions.length > 0 && (
                    <div className="bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-xl p-5 space-y-3">
                      <h4 className="text-xs font-bold text-red-800 dark:text-red-300 uppercase tracking-widest">Unmet Criteria</h4>
                      <ul className="space-y-2">
                        {evaluation.unmetConditions.map((reason, idx) => (
                          <li key={idx} className="flex items-start gap-2 text-sm text-red-700 dark:text-red-300 font-medium">
                            <XCircle className="h-4 w-4 shrink-0 mt-0.5" />
                            <span>{reason}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* 4. Required Documents Tab */}
            {activeTab === "documents" && (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-bold text-gray-900 dark:text-slate-100 border-l-4 border-government-blue dark:border-indigo-400 pl-4">
                    Required Documents
                  </h3>
                </div>

                {/* Unmapped state notification */}
                {canonicalChecklist?.documentStatus === "DOCUMENT_REQUIREMENTS_NOT_MAPPED" && (
                  <div className="p-4 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900 rounded-xl text-amber-800 dark:text-amber-300 text-sm flex items-start gap-3">
                    <AlertCircle className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
                    <div>
                      <p className="font-bold">Document requirements are not currently mapped from an official source.</p>
                      <p className="text-xs text-amber-700 dark:text-amber-400 mt-0.5">Please check official portal guidelines or consult the nodal department.</p>
                    </div>
                  </div>
                )}

                {/* Readiness Meter */}
                <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl p-5 space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-bold text-gray-700 dark:text-slate-200">Document Readiness</span>
                    <span
                      className={`text-sm font-bold px-3 py-1.5 rounded-full border ${
                        readiness?.readinessScore === 100
                          ? "bg-india-green/10 text-india-green border-india-green/20"
                          : readiness?.readinessScore >= 50
                          ? "bg-saffron/10 text-saffron-dark border-saffron/20"
                          : "bg-red-50 text-red-700 border-red-200"
                      }`}
                    >
                      {readiness?.readinessScore}% · {readiness?.readinessLabel}
                    </span>
                  </div>
                  <div className="h-3 bg-gray-200 dark:bg-slate-700 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${
                        readiness?.readinessScore === 100
                          ? "bg-india-green"
                          : readiness?.readinessScore >= 50
                          ? "bg-saffron"
                          : "bg-red-500"
                      }`}
                      style={{ width: `${readiness?.readinessScore}%` }}
                    />
                  </div>
                  <p className="text-xs text-gray-500 dark:text-slate-400">
                    {canonicalChecklist?.items?.length || scheme.requiredDocuments?.length || 0} document requirements identified for this scheme.
                  </p>
                </div>

                {/* Canonical Document Items */}
                <div className="space-y-3">
                  {(canonicalChecklist?.items && canonicalChecklist.items.length > 0 ? canonicalChecklist.items : (scheme.requiredDocuments || []).map(d => ({ documentName: typeof d === "string" ? d : d.documentName, mandatory: true }))).map((docItem, idx) => {
                    const docName = docItem.documentName || docItem.officialDocumentName || docItem.name || (typeof docItem === "string" ? docItem : "Required Document");
                    const evalMatch = readiness?.evaluatedItems?.[idx] || readiness?.evaluatedItems?.find(e => e.documentName?.toLowerCase() === docName.toLowerCase());
                    const isVer = evalMatch?.availabilityStatus === "VERIFIED";
                    const isPend = evalMatch?.availabilityStatus === "PENDING";
                    const isAvail = isVer || isPend;

                    return (
                      <div
                        key={idx}
                        className={`p-4 rounded-xl border shadow-sm space-y-2.5 transition ${
                          isVer
                            ? "bg-emerald-50/40 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-800/60"
                            : isPend
                            ? "bg-amber-50/40 dark:bg-amber-950/20 border-amber-200 dark:border-amber-800/60"
                            : "bg-white dark:bg-slate-800/70 border-gray-200 dark:border-slate-700"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="flex items-start gap-3 flex-1">
                            {isVer ? (
                              <CheckCircle className="h-5 w-5 text-india-green shrink-0 mt-0.5" />
                            ) : isPend ? (
                              <Clock className="h-5 w-5 text-saffron-dark dark:text-saffron shrink-0 mt-0.5" />
                            ) : (
                              <XCircle className="h-5 w-5 text-red-500 shrink-0 mt-0.5" />
                            )}
                            <div>
                              <span className="font-bold text-gray-800 dark:text-slate-100 text-sm leading-snug">{docName}</span>
                              <div className="flex items-center gap-2 mt-1">
                                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                                  isVer
                                    ? "bg-emerald-100 dark:bg-emerald-950/60 text-india-green border-emerald-300 dark:border-emerald-800"
                                    : isPend
                                    ? "bg-amber-100 dark:bg-amber-950/60 text-saffron-dark dark:text-saffron border-amber-300 dark:border-amber-800"
                                    : "bg-red-50 dark:bg-red-950/60 text-red-700 dark:text-red-300 border-red-200 dark:border-red-900"
                                }`}>
                                  {evalMatch?.statusLabel || (isAvail ? "Uploaded" : "Not Available")}
                                </span>
                                {evalMatch?.matchedVaultDoc && (
                                  <span className="text-[11px] text-gray-500 dark:text-slate-400">
                                    Matched: {evalMatch.matchedVaultDoc.name || evalMatch.matchedVaultDoc.documentName}
                                  </span>
                                )}
                              </div>
                              {docItem.whyRequired && (
                                <p className="text-xs text-gray-500 dark:text-slate-400 mt-1">{docItem.whyRequired}</p>
                              )}
                              {docItem.issuingAuthority && (
                                <p className="text-xs text-gray-400 dark:text-slate-500 font-medium mt-0.5">
                                  <span className="font-semibold text-gray-600 dark:text-slate-300">Issued by:</span> {docItem.issuingAuthority}
                                </p>
                              )}
                            </div>
                          </div>
                          <div className="flex flex-col items-end gap-1 shrink-0">
                            <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                              docItem.mandatory ? "bg-red-50 dark:bg-red-950/60 text-red-700 dark:text-red-300 border border-red-200 dark:border-red-900" : "bg-gray-100 dark:bg-slate-700 text-gray-600 dark:text-slate-300 border border-gray-200 dark:border-slate-600"
                            }`}>
                              {docItem.mandatory ? "Mandatory" : "Optional"}
                            </span>
                          </div>
                        </div>

                        {/* ONE_OF Alternative Group Display */}
                        {docItem.alternativeGroupType === "ONE_OF" && docItem.alternatives && docItem.alternatives.length > 0 && (
                          <div className="mt-2 pl-3 py-2 bg-blue-50/60 dark:bg-indigo-950/40 border-l-2 border-government-blue dark:border-indigo-400 rounded-r-lg space-y-1.5">
                            <p className="text-xs font-semibold text-gray-800 dark:text-slate-200">Any ONE of:</p>
                            <ul className="space-y-1 pl-1">
                              {docItem.alternatives.map((alt, aIdx) => (
                                <li key={aIdx} className="text-xs text-gray-700 dark:text-slate-300 flex items-center gap-2">
                                  <span className="h-1.5 w-1.5 rounded-full bg-government-blue dark:bg-indigo-400 shrink-0"></span>
                                  <span>{alt}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>


                {/* AI Guidance Box */}
                <div className="bg-government-blue dark:bg-slate-800 text-white rounded-xl p-5 space-y-3 border border-transparent dark:border-slate-700">
                  <div className="flex items-center gap-2">
                    <div className="bg-white/10 p-2 rounded-lg">
                      <svg className="h-4 w-4 text-saffron" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.346.9A2 2 0 0116 19H8a2 2 0 01-1.89-1.357l-.346-.9z" />
                      </svg>
                    </div>
                    <span className="text-xs font-bold text-saffron uppercase tracking-wider">AI Guidance</span>
                  </div>
                  <p className="text-sm leading-relaxed text-slate-100">
                    {readiness?.readinessScore === 100
                      ? `Excellent! Your vault is fully compliant. You can proceed to apply immediately.`
                      : readiness?.readinessScore >= 50
                      ? `You're close! Upload ${readiness.missingDocs.slice(0, 2).join(" and ")} to reach full compliance.`
                      : `Your vault needs ${readiness?.missingDocs.length} more document${readiness?.missingDocs.length > 1 ? "s" : ""}. Start with ${readiness?.missingDocs[0] || "the listed documents"}.`}
                  </p>
                </div>
              </div>
            )}

            {/* 5. FAQs Tab */}
            {activeTab === "faqs" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 dark:text-slate-100 border-l-4 border-government-blue dark:border-indigo-400 pl-4">
                    Frequently Asked Questions
                  </h3>
                </div>
                <FAQAccordion faqs={scheme.faqs} />
              </div>
            )}

            {/* 6. Application Process Tab */}
            {activeTab === "process" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 dark:text-slate-100 border-l-4 border-india-green pl-4">
                    Application Process
                  </h3>
                </div>
                <div className="space-y-5">
                  {Array.isArray(scheme.steps) ? (
                    scheme.steps.map((step, idx) => (
                      <div key={idx} className="flex gap-5 items-start">
                        <div className="h-10 w-10 rounded-full bg-government-blue dark:bg-indigo-600 text-white font-bold flex items-center justify-center shrink-0 shadow-sm">
                          {idx + 1}
                        </div>
                        <div className="pt-2">
                          <p className="text-sm text-gray-700 dark:text-slate-300 leading-relaxed font-medium">{step}</p>
                        </div>
                      </div>
                    ))
                  ) : (
                    <p className="text-sm text-gray-500 dark:text-slate-400">No application guidelines listed.</p>
                  )}
                </div>
              </div>
            )}

            {/* 7. Official Website Tab */}
            {activeTab === "website" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 dark:text-slate-100 border-l-4 border-saffron pl-4">
                    Official Ministry Website
                  </h3>
                </div>
                <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 p-8 rounded-2xl text-center space-y-5 max-w-md mx-auto">
                  <div className="p-4 bg-government-blue/10 dark:bg-indigo-950/60 text-government-blue dark:text-indigo-400 rounded-full inline-block border border-government-blue/20 dark:border-indigo-900">
                    <ExternalLink className="h-8 w-8" />
                  </div>
                  <div className="space-y-2">
                    <p className="font-bold text-sm text-gray-800 dark:text-slate-100">You are about to exit SchemeBridge</p>
                    <p className="text-xs text-gray-600 dark:text-slate-400 leading-normal font-medium">
                      All official disbursements and compliance processing happens directly on the official government portal at:
                    </p>
                    <p className="text-sm text-government-blue dark:text-indigo-400 font-bold font-mono">{scheme.officialLink}</p>
                  </div>

                  <a
                    href={scheme.officialLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-2 bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 text-white text-sm font-bold px-6 py-3 rounded-xl transition shadow-sm"
                  >
                    <span>Visit Official Portal</span>
                    <ExternalLink className="h-4 w-4" />
                  </a>
                </div>
              </div>
            )}

          </div>

        </div>

        {/* Right Side: Sticky Actions Panel */}
        <div className="lg:col-span-1 lg:sticky lg:top-24 lg:self-start space-y-5">

          {/* Main Action Console Card */}
          <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
            <div className="bg-gradient-to-r from-government-blue to-government-blue-dark text-white px-6 py-5 border-b border-government-blue/20">
              <h3 className="font-bold text-sm uppercase tracking-widest">
                Application Console
              </h3>
              <p className="text-white/80 text-xs mt-1 leading-relaxed">
                Submit your application or save for future reference.
              </p>
            </div>

            <div className="p-6 space-y-5">

              {/* Eligibility Score */}
              <div>
                <div className="flex justify-between items-center text-xs font-bold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-2">
                  <span>Eligibility Score</span>
                  <span className="text-gray-800 dark:text-slate-100 font-bold">{evaluation?.matchScore}%</span>
                </div>
                <div className="h-2.5 bg-gray-200 dark:bg-slate-700 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${
                      evaluation?.matchScore === 100 
                        ? "bg-india-green" 
                        : evaluation?.matchScore >= 50 
                        ? "bg-saffron" 
                        : "bg-red-500"
                    }`}
                    style={{ width: `${evaluation?.matchScore}%` }}
                  />
                </div>
              </div>

              {/* Opportunity Score */}
              {recDetails && (
                <div>
                  <div className="flex justify-between items-center text-xs font-bold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-2">
                    <span>Opportunity Priority</span>
                    <span className="text-government-blue dark:text-indigo-400 font-bold">{recDetails.opportunityScore}%</span>
                  </div>
                  <div className="h-2.5 bg-gray-200 dark:bg-slate-700 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-government-blue dark:bg-indigo-500 rounded-full transition-all"
                      style={{ width: `${recDetails.opportunityScore}%` }}
                    />
                  </div>
                  <p className="text-xs text-gray-500 dark:text-slate-400 leading-relaxed mt-2 italic">
                    {recDetails.opportunityScoreExplanation}
                  </p>
                </div>
              )}

              {/* Deadline Status */}
              {recDetails?.deadlineDays != null && (
                <div className="flex justify-between items-center text-sm p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-xl">
                  <span className="text-red-800 dark:text-red-300 font-bold flex items-center gap-1.5">
                    <Calendar className="h-4 w-4" />
                    Application Deadline
                  </span>
                  <span className="text-red-700 dark:text-red-300 font-bold">
                    Ends in {recDetails.deadlineDays} Days
                  </span>
                </div>
              )}


              <div className="space-y-3 pt-3 border-t border-gray-100 dark:border-slate-800">
                {/* Apply / Continue / View Trigger */}
                {appAction.action === "FILE_APPLICATION" && !appliedSuccess ? (
                  <button
                    onClick={handleApply}
                    disabled={isApplying}
                    className="w-full py-3.5 rounded-xl text-sm font-bold shadow-sm transition flex items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-government-blue bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 text-white"
                  >
                    {isApplying ? (
                      <>
                        <span className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
                        <span>Processing...</span>
                      </>
                    ) : (
                      <>
                        <span>File Application</span>
                        <ArrowRight className="h-4 w-4" />
                      </>
                    )}
                  </button>
                ) : appAction.action === "CONTINUE_APPLICATION" ? (
                  <button
                    onClick={() => navigate(appAction.targetRoute)}
                    className="w-full py-3.5 rounded-xl text-sm font-bold shadow-sm transition flex items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-saffron bg-saffron hover:bg-saffron-dark text-government-blue-dark font-bold"
                  >
                    <span>Continue Application</span>
                    <ArrowRight className="h-4 w-4" />
                  </button>
                ) : (
                  <button
                    onClick={() => navigate(appAction.targetRoute || "/applications")}
                    className="w-full py-3.5 rounded-xl text-sm font-bold shadow-sm transition flex items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-government-blue bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 text-white"
                  >
                    <CheckCircle className="h-4 w-4" />
                    <span>View Application</span>
                  </button>
                )}

                {(alreadyApplied || appliedSuccess) && (
                  <Link
                    to="/applications"
                    className="text-xs font-semibold text-government-blue dark:text-indigo-400 hover:underline flex items-center justify-center gap-1 py-1"
                  >
                    <span>View in My Applications</span>
                    <ArrowRight className="h-3 w-3" />
                  </Link>
                )}

                {/* Save Trigger */}
                <button
                  onClick={() => saveScheme(scheme)}
                  disabled={alreadySaved || alreadyApplied}
                  className={`w-full py-3.5 rounded-xl text-sm font-bold border transition flex items-center justify-center gap-2 focus:outline-none ${
                    alreadySaved || alreadyApplied
                      ? "bg-gray-50 dark:bg-slate-800 border-gray-200 dark:border-slate-700 text-gray-400 dark:text-slate-500 cursor-not-allowed"
                      : "bg-white dark:bg-slate-800 border-gray-300 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-700 text-gray-700 dark:text-slate-200"
                  }`}
                >
                  {alreadySaved ? (
                    <>
                      <BookmarkCheck className="h-4 w-4 text-india-green" />
                      <span>Saved to Tracker</span>
                    </>
                  ) : (
                    <>
                      <BookmarkPlus className="h-4 w-4 text-gray-500 dark:text-slate-400" />
                      <span>Save for Reference</span>
                    </>
                  )}
                </button>

                {/* Ask AI Trigger */}
                <button
                  onClick={() => handleAskAI(`I need detailed information about ${scheme.name}. What is the benefits breakdown, and how should I start application process?`)}
                  className="w-full py-3.5 rounded-xl text-sm font-bold bg-government-blue/10 dark:bg-indigo-950/60 border border-government-blue/20 dark:border-indigo-900/60 hover:bg-government-blue/20 dark:hover:bg-indigo-900/80 text-government-blue dark:text-indigo-300 transition flex items-center justify-center gap-2"
                >
                  <MessageSquare className="h-4 w-4" />
                  <span>Ask AI Assistant</span>
                </button>
              </div>

              <div className="text-xs text-gray-500 dark:text-slate-400 text-center leading-relaxed pt-1 select-none">
                Official government scheme details. Secure and encrypted.
              </div>

            </div>
          </div>

        </div>

      </div>

      {/* Floating Chat widget panel */}
      <SchemeAIChatWidget
        isOpen={aiChatOpen}
        initialQuery={aiInitialQuery}
        pageContext={window.location.pathname}
        contextData={{
          page: "schemeDetails",
          schemeName: scheme?.name || "",
          readinessScore: readiness?.readinessScore ?? 0,
          missingDocs: readiness?.missingDocs ?? [],
          availableDocs: readiness?.availableDocs ?? [],
        }}
        onClose={() => {
          setAiChatOpen(false);
          setAiInitialQuery("");
        }}
      />

    </div>
  );
}
