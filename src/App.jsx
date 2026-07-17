import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "@context/AuthContext";
import { useAuth } from "@context/AuthContext";
import { AppProvider } from "@context/AppContext";
import { SchemeProvider } from "@context/SchemeContext";
import { DocumentProvider } from "@context/DocumentContext";
import { NotificationProvider } from "@context/NotificationContext";
import LoadingSkeleton from "@components/ui/LoadingSkeleton";

// ── Layouts ───────────────────────────────────────────────────────────────────
import PublicLayout from "@public/layout/PublicLayout";
import CitizenLayout from "@user/layout/CitizenLayout";
import AdminLayout from "@admin/layout/AdminLayout";

// ── Lazy loaded pages — Public Portal ────────────────────────────────────────
const Home         = lazy(() => import("@public/pages/Home"));
const Login        = lazy(() => import("@public/pages/Login"));
const Signup       = lazy(() => import("@public/pages/Signup"));
const About        = lazy(() => import("@public/pages/info/About"));
const HelpSupport  = lazy(() => import("@public/pages/info/Help"));
const Terms        = lazy(() => import("@public/pages/info/Terms"));
const Disclaimer   = lazy(() => import("@public/pages/info/Disclaimer"));
const Unauthorized = lazy(() => import("@public/pages/Unauthorized"));

// ── Lazy loaded pages — Public Error Pages ────────────────────────────────────
const Error401 = lazy(() => import("@public/pages/errors/Error401"));
const Error403 = lazy(() => import("@public/pages/errors/Error403"));
const Error404 = lazy(() => import("@public/pages/errors/Error404"));
const Error500 = lazy(() => import("@public/pages/errors/Error500"));

// ── Lazy loaded pages — User (Citizen) Portal ─────────────────────────────────
const Onboarding     = lazy(() => import("@user/pages/onboarding/Onboarding"));
const Dashboard      = lazy(() => import("@user/pages/Dashboard"));
const Profile        = lazy(() => import("@user/pages/Profile"));
const Recommendations = lazy(() => import("@user/pages/schemes/Recommendations"));
const SchemeDetails  = lazy(() => import("@user/pages/schemes/SchemeDetails"));
const ApplicationWizard = lazy(() => import("@user/pages/schemes/ApplicationWizard"));
const Documents      = lazy(() => import("@user/pages/Documents"));
const Tracker        = lazy(() => import("@user/pages/Tracker"));
const Notifications  = lazy(() => import("@user/pages/Notifications"));
const Help           = lazy(() => import("@user/pages/Help"));
const Feedback       = lazy(() => import("@user/pages/Feedback"));

// ── Lazy loaded pages — Admin Portal ─────────────────────────────────────────
const AdminDashboard = lazy(() => import("@admin/pages/AdminDashboard"));

// ── Loading fallback ─────────────────────────────────────────────────────────
const PageLoader = () => (
  <div className="min-h-screen flex items-center justify-center bg-slate-50">
    <LoadingSkeleton />
  </div>
);

// ─── Route Guards ─────────────────────────────────────────────────────────────

/**
 * CitizenGuard — Redirect unauthenticated users to /login.
 * Redirect admins to the admin dashboard.
 */
function CitizenGuard({ children }) {
  const { isAuthenticated, isAdmin } = useAuth();
  if (!isAuthenticated) {
return <Navigate to="/login" replace />;
}
  if (isAdmin) {
return <Navigate to="/admin/dashboard" replace />;
}
  return children;
}

/**
 * OnboardingGuard — Redirect citizens who haven't finished onboarding.
 * Applied to all citizen routes EXCEPT /onboarding itself.
 * Admins bypass onboarding entirely.
 */
function OnboardingGuard({ children }) {
  const { onboardingComplete, isAdmin } = useAuth();
  if (isAdmin) {
return children;
}
  if (!onboardingComplete) {
return <Navigate to="/onboarding" replace />;
}
  return children;
}

