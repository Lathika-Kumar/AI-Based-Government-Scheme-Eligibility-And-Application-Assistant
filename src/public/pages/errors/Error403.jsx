/**
 * @file Error403.jsx
 * @description 403 Forbidden / Access Denied error page.
 */

import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { Ban, Home, ArrowLeft, Mail } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function Error403() {
  const navigate = useNavigate();
  const { t } = useApp();

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-rose-50/20 flex items-center justify-center p-6">
      <div className="max-w-md w-full text-center space-y-8">
        {/* Error code graphic */}
        <div className="space-y-4">
          <div className="relative inline-flex">
            <div className="h-24 w-24 rounded-3xl bg-rose-100 border-2 border-rose-200 flex items-center justify-center mx-auto shadow-lg">
              <Ban className="h-12 w-12 text-rose-500" />
            </div>
            <span className="absolute -top-2 -right-2 text-[10px] font-black bg-rose-500 text-white px-2 py-0.5 rounded-full border-2 border-white shadow">
              403
            </span>
          </div>

          <div className="space-y-2">
            <p className="text-[10px] font-bold text-rose-600 uppercase tracking-widest">
              {t("err_403_badge")}
            </p>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">
              {t("err_403_title")}
            </h1>
            <p className="text-sm text-slate-500 leading-relaxed max-w-xs mx-auto">
              {t("err_403_desc")}
            </p>
          </div>
        </div>

        {/* Permission info box */}
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 text-left space-y-1">
          <p className="text-xs font-bold text-rose-800">{t("err_403_why")}</p>
          <ul className="text-xs text-rose-700 space-y-1 list-disc list-inside leading-relaxed">
            <li>{t("err_403_reason_1")}</li>
            <li>{t("err_403_reason_2")}</li>
            <li>{t("err_403_reason_3")}</li>
          </ul>
        </div>

        {/* Divider */}
        <div className="border-t border-slate-200" />

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            to="/dashboard"
            className="inline-flex items-center justify-center gap-2 bg-slate-900 hover:bg-slate-800 text-white text-sm font-bold px-6 py-3 rounded-xl transition shadow-xs"
          >
            <Home className="h-4 w-4" />
            {t("err_btn_go_dashboard")}
          </Link>
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center justify-center gap-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-sm font-bold px-6 py-3 rounded-xl transition"
          >
            <ArrowLeft className="h-4 w-4" />
            {t("err_go_back")}
          </button>
        </div>

        {/* Contact support */}
        <a
          href="mailto:support@schemebridge.gov.in"
          className="inline-flex items-center gap-1.5 text-xs text-indigo-600 hover:underline font-semibold"
        >
          <Mail className="h-3.5 w-3.5" />
          {t("err_btn_contact_support")}
        </a>

        {/* Branding */}
        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">
          {t("branding_egov_portal")}
        </p>
      </div>
    </div>
  );
}
