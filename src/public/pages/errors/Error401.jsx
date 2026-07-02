/**
 * @file Error401.jsx
 * @description 401 Unauthorized / Session Expired error page.
 */

import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { ShieldOff, LogIn, ArrowLeft, RefreshCw } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function Error401() {
  const navigate = useNavigate();
  const { t } = useApp();

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-indigo-50/30 flex items-center justify-center p-6">
      <div className="max-w-md w-full text-center space-y-8">
        {/* Error code graphic */}
        <div className="space-y-4">
          <div className="relative inline-flex">
            <div className="h-24 w-24 rounded-3xl bg-amber-100 border-2 border-amber-200 flex items-center justify-center mx-auto shadow-lg">
              <ShieldOff className="h-12 w-12 text-amber-500" />
            </div>
            <span className="absolute -top-2 -right-2 text-[10px] font-black bg-amber-500 text-white px-2 py-0.5 rounded-full border-2 border-white shadow">
              401
            </span>
          </div>

          <div className="space-y-2">
            <p className="text-[10px] font-bold text-amber-600 uppercase tracking-widest">
              {t("err_401_badge")}
            </p>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">
              {t("err_401_title")}
            </h1>
            <p className="text-sm text-slate-500 leading-relaxed max-w-xs mx-auto">
              {t("err_401_desc")}
            </p>
          </div>
        </div>

        {/* Divider */}
        <div className="border-t border-slate-200" />

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            to="/login"
            className="inline-flex items-center justify-center gap-2 bg-slate-900 hover:bg-slate-800 text-white text-sm font-bold px-6 py-3 rounded-xl transition shadow-xs"
          >
            <LogIn className="h-4 w-4" />
            {t("err_401_btn_signin")}
          </Link>
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center justify-center gap-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-sm font-bold px-6 py-3 rounded-xl transition"
          >
            <ArrowLeft className="h-4 w-4" />
            {t("err_go_back")}
          </button>
        </div>

        {/* Footer note */}
        <p className="text-xs text-slate-400">
          {t("err_troubleshoot_prefix")}{" "}
          <button
            onClick={() => window.location.reload()}
            className="text-indigo-600 hover:underline font-semibold inline-flex items-center gap-1"
          >
            <RefreshCw className="h-3 w-3" />
            {t("err_refresh_page")}
          </button>{" "}
          {t("err_contact_prefix")}{" "}
          <a
            href="mailto:support@schemebridge.gov.in"
            className="text-indigo-600 hover:underline font-semibold"
          >
            {t("err_support")}
          </a>
          .
        </p>

        {/* Branding */}
        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">
          {t("branding_egov_portal")}
        </p>
      </div>
    </div>
  );
}
