// src/admin/pages/components/DashboardOverview.jsx
import React, { useCallback, useEffect, useState } from "react";
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

// Enterprise components
import WelcomeCard from "./WelcomeCard";
import KPICard from "./KPICard";
import AIOperationsSummary from "./AIOperationsSummary";
import PriorityQueue from "./PriorityQueue";
import QuickActions from "./QuickActions";
import RecentActivity from "./RecentActivity";
import AIAlerts from "./AIAlerts";
import NotificationDrawer from "./NotificationDrawer";

// Service functions
import {
  refreshMetrics,
  getPriorityQueue,
  getAnalyticsData,
  getAIOperationsSummary
} from "../../services/dashboardService";

export default function DashboardOverview({
  navigateToTab,
  onSelectApplication
}) {
  const { user } = useAuth();
  const {
    applications,
    schemes,
    grievances,
    documents,
    recentActivities,
    notifications,
    updateApplicationPriority
  } = useApp();

  // State
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [metrics, setMetrics] = useState({});
  const [priorityQueue, setPriorityQueue] = useState([]);
  const [analyticsData, setAnalyticsData] = useState({});
  const [aiSummary, setAiSummary] = useState({});

  const loadDashboardData = useCallback(() => {
    try {
      setLoading(true);
      const m = refreshMetrics({ applications, schemes, documents, grievances });
      setMetrics(m);
      const pq = getPriorityQueue(applications, grievances);
      setPriorityQueue(pq);
      const ad = getAnalyticsData(applications, schemes);
      setAnalyticsData(ad);
      const ais = getAIOperationsSummary(applications, documents, grievances);
      setAiSummary(ais);
    } catch (e) {
      console.error("Dashboard load error", e);
      setError(e.message || "Failed to load dashboard data");
    } finally {
      setLoading(false);
    }
  }, [applications, schemes, documents, grievances]);

  // Initial load
  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  const handleRefresh = () => {
    loadDashboardData();
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <span className="text-indigo-600">Loading dashboard…</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 bg-red-100 text-red-800 rounded">
        <p>Error loading dashboard: {error}</p>
        <button onClick={handleRefresh} className="mt-2 text-sm underline">Retry</button>
      </div>
    );
  }

  // Build KPI data from metrics
  const kpis = [
    {
      title: "Total Applications",
      value: metrics.totalApps,
      change: "+12.4%",
      isPositive: true,
      subtext: "vs last month",
      icon: FileText,
      color: "text-indigo-600 bg-indigo-50 border-indigo-100",
      sparklineData: []
    },
    {
      title: "Pending Review",
      value: metrics.pendingReviewsCount,
      change: "-5.1%",
      isPositive: true,
      subtext: "vs last week",
      icon: Clock,
      color: "text-amber-600 bg-amber-50 border-amber-100",
      sparklineData: []
    },
    {
      title: "Approved Today",
      value: metrics.approvedToday,
      change: "+15.0%",
      isPositive: true,
      subtext: "vs daily average",
      icon: CheckCircle,
      color: "text-emerald-600 bg-emerald-50 border-emerald-100",
      sparklineData: []
    },
    {
      title: "Rejected Today",
      value: metrics.rejectedToday,
      change: "-20.0%",
      isPositive: false,
      subtext: "vs daily average",
      icon: XCircle,
      color: "text-rose-600 bg-rose-50 border-rose-100",
      sparklineData: []
    },
    {
      title: "Pending Documents",
      value: metrics.pendingDocumentsCount,
      change: "-12.5%",
      isPositive: true,
      subtext: "vs yesterday",
      icon: Clock,
      color: "text-orange-600 bg-orange-50 border-orange-100",
      sparklineData: []
    },
    {
      title: "Active Schemes",
      value: metrics.activeSchemesCount,
      change: "+2",
      isPositive: true,
      subtext: "this quarter",
      icon: Users,
      color: "text-sky-600 bg-sky-50 border-sky-100",
      sparklineData: []
    },
    {
      title: "Registered Citizens",
      value: metrics.registeredCitizens,
      change: "+18.2%",
      isPositive: true,
      subtext: "vs last month",
      icon: Users,
      color: "text-violet-600 bg-violet-50 border-violet-100",
      sparklineData: []
    },
    {
      title: "Avg Processing Time",
      value: metrics.averageProcessingTime,
      change: "-1.5 Days",
      isPositive: true,
      subtext: "SLA target: 7 Days",
      icon: Clock,
      color: "text-teal-600 bg-teal-50 border-teal-100",
      sparklineData: []
    },
    {
      title: "Processing Performance",
      value: metrics.processingPerformance,
      change: "+0.8%",
      isPositive: true,
      subtext: "SLA Target: 95%",
      icon: TrendingUp,
      color: "text-emerald-600 bg-emerald-50 border-emerald-100",
      sparklineData: []
    }
  ];

  return (
    <div className="space-y-6">
      {/* Welcome Card */}
      <WelcomeCard user={user} onRefresh={handleRefresh} />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
        {kpis.map((kpi, idx) => (
          <KPICard key={idx} {...kpi} />
        ))}
      </div>

      {/* AI Operations Summary */}
      <AIOperationsSummary
        summaryData={aiSummary}
      />

      {/* Quick Actions */}
      <QuickActions onNavigate={navigateToTab} />

      {/* Split Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column */}
          <div className="lg:col-span-2 space-y-6">
            {/* Priority Queue */}
            <PriorityQueue
              data={priorityQueue}
              onReview={(app) => {
                if (onSelectApplication) onSelectApplication(app);
                navigateToTab("applications");
              }}
              onSetPriority={updateApplicationPriority}
            />
          </div>

          {/* Right Column */}
          <div className="space-y-6">
            {/* Recent Activity */}
            <RecentActivity activities={recentActivities} onViewAll={() => navigateToTab("audits")} />

            {/* AI Alerts */}
            <AIAlerts />

            {/* Notification Drawer */}
            <NotificationDrawer notifications={notifications} />
          </div>
        </div>
    </div>
  );
}

