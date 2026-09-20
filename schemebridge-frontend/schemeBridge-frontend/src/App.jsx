import { lazy, Suspense, useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate, useLocation, useParams } from "react-router-dom";
import { AuthProvider, useAuth } from "@context/AuthContext";
import { AppProvider } from "@context/AppContext";
import { SchemeProvider } from "@context/SchemeContext";
import { DocumentProvider } from "@context/DocumentContext";
import { NotificationProvider } from "@context/NotificationContext";
import { ThemeProvider } from "@context/ThemeContext";
import { ToastProvider } from "@components/ui/ToastNotification";
import LoadingSkeleton from "@components/ui/LoadingSkeleton";
import { storage } from "@utils/apiClient";

// ── Layouts ───────────────────────────────────────────────────────────────────
import PublicLayout from "@public/layout/PublicLayout";
import CitizenLayout from "@user/layout/CitizenLayout";
import AdminLayout from "@admin/layout/AdminLayout";

// ── Lazy loaded pages — Public Portal & Auth ──────────────────────────────────
const Home                  = lazy(() => import("@public/pages/Home"));
const Login                 = lazy(() => import("@public/pages/Login"));
const Signup                = lazy(() => import("@public/pages/Signup"));
const AccountCreatedSuccess = lazy(() => import("@public/pages/AccountCreatedSuccess"));
const OtpVerification       = lazy(() => import("@public/pages/OtpVerification"));
const ForgotPassword        = lazy(() => import("@public/pages/ForgotPassword"));
const About                 = lazy(() => import("@public/pages/info/About"));
const HelpSupport           = lazy(() => import("@public/pages/info/Help"));
const Terms                 = lazy(() => import("@public/pages/info/Terms"));
const Disclaimer            = lazy(() => import("@public/pages/info/Disclaimer"));
const Unauthorized          = lazy(() => import("@public/pages/Unauthorized"));

// ── Lazy loaded pages — Error Pages ───────────────────────────────────────────
const Error401 = lazy(() => import("@public/pages/errors/Error401"));
const Error403 = lazy(() => import("@public/pages/errors/Error403"));
const Error404 = lazy(() => import("@public/pages/errors/Error404"));
const Error500 = lazy(() => import("@public/pages/errors/Error500"));

// ── Lazy loaded pages — Citizen Portal ────────────────────────────────────────
const Onboarding        = lazy(() => import("@user/pages/onboarding/Onboarding"));
const Dashboard         = lazy(() => import("@user/pages/Dashboard"));
const Profile           = lazy(() => import("@user/pages/Profile"));
const Recommendations   = lazy(() => import("@user/pages/schemes/Recommendations"));
const SchemeSearch      = lazy(() => import("@user/pages/schemes/SchemeSearch"));
const SchemeDetails     = lazy(() => import("@user/pages/schemes/SchemeDetails"));
const ApplicationWizard = lazy(() => import("@user/pages/schemes/ApplicationWizard"));
const EligibilityChecker = lazy(() => import("@user/pages/EligibilityChecker"));
const MyApplications    = lazy(() => import("@user/pages/MyApplications"));
const ApplicationDetail = lazy(() => import("@user/pages/ApplicationDetail"));
const Documents         = lazy(() => import("@user/pages/Documents"));
const Tracker           = lazy(() => import("@user/pages/Tracker"));
const Notifications     = lazy(() => import("@user/pages/Notifications"));
const Help              = lazy(() => import("@user/pages/Help"));
const Feedback          = lazy(() => import("@user/pages/Feedback"));
const Settings          = lazy(() => import("@user/pages/Settings"));

// ── Lazy loaded pages — Admin Portal ──────────────────────────────────────────
const AdminDashboard        = lazy(() => import("@admin/pages/AdminDashboard"));
const AdminApplicationReview = lazy(() => import("@admin/pages/AdminApplicationReview"));

