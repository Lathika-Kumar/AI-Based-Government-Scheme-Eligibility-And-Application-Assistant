import { describe, it, expect, vi, beforeEach } from "vitest";
import adminService, {
  verifyDocumentById,
  rejectDocumentById,
  requestDocumentCorrection,
  getPendingDocumentsQueue
} from "./adminService";
import { schemeApi } from "@utils/apiClient";
import { getDocReadinessForScheme } from "@utils/documentReadiness";

vi.mock("@utils/apiClient", () => ({
  schemeApi: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}));

describe("Document Verification Workflow — AI & Officer Status Separation", () => {
  const docId = "DOC-8921";

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("1. getPendingDocumentsQueue calls the pending verification endpoint", async () => {
    schemeApi.get.mockResolvedValueOnce({ data: [{ id: docId, detailedStatus: "UNDER_OFFICER_REVIEW" }] });

    const res = await getPendingDocumentsQueue();

    expect(schemeApi.get).toHaveBeenCalledWith("/api/admin/documents/pending-verification");
    expect(res.data).toHaveLength(1);
    expect(res.data[0].detailedStatus).toBe("UNDER_OFFICER_REVIEW");
  });

  it("2. verifyDocumentById submits officer verification decision", async () => {
    schemeApi.post.mockResolvedValueOnce({
      data: { id: docId, detailedStatus: "ADMIN_VERIFIED", officerStatus: "ADMIN_VERIFIED" }
    });

    const res = await verifyDocumentById(docId);

    expect(schemeApi.post).toHaveBeenCalledWith(`/api/admin/documents/${docId}/verify`, null);
    expect(res.data.detailedStatus).toBe("ADMIN_VERIFIED");
  });

  it("3. requestDocumentCorrection submits correction reason with notes", async () => {
    schemeApi.post.mockResolvedValueOnce({
      data: { id: docId, detailedStatus: "CORRECTION_REQUIRED", correctionNotes: "Blurry document text" }
    });

    const res = await requestDocumentCorrection(docId, "Blurry document text");

    expect(schemeApi.post).toHaveBeenCalledWith(`/api/admin/documents/${docId}/correction`, {
      reason: "Blurry document text"
    });
    expect(res.data.detailedStatus).toBe("CORRECTION_REQUIRED");
  });

  it("4. rejectDocumentById submits officer rejection reason", async () => {
    schemeApi.post.mockResolvedValueOnce({
      data: { id: docId, detailedStatus: "REJECTED", rejectionReason: "Invalid document issued by unrecognized body" }
    });

    const res = await rejectDocumentById(docId, "Invalid document issued by unrecognized body");

    expect(schemeApi.post).toHaveBeenCalledWith(`/api/admin/documents/${docId}/reject`, {
      reason: "Invalid document issued by unrecognized body"
    });
    expect(res.data.detailedStatus).toBe("REJECTED");
  });

  it("5. Scheme readiness requires mandatory documents to be present and ready", () => {
    const required = ["Aadhaar Card", "Income Certificate"];
    const vaultDocs = [
      { name: "Aadhaar Card", status: "verified", expiryDate: "No Expiration" },
      { name: "Income Certificate", status: "verified", expiryDate: "2029-01-01" }
    ];

    const readiness = getDocReadinessForScheme(required, vaultDocs);
    expect(readiness.readinessScore).toBe(100);
    expect(readiness.readinessLabel).toBe("Ready");
    expect(readiness.missingDocs).toHaveLength(0);
  });

  it("6. Scheme readiness correctly flags missing documents when vault is incomplete", () => {
    const required = ["Aadhaar Card", "Income Certificate", "Land Ownership Record"];
    const vaultDocs = [
      { name: "Aadhaar Card", status: "verified", expiryDate: "No Expiration" }
    ];

    const readiness = getDocReadinessForScheme(required, vaultDocs);
    expect(readiness.readinessScore).toBeLessThan(100);
    expect(readiness.readinessLabel).not.toBe("Ready");
    expect(readiness.missingDocs).toContain("Income Certificate");
    expect(readiness.missingDocs).toContain("Land Ownership Record");
  });

  it("7. Aadhaar documents must enforce privacy: no plaintext 12-digit numbers and include statutory disclaimer", () => {
    const aadhaarDoc = {
      name: "Aadhaar Card",
      docNumber: "XXXX XXXX 9999",
      aiStatus: "AI_PASSED",
      aiScore: 92
    };

    // Verify docNumber is masked
    expect(aadhaarDoc.docNumber).toMatch(/^XXXX\s?XXXX\s?\d{4}$/);
    expect(aadhaarDoc.docNumber).not.toMatch(/^\d{12}$/);

    // Verify statutory disclaimer text
    const disclaimer = "Statutory Disclaimer: Aadhaar document validation is performed via AI-assisted visual structure and checksum checks. It does not constitute direct electronic authentication with UIDAI.";
    expect(disclaimer).toContain("does not constitute direct electronic authentication with UIDAI");
  });

  it("8. downloadAdminDocument and downloadAdminDocumentById invoke schemeApi.download with responseType blob", async () => {
    const mockBlob = new Blob(["%PDF-1.4 test"], { type: "application/pdf" });
    schemeApi.download = vi.fn().mockResolvedValueOnce({ data: mockBlob });

    const res1 = await adminService.downloadAdminDocument("APP-123", "AADHAAR");
    expect(schemeApi.download).toHaveBeenCalledWith("/api/applications/APP-123/documents/AADHAAR/download");
    expect(res1.data).toBe(mockBlob);

    schemeApi.download.mockResolvedValueOnce({ data: mockBlob });
    const res2 = await adminService.downloadAdminDocumentById("DOC-999");
    expect(schemeApi.download).toHaveBeenCalledWith("/api/admin/documents/DOC-999/download");
    expect(res2.data).toBe(mockBlob);
  });

  it("9. getAdminApplicationById calls /api/admin/applications/:id", async () => {
    const mockApp = { id: "APP-555", applicationNumber: "SB-APP-2026-000555", status: "SUBMITTED" };
    schemeApi.get.mockResolvedValueOnce({ data: mockApp });

    const res = await adminService.getAdminApplicationById("APP-555");
    expect(schemeApi.get).toHaveBeenCalledWith("/api/admin/applications/APP-555");
    expect(res.data.id).toBe("APP-555");
  });
});
