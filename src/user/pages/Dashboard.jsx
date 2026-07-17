import React, { useMemo, useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { checkEligibility } from "@utils/eligibilityEngine";
import { getDocReadinessForScheme } from "@utils/documentReadiness";
import { calculateCompletion } from "@data/mockProfile";
import { MOCK_RECENT_ACTIVITIES, MOCK_QUICK_ACTIONS } from "@data/mockDashboard";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import { StatCardSkeleton, AIBriefSkeleton, ListRowSkeleton } from "@components/ui/LoadingSkeleton";
import {
  Sparkles,
  ClipboardList,
  FileText,
  ArrowRight,
  AlertTriangle,
  MessageSquare,
  Zap,
  Calendar,
  Award,
  BarChart3,
  CheckCircle,
  Globe,
  Target,
  Bot,
  User,
  Building2,
  ShieldCheck
} from "lucide-react";

export default function Dashboard() {
  const { user } = useAuth();
  const { profile, schemes, applications, documents, savedSchemes, language, changeLanguage } = useApp();

  const [aiChatOpen, setAiChatOpen] = useState(false);
  const [aiInitialQuery, setAiInitialQuery] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 800);
    return () => clearTimeout(timer);
  }, []);

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return "Good Morning";
    if (hour < 17) return "Good Afternoon";
    return "Good Evening";
  }, []);

  const evalProfile = useMemo(() => ({
    name: profile?.name || user?.name || "Citizen",
    age: profile?.age || user?.age || 32,
    annualIncome: profile?.annualIncome || user?.income || 180000,
    occupation: profile?.occupation || user?.occupation || "Farmer",
    caste: profile?.caste || user?.caste || "OBC",
    gender: profile?.gender || user?.gender || "Male",
    state: profile?.state || user?.state || "Gujarat"
  }), [profile, user]);

  const eligibleSchemes = useMemo(() =>
    schemes.filter(s => checkEligibility(evalProfile, s, documents).status === "eligible"),
  [schemes, evalProfile, documents]);

  const matchingCount = eligibleSchemes.length;
  const appliedCount = applications.length;
  const verifiedDocCount = documents.filter(d => d.status === "verified").length;

  const profileCompletionScore = useMemo(() => {
    return calculateCompletion(evalProfile);
  }, [evalProfile]);

  const avgSavedReadiness = useMemo(() => {
    const list = savedSchemes.map(s => {
      const schemeObj = schemes.find(sc => sc.id === s.schemeId);
      if (!schemeObj) {
        return 0;
      }
      return getDocReadinessForScheme(schemeObj.requiredDocuments, documents).readinessScore;
    });
    return list.length > 0 ? Math.round(list.reduce((a, b) => a + b, 0) / list.length) : 0;
  }, [savedSchemes, schemes, documents]);

  const unappliedMatches = useMemo(() =>
    eligibleSchemes.filter(s => !applications.some(a => a.schemeId === s.id)),
  [eligibleSchemes, applications]);

  const topActionScheme = unappliedMatches[0] || schemes.find(s => s.id === "pm-kisan");

  const missingDocsList = useMemo(() => {
    const counts = {};
    savedSchemes.forEach(s => {
      const schemeObj = schemes.find(sc => sc.id === s.schemeId);
      if (schemeObj) {
        const { missingDocs } = getDocReadinessForScheme(schemeObj.requiredDocuments, documents);
        missingDocs.forEach(doc => {
          counts[doc] = (counts[doc] || 0) + 1;
        });
      }
    });
    return Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 3);
  }, [savedSchemes, schemes, documents]);

  const upcomingDeadlines = useMemo(() => {
    const mockDeadlineDays = [4, 15, 30];
    return unappliedMatches.slice(0, 3).map((s, i) => {
      const daysLeft = mockDeadlineDays[i] || 15 + i * 10;
      const deadlineDate = new Date();
      deadlineDate.setDate(deadlineDate.getDate() + daysLeft);
      return {
        id: s.id,
        name: s.name,
        deadline: deadlineDate.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }),
        daysLeft
      };
    });
  }, [unappliedMatches]);

  const handleAskAI = (promptText) => {
    setAiInitialQuery(promptText);
    setAiChatOpen(true);
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 p-6 rounded-xl shadow-xs animate-pulse">
          <div className="h-3 w-24 bg-gray-200 rounded mb-2" />
          <div className="h-7 w-56 bg-gray-200 rounded mb-1.5" />
          <div className="h-2.5 w-72 bg-gray-100 rounded" />
        </div>
        <AIBriefSkeleton />
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => <StatCardSkeleton key={i} />)}
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-xs space-y-3">
          <div className="h-3 w-40 bg-gray-200 rounded animate-pulse" />
          {[1, 2, 3].map((i) => <ListRowSkeleton key={i} />)}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="h-2 bg-gradient-to-r from-saffron via-white-official to-india-green rounded-lg" />

      <div className="bg-gradient-to-br from-government-blue via-government-blue-light to-government-blue text-white p-6 md:p-8 rounded-xl shadow-lg">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="bg-white p-2 rounded-lg shadow-md">
                <Building2 className="h-6 w-6 text-government-blue" />
              </div>
              <div>
                <span className="text-xs font-semibold text-white/80 uppercase tracking-wider">
                  Government of India
                </span>
                <h1 className="text-2xl md:text-3xl font-bold tracking-tight">
                  {greeting}, {evalProfile.name}!
                </h1>
              </div>
            </div>
            <p className="text-white/90 text-sm leading-relaxed max-w-2xl">
              Welcome to the official welfare portal. Here's your personalized overview of eligible schemes,
              application status, and important updates.
            </p>
          </div>

          <div className="flex items-center gap-2 bg-white/10 border border-white/20 p-2 rounded-lg shrink-0">
            <Globe className="h-4 w-4 text-white/70 ml-1 shrink-0" />
            <select
              value={language}
              onChange={(e) => changeLanguage(e.target.value)}
              className="bg-transparent border-none text-white text-sm font-medium focus:outline-none cursor-pointer pr-4 py-1 pl-0"
              aria-label="Toggle Dashboard Language"
            >
              <option value="en">English</option>
              <option value="hi">हिंदी</option>
              <option value="ta">தமிழ்</option>
              <option value="te">తెలుగు</option>
              <option value="kn">ಕನ್ನಡ</option>
            </select>
          </div>
        </div>
      </div>

      <div className="bg-white border border-gray-200 p-6 rounded-xl shadow-sm">
        <div className="flex items-center gap-3 mb-4">
          <div className="bg-saffron/10 p-2 rounded-lg">
            <Sparkles className="h-5 w-5 text-saffron-dark" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-gray-900">Daily Action Brief</h2>
            <p className="text-xs text-gray-500">AI Generated Recommendations</p>
          </div>
        </div>

        <div className="space-y-3 text-sm">
          <div className="flex items-start gap-2">
            <span className="text-government-blue font-bold mt-0.5">•</span>
            <p className="text-gray-700">
              You have <strong className="text-india-green font-semibold">{matchingCount}</strong> eligible schemes
              {unappliedMatches.length > 0 ? (
                <span> with <strong className="text-government-blue font-semibold">{unappliedMatches.length}</strong> ready to apply</span>
              ) : (
                <span> and you've applied to all of them</span>
              )}
            </p>
          </div>

          {upcomingDeadlines.length > 0 && (
            <div className="flex items-start gap-2">
              <span className="text-government-blue font-bold mt-0.5">•</span>
              <p className="text-gray-700">
                <strong className="text-red-600 font-semibold">Priority:</strong>{" "}
                {upcomingDeadlines[0].name} deadline is in <strong className="text-red-600 font-semibold">{upcomingDeadlines[0].daysLeft} days</strong> on {upcomingDeadlines[0].deadline}
              </p>
            </div>
          )}

          {missingDocsList.length > 0 && (
            <div className="flex items-start gap-2">
              <span className="text-government-blue font-bold mt-0.5">•</span>
              <p className="text-gray-700">
                Upload <strong className="text-saffron-dark font-semibold">{missingDocsList[0][0]}</strong> to unlock {missingDocsList[0][1]} more schemes
              </p>
            </div>
          )}

          <div className="flex items-start gap-2">
            <span className="text-government-blue font-bold mt-0.5">•</span>
            <p className="text-gray-700">
              Your profile is <strong className="text-india-green font-semibold">{profileCompletionScore}%</strong> complete
              {profileCompletionScore < 100 ? " – fill the remaining details for better recommendations" : " – excellent work! You're all set!"}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap gap-3 mt-5 pt-4 border-t border-gray-100">
          <button
            onClick={() => handleAskAI("Summary check: Tell me about my best matches and what to do next.")}
            className="inline-flex items-center gap-2 bg-government-blue hover:bg-government-blue-dark text-white px-4 py-2.5 rounded-lg text-sm font-semibold shadow-sm hover:shadow transition"
          >
            <MessageSquare className="h-4 w-4" />
            Ask AI Assistant
          </button>
          <Link
            to="/recommendations"
            className="inline-flex items-center gap-2 bg-white border border-gray-300 hover:bg-gray-50 text-gray-800 px-4 py-2.5 rounded-lg text-sm font-semibold transition"
          >
            View All Schemes
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          {
            label: "Eligible Schemes",
            value: matchingCount,
            sub: "Based on your profile",
            icon: Sparkles,
            color: "text-government-blue",
            bg: "bg-government-blue/10"
          },
          {
            label: "Active Applications",
            value: appliedCount,
            sub: "Currently being processed",
            icon: ClipboardList,
            color: "text-india-green",
            bg: "bg-india-green/10"
          },
          {
            label: "Verified Documents",
            value: verifiedDocCount,
            sub: "Securely stored in vault",
            icon: ShieldCheck,
            color: "text-india-green",
            bg: "bg-india-green/10"
          },
          {
            label: "Readiness Score",
            value: `${avgSavedReadiness}%`,
            sub: "Document readiness score",
            icon: Target,
            color: "text-saffron-dark",
            bg: "bg-saffron/10"
          }
        ].map((stat, idx) => (
          <div key={idx} className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm hover:shadow-md transition">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold text-gray-500 uppercase tracking-wide">{stat.label}</span>
              <div className={`${stat.bg} ${stat.color} p-2 rounded-lg`}>
                <stat.icon className="h-4.5 w-4.5" />
              </div>
            </div>
            <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
            <p className="text-xs text-gray-500 mt-1">{stat.sub}</p>
          </div>
        ))}
      </div>

      <div className="grid md:grid-cols-3 gap-6">
        <div className="md:col-span-2 space-y-6">
          <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
            <div className="flex items-center gap-2 mb-4 pb-3 border-b border-gray-100">
              <Zap className="h-4 w-4 text-government-blue" />
              <h2 className="text-sm font-bold text-gray-900">Quick Actions</h2>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {MOCK_QUICK_ACTIONS.map((action, index) => {
                let label = action.label;
                let desc = action.description;
                const IconComponent = [Sparkles, FileText, User, ClipboardList][index % 4];
                if (action.id === "qa-recommendations") {
                  label = "Find Schemes";
                  desc = "Browse eligible government schemes";
                } else if (action.id === "qa-documents") {
                  label = "Document Vault";
                  desc = "Manage your uploaded documents";
                } else if (action.id === "qa-profile") {
                  label = "Update Profile";
                  desc = "Edit your personal details";
                } else if (action.id === "qa-tracker") {
                  label = "Track Applications";
                  desc = "View your application status";
                }
                return (
                  <Link
                    key={action.id}
                    to={action.path}
                    className="flex flex-col items-center gap-2 p-4 rounded-lg border border-gray-200 hover:border-government-blue/30 hover:shadow-md transition text-center bg-gray-50 hover:bg-white"
                  >
                    <div className="p-2.5 rounded-lg bg-government-blue text-white shadow-sm">
                      <IconComponent className="h-4.5 w-4.5" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-gray-800">{label}</p>
                      <p className="text-[10px] text-gray-500 mt-1">{desc}</p>
                    </div>
                  </Link>
                );
              })}
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-gray-100">
              <div className="flex items-center gap-2">
                <BarChart3 className="h-4 w-4 text-government-blue" />
                <h2 className="text-sm font-bold text-gray-900">Recent Activity</h2>
              </div>
              <span className="text-[10px] text-gray-400 font-semibold">Secure Audit Synced</span>
            </div>

            <div className="relative pl-4 border-l border-gray-200 space-y-4">
              {MOCK_RECENT_ACTIVITIES.map((activity) => {
                const titleMap = {
                  "Application Status Update": "Application Status Updated",
                  "Document Verified": "Document Verified",
                  "Document Uploaded": "Document Uploaded",
                  "Profile Synchronized": "Profile Updated",
                  "Scheme Bookmarked": "Scheme Saved"
                };
                const title = titleMap[activity.title] || activity.title;
                return (
                  <div key={activity.id} className="relative">
                    <div className="absolute -left-[21px] top-1.5 h-2 w-2 rounded-full bg-gray-400 ring-4 ring-white" />
                    <div className="space-y-1">
                      <div className="flex items-center justify-between gap-2">
                        <h4 className="text-xs font-bold text-gray-800">{title}</h4>
                        <span className="text-[10px] text-gray-500">
                          {new Date(activity.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <p className="text-xs text-gray-500">{activity.description}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        <div className="space-y-6">
          {topActionScheme && (
            <div className="bg-gradient-to-br from-government-blue/5 to-india-green/5 border border-government-blue/20 rounded-xl p-5 shadow-sm space-y-3">
              <div className="flex items-center gap-2">
                <Award className="h-4 w-4 text-government-blue" />
                <span className="text-xs font-bold text-government-blue uppercase tracking-wider">Top Recommendation</span>
              </div>
              <div className="space-y-1">
                <h3 className="font-bold text-gray-900 text-sm">
                  {topActionScheme.name}
                </h3>
                <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold">
                  {topActionScheme.ministry}
                </p>
              </div>
              <p className="text-xs text-gray-600 leading-relaxed">
                Based on your profile, this scheme is highly recommended. Apply today to avail benefits.
              </p>
              <div className="flex gap-2 pt-1.5">
                <Link
                  to={`/scheme/${topActionScheme.id}`}
                  className="bg-government-blue hover:bg-government-blue-dark text-white text-xs font-bold px-3 py-2 rounded-lg transition flex-1 text-center shadow-sm"
                >
                  View Scheme Details
                </Link>
                <button
                  onClick={() => handleAskAI(`Tell me why I should apply for ${topActionScheme.name}`)}
                  className="bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 p-2 rounded-lg transition shadow-sm"
                  title="Ask AI about this recommendation"
                >
                  <Bot className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          )}

          <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm space-y-3">
            <div className="flex items-center gap-2 pb-2 border-b border-gray-100">
              <Calendar className="h-4 w-4 text-red-600" />
              <h3 className="text-sm font-bold text-gray-900">Upcoming Deadlines</h3>
            </div>
            {upcomingDeadlines.length === 0 ? (
              <p className="text-xs text-gray-500 py-1">No upcoming deadlines at the moment</p>
            ) : (
              <div className="space-y-2">
                {upcomingDeadlines.map((item) => (
                  <Link
                    key={item.id}
                    to={`/scheme/${item.id}`}
                    className="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition"
                  >
                    <div className="flex-1 min-w-0 mr-2">
                      <p className="text-xs font-bold text-gray-800 truncate">{item.name}</p>
                      <p className="text-[10px] text-gray-500 mt-0.5">{item.deadline}</p>
                    </div>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full shrink-0 ${
                      item.daysLeft <= 7 ? "bg-red-100 text-red-700" : "bg-saffron/20 text-saffron-dark"
                    }`}>
                      {item.daysLeft} Days
                    </span>
                  </Link>
                ))}
              </div>
            )}
          </div>

          {missingDocsList.length > 0 && (
            <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm space-y-3">
              <div className="flex items-center gap-2 pb-2 border-b border-gray-100">
                <AlertTriangle className="h-4 w-4 text-saffron-dark" />
                <h3 className="text-sm font-bold text-gray-900">Documents Required</h3>
              </div>
              <div className="space-y-2">
                {missingDocsList.map(([docName, count], idx) => (
                  <div key={idx} className="flex items-center justify-between p-3 bg-saffron/5 border border-saffron/20 rounded-lg text-xs">
                    <span className="text-gray-700 font-medium truncate pr-2">{docName}</span>
                    <span className="text-[10px] bg-saffron/20 text-saffron-dark px-2 py-0.5 rounded font-bold shrink-0">
                      Unlocks {count} Schemes
                    </span>
                  </div>
                ))}
              </div>
              <Link
                to="/documents"
                className="w-full inline-flex items-center justify-center gap-2 bg-government-blue hover:bg-government-blue-dark text-white text-xs font-bold py-2.5 rounded-lg transition shadow-sm"
              >
                <FileText className="h-4 w-4" />
                Open Document Vault
              </Link>
            </div>
          )}

          <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm space-y-3">
            <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Account Status</h3>
            <div className="space-y-2.5 text-sm">
              <div className="flex items-center gap-2">
                <CheckCircle className="h-4 w-4 text-india-green shrink-0" />
                <span className="text-gray-700 font-medium">Onboarding Complete</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle className={`h-4 w-4 shrink-0 ${verifiedDocCount > 0 ? "text-india-green" : "text-gray-300"}`} />
                <span className={verifiedDocCount > 0 ? "text-gray-700 font-medium" : "text-gray-400"}>Documents Verified</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle className={`h-4 w-4 shrink-0 ${appliedCount > 0 ? "text-india-green" : "text-gray-300"}`} />
                <span className={appliedCount > 0 ? "text-gray-700 font-medium" : "text-gray-400"}>Applications Submitted</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <SchemeAIChatWidget
        isOpen={aiChatOpen}
        initialQuery={aiInitialQuery}
        pageContext={window.location.pathname}
        onClose={() => {
          setAiChatOpen(false);
          setAiInitialQuery("");
        }}
      />

      <button
        onClick={() => handleAskAI("Show me summary check")}
        className="fixed bottom-6 right-6 z-40 flex items-center gap-2 shadow-xl transition-all bg-government-blue hover:bg-government-blue-dark hover:scale-105 px-4 py-3 rounded-xl"
        title="Ask AI Assistant"
      >
        <MessageSquare className="h-5 w-5 text-white" />
        <span className="text-white text-sm font-semibold">AI Assistant</span>
        <span className="h-2 w-2 bg-emerald-400 rounded-full animate-pulse" />
      </button>
    </div>
  );
}
