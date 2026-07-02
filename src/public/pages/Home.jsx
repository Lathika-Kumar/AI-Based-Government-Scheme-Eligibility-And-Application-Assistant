import React, { useState, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { checkEligibility } from "@utils/eligibilityEngine";
import {
  ShieldCheck,
  Search,
  ArrowRight,
  Sparkles,
  Layers,
  Award,
  BookOpen,
  Heart,
  TrendingUp,
  FileCheck,
  UserCheck
} from "lucide-react";

const STATES = [
  "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
  "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand",
  "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
  "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
  "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
  "Uttar Pradesh", "Uttarakhand", "West Bengal",
  "Delhi (NCT)", "Jammu and Kashmir", "Ladakh", "Puducherry"
];

const CASTES = [
  { value: "General", label: "General (UR)" },
  { value: "OBC", label: "OBC (Other Backward Class)" },
  { value: "SC", label: "SC (Scheduled Caste)" },
  { value: "ST", label: "ST (Scheduled Tribe)" }
];

export default function Home() {
  const { isAuthenticated, isAdmin } = useAuth();
  const { schemes, t } = useApp();
  const navigate = useNavigate();

  // Calculator State
  const [calcAge, setCalcAge] = useState(25);
  const [calcIncome, setCalcIncome] = useState(150000);
  const [calcCaste, setCalcCaste] = useState("General");
  const [calcState, setCalcState] = useState("Gujarat");
  const [isCalculating, setIsCalculating] = useState(false);

  // Dynamic Scheme Matching
  const matchedCount = useMemo(() => {
    if (!schemes) {
return 0;
}
    const tempProfile = {
      age: Number(calcAge),
      annualIncome: Number(calcIncome),
      caste: calcCaste,
      state: calcState,
      occupation: "Farmer / Agricultural Worker", // Default test occupation
      gender: "Male"
    };
    return schemes.filter(s => {
      const eligibility = checkEligibility(tempProfile, s);
      return eligibility.status === "eligible" || eligibility.status === "possibly_eligible";
    }).length;
  }, [schemes, calcAge, calcIncome, calcCaste, calcState]);

  const handleStartProfiling = () => {
    // Save calculator details in localStorage to pre-fill the onboarding wizard
    const prefillData = {
      age: Number(calcAge),
      annualIncome: Number(calcIncome),
      caste: calcCaste,
      state: calcState
    };
    localStorage.setItem("schemebridge_prefill_profile", JSON.stringify(prefillData));

    if (isAuthenticated) {
      navigate("/onboarding");
    } else {
      navigate("/signup?prefill=1");
    }
  };

  return (
    <div className="bg-slate-50 flex flex-col min-h-screen text-slate-800 font-sans">
      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-indigo-950 via-slate-900 to-indigo-950 text-white py-20 px-4 md:px-8 overflow-hidden border-b border-indigo-900/40">
        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#4f46e5_1px,transparent_1px)] [background-size:24px_24px]"></div>
        <div className="absolute -top-40 -right-40 w-96 h-96 bg-indigo-600/10 rounded-full blur-3xl"></div>
        <div className="absolute -bottom-40 -left-40 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl"></div>

        <div className="max-w-7xl mx-auto flex flex-col lg:flex-row items-center gap-12 relative z-10">
          {/* Hero Content */}
          <div className="flex-1 space-y-6 text-center lg:text-left">
            <div className="inline-flex items-center space-x-2 bg-indigo-500/10 border border-indigo-500/30 px-3.5 py-1.5 rounded-full text-xs font-bold tracking-wide text-indigo-300">
              <Sparkles className="h-4 w-4 text-amber-400 animate-pulse" />
              <span>{t("home_engine_tag") || "AI-POWERED ELIGIBILITY GATEWAY"}</span>
            </div>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight leading-tight text-white">
              {t("home_hero_title") || "Direct Gateway to Government Welfare"}
            </h1>

            <p className="text-slate-350 text-base sm:text-lg lg:text-xl leading-relaxed text-slate-300 max-w-2xl mx-auto lg:mx-0">
              {t("home_hero_desc") || "SchemeBridge uses secure AI engines to parse socio-economic attributes, verify credentials via DigiLocker, and match you with central & state benefits instantly."}
            </p>

            <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4 pt-2">
              {isAuthenticated ? (
                <Link
                  to={isAdmin ? "/admin/dashboard" : "/dashboard"}
                  className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 bg-indigo-600 hover:bg-indigo-700 text-white px-8 py-3.5 rounded-xl font-bold transition shadow-lg shadow-indigo-650/40 hover:-translate-y-0.5"
                >
                  <span>{t("home_go_dashboard") || "Go to Citizen Dashboard"}</span>
                  <ArrowRight className="h-5 w-5" />
                </Link>
              ) : (
                <>
                  <Link
                    to="/signup"
                    className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 bg-indigo-600 hover:bg-indigo-700 text-white px-8 py-3.5 rounded-xl font-bold transition shadow-lg shadow-indigo-650/40 hover:-translate-y-0.5 focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
                  >
                    <span>{t("home_cta_eligibility") || "Check Your Scheme Eligibility"}</span>
                    <ArrowRight className="h-5 w-5" />
                  </Link>
                  <Link
                    to="/login"
                    className="w-full sm:w-auto inline-flex items-center justify-center bg-slate-800/90 text-white border border-slate-700 px-8 py-3.5 rounded-xl font-bold hover:bg-slate-700 transition"
                  >
                    <span>{t("home_cta_signin") || "Sign In to Vault"}</span>
                  </Link>
                </>
              )}
            </div>

            {/* Micro badges */}
            <div className="flex flex-wrap justify-center lg:justify-start gap-6 text-xs text-slate-400 pt-4">
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="h-4 w-4 text-emerald-500" />
                {t("home_digilocker_integration") || "DigiLocker Integration"}
              </span>
              <span className="flex items-center gap-1.5">
                <FileCheck className="h-4 w-4 text-indigo-400" />
                {t("home_secure_private") || "100% Secure & Private"}
              </span>
              <span className="flex items-center gap-1.5">
                <UserCheck className="h-4 w-4 text-indigo-400" />
                {t("home_wcag_compliant") || "WCAG 2.1 Compliant"}
              </span>
            </div>
          </div>

          {/* Hero Interactive Calculator Widget */}
          <div className="w-full lg:w-[460px] shrink-0">
            <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 shadow-2xl space-y-6">
              <div>
                <h3 className="text-lg font-extrabold text-white flex items-center gap-2">
                  <span className="bg-indigo-600 p-1.5 rounded-lg text-white">⚡</span>
                  {t("calc_title") || "Eligibility Calculator"}
                </h3>
                <p className="text-slate-400 text-xs mt-1 leading-relaxed">
                  {t("calc_subtitle") || "Enter baseline socio-demographics to preview matching central & state schemes instantly."}
                </p>
              </div>

              <div className="space-y-4 text-left">
                {/* Age */}
                <div>
                  <div className="flex justify-between text-xs font-bold text-slate-400 mb-1.5">
                    <span className="uppercase tracking-wider">{t("calc_age_label") || "Age of Applicant"}</span>
                    <span className="text-indigo-400 text-sm font-extrabold">{calcAge} {t("calc_years") || "years"}</span>
                  </div>
                  <input
                    type="range"
                    min="1"
                    max="100"
                    value={calcAge}
                    onChange={(e) => setCalcAge(e.target.value)}
                    className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                    aria-label="Age slider"
                  />
                </div>

                {/* Household Income */}
                <div>
                  <div className="flex justify-between text-xs font-bold text-slate-400 mb-1.5">
                    <span className="uppercase tracking-wider">{t("calc_income_label") || "Annual Household Income"}</span>
                    <span className="text-indigo-400 text-sm font-extrabold">₹{Number(calcIncome).toLocaleString("en-IN")}</span>
                  </div>
                  <input
                    type="range"
                    min="10000"
                    max="1500000"
                    step="10000"
                    value={calcIncome}
                    onChange={(e) => setCalcIncome(e.target.value)}
                    className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                    aria-label="Income slider"
                  />
                </div>

                {/* State & Caste Row */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                      {t("calc_state_label") || "Residence State"}
                    </label>
                    <select
                      value={calcState}
                      onChange={(e) => setCalcState(e.target.value)}
                      className="w-full bg-slate-800 border border-slate-700 text-white rounded-xl px-3 py-2 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    >
                      {STATES.map(st => (
                        <option key={st} value={st}>{st}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                      {t("calc_caste_label") || "Social Category"}
                    </label>
                    <select
                      value={calcCaste}
                      onChange={(e) => setCalcCaste(e.target.value)}
                      className="w-full bg-slate-800 border border-slate-700 text-white rounded-xl px-3 py-2 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    >
                      {CASTES.map(c => (
                        <option key={c.value} value={c.value}>{c.label}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              {/* Dynamic Matches Counter Box */}
              <div className="bg-indigo-950/70 border border-indigo-500/20 rounded-2xl p-5 text-center space-y-1">
                <span className="text-[10px] uppercase font-bold tracking-wider text-indigo-300">{t("calc_matches_label") || "Potential AI Matches"}</span>
                <div className="text-3xl font-extrabold text-amber-400 flex items-center justify-center gap-2">
                  <Sparkles className="h-5 w-5 text-amber-400 shrink-0" />
                  {matchedCount} {matchedCount === 1 ? (t("calc_scheme_single") || "Scheme") : (t("calc_scheme_plural") || "Schemes")}
                </div>
                <p className="text-[10px] text-slate-400 leading-normal">
                  {t("calc_rules_desc", { count: schemes?.length || 15 })}
                </p>
              </div>

              {/* CTA button */}
              <button
                onClick={handleStartProfiling}
                className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-3.5 rounded-xl text-sm font-extrabold flex items-center justify-center gap-2 shadow-lg transition active:scale-[0.98]"
              >
                <span>{t("calc_cta") || "Apply with Onboarding Wizard"}</span>
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Feature Showcase Grid */}
      <section id="how-it-works" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 space-y-16">
        <div className="text-center space-y-4">
          <h2 className="text-3xl font-bold text-slate-900 tracking-tight">{t("home_feature_title") || "Unified Citizen Infrastructure"}</h2>
          <p className="text-slate-500 max-w-xl mx-auto text-sm sm:text-base leading-relaxed">
            {t("home_feature_desc") || "Eliminate administrative delays. Discover benefits, secure your records, and file applications in under 5 minutes."}
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          {/* Card 1 */}
          <div className="bg-white p-8 rounded-2xl border border-slate-200 shadow-sm space-y-4 hover:shadow-md hover:border-indigo-100 transition duration-200">
            <div className="bg-indigo-50 p-4 rounded-xl inline-block text-indigo-600">
              <Search className="h-6 w-6" />
            </div>
            <h3 className="font-bold text-lg text-slate-900">{t("home_f1_title") || "AI Scheme Matching"}</h3>
            <p className="text-slate-600 text-xs leading-relaxed">
              {t("home_f1_desc") || "Instantly calculates eligibility metrics based on complex state and national policies, sorting schemes matching your profile."}
            </p>
          </div>

          {/* Card 2 */}
          <div className="bg-white p-8 rounded-2xl border border-slate-200 shadow-sm space-y-4 hover:shadow-md hover:border-indigo-100 transition duration-200">
            <div className="bg-amber-50 p-4 rounded-xl inline-block text-amber-600">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <h3 className="font-bold text-lg text-slate-900">{t("home_f2_title") || "Secure Document Locker"}</h3>
            <p className="text-slate-600 text-xs leading-relaxed">
              {t("home_f2_desc") || "Vault and link your verified government certifications securely using DigiLocker integrations to automatically pass audits."}
            </p>
          </div>

          {/* Card 3 */}
          <div className="bg-white p-8 rounded-2xl border border-slate-200 shadow-sm space-y-4 hover:shadow-md hover:border-indigo-100 transition duration-200">
            <div className="bg-emerald-50 p-4 rounded-xl inline-block text-emerald-600">
              <Layers className="h-6 w-6" />
            </div>
            <h3 className="font-bold text-lg text-slate-900">{t("home_f3_title") || "Unified Trackers"}</h3>
            <p className="text-slate-600 text-xs leading-relaxed">
              {t("home_f3_desc") || "View application progress, review logs, and receive real-time sms or email updates for social security checks."}
            </p>
          </div>
        </div>
      </section>

      {/* Policy Focus Sections */}
      <section className="bg-slate-900 text-white py-16 px-4 md:px-8 border-t border-slate-800">
        <div className="max-w-7xl mx-auto grid md:grid-cols-2 gap-12 items-center">
          <div className="space-y-6">
            <div className="inline-flex items-center space-x-2 bg-indigo-500/10 border border-indigo-500/20 px-3 py-1 rounded-full text-xs font-bold text-indigo-300">
              <span>{t("home_democratizing_welfare") || "DEMOCRATIZING WELFARE"}</span>
            </div>
            <h2 className="text-3xl font-extrabold tracking-tight">{t("home_focusing_sectors") || "Focusing on Priority Sectors"}</h2>
            <p className="text-slate-400 text-sm leading-relaxed">
              {t("home_sectors_desc") || "SchemeBridge addresses details regarding multiple socioeconomic constraints, including rural subsidies, credit linkages, agricultural subsidies, educational grants, housing assistance, and pension schemes."}
            </p>
            <div className="grid grid-cols-2 gap-4">
              <div className="flex items-start gap-2.5">
                <div className="bg-indigo-600/45 p-1.5 rounded-lg text-indigo-400 mt-0.5"><Award className="h-4 w-4" /></div>
                <div>
                  <h4 className="text-xs font-bold">{t("home_agri_support") || "Agriculture Support"}</h4>
                  <p className="text-[10px] text-slate-400 mt-0.5">{t("home_agri_desc") || "PM-KISAN, crop subsidies, irrigation packages."}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <div className="bg-indigo-650/45 p-1.5 rounded-lg text-indigo-400 mt-0.5"><BookOpen className="h-4 w-4" /></div>
                <div>
                  <h4 className="text-xs font-bold">{t("home_edu_grants") || "Education Grants"}</h4>
                  <p className="text-[10px] text-slate-400 mt-0.5">{t("home_edu_desc") || "National scholarships, fee reimbursements."}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <div className="bg-indigo-650/45 p-1.5 rounded-lg text-indigo-400 mt-0.5"><Heart className="h-4 w-4" /></div>
                <div>
                  <h4 className="text-xs font-bold">{t("home_health_cover") || "Health Cover"}</h4>
                  <p className="text-[10px] text-slate-400 mt-0.5">{t("home_health_desc") || "Ayushman Bharat, medical aids, maternal benefits."}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <div className="bg-indigo-650/45 p-1.5 rounded-lg text-indigo-400 mt-0.5"><TrendingUp className="h-4 w-4" /></div>
                <div>
                  <h4 className="text-xs font-bold">{t("home_social_security") || "Social Security"}</h4>
                  <p className="text-[10px] text-slate-400 mt-0.5">{t("home_social_security_desc") || "Atal Pension, housing loans, subsidy grants."}</p>
                </div>
              </div>
            </div>
          </div>
          <div className="bg-slate-800 rounded-3xl p-6 md:p-8 border border-slate-700/60 shadow-xl space-y-4">
            <h3 className="text-lg font-bold">{t("home_sandbox_metrics") || "AI Sandbox Simulation Metrics"}</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              {t("home_sandbox_desc") || "Evaluating local server states, database synchronization times, and API simulations:"}
            </p>
            <div className="space-y-3">
              <div>
                <div className="flex justify-between text-[10px] text-slate-400 mb-1">
                  <span>{t("home_latency_label") || "SCHEME CLASSIFICATION LATENCY"}</span>
                  <span className="font-bold text-indigo-400">12ms</span>
                </div>
                <div className="h-1 bg-slate-700 rounded-full overflow-hidden">
                  <div className="h-full bg-indigo-500 w-11/12"></div>
                </div>
              </div>
              <div>
                <div className="flex justify-between text-[10px] text-slate-400 mb-1">
                  <span>{t("home_sync_label") || "DIGILOCKER API SYNC SPEED"}</span>
                  <span className="font-bold text-emerald-400">350ms</span>
                </div>
                <div className="h-1 bg-slate-700 rounded-full overflow-hidden">
                  <div className="h-full bg-emerald-500 w-4/5"></div>
                </div>
              </div>
              <div>
                <div className="flex justify-between text-[10px] text-slate-400 mb-1">
                  <span>{t("home_accuracy_label") || "ACCURACY SCORES (MOCK DATABASE)"}</span>
                  <span className="font-bold text-amber-400">99.8%</span>
                </div>
                <div className="h-1 bg-slate-700 rounded-full overflow-hidden">
                  <div className="h-full bg-amber-500 w-[99%]"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Statistics Section */}
      <section id="benefits" className="bg-indigo-900 text-white py-14 px-4 border-t border-indigo-950">
        <div className="max-w-7xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
          <div>
            <p className="text-4xl font-extrabold text-amber-400">{t("home_stat_schemes_val") || "350+"}</p>
            <p className="text-xs text-indigo-200 uppercase tracking-widest mt-1.5 font-bold">{t("home_stat_schemes") || "Welfare Programs"}</p>
          </div>
          <div>
            <p className="text-4xl font-extrabold text-amber-400">{t("home_stat_apps_val") || "12 Lakhs"}</p>
            <p className="text-xs text-indigo-200 uppercase tracking-widest mt-1.5 font-bold">{t("home_stat_apps") || "Processed Apps"}</p>
          </div>
          <div>
            <p className="text-4xl font-extrabold text-amber-400">{t("home_stat_accuracy_val") || "99.8%"}</p>
            <p className="text-xs text-indigo-200 uppercase tracking-widest mt-1.5 font-bold">{t("home_stat_accuracy") || "Matching Accuracy"}</p>
          </div>
          <div>
            <p className="text-4xl font-extrabold text-amber-400">{t("home_stat_disbursed_val") || "₹4.2 Cr"}</p>
            <p className="text-xs text-indigo-200 uppercase tracking-widest mt-1.5 font-bold">{t("home_stat_disbursed") || "Funds Disbursed"}</p>
          </div>
        </div>
      </section>
    </div>
  );
}
