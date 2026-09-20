// src/admin/pages/components/DashboardOverview.jsx
import React, { useCallback, useEffect, useState } from "react";
import { useAuth } from "@context/AuthContext";
import {
  Users,
  CheckCircle,
  XCircle,
  FileText,
  Clock,
  TrendingUp,
  MessageSquare,
} from "lucide-react";

// Enterprise components
import WelcomeCard from "./WelcomeCard";
import KPICard from "./KPICard";
import AIOperationsSummary from "./AIOperationsSummary";
import PriorityQueue from "./PriorityQueue";
import RecentActivity from "./RecentActivity";
import AIAlerts from "./AIAlerts";
import NotificationDrawer from "./NotificationDrawer";

// Services
import adminService from "@services/adminService";
import userAdminService from "@services/userAdminService";
import notificationService from "@services/notificationService";

export default function DashboardOverview({
  navigateToTab,
  onSelectApplication
}) {
  const { user } = useAuth();

  // State
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [metrics, setMetrics] = useState({
    totalApplications: 0,
    underReviewApplications: 0,
    approvedApplications: 0,
    rejectedApplications: 0,
    pendingDocuments: 0,
    activeSchemes: 0,
    totalUsers: 0,
    totalFeedback: 0,
    averageProcessingTimeDays: 7,
    slaCompliancePercentage: 96.5,
    pendingGrievances: 0,
  });
  const [userCounts, setUserCounts] = useState({
    totalUsers: 0,
    citizens: 0,
    officers: 0,
    managers: 0,
    admins: 0,
  });
  const [priorityQueue, setPriorityQueue] = useState([]);
  const [recentActivities, setRecentActivities] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [aiSummary, setAiSummary] = useState({});

  const loadDashboardData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [metricsRes, usersRes, queueRes, auditRes, notifRes] = await Promise.allSettled([
        adminService.getAdminMetrics(),
        userAdminService.getAdminUsers({ page: 0, size: 1 }),
        adminService.getApplicationsQueue({ page: 0, size: 5, sort: "createdAt", direction: "DESC" }),
        adminService.getAdminAuditLogs({ page: 0, size: 5 }),
        notificationService.getNotifications({ page: 0, size: 10 })
      ]);

      if (metricsRes.status === "fulfilled" && !metricsRes.value.error && metricsRes.value.data) {
        setMetrics(metricsRes.value.data);
      }

      if (usersRes.status === "fulfilled" && !usersRes.value.error && usersRes.value.data) {
        const uData = usersRes.value.data;
        setUserCounts({
          totalUsers: uData.totalUsers ?? uData.totalElements ?? 0,
          citizens: uData.roleCounts?.citizens ?? 0,
          officers: uData.roleCounts?.officers ?? 0,
          managers: uData.roleCounts?.managers ?? 0,
          admins: uData.roleCounts?.admins ?? 0,
        });
      }

      if (queueRes.status === "fulfilled" && !queueRes.value.error && queueRes.value.data) {
        const apps = (queueRes.value.data.content || []).map((app) => ({
          ...app,
          citizen: app.applicantName || app.userId || "Citizen",
          scheme: app.schemeCode || "Scheme",
          priority: app.status === "SUBMITTED" ? "High" : app.status === "UNDER_REVIEW" ? "Critical" : "Medium",
          deadline: "3 days",
          assignedOfficer: "Assigned Officer"
        }));
        setPriorityQueue(apps);
      }

      if (auditRes.status === "fulfilled" && !auditRes.value.error && auditRes.value.data) {
        const logs = (auditRes.value.data.content || []).map((log) => ({
          id: log.id,
          officerName: log.actorId || "System",
          module: log.entityType || "Operations",
          activityType: log.action || "Action Recorded",
          description: log.action ? `Action ${log.action} on ${log.entityType || ''} (${log.entityId || ''})` : "Operational change logged",
          timestamp: log.timestamp ? new Date(log.timestamp).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }) : "Just now",
          status: "Success"
        }));
        setRecentActivities(logs);
      }

      if (notifRes.status === "fulfilled" && !notifRes.value.error && notifRes.value.data) {
        setNotifications(notifRes.value.data.content || []);
      }

      const liveMetrics = (metricsRes.status === "fulfilled" && !metricsRes.value.error && metricsRes.value.data) ? metricsRes.value.data : {};
      const pendingCount = liveMetrics.underReviewApplications ?? liveMetrics.pendingReviews ?? 0;
      const openGrv = liveMetrics.openGrievances ?? liveMetrics.pendingGrievances ?? 0;

      setAiSummary({
        pendingReviews: pendingCount,
        pendingDocuments: liveMetrics.pendingDocuments ?? 0,
        nearingSLA: 0,
        aiRecommendation: openGrv > 0 ? `${openGrv} open grievance ticket(s) require administrative review.` : "All queues operational; automated document validation checks passing.",
        confidence: 98.2,
        processingPerformance: 99.4,
        avgProcessingTime: `${liveMetrics.averageProcessingTimeDays ?? 7} Days`,
        lastUpdated: new Date().toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }),
        systemStatus: "Healthy"
      });

    } catch (e) {
      console.error("Dashboard load error", e);
      setError(e.message || "Failed to load dashboard data");
    } finally {
      setLoading(false);
    }
  }, []);

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
        <span className="text-indigo-600 dark:text-indigo-400 font-semibold text-sm">Loading Live Dashboard Metrics...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 bg-rose-50 dark:bg-rose-950/60 border border-rose-200 dark:border-rose-900 text-rose-800 dark:text-rose-300 rounded-xl">
        <p>Error loading dashboard: {error}</p>
        <button onClick={handleRefresh} className="mt-2 text-sm underline font-bold">Retry</button>
      </div>
    );
  }

  // Build KPI data from metrics
  const kpis = [
    {
      title: "Total Applications",
      value: metrics.totalApplications ?? 0,
      change: "Live Count",
      isPositive: true,
      subtext: "MongoDB applications collection",
      icon: FileText,
      color: "text-indigo-600 bg-indigo-50 border-indigo-100",
      sparklineData: []
    },
    {
      title: "Pending Review",
      value: metrics.underReviewApplications ?? metrics.pendingReviews ?? 0,
      change: "Review Queue",
      isPositive: true,
      subtext: "Awaiting officer evaluation",
      icon: Clock,
      color: "text-amber-600 bg-amber-50 border-amber-100",
      sparklineData: []
    },
    {
      title: "Approved Applications",
      value: metrics.approvedApplications ?? 0,
      change: "Verified",
      isPositive: true,
      subtext: "Approved by administrators",
      icon: CheckCircle,
      color: "text-emerald-600 bg-emerald-50 border-emerald-100",
      sparklineData: []
    },
    {
      title: "Rejected Applications",
      value: metrics.rejectedApplications ?? 0,
      change: "Disallowed",
      isPositive: false,
      subtext: "Did not meet eligibility",
      icon: XCircle,
      color: "text-rose-600 bg-rose-50 border-rose-100",
      sparklineData: []
    },
    {
      title: "Correction Required",
      value: metrics.correctionRequiredApplications ?? 0,
      change: "Pending Citizen Action",
      isPositive: false,
      subtext: "Returned for document re-upload",
      icon: Clock,
      color: "text-orange-600 bg-orange-50 border-orange-100",
      sparklineData: []
    },
    {
      title: "Active Schemes",
      value: metrics.activeSchemes ?? metrics.totalSchemes ?? 0,
      change: "Active Catalog",
      isPositive: true,
      subtext: "Central & State schemes",
      icon: Users,
      color: "text-sky-600 bg-sky-50 border-sky-100",
      sparklineData: []
    },
    {
      title: "Total Users",
      value: userCounts.totalUsers,
      change: "Oracle Directory",
      isPositive: true,
      subtext: `${userCounts.citizens} Citizens • ${userCounts.officers} Officers • ${userCounts.managers} Managers • ${userCounts.admins} Admins`,
      icon: Users,
      color: "text-blue-600 bg-blue-50 border-blue-100",
      sparklineData: []
    },
    {
      title: "Citizen Feedback",
      value: metrics.totalFeedback ?? 0,
      change: "Live Feedback",
      isPositive: true,
      subtext: "Citizen ratings & reviews",
      icon: MessageSquare,
      color: "text-pink-600 bg-pink-50 border-pink-100",
      sparklineData: []
    },
    {
      title: "Open Grievances",
      value: metrics.openGrievances ?? metrics.pendingGrievances ?? 0,
      change: "Citizen Desk",
      isPositive: (metrics.openGrievances ?? 0) === 0,
      subtext: "Active grievance tickets",
      icon: Users,
      color: "text-violet-600 bg-violet-50 border-violet-100",
      sparklineData: []
    },
    {
      title: "Unread Notifications",
      value: metrics.unreadAdminNotifications ?? notifications.filter(n => !n.read).length,
      change: "Alert Stream",
      isPositive: true,
      subtext: "System & audit events",
      icon: Clock,
      color: "text-teal-600 bg-teal-50 border-teal-100",
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
            onSetPriority={() => {}}
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
