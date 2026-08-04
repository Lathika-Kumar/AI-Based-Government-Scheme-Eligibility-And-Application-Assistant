import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "@context/AuthContext";
import { AppProvider } from "@context/AppContext";
import { SchemeProvider } from "@context/SchemeContext";
import { DocumentProvider } from "@context/DocumentContext";
import { NotificationProvider } from "@context/NotificationContext";
import { ToastProvider } from "@components/ui/ToastNotification";
import LoadingSkeleton from "@components/ui/LoadingSkeleton";

// ── Layouts ───────────────────────────────────────────────────────────────────
import PublicLayout from "@public/layout/PublicLayout";
import CitizenLayout from "@user/layout/CitizenLayout";
import AdminLayout from "@admin/layout/AdminLayout";

// ── Lazy loaded pages — Public Portal & Auth ──────────────────────────────────
const Home                  = lazy(() => import("@public/pages/Home"));
const Login                 = lazy(() => import("@public/pages/Login"));
const Signup                = lazy(() => import("@public/pages/Signup"));
const AccountCreatedSuccess = lazy(() => import("@public/pages/AccountCreatedSuccess"));
const VerificationMethod   = lazy(() => import("@public/pages/VerificationMethod"));
const OtpVerification      = lazy(() => import("@public/pages/OtpVerification"));
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
const Onboarding      = lazy(() => import("@user/pages/onboarding/Onboarding"));
const Dashboard       = lazy(() => import("@user/pages/Dashboard"));
const Profile         = lazy(() => import("@user/pages/Profile"));
const Recommendations = lazy(() => import("@user/pages/schemes/Recommendations"));
const SchemeDetails   = lazy(() => import("@user/pages/schemes/SchemeDetails"));
const ApplicationWizard = lazy(() => import("@user/pages/schemes/ApplicationWizard"));
const Documents       = lazy(() => import("@user/pages/Documents"));
const Tracker         = lazy(() => import("@user/pages/Tracker"));
const Notifications   = lazy(() => import("@user/pages/Notifications"));
const Help            = lazy(() => import("@user/pages/Help"));
const Feedback        = lazy(() => import("@user/pages/Feedback"));

// ── Lazy loaded pages — Admin Portal ──────────────────────────────────────────
const AdminDashboard  = lazy(() => import("@admin/pages/AdminDashboard"));

// ── Loading fallback ──────────────────────────────────────────────────────────
const PageLoader = () => (
  <div className="min-h-screen flex items-center justify-center bg-slate-50">
    <LoadingSkeleton />
  </div>
);

// ─── Route Guards ─────────────────────────────────────────────────────────────

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

function VerificationGuard({ children }) {
  const { user, status } = useAuth();
  if (status === "ACTIVE") {
    return <Navigate to={user?.onboardingComplete ? "/dashboard" : "/onboarding"} replace />;
  }
  return children;
}

function OnboardingGuard({ children }) {
  const { onboardingComplete, isAdmin, status } = useAuth();
  if (isAdmin) {
    return children;
  }
  if (status === "PENDING_VERIFICATION") {
    return <Navigate to="/verification-method" replace />;
  }
  if (!onboardingComplete) {
    return <Navigate to="/onboarding" replace />;
  }
  return children;
}

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

function PublicGuard({ children }) {
  const { isAuthenticated, isAdmin, status, user } = useAuth();
  if (isAuthenticated) {
    if (isAdmin) return <Navigate to="/admin/dashboard" replace />;
    if (status === "PENDING_VERIFICATION") return <Navigate to="/verification-method" replace />;
    if (!user?.onboardingComplete) return <Navigate to="/onboarding" replace />;
    return <Navigate to="/dashboard" replace />;
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
          <Route path="/account-created" element={<CitizenGuard><VerificationGuard><AccountCreatedSuccess /></VerificationGuard></CitizenGuard>} />
          <Route path="/verification-method" element={<CitizenGuard><VerificationGuard><VerificationMethod /></VerificationGuard></CitizenGuard>} />
          <Route path="/otp-verification" element={<CitizenGuard><VerificationGuard><OtpVerification /></VerificationGuard></CitizenGuard>} />
          <Route path="/forgot-password" element={<PublicGuard><ForgotPassword /></PublicGuard>} />
          <Route path="/about"       element={<About />} />
          <Route path="/help-support" element={<HelpSupport />} />
          <Route path="/terms"       element={<Terms />} />
          <Route path="/disclaimer"  element={<Disclaimer />} />
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="/401" element={<Error401 />} />
          <Route path="/403" element={<Error403 />} />
          <Route path="/500" element={<Error500 />} />
          <Route path="*" element={<Error404 />} />
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

export default function App() {
  return (
    <BrowserRouter>
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
    </BrowserRouter>
  );
}
