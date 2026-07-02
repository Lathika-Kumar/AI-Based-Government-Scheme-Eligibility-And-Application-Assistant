import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { ShieldAlert, Home } from "lucide-react";

export default function Unauthorized() {
  const { isAdmin } = useAuth();
  const { t } = useApp();

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center px-4">
      <div className="max-w-md w-full text-center space-y-6">
        <div className="bg-white border border-slate-200 p-8 rounded-3xl shadow-2xl space-y-5">
          <div className="bg-rose-50 text-rose-600 p-4 rounded-2xl inline-block">
            <ShieldAlert className="h-12 w-12" />
          </div>

          <div className="space-y-2">
            <h1 className="text-4xl font-extrabold text-slate-900 tracking-tight">403</h1>
            <h2 className="text-xl font-bold text-slate-800">{t("err_unauthorized_title")}</h2>
            <p className="text-slate-500 text-sm leading-relaxed">
              {t("err_unauthorized_desc")}
            </p>
          </div>

          <div className="pt-4 flex flex-col space-y-3">
            <Link
              to={isAdmin ? "/admin/dashboard" : "/dashboard"}
              className="inline-flex items-center justify-center bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-xl text-sm font-semibold transition shadow-md hover:shadow-lg"
            >
              {t("err_btn_return_dashboard")}
            </Link>

            <Link
              to="/"
              className="inline-flex items-center justify-center bg-slate-100 hover:bg-slate-200 text-slate-700 py-3 rounded-xl text-sm font-semibold transition"
            >
              <Home className="h-4 w-4 mr-2" />
              {t("err_btn_go_homepage")}
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
