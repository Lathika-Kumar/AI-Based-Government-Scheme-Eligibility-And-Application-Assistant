import React from "react";
import { ShieldCheck, FileText, Info } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function Terms() {
  const { t } = useApp();
  return (
    <div className="max-w-3xl mx-auto px-4 py-12 space-y-8">
      {/* Header */}
      <div className="border-b border-slate-200 pb-6">
        <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">{t("terms_title_main")}</h1>
        <p className="text-slate-500 text-xs mt-2 leading-relaxed">
          {t("terms_subtitle_main")}
        </p>
      </div>

      {/* Info Callout */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 flex gap-3 text-blue-800 text-xs select-none">
        <Info className="h-5 w-5 text-blue-600 shrink-0 mt-0.5" />
        <div className="space-y-1">
          <p className="font-bold">{t("terms_notice_title")}</p>
          <p className="leading-relaxed">
            {t("terms_notice_desc")}
          </p>
        </div>
      </div>

      {/* Content sections */}
      <div className="space-y-6 text-xs text-slate-600 leading-relaxed">
        {/* Section 1 */}
        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
            <FileText className="h-4.5 w-4.5 text-slate-500" />
            {t("terms_sec1_title")}
          </h2>
          <p>
            {t("terms_sec1_desc")}
          </p>
        </div>

        {/* Section 2 */}
        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
            <ShieldCheck className="h-4.5 w-4.5 text-slate-500" />
            {t("terms_sec2_title")}
          </h2>
          <p>
            {t("terms_sec2_desc1")}
          </p>
          <p>
            {t("terms_sec2_desc2")}
          </p>
        </div>

        {/* Section 3 */}
        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900">{t("terms_sec3_title")}</h2>
          <p>
            {t("terms_sec3_desc")}
          </p>
        </div>
      </div>
    </div>
  );
}
