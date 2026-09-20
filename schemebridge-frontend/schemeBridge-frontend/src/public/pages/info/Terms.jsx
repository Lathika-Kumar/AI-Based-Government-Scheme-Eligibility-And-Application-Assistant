import React from "react";
import { ShieldCheck, FileText, Info } from "lucide-react";

export default function Terms() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-12 space-y-8">
      {/* Header */}
      <div className="border-b border-slate-200 pb-6">
        <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Terms of Service & Privacy Policy</h1>
        <p className="text-slate-500 text-xs mt-2 leading-relaxed">
          Governing terms for utilizing the SchemeBridge e-governance evaluation portal.
        </p>
      </div>

      {/* Info Callout */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 flex gap-3 text-blue-800 text-xs select-none">
        <Info className="h-5 w-5 text-blue-600 shrink-0 mt-0.5" />
        <div className="space-y-1">
          <p className="font-bold">Local Client Storage Commitment</p>
          <p className="leading-relaxed">
            SchemeBridge processes all citizen profile attributes locally on your machine. No identity tokens or sensitive documents leave your device.
          </p>
        </div>
      </div>

      {/* Content sections */}
      <div className="space-y-6 text-xs text-slate-600 leading-relaxed">
        {/* Section 1 */}
        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
            <FileText className="h-4.5 w-4.5 text-slate-500" />
            1. Acceptance of Terms
          </h2>
          <p>
            By accessing or using the SchemeBridge platform, you acknowledge and agree to abide by these Terms of Service. This platform is provided as an e-governance assistant simulation.
          </p>
        </div>

        {/* Section 2 */}
        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
            <ShieldCheck className="h-4.5 w-4.5 text-slate-500" />
            2. Privacy & Data Handling
          </h2>
          <p>
            Your data remains your property. Document files and profile entries are saved in client-side localStorage sandbox containers.
          </p>
          <p>
            We do not sell, track, or harvest citizen demographic records for commercial profiling.
          </p>
        </div>

        {/* Section 3 */}
        <div className="space-y-2">
          <h2 className="text-sm font-bold text-slate-900">3. Platform Modifications</h2>
          <p>
            SchemeBridge reserves the right to update scheme algorithms, eligibility matrices, and portal interfaces as official government guidelines evolve.
          </p>
        </div>
      </div>
    </div>
  );
}
