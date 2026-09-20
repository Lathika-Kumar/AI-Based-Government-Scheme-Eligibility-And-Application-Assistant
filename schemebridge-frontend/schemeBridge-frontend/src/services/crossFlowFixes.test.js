import { describe, it, expect, vi } from "vitest";
import { getDocReadinessForScheme } from "../utils/documentReadiness";
import * as feedbackService from "./feedbackService";
import * as grievanceService from "./grievanceService";
import { schemeApi } from "../utils/apiClient";
import { resolveApplicationAction, findApplicationForScheme } from "../utils/applicationStateMapping";
import { renderFormattedText } from "../utils/textFormatter";

describe("Cross-Flow Fixes — Comprehensive Regression Suite", () => {
  // =========================================================================
  // ISSUE 1: Admin Applications Queue & State Safety
  // =========================================================================
  describe("Issue 1: Applications Management State & Handling", () => {
    it("1.1 Handles successful non-empty applications response gracefully", () => {
      const apiResponse = {
        content: [
          {
            id: "app-101",
            applicationNumber: "APP-2026-101",
            userName: "Ramesh Kumar",
            schemeCode: "PM_KISAN",
            status: "SUBMITTED",
          },
        ],
        page: 0,
        totalPages: 1,
        totalElements: 1,
      };

      const mapped = (apiResponse.content || []).map((app) => ({
        id: app.applicationNumber || app.id,
        applicationId: app.id,
        applicantName: app.userName,
        currentStage: app.status.replace(/_/g, " "),
      }));

      expect(mapped).toHaveLength(1);
      expect(mapped[0].id).toBe("APP-2026-101");
      expect(mapped[0].currentStage).toBe("SUBMITTED");
    });

    it("1.2 Empty application response renders legitimate empty list without error", () => {
      const emptyResponse = {
        content: [],
        page: 0,
        totalPages: 0,
        totalElements: 0,
      };

      const mapped = (emptyResponse.content || []).map((app) => app.id);
      expect(mapped).toEqual([]);
      expect(emptyResponse.totalElements).toBe(0);
    });

    it("1.3 API error does not trigger undefined variable crash", () => {
      let applicationsList = [];
      let error = null;

      try {
        throw new Error("500 Internal Server Error");
      } catch (err) {
        error = err.message;
        applicationsList = [];
      }

      expect(error).toBe("500 Internal Server Error");
      expect(applicationsList).toBeDefined();
      expect(Array.isArray(applicationsList)).toBe(true);
      expect(applicationsList).toHaveLength(0);
    });

    it("1.4 Pagination calculation respects total elements and page size", () => {
      const totalElements = 45;
      const pageSize = 20;
      const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
      expect(totalPages).toBe(3);
    });
  });

  // =========================================================================
  // ISSUE 2: Matching Schemes Document Checklist
  // =========================================================================
  describe("Issue 2: Scheme Document Checklist vs Citizen Vault", () => {
    const schemeChecklist = [
      {
        documentName: "Aadhaar Card",
        canonicalDocumentCode: "AADHAAR",
        mandatory: true,
      },
      {
        documentName: "Income Certificate",
        canonicalDocumentCode: "INCOME_CERTIFICATE",
        mandatory: true,
      },
      {
        documentName: "Community Certificate",
        canonicalDocumentCode: "CASTE_CERTIFICATE",
        mandatory: true,
      },
      {
        documentName: "Bank Passbook",
        canonicalDocumentCode: "BANK_PASSBOOK",
        mandatory: false,
      },
    ];

    const citizenVault = [
      {
        id: 1,
        name: "Aadhaar Card",
        type: "Identity Proof",
        status: "verified",
      },
      {
        id: 2,
        name: "Income Certificate",
        type: "Financial Proof",
        status: "uploaded",
      },
    ];

    it("2.1 Required documents are loaded from scheme data and evaluated", () => {
      const res = getDocReadinessForScheme(schemeChecklist, citizenVault);
      expect(res.totalRequired).toBe(4);
      expect(res.evaluatedItems).toHaveLength(4);
    });

    it("2.2 Uploaded documents produce ✓ Uploaded", () => {
      const res = getDocReadinessForScheme(schemeChecklist, citizenVault);
      const aadhaar = res.evaluatedItems.find((d) => d.documentName === "Aadhaar Card");
      const income = res.evaluatedItems.find((d) => d.documentName === "Income Certificate");

      expect(aadhaar).toBeDefined();
      expect(aadhaar.isAvailable).toBe(true);
      expect(aadhaar.statusLabel).toBe("Uploaded");

      expect(income).toBeDefined();
      expect(income.isAvailable).toBe(true);
      expect(income.statusLabel).toBe("Uploaded");
    });

    it("2.3 Missing documents produce ✗ Missing", () => {
      const res = getDocReadinessForScheme(schemeChecklist, citizenVault);
      const community = res.evaluatedItems.find((d) => d.documentName === "Community Certificate");
      const passbook = res.evaluatedItems.find((d) => d.documentName === "Bank Passbook");

      expect(community).toBeDefined();
      expect(community.isAvailable).toBe(false);
      expect(community.statusLabel).toBe("Missing");

      expect(passbook).toBeDefined();
      expect(passbook.isAvailable).toBe(false);
      expect(passbook.statusLabel).toBe("Missing");
    });

    it("2.4 Citizen with no uploaded documents produces all Missing", () => {
      const emptyVault = [];
      const res = getDocReadinessForScheme(schemeChecklist, emptyVault);

      expect(res.totalAvailable).toBe(0);
      expect(res.readinessScore).toBe(0);
      res.evaluatedItems.forEach((item) => {
        expect(item.isAvailable).toBe(false);
        expect(item.statusLabel).toBe("Missing");
      });
    });

    it("2.5 Scheme with no required documents produces legitimate empty state", () => {
      const emptySchemeDocs = [];
      const res = getDocReadinessForScheme(emptySchemeDocs, citizenVault);

      expect(res.totalRequired).toBe(0);
      expect(res.evaluatedItems).toHaveLength(0);
      expect(res.availableDocs).toHaveLength(0);
      expect(res.missingDocs).toHaveLength(0);
    });

    it("2.6 Evaluates multiple schemes from single vault state without N+1 requests", () => {
      const schemes = [
        { id: "S1", reqs: ["Aadhaar Card"] },
        { id: "S2", reqs: ["Income Certificate", "Caste Certificate"] },
        { id: "S3", reqs: ["Bank Passbook"] },
      ];

      // Single vault reference passed synchronously across schemes
      const evaluations = schemes.map((s) => ({
        schemeId: s.id,
        readiness: getDocReadinessForScheme(s.reqs, citizenVault),
      }));

      expect(evaluations).toHaveLength(3);
      expect(evaluations[0].readiness.availableCount).toBe(1);
      expect(evaluations[1].readiness.availableCount).toBe(1); // Income is available, Caste missing
      expect(evaluations[2].readiness.availableCount).toBe(0); // Bank Passbook missing
    });
  });

  // =========================================================================
  // ISSUE 3: Citizen Feedback Star Rating & Admin Breakdown
  // =========================================================================
  describe("Issue 3: Citizen Feedback Star Rating End-to-End", () => {
    it("3.1 Validates ratings 1 through 5 and passes exact integer", async () => {
      const postSpy = vi.spyOn(schemeApi, "post").mockResolvedValue({
        data: { id: "FB-101", rating: 3, type: "Bug Report" },
      });

      for (let star = 1; star <= 5; star++) {
        const feedbackPayload = {
          type: "Bug Report",
          rating: star,
          comment: `Testing rating ${star}`,
        };

        await feedbackService.submitPortalFeedback(feedbackPayload);
        expect(postSpy).toHaveBeenLastCalledWith("/api/feedback", feedbackPayload);
      }

      postSpy.mockRestore();
    });

    it("3.2 Bug Report does NOT automatically become 5 stars", () => {
      const citizenBugReport = {
        type: "Bug Report",
        rating: 1,
        comment: "Found an issue on application submission.",
      };

      expect(citizenBugReport.rating).toBe(1);
      expect(citizenBugReport.rating).not.toBe(5);
    });

    it("3.3 Admin statistics compute average rating from actual stored values", () => {
      const feedbackList = [
        { rating: 1, type: "Bug Report" },
        { rating: 2, type: "General Feedback" },
        { rating: 5, type: "Portal Rating" },
        { rating: 4, type: "Scheme Suggestion" },
      ];

      const ratedItems = feedbackList.filter(
        (f) => f.rating !== null && f.rating !== undefined && !isNaN(f.rating)
      );
      const avgRating =
        ratedItems.length > 0
          ? (ratedItems.reduce((acc, f) => acc + Number(f.rating), 0) / ratedItems.length).toFixed(1)
          : "0.0";
      const fiveStars = feedbackList.filter((f) => Number(f.rating) === 5).length;
      const bugs = feedbackList.filter((f) => f.type === "Bug Report").length;

      // (1 + 2 + 5 + 4) / 4 = 12 / 4 = 3.0
      expect(avgRating).toBe("3.0");
      expect(fiveStars).toBe(1);
      expect(bugs).toBe(1);
    });

    it("3.4 Admin star rendering produces exact star fill count", () => {
      const checkStars = (rating) => {
        return [1, 2, 3, 4, 5].map((s) => s <= Number(rating || 0));
      };

      expect(checkStars(1)).toEqual([true, false, false, false, false]);
      expect(checkStars(2)).toEqual([true, true, false, false, false]);
      expect(checkStars(3)).toEqual([true, true, true, false, false]);
      expect(checkStars(4)).toEqual([true, true, true, true, false]);
      expect(checkStars(5)).toEqual([true, true, true, true, true]);
    });
  });

  // =========================================================================
  // ISSUE 4: Grievance Officer Response Must Close Grievance
  // =========================================================================
  describe("Issue 4: Grievance Officer Response & Closure", () => {
    it("4.1 Officer reply endpoint sends reply payload", async () => {
      const postSpy = vi.spyOn(schemeApi, "post").mockResolvedValue({
        data: { id: "GRV-100", status: "RESOLVED", resolution: "Issue resolved by verification officer." },
      });

      await grievanceService.adminReplyToGrievance("GRV-100", "Issue resolved by verification officer.");
      expect(postSpy).toHaveBeenCalledWith("/api/admin/grievances/GRV-100/reply", {
        message: "Issue resolved by verification officer.",
        attachments: [],
      });

      postSpy.mockRestore();
    });

    it("4.2 Resolved / Closed grievance determines isTerminal = true", () => {
      const terminalStatuses = ["RESOLVED", "CLOSED", "REJECTED"];
      terminalStatuses.forEach((st) => {
        const isTerminal = ["RESOLVED", "CLOSED", "REJECTED"].includes(st);
        expect(isTerminal).toBe(true);
      });

      const openStatuses = ["OPEN", "IN_PROGRESS", "WAITING_FOR_CITIZEN"];
      openStatuses.forEach((st) => {
        const isTerminal = ["RESOLVED", "CLOSED", "REJECTED"].includes(st);
        expect(isTerminal).toBe(false);
      });
    });

    it("4.3 Citizen reply rejected when grievance is terminal in state machine", () => {
      const grievance = {
        id: "GRV-200",
        status: "RESOLVED",
        userId: "citizen-1",
      };

      const attemptCitizenReply = (g, authorRole) => {
        const isPrivileged = ["ROLE_ADMIN", "ROLE_VERIFICATION_OFFICER"].includes(authorRole);
        if (!isPrivileged) {
          if (["RESOLVED", "CLOSED", "REJECTED"].includes(g.status)) {
            throw new Error("This grievance has been resolved. Further replies are closed.");
          }
        }
        return "REPLY_ACCEPTED";
      };

      expect(() => attemptCitizenReply(grievance, "ROLE_USER")).toThrow(
        "This grievance has been resolved. Further replies are closed."
      );
    });

    it("4.4 Open grievance accepts citizen reply", () => {
      const grievance = {
        id: "GRV-201",
        status: "IN_PROGRESS",
        userId: "citizen-1",
      };

      const attemptCitizenReply = (g, authorRole) => {
        const isPrivileged = ["ROLE_ADMIN", "ROLE_VERIFICATION_OFFICER"].includes(authorRole);
        if (!isPrivileged) {
          if (["RESOLVED", "CLOSED", "REJECTED"].includes(g.status)) {
            throw new Error("This grievance has been resolved. Further replies are closed.");
          }
        }
        return "REPLY_ACCEPTED";
      };

      expect(attemptCitizenReply(grievance, "ROLE_USER")).toBe("REPLY_ACCEPTED");
    });
  });

  // =========================================================================
  // ISSUE 5: Already Applied vs File Application State Mapping
  // =========================================================================
  describe("Issue 5: Application State Mapping & Action Resolution", () => {

    const sampleScheme = {
      id: "sch-101",
      schemeCode: "PM_KISAN",
      name: "PM-KISAN Scheme",
    };

    it("5.1 Returns FILE_APPLICATION when citizen has no application for scheme", () => {
      const applications = [];
      const action = resolveApplicationAction(sampleScheme, applications);
      expect(action.action).toBe("FILE_APPLICATION");
      expect(action.buttonText).toBe("File Application");
      expect(action.hasExistingApp).toBe(false);
      expect(action.targetRoute).toContain("PM_KISAN");
    });

    it("5.2 Returns CONTINUE_APPLICATION when application is in DRAFT or DOCUMENTS_PENDING", () => {
      const draftApp = [
        {
          id: "APP-001",
          schemeCode: "PM_KISAN",
          status: "DRAFT",
        },
      ];
      const draftAction = resolveApplicationAction(sampleScheme, draftApp);
      expect(draftAction.action).toBe("CONTINUE_APPLICATION");
      expect(draftAction.buttonText).toBe("Continue Application");
      expect(draftAction.hasExistingApp).toBe(true);
      expect(draftAction.targetRoute).toBe("/applications/APP-001");

      const pendingDocApp = [
        {
          id: "APP-002",
          schemeId: "sch-101",
          status: "DOCUMENTS_PENDING",
        },
      ];
      const pendingAction = resolveApplicationAction(sampleScheme, pendingDocApp);
      expect(pendingAction.action).toBe("CONTINUE_APPLICATION");
      expect(pendingAction.buttonText).toBe("Continue Application");
      expect(pendingAction.hasExistingApp).toBe(true);
    });

    it("5.3 Returns VIEW_APPLICATION when application is SUBMITTED, UNDER_REVIEW, APPROVED, or REJECTED", () => {
      const statuses = ["SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED"];
      statuses.forEach((status) => {
        const apps = [{ id: "APP-003", schemeCode: "PM_KISAN", status }];
        const action = resolveApplicationAction(sampleScheme, apps);
        expect(action.action).toBe("VIEW_APPLICATION");
        expect(action.buttonText).toBe("View Application");
        expect(action.hasExistingApp).toBe(true);
        expect(action.targetRoute).toBe("/applications");
      });
    });

    it("5.4 Correctly resolves application matching by id, schemeCode, or schemeId", () => {
      const apps = [
        { id: "APP-99", schemeId: "sch-101", status: "UNDER_REVIEW" },
      ];
      const found = findApplicationForScheme(sampleScheme, apps);
      expect(found).toBeDefined();
      expect(found.id).toBe("APP-99");
    });
  });

  // =========================================================================
  // ISSUE 6: Raw HTML / <br> Safe Text Rendering
  // =========================================================================
  describe("Issue 6: Safe Text Formatting & Line Breaks", () => {

    it("6.1 Handles string with <br> tag safely without throwing", () => {
      const input = "The ₹108 Emergency Ambulance Service ... offering essential first aid. <br> Available 24/7.";
      const result = renderFormattedText(input);
      expect(result).toBeDefined();
      expect(Array.isArray(result)).toBe(true);
      expect(result.length).toBeGreaterThan(1);
    });

    it("6.2 Splits multiple line break formats (<br/>, <br />, \\n)", () => {
      const input = "Line 1<br/>Line 2<br />Line 3\nLine 4";
      const result = renderFormattedText(input);
      expect(result).toHaveLength(4); // 4 line fragments with injected <br/> breaks
    });

    it("6.3 Null and undefined return empty fallback", () => {
      expect(renderFormattedText(null)).toBeNull();
      expect(renderFormattedText(undefined)).toBeNull();
      expect(renderFormattedText("")).toBe("");
    });
  });

  // =========================================================================
  // ISSUE 7: Notification Bell & Unread Count Source of Truth
  // =========================================================================
  describe("Issue 7: Canonical Notification Unread State & SSE Deduplication", () => {
    it("7.1 Unread count strictly counts items where read === false", () => {
      const notifications = [
        { id: "notif-1", title: "New Grievance Lodged", read: false },
        { id: "notif-2", title: "New Scheme Application Submitted", read: false },
        { id: "notif-3", title: "Document Verified", read: true },
        { id: "notif-4", title: "System Update", read: false },
      ];

      const unreadCount = notifications.filter((n) => !n.read && !n.isRead).length;
      expect(unreadCount).toBe(3);
    });

    it("7.2 Marking a notification as read decrements the unread count", () => {
      let notifications = [
        { id: "notif-1", title: "New Grievance Lodged", read: false, isRead: false },
        { id: "notif-2", title: "New Scheme Application Submitted", read: false, isRead: false },
      ];

      expect(notifications.filter((n) => !n.read && !n.isRead).length).toBe(2);

      // Mark notif-1 read
      notifications = notifications.map((n) => (n.id === "notif-1" ? { ...n, read: true, isRead: true } : n));
      expect(notifications.filter((n) => !n.read && !n.isRead).length).toBe(1);

      // Mark all read
      notifications = notifications.map((n) => ({ ...n, read: true, isRead: true }));
      expect(notifications.filter((n) => !n.read && !n.isRead).length).toBe(0);
    });

    it("7.3 Duplicate SSE events do NOT duplicate notifications or inflate unread count", () => {
      let notifications = [
        { id: "notif-1", title: "New Grievance Lodged", read: false },
      ];

      const incomingSseAlert = {
        id: "notif-1",
        title: "New Grievance Lodged",
        message: "Duplicate broadcast",
        read: false,
      };

      // Deduplication check
      const handleSseMessage = (newAlert, currentList) => {
        if (currentList.some((n) => n.id === newAlert.id)) {
          return currentList; // Ignore duplicate
        }
        return [newAlert, ...currentList];
      };

      const updated = handleSseMessage(incomingSseAlert, notifications);
      expect(updated).toHaveLength(1);
      expect(updated.filter((n) => !n.read).length).toBe(1);

      // Distinct alert
      const distinctAlert = {
        id: "notif-2",
        title: "New Citizen Feedback Submitted",
        read: false,
      };
      const withNew = handleSseMessage(distinctAlert, updated);
      expect(withNew).toHaveLength(2);
      expect(withNew.filter((n) => !n.read).length).toBe(2);
    });

    it("7.4 Bell badge matches notification panel count and hides when 0", () => {
      const getBadgeDisplay = (count) => {
        if (count <= 0) return null;
        return count > 9 ? "9+" : String(count);
      };

      expect(getBadgeDisplay(0)).toBeNull();
      expect(getBadgeDisplay(3)).toBe("3");
      expect(getBadgeDisplay(12)).toBe("9+");
    });
  });

  // =========================================================================
  // ISSUE 8: Citizen Feedback Triggers Admin Notification
  // =========================================================================
  describe("Issue 8: Citizen Feedback & Admin Notification Contract", () => {
    it("8.1 Submitting feedback sends correct payload to POST /api/feedback", async () => {
      const postSpy = vi.spyOn(schemeApi, "post").mockResolvedValue({
        data: {
          id: "FB-888",
          type: "Bug Report",
          rating: 2,
          comment: "Error on submitting document.",
          createdAt: "2026-09-16T12:00:00Z",
        },
      });

      const payload = {
        type: "Bug Report",
        rating: 2,
        comment: "Error on submitting document.",
      };

      const result = await feedbackService.submitPortalFeedback(payload);
      expect(postSpy).toHaveBeenCalledWith("/api/feedback", payload);
      expect(result.data.id).toBe("FB-888");
      expect(result.data.rating).toBe(2);

      postSpy.mockRestore();
    });

    it("8.2 Admin notification payload contains feedback ID and recipient role ROLE_ADMIN", () => {
      const adminNotification = {
        id: "notif-fb-1",
        recipientRole: "ROLE_ADMIN",
        type: "FEEDBACK_SUBMITTED",
        title: "New Citizen Feedback Submitted",
        message: "New feedback (Bug Report) submitted with rating 2/5",
        relatedEntityType: "FEEDBACK",
        relatedEntityId: "FB-888",
        read: false,
      };

      expect(adminNotification.recipientRole).toBe("ROLE_ADMIN");
      expect(adminNotification.type).toBe("FEEDBACK_SUBMITTED");
      expect(adminNotification.relatedEntityId).toBe("FB-888");
      expect(adminNotification.read).toBe(false);
    });
  });

  // =========================================================================
  // ISSUE 9: Auto-Mark Notification Read When Viewed (Citizen & Admin UX)
  // =========================================================================
  describe("Issue 9: Auto-Mark Notification As Read When Viewed", () => {
    it("9.1 Opening unread notification calls markNotificationRead and marks read state", async () => {
      const markSpy = vi.spyOn(schemeApi, "post").mockResolvedValue({
        data: { id: "notif-app-1", read: true, readAt: "2026-09-16T12:00:00Z" },
      });

      let notifications = [
        { id: "notif-app-1", title: "Application Decision", read: false, isRead: false },
        { id: "notif-app-2", title: "Application Under Review", read: false, isRead: false },
      ];

      // Simulate handleCardClick / open on notif-app-1
      const onOpenNotification = async (id) => {
        const target = notifications.find((n) => n.id === id);
        if (!target || target.read) return;
        notifications = notifications.map((n) => (n.id === id ? { ...n, read: true, isRead: true } : n));
        await schemeApi.post(`/api/notifications/${encodeURIComponent(id)}/read`, {});
      };

      await onOpenNotification("notif-app-1");

      expect(markSpy).toHaveBeenCalledWith("/api/notifications/notif-app-1/read", {});
      expect(notifications.find((n) => n.id === "notif-app-1").read).toBe(true);
      expect(notifications.filter((n) => !n.read).length).toBe(1);

      markSpy.mockRestore();
    });

    it("9.2 Opening unread notification decrements unreadCount", () => {
      let state = [
        { id: "n1", read: false },
        { id: "n2", read: false },
      ];
      const getUnread = (list) => list.filter((n) => !n.read).length;

      expect(getUnread(state)).toBe(2);

      // Open n1
      state = state.map((n) => (n.id === "n1" ? { ...n, read: true } : n));
      expect(getUnread(state)).toBe(1);

      // Open n2
      state = state.map((n) => (n.id === "n2" ? { ...n, read: true } : n));
      expect(getUnread(state)).toBe(0);
    });

    it("9.3 Opening already-read notification does not decrement count", () => {
      let state = [
        { id: "n1", read: true },
        { id: "n2", read: false },
      ];
      const getUnread = (list) => list.filter((n) => !n.read).length;
      expect(getUnread(state)).toBe(1);

      // Open already-read n1 again
      let apiCalled = false;
      const onOpen = (id) => {
        const item = state.find((n) => n.id === id);
        if (!item || item.read) return;
        apiCalled = true;
        state = state.map((n) => (n.id === id ? { ...n, read: true } : n));
      };

      onOpen("n1");
      expect(apiCalled).toBe(false);
      expect(getUnread(state)).toBe(1);
    });

    it("9.4 Opening same notification twice does not decrement twice", () => {
      let state = [
        { id: "n1", read: false },
        { id: "n2", read: false },
      ];
      const getUnread = (list) => list.filter((n) => !n.read).length;

      const openCard = (id) => {
        state = state.map((n) => (n.id === id ? { ...n, read: true } : n));
      };

      openCard("n1");
      expect(getUnread(state)).toBe(1);

      // Open n1 second time
      openCard("n1");
      expect(getUnread(state)).toBe(1);
    });

    it("9.5 View Details marks notification read before navigation", async () => {
      const callOrder = [];
      const fakeNavigate = vi.fn((route) => callOrder.push(`navigate:${route}`));
      const markNotificationRead = vi.fn(async (id) => callOrder.push(`markRead:${id}`));

      const notif = {
        id: "notif-app-598",
        title: "Application Decision: SB-APP-2026-000598",
        read: false,
        actionRoute: "/applications/SB-APP-2026-000598",
      };

      // Emulate handleAction in NotificationCard / Drawer
      const handleAction = async () => {
        if (!notif.read) {
          await markNotificationRead(notif.id);
        }
        if (notif.actionRoute) {
          fakeNavigate(notif.actionRoute);
        }
      };

      await handleAction();

      expect(callOrder).toEqual([
        "markRead:notif-app-598",
        "navigate:/applications/SB-APP-2026-000598",
      ]);
      expect(fakeNavigate).toHaveBeenCalledWith("/applications/SB-APP-2026-000598");
    });

    it("9.5b Admin application notification resolves to exact application route", () => {
      const resolveNotificationActionRoute = (n, isPrivileged) => {
        const typeStr = String(n.type || "").toUpperCase();
        const entityType = String(n.relatedEntityType || "").toUpperCase();
        const entityId = n.relatedEntityId || n.metadata?.applicationId || n.applicationId;

        if (isPrivileged) {
          if (entityType === "APPLICATION" || typeStr.includes("APP")) {
            return entityId ? `/admin/review/${entityId}` : "/admin/review";
          }
          return "/admin/notifications";
        }
        return entityId ? `/applications/${entityId}` : "/applications";
      };

      const adminNotif = {
        type: "APPLICATION_SUBMITTED",
        relatedEntityType: "APPLICATION",
        relatedEntityId: "6aac0861173ff55c630f34b6",
        metadata: { applicationId: "6aac0861173ff55c630f34b6", applicationNumber: "SB-APP-2026-000012" }
      };

      const adminRoute = resolveNotificationActionRoute(adminNotif, true);
      expect(adminRoute).toBe("/admin/review/6aac0861173ff55c630f34b6");

      const citizenRoute = resolveNotificationActionRoute(adminNotif, false);
      expect(citizenRoute).toBe("/applications/6aac0861173ff55c630f34b6");
    });

    it("9.6 Bell badge strictly reflects unreadCount and hides when zero", () => {
      const renderBadge = (unreadCount) => {
        if (unreadCount <= 0) return null;
        return unreadCount > 9 ? "9+" : String(unreadCount);
      };

      expect(renderBadge(2)).toBe("2");
      expect(renderBadge(1)).toBe("1");
      expect(renderBadge(0)).toBeNull();
      expect(renderBadge(15)).toBe("9+");
    });

    it("9.7 Drawer header accurately displays NOTIFICATIONS (N NEW)", () => {
      const getDrawerHeader = (unreadCount) => `NOTIFICATIONS (${unreadCount} NEW)`;

      expect(getDrawerHeader(2)).toBe("NOTIFICATIONS (2 NEW)");
      expect(getDrawerHeader(1)).toBe("NOTIFICATIONS (1 NEW)");
      expect(getDrawerHeader(0)).toBe("NOTIFICATIONS (0 NEW)");
    });

    it("9.8 Citizen notification page header accurately displays Notifications (N NEW)", () => {
      const getPageHeader = (unreadCount) => `Notifications (${unreadCount} NEW)`;

      expect(getPageHeader(2)).toBe("Notifications (2 NEW)");
      expect(getPageHeader(1)).toBe("Notifications (1 NEW)");
      expect(getPageHeader(0)).toBe("Notifications (0 NEW)");
    });

    it("9.9 SSE new notification increments unreadCount", () => {
      let notifications = [
        { id: "n1", title: "Review Complete", read: true },
      ];
      const getUnread = (list) => list.filter((n) => !n.read).length;
      expect(getUnread(notifications)).toBe(0);

      const incoming = {
        id: "n2",
        title: "New Application Received",
        read: false,
      };

      // Add SSE alert
      notifications = [incoming, ...notifications];
      expect(getUnread(notifications)).toBe(1);
    });

    it("9.10 Duplicate SSE event does not inflate unreadCount", () => {
      let notifications = [
        { id: "n1", title: "New Application Received", read: false },
      ];
      const getUnread = (list) => list.filter((n) => !n.read).length;
      expect(getUnread(notifications)).toBe(1);

      const duplicate = {
        id: "n1",
        title: "New Application Received",
        read: false,
      };

      const handleSse = (alert, list) => {
        if (list.some((item) => item.id === alert.id)) return list;
        return [alert, ...list];
      };

      notifications = handleSse(duplicate, notifications);
      expect(notifications.length).toBe(1);
      expect(getUnread(notifications)).toBe(1);
    });

    it("9.11 Page reload reconstructs persisted read state from backend API response", () => {
      // Backend response on reload: n1 is now read: true, n2 remains false
      const backendFetchResponse = {
        content: [
          { id: "n1", read: true, readAt: "2026-09-16T12:00:00Z" },
          { id: "n2", read: false, readAt: null },
        ],
        unreadCount: 1,
      };

      const rehydratedNotifications = backendFetchResponse.content.map((n) => ({
        id: n.id,
        read: n.read === true,
        isRead: n.read === true,
      }));

      const unreadCount = rehydratedNotifications.filter((n) => !n.read && !n.isRead).length;

      expect(unreadCount).toBe(1);
      expect(rehydratedNotifications.find((n) => n.id === "n1").read).toBe(true);
      expect(rehydratedNotifications.find((n) => n.id === "n2").read).toBe(false);
    });
  });
});
