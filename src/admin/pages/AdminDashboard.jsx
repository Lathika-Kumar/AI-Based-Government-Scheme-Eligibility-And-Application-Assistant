import React, { useState, useEffect } from "react";
import { useAuth } from "@context/AuthContext";
import { useApp } from "@context/AppContext";
import { CONFIG } from "@config/env";
import { AlertTriangle, ShieldCheck } from "lucide-react";

// Sub-components
import DashboardOverview from "./components/DashboardOverview";
import AnalyticsDashboard from "./components/AnalyticsDashboard";
import ApplicationsManagement from "./components/ApplicationsManagement";
import ApplicationReviewWorkspace from "./components/ApplicationReviewWorkspace";
import DocumentVerificationCenter from "./components/DocumentVerificationCenter";
import SchemeManagementConsole from "./components/SchemeManagementConsole";
import UserManagementConsole from "./components/UserManagementConsole";
import GrievanceManagementDesk from "./components/GrievanceManagementDesk";
import AuditLogsConsole from "./components/AuditLogsConsole";
import AdminNotificationsCenter from "./components/AdminNotificationsCenter";
import AdminSettingsPanel from "./components/AdminSettingsPanel";
import GovernmentReportsCenter from "./components/GovernmentReportsCenter";

// Loading Skeleton component
function SkeletonLoader({ type }) {
  if (type === "overview" || type === "analytics") {
    return (
      <div className="space-y-6 animate-pulse select-none">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-28 bg-slate-200 rounded-2xl"></div>
          ))}
        </div>
        <div className="h-44 bg-slate-200 rounded-2xl"></div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-64 bg-slate-200 rounded-2xl"></div>
          ))}
        </div>
      </div>
    );
  }
  if (type === "applications" || type === "audit" || type === "grievances") {
    return (
      <div className="space-y-4 animate-pulse select-none">
        <div className="h-16 bg-slate-200 rounded-2xl"></div>
        <div className="bg-white border border-slate-200 rounded-2xl p-6 space-y-4">
          <div className="h-6 bg-slate-200 rounded w-1/4"></div>
          <div className="space-y-2">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-10 bg-slate-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }
  return (
    <div className="space-y-4 animate-pulse select-none">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-56 bg-slate-200 rounded-2xl"></div>
        ))}
      </div>
    </div>
  );
}

