/**
 * @file Error404.jsx
 * @description 404 Not Found error page.
 * Standalone version of the existing NotFound.jsx, enhanced with the new error page design system.
 */

import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { Search, Home, ArrowLeft, Compass } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function Error404() {
  const navigate = useNavigate();
  const { t } = useApp();

  const QUICK_LINKS = [
    { labelKey: "nav_dashboard", path: "/dashboard", descKey: "err_404_quick_dashboard" },
    { labelKey: "nav_schemes",   path: "/recommendations", descKey: "err_404_quick_recs" },
    { labelKey: "nav_vault",     path: "/documents",       descKey: "err_404_quick_vault" },
    { labelKey: "nav_tracker",   path: "/tracker",         descKey: "err_404_quick_tracker" },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-indigo-50/20 flex items-center justify-center p-6">
      <div className="max-w-lg w-full text-center space-y-8">
        {/* Error code graphic */}
        <div className="space-y-4">
          {/* Large 404 display */}
          <div className="relative">
            <p className="text-[120px] sm:text-[160px] font-black text-slate-100 leading-none select-none tracking-tighter">
              404
            </p>
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="h-20 w-20 rounded-3xl bg-white border-2 border-slate-200 flex items-center justify-center shadow-xl">
                <Compass className="h-10 w-10 text-indigo-500" />
              </div>
            </div>
          </div>

          <div className="space-y-2">
            <p className="text-[10px] font-bold text-indigo-600 uppercase tracking-widest">
              {t("err_404_badge")}
            </p>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">
              {t("err_404_title")}
            </h1>
            <p className="text-sm text-slate-500 leading-relaxed max-w-sm mx-auto">
              {t("err_404_desc")}
            </p>
          </div>
        </div>

        {/* Quick navigation links */}
        <div className="grid grid-cols-2 gap-2 text-left">
          {QUICK_LINKS.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className="group p-3 bg-white border border-slate-200 rounded-xl hover:border-indigo-300 hover:shadow-xs transition"
            >
              <p className="text-xs font-bold text-slate-800 group-hover:text-indigo-700 transition">
                {t(link.labelKey)}
              </p>
              <p className="text-[10px] text-slate-400 mt-0.5 leading-tight">
                {t(link.descKey)}
              </p>
            </Link>
          ))}
        </div>

        {/* Divider */}
        <div className="border-t border-slate-200" />

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            to="/"
            className="inline-flex items-center justify-center gap-2 bg-slate-900 hover:bg-slate-800 text-white text-sm font-bold px-6 py-3 rounded-xl transition shadow-xs"
          >
            <Home className="h-4 w-4" />
            {t("err_404_btn_gohome")}
          </Link>
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center justify-center gap-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-sm font-bold px-6 py-3 rounded-xl transition"
          >
            <ArrowLeft className="h-4 w-4" />
            {t("err_go_back")}
          </button>
        </div>

        {/* Search hint */}
        <div className="inline-flex items-center gap-1.5 text-xs text-slate-400">
          <Search className="h-3.5 w-3.5" />
          {t("err_404_search_hint_prefix")}{" "}
          <Link to="/recommendations" className="text-indigo-600 hover:underline font-semibold">
            {t("nav_schemes")}
          </Link>{" "}
          {t("err_404_search_hint_suffix")}
        </div>

        {/* Branding */}
        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">
          {t("branding_egov_portal")}
        </p>
      </div>
    </div>
  );
}
