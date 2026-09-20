import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { usePageMeta } from "@utils/usePageMeta";
import { useMyApplications } from "@user/hooks/useApplications";
import applicationService, { REAPPLYABLE_STATUSES } from "@services/applicationService";
import { useToast } from "@components/ui/ToastNotification";
import {
  ClipboardList, RefreshCw, AlertCircle, Search, CheckCircle2,
  Clock, XCircle, AlertTriangle, ArrowRight, FileText, Send, X, RotateCcw
} from "lucide-react";

const STATUS_CONFIG = {
  DRAFT:                  { label: "Draft",                  color: "bg-gray-100 text-gray-700 border-gray-300",      icon: FileText,     dot: "bg-gray-400" },
  DOCUMENTS_PENDING:      { label: "Documents Pending",      color: "bg-amber-100 text-amber-800 border-amber-300",   icon: AlertTriangle, dot: "bg-amber-400" },
  READY_FOR_SUBMISSION:   { label: "Ready to Submit",        color: "bg-blue-100 text-blue-800 border-blue-300",      icon: CheckCircle2, dot: "bg-blue-400" },
  SUBMITTED:              { label: "Submitted",              color: "bg-indigo-100 text-indigo-800 border-indigo-300", icon: Send,         dot: "bg-indigo-400" },
  UNDER_REVIEW:           { label: "Under Review",           color: "bg-purple-100 text-purple-800 border-purple-300", icon: Clock,        dot: "bg-purple-400" },
  DOCUMENT_VERIFICATION:  { label: "Document Verification",  color: "bg-purple-100 text-purple-800 border-purple-300", icon: FileText,     dot: "bg-purple-400" },
  CORRECTION_REQUIRED:    { label: "Correction Required",    color: "bg-red-100 text-red-800 border-red-300",         icon: AlertCircle,  dot: "bg-red-400" },
  APPROVED:               { label: "Approved",               color: "bg-green-100 text-green-800 border-green-300",   icon: CheckCircle2, dot: "bg-green-400" },
  REJECTED:               { label: "Rejected",               color: "bg-red-100 text-red-800 border-red-300",         icon: XCircle,      dot: "bg-red-400" },
  CANCELLED:              { label: "Cancelled",              color: "bg-gray-100 text-gray-600 border-gray-200",      icon: X,            dot: "bg-gray-300" },
};

function getStatusConfig(status) {
  return STATUS_CONFIG[status] || STATUS_CONFIG.DRAFT;
}

function fmtDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