// ── Loading fallback ──────────────────────────────────────────────────────────
const PageLoader = () => (
  <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-950">
    <LoadingSkeleton />
  </div>
);

// ── Route Tracker ─────────────────────────────────────────────────────────────
// Automatically remembers the last protected route visited by an authenticated user.
function RouteTracker() {
  const location = useLocation();
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (isAuthenticated) {
      const path = location.pathname;
      const EXCLUDED_PREFIXES = [
        "/login", "/signup", "/account-created", "/otp-verification",
        "/forgot-password", "/unauthorized", "/401", "/403", "/404", "/500",
        "/about", "/help-support", "/terms", "/disclaimer"
      ];
      const isExcluded = EXCLUDED_PREFIXES.some(p => path === p || path.startsWith(p + "/"));

      if (!isExcluded && path !== "/") {
        const fullRoute = location.pathname + (location.search || "") + (location.hash || "");
        storage.setLastProtectedRoute(fullRoute);
      }
    }
  }, [location, isAuthenticated]);

  return null;
}

// ─── Route Guards ─────────────────────────────────────────────────────────────
// All guards defer rendering while auth session is being restored from /me.
// This prevents flash-redirects to /login on page refresh for logged-in users.

/** Citizen-only routes: redirect unauthenticated → /login, admin → /admin/dashboard */
function CitizenGuard({ children }) {
  const { isAuthenticated, isAdmin, loading } = useAuth();
  const location = useLocation();

  if (loading) return <PageLoader />;
  if (!isAuthenticated) {
    const fullPath = location.pathname + (location.search || "") + (location.hash || "");
    storage.setLastProtectedRoute(fullPath);
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (isAdmin) return <Navigate to="/admin/dashboard" replace />;
  return children;
}

/** Onboarding guard: citizens who haven't completed onboarding go to /onboarding */
function OnboardingGuard({ children }) {
  const { user, isAdmin, loading } = useAuth();
  if (loading) return <PageLoader />;
  if (isAdmin) return children;
  if (!user?.onboardingComplete) return <Navigate to="/onboarding" replace />;
  return children;
}

/** Admin-only routes: redirect unauthenticated → /login, citizens → /dashboard */
function AdminGuard({ children }) {
  const { isAuthenticated, isAdmin, loading } = useAuth();
  const location = useLocation();

  if (loading) return <PageLoader />;
  if (!isAuthenticated) {
    const fullPath = location.pathname + (location.search || "") + (location.hash || "");
    storage.setLastProtectedRoute(fullPath);
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (!isAdmin) return <Navigate to="/dashboard" replace />;
  return children;
}

/** Public-only routes: redirect logged-in users to their portal */
function PublicGuard({ children }) {
  const { isAuthenticated, isAdmin, user, loading } = useAuth();
  if (loading) return <PageLoader />;
  if (isAuthenticated) {
    if (isAdmin) return <Navigate to="/admin/dashboard" replace />;
    if (!user?.onboardingComplete) return <Navigate to="/onboarding" replace />;
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}

/**
 * Compatibility redirect: legacy /application/:id -> canonical /applications/:id
 * Preserves the application ID, search query parameters, and hash without 404.
 */
function ApplicationRedirect() {
  const { id } = useParams();
  const location = useLocation();
  return <Navigate to={`/applications/${id}${location.search || ""}${location.hash || ""}`} replace />;
}

// ─── AppRoutes ─────────────────────────────────────────────────────────────────

function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <RouteTracker />
      <Routes>

        {/* ── PUBLIC ROUTES ─────────────────────────────────────────────────── */}
        <Route element={<PublicLayout />}>
          <Route path="/"            element={<Home />} />
          <Route path="/login"       element={<PublicGuard><Login /></PublicGuard>} />
          <Route path="/signup"      element={<PublicGuard><Signup /></PublicGuard>} />
          <Route path="/account-created" element={<AccountCreatedSuccess />} />
          <Route path="/otp-verification" element={<OtpVerification />} />
          <Route path="/forgot-password" element={<PublicGuard><ForgotPassword /></PublicGuard>} />
          <Route path="/about"       element={<About />} />
          <Route path="/help-support" element={<HelpSupport />} />
          <Route path="/terms"       element={<Terms />} />
          <Route path="/disclaimer"  element={<Disclaimer />} />
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="/401" element={<Error401 />} />
          <Route path="/403" element={<Error403 />} />
          <Route path="/500" element={<Error500 />} />
        </Route>

        {/* ── CITIZEN ROUTES ────────────────────────────────────────────────── */}
        <Route
          path="/onboarding"
          element={<CitizenGuard><Onboarding /></CitizenGuard>}
        />
        <Route
          element={
            <CitizenGuard>
              <OnboardingGuard>
                <CitizenLayout />
              </OnboardingGuard>
            </CitizenGuard>
          }
        >
          <Route path="/dashboard"       element={<Dashboard />} />
          <Route path="/profile"         element={<Profile />} />
          <Route path="/recommendations" element={<Recommendations />} />
          <Route path="/schemes"         element={<Recommendations />} />
          <Route path="/eligibility"     element={<EligibilityChecker />} />
          <Route path="/applications"    element={<MyApplications />} />
          <Route path="/applications/:id" element={<ApplicationDetail />} />
          <Route path="/application/:id" element={<ApplicationRedirect />} />
          <Route path="/scheme/:id"      element={<SchemeDetails />} />
          <Route path="/scheme/:id/apply" element={<ApplicationWizard />} />
          <Route path="/documents"       element={<Documents />} />
          <Route path="/tracker"         element={<Tracker />} />
          <Route path="/notifications"   element={<Notifications />} />
          <Route path="/help"            element={<Help />} />
          <Route path="/feedback"        element={<Feedback />} />
          <Route path="/settings"        element={<Settings />} />
        </Route>

        {/* ── ADMIN ROUTES ──────────────────────────────────────────────────── */}
        <Route path="/admin" element={<AdminGuard><AdminLayout /></AdminGuard>}>
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard"    element={<AdminDashboard tab="overview" />} />
          <Route path="schemes"      element={<AdminDashboard tab="schemes" />} />
          <Route path="applications" element={<AdminDashboard tab="applications" />} />
          {/* New backend-integrated admin review page */}
          <Route path="review"       element={<AdminApplicationReview />} />
          <Route path="review/:applicationId" element={<AdminApplicationReview />} />
          <Route path="documents"    element={<AdminDashboard tab="documents" />} />
          <Route path="users"        element={<AdminDashboard tab="users" />} />
          <Route path="grievances"   element={<AdminDashboard tab="grievances" />} />
          <Route path="analytics-reports" element={<AdminDashboard tab="analytics-reports" />} />
          <Route path="analytics"    element={<Navigate to="/admin/analytics-reports" replace />} />
          <Route path="feedback"     element={<AdminDashboard tab="feedback" />} />
          <Route path="reports"      element={<Navigate to="/admin/analytics-reports" replace />} />
          <Route path="audit"        element={<AdminDashboard tab="audits" />} />
          <Route path="notifications" element={<AdminDashboard tab="notifications" />} />
          <Route path="settings"     element={<AdminDashboard tab="settings" />} />
        </Route>

        {/* ── GLOBAL 404 CATCH-ALL ROUTE ───────────────────────────────────── */}
        <Route element={<PublicLayout />}>
          <Route path="*" element={<Error404 />} />
        </Route>
      </Routes>
    </Suspense>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AuthProvider>
          <SchemeProvider>
            <DocumentProvider>
              <NotificationProvider>
                <ToastProvider>
                  <AppProvider>
                    <AppRoutes />
                  </AppProvider>
                </ToastProvider>
              </NotificationProvider>
            </DocumentProvider>
          </SchemeProvider>
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
}
