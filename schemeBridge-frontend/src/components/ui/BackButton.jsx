import React from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";

/**
 * Reusable BackButton Component for SchemeBridge.
 * Uses React Router history navigation (`navigate(-1)`).
 * Falls back to `fallbackPath` if no meaningful history is available.
 *
 * @param {object} props
 * @param {string} [props.label="Back"] - Text label for the button
 * @param {string} [props.fallbackPath="/"] - Fallback URL if back navigation cannot be performed
 * @param {string} [props.className=""] - Additional CSS classes
 * @param {"default" | "light" | "outline" | "ghost" | "dark"} [props.variant="default"] - Styling variant
 */
export default function BackButton({
  label = "Back",
  fallbackPath = "/",
  className = "",
  variant = "default",
}) {
  const navigate = useNavigate();

  const handleBack = (e) => {
    e.preventDefault();
    console.log("[SchemeBridge Navigation] Back button clicked");
    console.log("[SchemeBridge Navigation] Navigating to previous route");

    if (window.history.state && window.history.state.idx > 0) {
      navigate(-1);
    } else {
      navigate(fallbackPath);
    }
  };

  const variantStyles = {
    default:
      "inline-flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-300 rounded-lg shadow-xs hover:bg-slate-50 hover:text-slate-900 transition-colors focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20",
    light:
      "inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-white/90 bg-white/10 hover:bg-white/20 rounded-lg backdrop-blur-xs transition-colors border border-white/20",
    outline:
      "inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-slate-600 border border-slate-200 rounded-lg hover:border-slate-300 hover:text-slate-900 transition-colors",
    ghost:
      "inline-flex items-center gap-1.5 text-xs font-medium text-slate-600 hover:text-indigo-600 transition-colors py-1 px-2 rounded-md hover:bg-slate-100",
    dark:
      "inline-flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold text-slate-200 bg-slate-800 border border-slate-700 rounded-lg hover:bg-slate-700 hover:text-white transition-colors",
  };

  return (
    <button
      type="button"
      onClick={handleBack}
      className={`${variantStyles[variant] || variantStyles.default} ${className}`}
      aria-label={label}
    >
      <ArrowLeft className="w-4 h-4 shrink-0" />
      <span>{label}</span>
    </button>
  );
}
