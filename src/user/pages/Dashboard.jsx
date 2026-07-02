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
  Bot
} from "lucide-react";

export default function Dashboard() {
  const { user } = useAuth();
  const { profile, schemes, applications, documents, savedSchemes, language, changeLanguage, t } = useApp();

  const [aiChatOpen, setAiChatOpen] = useState(false);
  const [aiInitialQuery, setAiInitialQuery] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  // Simulate async data loading (replace with real service call in backend integration)
  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 800);
    return () => clearTimeout(timer);
  }, []);

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return t("dash_greeting_morning");
    if (hour < 17) return t("dash_greeting_afternoon");
    return t("dash_greeting_evening");
  }, [t]);

  // Compute profile values reactively
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

  // Average doc readiness across saved schemes
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

  // Unapplied matching schemes
  const unappliedMatches = useMemo(() =>
    eligibleSchemes.filter(s => !applications.some(a => a.schemeId === s.id)),
  [eligibleSchemes, applications]);

  // Top action scheme
  const topActionScheme = unappliedMatches[0] || schemes.find(s => s.id === "pm-kisan");

  // Missing documents list across all matching/saved schemes
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

  // Upcoming simulated deadlines
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

  // ── Skeleton loading state ────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="space-y-6">
        {/* Header skeleton */}
        <div className="bg-white border border-slate-200/80 p-6 rounded-2xl shadow-xs animate-pulse">
          <div className="h-3 w-24 bg-slate-200 rounded mb-2" />
          <div className="h-7 w-56 bg-slate-200 rounded mb-1.5" />
          <div className="h-2.5 w-72 bg-slate-100 rounded" />
        </div>
        {/* AI brief skeleton */}
        <AIBriefSkeleton />
        {/* Stats skeleton */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => <StatCardSkeleton key={i} />)}
        </div>
        {/* Activity skeleton */}
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs space-y-3">
          <div className="h-3 w-40 bg-slate-200 rounded animate-pulse" />
          {[1, 2, 3].map((i) => <ListRowSkeleton key={i} />)}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Dynamic Header & Personalized Greeting */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white border border-slate-200/80 p-6 rounded-2xl shadow-xs">
        <div>
          <span className="text-[10px] bg-indigo-50 border border-indigo-200/50 text-indigo-700 px-2 py-0.5 rounded font-bold uppercase tracking-wider">
            {t("dash_citizen_dashboard") || "Citizen Dashboard"}
          </span>
          <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight mt-1.5">
            {greeting}, {evalProfile.name}!
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {t("dash_daily_brief_subtitle") || "Welcome back. Here is your personalized e-governance daily status brief."}
          </p>
        </div>

        {/* Inline Language Selector */}
        <div className="flex items-center gap-2 border border-slate-200 bg-slate-50/50 p-1.5 rounded-xl self-start sm:self-auto shrink-0 shadow-inner">
          <Globe className="h-3.5 w-3.5 text-slate-400 ml-1.5 shrink-0" />
          <select
            value={language}
            onChange={(e) => changeLanguage(e.target.value)}
            className="bg-transparent border-none text-slate-700 text-xs font-bold focus:outline-none cursor-pointer pr-5 py-0.5 pl-0"
            aria-label="Toggle Dashboard Language"
          >
            <option value="en">English</option>
            <option value="hi">हिंदी (Hindi)</option>
            <option value="ta">தமிழ் (Tamil)</option>
            <option value="te">తెలుగు (Telugu)</option>
            <option value="kn">ಕನ್ನಡ (Kannada)</option>
          </select>
        </div>
      </div>

      {/* AI Summary Section: "What should the citizen do today?" */}
      <div className="relative overflow-hidden bg-slate-900 border border-slate-800 text-white rounded-2xl p-6 shadow-md">
        {/* Visual background details */}
        <div className="absolute top-0 right-0 w-80 h-80 bg-gradient-to-br from-indigo-500/10 to-violet-500/10 rounded-full blur-2xl -translate-y-1/3 translate-x-1/4 pointer-events-none" />

        <div className="relative space-y-4">
          <div className="flex items-center justify-between border-b border-white/10 pb-3">
            <div className="flex items-center gap-2">
              <Bot className="h-5 w-5 text-indigo-400 animate-pulse" />
              <h2 className="text-xs font-extrabold uppercase tracking-widest text-indigo-300">
                {t("dash_action_brief_title") || "SchemeAI Daily Action Brief"}
              </h2>
            </div>
            <span className="text-[10px] bg-white/10 text-indigo-200 border border-white/10 px-2 py-0.5 rounded-full font-bold select-none">
              {t("dash_ai_generated") || "AI Generated"}
            </span>
          </div>

          <div className="space-y-3">
            <p className="text-sm font-bold text-slate-100 leading-snug">
              {t("dash_focus_today") || "\"What should you focus on today?\""}
            </p>
            <div className="text-xs text-slate-300 space-y-2.5 leading-relaxed">
              <div className="flex gap-2">
                <span className="text-indigo-400 font-extrabold">✦</span>
                <p>
                  {t("dash_eligible_matches_summary", { count: matchingCount })}
                  {unappliedMatches.length > 0 ? (
                    <span> {t("dash_active_ready_summary", { count: unappliedMatches.length })}</span>
                  ) : (
                    <span> {t("dash_applied_all_summary")}</span>
                  )}
                </p>
              </div>

              {upcomingDeadlines.length > 0 && (
                <div className="flex gap-2">
                  <span className="text-indigo-400 font-extrabold">✦</span>
                  <p>
                    <strong className="text-rose-300 font-semibold">{t("dash_priority_action") || "Priority Action:"}</strong>{" "}
                    {t("dash_deadline_alert", { name: upcomingDeadlines[0].name, days: upcomingDeadlines[0].daysLeft, date: upcomingDeadlines[0].deadline })}
                  </p>
                </div>
              )}

              {missingDocsList.length > 0 && (
                <div className="flex gap-2">
                  <span className="text-indigo-400 font-extrabold">✦</span>
                  <p>
                    {t("dash_missing_doc_unlock", { doc: missingDocsList[0][0], count: missingDocsList[0][1] })}
                  </p>
                </div>
              )}

              <div className="flex gap-2">
                <span className="text-indigo-400 font-extrabold">✦</span>
                <p>
                  {t("dash_profile_complete_status", { pct: profileCompletionScore })}{" "}
                  <strong className="text-emerald-400 font-semibold">{profileCompletionScore}%</strong>{" "}
                  {profileCompletionScore < 100 ? t("dash_profile_complete_education") : t("dash_profile_complete_max")}
                </p>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap gap-2.5 pt-2 border-t border-white/5">
            <button
              onClick={() => handleAskAI("Summary check: Tell me about my best matches and what to do next.")}
              className="inline-flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-500 text-white px-3.5 py-2 rounded-xl text-xs font-bold shadow-xs hover:shadow transition"
            >
              <MessageSquare className="h-3.5 w-3.5" />
              {t("dash_ask_ai_plan") || "Ask AI For Daily Plan"}
            </button>
            <Link
              to="/recommendations"
              className="inline-flex items-center gap-1.5 bg-white/10 hover:bg-white/15 border border-white/10 text-white px-3.5 py-2 rounded-xl text-xs font-bold transition"
            >
              {t("dash_submit_file") || "Submit Application File"}
              <ArrowRight className="h-3 w-3" />
            </Link>
          </div>
        </div>
      </div>

      {/* KPI Stats Counters */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          {
            label: t("dash_eligible_schemes_card") || "Eligible Schemes",
            value: matchingCount,
            sub: t("dash_demographic_matches") || "Demographic matches",
            icon: Sparkles,
            color: "text-indigo-600",
            bg: "bg-indigo-50",
            border: "border-indigo-100"
          },
          {
            label: t("dash_active_files_card") || "Active Files",
            value: appliedCount,
            sub: t("dash_in_progress") || "In progress",
            icon: ClipboardList,
            color: "text-violet-600",
            bg: "bg-violet-50",
            border: "border-violet-100"
          },
          {
            label: t("dash_verified_vault_card") || "Verified Vault Documents",
            value: verifiedDocCount,
            sub: t("dash_securely_verified") || "Securely verified",
            icon: FileText,
            color: "text-emerald-600",
            bg: "bg-emerald-50",
            border: "border-emerald-100"
          },
          {
            label: t("dash_average_readiness_card") || "Average Readiness",
            value: `${avgSavedReadiness}%`,
            sub: t("dash_doc_match_strength") || "Document match strength",
            icon: Target,
            color: "text-amber-600",
            bg: "bg-amber-50",
            border: "border-amber-100"
          }
        ].map((stat, idx) => (
          <div key={idx} className={`bg-white border border-slate-200 rounded-2xl p-4 shadow-xs border-b-2 hover:shadow transition ${stat.border}`}>
            <div className="flex items-center justify-between mb-2">
              <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest leading-none">{stat.label}</span>
              <div className={`${stat.bg} ${stat.color} p-1.5 rounded-xl border border-white`}>
                <stat.icon className="h-4 w-4" />
              </div>
            </div>
            <p className="text-2xl font-black text-slate-900 tracking-tight">{stat.value}</p>
            <p className="text-[10px] text-slate-400 mt-0.5 leading-none">{stat.sub}</p>
          </div>
        ))}
      </div>

      {/* Main Grid Section */}
      <div className="grid md:grid-cols-3 gap-6">

        {/* Left 2 Columns: Activity, Actions, and Overview */}
        <div className="md:col-span-2 space-y-6">

          {/* Quick Actions Panel */}
          <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
            <div className="flex items-center gap-2 mb-4 border-b border-slate-100 pb-3">
              <Zap className="h-4 w-4 text-indigo-600" />
              <h2 className="text-xs font-bold text-slate-900 uppercase tracking-widest">{t("dash_quick_links_title")}</h2>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {MOCK_QUICK_ACTIONS.map((action) => {
                let label = action.label;
                let desc = action.description;
                if (action.id === "qa-recommendations") {
                  label = t("dash_find_schemes") || t("nav_schemes") || action.label;
                  desc = t("rec_desc") ? t("rec_desc").slice(0, 30) + "..." : action.description;
                } else if (action.id === "qa-documents") {
                  label = t("nav_vault") || action.label;
                  desc = t("vault_desc") ? t("vault_desc").slice(0, 30) + "..." : action.description;
                } else if (action.id === "qa-profile") {
                  label = t("dash_update_profile") || action.label;
                  desc = t("profile_subtitle") ? t("profile_subtitle").slice(0, 30) + "..." : action.description;
                } else if (action.id === "qa-tracker") {
                  label = t("nav_tracker") || action.label;
                  desc = t("tracker_desc") ? t("tracker_desc").slice(0, 30) + "..." : action.description;
                }
                return (
                  <Link
                    key={action.id}
                    to={action.path}
                    className="group flex flex-col items-center gap-2 p-4 rounded-xl border border-slate-200 hover:border-slate-350 hover:shadow-xs transition text-center bg-slate-50/50 hover:bg-white"
                  >
                    <div className={`p-2.5 rounded-xl text-white bg-gradient-to-br ${action.color} group-hover:scale-105 transition-transform shadow-xs`}>
                      <Sparkles className="h-4 w-4" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-slate-800 leading-tight">{label}</p>
                      <p className="text-[9px] text-slate-400 mt-1 leading-tight">{desc}</p>
                    </div>
                  </Link>
                );
              })}
            </div>
          </div>

          {/* Recent Activity Timeline */}
          <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
            <div className="flex items-center justify-between mb-4 border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <BarChart3 className="h-4 w-4 text-indigo-600" />
                <h2 className="text-xs font-bold text-slate-900 uppercase tracking-widest">{t("dash_activity_log_title")}</h2>
              </div>
              <span className="text-[10px] text-slate-400 font-bold">{t("dash_secure_audit_synced")}</span>
            </div>

            <div className="relative pl-4 border-l border-slate-100 space-y-4">
              {MOCK_RECENT_ACTIVITIES.map((activity) => {
                const titleMap = {
                  "Application Status Update": t("tracker_current_stage") || "Application Status Update",
                  "Document Verified": t("vault_status_verified_badge") || "Document Verified",
                  "Document Uploaded": t("vault_status_uploaded_badge") || "Document Uploaded",
                  "Profile Synchronized": t("profile_tab_wizard") || "Profile Synchronized",
                  "Scheme Bookmarked": t("nav_tracker") || "Scheme Bookmarked"
                };
                const title = titleMap[activity.title] || activity.title;
                return (
                  <div key={activity.id} className="relative group">
                    {/* Timeline bullet */}
                    <div className="absolute -left-[21px] top-1.5 h-2 w-2 rounded-full bg-slate-400 group-hover:bg-indigo-600 ring-4 ring-white transition" />

                    <div className="space-y-0.5">
                      <div className="flex items-center justify-between gap-2">
                        <h4 className="text-xs font-bold text-slate-800">{title}</h4>
                        <span className="text-[9px] text-slate-400">
                          {new Date(activity.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 leading-normal">{activity.description}</p>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-[8px] bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded font-mono">
                          IP: {activity.ip}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Right Column: Widgets */}
        <div className="space-y-6">

          {/* Recommended Action Card */}
          {topActionScheme && (
            <div className="bg-gradient-to-br from-indigo-50 to-violet-50/50 border border-indigo-200 rounded-2xl p-5 shadow-xs space-y-3">
              <div className="flex items-center gap-1.5">
                <Award className="h-4 w-4 text-indigo-600" />
                <span className="text-[9px] font-bold text-indigo-700 uppercase tracking-wider">{t("dash_top_recommendation")}</span>
              </div>

              <div className="space-y-1">
                <h3 className="font-extrabold text-slate-900 text-sm leading-snug">
                  {topActionScheme.name}
                </h3>
                <p className="text-[10px] text-slate-400 uppercase tracking-wider font-semibold">
                  {topActionScheme.ministry}
                </p>
              </div>

              <p className="text-xs text-slate-650 text-slate-600 leading-relaxed font-medium">
                {t("dash_top_rec_desc")}
              </p>

              <div className="flex gap-2 pt-1.5">
                <Link
                  to={`/scheme/${topActionScheme.id}`}
                  className="bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold px-3 py-2 rounded-xl transition flex-1 text-center"
                >
                  {t("dash_file_app")}
                </Link>
                <button
                  onClick={() => handleAskAI(`Tell me why I should apply for ${topActionScheme.name}`)}
                  className="bg-white border border-slate-200 hover:bg-slate-50 text-slate-705 text-slate-700 p-2 rounded-xl transition"
                  title="Ask AI about this recommendation"
                >
                  <Bot className="h-3.5 w-3.5 animate-pulse" />
                </button>
              </div>
            </div>
          )}

          {/* Upcoming Deadlines Widget */}
          <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs space-y-3">
            <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
              <Calendar className="h-4 w-4 text-rose-500" />
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-widest">{t("dash_upcoming_deadlines")}</h3>
            </div>

            {upcomingDeadlines.length === 0 ? (
              <p className="text-xs text-slate-400 py-1">{t("dash_no_deadlines")}</p>
            ) : (
              <div className="space-y-2">
                {upcomingDeadlines.map((item) => (
                  <Link
                    key={item.id}
                    to={`/scheme/${item.id}`}
                    className="flex items-center justify-between p-2 border border-slate-100 rounded-xl hover:bg-slate-50 hover:border-slate-200 transition group"
                  >
                    <div className="flex-1 min-w-0 mr-2">
                      <p className="text-xs font-bold text-slate-805 text-slate-800 truncate group-hover:text-indigo-600">{item.name}</p>
                      <p className="text-[10px] text-slate-405 text-slate-400 mt-0.5">{item.deadline}</p>
                    </div>
                    <span className={`text-[9px] font-black px-2 py-0.5 rounded-full shrink-0 ${
                      item.daysLeft <= 7 ? "bg-rose-100 text-rose-700" : "bg-amber-100 text-amber-700"
                    }`}>
                      {t("dash_days_left", { n: item.daysLeft })}
                    </span>
                  </Link>
                ))}
              </div>
            )}
          </div>

          {/* Missing Documents Checklist */}
          {missingDocsList.length > 0 && (
            <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs space-y-3">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                <AlertTriangle className="h-4 w-4 text-amber-500" />
                <h3 className="text-xs font-bold text-slate-900 uppercase tracking-widest">{t("dash_missing_docs_title")}</h3>
              </div>

              <div className="space-y-2">
                {missingDocsList.map(([docName, count], idx) => (
                  <div key={idx} className="flex items-center justify-between p-2 bg-amber-50/50 border border-amber-100 rounded-xl text-xs">
                    <span className="text-slate-700 font-medium truncate pr-2">{docName}</span>
                    <span className="text-[10px] bg-amber-100 text-amber-800 border border-amber-200 px-2 py-0.5 rounded font-bold shrink-0">
                      {t("dash_unlocks_saved", { count })}
                    </span>
                  </div>
                ))}
              </div>

              <Link
                to="/documents"
                className="w-full inline-flex items-center justify-center gap-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold py-2 rounded-xl transition"
              >
                <FileText className="h-4 w-4" />
                {t("dash_open_vault_btn")}
              </Link>
            </div>
          )}

          {/* Scheme Bridge Achievement Widget */}
          <div className="bg-gradient-to-br from-slate-50 to-indigo-50/20 border border-slate-200 rounded-2xl p-4 shadow-xs space-y-3">
            <h3 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{t("dash_account_status_overview")}</h3>
            <div className="space-y-2 text-xs">
              <div className="flex items-center gap-2">
                <CheckCircle className="h-4 w-4 text-emerald-600 shrink-0" />
                <span className="text-slate-700">{t("dash_status_onboarding")}</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle className={`h-4 w-4 shrink-0 ${verifiedDocCount > 0 ? "text-emerald-600" : "text-slate-350"}`} />
                <span className={verifiedDocCount > 0 ? "text-slate-700" : "text-slate-400"}>{t("dash_status_digilocker")}</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle className={`h-4 w-4 shrink-0 ${appliedCount > 0 ? "text-emerald-600" : "text-slate-350"}`} />
                <span className={appliedCount > 0 ? "text-slate-700" : "text-slate-450 text-slate-400"}>{t("dash_status_subsidy")}</span>
              </div>
            </div>
          </div>

        </div>
      </div>

      {/* Reusable SchemeAIChatWidget Floating Container */}
      <SchemeAIChatWidget
        isOpen={aiChatOpen}
        initialQuery={aiInitialQuery}
        pageContext={window.location.pathname}
        onClose={() => {
          setAiChatOpen(false);
          setAiInitialQuery("");
        }}
      />

      {/* Floating Widget Button */}
      <button
        onClick={() => handleAskAI("Show me summary check")}
        className="fixed bottom-6 right-6 z-40 group flex items-center gap-2 shadow-2xl transition-all duration-300 bg-slate-900 hover:bg-slate-800 hover:scale-105 px-4 py-3 rounded-2xl hover:shadow-indigo-650/40"
        title="Ask SchemeAI Assistant"
      >
        <MessageSquare className="h-5 w-5 text-white" />
        <span className="text-white text-xs font-bold">{t("dash_ask_schemeai")}</span>
        <span className="h-2 w-2 bg-emerald-400 rounded-full animate-pulse" />
      </button>

    </div>
  );
}