/**
 * AdminGuard — Restrict access to admin-only routes.
 * Redirects unauthenticated users to /login, citizens to their own /dashboard.
 */
function AdminGuard({ children }) {
  const { isAuthenticated, isAdmin } = useAuth();
  if (!isAuthenticated) {
return <Navigate to="/login" replace />;
}
  if (!isAdmin) {
return <Navigate to="/dashboard" replace />;
}
  return children;
}

/**
 * PublicGuard — Redirect already-authenticated users away from /login and /signup.
 */
function PublicGuard({ children }) {
  const { isAuthenticated, isAdmin } = useAuth();
  if (isAuthenticated) {
    return <Navigate to={isAdmin ? "/admin/dashboard" : "/dashboard"} replace />;
  }
  return children;
}

// ─── AppRoutes ─────────────────────────────────────────────────────────────────

function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        {/* ── PUBLIC ROUTES ─────────────────────────────────────────────────── */}
        <Route element={<PublicLayout />}>
          <Route path="/"            element={<Home />} />
          <Route path="/login"       element={<PublicGuard><Login /></PublicGuard>} />
          <Route path="/signup"      element={<PublicGuard><Signup /></PublicGuard>} />
          <Route path="/about"       element={<About />} />
          <Route path="/help-support" element={<HelpSupport />} />
          <Route path="/terms"       element={<Terms />} />
          <Route path="/disclaimer"  element={<Disclaimer />} />
          <Route path="/unauthorized" element={<Unauthorized />} />
          {/* Error pages (accessible directly) */}
          <Route path="/401" element={<Error401 />} />
          <Route path="/403" element={<Error403 />} />
          <Route path="/500" element={<Error500 />} />
          {/* 404 catch-all */}
          <Route path="*" element={<Error404 />} />
        </Route>

        {/* ── CITIZEN ROUTES ────────────────────────────────────────────────── */}
        {/* Onboarding — auth required, no sidebar layout */}
        <Route
          path="/onboarding"
          element={<CitizenGuard><Onboarding /></CitizenGuard>}
        />
        {/* Citizen portal — auth + onboarding required */}
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
          <Route path="/scheme/:id"      element={<SchemeDetails />} />
          <Route path="/scheme/:id/apply" element={<ApplicationWizard />} />
          <Route path="/documents"       element={<Documents />} />
          <Route path="/tracker"         element={<Tracker />} />
          <Route path="/notifications"   element={<Notifications />} />
          <Route path="/help"            element={<Help />} />
          <Route path="/feedback"        element={<Feedback />} />
        </Route>

        {/* ── ADMIN ROUTES ──────────────────────────────────────────────────── */}
        <Route path="/admin" element={<AdminGuard><AdminLayout /></AdminGuard>}>
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard"    element={<AdminDashboard tab="overview" />} />
          <Route path="schemes"      element={<AdminDashboard tab="schemes" />} />
          <Route path="applications" element={<AdminDashboard tab="applications" />} />
          <Route path="documents"    element={<AdminDashboard tab="documents" />} />
          <Route path="users"        element={<AdminDashboard tab="users" />} />
          <Route path="grievances"   element={<AdminDashboard tab="grievances" />} />
          <Route path="analytics"    element={<AdminDashboard tab="analytics" />} />
          <Route path="reports"      element={<AdminDashboard tab="reports" />} />
          <Route path="audit"        element={<AdminDashboard tab="audits" />} />
          <Route path="notifications" element={<AdminDashboard tab="notifications" />} />
          <Route path="settings"     element={<AdminDashboard tab="settings" />} />
        </Route>
      </Routes>
    </Suspense>
  );
}

// ─── App ───────────────────────────────────────────────────────────────────────

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <SchemeProvider>
          <DocumentProvider>
            <NotificationProvider>
              <AppProvider>
                <AppRoutes />
              </AppProvider>
            </NotificationProvider>
          </DocumentProvider>
        </SchemeProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
