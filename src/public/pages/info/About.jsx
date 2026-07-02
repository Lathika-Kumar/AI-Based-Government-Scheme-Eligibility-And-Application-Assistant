import React from "react";
import { Link } from "react-router-dom";
import { Building2, ShieldCheck, Cpu, ArrowRight } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function About() {
  const { t } = useApp();
  return (
    <div className="max-w-4xl mx-auto px-4 py-12 space-y-10">
      {/* Header */}
      <div className="border-b border-slate-200 pb-6 text-center sm:text-left">
        <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">{t("about_title")}</h1>
        <p className="text-slate-500 text-sm mt-2 max-w-2xl leading-relaxed">
          {t("about_desc")}
        </p>
      </div>

      {/* Intro section */}
      <div className="grid md:grid-cols-3 gap-6 items-center">
        <div className="md:col-span-2 space-y-4">
          <h2 className="text-lg font-bold text-slate-900">{t("about_bridge_title")}</h2>
          <p className="text-xs text-slate-600 leading-relaxed">
            {t("about_bridge_desc1")}
          </p>
          <p className="text-xs text-slate-600 leading-relaxed">
            {t("about_bridge_desc2")}
          </p>
        </div>
        <div className="bg-slate-50 border border-slate-250 border-slate-200 p-6 rounded-2xl flex flex-col items-center justify-center text-center space-y-3">
          <div className="bg-indigo-50 p-3 rounded-2xl text-indigo-650">
            <Building2 className="h-8 w-8" />
          </div>
          <span className="text-xs font-bold text-slate-900 block">{t("about_sandbox_title")}</span>
          <span className="text-[10px] text-slate-400 font-semibold leading-relaxed">
            {t("about_sandbox_desc")}
          </span>
        </div>
      </div>

      {/* Core values */}
      <div className="space-y-4">
        <h3 className="text-sm font-bold text-slate-900 uppercase tracking-widest">{t("about_pillar_title")}</h3>
        <div className="grid sm:grid-cols-2 gap-6">
          <div className="flex gap-4 items-start p-4 border border-slate-200 rounded-xl bg-white shadow-xs">
            <div className="bg-emerald-50 text-emerald-600 p-2 rounded-lg shrink-0">
              <Cpu className="h-5 w-5" />
            </div>
            <div className="space-y-1">
              <h4 className="text-xs font-bold text-slate-900">{t("about_p1_title")}</h4>
              <p className="text-[11px] text-slate-500 leading-relaxed">
                {t("about_p1_desc")}
              </p>
            </div>
          </div>

          <div className="flex gap-4 items-start p-4 border border-slate-200 rounded-xl bg-white shadow-xs">
            <div className="bg-blue-50 text-blue-600 p-2 rounded-lg shrink-0">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div className="space-y-1">
              <h4 className="text-xs font-bold text-slate-900">{t("about_p2_title")}</h4>
              <p className="text-[11px] text-slate-500 leading-relaxed">
                {t("about_p2_desc")}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* CTA */}
      <div className="bg-indigo-900 text-white rounded-2xl p-6 sm:p-8 flex flex-col sm:flex-row items-center justify-between gap-4 shadow">
        <div className="space-y-1 text-center sm:text-left">
          <h4 className="font-bold text-sm">{t("about_ready_check")}</h4>
          <p className="text-xs text-indigo-200">{t("about_ready_desc")}</p>
        </div>
        <Link
          to="/signup"
          className="inline-flex items-center gap-1.5 bg-white text-indigo-900 hover:bg-indigo-50 px-5 py-2.5 rounded-xl text-xs font-bold transition shadow shrink-0"
        >
          <span>{t("about_get_started")}</span>
          <ArrowRight className="h-4 w-4" />
        </Link>
      </div>
    </div>
  );
}
