import React, { useState } from "react";
import { X, ShieldCheck, FileText, CheckCircle2, Download, ArrowRight, Wifi } from "lucide-react";

/** Mock certificates available in the simulated DigiLocker */
const DIGILOCKER_CERTS = [
  {
    id: "dl-aadhaar",
    name: "Aadhaar Card",
    type: "Identity Proof",
    issuer: "UIDAI — Unique Identification Authority of India",
    expiry: "No Expiration",
    status: "verified",
    icon: "🪪",
  },
  {
    id: "dl-pan",
    name: "PAN Card",
    type: "Identity Proof",
    issuer: "Income Tax Department, Govt. of India",
    expiry: "No Expiration",
    status: "verified",
    icon: "💳",
  },
  {
    id: "dl-income",
    name: "Income Certificate",
    type: "Financial Proof",
    issuer: "Revenue Department (State Government)",
    expiry: "2028-03-31",
    status: "verified",
    icon: "📄",
  },
  {
    id: "dl-caste",
    name: "Caste Certificate (SC/ST/OBC)",
    type: "Category Proof",
    issuer: "Social Welfare Board & Revenue Dept.",
    expiry: "No Expiration",
    status: "verified",
    icon: "📋",
  },
  {
    id: "dl-domicile",
    name: "Domicile / Residence Certificate",
    type: "Domicile Proof",
    issuer: "Local Revenue Authority",
    expiry: "2029-06-30",
    status: "verified",
    icon: "🏠",
  },
  {
    id: "dl-land",
    name: "Land Records / Khatauni",
    type: "Property Proof",
    issuer: "Land Revenue Department",
    expiry: "No Expiration",
    status: "uploaded",
    icon: "🗺️",
  },
];