export default function MyApplications() {
  usePageMeta("My Applications", "Track and manage your government scheme applications");
  const showToast = useToast();
  const navigate = useNavigate();
  const { applications, loading, error, refetch } = useMyApplications();

  const [search, setSearch]   = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [cancelling, setCancelling] = useState({}); // { [id]: true }
  const [reapplyTarget, setReapplyTarget] = useState(null); // application object or null
  const [reapplying, setReapplying] = useState(false);

  const filtered = applications.filter((app) => {
    const q = search.toLowerCase();
    const titleStr = typeof app.schemeTitle === "string"
      ? app.schemeTitle
      : (app.schemeTitle?.english || app.schemeName || "");
    const matchesSearch = !q || (
      app.applicationNumber?.toLowerCase().includes(q) ||
      app.schemeCode?.toLowerCase().includes(q) ||
      titleStr.toLowerCase().includes(q)
    );
    const matchesStatus = statusFilter === "all" || app.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const handleCancel = async (app) => {
    if (!window.confirm(`Cancel application ${app.applicationNumber}? This cannot be undone.`)) return;
    setCancelling((prev) => ({ ...prev, [app.id]: true }));
    const result = await applicationService.cancelApplication(app.id);
    setCancelling((prev) => ({ ...prev, [app.id]: false }));
    if (result.error) {
      showToast(result.message || "Cancellation failed.", "error");
    } else {
      showToast(`Application ${app.applicationNumber} cancelled.`, "info");
      refetch();
    }
  };

  const handleConfirmReapply = async () => {
    if (!reapplyTarget) return;
    setReapplying(true);
    const result = await applicationService.reapplyApplication(reapplyTarget.id);
    setReapplying(false);
    if (result.error) {
      showToast(result.message || "Failed to reapply for scheme.", "error");
    } else {
      const newApp = result.data;
      showToast(`New application ${newApp.applicationNumber} created! Reused saved documents from your Document Vault.`, "success");
      setReapplyTarget(null);
      await refetch();
      navigate(`/applications/${newApp.id}`);
    }
  };

  const CANCELLABLE = ["DRAFT", "DOCUMENTS_PENDING", "READY_FOR_SUBMISSION", "SUBMITTED", "UNDER_REVIEW"];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-r from-government-blue via-government-blue-light to-government-blue text-white p-6 rounded-xl shadow-lg">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <ClipboardList className="h-5 w-5 text-saffron" />
              <span className="text-xs font-semibold text-white/80 uppercase tracking-wider">Application Tracker</span>
            </div>
            <h1 className="text-2xl font-bold">My Applications</h1>
            <p className="text-sm text-white/90 mt-1">Track the status and history of your government scheme applications.</p>
          </div>
          <button onClick={refetch} disabled={loading}
            className="flex items-center gap-2 bg-white/10 hover:bg-white/20 text-white text-sm font-semibold px-4 py-2 rounded-lg border border-white/20 transition">
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Controls */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 dark:text-slate-400" />
          <input type="text" value={search} onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by application number, scheme..."
            className="w-full pl-10 pr-4 py-2.5 text-sm border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400" />
        </div>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}
          className="text-sm border border-gray-300 dark:border-slate-700 rounded-lg px-3 py-2.5 bg-white dark:bg-slate-800 text-gray-800 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
          <option value="all">All Statuses</option>
          {Object.entries(STATUS_CONFIG).map(([k, v]) => (
            <option key={k} value={k}>{v.label}</option>
          ))}
        </select>
      </div>

      {/* Error */}
      {error && (
        <div className="flex items-center gap-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 p-4 rounded-xl text-sm">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <div>
            <p className="font-semibold">Failed to load applications</p>
            <p className="text-xs mt-0.5">{error}</p>
          </div>
          <button onClick={refetch} className="ml-auto text-xs font-bold text-red-700 dark:text-red-300 hover:underline">Retry</button>
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="space-y-3">
          {[1,2,3].map(i => <div key={i} className="bg-gray-100 dark:bg-slate-800 rounded-xl animate-pulse h-28" />)}
        </div>
      )}

      {/* Summary badges */}
      {!loading && applications.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {["APPROVED","UNDER_REVIEW","CORRECTION_REQUIRED","DOCUMENTS_PENDING"].map((s) => {
            const count = applications.filter(a => a.status === s).length;
            if (!count) return null;
            const cfg = getStatusConfig(s);
            return (
              <button key={s} onClick={() => setStatusFilter(statusFilter === s ? "all" : s)}
                className={`text-xs font-semibold px-3 py-1.5 rounded-full border flex items-center gap-1.5 transition ${statusFilter === s ? "ring-2 ring-government-blue dark:ring-indigo-400" : ""} ${cfg.color}`}>
                <span className={`w-2 h-2 rounded-full ${cfg.dot}`}></span>
                {cfg.label}: {count}
              </button>
            );
          })}
        </div>
      )}

      {/* Applications list */}
      {!loading && filtered.length > 0 && (
        <div className="space-y-4">
          {filtered.map((app) => {
            const cfg = getStatusConfig(app.status);
            const StatusIcon = cfg.icon;
            const canCancel = CANCELLABLE.includes(app.status);
            const canReapply = REAPPLYABLE_STATUSES.includes(app.status);

            return (
              <div key={app.id} className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl shadow-sm overflow-hidden">
                <div className="p-5">
                  {/* Top row */}
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border flex items-center gap-1.5 ${cfg.color}`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${cfg.dot}`}></span>
                          <StatusIcon className="h-3 w-3" />
                          {cfg.label}
                        </span>
                        <span className="text-xs text-gray-400 dark:text-slate-500 font-mono">{app.applicationNumber}</span>
                      </div>
                      <h2 className="text-base font-bold text-gray-900 dark:text-slate-100">
                        {typeof app.schemeTitle === "string" ? app.schemeTitle : (app.schemeTitle?.english || app.schemeName || app.schemeCode)}
                      </h2>
                      <p className="text-xs text-gray-500 dark:text-slate-400 mt-0.5">{app.schemeCode}</p>
                    </div>
                    <div className="text-right text-xs text-gray-500 dark:text-slate-400 shrink-0">
                      <p>Created: {fmtDate(app.createdAt)}</p>
                      {app.submittedAt && <p>Submitted: {fmtDate(app.submittedAt)}</p>}
                    </div>
                  </div>

                  {/* Document readiness */}
                  {app.documentReadiness !== undefined && (() => {
                    const readinessPct = typeof app.documentReadiness === "number"
                      ? app.documentReadiness
                      : (app.documentReadiness?.percentage ?? 0);
                    return (
                      <div className="mt-3">
                        <div className="flex justify-between text-xs text-gray-500 dark:text-slate-400 mb-1">
                          <span>Document Readiness</span>
                          <span className="font-bold text-gray-800 dark:text-slate-200">{readinessPct}%</span>
                        </div>
                        <div className="h-1.5 bg-gray-100 dark:bg-slate-800 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all ${readinessPct === 100 ? "bg-green-500" : readinessPct >= 50 ? "bg-amber-400" : "bg-red-400"}`}
                            style={{ width: `${readinessPct}%` }}
                          />
                        </div>
                      </div>
                    );
                  })()}

                  {/* Correction note */}
                  {app.status === "CORRECTION_REQUIRED" && (
                    <div className="mt-3 flex items-start gap-2 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 text-xs p-3 rounded-lg">
                      <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
                      <p>A reviewer has requested document corrections. Please upload corrected documents and resubmit.</p>
                    </div>
                  )}
                </div>

                {/* Actions */}
                <div className="border-t border-gray-100 dark:border-slate-800 px-5 py-3 flex flex-wrap gap-2">
                  <Link to={`/applications/${app.id}`}
                    className="flex-1 sm:flex-none inline-flex items-center justify-center gap-1.5 bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-xs font-semibold transition">
                    View Details <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                  {canReapply && (
                    <button
                      onClick={() => setReapplyTarget(app)}
                      className="flex-1 sm:flex-none inline-flex items-center justify-center gap-1.5 bg-amber-600 hover:bg-amber-700 active:bg-amber-800 text-white px-4 py-2 rounded-lg text-xs font-bold transition shadow-sm"
                      title="Reapply for this scheme using your saved Document Vault">
                      <RotateCcw className="h-3.5 w-3.5" /> Reapply
                    </button>
                  )}
                  {canCancel && (
                    <button
                      onClick={() => handleCancel(app)}
                      disabled={cancelling[app.id]}
                      className="flex-1 sm:flex-none inline-flex items-center justify-center gap-1.5 border border-red-300 dark:border-red-800 text-red-700 dark:text-red-300 hover:bg-red-50 dark:hover:bg-red-950/40 px-4 py-2 rounded-lg text-xs font-semibold transition disabled:opacity-50">
                      {cancelling[app.id] ? <><RefreshCw className="h-3.5 w-3.5 animate-spin" /> Cancelling...</> : <><X className="h-3.5 w-3.5" /> Cancel</>}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Reapply Confirmation Modal */}
      {reapplyTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-fade-in">
          <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-2xl max-w-md w-full shadow-2xl p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100 dark:border-slate-800">
              <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400 font-bold text-base">
                <RotateCcw className="h-5 w-5" />
                <span>Reapply for Scheme</span>
              </div>
              <button
                onClick={() => !reapplying && setReapplyTarget(null)}
                disabled={reapplying}
                className="text-gray-400 hover:text-gray-600 dark:hover:text-slate-200 p-1 rounded-lg">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-3 text-sm text-gray-600 dark:text-slate-300">
              <p className="font-semibold text-gray-900 dark:text-slate-100 text-base">
                {typeof reapplyTarget.schemeTitle === "string"
                  ? reapplyTarget.schemeTitle
                  : (reapplyTarget.schemeTitle?.english || reapplyTarget.schemeName || reapplyTarget.schemeCode)}
              </p>
              <p className="text-xs text-gray-500 dark:text-slate-400 font-mono">
                Previous Application: {reapplyTarget.applicationNumber} ({reapplyTarget.status})
              </p>

              <div className="p-3.5 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 rounded-xl text-xs text-amber-900 dark:text-amber-200 space-y-1.5">
                <p className="font-semibold flex items-center gap-1.5 text-amber-800 dark:text-amber-300">
                  <CheckCircle2 className="h-4 w-4 text-amber-600 shrink-0" />
                  Reapply for this scheme?
                </p>
                <p>
                  Your previous application will remain in your application history. Your saved documents will be reused where applicable.
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={reapplying}
                onClick={() => setReapplyTarget(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold border border-gray-300 dark:border-slate-700 text-gray-700 dark:text-slate-300 hover:bg-gray-100 dark:hover:bg-slate-800 transition disabled:opacity-50">
                Cancel
              </button>
              <button
                type="button"
                disabled={reapplying}
                onClick={handleConfirmReapply}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-amber-600 hover:bg-amber-700 active:bg-amber-800 text-white shadow-sm flex items-center gap-2 transition disabled:opacity-50">
                {reapplying ? (
                  <>
                    <RefreshCw className="h-4 w-4 animate-spin" />
                    Creating Application...
                  </>
                ) : (
                  <>
                    <RotateCcw className="h-4 w-4" />
                    Continue Reapplication
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && filtered.length === 0 && (
        <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl p-12 text-center text-gray-400 dark:text-slate-500">
          <ClipboardList className="h-12 w-12 mx-auto mb-3 opacity-30" />
          {applications.length === 0 ? (
            <>
              <p className="font-semibold text-gray-700 dark:text-slate-200">No applications yet</p>
              <p className="text-sm mt-1">Search for schemes and apply to track your applications here.</p>
              <Link to="/schemes" className="mt-4 inline-flex items-center gap-1.5 text-government-blue dark:text-indigo-400 text-sm font-semibold hover:underline">
                Browse Schemes <ArrowRight className="h-4 w-4" />
              </Link>
            </>
          ) : (
            <>
              <p className="font-semibold text-gray-700 dark:text-slate-200">No applications match your search</p>
              <button onClick={() => { setSearch(""); setStatusFilter("all"); }}
                className="mt-3 text-government-blue dark:text-indigo-400 text-sm font-semibold hover:underline">Clear filters</button>
            </>
          )}
        </div>
      )}
    </div>
  );
}
