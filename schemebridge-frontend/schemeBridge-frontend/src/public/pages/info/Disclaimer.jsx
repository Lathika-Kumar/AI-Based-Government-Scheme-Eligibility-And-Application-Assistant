import React from "react";
import { AlertTriangle } from "lucide-react";

export default function Disclaimer() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-12 space-y-8">
      {/* Header */}
      <div className="border-b border-slate-200 pb-6">
        <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Legal Disclaimer & Notice</h1>
        <p className="text-slate-500 text-xs mt-2 leading-relaxed">
          Important information regarding the simulated nature and data policies of SchemeBridge.
        </p>
      </div>

      {/* Warning Callout */}
      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex gap-3 text-amber-900 text-xs select-none">
        <AlertTriangle className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
        <div className="space-y-1">
          <p className="font-bold">Non-Governmental Educational Portal</p>
          <p className="leading-relaxed">
            SchemeBridge is a prototype demonstration portal and is NOT affiliated with, authorized by, or endorsed by any official central or state government ministry.
          </p>
        </div>
      </div>

      {/* Content */}
      <div className="space-y-6 text-xs text-slate-600 leading-relaxed">
        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900">1. Scheme Information Accuracy</h2>
          <p>
            Scheme details, eligibility criteria, and financial benefit amounts presented on this portal are mock data modeled after public government programs. Citizens must verify all details on official government websites (.gov.in / .nic.in).
          </p>
        </div>

        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900">2. Privacy & Sandbox Data Storage</h2>
          <p>
            All profile information and documents uploaded to SchemeBridge remain strictly within client-side browser storage (localStorage sandbox) and are never transmitted to external servers or third parties.
          </p>
        </div>

        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900">3. Financial & Legal Non-Liability</h2>
          <p>
            SchemeBridge does not guarantee approval or disbursement of government grants, subsidies, or pensions. Users should contact respective district departments or CSC centers for formal scheme enrollment.
          </p>
        </div>
      </div>
    </div>
  );
}
