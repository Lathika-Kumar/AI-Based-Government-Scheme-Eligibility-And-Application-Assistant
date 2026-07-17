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
  UserCheck,
  Building2,
  LandPlot,
  GraduationCap,
  HeartPulse,
  Users,
  Clock,
  CheckCircle2
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
  const { schemes } = useApp();
  const navigate = useNavigate();

  const [calcAge, setCalcAge] = useState(25);
  const [calcIncome, setCalcIncome] = useState(150000);
  const [calcCaste, setCalcCaste] = useState("General");
  const [calcState, setCalcState] = useState("Gujarat");
  const [isCalculating, setIsCalculating] = useState(false);

  const matchedCount = useMemo(() => {
    if (!schemes) {
      return 0;
    }
    const tempProfile = {
      age: Number(calcAge),
      annualIncome: Number(calcIncome),
      caste: calcCaste,
      state: calcState,
      occupation: "Farmer / Agricultural Worker",
      gender: "Male"
    };
    return schemes.filter(s => {
      const eligibility = checkEligibility(tempProfile, s);
      return eligibility.status === "eligible" || eligibility.status === "possibly_eligible";
    }).length;
  }, [schemes, calcAge, calcIncome, calcCaste, calcState]);

  const handleStartProfiling = () => {
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
    <div className="bg-gray-50 flex flex-col min-h-screen text-gray-800 font-sans">
      {/* Tricolor Banner */}
      <div className="h-2 bg-gradient-to-r from-saffron via-white-official to-india-green"></div>

      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-government-blue via-government-blue-light to-government-blue text-white py-16 md:py-24 px-4 md:px-8 overflow-hidden">
        <div className="absolute inset-0 opacity-5 bg-[radial-gradient(#ffffff_1px,transparent_1px)] [background-size:24px_24px]"></div>

        <div className="max-w-7xl mx-auto flex flex-col lg:flex-row items-center gap-12 relative z-10">
          {/* Hero Content */}
          <div className="flex-1 space-y-8 text-center lg:text-left">
            <div className="inline-flex items-center space-x-2 bg-white/10 border border-white/20 px-4 py-2 rounded-full text-sm font-semibold tracking-wide text-white/90">
              <Sparkles className="h-4 w-4 text-saffron" />
              <span>OFFICIAL GOVERNMENT WELFARE PLATFORM</span>
            </div>

            <div>
              <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight leading-tight text-white">
                SchemeBridge
              </h1>
              <p className="text-xl sm:text-2xl font-medium text-white/90 mt-2">
                Your Gateway to Government Welfare Schemes
              </p>
            </div>

            <p className="text-gray-200 text-base sm:text-lg leading-relaxed max-w-2xl mx-auto lg:mx-0">
              Discover, apply, and track all Central and State Government welfare schemes through a single, secure platform.
              Powered by AI eligibility matching and integrated with DigiLocker for seamless document verification.
            </p>

            <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4 pt-2">
              {isAuthenticated ? (
                <Link
                  to={isAdmin ? "/admin/dashboard" : "/dashboard"}
                  className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 bg-saffron hover:bg-saffron-dark text-government-blue-dark px-8 py-4 rounded-lg font-bold transition shadow-lg hover:shadow-xl hover:-translate-y-0.5"
                >
                  <span>Access Citizen Dashboard</span>
                  <ArrowRight className="h-5 w-5" />
                </Link>
              ) : (
                <>
                  <Link
                    to="/signup"
                    className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 bg-saffron hover:bg-saffron-dark text-government-blue-dark px-8 py-4 rounded-lg font-bold transition shadow-lg hover:shadow-xl"
                  >
                    <span>Register Now</span>
                    <ArrowRight className="h-5 w-5" />
                  </Link>
                  <Link
                    to="/login"
                    className="w-full sm:w-auto inline-flex items-center justify-center bg-white/10 text-white border border-white/30 px-8 py-4 rounded-lg font-bold hover:bg-white/20 transition"
                  >
                    <span>Sign In</span>
                  </Link>
                </>
              )}
            </div>

            <div className="flex flex-wrap justify-center lg:justify-start gap-8 text-sm text-white/80 pt-4">
              <div className="flex items-center gap-2">
                <ShieldCheck className="h-5 w-5 text-emerald" />
                <span>DigiLocker Integration</span>
              </div>
              <div className="flex items-center gap-2">
                <FileCheck className="h-5 w-5 text-emerald" />
                <span>100% Secure & Private</span>
              </div>
              <div className="flex items-center gap-2">
                <UserCheck className="h-5 w-5 text-emerald" />
                <span>WCAG 2.1 AA Compliant</span>
              </div>
            </div>
          </div>

          {/* Eligibility Calculator Widget */}
          <div className="w-full lg:w-[480px] shrink-0">
            <div className="bg-white rounded-xl p-6 md:p-8 shadow-xl space-y-6">
              <div>
                <div className="flex items-center gap-3 mb-2">
                  <div className="bg-government-blue/10 p-2 rounded-lg">
                    <Sparkles className="h-6 w-6 text-government-blue" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-gray-900">AI Eligibility Checker</h3>
                    <p className="text-gray-600 text-sm">Get instant scheme recommendations</p>
                  </div>
                </div>
              </div>

              <div className="space-y-5">
                <div>
                  <div className="flex justify-between text-sm font-medium text-gray-700 mb-2">
                    <span>Age of Applicant</span>
                    <span className="text-government-blue font-bold">{calcAge} years</span>
                  </div>
                  <input
                    type="range"
                    min="1"
                    max="100"
                    value={calcAge}
                    onChange={(e) => setCalcAge(e.target.value)}
                    className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-government-blue"
                    aria-label="Age slider"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-sm font-medium text-gray-700 mb-2">
                    <span>Annual Household Income</span>
                    <span className="text-government-blue font-bold">₹{Number(calcIncome).toLocaleString("en-IN")}</span>
                  </div>
                  <input
                    type="range"
                    min="10000"
                    max="1500000"
                    step="10000"
                    value={calcIncome}
                    onChange={(e) => setCalcIncome(e.target.value)}
                    className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-government-blue"
                    aria-label="Income slider"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Residence State
                    </label>
                    <select
                      value={calcState}
                      onChange={(e) => setCalcState(e.target.value)}
                      className="w-full bg-gray-50 border border-gray-300 text-gray-900 rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-government-blue focus:border-government-blue focus:outline-none"
                    >
                      {STATES.map(st => (
                        <option key={st} value={st}>{st}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Social Category
                    </label>
                    <select
                      value={calcCaste}
                      onChange={(e) => setCalcCaste(e.target.value)}
                      className="w-full bg-gray-50 border border-gray-300 text-gray-900 rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-government-blue focus:border-government-blue focus:outline-none"
                    >
                      {CASTES.map(c => (
                        <option key={c.value} value={c.value}>{c.label}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              <div className="bg-government-blue/5 border border-government-blue/20 rounded-lg p-5 text-center space-y-2">
                <span className="text-xs uppercase font-bold tracking-wider text-government-blue">Potential Schemes Eligible</span>
                <div className="text-4xl font-extrabold text-saffron flex items-center justify-center gap-2">
                  <Sparkles className="h-6 w-6 text-saffron shrink-0" />
                  {matchedCount} {matchedCount === 1 ? "Scheme" : "Schemes"}
                </div>
                <p className="text-sm text-gray-600">
                  Based on {schemes?.length || 15}+ government welfare programs
                </p>
              </div>

              <button
                onClick={handleStartProfiling}
                className="w-full bg-government-blue hover:bg-government-blue-dark text-white py-3.5 rounded-lg text-sm font-bold flex items-center justify-center gap-2 shadow-md transition hover:shadow-lg active:scale-[0.98]"
              >
                <span>Proceed with Application</span>
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Official Info Bar */}
      <section className="bg-white border-b border-gray-200 py-6 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="flex flex-wrap items-center justify-center gap-8 text-sm">
            <div className="flex items-center gap-2 text-gray-700">
              <Building2 className="h-4 w-4 text-government-blue" />
              <span><strong>Ministry:</strong> Ministry of Social Justice & Empowerment</span>
            </div>
            <div className="flex items-center gap-2 text-gray-700">
              <Clock className="h-4 w-4 text-government-blue" />
              <span><strong>Last Updated:</strong> July 2026</span>
            </div>
            <div className="flex items-center gap-2 text-gray-700">
              <CheckCircle2 className="h-4 w-4 text-india-green" />
              <span><strong>Status:</strong> Operational</span>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 md:py-20 space-y-12">
        <div className="text-center space-y-4">
          <h2 className="text-2xl md:text-3xl font-bold text-gray-900">Key Features</h2>
          <p className="text-gray-600 max-w-2xl mx-auto text-base leading-relaxed">
            Streamlined access to government welfare benefits through a single, integrated platform
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          <div className="bg-white p-8 rounded-xl border border-gray-200 shadow-sm hover:shadow-card-hover hover:border-government-blue/20 transition duration-200">
            <div className="bg-government-blue/10 p-4 rounded-lg inline-block text-government-blue mb-4">
              <Search className="h-7 w-7" />
            </div>
            <h3 className="font-bold text-lg text-gray-900 mb-2">AI-Powered Eligibility Matching</h3>
            <p className="text-gray-600 text-sm leading-relaxed">
              Our advanced AI engine instantly evaluates your profile against hundreds of government schemes
              to find the benefits you qualify for, saving you time and effort.
            </p>
          </div>

          <div className="bg-white p-8 rounded-xl border border-gray-200 shadow-sm hover:shadow-card-hover hover:border-government-blue/20 transition duration-200">
            <div className="bg-india-green/10 p-4 rounded-lg inline-block text-india-green mb-4">
              <ShieldCheck className="h-7 w-7" />
            </div>
            <h3 className="font-bold text-lg text-gray-900 mb-2">Secure Digital Document Vault</h3>
            <p className="text-gray-600 text-sm leading-relaxed">
              Store and manage all your government-issued documents securely with DigiLocker integration.
              Your documents are verified and easily accessible for scheme applications.
            </p>
          </div>

          <div className="bg-white p-8 rounded-xl border border-gray-200 shadow-sm hover:shadow-card-hover hover:border-government-blue/20 transition duration-200">
            <div className="bg-saffron/20 p-4 rounded-lg inline-block text-saffron-dark mb-4">
              <Layers className="h-7 w-7" />
            </div>
            <h3 className="font-bold text-lg text-gray-900 mb-2">Real-Time Application Tracking</h3>
            <p className="text-gray-600 text-sm leading-relaxed">
              Track the progress of all your applications in one place. Receive updates, review officer comments,
              and download acknowledgments instantly.
            </p>
          </div>
        </div>
      </section>

      {/* Priority Sectors Section */}
      <section className="bg-gray-100 py-16 px-4 md:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="text-center space-y-4 mb-12">
            <h2 className="text-2xl md:text-3xl font-bold text-gray-900">Priority Sectors</h2>
            <p className="text-gray-600 max-w-2xl mx-auto text-base">
              Comprehensive coverage across key government welfare domains
            </p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm text-center">
              <div className="bg-saffron/10 p-3 rounded-lg inline-block text-saffron-dark mb-4">
                <LandPlot className="h-8 w-8 mx-auto" />
              </div>
              <h4 className="font-bold text-gray-900 mb-1">Agriculture</h4>
              <p className="text-gray-600 text-xs">PM-KISAN, Crop Subsidies</p>
            </div>

            <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm text-center">
              <div className="bg-government-blue/10 p-3 rounded-lg inline-block text-government-blue mb-4">
                <GraduationCap className="h-8 w-8 mx-auto" />
              </div>
              <h4 className="font-bold text-gray-900 mb-1">Education</h4>
              <p className="text-gray-600 text-xs">Scholarships, Fee Reimbursement</p>
            </div>

            <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm text-center">
              <div className="bg-india-green/10 p-3 rounded-lg inline-block text-india-green mb-4">
                <HeartPulse className="h-8 w-8 mx-auto" />
              </div>
              <h4 className="font-bold text-gray-900 mb-1">Healthcare</h4>
              <p className="text-gray-600 text-xs">Ayushman Bharat, Medical Aids</p>
            </div>

            <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm text-center">
              <div className="bg-gray-100 p-3 rounded-lg inline-block text-gray-700 mb-4">
                <Users className="h-8 w-8 mx-auto" />
              </div>
              <h4 className="font-bold text-gray-900 mb-1">Social Security</h4>
              <p className="text-gray-600 text-xs">Pensions, Housing Assistance</p>
            </div>
          </div>
        </div>
      </section>

      {/* Statistics Section */}
      <section className="bg-gradient-to-r from-government-blue via-government-blue-light to-government-blue text-white py-12 px-4">
        <div className="max-w-7xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
          <div>
            <p className="text-3xl md:text-4xl font-extrabold text-saffron">350+</p>
            <p className="text-sm text-white/90 uppercase tracking-widest mt-2 font-semibold">Welfare Programs</p>
          </div>
          <div>
            <p className="text-3xl md:text-4xl font-extrabold text-saffron">12 Lakhs+</p>
            <p className="text-sm text-white/90 uppercase tracking-widest mt-2 font-semibold">Applications Processed</p>
          </div>
          <div>
            <p className="text-3xl md:text-4xl font-extrabold text-saffron">99.8%</p>
            <p className="text-sm text-white/90 uppercase tracking-widest mt-2 font-semibold">Matching Accuracy</p>
          </div>
          <div>
            <p className="text-3xl md:text-4xl font-extrabold text-saffron">₹4.2 Cr+</p>
            <p className="text-sm text-white/90 uppercase tracking-widest mt-2 font-semibold">Funds Disbursed</p>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="bg-white py-16 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-4">
            Start Your Welfare Journey Today
          </h2>
          <p className="text-gray-600 text-base mb-8 max-w-2xl mx-auto">
            Register now and discover all the government benefits you are eligible for.
            Complete your profile and start applying in minutes.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            {isAuthenticated ? (
              <Link
                to={isAdmin ? "/admin/dashboard" : "/dashboard"}
                className="inline-flex items-center justify-center space-x-2 bg-saffron hover:bg-saffron-dark text-government-blue-dark px-8 py-4 rounded-lg font-bold transition shadow-lg hover:shadow-xl"
              >
                <span>Go to Dashboard</span>
                <ArrowRight className="h-5 w-5" />
              </Link>
            ) : (
              <>
                <Link
                  to="/signup"
                  className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 bg-government-blue hover:bg-government-blue-dark text-white px-8 py-4 rounded-lg font-bold transition shadow-lg hover:shadow-xl"
                >
                  <span>Create Your Account</span>
                  <ArrowRight className="h-5 w-5" />
                </Link>
                <Link
                  to="/login"
                  className="w-full sm:w-auto inline-flex items-center justify-center bg-gray-100 text-gray-900 border border-gray-300 px-8 py-4 rounded-lg font-bold hover:bg-gray-200 transition"
                >
                  <span>Sign In</span>
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      {/* Footer Info */}
      <section className="bg-gray-50 border-t border-gray-200 py-8 px-4">
        <div className="max-w-7xl mx-auto text-center">
          <p className="text-gray-600 text-sm">
            <strong>Disclaimer:</strong> This is a demonstration platform. For official government services,
            please refer to the respective ministry websites.
          </p>
        </div>
      </section>
    </div>
  );
}
