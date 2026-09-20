import { describe, it, expect } from "vitest";
import { normalizeScheme } from "./pages/components/SchemeManagementConsole";
import adminService, {
  getAdminMetrics,
  getAdminAnalytics,
  getAdminAuditLogs,
  downloadAdminReport,
  getAdminSettings,
  updateAdminSettings,
  getAdminDocuments,
  reviewApplication,
  getApplicationsQueue,
  startReview,
  verifyDocument,
  rejectDocument,
  approveApplication,
  rejectApplication,
  getDocumentOcr
} from "../services/adminService";

describe("Admin Scheme Normalization & Real Database Data Integrity", () => {
  it("normalizes real MongoDB SchemeResponse correctly without fabricating missing fields", () => {
    const backendDto = {
      id: "6a8eddd88762e70b86362a8c",
      schemeCode: "SCH-SEARCH-01",
      slug: "national-scholarship-scheme",
      title: {
        english: "National Scholarship Scheme",
        tamil: "தேசிய கல்வி உதவித்தொகை",
        translations: {}
      },
      shortDescription: {
        english: "Merit-cum-means scholarship for technical students.",
        tamil: "சுருக்கம்",
        translations: {}
      },
      schemeLevel: "CENTRAL",
      status: "ACTIVE",
      createdAt: "2026-08-26T11:36:40.364Z",
      benefits: [
        { benefitType: "Scholarship Allowance", amount: 12000 }
      ],
      documents: [
        { documentCode: "INCOME_CERTIFICATE", isMandatory: true }
      ]
    };

    const normalized = normalizeScheme(backendDto);

    expect(normalized.id).toBe("6a8eddd88762e70b86362a8c");
    expect(normalized.schemeCode).toBe("SCH-SEARCH-01");
    expect(normalized.name).toBe("National Scholarship Scheme");
    expect(normalized.description).toBe("Merit-cum-means scholarship for technical students.");
    expect(normalized.ministry).toBe("Central Government");
    expect(normalized.status).toBe("published");
    expect(normalized.sourceType).toBe("Central");
    expect(normalized.approvalRate).toBeNull(); // Must NOT be fabricated
    expect(normalized.deadline).toBe("—"); // Must NOT be fabricated
    expect(normalized.benefits).toEqual(["Scholarship Allowance: ₹12000"]);
    expect(normalized.requiredDocuments).toEqual(["INCOME_CERTIFICATE"]);
  });

  it("handles state-level scheme DTO and non-active statuses", () => {
    const stateDto = {
      id: "6a8eddd88762e70b86362a8d",
      schemeCode: "SCH-SEARCH-02",
      slug: "state-health-benefit",
      title: {
        english: "State Health Benefit",
      },
      schemeLevel: "STATE",
      stateOrUt: "TAMIL_NADU",
      department: "Health and Family Welfare",
      status: "DRAFT",
      updatedAt: "2026-08-26T12:00:00.000Z"
    };

    const normalized = normalizeScheme(stateDto);

    expect(normalized.schemeCode).toBe("SCH-SEARCH-02");
    expect(normalized.name).toBe("State Health Benefit");
    expect(normalized.ministry).toBe("State Government (TAMIL_NADU)");
    expect(normalized.department).toBe("Health and Family Welfare");
    expect(normalized.status).toBe("draft");
    expect(normalized.sourceType).toBe("State");
    expect(normalized.lastUpdated).toBe("2026-08-26");
  });

  it("extracts deadline correctly from applicationInfo.deadline, applicationEndDate, closingDate, or returns '—'", () => {
    // 1. With applicationInfo.deadline
    const dto1 = {
      schemeCode: "SCH-DL-01",
      applicationInfo: { deadline: "2026-12-31T23:59:59Z" }
    };
    expect(normalizeScheme(dto1).deadline).toBe("2026-12-31");

    // 2. With applicationInfo.applicationEndDate
    const dto2 = {
      schemeCode: "SCH-DL-02",
      applicationInfo: { applicationEndDate: "2026-10-15T00:00:00Z" }
    };
    expect(normalizeScheme(dto2).deadline).toBe("2026-10-15");

    // 3. With closingDate
    const dto3 = {
      schemeCode: "SCH-DL-03",
      applicationInfo: { closingDate: "2026-11-30" }
    };
    expect(normalizeScheme(dto3).deadline).toBe("2026-11-30");

    // 4. Missing deadline -> must be '—', never updatedAt or current date
    const dto4 = {
      schemeCode: "SCH-DL-04",
      updatedAt: "2026-08-30T10:00:00Z",
      applicationInfo: { deadline: null }
    };
    expect(normalizeScheme(dto4).deadline).toBe("—");
  });

  it("extracts backend applicationsCount and approvalRate without alteration", () => {
    const dto = {
      schemeCode: "SCH-EDU-003",
      applicationsCount: 8,
      approvalRate: 87.5
    };
    const normalized = normalizeScheme(dto);
    expect(normalized.applicationsCount).toBe(8);
    expect(normalized.approvalRate).toBe(87.5);
  });

  it("returns null safely for invalid or empty inputs without throwing", () => {
    expect(normalizeScheme(null)).toBeNull();
    expect(normalizeScheme(undefined)).toBeNull();
    expect(normalizeScheme("not-an-object")).toBeNull();
  });
});

describe("Admin Service Contract & Operations Export Integrity", () => {
  it("exports all mandatory administrative functions on default and named exports", () => {
    // Check named exports
    expect(typeof getAdminMetrics).toBe("function");
    expect(typeof getAdminAnalytics).toBe("function");
    expect(typeof getAdminAuditLogs).toBe("function");
    expect(typeof downloadAdminReport).toBe("function");
    expect(typeof getAdminSettings).toBe("function");
    expect(typeof updateAdminSettings).toBe("function");
    expect(typeof getAdminDocuments).toBe("function");
    expect(typeof reviewApplication).toBe("function");
    expect(typeof getApplicationsQueue).toBe("function");
    expect(typeof startReview).toBe("function");
    expect(typeof verifyDocument).toBe("function");
    expect(typeof rejectDocument).toBe("function");
    expect(typeof approveApplication).toBe("function");
    expect(typeof rejectApplication).toBe("function");
    expect(typeof getDocumentOcr).toBe("function");

    // Check default object exports
    expect(typeof adminService.getAdminMetrics).toBe("function");
    expect(typeof adminService.getAdminAnalytics).toBe("function");
    expect(typeof adminService.getAdminAuditLogs).toBe("function");
    expect(typeof adminService.downloadAdminReport).toBe("function");
    expect(typeof adminService.getAdminSettings).toBe("function");
    expect(typeof adminService.updateAdminSettings).toBe("function");
    expect(typeof adminService.getAdminDocuments).toBe("function");
    expect(typeof adminService.reviewApplication).toBe("function");
    expect(typeof adminService.getApplicationsQueue).toBe("function");
  });
});
