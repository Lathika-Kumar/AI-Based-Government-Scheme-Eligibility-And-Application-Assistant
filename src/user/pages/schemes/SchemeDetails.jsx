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
  MessageSquare,
  FileText,
  Users,
  Briefcase,
  Calendar,
  IndianRupee
} from "lucide-react";

function FAQAccordion({ faqs }) {
  const [openIdx, setOpenIdx] = useState(null);
  if (!Array.isArray(faqs) || faqs.length === 0) {
    return (
      <p className="text-sm text-gray-500 p-4 bg-gray-50 rounded-xl">
        No FAQs available for this scheme.
      </p>
    );
  }
  return (
    <div className="space-y-3">
      {faqs.map((faq, idx) => (
        <div key={idx} className="border border-gray-200 rounded-xl overflow-hidden bg-white shadow-sm">
          <button
            className="w-full flex items-start justify-between gap-3 p-4 text-left hover:bg-gray-50 transition"
            onClick={() => setOpenIdx(openIdx === idx ? null : idx)}
          >
            <div className="flex items-start gap-2">
              <span className="text-government-blue font-bold text-sm mt-0.5">Q.</span>
              <span className="text-sm font-semibold text-gray-800 leading-snug">{faq.question}</span>
            </div>
            {openIdx === idx ? (
              <ChevronUp className="h-4 w-4 text-gray-400 shrink-0 mt-0.5" />
            ) : (
              <ChevronDown className="h-4 w-4 text-gray-400 shrink-0 mt-0.5" />
            )}
          </button>
          {openIdx === idx && (
            <div className="px-4 pb-4 text-sm text-gray-600 leading-relaxed border-t border-gray-100 pt-3 bg-gray-50/30">
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
  const { schemes, applyToScheme, hasApplied, saveScheme, isSaved, profile, documents } = useApp();

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
        <h2 className="text-xl font-bold text-gray-800">Scheme Not Found</h2>
        <p className="text-gray-500 text-sm">
          The requested scheme identifier does not correspond to an active government portal.
        </p>
        <Link to="/recommendations" className="inline-flex items-center gap-1.5 text-government-blue font-semibold underline text-sm">
          <ArrowLeft className="h-4 w-4" />
          Back to Recommendations
        </Link>
      </div>
    );
  }

  const handleApply = () => {
    navigate(`/scheme/${scheme.id}/apply`);
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
    { id: "process", label: "Process" },
    { id: "website", label: "Official Site" },
  ];

  return (
    <div className="space-y-6">

      {/* Back breadcrumb */}
      <Link
        to="/recommendations"
        className="inline-flex items-center gap-1.5 text-gray-600 hover:text-gray-900 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-government-blue rounded px-1"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to Recommendations
      </Link>

      {/* Government Hero Banner */}
      <div className="bg-gradient-to-r from-government-blue via-government-blue-light to-government-blue text-white p-6 sm:p-8 rounded-2xl shadow-lg space-y-4 relative overflow-hidden">
        {/* Decorative elements */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-saffron/10 rounded-full -mr-32 -mt-32" />
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-india-green/10 rounded-full -ml-24 -mb-24" />

        <div className="relative space-y-3">
          <div className="flex flex-wrap gap-2 items-center">
            <span className="text-xs bg-white/20 text-white border border-white/30 px-3 py-1 rounded-full font-bold uppercase tracking-wider">
              {scheme.sourceType === "Central" ? "Central Government" : "State Government"}
            </span>
            <span className="text-xs bg-india-green/20 text-india-green border border-india-green/30 px-3 py-1 rounded-full font-bold uppercase tracking-wider flex items-center gap-1">
              <ShieldCheck className="h-3.5 w-3.5" />
              Verified Government Source
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
          <span>Application successfully submitted! Redirecting to tracker...</span>
        </div>
      )}

      {/* Main Grid Content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">

        {/* Left Side: Dynamic Tabbed Layout */}
        <div className="lg:col-span-2 space-y-6">

          {/* Tab Selection Bar */}
          <div className="flex bg-gray-100 p-1 rounded-2xl overflow-x-auto scrollbar-none shadow-inner border border-gray-200">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex-1 min-w-[100px] text-center px-4 py-2.5 rounded-xl text-sm font-semibold whitespace-nowrap transition ${
                  activeTab === tab.id
                    ? "bg-white text-gray-900 shadow-sm border border-gray-200"
                    : "text-gray-600 hover:text-gray-900"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Tab Content Panel */}
          <div className="bg-white border border-gray-200 p-6 rounded-2xl shadow-sm min-h-[300px]">

            {/* 1. Overview Tab */}
            {activeTab === "overview" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 border-l-4 border-government-blue pl-4">
                    Scheme Overview
                  </h3>
                  <p className="text-sm text-gray-700 leading-relaxed mt-4">
                    {scheme.description}
                  </p>
                </div>

                <div className="grid sm:grid-cols-2 gap-4">
                  <div className="bg-gray-50 border border-gray-200 p-5 rounded-xl">
                    <div className="flex items-center gap-2 mb-3">
                      <Building2 className="h-5 w-5 text-government-blue" />
                      <h4 className="text-sm font-bold text-gray-900">Implementing Agency</h4>
                    </div>
                    <p className="text-gray-700 font-semibold">{scheme.department || "State Directorate"}</p>
                  </div>

                  <div className="bg-gray-50 border border-gray-200 p-5 rounded-xl">
                    <div className="flex items-center gap-2 mb-3">
                      <FileText className="h-5 w-5 text-india-green" />
                      <h4 className="text-sm font-bold text-gray-900">Funding Authority</h4>
                    </div>
                    <p className="text-gray-700 font-semibold">{scheme.sourceType === "Central" ? "Central Government" : "State Government"}</p>
                  </div>
                </div>

                {recDetails && (
                  <div className="bg-government-blue/5 border border-government-blue/20 p-5 rounded-xl">
                    <h4 className="text-sm font-bold text-government-blue mb-2">AI Recommendation</h4>
                    <p className="text-sm text-gray-700">{recDetails.benefitSummary}</p>
                  </div>
                )}
              </div>
            )}

            {/* 2. Benefits Tab */}
            {activeTab === "benefits" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 border-l-4 border-india-green pl-4">
                    Financial & Welfare Benefits
                  </h3>
                </div>

                <ul className="space-y-4">
                  {Array.isArray(scheme.benefits) ? (
                    scheme.benefits.map((benefit, idx) => (
                      <li key={idx} className="flex items-start gap-3 p-4 bg-gray-50 rounded-xl border border-gray-200">
                        <div className="bg-india-green/10 p-2 rounded-full shrink-0 mt-0.5">
                          <IndianRupee className="h-4 w-4 text-india-green" />
                        </div>
                        <span className="text-sm text-gray-700 leading-relaxed font-medium">{benefit}</span>
                      </li>
                    ))
                  ) : (
                    <li className="text-sm text-gray-500">No benefits listed.</li>
                  )}
                </ul>
              </div>
            )}

            {/* 3. Eligibility Tab */}
            {activeTab === "eligibility" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 border-l-4 border-saffron pl-4">
                    Eligibility Criteria
                  </h3>
                </div>

                <div className={`p-5 rounded-xl border flex items-start gap-3 ${sStyle.color}`}>
                  <StatusIcon className="h-6 w-6 shrink-0 mt-0.5" />
                  <div>
                    <h4 className="font-bold text-sm">
                      {evaluation?.status === "eligible" 
                        ? "You are Eligible" 
                        : evaluation?.status === "possibly_eligible" 
                        ? "You May Be Eligible" 
                        : "You Are Not Eligible"}
                    </h4>
                    <p className="text-xs mt-1 opacity-90 leading-relaxed font-medium">
                      Based on criteria verified via DigiLocker and your profile demographics.
                    </p>
                  </div>
                </div>

                <div className="space-y-4">
                  {/* Criteria Match Details */}
                  {evaluation?.qualifyingReasons.length > 0 && (
                    <div className="bg-india-green/5 border border-india-green/20 rounded-xl p-5 space-y-3">
                      <h4 className="text-xs font-bold text-india-green uppercase tracking-widest">Satisfied Parameters</h4>
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
                    <div className="bg-red-50 border border-red-200 rounded-xl p-5 space-y-3">
                      <h4 className="text-xs font-bold text-red-800 uppercase tracking-widest">Unsatisfied Parameters</h4>
                      <ul className="space-y-2">
                        {evaluation.unmetConditions.map((reason, idx) => (
                          <li key={idx} className="flex items-start gap-2 text-sm text-red-700 font-medium">
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
                <div>
                  <h3 className="text-lg font-bold text-gray-900 border-l-4 border-government-blue pl-4">
                    Required Documents
                  </h3>
                </div>

                {/* Readiness Meter */}
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-5 space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-bold text-gray-700">Document Readiness</span>
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
                  <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
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
                  <p className="text-xs text-gray-500">
                    {readiness?.totalAvailable} of {readiness?.totalRequired} required documents verified in your Document Vault.
                  </p>
                </div>

                {/* Document List */}
                <div className="space-y-3">
                  {scheme.requiredDocuments.map((doc, idx) => {
                    const have = readiness?.availableDocs.includes(doc);
                    return (
                      <div
                        key={idx}
                        className={`flex items-center gap-4 p-4 rounded-xl border transition ${
                          have ? "bg-india-green/5 border-india-green/20" : "bg-red-50 border-red-200"
                        }`}
                      >
                        {have ? (
                          <CheckCircle className="h-5 w-5 text-india-green shrink-0" />
                        ) : (
                          <XCircle className="h-5 w-5 text-red-500 shrink-0" />
                        )}
                        <span className="font-semibold text-gray-800 flex-1">{doc}</span>
                        {have && (
                          <span className="text-xs bg-india-green/10 border border-india-green/20 text-india-green px-3 py-1 rounded-full font-bold uppercase">
                            Verified
                          </span>
                        )}
                        {!have && (
                          <span className="text-xs bg-red-100 border border-red-200 text-red-700 px-3 py-1 rounded-full font-bold uppercase">
                            Missing
                          </span>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Missing Docs CTA */}
                {readiness?.missingDocs.length > 0 && (
                  <div className="bg-saffron/10 border border-saffron/20 text-saffron-dark p-5 rounded-xl text-sm space-y-2">
                    <p className="font-bold flex items-center gap-2">
                      <AlertCircle className="h-4 w-4 shrink-0" />
                      Missing Files Detected ({readiness.missingDocs.length})
                    </p>
                    <p className="text-saffron-dark/90 font-medium">
                      Please upload the missing files in the Document Vault to complete compliance checks before submitting.
                    </p>
                    <div className="pt-2">
                      <Link to="/documents" className="inline-flex items-center gap-1.5 text-government-blue font-bold hover:underline">
                        <FileText className="h-4 w-4" />
                        Upload Files Now
                      </Link>
                    </div>
                  </div>
                )}

                {/* AI Guidance Box */}
                <div className="bg-government-blue text-white rounded-xl p-5 space-y-3">
                  <div className="flex items-center gap-2">
                    <div className="bg-white/10 p-2 rounded-lg">
                      <svg className="h-4 w-4 text-saffron" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.346.9A2 2 0 0116 19H8a2 2 0 01-1.89-1.357l-.346-.9z" />
                      </svg>
                    </div>
                    <span className="text-xs font-bold text-saffron uppercase tracking-wider">AI Guidance</span>
                  </div>
                  <p className="text-sm leading-relaxed">
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
                  <h3 className="text-lg font-bold text-gray-900 border-l-4 border-government-blue pl-4">
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
                  <h3 className="text-lg font-bold text-gray-900 border-l-4 border-india-green pl-4">
                    Application Process
                  </h3>
                </div>
                <div className="space-y-5">
                  {Array.isArray(scheme.steps) ? (
                    scheme.steps.map((step, idx) => (
                      <div key={idx} className="flex gap-5 items-start">
                        <div className="h-10 w-10 rounded-full bg-government-blue text-white font-bold flex items-center justify-center shrink-0 shadow-sm">
                          {idx + 1}
                        </div>
                        <div className="pt-2">
                          <p className="text-sm text-gray-700 leading-relaxed font-medium">{step}</p>
                        </div>
                      </div>
                    ))
                  ) : (
                    <p className="text-sm text-gray-500">No application guidelines listed.</p>
                  )}
                </div>
              </div>
            )}

            {/* 7. Official Website Tab */}
            {activeTab === "website" && (
              <div className="space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 border-l-4 border-saffron pl-4">
                    Official Ministry Website
                  </h3>
                </div>
                <div className="bg-gray-50 border border-gray-200 p-8 rounded-2xl text-center space-y-5 max-w-md mx-auto">
                  <div className="p-4 bg-government-blue/10 text-government-blue rounded-full inline-block border border-government-blue/20">
                    <ExternalLink className="h-8 w-8" />
                  </div>
                  <div className="space-y-2">
                    <p className="font-bold text-sm text-gray-800">You are about to exit SchemeBridge</p>
                    <p className="text-xs text-gray-600 leading-normal font-medium">
                      All official disbursements and compliance processing happens directly on the official government portal at:
                    </p>
                    <p className="text-sm text-government-blue font-bold font-mono">{scheme.officialLink}</p>
                  </div>

                  <a
                    href={scheme.officialLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-2 bg-government-blue hover:bg-government-blue-dark text-white text-sm font-bold px-6 py-3 rounded-xl transition shadow-sm"
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
          <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">
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
                <div className="flex justify-between items-center text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                  <span>Eligibility Score</span>
                  <span className="text-gray-800 font-bold">{evaluation?.matchScore}%</span>
                </div>
                <div className="h-2.5 bg-gray-200 rounded-full overflow-hidden">
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
                  <div className="flex justify-between items-center text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                    <span>Opportunity Priority</span>
                    <span className="text-government-blue font-bold">{recDetails.opportunityScore}%</span>
                  </div>
                  <div className="h-2.5 bg-gray-200 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-government-blue rounded-full transition-all"
                      style={{ width: `${recDetails.opportunityScore}%` }}
                    />
                  </div>
                  <p className="text-xs text-gray-500 leading-relaxed mt-2 italic">
                    {recDetails.opportunityScoreExplanation}
                  </p>
                </div>
              )}

              {/* Deadline Status */}
              {recDetails && (
                <div className="flex justify-between items-center text-sm p-4 bg-red-50 border border-red-200 rounded-xl">
                  <span className="text-red-800 font-bold flex items-center gap-1.5">
                    <Calendar className="h-4 w-4" />
                    Application Deadline
                  </span>
                  <span className="text-red-700 font-bold">
                    Ends in {recDetails.deadlineDays} Days
                  </span>
                </div>
              )}

              <div className="space-y-3 pt-3 border-t border-gray-100">
                {/* Apply Trigger */}
                <button
                  onClick={handleApply}
                  disabled={isApplying || appliedSuccess || alreadyApplied}
                  className={`w-full py-3.5 rounded-xl text-sm font-bold shadow-sm transition flex items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-government-blue ${
                    alreadyApplied || appliedSuccess
                      ? "bg-india-green border border-india-green/20 text-white cursor-not-allowed"
                      : "bg-government-blue hover:bg-government-blue-dark text-white"
                  }`}
                >
                  {isApplying ? (
                    <>
                      <span className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
                      <span>Processing...</span>
                    </>
                  ) : alreadyApplied || appliedSuccess ? (
                    <>
                      <CheckCircle className="h-4 w-4" />
                      <span>Application Submitted</span>
                    </>
                  ) : (
                    <>
                      <span>File Application</span>
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>

                {/* Save Trigger */}
                <button
                  onClick={() => saveScheme(scheme)}
                  disabled={alreadySaved || alreadyApplied}
                  className={`w-full py-3.5 rounded-xl text-sm font-bold border transition flex items-center justify-center gap-2 focus:outline-none ${
                    alreadySaved || alreadyApplied
                      ? "bg-gray-50 border-gray-200 text-gray-400 cursor-not-allowed"
                      : "bg-white border-gray-300 hover:bg-gray-50 text-gray-700"
                  }`}
                >
                  {alreadySaved ? (
                    <>
                      <BookmarkCheck className="h-4 w-4 text-india-green" />
                      <span>Saved to Tracker</span>
                    </>
                  ) : (
                    <>
                      <BookmarkPlus className="h-4 w-4 text-gray-500" />
                      <span>Save for Reference</span>
                    </>
                  )}
                </button>

                {/* Ask AI Trigger */}
                <button
                  onClick={() => handleAskAI(`I need detailed information about ${scheme.name}. What is the benefits breakdown, and how should I start application process?`)}
                  className="w-full py-3.5 rounded-xl text-sm font-bold bg-government-blue/10 border border-government-blue/20 hover:bg-government-blue/20 text-government-blue transition flex items-center justify-center gap-2"
                >
                  <MessageSquare className="h-4 w-4" />
                  <span>Ask AI Assistant</span>
                </button>
              </div>

              <div className="text-xs text-gray-500 text-center leading-relaxed pt-1 select-none">
                Auto-verified details via DigiLocker. Secure and encrypted.
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
