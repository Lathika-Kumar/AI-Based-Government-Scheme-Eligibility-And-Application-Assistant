import React, { useState } from "react";
import { Link } from "react-router-dom";
import { usePageMeta } from "@utils/usePageMeta";
import { useAuth } from "@context/AuthContext";
import schemeService from "@services/schemeService";
import applicationService from "@services/applicationService";
import { useToast } from "@components/ui/ToastNotification";
import { IS_FLOW_TEST_MODE } from "@utils/flowTestMode";

import {
  CheckCircle, XCircle, AlertCircle, ArrowRight, Sparkles,
  User, MapPin, IndianRupee, Briefcase, AlertTriangle, RefreshCw, Info
} from "lucide-react";

const STATES = [
  "Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh","Goa","Gujarat","Haryana",
  "Himachal Pradesh","Jharkhand","Karnataka","Kerala","Madhya Pradesh","Maharashtra","Manipur",
  "Meghalaya","Mizoram","Nagaland","Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana",
  "Tripura","Uttar Pradesh","Uttarakhand","West Bengal","Andaman and Nicobar Islands","Chandigarh",
  "Dadra and Nagar Haveli and Daman and Diu","Delhi","Jammu and Kashmir","Ladakh","Lakshadweep","Puducherry"
];
const GENDERS = ["Male","Female","Transgender","Other"];
const OCCUPATIONS = ["Farmer","Agricultural Labourer","Salaried Employee","Self-Employed","Business Owner","Student","Unemployed","Daily Wage Worker","Fisherman","Artisan","Other"];
const SOCIAL_CATEGORIES = ["General","OBC","SC","ST","Minority","EWS"];

const STATUS_META = {
  ELIGIBLE: {
    color: "bg-green-50 border-green-200 text-green-800",
    badge: "bg-green-100 text-green-800 border-green-200",
    icon: CheckCircle,
    iconColor: "text-green-600",
    label: "Eligible",
  },
  NOT_ELIGIBLE: {
    color: "bg-red-50 border-red-200 text-red-800",
    badge: "bg-red-100 text-red-800 border-red-200",
    icon: XCircle,
    iconColor: "text-red-500",
    label: "Not Eligible",
  },
  INDETERMINATE: {
    color: "bg-amber-50 border-amber-200 text-amber-800",
    badge: "bg-amber-100 text-amber-800 border-amber-200",
    icon: AlertCircle,
    iconColor: "text-amber-500",
    label: "Cannot Determine",
  },
};

