import React, { Component } from "react";
import { AlertTriangle, RefreshCw, Home, Building2 } from "lucide-react";

/**
 * Global Error Boundary Component
 * Catches JavaScript errors anywhere in the component tree,
 * logs them, and displays a fallback UI.
 */
class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    // Update state so the next render will show the fallback UI
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    // Log the error to console
    console.error("Error Boundary caught an error:", error, errorInfo);

    // Log the error to state for display
    this.setState({
      error,
      errorInfo,
    });

    // Log to external service in production
    if (import.meta.env.PROD) {
      this.logErrorToService(error, errorInfo);
    }
  }

  logErrorToService(error, errorInfo) {
    // In production, send to error tracking service (e.g., Sentry, LogRocket)
    // For now, just log to console
    console.error("Production Error Log:", {
      message: error.message,
      stack: error.stack,
      componentStack: errorInfo.componentStack,
      timestamp: new Date().toISOString(),
    });
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
  };

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="w-full py-12 px-6 flex flex-col items-center justify-center text-center bg-white border border-gray-200 rounded-2xl shadow-sm">
          {/* Logo */}
          <div className="flex items-center gap-2 mb-6 select-none">
            <div className="bg-indigo-650 bg-indigo-600 p-1.5 rounded-lg text-white">
              <Building2 className="h-5 w-5" />
            </div>
            <span className="font-bold text-sm text-gray-800 tracking-wide">SchemeBridge</span>
          </div>

          {/* Icon */}
          <AlertTriangle className="h-16 w-16 text-amber-500 mb-4" />

          {/* Heading */}
          <h2 className="text-base font-bold text-gray-800">Something went wrong</h2>

          {/* Subtext */}
          <p className="text-xs text-gray-500 mt-1 max-w-sm">
            An unexpected error occurred in this section. Your data is safe.
          </p>

          {/* Buttons */}
          <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
            <button
              onClick={this.handleReload}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-xs"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Reload Page
            </button>
            <a
              href="/dashboard"
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-200 transition"
            >
              <Home className="h-3.5 w-3.5" />
              Go to Dashboard
            </a>
          </div>

          {/* Collapsible Details */}
          {this.state.error && (
            <details className="mt-6 text-left w-full max-w-md">
              <summary className="cursor-pointer text-xs font-semibold text-gray-500 hover:text-gray-700 select-none text-center">
                Show technical details
              </summary>
              <div className="mt-2 p-3 bg-gray-50 border border-gray-200 rounded-xl max-h-40 overflow-auto">
                <code className="text-[11px] font-mono text-red-600 break-all whitespace-pre-wrap block">
                  {this.state.error.message || String(this.state.error)}
                </code>
              </div>
            </details>
          )}
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;

