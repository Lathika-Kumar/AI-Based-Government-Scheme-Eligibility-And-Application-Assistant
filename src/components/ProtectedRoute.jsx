import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * Guard for Citizen pages
 */
export const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, isAdmin } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (isAdmin) {
    // Admin should not visit citizen pages, redirect to admin home
    return <Navigate to="/admin/dashboard" replace />;
  }

  return children;
};

/**
 * Guard for Admin pages
 */
export const AdminRoute = ({ children }) => {
  const { isAuthenticated, isAdmin } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin) {
    // Citizen should not visit admin pages, redirect to unauthorized
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
};

/**
 * Guard for Guest/Public Auth pages (Login, Signup)
 * Redirects logged in users to their respective portals
 */
export const PublicRoute = ({ children }) => {
  const { isAuthenticated, isAdmin } = useAuth();

  if (isAuthenticated) {
    if (isAdmin) {
      return <Navigate to="/admin/dashboard" replace />;
    }
    return <Navigate to="/dashboard" replace />;
  }

  return children;
};