export default function EligibilityChecker() {
  usePageMeta("Check Eligibility", "Evaluate your eligibility for government welfare schemes");
  const { user } = useAuth();
  const showToast = useToast();

  const [profile, setProfile] = useState({
    age: "", gender: "", annualIncome: "", occupation: "",
    state: "", socialCategory: "", disabilityStatus: false,
  });

  const [mode, setMode]     = useState("all"); // "all" | "single"
  const [schemeCode, setSchemeCode] = useState("");

  const [results, setResults]   = useState(null);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState(null);
  const [applying, setApplying] = useState({}); // { [schemeCode]: loading }

  const setField = (key, val) => setProfile((p) => ({ ...p, [key]: val }));

  const buildProfile = () => ({
    age:            profile.age    ? parseInt(profile.age, 10) : undefined,
    gender:         profile.gender || undefined,
    annualIncome:   profile.annualIncome ? parseFloat(profile.annualIncome) : undefined,
    occupation:     profile.occupation || undefined,
    state:          profile.state || undefined,
    socialCategory: profile.socialCategory || undefined,
    disabilityStatus: profile.disabilityStatus || undefined,
    attributes:     {},
  });

  const handleEvaluate = async (e) => {
    e.preventDefault();
    // TEMPORARY TEST MODE: Skip client-side required field block when test mode is enabled
    if (!IS_FLOW_TEST_MODE) {
      if (!profile.age && !profile.gender && !profile.state) {
        setError("Please fill in at least age, gender, and state to evaluate eligibility.");
        return;
      }
    }
    setLoading(true);
    setError(null);
    setResults(null);


    const builtProfile = buildProfile();

    let result;
    if (mode === "single" && schemeCode) {
      result = await schemeService.evaluateEligibilityByCode(schemeCode.trim().toUpperCase(), builtProfile);
      if (!result.error) {
        setResults({ mode: "single", data: result.data });
      }
    } else {
      result = await schemeService.evaluateEligibilityForAll(builtProfile);
      if (!result.error) {
        setResults({ mode: "all", data: result.data });
      }
    }

    if (result.error) {
      setError(result.message || "Eligibility evaluation failed. Please try again.");
    }
    setLoading(false);
  };

  const handleApply = async (sCode) => {
    setApplying((prev) => ({ ...prev, [sCode]: true }));
    const result = await applicationService.createApplication({
      schemeCode: sCode,
    });
    setApplying((prev) => ({ ...prev, [sCode]: false }));
    if (result.error) {
      showToast(result.message || "Could not create application.", "error");
    } else {
      showToast(`Application ${result.data.applicationNumber} created!`, "success");
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-r from-government-blue via-government-blue-light to-government-blue text-white p-6 rounded-xl shadow-lg">
        <div className="flex items-center gap-3 mb-2">
          <Sparkles className="h-5 w-5 text-saffron" />
          <span className="text-xs font-semibold text-white/80 uppercase tracking-wider">Backend Eligibility Engine</span>
        </div>
        <h1 className="text-2xl font-bold">Check Your Scheme Eligibility</h1>
        <p className="text-sm text-white/90 mt-1">
          Fill in your profile details and the SchemeBridge eligibility engine will evaluate which government schemes you qualify for.
        </p>
      </div>

      {/* Disclaimer */}
      <div className="flex items-start gap-3 bg-amber-50 border border-amber-200 text-amber-800 text-xs p-4 rounded-xl">
        <Info className="h-4 w-4 shrink-0 mt-0.5" />
        <p>
          Eligibility results are based on your profile data and published scheme criteria.
          They are <strong>not a guarantee of government approval</strong> or official eligibility certification.
          Results depend on the completeness and accuracy of your profile information.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Profile Form */}
        <form onSubmit={handleEvaluate} className="lg:col-span-2 bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl p-6 shadow-sm space-y-5 self-start">
          <h2 className="text-base font-bold text-gray-900 dark:text-slate-100 flex items-center gap-2">
            <User className="h-4 w-4 text-government-blue dark:text-indigo-400" /> Your Profile
          </h2>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1">Age</label>
              <input type="number" min="0" max="120" value={profile.age}
                onChange={(e) => setField("age", e.target.value)}
                placeholder="e.g. 32"
                className="w-full px-3 py-2 text-sm border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1">Gender</label>
              <select value={profile.gender} onChange={(e) => setField("gender", e.target.value)}
                className="w-full px-3 py-2 text-sm border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
                <option value="">Select</option>
                {GENDERS.map(g => <option key={g} value={g}>{g}</option>)}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              <IndianRupee className="inline h-3 w-3 mr-1" />Annual Income (₹)
            </label>
            <input type="number" min="0" value={profile.annualIncome}
              onChange={(e) => setField("annualIncome", e.target.value)}
              placeholder="e.g. 180000"
              className="w-full px-3 py-2 text-sm border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400" />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              <Briefcase className="inline h-3 w-3 mr-1" />Occupation
            </label>
            <select value={profile.occupation} onChange={(e) => setField("occupation", e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
              <option value="">Select</option>
              {OCCUPATIONS.map(o => <option key={o} value={o}>{o}</option>)}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1">
              <MapPin className="inline h-3 w-3 mr-1" />State / UT
            </label>
            <select value={profile.state} onChange={(e) => setField("state", e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
              <option value="">Select</option>
              {STATES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1">Social Category</label>
            <select value={profile.socialCategory} onChange={(e) => setField("socialCategory", e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
              <option value="">Select</option>
              {SOCIAL_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>

          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={profile.disabilityStatus}
              onChange={(e) => setField("disabilityStatus", e.target.checked)}
              className="rounded border-gray-300 dark:border-slate-700 text-government-blue focus:ring-government-blue" />
            <span className="text-sm text-gray-700 dark:text-slate-300 font-medium">Person with disability</span>
          </label>

          {/* Mode toggle */}
          <div className="border-t border-gray-100 dark:border-slate-800 pt-4">
            <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-2">Evaluation Mode</label>
            <div className="flex gap-2">
              <button type="button" onClick={() => setMode("all")}
                className={`flex-1 text-xs font-semibold py-2 rounded-lg border transition ${mode === "all" ? "bg-government-blue dark:bg-indigo-600 text-white border-government-blue dark:border-indigo-600" : "bg-white dark:bg-slate-800 text-gray-700 dark:text-slate-200 border-gray-300 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-700"}`}>
                All Active Schemes
              </button>
              <button type="button" onClick={() => setMode("single")}
                className={`flex-1 text-xs font-semibold py-2 rounded-lg border transition ${mode === "single" ? "bg-government-blue dark:bg-indigo-600 text-white border-government-blue dark:border-indigo-600" : "bg-white dark:bg-slate-800 text-gray-700 dark:text-slate-200 border-gray-300 dark:border-slate-700 hover:bg-gray-50 dark:hover:bg-slate-700"}`}>
                Specific Scheme
              </button>
            </div>
            {mode === "single" && (
              <input type="text" value={schemeCode} onChange={(e) => setSchemeCode(e.target.value)}
                placeholder="Scheme code e.g. SCH-PMKISAN-001"
                className="mt-2 w-full px-3 py-2 text-sm border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400" />
            )}
          </div>

          {error && (
            <div className="flex items-start gap-2 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 text-xs p-3 rounded-lg">
              <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <button type="submit" disabled={loading}
            className="w-full bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 disabled:opacity-60 text-white py-3 rounded-lg text-sm font-bold flex items-center justify-center gap-2 transition">
            {loading ? (
              <><RefreshCw className="h-4 w-4 animate-spin" /> Evaluating...</>
            ) : (
              <><Sparkles className="h-4 w-4" /> Evaluate Eligibility</>
            )}
          </button>
        </form>

        {/* Results Panel */}
        <div className="lg:col-span-3 space-y-4">
          {!results && !loading && (
            <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl p-12 text-center text-gray-400 dark:text-slate-500">
              <Sparkles className="h-10 w-10 mx-auto mb-3 opacity-30" />
              <p className="font-semibold text-gray-700 dark:text-slate-300">Fill in your profile and click Evaluate</p>
              <p className="text-sm mt-1">Results from the SchemeBridge eligibility engine will appear here.</p>
            </div>
          )}

          {loading && (
            <div className="space-y-3">
              {[1,2,3].map(i => (
                <div key={i} className="bg-gray-100 dark:bg-slate-800 rounded-xl animate-pulse h-32" />
              ))}
            </div>
          )}

          {results && results.mode === "all" && results.data && (
            <>
              <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl p-4 shadow-sm">
                <div className="grid grid-cols-3 gap-4 text-center">
                  <div>
                    <div className="text-2xl font-bold text-green-600 dark:text-emerald-400">{results.data.eligible?.length ?? 0}</div>
                    <div className="text-xs text-gray-500 dark:text-slate-400 font-semibold mt-0.5">Eligible</div>
                  </div>
                  <div>
                    <div className="text-2xl font-bold text-amber-500 dark:text-amber-400">{results.data.indeterminate?.length ?? 0}</div>
                    <div className="text-xs text-gray-500 dark:text-slate-400 font-semibold mt-0.5">Indeterminate</div>
                  </div>
                  <div>
                    <div className="text-2xl font-bold text-red-500 dark:text-red-400">{results.data.notEligible?.length ?? 0}</div>
                    <div className="text-xs text-gray-500 dark:text-slate-400 font-semibold mt-0.5">Not Eligible</div>
                  </div>
                </div>
              </div>

              {/* Show eligible first */}
              {[...( results.data.eligible ?? []), ...(results.data.indeterminate ?? [])].map((item) => {
                const meta = STATUS_META[item.status] || STATUS_META.INDETERMINATE;
                const Icon = meta.icon;
                return (
                  <EligibilityCard key={item.schemeCode} item={item} meta={meta} Icon={Icon}
                    onApply={handleApply} applying={applying[item.schemeCode]} />
                );
              })}
            </>
          )}

          {results && results.mode === "single" && results.data && (() => {
            const item = results.data;
            const meta = STATUS_META[item.status] || STATUS_META.INDETERMINATE;
            const Icon = meta.icon;
            return (
              <EligibilityCard item={item} meta={meta} Icon={Icon}
                onApply={handleApply} applying={applying[item.schemeCode]} />
            );
          })()}
        </div>
      </div>
    </div>
  );
}

function EligibilityCard({ item, meta, Icon, onApply, applying }) {
  return (
    <div className={`bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl p-5 shadow-sm space-y-3 ${meta.color}`}>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Icon className={`h-5 w-5 ${meta.iconColor}`} />
          <div>
            <p className="font-bold text-sm text-gray-900 dark:text-slate-100">{item.schemeCode}</p>
            <span className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${meta.badge}`}>{meta.label}</span>
          </div>
        </div>
        <Link to={`/scheme/${item.schemeCode}`} className="text-government-blue dark:text-indigo-400 text-xs font-semibold hover:underline flex items-center gap-1">
          View Scheme <ArrowRight className="h-3 w-3" />
        </Link>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
        {item.matchedConditions?.length > 0 && (
          <div>
            <p className="font-semibold text-green-700 dark:text-emerald-400 mb-1">✓ Matched Conditions</p>
            <ul className="space-y-0.5">
              {item.matchedConditions.map((c, i) => (
                <li key={i} className="text-green-800 dark:text-emerald-300">{c}</li>
              ))}
            </ul>
          </div>
        )}
        {item.failedConditions?.length > 0 && (
          <div>
            <p className="font-semibold text-red-700 dark:text-red-400 mb-1">✗ Failed Conditions</p>
            <ul className="space-y-0.5">
              {item.failedConditions.map((c, i) => (
                <li key={i} className="text-red-800 dark:text-red-300">{c}</li>
              ))}
            </ul>
          </div>
        )}
        {item.missingInformation?.length > 0 && (
          <div className="sm:col-span-2">
            <p className="font-semibold text-amber-700 dark:text-amber-400 mb-1">? Missing Information</p>
            <ul className="space-y-0.5">
              {item.missingInformation.map((m, i) => (
                <li key={i} className="text-amber-800 dark:text-amber-300">{m}</li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {item.status === "ELIGIBLE" && onApply && (
        <button
          onClick={() => onApply(item.schemeCode)}
          disabled={applying}
          className="mt-2 w-full bg-green-600 hover:bg-green-700 disabled:opacity-60 text-white py-2 rounded-lg text-xs font-bold flex items-center justify-center gap-1.5 transition"
        >
          {applying ? <><RefreshCw className="h-3.5 w-3.5 animate-spin" /> Creating application...</> : "Apply for this Scheme"}
        </button>
      )}
    </div>
  );
}