export default function AdminDashboard({ tab = "overview" }) {
  const { user } = useAuth();
  const {
    schemes,
    addScheme,
    editScheme,
    deleteScheme,
    applications,
    updateApplicationStatus,
    auditLogs,
    grievances,
    updateGrievanceStatus,
    documents,
    updateDocumentStatus,
    t
  } = useApp();

  const [selectedTab, setSelectedTab] = useState(tab);
  const [selectedAppForReview, setSelectedAppForReview] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Safeguard modal confirmation state
  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    title: "",
    message: "",
    onConfirm: null
  });

  // Watch tab change to simulate premium loading transition
  useEffect(() => {
    setSelectedTab(tab);
    setIsLoading(true);
    setSelectedAppForReview(null);
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 450); // Sleek 450ms shimmer timeout
    return () => clearTimeout(timer);
  }, [tab]);

  // Secure triage action trigger
  const secureUpdateApplicationStatus = (appId, newStage) => {
    if (newStage === "Rejected") {
      setConfirmModal({
        isOpen: true,
        title: "Confirm Reject Application",
        message: `Warning: You are about to mark application ${appId} as REJECTED. The citizen will be notified and all linked database records will transition to lock mode. Proceed?`,
        onConfirm: () => updateApplicationStatus(appId, newStage)
      });
    } else {
      updateApplicationStatus(appId, newStage);
    }
  };

  const secureDeleteScheme = (schemeId) => {
    setConfirmModal({
      isOpen: true,
      title: "Confirm Delete Scheme",
      message: `Warning: You are about to permanently delete scheme ID ${schemeId}. This will remove all citizen matching configurations. This action cannot be undone.`,
      onConfirm: () => deleteScheme(schemeId)
    });
  };

  // Switch rendering dispatcher
  const renderTabContent = () => {
    if (isLoading) {
      return <SkeletonLoader type={selectedTab} />;
    }

    switch (selectedTab) {
      case "overview":
        return (
          <DashboardOverview
            applications={applications}
            schemes={schemes}
            grievances={grievances}
            documents={documents}
            navigateToTab={setSelectedTab}
            onSelectApplication={setSelectedAppForReview}
          />
        );
      case "analytics":
        return (
          <AnalyticsDashboard
            applications={applications}
            schemes={schemes}
            documents={documents}
            grievances={grievances}
          />
        );
      case "applications":
        if (selectedAppForReview) {
          return (
            <ApplicationReviewWorkspace
              application={selectedAppForReview}
              onBack={() => setSelectedAppForReview(null)}
              updateApplicationStatus={secureUpdateApplicationStatus}
            />
          );
        }
        return (
          <ApplicationsManagement
            applications={applications}
            updateApplicationStatus={secureUpdateApplicationStatus}
            onSelectApplication={setSelectedAppForReview}
          />
        );
      case "documents":
        return (
          <DocumentVerificationCenter
            documents={documents}
            updateDocumentStatus={updateDocumentStatus}
          />
        );
      case "schemes":
        return (
          <SchemeManagementConsole
            schemes={schemes}
            addScheme={addScheme}
            editScheme={editScheme}
            deleteScheme={secureDeleteScheme}
            applications={applications}
          />
        );
      case "users":
        return <UserManagementConsole />;
      case "grievances":
        return (
          <GrievanceManagementDesk
            grievances={grievances}
            updateGrievanceStatus={updateGrievanceStatus}
          />
        );
      case "audits":
        return <AuditLogsConsole auditLogs={auditLogs} />;
      case "notifications":
        return <AdminNotificationsCenter />;
      case "reports":
        return (
          <GovernmentReportsCenter
            applications={applications}
            schemes={schemes}
            documents={documents}
            grievances={grievances}
          />
        );
      case "settings":
        return <AdminSettingsPanel />;
      default:
        return (
          <div className="bg-white border border-slate-200 p-8 rounded-2xl text-center text-slate-400 font-bold">
            Tab workspace not initialized.
          </div>
        );
    }
  };

  return (
    <div className="space-y-5">
      {/* Nodal Title Header */}
      <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4 select-none">
        <div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-indigo-650 text-indigo-650 text-indigo-650 text-indigo-600" />
            <h1 className="text-base font-black text-slate-900 tracking-tight uppercase">
              {selectedTab === "overview" && "Nodal Overview Operations"}
              {selectedTab === "analytics" && "BI Analytics Dashboard"}
              {selectedTab === "applications" && (selectedAppForReview ? "Evaluation Workspace" : "Citizen Applications Queue")}
              {selectedTab === "documents" && "Document Verification Vault"}
              {selectedTab === "schemes" && "Government Schemes Director"}
              {selectedTab === "users" && "Access & Role Manager"}
              {selectedTab === "grievances" && "Grievance Desk Tickets"}
              {selectedTab === "audits" && "Security Audit Registry"}
              {selectedTab === "notifications" && "Operational Notifications"}
              {selectedTab === "reports" && "Government Reports Center"}
              {selectedTab === "settings" && "Console Settings Panel"}
            </h1>
          </div>
          <p className="text-slate-400 text-[10px] font-bold mt-0.5">
            Logged: <strong className="text-slate-655 text-slate-700">{user?.name || "Administrator"}</strong> • {user?.department || "Govt. Scheme Evaluation Board"}
          </p>
        </div>
        <div className="text-[10px] font-bold text-slate-400">
          Environment: {CONFIG.ENVIRONMENT} • Node: Active
        </div>
      </div>

      {/* Main Workspace Frame */}
      <div className="min-h-[60vh] transition duration-200">
        {renderTabContent()}
      </div>

      {/* ── Security Safeguard Action Confirm Modal ── */}
      {confirmModal.isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm"
            onClick={() => setConfirmModal({ ...confirmModal, isOpen: false })}
          />
          <div className="relative bg-white rounded-2xl border border-slate-200 shadow-2xl max-w-sm w-full overflow-hidden z-10 p-6 space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-2.5 text-rose-655 text-rose-600">
              <AlertTriangle className="h-6 w-6 shrink-0" />
              <h3 className="font-bold text-sm text-slate-800">{confirmModal.title}</h3>
            </div>
            <p className="text-xs text-slate-500 leading-relaxed font-semibold">{confirmModal.message}</p>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setConfirmModal({ ...confirmModal, isOpen: false })}
                className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-655 text-slate-600 rounded-xl text-xs font-bold transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  confirmModal.onConfirm();
                  setConfirmModal({ ...confirmModal, isOpen: false });
                }}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold shadow-sm transition"
              >
                Authorize Action
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