export default function DigiLockerModal({ onImport, onClose }) {
  // screen: "consent" | "picker" | "success"
  const [screen, setScreen] = useState("consent");
  const [selected, setSelected] = useState(
    // Default: Aadhaar, Income, Caste pre-checked
    new Set(["dl-aadhaar", "dl-income", "dl-caste"])
  );

  const toggleCert = (id) => {
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const handleImport = () => {
    const docsToImport = DIGILOCKER_CERTS.filter((c) => selected.has(c.id)).map((c) => ({
      name: c.name,
      type: c.type,
      issuer: c.issuer,
      expiryDate: c.expiry,
      status: c.status,
    }));
    onImport(docsToImport);
    setScreen("success");
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-slate-900/70 backdrop-blur-sm"
        onClick={screen !== "success" ? onClose : undefined}
      />

      {/* Modal */}
      <div className="relative bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden z-10">

        {/* ── CONSENT SCREEN ─────────────────────────────────── */}
        {screen === "consent" && (
          <>
            {/* Header — DigiLocker branding */}
            <div className="bg-gradient-to-r from-emerald-700 to-teal-600 px-6 py-5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="bg-white/20 p-2 rounded-xl">
                    <ShieldCheck className="h-6 w-6 text-white" />
                  </div>
                  <div>
                    <p className="text-white font-bold text-sm tracking-wide">DigiLocker</p>
                    <p className="text-emerald-200 text-[10px]">Ministry of Electronics & IT, Govt. of India</p>
                  </div>
                </div>
                <button onClick={onClose} className="text-emerald-200 hover:text-white p-1 rounded-lg transition">
                  <X className="h-5 w-5" />
                </button>
              </div>

              {/* Simulated secure connection bar */}
              <div className="mt-4 flex items-center gap-2 bg-emerald-800/40 rounded-xl px-3 py-2">
                <Wifi className="h-3.5 w-3.5 text-emerald-300 animate-pulse" />
                <span className="text-emerald-200 text-[10px] font-medium">
                  Secure connection · digilocker.gov.in · TLS 1.3
                </span>
                <span className="ml-auto bg-emerald-500 text-white text-[9px] font-bold px-1.5 py-0.5 rounded">
                  SIMULATION
                </span>
              </div>
            </div>

            {/* Consent Body */}
            <div className="px-6 py-6 space-y-5">
              <div>
                <h3 className="text-base font-bold text-slate-900">SchemeBridge is requesting access</h3>
                <p className="text-slate-500 text-xs mt-1 leading-relaxed">
                  SchemeBridge wants to access your DigiLocker documents to automatically verify
                  your eligibility for government schemes. No data will be shared with third parties.
                </p>
              </div>

              {/* What will be accessed */}
              <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-2.5">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  Permissions Requested
                </p>
                {[
                  "Read your issued government certificates",
                  "Import selected documents to your SchemeBridge vault",
                  "Use document metadata for eligibility matching",
                ].map((item, i) => (
                  <div key={i} className="flex items-start gap-2.5 text-xs text-slate-700">
                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500 shrink-0 mt-0.5" />
                    {item}
                  </div>
                ))}
              </div>

              <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 text-[11px] text-amber-800">
                ⚠️ This is a <strong>simulated DigiLocker</strong> integration for demonstration purposes.
                No real government data is accessed.
              </div>

              <div className="flex gap-3">
                <button
                  onClick={onClose}
                  className="flex-1 py-2.5 border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-xl text-sm font-semibold transition"
                >
                  Deny
                </button>
                <button
                  id="digilocker-allow"
                  onClick={() => setScreen("picker")}
                  className="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-semibold flex items-center justify-center gap-2 shadow-sm transition"
                >
                  Allow Access
                  <ArrowRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          </>
        )}

        {/* ── CERTIFICATE PICKER ─────────────────────────────── */}
        {screen === "picker" && (
          <>
            <div className="bg-gradient-to-r from-emerald-700 to-teal-600 px-6 py-4 flex items-center justify-between">
              <div>
                <p className="text-white font-bold text-sm">Select Certificates to Import</p>
                <p className="text-emerald-200 text-[10px] mt-0.5">{selected.size} selected</p>
              </div>
              <button onClick={onClose} className="text-emerald-200 hover:text-white p-1 rounded-lg transition">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="px-6 py-5 space-y-3 max-h-[60vh] overflow-y-auto">
              {DIGILOCKER_CERTS.map((cert) => {
                const isSelected = selected.has(cert.id);
                return (
                  <label
                    key={cert.id}
                    className={`flex items-center gap-4 p-3.5 rounded-2xl border-2 cursor-pointer transition
                      ${isSelected
                        ? "border-emerald-500 bg-emerald-50"
                        : "border-slate-200 bg-white hover:border-slate-300"
                      }`}
                  >
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => toggleCert(cert.id)}
                      className="sr-only"
                    />
                    <span className="text-2xl shrink-0">{cert.icon}</span>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-slate-800">{cert.name}</p>
                      <p className="text-[10px] text-slate-400 truncate mt-0.5">{cert.issuer}</p>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-[9px] bg-slate-100 text-slate-500 border border-slate-200 px-1.5 py-0.5 rounded font-medium">
                          {cert.type}
                        </span>
                        <span className="text-[9px] bg-emerald-100 text-emerald-700 border border-emerald-200 px-1.5 py-0.5 rounded font-bold">
                          ✓ {cert.status === "verified" ? "Registry Verified" : "Uploaded"}
                        </span>
                      </div>
                    </div>
                    <div className={`h-5 w-5 rounded-full border-2 flex items-center justify-center shrink-0
                      ${isSelected ? "bg-emerald-600 border-emerald-600" : "border-slate-300"}`}>
                      {isSelected && <CheckCircle2 className="h-3.5 w-3.5 text-white" />}
                    </div>
                  </label>
                );
              })}
            </div>

            <div className="px-6 py-4 border-t border-slate-100 flex gap-3">
              <button
                onClick={onClose}
                className="px-4 py-2.5 border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-xl text-sm font-semibold transition"
              >
                Skip
              </button>
              <button
                id="digilocker-import"
                onClick={handleImport}
                disabled={selected.size === 0}
                className="flex-1 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white py-2.5 rounded-xl text-sm font-semibold flex items-center justify-center gap-2 shadow-sm transition"
              >
                <Download className="h-4 w-4" />
                Import {selected.size} Certificate{selected.size !== 1 ? "s" : ""}
              </button>
            </div>
          </>
        )}

        {/* ── SUCCESS SCREEN ─────────────────────────────────── */}
        {screen === "success" && (
          <div className="px-8 py-10 text-center space-y-4">
            <div className="inline-flex items-center justify-center bg-emerald-100 h-16 w-16 rounded-full mb-2">
              <CheckCircle2 className="h-8 w-8 text-emerald-600" />
            </div>
            <h3 className="text-xl font-bold text-slate-900">Import Successful!</h3>
            <p className="text-slate-500 text-sm leading-relaxed">
              <strong>{selected.size} certificate{selected.size !== 1 ? "s" : ""}</strong> have been
              imported from DigiLocker and added to your document vault as verified documents.
            </p>
            <div className="flex flex-wrap justify-center gap-2 pt-2">
              {DIGILOCKER_CERTS.filter((c) => selected.has(c.id)).map((c) => (
                <span key={c.id} className="inline-flex items-center gap-1.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium px-3 py-1.5 rounded-full">
                  <FileText className="h-3 w-3" />
                  {c.name}
                </span>
              ))}
            </div>
            <button
              onClick={onClose}
              className="mt-4 w-full bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-xl text-sm font-semibold transition"
            >
              Done — Continue to Dashboard
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
