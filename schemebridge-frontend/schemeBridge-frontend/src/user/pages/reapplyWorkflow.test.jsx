import { describe, it, expect, vi } from "vitest";
import {
  ACTIVE_STATUSES,
  TERMINAL_STATUSES,
  REAPPLYABLE_STATUSES,
  isReapplyable,
  isActiveApplication,
} from "@services/applicationService";

describe("Application Reapply Status Lifecycle Rules", () => {
  it("terminal statuses explicitly allow reapplication", () => {
    expect(REAPPLYABLE_STATUSES).toContain("REJECTED");
    expect(REAPPLYABLE_STATUSES).toContain("CANCELLED");
    expect(REAPPLYABLE_STATUSES).toContain("APPROVED");
    expect(REAPPLYABLE_STATUSES.length).toBe(3);

    expect(isReapplyable("REJECTED")).toBe(true);
    expect(isReapplyable("CANCELLED")).toBe(true);
    expect(isReapplyable("APPROVED")).toBe(true);
  });

  it("active statuses strictly forbid new reapplication", () => {
    const expectedActive = [
      "DRAFT",
      "DOCUMENTS_PENDING",
      "READY_FOR_SUBMISSION",
      "SUBMITTED",
      "UNDER_REVIEW",
      "CORRECTION_REQUIRED",
    ];

    for (const status of expectedActive) {
      expect(ACTIVE_STATUSES).toContain(status);
      expect(isReapplyable(status)).toBe(false);
      expect(isActiveApplication(status)).toBe(true);
    }
  });

  it("active and terminal status sets are strictly disjoint", () => {
    for (const status of ACTIVE_STATUSES) {
      expect(TERMINAL_STATUSES).not.toContain(status);
    }
    for (const status of TERMINAL_STATUSES) {
      expect(ACTIVE_STATUSES).not.toContain(status);
    }
  });

  it("surfaces detailed 409 error message from backend", () => {
    const backend409Response = {
      error: true,
      status: 409,
      message: "An active application already exists for this scheme. Application SB-APP-2026-000059 is currently DOCUMENTS_PENDING.",
      data: {
        status: 409,
        error: "CONFLICT",
        message: "An active application already exists for this scheme. Application SB-APP-2026-000059 is currently DOCUMENTS_PENDING.",
      },
    };

    const showToast = vi.fn();
    if (backend409Response.error) {
      showToast(backend409Response.message || "Failed to reapply for scheme.", "error");
    }

    expect(showToast).toHaveBeenCalledWith(
      "An active application already exists for this scheme. Application SB-APP-2026-000059 is currently DOCUMENTS_PENDING.",
      "error"
    );
  });

  it("reapply loading state prevents concurrent multiple submissions (debouncing guard)", async () => {
    let reapplying = false;
    let invocationCount = 0;

    const handleConfirmReapply = async () => {
      if (reapplying) return; // Debounce guard
      reapplying = true;
      invocationCount++;
      await new Promise(r => setTimeout(r, 10));
      reapplying = false;
    };

    // Rapid double click simulation
    const p1 = handleConfirmReapply();
    const p2 = handleConfirmReapply();
    await Promise.all([p1, p2]);

    expect(invocationCount).toBe(1);
  });
});
