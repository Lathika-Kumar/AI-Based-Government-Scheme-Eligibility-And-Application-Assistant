import React from "react";
import { AlertTriangle } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function Disclaimer() {
  const { t } = useApp();

  return (
    <div className="max-w-3xl mx-auto px-4 py-12 space-y-8">
      {/* Header */}
      <div className="border-b border-slate-200 pb-6">
        <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">{t("disclaimer_title_main")}</h1>
        <p className="text-slate-500 text-xs mt-2 leading-relaxed">
          {t("disclaimer_subtitle_main")}
        </p>
      </div>

      {/* Warning Callout */}
      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex gap-3 text-amber-900 text-xs select-none">
        <AlertTriangle className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
        <div className="space-y-1">
          <p className="font-bold">{t("disclaimer_warn_title")}</p>
          <p className="leading-relaxed">
            {t("disclaimer_warn_desc")}
          </p>
        </div>
      </div>

      {/* Content */}
      <div className="space-y-6 text-xs text-slate-600 leading-relaxed">
        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900">{t("disclaimer_sec1_title")}</h2>
          <p>
            {t("disclaimer_sec1_desc")}
          </p>
        </div>

        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900">{t("disclaimer_sec2_title")}</h2>
          <p>
            {t("disclaimer_sec2_desc")}
          </p>
        </div>

        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900">{t("disclaimer_sec3_title")}</h2>
          <p>
            {t("disclaimer_sec3_desc")}
          </p>
        </div>
      </div>
    </div>
  );
}
