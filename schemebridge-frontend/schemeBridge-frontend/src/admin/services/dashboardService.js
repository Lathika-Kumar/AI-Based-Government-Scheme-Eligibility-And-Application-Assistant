/* src/admin/services/dashboardService.js */
// Service layer for admin dashboard data aggregation and business logic.
// All functions are pure and operate on the data stored in AppContext.
// NOTE: This file contains only mock‑derived calculations suitable for future Spring Boot + MongoDB integration.

/**
 * Compute priority for a single application based on rule set.
 * @param {object} app - Application object from context.
 * @param {Array} grievances - List of grievance objects.
 * @returns {string} Priority: 'Critical' | 'High' | 'Medium' | 'Low'
 */
export const computePriority = (app, grievances = []) => {
  if (app.manualPriority) {
    return app.manualPriority;
  }
  const hasGrievance = grievances.some(g => g.citizenName === app.applicantName);
  const daysSinceApplied = (() => {
    const applied = new Date(app.appliedDate);
    const now = new Date();
    const diff = (now - applied) / (1000 * 60 * 60 * 24);
    return Math.floor(diff);
  })();
  if (
    daysSinceApplied >= 6 ||
    (app.schemeName && app.schemeName.toLowerCase().includes('fraud')) ||
    (app.schemeName && app.schemeName.toLowerCase().includes('scholarship')) ||
    hasGrievance
  ) {
    return 'Critical';
  }
  if (
    daysSinceApplied >= 4 ||
    app.documentsPending ||
    (app.aiConfidence && app.aiConfidence < 80)
  ) {
    return 'High';
  }
  if (app.currentStage && app.currentStage.toLowerCase().includes('review')) {
    return 'Medium';
  }
  return 'Low';
};

export const getPriorityQueue = (applications = [], grievances = []) => {
  const appsWithPriority = applications.map(app => ({
    ...app,
    citizen: app.applicantName,
    scheme: app.schemeName,
    priority: computePriority(app, grievances),
    deadline: (() => {
      const days = Math.floor((new Date() - new Date(app.appliedDate)) / (1000 * 60 * 60 * 24));
      if (days < 1) return 'Today';
      if (days < 3) return 'Tomorrow';
      return `${days} days`;
    })(),
    assignedOfficer: app.assignedOfficer || 'Officer (auto‑assigned)',
  }));
  const priorityOrder = { Critical: 1, High: 2, Medium: 3, Low: 4 };
  const sorted = appsWithPriority.sort((a, b) => {
    const diff = priorityOrder[a.priority] - priorityOrder[b.priority];
    if (diff !== 0) return diff;
    const parseDeadline = d => {
      if (d === 'Today') return 0;
      if (d === 'Tomorrow') return 1;
      const m = d.match(/(\d+)/);
      return m ? parseInt(m[1], 10) : 99;
    };
    return parseDeadline(a.deadline) - parseDeadline(b.deadline);
  });
  return sorted.slice(0, 5);
};

export const getAnalyticsData = (applications = [], schemes = []) => {
  const appsByStateMap = {};
  applications.forEach(app => {
    const state = app.applicantState || 'Unknown';
    appsByStateMap[state] = (appsByStateMap[state] || 0) + 1;
  });
  const applicationsByState = Object.entries(appsByStateMap).map(([name, value]) => ({ name, value }));

  const appsByCategoryMap = {};
  applications.forEach(app => {
    const scheme = schemes.find(s => s.id === app.schemeId);
    const cat = scheme?.category || 'Other';
    appsByCategoryMap[cat] = (appsByCategoryMap[cat] || 0) + 1;
  });
  const colors = ['#4F46E5', '#2563EB', '#16A34A', '#D97706', '#9333EA'];
  const applicationsByCategory = Object.entries(appsByCategoryMap).map(([name, value], idx) => ({
    name,
    value,
    color: colors[idx % colors.length],
  }));

  const now = new Date();
  const monthLabels = [];
  const trend = [];
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const month = d.toLocaleString('en-IN', { month: 'short' });
    monthLabels.push(month);
    const appsInMonth = applications.filter(app => {
      const ad = new Date(app.appliedDate);
      return ad.getFullYear() === d.getFullYear() && ad.getMonth() === d.getMonth();
    }).length;
    const approved = applications.filter(app => app.currentStage === 'Approved' && new Date(app.appliedDate).getFullYear() === d.getFullYear() && new Date(app.appliedDate).getMonth() === d.getMonth()).length;
    const rejected = applications.filter(app => app.currentStage === 'Rejected' && new Date(app.appliedDate).getFullYear() === d.getFullYear() && new Date(app.appliedDate).getMonth() === d.getMonth()).length;
    trend.push({ month, applications: appsInMonth, approved, rejected });
  }

  const totalApps = applications.length || 1;
  const approved = applications.filter(a => a.currentStage === 'Approved').length;
  const approvalRate = (approved / totalApps) * 100;
  const avgProcessingDays = applications.reduce((sum, a) => {
    const applied = new Date(a.appliedDate);
    const now = new Date();
    const diff = (now - applied) / (1000 * 60 * 60 * 24);
    return sum + diff;
  }, 0) / totalApps;
  const processingPerformance = Math.max(0, 100 - avgProcessingDays);
  const approvalRateData = monthLabels.map(month => ({ month, rate: Math.min(100, Math.round(approvalRate + (Math.random() - 0.5) * 5)) }));

  return {
    applicationsByState,
    applicationsByCategory,
    monthlyTrendData: trend,
    approvalRateData,
    processingPerformance: Number(processingPerformance.toFixed(1)),
  };
};

