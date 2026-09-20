import { describe, it, expect, vi } from "vitest";
import * as feedbackService from "./feedbackService";
import { schemeApi } from "../utils/apiClient";

vi.mock("../utils/apiClient", () => ({
  schemeApi: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

describe("feedbackService — Citizen & Admin Feedback API client", () => {
  it("submits portal feedback via schemeApi.post", async () => {
    const mockResponse = { data: { id: "FB-999", rating: 5, status: "Received" } };
    schemeApi.post.mockResolvedValueOnce(mockResponse);

    const res = await feedbackService.submitPortalFeedback({
      type: "PLATFORM",
      rating: 5,
      comment: "Great portal",
      citizenEmail: "citizen@example.com"
    });

    expect(schemeApi.post).toHaveBeenCalledWith(
      "/api/feedback",
      expect.objectContaining({ rating: 5, comment: "Great portal" })
    );
    expect(res.data.id).toBe("FB-999");
  });

  it("retrieves citizen feedback via schemeApi.get", async () => {
    const mockList = [{ id: "FB-1" }, { id: "FB-2" }];
    schemeApi.get.mockResolvedValueOnce({ data: mockList });

    const res = await feedbackService.getCitizenFeedback();
    expect(schemeApi.get).toHaveBeenCalledWith("/api/feedback/my");
    expect(res.data.length).toBe(2);
  });

  it("retrieves admin feedback directory with filter parameters", async () => {
    const mockPage = { data: { content: [{ id: "FB-1" }], totalElements: 1 } };
    schemeApi.get.mockResolvedValueOnce(mockPage);

    const res = await feedbackService.getAdminFeedback({ page: 0, size: 20 });
    expect(schemeApi.get).toHaveBeenCalledWith(
      "/api/admin/feedback",
      { params: { page: 0, size: 20 } }
    );
    expect(res.data.content.length).toBe(1);
  });

  it("updates feedback review status with admin notes", async () => {
    const mockUpdated = { data: { id: "FB-1", status: "ACKNOWLEDGED" } };
    schemeApi.patch.mockResolvedValueOnce(mockUpdated);

    const res = await feedbackService.updateFeedbackStatus("FB-1", "ACKNOWLEDGED");
    expect(schemeApi.patch).toHaveBeenCalledWith(
      "/api/admin/feedback/FB-1/status",
      { status: "ACKNOWLEDGED" }
    );
    expect(res.data.status).toBe("ACKNOWLEDGED");
  });
});
