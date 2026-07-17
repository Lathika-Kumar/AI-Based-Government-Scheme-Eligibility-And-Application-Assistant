import React, { useState, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useApp } from '@context/AppContext';
import { checkEligibility } from '@utils/eligibilityEngine';
import { getDocReadinessForScheme } from '@utils/documentReadiness';
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

export default function ApplicationWizard() {
  const id = useParams().id;
  const navigate = useNavigate();
  const { schemes, applyToScheme, hasApplied, profile, documents } = useApp();
  const scheme = useMemo(() => schemes.find((s) => s.id === id), [schemes, id]);
  const alreadyApplied = scheme ? hasApplied(scheme.id) : false;
  const evaluation = useMemo(() => scheme ? checkEligibility(profile, scheme, documents) : null, [scheme, profile, documents]);
  const readiness = useMemo(() => scheme ? getDocReadinessForScheme(scheme.requiredDocuments, documents) : null, [scheme, documents]);

  const [currentStep, setCurrentStep] = useState(0);
  const [selectedDocs, setSelectedDocs] = useState([]);
  const [agreedDeclaration, setAgreedDeclaration] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isSavedDraft, setIsSavedDraft] = useState(false);

  const steps = [
    { id: 'profile', name: 'Review Profile', icon: User },
    { id: 'documents', name: 'Select Documents', icon: FileText },
    { id: 'eligibility', name: 'Confirm Eligibility', icon: ShieldCheck },
    { id: 'declaration', name: 'Declaration', icon: FileCheck },
    { id: 'submit', name: 'Submit', icon: Send },
  ];

  if (!scheme) {
    return (
      <div className="max-w-md mx-auto my-16 text-center space-y-4">
        <h2 className="text-xl font-bold text-gray-800">Scheme Not Found</h2>
        <Link to="/recommendations" className="inline-flex items-center gap-1.5 text-government-blue font-semibold underline text-sm">
          <ArrowLeft className="h-4 w-4" />
          Back to Recommendations
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

  const handleSubmit = () => {
    setIsSubmitting(true);
    setTimeout(() => {
      const success = applyToScheme(scheme);
      setIsSubmitting(false);
      if (success) {
        setIsSubmitted(true);
        setTimeout(() => navigate('/tracker'), 2000);
      }
    }, 1500);
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
      <div className="bg-white border border-gray-200 rounded-2xl shadow-sm p-6 sm:p-8 space-y-8">
        <div className="flex items-center gap-3 mb-2">
          <Link
            to={`/scheme/${scheme.id}`} className="text-gray-400 hover:text-gray-700">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-lg font-bold text-gray-900">Apply for {scheme.name}</h1>
            <p className="text-xs text-gray-500">Multi-step application process</p>
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
                    ? 'bg-government-blue border-government-blue text-white ring-4 ring-government-blue/10'
                    : 'bg-white border-gray-300 text-gray-500'
                }`}
                >
                  {idx < currentStep ? <Check className="h-4 w-4" /> : <StepIcon className="h-4 w-4" />}
                </div>
                <span className={`text-[10px] font-bold uppercase tracking-wider text-gray-700 whitespace-nowrap`}>{step.name}</span>
              </div>
            );
          })}
          </div>
          <div className="absolute top-5 left-10 right-10 h-0.5 bg-gray-200 -z-10">
            <div
              className="h-full bg-government-blue transition-all" style={{ width: `${(currentStep / (steps.length - 1)) * 100}%` }}
            />
          </div>
        </div>

        <div className="mt-8">
          {currentStep === 0 && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-gray-900 uppercase tracking-widest border-l-4 border-government-blue pl-4">Step 1: Review Your Profile</h3>
              <div className="grid sm:grid-cols-2 gap-4">
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                  <p className="text-xs text-gray-500 mb-1">Name</p>
                  <p className="text-sm font-bold text-gray-800">{profile.name}</p>
                </div>
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                  <p className="text-xs text-gray-500 mb-1">Annual Income</p>
                  <p className="text-sm font-bold text-gray-800">₹{profile.annualIncome.toLocaleString()}</p>
                </div>
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                  <p className="text-xs text-gray-500 mb-1">Occupation</p>
                  <p className="text-sm font-bold text-gray-800">{profile.occupation}</p>
                </div>
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                  <p className="text-xs text-gray-500 mb-1">State</p>
                  <p className="text-sm font-bold text-gray-800">{profile.state}</p>
                </div>
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                  <p className="text-xs text-gray-500 mb-1">Category</p>
                  <p className="text-sm font-bold text-gray-800">{profile.caste}</p>
                </div>
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-4">
                  <p className="text-xs text-gray-500 mb-1">Age</p>
                  <p className="text-sm font-bold text-gray-800">{profile.age}</p>
                </div>
              </div>
              <p className="text-[11px] text-gray-500 text-center">
                To update your profile, go to Citizen Dashboard
              </p>
            </div>
          )}

          {currentStep === 1 && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-gray-900 uppercase tracking-widest border-l-4 border-india-green pl-4">Step 2: Select Required Documents</h3>
              <p className="text-sm text-gray-500">Select the documents you want to attach to this application</p>
              <div className="grid gap-3">
                {scheme.requiredDocuments.map((doc, idx) => {
                  const available = readiness?.availableDocs.includes(doc);
                  const isSelected = selectedDocs.includes(doc);
                  return (
                    <div
                      key={idx}
                      onClick={() => available && toggleDoc(doc)}
                      className={`flex items-center gap-3 p-4 rounded-xl border transition-all cursor-pointer ${
                        !available
                          ? 'bg-red-50 border-red-200 opacity-60'
                          : isSelected
                          ? 'bg-india-green/5 border-india-green/20 cursor-pointer'
                          : 'bg-white border-gray-200 hover:bg-gray-50'
                      }`}
                    >
                      <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 border-2 ${
                          !available
                            ? 'border-red-300 text-red-400'
                            : isSelected
                            ? 'bg-india-green border-india-green text-white'
                            : 'border-gray-300'
                        }`}>
                        {!available ? <XCircle className="h-3 w-3" /> : isSelected && <Check className="h-3 w-3" />}
                      </div>
                      <div className="flex-1">
                        <p className={`text-sm font-bold ${
                          !available ? 'text-red-700' : 'text-gray-800'
                        }`}>{doc}</p>
                        {!available && <p className="text-[10px] text-red-500">Missing from vault</p>}
                        {available && !isSelected && <p className="text-[10px] text-gray-400">Click to attach</p>}
                        {available && isSelected && <p className="text-[10px] text-india-green">Selected</p>}
                      </div>
                      {available && <FileText className={`h-4 w-4 ${!isSelected ? 'text-gray-300' : 'text-india-green'}`} />}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {currentStep === 2 && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-gray-900 uppercase tracking-widest border-l-4 border-saffron pl-4">Step 3: Confirm Eligibility</h3>
              <div className={`p-5 rounded-xl border flex items-start gap-3 ${
                evaluation?.status === 'eligible'
                  ? 'bg-india-green/5 border-india-green/20'
                  : evaluation?.status === 'possibly_eligible'
                  ? 'bg-saffron/10 border-saffron/20'
                  : 'bg-red-50 border-red-200'
              }`}>
                {evaluation?.status === 'eligible'
                  ? <CheckCircle className="h-8 w-8 text-india-green" />
                  : evaluation?.status === 'possibly_eligible'
                  ? <AlertCircle className="h-8 w-8 text-saffron-dark" />
                  : <XCircle className="h-8 w-8 text-red-600" />}
                <div>
                  <h4 className="font-bold text-sm">
                    {evaluation?.status === 'eligible' ? 'You are Eligible' : evaluation?.status === 'possibly_eligible' ? 'Possibly Eligible' : 'Not Eligible'}
                  </h4>
                  <p className="text-xs text-gray-600">{evaluation?.status === 'eligible' ? 'Based on your profile, you meet all eligibility criteria.' : 'You may still apply, but final approval is subject to officer verification.'}</p>
                </div>
              </div>
              <div className="bg-gray-50 border border-gray-200 rounded-xl p-4 space-y-2">
                <p className="text-xs font-bold text-gray-700">Match Score: <span className="text-government-blue">{evaluation?.matchScore}%</span></p>
              </div>
            </div>
          )}

          {currentStep === 3 && (
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-gray-900 uppercase tracking-widest border-l-4 border-government-blue pl-4">Step 4: Declaration</h3>
              <div className="bg-gray-50 border border-gray-200 rounded-xl p-5 text-sm leading-relaxed text-gray-700">
                <p className="mb-3 font-bold text-gray-800">I hereby declare that:</p>
                <ul className="space-y-2 ml-4 list-disc">
                  <li>The information provided in my application is true and correct to the best of my knowledge and belief.</li>
                  <li>I understand that any false or misleading information may result in rejection of my application.</li>
                  <li>I agree to provide additional documents or information if required by the authorities.</li>
                  <li>I consent to the verification of my documents through DigiLocker and other government databases.</li>
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
                <label htmlFor="declaration" className="text-sm text-gray-700 font-medium cursor-pointer">
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
                    <h3 className="text-sm font-bold text-gray-900">Application Submitted!</h3>
                    <p className="text-xs text-gray-500">Redirecting to application tracker...</p>
                  </div>
                </div>
              ) : (
                <>
                  <div className="w-20 h-20 bg-government-blue/10 rounded-full flex items-center justify-center mx-auto">
                  <Send className="h-10 w-10 text-government-blue" />
                </div>
                <div className="space-y-2">
                  <h3 className="text-sm font-bold text-gray-900">Ready to Submit?</h3>
                  <p className="text-sm text-gray-500">Click the button below to submit your application to {scheme.name}</p>
                </div>
              </>
              )}
            </div>
          )}
        </div>

        <div className="flex justify-between pt-6 border-t border-gray-100 mt-8">
          {currentStep > 0 && (
            <button
              onClick={handleBack}
              className="flex items-center gap-2 px-5 py-2.5 border border-gray-300 bg-white text-gray-700 rounded-xl text-sm font-semibold hover:bg-gray-50 transition"
            >
              <ArrowLeft className="h-4 w-4" />
              Back
            </button>
          )}
          <div className="flex-1" />
          {currentStep < 4 && (
            <button
              onClick={handleSaveDraft}
              className="flex items-center gap-2 px-5 py-2.5 border border-gray-300 bg-white text-gray-700 rounded-xl text-sm font-semibold hover:bg-gray-50 transition mr-2"
            >
              <Save className="h-4 w-4" />
              Save Draft
            </button>
          )}
          {currentStep < steps.length - 1 && (
            <button
              onClick={handleNext}
              className="flex items-center gap-2 bg-government-blue hover:bg-government-blue-dark text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition"
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
