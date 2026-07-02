import React, { useState, useMemo } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useApp } from "@context/AppContext";
import { checkEligibility } from "@utils/eligibilityEngine";
import { getDocReadinessForScheme } from "@utils/documentReadiness";
import { getSchemeRecommendationDetails } from "@data/mockRecommendations";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import {
  ArrowLeft,
  CheckCircle,
  XCircle,
  AlertCircle,
  ArrowRight,
  BookmarkPlus,
  BookmarkCheck,
  ChevronDown,
  ChevronUp,
  ShieldCheck,
  Clock,
  Building2,
  ExternalLink,
  MessageSquare
} from "lucide-react";

function FAQAccordion({ faqs }) {
  const { t } = useApp();
  const [openIdx, setOpenIdx] = useState(null);
  if (!Array.isArray(faqs) || faqs.length === 0) {
    return (
      <p className="text-xs text-slate-500 p-4 bg-slate-50 rounded-xl">
        {t("det_no_faqs") || "No FAQs available for this scheme."}
      </p>
    );
  }
  return (
    <div className="space-y-2">
      {faqs.map((faq, idx) => (
        <div key={idx} className="border border-slate-200 rounded-xl overflow-hidden bg-white shadow-xs">
          <button
            className="w-full flex items-start justify-between gap-3 p-4 text-left hover:bg-slate-50/50 transition"
            onClick={() => setOpenIdx(openIdx === idx ? null : idx)}
          >
            <div className="flex items-start gap-2">
              <span className="text-indigo-650 text-indigo-600 font-black text-xs mt-0.5">Q.</span>
              <span className="text-xs font-bold text-slate-800 leading-snug">{faq.question}</span>
            </div>
            {openIdx === idx ? (
              <ChevronUp className="h-4 w-4 text-slate-400 shrink-0 mt-0.5" />
            ) : (
              <ChevronDown className="h-4 w-4 text-slate-400 shrink-0 mt-0.5" />
            )}
          </button>
          {openIdx === idx && (
            <div className="px-4 pb-4 text-xs text-slate-500 leading-relaxed border-t border-slate-100 pt-3 bg-slate-50/30">
              {faq.answer}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

export default function SchemeDetails() {
  const id = useParams().id;
  const navigate = useNavigate();
  const { schemes, applyToScheme, hasApplied, saveScheme, isSaved, profile, documents, t } = useApp();

  const [activeTab, setActiveTab] = useState("overview");
  const [isApplying, setIsApplying] = useState(false);
  const [appliedSuccess, setAppliedSuccess] = useState(false);

  // AI chat states
  const [aiChatOpen, setAiChatOpen] = useState(false);
  const [aiInitialQuery, setAiInitialQuery] = useState("");

  const scheme = useMemo(() => schemes.find((s) => s.id === id), [schemes, id]);
  const alreadyApplied = scheme ? hasApplied(scheme.id) : false;
  const alreadySaved = scheme ? isSaved(scheme.id) : false;

  const evaluation = useMemo(() => scheme ? checkEligibility(profile, scheme, documents) : null, [scheme, profile, documents]);
  const readiness = useMemo(() => scheme ? getDocReadinessForScheme(scheme.requiredDocuments, documents) : null, [scheme, documents]);
  const recDetails = useMemo(() => scheme ? getSchemeRecommendationDetails(scheme.id, scheme.name) : null, [scheme]);

  if (!scheme) {
    return (
      <div className="max-w-md mx-auto my-16 text-center space-y-4">
        <h2 className="text-xl font-bold text-slate-800">{t("det_not_found") || "Scheme Not Found"}</h2>
        <p className="text-slate-500 text-xs">
          {t("det_not_found_desc") || "The requested scheme identifier does not correspond to an active government portal."}
        </p>
        <Link to="/recommendations" className="inline-flex items-center gap-1.5 text-indigo-600 font-semibold underline text-sm">
          <ArrowLeft className="h-4 w-4" />
          {t("det_back_recommendations") || "Back to Recommendations"}
        </Link>
      </div>
    );
  }

  const handleApply = () => {
    setIsApplying(true);
    setTimeout(() => {
      const success = applyToScheme(scheme);
      setIsApplying(false);
      if (success) {
        if (scheme.officialLink) {
          window.open(scheme.officialLink, "_blank", "noopener,noreferrer");
        }
        setAppliedSuccess(true);
        setTimeout(() => navigate("/tracker"), 1500);
      }
    }, 1200);
  };

  const handleAskAI = (queryText) => {
    setAiInitialQuery(queryText);
    setAiChatOpen(true);
  };

  const statusStyleMap = {
    eligible: { color: "text-emerald-700 bg-emerald-50 border-emerald-200", icon: CheckCircle },
    possibly_eligible: { color: "text-amber-700 bg-amber-50 border-amber-200", icon: AlertCircle },
    not_eligible: { color: "text-rose-700 bg-rose-50 border-rose-200", icon: XCircle },
  };
  const sStyle = evaluation ? statusStyleMap[evaluation.status] : statusStyleMap.not_eligible;
  const StatusIcon = sStyle.icon;

  const tabs = [
    { id: "overview", label: t("det_overview") || "Overview" },
    { id: "benefits", label: t("det_benefits") || "Benefits" },
    { id: "eligibility", label: t("det_eligibility") || "Eligibility" },
    { id: "documents", label: t("det_documents") || "Required Documents" },
    { id: "faqs", label: t("det_faqs") || "FAQs" },
    { id: "process", label: t("det_process") || "Application Process" },
    { id: "website", label: t("det_website") || "Official Website" },
  ];

  return (
    <div className="space-y-6">

      {/* Back breadcrumb */}
      <Link
        to="/recommendations"
        className="inline-flex items-center gap-1.5 text-slate-500 hover:text-slate-800 text-xs font-bold transition focus:outline-none focus:ring-2 focus:ring-indigo-500 rounded px-1"
      >
        <ArrowLeft className="h-4 w-4" />
        {t("det_back_matches") || "Back to Matches"}
      </Link>

      {/* Premium Hero Banner */}
      <div className="bg-slate-900 border border-slate-850 text-white p-6 sm:p-8 rounded-2xl shadow-md space-y-4 relative overflow-hidden">
        {/* Background Visual Grid */}
        <div className="absolute inset-0 opacity-5 bg-[linear-gradient(to_right,#808080_1px,transparent_1px),linear-gradient(to_bottom,#808080_1px,transparent_1px)] bg-[size:24px_24px] pointer-events-none" />

        <div className="relative space-y-2">
          <div className="flex flex-wrap gap-2 items-center">
            <span className="text-[9px] bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 px-2 py-0.5 rounded font-bold uppercase tracking-wider">
              {t(scheme.sourceType === "Central" ? "rec_type_central" : "rec_type_state") || `${scheme.sourceType} Government`}
            </span>
            <span className="text-[9px] bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 px-2 py-0.5 rounded font-bold uppercase tracking-wider flex items-center gap-1">
              <ShieldCheck className="h-3 w-3" />
              {t("det_verified_govt_source") || "Verified Govt Source"}
            </span>
          </div>

          <h1 className="text-xl sm:text-2xl font-black tracking-tight leading-snug text-white">
            {scheme.name}
          </h1>

          <div className="flex flex-wrap gap-x-4 gap-y-2 text-[11px] text-slate-400 font-medium">
            <span className="flex items-center gap-1">
              <Building2 className="h-3.5 w-3.5 text-slate-500" />
              {scheme.ministry}
            </span>
            <span className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5 text-slate-500" />
              {t("det_last_updated") || "Last updated"}: {scheme.lastUpdated}
            </span>
          </div>
        </div>
      </div>

      {appliedSuccess && (
        <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs p-4 rounded-xl flex items-center gap-2 shadow-xs">
          <CheckCircle className="h-5 w-5 text-emerald-600 shrink-0" />
          <span>{t("det_app_submitted") || "Application file successfully submitted! Redirecting to tracker..."}</span>
        </div>
      )}

      {/* Main Grid Content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">

        {/* Left Side: Dynamic Tabbed Layout */}
        <div className="lg:col-span-2 space-y-6">

          {/* Tab Selection Bar */}
          <div className="flex bg-slate-200/50 p-1 rounded-2xl overflow-x-auto scrollbar-none shadow-inner border border-slate-200/20">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex-1 min-w-[100px] text-center px-4 py-2.5 rounded-xl text-xs font-extrabold whitespace-nowrap transition ${
                  activeTab === tab.id
                    ? "bg-white text-slate-900 shadow-xs border border-slate-200/20"
                    : "text-slate-500 hover:text-slate-905 hover:text-slate-900"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Tab Content Panel */}
          <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-xs min-h-[300px]">

            {/* 1. Overview Tab */}
            {activeTab === "overview" && (
              <div className="space-y-4">
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
                  {t("det_overview_title") || "Scheme Overview & Objectives"}
                </h3>
                <p className="text-xs text-slate-600 leading-relaxed font-medium">
                  {scheme.description}
                </p>
                <div className="bg-slate-50 border border-slate-100 p-4 rounded-xl space-y-3">
                  <h4 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{t("det_admin_context") || "Administrative Context"}</h4>
                  <div className="grid grid-cols-2 gap-4 text-xs">
                    <div>
                      <span className="text-slate-400 block font-semibold">{t("det_implemented_by") || "Implementing Agency"}</span>
                      <span className="font-bold text-slate-700 block mt-0.5">{scheme.department || "State Directorate"}</span>
                    </div>
                    <div>
                      <span className="text-slate-400 block font-semibold">{t("det_funding_authority") || "Funding Authority"}</span>
                      <span className="font-bold text-slate-700 block mt-0.5">{t(scheme.sourceType === "Central" ? "rec_type_central" : "rec_type_state") || `${scheme.sourceType} Government`}</span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 2. Benefits Tab */}
            {activeTab === "benefits" && (
              <div className="space-y-4">
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
                  {t("det_financial_welfare") || "Financial & Welfare Benefits"}
                </h3>
                <ul className="space-y-3">
                  {Array.isArray(scheme.benefits) ? (
                    scheme.benefits.map((benefit, idx) => (
                      <li key={idx} className="flex items-start gap-2.5 text-xs text-slate-700 leading-relaxed">
                        <CheckCircle className="h-4.5 w-4.5 text-indigo-500 shrink-0 mt-0.5" />
                        <span className="font-medium">{benefit}</span>
                      </li>
                    ))
                  ) : (
                    <li className="text-xs text-slate-500">{t("det_no_benefits") || "No benefits listed."}</li>
                  )}
                </ul>
              </div>
            )}

            {/* 3. Eligibility Tab */}
            {activeTab === "eligibility" && (
              <div className="space-y-5">
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
                  {t("det_eligibility_breakdown") || "Detailed Eligibility Breakdown"}
                </h3>

                <div className={`p-4 rounded-xl border flex items-start gap-3 ${sStyle.color}`}>
                  <StatusIcon className="h-5 w-5 shrink-0 mt-0.5" />
                  <div>
                    <h4 className="font-bold text-xs">
                      {evaluation?.status === "eligible" ? (t("det_you_qualify") || "You qualify for this scheme") : evaluation?.status === "possibly_eligible" ? (t("det_you_possibly_qualify") || "You possibly qualify") : (t("det_you_do_not_qualify") || "You do not qualify")}
                    </h4>
                    <p className="text-[10px] mt-0.5 opacity-90 leading-relaxed font-medium">
                      {t("det_based_on_criteria") || "Based on criteria verified via DigiLocker and your locked profile demographics."}
                    </p>
                  </div>
                </div>

                <div className="space-y-3">
                  {/* Criteria Match Details */}
                  {evaluation?.qualifyingReasons.length > 0 && (
                    <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-4 space-y-2">
                      <h4 className="text-[9px] font-bold text-emerald-800 uppercase tracking-widest">{t("det_satisfied_parameters") || "Satisfied Parameters"}</h4>
                      <ul className="space-y-1.5">
                        {evaluation.qualifyingReasons.map((reason, idx) => (
                          <li key={idx} className="flex items-start gap-2 text-xs text-emerald-700 font-medium">
                            <span className="text-emerald-500 font-black">✓</span>
                            <span>{reason}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {evaluation?.unmetConditions.length > 0 && (
                    <div className="bg-rose-50/50 border border-rose-100 rounded-xl p-4 space-y-2">
                      <h4 className="text-[9px] font-bold text-rose-800 uppercase tracking-widest">{t("det_unsatisfied_parameters") || "Unsatisfied Parameters"}</h4>
                      <ul className="space-y-1.5">
                        {evaluation.unmetConditions.map((reason, idx) => (
                          <li key={idx} className="flex items-start gap-2 text-xs text-rose-700 font-medium">
                            <span className="text-rose-500 font-black">✗</span>
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
              <div className="space-y-5">
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
                  {t("det_compliance_title") || "Document Vault Compliance"}
                </h3>

                {/* Readiness Meter */}
                <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-600">{t("det_doc_readiness") || "Document Readiness"}</span>
                    <span
                      className={`text-xs font-bold px-3 py-1 rounded-full border ${
                        readiness?.readinessScore === 100
                          ? "bg-emerald-100 text-emerald-700 border-emerald-200"
                          : readiness?.readinessScore >= 50
                          ? "bg-amber-100 text-amber-700 border-amber-200"
                          : "bg-rose-100 text-rose-700 border-rose-200"
                      }`}
                    >
                      {readiness?.readinessScore}% · {readiness?.readinessLabel}
                    </span>
                  </div>
                  <div className="h-2.5 bg-slate-200 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all ${
                        readiness?.readinessScore === 100
                          ? "bg-emerald-500"
                          : readiness?.readinessScore >= 50
                          ? "bg-amber-400"
                          : "bg-rose-500"
                      }`}
                      style={{ width: `${readiness?.readinessScore}%` }}
                    />
                  </div>
                  <p className="text-[11px] text-slate-400">
                    {t("det_vault_verified_count", { available: readiness?.totalAvailable, required: readiness?.totalRequired }) || `${readiness?.totalAvailable}/${readiness?.totalRequired} required documents found in your vault.`}
                  </p>
                </div>

                {/* Available / Missing Grid */}
                <div className="grid sm:grid-cols-2 gap-3">
                  {scheme.requiredDocuments.map((doc, idx) => {
                    const have = readiness?.availableDocs.includes(doc);
                    return (
                      <div
                        key={idx}
                        className={`flex items-center gap-3 p-3 rounded-xl border text-xs font-bold transition ${
                          have ? "bg-emerald-50/50 border-emerald-200 text-emerald-700" : "bg-rose-50/50 border-rose-200 text-rose-700"
                        }`}
                      >
                        {have ? (
                          <CheckCircle className="h-4 w-4 text-emerald-600 shrink-0" />
                        ) : (
                          <XCircle className="h-4 w-4 text-rose-500 shrink-0" />
                        )}
                        <span className="truncate pr-1">{doc}</span>
                        {have && (
                          <span className="ml-auto text-[8px] bg-emerald-100 border border-emerald-200 text-emerald-700 px-2 py-0.5 rounded font-black uppercase">
                            {t("det_vault_verified") || "Vault Verified"}
                          </span>
                        )}
                        {!have && (
                          <span className="ml-auto text-[8px] bg-rose-100 border border-rose-200 text-rose-700 px-2 py-0.5 rounded font-black uppercase">
                            {t("det_missing") || "Missing"}
                          </span>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Missing Docs CTA */}
                {readiness?.missingDocs.length > 0 && (
                  <div className="bg-amber-50 border border-amber-200 text-amber-800 p-4 rounded-xl text-xs space-y-1.5">
                    <p className="font-bold flex items-center gap-1">
                      <AlertCircle className="h-4 w-4 text-amber-600 shrink-0" />
                      {t("det_missing_files_detected") || "Missing Files Detected"} ({readiness.missingDocs.length})
                    </p>
                    <p className="text-amber-700 font-medium">
                      {t("det_please_upload") || "Please upload the missing files in the Document Vault to complete compliance checks before submitting."}
                    </p>
                    <div className="pt-1">
                      <Link to="/documents" className="text-amber-800 font-extrabold underline hover:text-amber-950">
                        {t("det_upload_files_now") || "Upload Files Now"}
                      </Link>
                    </div>
                  </div>
                )}

                {/* AI Guidance Box */}
                <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2">
                  <div className="flex items-center gap-2">
                    <div className="bg-indigo-500/20 p-1.5 rounded-lg">
                      <svg className="h-3.5 w-3.5 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.346.9A2 2 0 0116 19H8a2 2 0 01-1.89-1.357l-.346-.9z" /></svg>
                    </div>
                    <span className="text-[10px] font-extrabold text-indigo-300 uppercase tracking-wider">{t("det_ai_guidance") || "AI Guidance"}</span>
                  </div>
                  <p className="text-xs text-slate-300 leading-relaxed">
                    {readiness?.readinessScore === 100
                      ? `✅ Excellent! Your vault is fully compliant for ${scheme.name}. You can proceed to apply immediately. Linking via DigiLocker will ensure instant officer verification.`
                      : readiness?.readinessScore >= 50
                      ? `⚡ You're close! Upload ${readiness.missingDocs.slice(0, 2).join(" and ")} to reach full compliance. You can use DigiLocker to fetch these documents instantly from registered government issuers.`
                      : `📋 Your vault needs ${readiness?.missingDocs.length} more document${readiness?.missingDocs.length > 1 ? "s" : ""} for this scheme. Start with ${readiness?.missingDocs[0] || "the listed documents"} — it's accepted by most government departments.`}
                  </p>
                </div>
              </div>
            )}

            {/* 5. FAQs Tab */}
            {activeTab === "faqs" && (
              <div className="space-y-4">
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
                  {t("det_frequently_asked") || "Frequently Asked Questions"}
                </h3>
                <FAQAccordion faqs={scheme.faqs} />
              </div>
            )}

            {/* 6. Application Process Tab */}
            {activeTab === "process" && (
              <div className="space-y-4">
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
                  {t("det_efiling_steps") || "E-Filing Process Steps"}
                </h3>
                <div className="space-y-3 pl-2">
                  {Array.isArray(scheme.steps) ? (
                    scheme.steps.map((step, idx) => (
                      <div key={idx} className="flex gap-4 items-start text-xs">
                        <div className="h-6 w-6 rounded-full bg-slate-900 text-white font-bold flex items-center justify-center shrink-0 text-[10px] shadow-xs">
                          {idx + 1}
                        </div>
                        <p className="text-slate-600 leading-relaxed font-medium pt-0.5">{step}</p>
                      </div>
                    ))
                  ) : (
                    <p className="text-xs text-slate-500">{t("det_no_guidelines") || "No application guidelines listed."}</p>
                  )}
                </div>
              </div>
            )}

            {/* 7. Official Website Tab */}
            {activeTab === "website" && (
              <div className="space-y-5">
                <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
                  {t("det_redirect_title") || "Redirect to Official Ministry Site"}
                </h3>
                <div className="bg-slate-50 border border-slate-200 p-6 rounded-2xl text-center space-y-4 max-w-md mx-auto">
                  <div className="p-3 bg-indigo-50 text-indigo-700 rounded-full inline-block border border-indigo-100">
                    <ExternalLink className="h-6 w-6" />
                  </div>
                  <div className="space-y-1">
                    <p className="font-bold text-xs text-slate-800">{t("det_exit_warning") || "You are about to exit SchemeBridge"}</p>
                    <p className="text-[10px] text-slate-450 text-slate-400 leading-normal font-medium">
                      {t("det_exit_desc") || "All official disbursements and primary compliance is processed directly on the central department portal at:"}
                    </p>
                    <p className="text-xs text-indigo-700 font-bold font-mono tracking-tight">{scheme.officialLink}</p>
                  </div>

                  <a
                    href={scheme.officialLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold px-4 py-2.5 rounded-xl transition shadow-xs"
                  >
                    <span>{t("det_visit_external") || "Visit External Portal"}</span>
                    <ArrowRight className="h-3.5 w-3.5" />
                  </a>
                </div>
              </div>
            )}

          </div>

        </div>

        {/* Right Side: Sticky Actions Panel */}
        <div className="lg:col-span-1 lg:sticky lg:top-24 lg:self-start space-y-5">

          {/* Main Action Console Card */}
          <div className="bg-white border border-slate-200 rounded-2xl shadow-xs overflow-hidden">
            <div className="bg-slate-900 text-white px-5 py-4 border-b border-slate-800">
              <h3 className="font-black text-xs uppercase tracking-widest text-slate-300">
                {t("det_action_card_title") || "Application Action Card"}
              </h3>
              <p className="text-slate-400 text-[10px] mt-1 font-medium leading-normal">
                {t("det_action_card_desc") || "Submit this file or save to eligibility tracking dashboard."}
              </p>
            </div>

            <div className="p-5 space-y-4">

              {/* Match Score Indicator */}
              <div>
                <div className="flex justify-between items-center text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                  <span>{t("det_match_score") || "Match score"}</span>
                  <span className="text-slate-800 font-extrabold">{evaluation?.matchScore}%</span>
                </div>
                <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${
                      evaluation?.matchScore === 100 ? "bg-emerald-500" : evaluation?.matchScore >= 50 ? "bg-amber-400" : "bg-rose-500"
                    }`}
                    style={{ width: `${evaluation?.matchScore}%` }}
                  />
                </div>
              </div>

              {/* Opportunity Score Indicator */}
              {recDetails && (
                <div>
                  <div className="flex justify-between items-center text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                    <span>{t("det_opportunity_priority") || "Opportunity Priority"}</span>
                    <span className="text-indigo-700 font-extrabold">{recDetails.opportunityScore}%</span>
                  </div>
                  <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-indigo-600 rounded-full"
                      style={{ width: `${recDetails.opportunityScore}%` }}
                    />
                  </div>
                  <p className="text-[9px] text-slate-400 leading-normal mt-1 italic">
                    {recDetails.opportunityScoreExplanation}
                  </p>
                </div>
              )}

              {/* Deadline Status Display */}
              {recDetails && (
                <div className="flex justify-between items-center text-xs p-3 bg-rose-50/50 border border-rose-100 rounded-xl">
                  <span className="text-rose-800 font-bold">{t("det_filing_deadline") || "Filing Deadline"}</span>
                  <span className="text-rose-700 font-extrabold font-mono">
                    {t("det_ends_in", { days: recDetails.deadlineDays }) || `Ends in ${recDetails.deadlineDays} Days`}
                  </span>
                </div>
              )}

              <div className="space-y-2 pt-2 border-t border-slate-100">
                {/* Apply Trigger */}
                <button
                  onClick={handleApply}
                  disabled={isApplying || appliedSuccess || alreadyApplied}
                  className={`w-full py-3 rounded-xl text-xs font-bold shadow-xs transition flex items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-slate-900 ${
                    alreadyApplied || appliedSuccess
                      ? "bg-emerald-600 border border-emerald-700 text-white cursor-not-allowed"
                      : "bg-slate-900 hover:bg-slate-800 text-white"
                  }`}
                >
                  {isApplying ? (
                    <>
                      <span className="animate-spin rounded-full h-3.5 w-3.5 border-2 border-white border-t-transparent" />
                      <span>{t("det_filing_file") || "Filing File..."}</span>
                    </>
                  ) : alreadyApplied || appliedSuccess ? (
                    <>
                      <CheckCircle className="h-4 w-4 text-white" />
                      <span>{t("det_app_submitted_btn") || "Application Submitted"}</span>
                    </>
                  ) : (
                    <>
                      <span>{t("det_file_online") || "File Application Online"}</span>
                      <ArrowRight className="h-3.5 w-3.5" />
                    </>
                  )}
                </button>

                {/* Save Trigger */}
                <button
                  onClick={() => saveScheme(scheme)}
                  disabled={alreadySaved || alreadyApplied}
                  className={`w-full py-3 rounded-xl text-xs font-bold border transition flex items-center justify-center gap-2 focus:outline-none ${
                    alreadySaved || alreadyApplied
                      ? "bg-slate-50 border-slate-200 text-slate-400 cursor-not-allowed"
                      : "bg-white border-slate-350 hover:bg-slate-50 text-slate-700 border-slate-300"
                  }`}
                >
                  {alreadySaved ? (
                    <>
                      <BookmarkCheck className="h-4 w-4 text-emerald-600" />
                      <span>{t("det_saved_to_tracker") || "Saved to Tracker Dashboard"}</span>
                    </>
                  ) : (
                    <>
                      <BookmarkPlus className="h-4 w-4 text-slate-500" />
                      <span>{t("det_save_reference") || "Save for Reference"}</span>
                    </>
                  )}
                </button>

                {/* Ask AI Trigger */}
                <button
                  onClick={() => handleAskAI(`I need detailed information about ${scheme.name}. What is the benefits breakdown, and how should I start application process?`)}
                  className="w-full py-3 rounded-xl text-xs font-bold bg-indigo-50 border border-indigo-100 hover:bg-indigo-100 text-indigo-700 transition flex items-center justify-center gap-2"
                >
                  <MessageSquare className="h-4 w-4" />
                  <span>{t("det_ask_ai_assistant") || "Ask SchemeAI Assistant"}</span>
                </button>
              </div>

              <div className="text-[9px] text-slate-400 text-center leading-normal pt-1 select-none">
                {t("det_auto_verified") || "Auto-verified details via DigiLocker Central Node. Secure and encrypted."}
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
