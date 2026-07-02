import React from "react";
import { useApp } from "@context/AppContext";
import { useAuth } from "@context/AuthContext";
import {
  Users,
  CheckCircle,
  XCircle,
  FileText,
  Clock,
  TrendingUp,
  TrendingDown,
  ChevronRight
} from "lucide-react";

// New enterprise components
import WelcomeCard from "./WelcomeCard";
import KPICard from "./KPICard";
import AIOperationsSummary from "./AIOperationsSummary";
import PriorityQueue from "./PriorityQueue";
import QuickActions from "./QuickActions";
import AnalyticsPreview from "./AnalyticsPreview";
import RecentActivity from "./RecentActivity";
import AIAlerts from "./AIAlerts";


export default function DashboardOverview({
  applications,
  schemes,
  grievances,
  navigateToTab,
  onSelectApplication
}) {
  const { user } = useAuth();
  const { documents } = useApp();

  const totalApps = applications?.length || 0;
  const pendingReviewsCount = applications?.filter(a => a.currentStage !== "Approved" && a.currentStage !== "Rejected")?.length || 0;
  const approvedToday = 14;
  const rejectedToday = 3;
  const activeSchemesCount = schemes?.filter(s => s.status === "published")?.length || 0;
  const pendingDocumentsCount = documents?.filter(d => d.status !== "verified" && d.status !== "rejected").length || 8;
  const registeredCitizens = "14,802";
  const averageProcessingTime = "4.2 Days";

  const kpis = [
    {
      title: "Total Applications",
      value: totalApps + 524,
      change: "+12.4%",
      isPositive: true,
      subtext: "vs last month",
      icon: FileText,
      color: "text-indigo-600 bg-indigo-50 border-indigo-100",
      sparklineData: [40, 45, 42, 50, 48, 55, 52, 60, 58, 65]
    },
    {
      title: "Pending Review",
      value: pendingReviewsCount,
      change: "-5.1%",
      isPositive: true,
      subtext: "vs last week",
      icon: Clock,
      color: "text-amber-600 bg-amber-50 border-amber-100",
      sparklineData: [50, 48, 45, 42, 40, 38, 35, 32, 30, 28]
    },
    {
      title: "Approved Today",
      value: approvedToday,
      change: "+15.0%",
      isPositive: true,
      subtext: "vs daily average",
      icon: CheckCircle,
      color: "text-emerald-600 bg-emerald-50 border-emerald-100",
      sparklineData: [8, 10, 9, 11, 12, 10, 13, 14, 12, 14]
    },
    {
      title: "Rejected Today",
      value: rejectedToday,
      change: "-20.0%",
      isPositive: true,
      subtext: "vs daily average",
      icon: XCircle,
      color: "text-rose-600 bg-rose-50 border-rose-100",
      sparklineData: [5, 4, 6, 5, 4, 3, 4, 3, 3, 3]
    },
    {
      title: "Pending Documents",
      value: pendingDocumentsCount,
      change: "-12.5%",
      isPositive: true,
      subtext: "vs yesterday",
      icon: Clock,
      color: "text-orange-600 bg-orange-50 border-orange-100",
      sparklineData: [15, 14, 13, 12, 11, 10, 9, 8, 8, 8]
    },
    {
      title: "Active Schemes",
      value: activeSchemesCount,
      change: "+2",
      isPositive: true,
      subtext: "this quarter",
      icon: Users,
      color: "text-sky-600 bg-sky-50 border-sky-100",
      sparklineData: [5, 6, 6, 7, 7, 8, 8, 8, 9, 9]
    },
    {
      title: "Registered Citizens",
      value: registeredCitizens,
      change: "+18.2%",
      isPositive: true,
      subtext: "vs last month",
      icon: Users,
      color: "text-violet-600 bg-violet-50 border-violet-100",
      sparklineData: [12000, 12500, 13000, 13500, 13800, 14000, 14200, 14500, 14700, 14802]
    },
    {
      title: "Avg Processing Time",
      value: averageProcessingTime,
      change: "-1.5 Days",
      isPositive: true,
      subtext: "SLA target: 7 Days",
      icon: Clock,
      color: "text-teal-600 bg-teal-50 border-teal-100",
      sparklineData: [6.5, 6.2, 5.8, 5.5, 5.2, 5.0, 4.8, 4.5, 4.3, 4.2]
    }
  ];

  return (
    <div className="space-y-6">
      {/* Welcome Card */}
      <WelcomeCard user={user} />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {kpis.map((kpi, idx) => (
          <KPICard key={idx} {...kpi} />
        ))}
      </div>

      {/* AI Operations Summary */}
      <AIOperationsSummary 
        pendingReviews={pendingReviewsCount} 
        pendingDocuments={pendingDocumentsCount} 
      />

      {/* Quick Actions */}
      <QuickActions onNavigate={navigateToTab} />

      {/* Split Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column */}
        <div className="lg:col-span-2 space-y-6">
          {/* Priority Queue */}
          <PriorityQueue onReview={(app) => {
            if (onSelectApplication) onSelectApplication(app);
            navigateToTab("applications");
          }} />

          {/* Analytics Preview */}
          <AnalyticsPreview />
        </div>

        {/* Right Column */}
        <div className="space-y-6">
          {/* Recent Activity */}
          <RecentActivity onViewAll={() => navigateToTab("audits")} />

          {/* AI Alerts */}
          <AIAlerts />
        </div>
      </div>
    </div>
  );
}
