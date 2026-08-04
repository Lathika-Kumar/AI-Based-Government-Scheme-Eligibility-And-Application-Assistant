import React, { useMemo, useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { checkEligibility } from "@utils/eligibilityEngine";
import { getDocReadinessForScheme } from "@utils/documentReadiness";
import profileService from "@services/profileService";
import notificationService from "@services/notificationService";
import dashboardService from "@services/dashboardService";
import schemeService from "@services/schemeService";
import applicationService from "@services/applicationService";
import SchemeAIChatWidget from "@components/SchemeAIChatWidget";
import FirstTimeWelcomeCard from "@components/ui/FirstTimeWelcomeCard";
import { StatCardSkeleton, FormSectionSkeleton } from "@components/ui/LoadingSkeleton";
import { usePageMeta } from "@utils/usePageMeta";
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
  Target,
  Bot,
  Building2,
  ShieldCheck,
  User
} from "lucide-react";

export default function Dashboard() {
  usePageMeta("Citizen Dashboard", "Overview of eligible schemes, documents & applications");
  const { user } = useAuth();
  const { profile, schemes, applications, documents, savedSchemes } = useApp();

  const [aiChatOpen, setAiChatOpen] = useState(false);
  const [aiInitialQuery, setAiInitialQuery] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  // First-time arrival welcome card state
  const [showWelcomeCard, setShowWelcomeCard] = useState(() => {
    const seen = localStorage.getItem("schemebridge_welcome_seen");
    return !seen;
  });

  const handleDismissWelcomeCard = () => {
    localStorage.setItem("schemebridge_welcome_seen", "true");
    setShowWelcomeCard(false);
  };

  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 500);
    return () => clearTimeout(timer);
  }, []);

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return "Good morning";
    if (hour < 17) return "Good afternoon";
    return "Good evening";
  }, []);

  const evalProfile = useMemo(() => ({
    name: profile?.name || user?.fullName || user?.name || "Citizen",
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

  // Prefer backend-provided summary values when available
  const displayedMatchingCount = dashboardSummary?.eligibleSchemesCount ?? matchingCount;
  const displayedAppliedCount = (dashboardSummary?.completedApplicationsCount ?? 0) + (dashboardSummary?.pendingApplicationsCount ?? 0) || appliedCount;
  const displayedDocumentReadiness = dashboardSummary?.documentCompletionPercentage ?? avgSavedReadiness;
  const displayedProfileCompletion = dashboardSummary?.profileCompletionPercentage ?? profileCompletionScore;

  const [profileCompletionScore, setProfileCompletionScore] = useState(() => {
    const fields = ["name", "age", "annualIncome", "occupation", "caste", "gender", "state"];
    const present = fields.filter((f) => !!evalProfile?.[f]).length;
    return Math.round((present / fields.length) * 100);
  });

  // Dashboard summary from backend (optional)
  const [dashboardSummary, setDashboardSummary] = useState(null);
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [dashboardError, setDashboardError] = useState(null);

  const [recommendationCount, setRecommendationCount] = useState(null);
  const [recentSchemesList, setRecentSchemesList] = useState([]);
  const [recentApplicationsList, setRecentApplicationsList] = useState([]);

  const [recentActivities, setRecentActivities] = useState([]);
  const quickActions = [
    { id: "qa-recommendations", path: "/recommendations" },
    { id: "qa-documents", path: "/documents" },
    { id: "qa-profile", path: "/profile" },
    { id: "qa-tracker", path: "/tracker" },
  ];

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const res = await profileService.getCompletionScore(evalProfile);
        if (mounted && res && typeof res.score === "number") setProfileCompletionScore(res.score);
      } catch (err) {
        console.warn("Profile completion fetch failed, using local heuristic", err);
      }

      try {
        // Only use local recent activities when notification backend is not available
        const saved = localStorage.getItem("schemebridge_recent_activities");
        if (mounted && saved) {
          const parsed = JSON.parse(saved);
          if (Array.isArray(parsed)) setRecentActivities(parsed.slice(0, 5));
        }
      } catch (err) {
        console.warn("Reading recent activities from localStorage failed", err);
      }

      // Fetch consolidated dashboard summary (if backend exposes it)
      try {
        setDashboardLoading(true);
        setDashboardError(null);
        const summary = await dashboardService.getSummary();
        if (mounted && summary) setDashboardSummary(summary);
      } catch (err) {
        console.warn("Dashboard summary fetch failed", err);
        if (mounted) setDashboardError(err);
      } finally {
        if (mounted) setDashboardLoading(false);
      }

      // Fetch recommendations, recent schemes and applications (backend endpoints available)
      try {
        const recs = await schemeService.getRecommendations(evalProfile);
        if (mounted && recs) {
          // recs may be an object map or array depending on backend; normalize
          if (Array.isArray(recs)) setRecommendationCount(recs.length);
          else if (typeof recs === "object") setRecommendationCount(Object.keys(recs).length);
        }
      } catch (e) {
        console.warn("Fetching recommendations failed", e);
      }

      try {
        const schemesRes = await schemeService.getSchemes({ page: 0, size: 3 });
        if (mounted && schemesRes) {
          // service returns { data, total } or an array depending on implementation
          if (Array.isArray(schemesRes)) setRecentSchemesList(schemesRes.slice(0, 3));
          else if (schemesRes.data) setRecentSchemesList(schemesRes.data.slice(0, 3));
        }
      } catch (e) {
        console.warn("Fetching recent schemes failed", e);
      }

      try {
        const apps = await applicationService.getApplications();
        if (mounted && Array.isArray(apps)) {
          setRecentApplicationsList(apps.slice(0, 5));
          // Merge application events into recent activities list (non-destructive)
          const appActs = apps.slice(0, 5).map((a) => ({
            id: a.id,
            title: "Application Status Update",
            description: `${a.schemeName} — ${a.status}`,
            timestamp: a.updatedAt || a.submittedAt || new Date().toISOString(),
          }));
          setRecentActivities((prev) => {
            const merged = [...appActs, ...prev];
            // keep only latest 10
            return merged.slice(0, 10);
          });
        }
      } catch (e) {
        console.warn("Fetching recent applications failed", e);
      }
    })();
    return () => {
      mounted = false;
    };
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

  const handleRetryDashboard = async () => {
    setDashboardLoading(true);
    setDashboardError(null);
    try {
      const summary = await dashboardService.getSummary();
      setDashboardSummary(summary);
    } catch (err) {
      setDashboardError(err);
    } finally {
      setDashboardLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => <StatCardSkeleton key={i} />)}
        </div>
        <FormSectionSkeleton />
        <FormSectionSkeleton />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="h-2 bg-gradient-to-r from-saffron via-white-official to-india-green rounded-lg" />

      {/* First-Time Welcome Card */}
      {showWelcomeCard && (
        <FirstTimeWelcomeCard
          userName={evalProfile.name}
          onClose={handleDismissWelcomeCard}
        />
      )}

      <div className="bg-gradient-to-br from-government-blue via-government-blue-light to-government-blue text-white p-6 md:p-8 rounded-xl shadow-lg">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="bg-white p-2 rounded-lg shadow-md">
                <Building2 className="h-6 w-6 text-government-blue" />
              </div>
              <div>
                <span className="text-xs font-semibold text-white/80 uppercase tracking-wider">
                  Government of India Welfare Portal
                </span>
                <h1 className="text-2xl md:text-3xl font-bold tracking-tight">
                  {greeting}, {evalProfile.name}!
                </h1>
              </div>
            </div>
            <p className="text-white/90 text-sm leading-relaxed max-w-2xl">
              Welcome to your unified portal for scheme discovery, document verification, and application tracking.
            </p>
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
            <p className="text-xs text-gray-500">AI-synthesized summary of your welfare status and priorities</p>
          </div>
        </div>

        <div className="space-y-3 text-sm">
          {dashboardError && (
            <div className="mb-3 p-3 rounded bg-red-50 border border-red-100 text-red-700 flex items-center justify-between">
              <div>
                <strong>Failed to load dashboard summary.</strong>
                <div className="text-xs text-red-600">{dashboardError?.message || String(dashboardError)}</div>
              </div>
              <div>
                <button onClick={handleRetryDashboard} className="ml-3 inline-flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white px-3 py-1.5 rounded text-xs">Retry</button>
              </div>
            </div>
          )}
          <div className="flex items-start gap-2">
            <span className="text-government-blue font-bold mt-0.5">•</span>
            <p className="text-gray-700">
              You qualify for {displayedMatchingCount} government welfare schemes.
              {unappliedMatches.length > 0 ? (
                <span> You have {unappliedMatches.length} eligible schemes ready for application.</span>
              ) : (
                <span> You have submitted applications for all eligible schemes.</span>
              )}
            </p>
          </div>

          {upcomingDeadlines.length > 0 && (
            <div className="flex items-start gap-2">
              <span className="text-government-blue font-bold mt-0.5">•</span>
              <p className="text-gray-700">
                <strong className="text-red-600 font-semibold">Urgent Deadline:</strong>{" "}
                {upcomingDeadlines[0].name} application closes in {upcomingDeadlines[0].daysLeft} days ({upcomingDeadlines[0].deadline}).
              </p>
            </div>
          )}

          {missingDocsList.length > 0 && (
            <div className="flex items-start gap-2">
              <span className="text-government-blue font-bold mt-0.5">•</span>
              <p className="text-gray-700">
                Uploading your {missingDocsList[0][0]} unlocks {missingDocsList[0][1]} bookmarked schemes.
              </p>
            </div>
          )}

          <div className="flex items-start gap-2">
            <span className="text-government-blue font-bold mt-0.5">•</span>
            <p className="text-gray-700">
              Your eligibility profile is {displayedProfileCompletion}% complete.
              {displayedProfileCompletion < 100 ? " Complete remaining parameters to maximize match accuracy." : " Your profile details are fully updated."}
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
            Browse All Schemes
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          {
            label: "Eligible Schemes",
            value: displayedMatchingCount,
            sub: "Matching profile",
            icon: Sparkles,
            color: "text-government-blue",
            bg: "bg-government-blue/10"
          },
          {
            label: "Active Applications",
            value: displayedAppliedCount,
            sub: "Tracked submissions",
            icon: ClipboardList,
            color: "text-india-green",
            bg: "bg-india-green/10"
          },
          {
            label: "Verified Documents",
            value: verifiedDocCount,
            sub: "DigiLocker synced",
            icon: ShieldCheck,
            color: "text-india-green",
            bg: "bg-india-green/10"
          },
          {
            label: "Document Readiness",
            value: `${displayedDocumentReadiness}%`,
            sub: "Saved schemes average",
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
              {quickActions.map((action, index) => {
                let label = "Find Schemes";
                let desc = "Browse eligible government schemes";
                const IconComponent = [Sparkles, FileText, User, ClipboardList][index % 4];
                if (action.id === "qa-recommendations") {
                  label = "Find Schemes";
                  desc = "Browse eligible government schemes";
                } else if (action.id === "qa-documents") {
                  label = "Document Vault";
                  desc = "Manage and upload verified documents";
                } else if (action.id === "qa-profile") {
                  label = "Update Profile";
                  desc = "Edit your personal details";
                } else if (action.id === "qa-tracker") {
                  label = "Application Tracker";
                  desc = "Monitor active application statuses";
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
              <span className="text-[10px] text-gray-400 font-semibold">Encrypted Audit Log</span>
            </div>

            <div className="relative pl-4 border-l border-gray-200 space-y-4">
              {recentActivities.map((activity) => {
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
                <span className="text-xs font-bold text-government-blue uppercase tracking-wider">Top Recommended Scheme</span>
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
                Based on your socio-economic profile and document readiness score, this scheme offers maximum financial benefit and fast-track processing.
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
                  className="bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 p-2 rounded-lg transition shadow-sm cursor-pointer"
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
              <p className="text-xs text-gray-500 py-1">No urgent deadlines pending.</p>
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
                      {item.daysLeft} days left
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
                <h3 className="text-sm font-bold text-gray-900">Missing Documents Needed</h3>
              </div>
              <div className="space-y-2">
                {missingDocsList.map(([docName, count], idx) => (
                  <div key={idx} className="flex items-center justify-between p-3 bg-saffron/5 border border-saffron/20 rounded-lg text-xs">
                    <span className="text-gray-700 font-medium truncate pr-2">{docName}</span>
                    <span className="text-[10px] bg-saffron/20 text-saffron-dark px-2 py-0.5 rounded font-bold shrink-0">
                      Unlocks {count} schemes
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
            <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Account Verification Status</h3>
            <div className="space-y-2.5 text-sm">
              <div className="flex items-center gap-2">
                <CheckCircle className="h-4 w-4 text-india-green shrink-0" />
                <span className="text-gray-700 font-medium">Onboarding Profile Complete</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle className={`h-4 w-4 shrink-0 ${verifiedDocCount > 0 ? "text-india-green" : "text-gray-300"}`} />
                <span className={verifiedDocCount > 0 ? "text-gray-700 font-medium" : "text-gray-400"}>Documents Verified in Vault</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle className={`h-4 w-4 shrink-0 ${appliedCount > 0 ? "text-india-green" : "text-gray-300"}`} />
                <span className={displayedAppliedCount > 0 ? "text-gray-700 font-medium" : "text-gray-400"}>Applications Submitted</span>
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
        className="fixed bottom-6 right-6 z-40 flex items-center gap-2 shadow-xl transition-all bg-government-blue hover:bg-government-blue-dark hover:scale-105 px-4 py-3 rounded-xl cursor-pointer"
        title="Ask AI Assistant"
      >
        <MessageSquare className="h-5 w-5 text-white" />
        <span className="text-white text-sm font-semibold">AI Assistant</span>
        <span className="h-2 w-2 bg-emerald-400 rounded-full animate-pulse" />
      </button>
    </div>
  );
}
