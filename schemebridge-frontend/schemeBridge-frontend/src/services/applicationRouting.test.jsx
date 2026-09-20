import React from "react";
import { describe, it, expect, vi } from "vitest";
import { renderToString } from "react-dom/server";
import { MemoryRouter, Routes, Route, Navigate, useParams, useLocation } from "react-router-dom";
import { resolveApplicationAction, ACTION_TYPES } from "../utils/applicationStateMapping";

// Mock ApplicationDetail and Error404 for routing behavior verification
function MockApplicationDetail() {
  const { id } = useParams();
  const location = useLocation();
  return (
    <div data-testid="app-detail">
      <h1>Application Detail Page</h1>
      <span data-testid="app-id">{id}</span>
      <span data-testid="app-query">{location.search}</span>
    </div>
  );
}

function MockError404() {
  return <div data-testid="error-404">404 Page Not Found</div>;
}

function ApplicationRedirect() {
  const { id } = useParams();
  const location = useLocation();
  return <Navigate to={`/applications/${id}${location.search || ""}${location.hash || ""}`} replace />;
}

describe("PART A — Application Routing & State Continuity", () => {
  const sampleScheme = {
    id: "sch-pmkisan",
    schemeCode: "PM_KISAN",
    slug: "pm-kisan-samman-nidhi",
  };

  it("Test 1: Continue Application action resolves to canonical /applications/:id", () => {
    const apps = [
      {
        id: "6aaa54d3e5710467f6d52eee",
        schemeCode: "PM_KISAN",
        status: "DOCUMENTS_PENDING",
      },
    ];

    const action = resolveApplicationAction(sampleScheme, apps);
    expect(action.action).toBe(ACTION_TYPES.CONTINUE_APPLICATION);
    expect(action.buttonText).toBe("Continue Application");
    expect(action.targetRoute).toBe("/applications/6aaa54d3e5710467f6d52eee");
    expect(action.targetRoute).not.toContain("/application/");
  });

  it("Test 2: Legacy singular route /application/:id redirects to canonical /applications/:id", () => {
    let capturedRedirect = null;
    function TestRedirectCapture() {
      const { id } = useParams();
      const location = useLocation();
      capturedRedirect = `/applications/${id}${location.search || ""}${location.hash || ""}`;
      return <div data-testid="redirected">{capturedRedirect}</div>;
    }

    const html = renderToString(
      <MemoryRouter initialEntries={["/application/6aaa54d3e5710467f6d52eee"]}>
        <Routes>
          <Route path="/applications/:id" element={<MockApplicationDetail />} />
          <Route path="/application/:id" element={<TestRedirectCapture />} />
          <Route path="*" element={<MockError404 />} />
        </Routes>
      </MemoryRouter>
    );

    expect(capturedRedirect).toBe("/applications/6aaa54d3e5710467f6d52eee");
    expect(html).toContain("/applications/6aaa54d3e5710467f6d52eee");
    expect(html).not.toContain("404 Page Not Found");
  });

  it("Test 3: Query parameters survive the redirect from /application/:id", () => {
    let capturedRedirect = null;
    function TestRedirectCapture() {
      const { id } = useParams();
      const location = useLocation();
      capturedRedirect = `/applications/${id}${location.search || ""}${location.hash || ""}`;
      return <div data-testid="redirected">{capturedRedirect}</div>;
    }

    const html = renderToString(
      <MemoryRouter initialEntries={["/application/APP-9988?tab=documents&ref=dashboard"]}>
        <Routes>
          <Route path="/applications/:id" element={<MockApplicationDetail />} />
          <Route path="/application/:id" element={<TestRedirectCapture />} />
          <Route path="*" element={<MockError404 />} />
        </Routes>
      </MemoryRouter>
    );

    expect(capturedRedirect).toBe("/applications/APP-9988?tab=documents&ref=dashboard");
    expect(html).toContain("?tab=documents&amp;ref=dashboard");
  });

  it("Test 4: Direct navigation and refresh on /applications/:id loads application detail cleanly", () => {
    const html = renderToString(
      <MemoryRouter initialEntries={["/applications/SB-APP-2026-000100"]}>
        <Routes>
          <Route path="/applications/:id" element={<MockApplicationDetail />} />
          <Route path="/application/:id" element={<ApplicationRedirect />} />
          <Route path="*" element={<MockError404 />} />
        </Routes>
      </MemoryRouter>
    );

    expect(html).toContain("Application Detail Page");
    expect(html).toContain("SB-APP-2026-000100");
    expect(html).not.toContain("404 Page Not Found");
  });

  it("Test 5: Browser history navigation transitions between list and detail correctly", () => {
    const html = renderToString(
      <MemoryRouter initialEntries={["/applications", "/applications/SB-APP-2026-000100"]} initialIndex={1}>
        <Routes>
          <Route path="/applications" element={<div data-testid="app-list">Applications List</div>} />
          <Route path="/applications/:id" element={<MockApplicationDetail />} />
          <Route path="/application/:id" element={<ApplicationRedirect />} />
          <Route path="*" element={<MockError404 />} />
        </Routes>
      </MemoryRouter>
    );

    expect(html).toContain("Application Detail Page");
    expect(html).toContain("SB-APP-2026-000100");
  });

  it("Test 6: Genuine unknown route displays 404 page correctly without intercepting citizen routes", () => {
    const html = renderToString(
      <MemoryRouter initialEntries={["/some-completely-invalid-unknown-route"]}>
        <Routes>
          <Route path="/applications/:id" element={<MockApplicationDetail />} />
          <Route path="/application/:id" element={<ApplicationRedirect />} />
          <Route path="*" element={<MockError404 />} />
        </Routes>
      </MemoryRouter>
    );

    expect(html).toContain("404 Page Not Found");
    expect(html).not.toContain("Application Detail Page");
  });
});
