import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useApp } from '@context/AppContext';
import { checkEligibility } from '@utils/eligibilityEngine';
import { getDocReadinessForScheme } from '@utils/documentReadiness';
import { usePageMeta } from '@utils/usePageMeta';
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle,
  XCircle,
  FileCheck,
  ShieldCheck,
  ClipboardCheck,
  Save,
  Send,
  Check,
  FileText,
  User,
  AlertCircle,
  ChevronRight,
  CircleCheck
} from 'lucide-react';

import { getSchemeById, getSchemeByCode, trackRecommendationEvent } from "@services/schemeService";
import { getSchemeDocumentChecklist } from "@services/applicationService";

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
    tags: data.tags || [],
    rawBackendScheme: data
  };
}

export default function ApplicationWizard() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { schemes = [], applyToScheme, hasApplied, profile, documents, addDocument } = useApp();

  const [loadedScheme, setLoadedScheme] = useState(null);
  const [loading, setLoading] = useState(true);
  const [canonicalChecklist, setCanonicalChecklist] = useState(null);

  useEffect(() => {
    let isMounted = true;
    setLoading(true);

    const cached = (schemes || []).find(
      (s) => s.id === id || s.schemeCode === id || s.slug === id || s.schemeCode?.toLowerCase() === id?.toLowerCase() || s.slug?.toLowerCase() === id?.toLowerCase()
    );
    if (cached) {
      setLoadedScheme(cached);
      setLoading(false);
      const effectiveCode = cached.schemeCode || cached.slug || cached.id;
      getSchemeDocumentChecklist(effectiveCode)
        .then((res) => { if (isMounted) setCanonicalChecklist(res?.data || res); })
        .catch(() => { if (isMounted) setCanonicalChecklist(null); });
      return;
    }

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
        console.error("Failed to load scheme for application wizard:", err);
        if (isMounted) setLoadedScheme(null);
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    fetchFromApi();
    return () => { isMounted = false; };
  }, [id, schemes]);

  const scheme = loadedScheme;
  usePageMeta(scheme ? `Apply: ${scheme.name}` : "Application Wizard", "Complete application steps to apply for scheme");

  const effectiveRequirements = useMemo(() => {
    if (canonicalChecklist?.items && canonicalChecklist.items.length > 0) {
      return canonicalChecklist.items;
    }
    return scheme?.requiredDocuments || [];
  }, [canonicalChecklist, scheme]);

  const alreadyApplied = scheme ? hasApplied(scheme.id) : false;
  const evaluation = useMemo(() => (scheme ? checkEligibility(profile, scheme, documents) : null), [scheme, profile, documents]);
  const readiness = useMemo(
    () => (scheme ? getDocReadinessForScheme(effectiveRequirements, documents) : null),
    [scheme, effectiveRequirements, documents]
  );

  const [currentStep, setCurrentStep] = useState(0);
  const [selectedDocs, setSelectedDocs] = useState([]);
  const [agreedDeclaration, setAgreedDeclaration] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isSavedDraft, setIsSavedDraft] = useState(false);

  const steps = [
    { id: "profile", name: "Profile Review", icon: User },
    { id: "documents", name: "Document Vault", icon: FileText },
    { id: "eligibility", name: "Eligibility Verification", icon: ShieldCheck },
    { id: "declaration", name: "Declaration & Review", icon: FileCheck },
    { id: "submit", name: "Submit Application", icon: Send },
  ];

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto py-12 px-4 space-y-6 animate-pulse">
        <div className="h-6 w-32 bg-slate-200 rounded-lg" />
        <div className="h-40 bg-slate-200 rounded-2xl" />
        <div className="h-64 bg-slate-200 rounded-2xl" />
      </div>
    );
  }

  if (!scheme) {
    return (
      <div className="max-w-md mx-auto my-16 text-center space-y-4">
        <h2 className="text-xl font-bold text-gray-800">Scheme Not Found</h2>
        <Link to="/recommendations" className="inline-flex items-center gap-1.5 text-government-blue font-semibold underline text-sm">
          <ArrowLeft className="h-4 w-4" />
          Back to Schemes
        </Link>
      </div>
    );
  }

  if (alreadyApplied && !isSavedDraft) {
    return (
      <div className="max-w-md mx-auto my-16 text-center space-y-4">
        <CheckCircle className="h-16 w-16 text-india-green mx-auto" />
        <h2 className="text-xl font-bold text-gray-800">Application Already Submitted</h2>
        <p className="text-gray-500 text-sm">You've already applied to this scheme.</p>
        <Link to="/tracker" className="inline-flex items-center gap-1.5 text-government-blue font-semibold underline text-sm">
          View in Tracker
        </Link>
      </div>
    );
  }

  const handleNext = () => {
    if (currentStep < steps.length - 1) {
      setCurrentStep(currentStep + 1);
    }
  };

  const handleBack = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleSaveDraft = () => {
    setIsSavedDraft(true);
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    try {
      const success = await applyToScheme(scheme);
      setIsSubmitting(false);
      if (success) {
        setIsSubmitted(true);
        const effectiveCode = scheme?.code || scheme?.schemeCode || scheme?.id;
        const sessionId = (typeof window !== "undefined" && window.sessionStorage)
          ? window.sessionStorage.getItem("sb_telemetry_session") || ("sess_" + Date.now())
          : "sess_" + Date.now();

        trackRecommendationEvent({
          schemeCode: effectiveCode,
          eventType: "SCHEME_APPLIED",
          recommendationRank: 1,
          recommendationScore: evaluation?.matchScore ? evaluation.matchScore / 100.0 : 0.8,
          sessionId,
          metadata: { surface: "application_wizard_submit" }
        });

        trackRecommendationEvent({
          schemeCode: effectiveCode,
          eventType: "APPLICATION_COMPLETED",
          recommendationRank: 1,
          recommendationScore: evaluation?.matchScore ? evaluation.matchScore / 100.0 : 0.8,
          sessionId,
          metadata: { surface: "application_wizard_completed" }
        });

        setTimeout(() => navigate('/applications'), 2000);
      }
    } catch (err) {
      console.error("Submission failed:", err);
      setIsSubmitting(false);
    }
  };

  const toggleDoc = (docName) => {
    setSelectedDocs(prev =>
      prev.includes(docName)
        ? prev.filter(d => d !== docName)
        : [...prev, docName]
    );
  };

  return (
    <div className="max-w-4xl mx-auto my-8">
      <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl shadow-sm p-6 sm:p-8 space-y-8">
        <div className="flex items-center gap-3 mb-2">
          <Link
            to={`/scheme/${scheme.id}`} className="text-gray-400 dark:text-slate-500 hover:text-gray-700 dark:hover:text-slate-300">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-lg font-bold text-gray-900 dark:text-slate-100">Apply for {scheme.name}</h1>
            <p className="text-xs text-gray-500 dark:text-slate-400">Multi-step application process</p>
          </div>
        </div>

        <div className="relative">
          <div className="flex items-center justify-between">
          {steps.map((step, idx) => {
            const StepIcon = step.icon;
            return (
              <div key={step.id} className="flex flex-col items-center gap-2">
                <div
                className={`w-10 h-10 rounded-full flex items-center justify-center border-2 font-bold text-xs transition-all ${
                  idx < currentStep
                    ? 'bg-india-green border-india-green text-white'
                    : idx === currentStep
                    ? 'bg-government-blue dark:bg-indigo-600 border-government-blue dark:border-indigo-500 text-white ring-4 ring-government-blue/10 dark:ring-indigo-500/20'
                    : 'bg-white dark:bg-slate-800 border-gray-300 dark:border-slate-700 text-gray-500 dark:text-slate-400'
                }`}
                >
                  {idx < currentStep ? <Check className="h-4 w-4" /> : <StepIcon className="h-4 w-4" />}
                </div>
                <span className={`text-[10px] font-bold uppercase tracking-wider text-gray-700 dark:text-slate-300 whitespace-nowrap`}>{step.name}</span>
              </div>
            );
          })}
          </div>
          <div className="absolute top-5 left-10 right-10 h-0.5 bg-gray-200 dark:bg-slate-700 -z-10">
            <div
              className="h-full bg-government-blue dark:bg-indigo-500 transition-all" style={{ width: `${(currentStep / (steps.length - 1)) * 100}%` }}
            />
          </div>
        </div>

        <div className="mt-8">
          {currentStep === 0 && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-gray-900 dark:text-slate-100 uppercase tracking-widest border-l-4 border-government-blue dark:border-indigo-400 pl-4">Step 1: Review Your Profile</h3>
              <div className="grid sm:grid-cols-2 gap-4">
                <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl p-4">
                  <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Name</p>
                  <p className="text-sm font-bold text-gray-800 dark:text-slate-100">{profile.name}</p>
                </div>
                <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl p-4">
                  <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Annual Income</p>
                  <p className="text-sm font-bold text-gray-800 dark:text-slate-100">₹{profile.annualIncome.toLocaleString()}</p>
                </div>
                <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl p-4">
                  <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Occupation</p>
                  <p className="text-sm font-bold text-gray-800 dark:text-slate-100">{profile.occupation}</p>
                </div>
                <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl p-4">
                  <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">State</p>
                  <p className="text-sm font-bold text-gray-800 dark:text-slate-100">{profile.state}</p>
                </div>
                <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl p-4">
                  <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Category</p>
                  <p className="text-sm font-bold text-gray-800 dark:text-slate-100">{profile.caste}</p>
                </div>
                <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl p-4">
                  <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Age</p>
                  <p className="text-sm font-bold text-gray-800 dark:text-slate-100">{profile.age}</p>
                </div>
              </div>
              <p className="text-[11px] text-gray-500 dark:text-slate-400 text-center">
                To update your profile, go to Citizen Dashboard
              </p>
            </div>
          )}

          {currentStep === 1 && (
            <div className="space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h3 className="text-sm font-bold text-gray-900 dark:text-slate-100 uppercase tracking-widest border-l-4 border-india-green pl-4">Step 2: Select Required Documents</h3>
                  <p className="text-xs text-gray-500 dark:text-slate-400 mt-0.5">Select the documents you want to attach to this application from your verified vault</p>
                </div>
              </div>

              {/* Authoritative Readiness Indicator matching Scheme Details */}
              <div className="p-4 rounded-xl border bg-gray-50/80 dark:bg-slate-800/80 border-gray-200 dark:border-slate-700 space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-gray-700 dark:text-slate-300">Document Readiness</span>
                  <span className={`px-2 py-0.5 rounded-full font-bold uppercase tracking-wider text-[10px] border ${
                    readiness?.readinessScore === 100
                      ? "bg-india-green/10 text-india-green border-india-green/20"
                      : readiness?.readinessScore >= 50
                      ? "bg-saffron/10 text-saffron-dark border-saffron/20"
                      : "bg-red-50 text-red-700 border-red-200"
                  }`}>
                    {readiness?.readinessScore}% · {readiness?.readinessLabel}
                  </span>
                </div>
                <div className="h-2.5 bg-gray-200 dark:bg-slate-700 rounded-full overflow-hidden">
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
                <p className="text-[11px] text-gray-500 dark:text-slate-400">
                  {effectiveRequirements.length} official requirement{effectiveRequirements.length !== 1 ? 's' : ''} identified.
                  {readiness?.readinessScore === 100 && " All mandatory requirements satisfied."}
                </p>
              </div>

              <div className="grid gap-3">
                {effectiveRequirements.map((item, idx) => {
                  const docName = typeof item === 'string'
                    ? item
                    : item.documentName || item.officialDocumentName || item.name || 'Official Document';
                  
                  const isOneOf = typeof item === 'object' && (
                    item.alternativeGroupType === "ONE_OF" ||
                    item.type === "ONE_OF" ||
                    (Array.isArray(item.alternatives) && item.alternatives.length > 0) ||
                    (Array.isArray(item.options) && item.options.length > 0)
                  );
                  
                  const alternatives = isOneOf
                    ? (item.alternatives || item.options || [])
                    : [];

                  // Check if this requirement is satisfied by checking vault/readiness
                  const isSatisfiedInVault = readiness?.availableDocs?.some(d =>
                    d.toLowerCase().includes(docName.toLowerCase()) ||
                    docName.toLowerCase().includes(d.toLowerCase()) ||
                    alternatives.some(alt => d.toLowerCase().includes(alt.toLowerCase()) || alt.toLowerCase().includes(d.toLowerCase()))
                  );

                  // Check if user has selected this doc or any of its alternatives
                  const isRequirementSelected = selectedDocs.includes(docName) ||
                    alternatives.some(alt => selectedDocs.includes(alt));

                  if (isOneOf && alternatives.length > 0) {
                    return (
                      <div
                        key={idx}
                        className={`p-4 rounded-xl border transition-all space-y-3 ${
                          isSatisfiedInVault
                            ? 'bg-blue-50/40 dark:bg-slate-800 border-government-blue/30 dark:border-indigo-800'
                            : 'bg-red-50/50 dark:bg-red-950/20 border-red-200 dark:border-red-900'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex items-center gap-2">
                            <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 border-2 ${
                              isRequirementSelected || isSatisfiedInVault
                                ? 'bg-india-green border-india-green text-white'
                                : 'border-red-300 text-red-400'
                            }`}>
                              {isRequirementSelected || isSatisfiedInVault ? <Check className="h-3 w-3" /> : <XCircle className="h-3 w-3" />}
                            </div>
                            <div>
                              <p className="text-sm font-bold text-gray-800 dark:text-slate-100">{docName}</p>
                              <p className="text-[11px] text-government-blue dark:text-indigo-400 font-semibold">
                                Alternative Group: Choose any ONE valid document below
                              </p>
                            </div>
                          </div>
                          <span className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase bg-government-blue/10 dark:bg-indigo-900/60 text-government-blue dark:text-indigo-300 border border-government-blue/20">
                            ONE OF
                          </span>
                        </div>

                        {/* List alternatives as selectable options */}
                        <div className="grid gap-2 pl-8">
                          {alternatives.map((alt, aIdx) => {
                            const altAvailable = (documents || []).some(d =>
                              (d.name || d.documentName || d.type || '').toLowerCase().includes(alt.toLowerCase()) ||
                              alt.toLowerCase().includes((d.name || d.documentName || d.type || '').toLowerCase())
                            );
                            const altSelected = selectedDocs.includes(alt);

                            return (
                              <div
                                key={aIdx}
                                onClick={() => altAvailable && toggleDoc(alt)}
                                className={`flex items-center justify-between p-2.5 rounded-lg border text-xs cursor-pointer transition ${
                                  !altAvailable
                                    ? 'bg-gray-50/60 dark:bg-slate-900/40 border-gray-200 dark:border-slate-800 opacity-60'
                                    : altSelected
                                    ? 'bg-india-green/10 border-india-green text-india-green-dark font-bold dark:bg-emerald-950/60'
                                    : 'bg-white dark:bg-slate-800 border-gray-200 dark:border-slate-700 hover:border-gray-300'
                                }`}
                              >
                                <div className="flex items-center gap-2">
                                  <div className={`w-4 h-4 rounded border flex items-center justify-center ${
                                    altSelected ? 'bg-india-green border-india-green text-white' : 'border-gray-400'
                                  }`}>
                                    {altSelected && <Check className="h-2.5 w-2.5" />}
                                  </div>
                                  <span className={altAvailable ? 'text-gray-800 dark:text-slate-200' : 'text-gray-400 dark:text-slate-500'}>
                                    {alt}
                                  </span>
                                </div>
                                <span className={`text-[10px] font-medium ${altAvailable ? 'text-india-green' : 'text-red-500'}`}>
                                  {altAvailable ? (altSelected ? 'Attached' : 'Available in Vault') : 'Missing from Vault'}
                                </span>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    );
                  }

                  // Single document requirement
                  const available = isSatisfiedInVault;
                  const isSelected = selectedDocs.includes(docName);

                  return (
                    <div
                      key={idx}
                      onClick={() => available && toggleDoc(docName)}
                      className={`flex items-center gap-3 p-4 rounded-xl border transition-all cursor-pointer ${
                        !available
                          ? 'bg-red-50 dark:bg-red-950/40 border-red-200 dark:border-red-900 opacity-60'
                          : isSelected
                          ? 'bg-india-green/5 dark:bg-emerald-950/40 border-india-green/20 dark:border-emerald-800 cursor-pointer'
                          : 'bg-white dark:bg-slate-800 border-gray-200 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-700/60'
                      }`}
                    >
                      <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 border-2 ${
                          !available
                            ? 'border-red-300 dark:border-red-700 text-red-400'
                            : isSelected
                            ? 'bg-india-green border-india-green text-white'
                            : 'border-gray-300 dark:border-slate-600'
                        }`}>
                        {!available ? <XCircle className="h-3 w-3" /> : isSelected && <Check className="h-3 w-3" />}
                      </div>
                      <div className="flex-1">
                        <p className={`text-sm font-bold ${
                          !available ? 'text-red-700 dark:text-red-300' : 'text-gray-800 dark:text-slate-100'
                        }`}>{docName}</p>
                        {!available && <p className="text-[10px] text-red-500 dark:text-red-400">Missing from vault</p>}
                        {available && !isSelected && <p className="text-[10px] text-gray-400 dark:text-slate-500">Click to attach</p>}
                        {available && isSelected && <p className="text-[10px] text-india-green">Selected</p>}
                      </div>
                      {available && <FileText className={`h-4 w-4 ${!isSelected ? 'text-gray-300 dark:text-slate-600' : 'text-india-green'}`} />}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {currentStep === 2 && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-gray-900 dark:text-slate-100 uppercase tracking-widest border-l-4 border-saffron pl-4">Step 3: Confirm Eligibility</h3>
              <div className={`p-5 rounded-xl border flex items-start gap-3 ${
                evaluation?.status === 'eligible'
                  ? 'bg-india-green/5 dark:bg-emerald-950/40 border-india-green/20 dark:border-emerald-800'
                  : evaluation?.status === 'possibly_eligible'
                  ? 'bg-saffron/10 dark:bg-amber-950/40 border-saffron/20 dark:border-amber-800'
                  : 'bg-red-50 dark:bg-red-950/40 border-red-200 dark:border-red-900'
              }`}>
                {evaluation?.status === 'eligible'
                  ? <CheckCircle className="h-8 w-8 text-india-green" />
                  : evaluation?.status === 'possibly_eligible'
                  ? <AlertCircle className="h-8 w-8 text-saffron-dark dark:text-amber-400" />
                  : <XCircle className="h-8 w-8 text-red-600 dark:text-red-400" />}
                <div>
                  <h4 className="font-bold text-sm text-gray-900 dark:text-slate-100">
                    {evaluation?.status === 'eligible' ? 'You are Eligible' : evaluation?.status === 'possibly_eligible' ? 'Possibly Eligible' : 'Not Eligible'}
                  </h4>
                  <p className="text-xs text-gray-600 dark:text-slate-300">{evaluation?.status === 'eligible' ? 'Based on your profile, you meet all eligibility criteria.' : 'You may still apply, but final approval is subject to officer verification.'}</p>
                </div>
              </div>
              <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl p-4 space-y-2">
                <p className="text-xs font-bold text-gray-700 dark:text-slate-300">Match Score: <span className="text-government-blue dark:text-indigo-400">{evaluation?.matchScore}%</span></p>
              </div>
            </div>
          )}

          {currentStep === 3 && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-gray-900 dark:text-slate-100 uppercase tracking-widest border-l-4 border-government-blue dark:border-indigo-400 pl-4">Step 4: Declaration</h3>
              <div className="bg-gray-50 dark:bg-slate-800/70 border border-gray-200 dark:border-slate-700 rounded-xl p-5 text-sm leading-relaxed text-gray-700 dark:text-slate-300">
                <p className="mb-3 font-bold text-gray-800 dark:text-slate-100">I hereby declare that:</p>
                <ul className="space-y-2 ml-4 list-disc">
                  <li>The information provided in my application is true and correct to the best of my knowledge and belief.</li>
                  <li>I understand that any false or misleading information may result in rejection of my application.</li>
                  <li>I agree to provide additional documents or information if required by the authorities.</li>
                  <li>I consent to the verification of my documents through official government databases.</li>
                </ul>
              </div>
              <div className="flex items-start gap-3 mt-4">
                <input
                  type="checkbox"
                  checked={agreedDeclaration}
                  onChange={(e) => setAgreedDeclaration(e.target.checked)}
                  id="declaration"
                  className="h-4 w-4 text-government-blue focus:ring-government-blue rounded"
                />
                <label htmlFor="declaration" className="text-sm text-gray-700 dark:text-slate-300 font-medium cursor-pointer">
                  I agree to the declaration and terms
                </label>
              </div>
            </div>
          )}

          {currentStep === 4 && (
            <div className="text-center space-y-6">
              {isSubmitted ? (
                <div className="space-y-3">
                  <div className="w-20 h-20 bg-india-green/10 rounded-full flex items-center justify-center mx-auto">
                    <CheckCircle className="h-10 w-10 text-india-green" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-gray-900 dark:text-slate-100">Application Submitted!</h3>
                    <p className="text-xs text-gray-500 dark:text-slate-400">Redirecting to application tracker...</p>
                  </div>
                </div>
              ) : (
                <>
                  <div className="w-20 h-20 bg-government-blue/10 dark:bg-indigo-950/60 rounded-full flex items-center justify-center mx-auto">
                  <Send className="h-10 w-10 text-government-blue dark:text-indigo-400" />
                </div>
                <div className="space-y-2">
                  <h3 className="text-sm font-bold text-gray-900 dark:text-slate-100">Ready to Submit?</h3>
                  <p className="text-sm text-gray-500 dark:text-slate-400">Click the button below to submit your application to {scheme.name}</p>
                </div>
              </>
              )}
            </div>
          )}
        </div>

        <div className="flex justify-between pt-6 border-t border-gray-100 dark:border-slate-800 mt-8">
          {currentStep > 0 && (
            <button
              onClick={handleBack}
              className="flex items-center gap-2 px-5 py-2.5 border border-gray-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-700 dark:text-slate-200 rounded-xl text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700 transition"
            >
              <ArrowLeft className="h-4 w-4" />
              Back
            </button>
          )}
          <div className="flex-1" />
          {currentStep < 4 && (
            <button
              onClick={handleSaveDraft}
              className="flex items-center gap-2 px-5 py-2.5 border border-gray-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-gray-700 dark:text-slate-200 rounded-xl text-sm font-semibold hover:bg-gray-50 dark:hover:bg-slate-700 transition mr-2"
            >
              <Save className="h-4 w-4" />
              Save Draft
            </button>
          )}
          {currentStep < steps.length - 1 && (
            <button
              onClick={handleNext}
              className="flex items-center gap-2 bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition"
            >
              Next
              <ArrowRight className="h-4 w-4" />
            </button>
          )}
          {currentStep === steps.length - 1 && !isSubmitted && (
            <button
              onClick={handleSubmit}
              disabled={!agreedDeclaration || isSubmitting}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition
              ${
                !agreedDeclaration
                  ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  : 'bg-india-green hover:bg-india-green-dark text-white'
              }`}
            >
              {isSubmitting ? (
                <>
                  <div className="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
                  Submitting...
                </>
              ) : (
                <>
                  Submit Application
                  <ChevronRight className="h-4 w-4" />
                </>
              )}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
