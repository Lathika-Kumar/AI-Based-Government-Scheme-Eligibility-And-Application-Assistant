/**
 * @file Error500.jsx
 * @description 500 Internal Server Error / unexpected failure page.
 */

import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { ServerCrash, RefreshCw, Home, ArrowLeft, Mail } from "lucide-react";

export default function Error500() {
  const navigate = useNavigate();

  const handleRetry = () => {
    window.location.reload();
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-violet-50/20 flex items-center justify-center p-6">
      <div className="max-w-md w-full text-center space-y-8">
        {/* Error code graphic */}
        <div className="space-y-4">
          <div className="relative inline-flex">
            <div className="h-24 w-24 rounded-3xl bg-violet-100 border-2 border-violet-200 flex items-center justify-center mx-auto shadow-lg">
              <ServerCrash className="h-12 w-12 text-violet-500" />
            </div>
            <span className="absolute -top-2 -right-2 text-[10px] font-black bg-violet-600 text-white px-2 py-0.5 rounded-full border-2 border-white shadow">
              500
            </span>
          </div>

          <div className="space-y-2">
            <p className="text-[10px] font-bold text-violet-600 uppercase tracking-widest">
              SYSTEM UNEXPECTED FAILURE
            </p>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">
              Internal System Error
            </h1>
            <p className="text-sm text-slate-500 leading-relaxed max-w-xs mx-auto">
              An unexpected technical issue occurred while processing your request. Our technical team has been notified.
            </p>
          </div>
        </div>

        {/* What you can do */}
        <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 text-left space-y-2">
          <p className="text-xs font-bold text-slate-700">Recommended actions:</p>
          <ul className="text-xs text-slate-600 space-y-1.5">
            <li className="flex items-start gap-2">
              <span className="text-violet-500 font-black mt-0.5">✦</span>
              <span>Click 'Try Again' below to refresh the current transaction.</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-violet-500 font-black mt-0.5">✦</span>
              <span>Check your network connection and verify browser storage settings.</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-violet-500 font-black mt-0.5">✦</span>
              <span>Return to the home page and retry your application.</span>
            </li>
          </ul>
        </div>

        {/* Status indicator */}
        <div className="flex items-center justify-center gap-2">
          <span className="h-2 w-2 rounded-full bg-amber-500 animate-pulse" />
          <span className="text-xs text-slate-500 font-medium">
            Platform status: Investigating
          </span>
        </div>

        {/* Divider */}
        <div className="border-t border-slate-200" />

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <button
            onClick={handleRetry}
            className="inline-flex items-center justify-center gap-2 bg-slate-900 hover:bg-slate-800 text-white text-sm font-bold px-6 py-3 rounded-xl transition shadow-xs"
          >
            <RefreshCw className="h-4 w-4" />
            Try Again
          </button>
          <Link
            to="/"
            className="inline-flex items-center justify-center gap-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-sm font-bold px-6 py-3 rounded-xl transition"
          >
            <Home className="h-4 w-4" />
            Return to Portal Home
          </Link>
        </div>

        {/* Help link */}
        <div className="flex flex-col items-center gap-1.5">
          <p className="text-xs text-slate-400">
            Government Helpline:{" "}
            <a href="tel:18003456789" className="text-indigo-600 font-semibold hover:underline">
              1800-345-6789
            </a>
          </p>
          <a
            href="mailto:support@schemebridge.gov.in"
            className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-indigo-600 transition font-medium"
          >
            <Mail className="h-3.5 w-3.5" />
            support@schemebridge.gov.in
          </a>
        </div>

        {/* Branding */}
        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">
          SchemeBridge — e-Governance Citizen Portal
        </p>
      </div>
    </div>
  );
}
