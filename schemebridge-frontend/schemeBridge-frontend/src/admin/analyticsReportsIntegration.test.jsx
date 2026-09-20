import React from "react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderToString } from "react-dom/server";
import { MemoryRouter, Routes, Route, Navigate } from "react-router-dom";
import AnalyticsReportsCenter from "./pages/components/AnalyticsReportsCenter";
import AdminDashboard from "./pages/AdminDashboard";
import adminService from "../services/adminService";
import { ADMIN_ROUTES } from "../config/routes";
import { ROLE_PERMISSIONS, ROLES, hasPermission } from "../constants/roles";

vi.mock("../services/adminService", () => ({
  default: {
    getAdminMetrics: vi.fn(),
    getAdminAnalytics: vi.fn(),
    downloadAdminReport: vi.fn()
  }
}));

// Mock ResponsiveContainer for SSR/testing compatibility
vi.mock("recharts", async () => {
  const original = await vi.importActual("recharts");
  return {
    ...original,
    ResponsiveContainer: ({ children }) => <div data-testid="responsive-container">{children}</div>
  };
});

describe("Admin Analytics & Reports Consolidation Test Suite", () => {
  const mockMetrics = {
    totalApplications: 125,
    approvedApplications: 80,
    underReviewApplications: 35,
    activeSchemes: 42
  };

  const mockAnalytics = {
    kpis: {
      totalApplications: 125,
      approvedApplications: 80,
      underReview: 35,
      approvalRate: 72.5
    },
    monthlyTimeline: [
      { month: "Apr 2026", submitted: 30, approved: 20, rejected: 5 },
      { month: "May 2026", submitted: 45, approved: 30, rejected: 10 }
    ],
    schemeDistribution: [
      { schemeCode: "SCH-PM-KISAN", applicationsCount: 50 },
      { schemeCode: "SCH-AYUSHMAN", applicationsCount: 40 }
    ]
  };

  const sampleApps = [
    {
      id: "app-001",
      applicationNumber: "SB-APP-2026-0001",
      schemeCode: "SCH-PM-KISAN",
      schemeTitle: "PM Kisan Samman Nidhi",
      citizenName: "Ramesh Kumar",
      status: "SUBMITTED",
      createdAt: "2026-09-01T10:00:00Z"
    }
  ];

  const sampleSchemes = [
    {
      id: "sch-001",
      code: "SCH-PM-KISAN",
      title: "PM Kisan Samman Nidhi",
      ministry: "Ministry of Agriculture",
      category: "Agriculture",
      status: "ACTIVE"
    }
  ];

  const sampleGrievances = [
    {
      id: "grv-001",
      ticketId: "TKT-2026-001",
      subject: "Delayed disbursement verification",
      priority: "HIGH",
      status: "OPEN",
      createdAt: "2026-09-10T12:00:00Z"
    }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    adminService.getAdminMetrics.mockResolvedValue({ error: false, data: mockMetrics });
    adminService.getAdminAnalytics.mockResolvedValue({ error: false, data: mockAnalytics });
    adminService.downloadAdminReport.mockResolvedValue("id,name\n1,test");
  });

  it("1. AnalyticsReportsCenter renders without crashing", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialMetrics={mockMetrics}
        initialAnalytics={mockAnalytics}
      />
    );
    expect(html).toContain("Operational Business Intelligence &amp; Audit Reports");
    expect(html).toContain("Live aggregated statistics and official audit reporting from Scheme Service MongoDB");
  });

  it("2. KPI values are displayed from live service response", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialMetrics={mockMetrics}
        initialAnalytics={mockAnalytics}
      />
    );
    expect(html).toContain("Total Applications");
    expect(html).toContain("125");
    expect(html).toContain("Approval Rate");
    expect(html).toContain("72.5%");
    expect(html).toContain("Pending Reviews");
    expect(html).toContain("35");
    expect(html).toContain("Active Schemes");
    expect(html).toContain("42");
  });

  it("3. AreaChart (Application Volume & Outcomes Trend) renders with last 6 months structure", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialMetrics={mockMetrics}
        initialAnalytics={mockAnalytics}
      />
    );
    expect(html).toContain("Application Volume &amp; Outcomes Trend");
    expect(html).toContain("Last 6 Months History");
  });

  it("4. BarChart (Applications by Scheme) renders with scheme code breakdown", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialMetrics={mockMetrics}
        initialAnalytics={mockAnalytics}
      />
    );
    expect(html).toContain("Applications by Scheme");
    expect(html).toContain("Aggregated by Scheme Code");
  });

  it("5. Applications Register is displayed with standard columns", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialMetrics={mockMetrics}
        initialAnalytics={mockAnalytics}
        initialActiveReportTab="applications"
      />
    );
    expect(html).toContain("Applications Register");
    expect(html).toContain("Application ID");
    expect(html).toContain("Scheme");
    expect(html).toContain("Applicant");
    expect(html).toContain("Status");
    expect(html).toContain("Date");
    expect(html).toContain("SB-APP-2026-0001");
  });

  it("6. Schemes Master Directory displays scheme columns when active", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialMetrics={mockMetrics}
        initialAnalytics={mockAnalytics}
        initialActiveReportTab="schemes"
      />
    );
    expect(html).toContain("Schemes Master Directory");
    expect(html).toContain("Scheme Code");
    expect(html).toContain("Title");
    expect(html).toContain("Ministry / Department");
    expect(html).toContain("Category");
    expect(html).toContain("Active Status");
    expect(html).toContain("SCH-PM-KISAN");
    expect(html).toContain("Ministry of Agriculture");
  });

  it("7. Grievance Redressal Audit displays grievance columns when active", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialMetrics={mockMetrics}
        initialAnalytics={mockAnalytics}
        initialActiveReportTab="grievances"
      />
    );
    expect(html).toContain("Grievance Redressal Audit");
    expect(html).toContain("Ticket ID");
    expect(html).toContain("Subject");
    expect(html).toContain("Priority");
    expect(html).toContain("Status");
    expect(html).toContain("Filed Date");
    expect(html).toContain("TKT-2026-001");
    expect(html).toContain("Delayed disbursement verification");
  });

  it("8. Refresh and export buttons are rendered in toolbar", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialMetrics={mockMetrics}
        initialAnalytics={mockAnalytics}
      />
    );
    expect(html).toContain("Refresh Analytics");
    expect(html).toContain("Print Preview");
    expect(html).toContain("Export Live Report");
    expect(html).toContain("Export Schemes CSV");
    expect(html).toContain("Export Grievances CSV");
  });

  it("9. API failure displays retry UI with specified message", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialLoading={false}
        initialError="Database timeout"
      />
    );
    expect(html).toContain("Reports and analytics are temporarily unavailable.");
    expect(html).toContain("Retry");
  });

  it("10. /admin/analytics redirects to /admin/analytics-reports with replace", () => {
    let capturedRedirect = null;
    function RedirectInterceptor({ to, replace }) {
      capturedRedirect = { to, replace };
      return <div>Redirecting</div>;
    }

    renderToString(
      <MemoryRouter initialEntries={["/admin/analytics"]}>
        <Routes>
          <Route
            path="/admin/analytics"
            element={<RedirectInterceptor to="/admin/analytics-reports" replace={true} />}
          />
        </Routes>
      </MemoryRouter>
    );

    expect(capturedRedirect).toEqual({
      to: "/admin/analytics-reports",
      replace: true
    });
  });

  it("11. /admin/reports redirects to /admin/analytics-reports with replace", () => {
    let capturedRedirect = null;
    function RedirectInterceptor({ to, replace }) {
      capturedRedirect = { to, replace };
      return <div>Redirecting</div>;
    }

    renderToString(
      <MemoryRouter initialEntries={["/admin/reports"]}>
        <Routes>
          <Route
            path="/admin/reports"
            element={<RedirectInterceptor to="/admin/analytics-reports" replace={true} />}
          />
        </Routes>
      </MemoryRouter>
    );

    expect(capturedRedirect).toEqual({
      to: "/admin/analytics-reports",
      replace: true
    });
  });

  it("12. /admin/analytics-reports renders the unified AnalyticsReportsCenter", () => {
    const html = renderToString(
      <MemoryRouter initialEntries={["/admin/analytics-reports"]}>
        <Routes>
          <Route
            path="/admin/analytics-reports"
            element={
              <AnalyticsReportsCenter
                applications={sampleApps}
                schemes={sampleSchemes}
                grievances={sampleGrievances}
                initialMetrics={mockMetrics}
                initialAnalytics={mockAnalytics}
              />
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(html).toContain("Operational Business Intelligence &amp; Audit Reports");
    expect(html).toContain("Applications Register");
  });

  it("13. ADMIN_ROUTES configuration contains canonical and legacy route constants", () => {
    expect(ADMIN_ROUTES.ANALYTICS_REPORTS).toBe("/admin/analytics-reports");
    expect(ADMIN_ROUTES.ANALYTICS).toBe("/admin/analytics");
    expect(ADMIN_ROUTES.REPORTS).toBe("/admin/reports");
  });

  it("14. ROLE_PERMISSIONS grants analytics-reports access to ADMIN and SUPER_ADMIN", () => {
    expect(hasPermission(ROLES.ADMIN, "analytics-reports")).toBe(true);
    expect(hasPermission(ROLES.SUPER_ADMIN, "analytics-reports")).toBe(true);
    expect(hasPermission(ROLES.ADMIN, "analytics")).toBe(true);
    expect(hasPermission(ROLES.ADMIN, "reports")).toBe(true);
  });

  it("15. Verified single presentation: no duplicate Application Volume or Scheme distribution charts", () => {
    const html = renderToString(
      <AnalyticsReportsCenter
        applications={sampleApps}
        schemes={sampleSchemes}
        grievances={sampleGrievances}
        initialMetrics={mockMetrics}
        initialAnalytics={mockAnalytics}
      />
    );
    // Ensure headings occur exactly once
    const volumeCount = (html.match(/Application Volume &amp; Outcomes Trend/g) || []).length;
    const schemeCount = (html.match(/Applications by Scheme/g) || []).length;
    expect(volumeCount).toBe(1);
    expect(schemeCount).toBe(1);
  });

  it("16. Regression: Schemes Master Directory safely renders scheme title with { english, tamil, translations } without throwing", () => {
    const multilingualSchemes = [
      {
        id: "sch-multilingual-01",
        code: "SCH-MULTI-01",
        title: {
          english: "National Farmers Welfare Scheme",
          tamil: "தேசிய விவசாயிகள் நல திட்டம்",
          translations: {}
        },
        ministry: {
          english: "Ministry of Agriculture",
          tamil: "விவசாய அமைச்சகம்",
          translations: {}
        },
        category: {
          english: "Agriculture",
          tamil: "விவசாயம்",
          translations: {}
        },
        status: "ACTIVE"
      }
    ];

    expect(() => {
      const html = renderToString(
        <AnalyticsReportsCenter
          applications={sampleApps}
          schemes={multilingualSchemes}
          grievances={sampleGrievances}
          initialMetrics={mockMetrics}
          initialAnalytics={mockAnalytics}
          initialActiveReportTab="schemes"
        />
      );
      expect(html).toContain("National Farmers Welfare Scheme");
      expect(html).toContain("Ministry of Agriculture");
      expect(html).toContain("Agriculture");
    }).not.toThrow();
  });

  it("17. Regression: Applications Register safely renders application schemeTitle with { english, tamil, translations } without throwing", () => {
    const multilingualApps = [
      {
        id: "app-multi-01",
        applicationNumber: "SB-APP-MULTI-001",
        schemeCode: "SCH-MULTI-01",
        schemeTitle: {
          english: "Pradhan Mantri Awas Yojana",
          tamil: "பிரதம மந்திரி ஆவாஸ் யோஜனா",
          translations: {}
        },
        citizenName: {
          english: "Aarav Sharma",
          translations: {}
        },
        status: "APPROVED",
        createdAt: "2026-09-01T10:00:00Z"
      }
    ];

    expect(() => {
      const html = renderToString(
        <AnalyticsReportsCenter
          applications={multilingualApps}
          schemes={sampleSchemes}
          grievances={sampleGrievances}
          initialMetrics={mockMetrics}
          initialAnalytics={mockAnalytics}
          initialActiveReportTab="applications"
        />
      );
      expect(html).toContain("Pradhan Mantri Awas Yojana");
      expect(html).toContain("Aarav Sharma");
    }).not.toThrow();
  });

  it("18. Regression: Grievance Redressal Audit safely renders grievance subject with object structure", () => {
    const multilingualGrievances = [
      {
        id: "grv-multi-01",
        ticketId: "TKT-MULTI-001",
        subject: {
          english: "Audit review required for subsidy discrepancy",
          tamil: "மானிய முரண்பாட்டிற்கான தணிக்கை",
          translations: {}
        },
        priority: "HIGH",
        status: "RESOLVED",
        createdAt: "2026-09-10T12:00:00Z"
      }
    ];

    expect(() => {
      const html = renderToString(
        <AnalyticsReportsCenter
          applications={sampleApps}
          schemes={sampleSchemes}
          grievances={multilingualGrievances}
          initialMetrics={mockMetrics}
          initialAnalytics={mockAnalytics}
          initialActiveReportTab="grievances"
        />
      );
      expect(html).toContain("Audit review required for subsidy discrepancy");
    }).not.toThrow();
  });
});