export const globalSearch = (query, { applications = [], schemes = [], citizens = [], documents = [], grievances = [], officers = [] }) => {
  const term = query.trim().toLowerCase();
  if (!term) return {};
  const limit = 5;
  const filter = (arr, fields) => {
    const allMatches = arr.filter(item => fields.some(f => String(item[f] || '').toLowerCase().includes(term)));
    return {
      items: allMatches.slice(0, limit),
      hasMore: allMatches.length > limit,
      totalCount: allMatches.length
    };
  };
  return {
    applications: filter(applications, ['id', 'applicantName', 'schemeName']),
    schemes: filter(schemes, ['id', 'name', 'ministry']),
    citizens: filter(citizens, ['name', 'email', 'phone']),
    documents: filter(documents, ['name', 'type', 'issuer']),
    officers: filter(officers, ['name', 'email', 'dept']),
    grievances: filter(grievances, ['id', 'citizenName', 'category', 'relatedScheme']),
  };
};

export const getAIOperationsSummary = (applications = [], documents = [], grievances = []) => {
  const pendingReviews = applications.filter(
    (a) => a.currentStage !== "Approved" && a.currentStage !== "Rejected"
  ).length;

  const pendingDocuments = documents.filter(
    (d) => d.status !== "verified" && d.status !== "rejected"
  ).length;

  const nearingSLA = applications.filter(
    (a) => {
      if (a.currentStage === "Approved" || a.currentStage === "Rejected") return false;
      const applied = new Date(a.appliedDate);
      const diffDays = Math.floor((new Date() - applied) / (1000 * 60 * 60 * 24));
      return diffDays >= 4;
    }
  ).length;

  const totalApps = applications.length || 1;
  const averageProcessingTime = applications.reduce((sum, a) => {
    const applied = new Date(a.appliedDate);
    const diff = (new Date() - applied) / (1000 * 60 * 60 * 24);
    return sum + diff;
  }, 0) / totalApps;

  const processingPerformance = Math.max(0, 100 - averageProcessingTime);
  const confidence = 97.4;

  return {
    pendingReviews,
    pendingDocuments,
    nearingSLA,
    aiRecommendation: "Prioritize PM-KISAN applications submitted before July 1.",
    confidence,
    processingPerformance: Number(processingPerformance.toFixed(1)),
    avgProcessingTime: `${averageProcessingTime.toFixed(1)} Days`,
    lastUpdated: "Just Now",
    systemStatus: "Healthy"
  };
};

export const getMetrics = ({ applications = [], schemes = [], documents = [], grievances = [] }) => {
  const totalApps = applications.length;
  const pendingReviewsCount = applications.filter(
    (a) => a.currentStage !== "Approved" && a.currentStage !== "Rejected"
  ).length;
  const today = new Date();
  const isToday = (dateStr) => {
    const d = new Date(dateStr);
    return d.toDateString() === today.toDateString();
  };
  const approvedToday = applications.filter(
    (a) => a.currentStage === "Approved" && isToday(a.appliedDate)
  ).length;
  const rejectedToday = applications.filter(
    (a) => a.currentStage === "Rejected" && isToday(a.appliedDate)
  ).length;
  const pendingDocumentsCount = documents.filter(
    (d) => d.status !== "verified" && d.status !== "rejected"
  ).length;
  const registeredCitizens = applications.reduce((set, a) => {
    set.add(a.applicantName);
    return set;
  }, new Set()).size;
  const activeSchemesCount = schemes.filter((s) => s.status === "published").length;
  const averageProcessingTime = applications.reduce((sum, a) => {
    const applied = new Date(a.appliedDate);
    const diff = (today - applied) / (1000 * 60 * 60 * 24);
    return sum + diff;
  }, 0) / (totalApps || 1);
  const processingPerformance = Math.max(0, 100 - averageProcessingTime);

  return {
    totalApps,
    pendingReviewsCount,
    approvedToday,
    rejectedToday,
    pendingDocumentsCount,
    registeredCitizens,
    activeSchemesCount,
    averageProcessingTime: `${averageProcessingTime.toFixed(1)} Days`,
    processingPerformance: `${processingPerformance.toFixed(1)}%`,
  };
};

export const refreshMetrics = (context) => {
  return getMetrics(context);
};

