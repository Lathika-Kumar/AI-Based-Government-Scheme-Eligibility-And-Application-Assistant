import { describe, it, expect, vi, beforeEach } from "vitest";
import adminService, {
  reviewApplication,
  approveApplication,
  rejectApplication,
  requestMoreDocuments
} from "../services/adminService";
import { schemeApi } from "@utils/apiClient";

vi.mock("@utils/apiClient", () => ({
  schemeApi: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}));

describe("Admin Review Workflow Service & Canonical Contract", () => {
  const appId = "6a9fdf570fd527585a2305c9";

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("1. reviewApplication sends canonical action: 'APPROVE'", async () => {
    schemeApi.post.mockResolvedValueOnce({ data: { id: appId, status: "APPROVED" } });

    const res = await reviewApplication(appId, {
      action: "APPROVE",
      remarks: "Eligible and all documents verified"
    });

    expect(schemeApi.post).toHaveBeenCalledWith(
      `/api/admin/applications/${appId}/review`,
      expect.objectContaining({
        action: "APPROVE",
        remarks: "Eligible and all documents verified"
      })
    );
    expect(res.data.status).toBe("APPROVED");
  });

  it("2. reviewApplication sends canonical action: 'REJECT'", async () => {
    schemeApi.post.mockResolvedValueOnce({ data: { id: appId, status: "REJECTED" } });

    const res = await reviewApplication(appId, {
      action: "REJECT",
      reason: "Income exceeds threshold",
      remarks: "Income exceeds threshold"
    });

    expect(schemeApi.post).toHaveBeenCalledWith(
      `/api/admin/applications/${appId}/review`,
      expect.objectContaining({
        action: "REJECT",
        reason: "Income exceeds threshold"
      })
    );
    expect(res.data.status).toBe("REJECTED");
  });

  it("3. reviewApplication sends canonical action: 'REQUEST_MORE_DOCUMENTS'", async () => {
    schemeApi.post.mockResolvedValueOnce({ data: { id: appId, status: "CORRECTION_REQUIRED" } });

    const res = await reviewApplication(appId, {
      action: "REQUEST_MORE_DOCUMENTS",
      reason: "Upload clear land record copy"
    });

    expect(schemeApi.post).toHaveBeenCalledWith(
      `/api/admin/applications/${appId}/review`,
      expect.objectContaining({
        action: "REQUEST_MORE_DOCUMENTS",
        reason: "Upload clear land record copy"
      })
    );
    expect(res.data.status).toBe("CORRECTION_REQUIRED");
  });

  it("4. Normalizes legacy/workspace status: 'APPROVED' to canonical action: 'APPROVE'", async () => {
    schemeApi.post.mockResolvedValueOnce({ data: { id: appId, status: "APPROVED" } });

    await reviewApplication(appId, {
      status: "APPROVED",
      reviewerNotes: "Approved by Admin"
    });

    expect(schemeApi.post).toHaveBeenCalledWith(
      `/api/admin/applications/${appId}/review`,
      expect.objectContaining({
        action: "APPROVE",
        remarks: "Approved by Admin"
      })
    );
  });

  it("5. Normalizes legacy/workspace status: 'REJECTED' to canonical action: 'REJECT'", async () => {
    schemeApi.post.mockResolvedValueOnce({ data: { id: appId, status: "REJECTED" } });

    await reviewApplication(appId, {
      status: "REJECTED",
      reviewerNotes: "Rejected due to invalid document"
    });

    expect(schemeApi.post).toHaveBeenCalledWith(
      `/api/admin/applications/${appId}/review`,
      expect.objectContaining({
        action: "REJECT",
        remarks: "Rejected due to invalid document"
      })
    );
  });

  it("6. Normalizes legacy/workspace status: 'CORRECTION_REQUIRED' to canonical action: 'REQUEST_MORE_DOCUMENTS'", async () => {
    schemeApi.post.mockResolvedValueOnce({ data: { id: appId, status: "CORRECTION_REQUIRED" } });

    await reviewApplication(appId, {
      status: "CORRECTION_REQUIRED",
      correctionReason: "Missing certificate"
    });

    expect(schemeApi.post).toHaveBeenCalledWith(
      `/api/admin/applications/${appId}/review`,
      expect.objectContaining({
        action: "REQUEST_MORE_DOCUMENTS",
        reason: "Missing certificate"
      })
    );
  });

  it("7. Defensively rejects undefined action with descriptive error without network call", async () => {
    const res = await reviewApplication(appId, {});
    expect(res).toEqual({
      error: true,
      message: "Unknown review action: undefined"
    });
    expect(schemeApi.post).not.toHaveBeenCalled();
  });

  it("8. Defensively rejects unknown action with descriptive error", async () => {
    const res = await reviewApplication(appId, { action: "INVALID_ACTION" });
    expect(res).toEqual({
      error: true,
      message: "Unknown review action: INVALID_ACTION"
    });
    expect(schemeApi.post).not.toHaveBeenCalled();
  });

  it("9. requestMoreDocuments calls dedicated /review/request-documents endpoint", async () => {
    schemeApi.post.mockResolvedValueOnce({ data: { id: appId, status: "CORRECTION_REQUIRED" } });

    await requestMoreDocuments(appId, "Please re-upload Aadhaar card");

    expect(schemeApi.post).toHaveBeenCalledWith(
      `/api/admin/applications/${appId}/review/request-documents`,
      {
        remarks: "Please re-upload Aadhaar card",
        reason: "Please re-upload Aadhaar card"
      }
    );
  });

  it("10. rejectApplication forwards remarks as reason", async () => {
    schemeApi.post.mockResolvedValueOnce({ data: { id: appId, status: "REJECTED" } });

    await rejectApplication(appId, "Not eligible");

    expect(schemeApi.post).toHaveBeenCalledWith(
      `/api/admin/applications/${appId}/review/reject`,
      {
        remarks: "Not eligible",
        reason: "Not eligible"
      }
    );
  });
});
