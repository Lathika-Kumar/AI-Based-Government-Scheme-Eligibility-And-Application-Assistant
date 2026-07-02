import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { HelpCircle, ArrowLeft, LayoutDashboard, Home } from "lucide-react";

export default function NotFound() {
  const { isAuthenticated, isAdmin } = useAuth();
  const { t } = useApp();

  return (
    <div className="max-w-md mx-auto my-20 text-center px-4 space-y-6">
      <div className="bg-white border border-slate-200 p-8 rounded-2xl shadow-lg space-y-4">
        <div className="bg-slate-100 text-slate-500 p-4 rounded-full inline-block">
          <HelpCircle className="h-12 w-12" />
        </div>

        <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">{t("err_notfound_title")}</h1>

        <p className="text-slate-600 text-sm leading-relaxed">
          {t("err_notfound_desc")}
        </p>

        <div className="pt-4 flex flex-col space-y-2.5">
          {isAuthenticated ? (
            <Link
              to={isAdmin ? "/admin/dashboard" : "/dashboard"}
              className="inline-flex items-center justify-center space-x-2 bg-indigo-650 bg-indigo-600 hover:bg-indigo-700 text-white py-2.5 rounded-lg text-sm font-semibold transition"
            >
              <LayoutDashboard className="h-4 w-4" />
              <span>{t("err_notfound_btn_dashboard")}</span>
            </Link>
          ) : (
            <Link
              to="/"
              className="inline-flex items-center justify-center space-x-2 bg-slate-900 hover:bg-slate-800 text-white py-2.5 rounded-lg text-sm font-semibold transition"
            >
              <Home className="h-4 w-4" />
              <span>{t("err_notfound_btn_home")}</span>
            </Link>
          )}

          <Link
            to={-1}
            className="inline-flex items-center justify-center space-x-1.5 text-slate-500 hover:text-slate-800 text-xs font-semibold py-1.5 transition"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            <span>{t("err_go_back_prev")}</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
