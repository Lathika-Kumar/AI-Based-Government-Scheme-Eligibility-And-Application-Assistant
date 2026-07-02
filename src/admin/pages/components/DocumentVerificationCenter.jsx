import React, { useState } from "react";
import {
  FileText,
  CheckCircle,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Search,
  Sparkles,
  ShieldCheck,
  FileDigit,
  Lock,
  Cpu
} from "lucide-react";

export default function DocumentVerificationCenter({ documents, updateDocumentStatus }) {
  const [search, setSearch] = useState("");
  const [ocrFilter, setOcrFilter] = useState("all");
  const [riskFilter, setRiskFilter] = useState("all");

  // Enriching document mock objects with high-fidelity visual variables
  const enrichedDocs = React.useMemo(() => {
    return (documents || []).map((doc, idx) => {
      const citizenNames = [
        "Rajesh Patel",
        "Aravind Swamy",
        "Sunita Sharma",
        "Vikram Singh",
        "Ananya Rao"
      ];
      const citizen = citizenNames[doc.id % citizenNames.length];
      
      let ocrStatus = "Success";
      let aiConfidence = 98;
      let riskLevel = "Low";
      let ocrData = {};

      if (doc.name.includes("Income")) {
        ocrStatus = "Warning";
        aiConfidence = 82;
        riskLevel = "Medium";
        ocrData = {
          "Tax assessment year": "2025-2026",
          "Declared Income": "₹1,80,000",
          "Tax Registry Match": "Mismatch (ITR shows ₹1,95,000)"
        };
      } else if (doc.name.includes("Land") || doc.status === "uploaded") {
        ocrStatus = "Success";
        aiConfidence = 94;
        riskLevel = "Low";
        ocrData = {
          "Khasra Number": "104/A",
          "Hectares Owned": "1.82",
          "Registry Signature": "Valid Digital Certificate"
        };
      } else if (doc.name.includes("Domicile")) {
        ocrStatus = "Failed";
        aiConfidence = 45;
        riskLevel = "High";
        ocrData = {
          "Certificate Number": "DOM/GJ/88210",
          "Issuer Authority": "Tehsildar Office",
          "Expiry Validation": "Expired on 2024-05-10"
        };
      } else {
        ocrData = {
          "Aadhaar Number": "xxxx-xxxx-4012",
          "Name Authenticated": "Match (Rajesh Patel)",
          "DOB Registry Match": "Match (15-08-1994)"
        };
      }

      return {
        ...doc,
        citizen,
        ocrStatus,
        aiConfidence,
        riskLevel,
        ocrData
      };
    });
  }, [documents]);

  const filteredDocs = React.useMemo(() => {
    return enrichedDocs.filter((doc) => {
      const matchesSearch =
        doc.name.toLowerCase().includes(search.toLowerCase()) ||
        doc.citizen.toLowerCase().includes(search.toLowerCase());
      const matchesOcr = ocrFilter === "all" || doc.ocrStatus === ocrFilter;
      const matchesRisk = riskFilter === "all" || doc.riskLevel === riskFilter;

      return matchesSearch && matchesOcr && matchesRisk;
    });
  }, [enrichedDocs, search, ocrFilter, riskFilter]);

  const ocrStatusStyles = {
    Success: "bg-emerald-50 text-emerald-700 border-emerald-200",
    Warning: "bg-amber-50 text-amber-700 border-amber-200",
    Failed: "bg-rose-50 text-rose-700 border-rose-200"
  };

  const riskStyles = {
    Low: "bg-emerald-50 text-emerald-700 border-emerald-200",
    Medium: "bg-amber-50 text-amber-750 text-amber-800 border-amber-200",
    High: "bg-rose-50 text-rose-700 border-rose-200"
  };

  return (
    <div className="space-y-4">
      {/* ── Search & Filter Controls ── */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm flex flex-col md:flex-row gap-3">
        <div className="flex-1 relative">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search documents by name or citizen..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-50 hover:bg-slate-100/50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 transition"
          />
        </div>
        <div className="flex gap-2">
          <select
            value={ocrFilter}
            onChange={(e) => setOcrFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer"
          >
            <option value="all">All OCR Statuses</option>
            <option value="Success">OCR Success</option>
            <option value="Warning">OCR Warning</option>
            <option value="Failed">OCR Failed</option>
          </select>
          <select
            value={riskFilter}
            onChange={(e) => setRiskFilter(e.target.value)}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer"
          >
            <option value="all">All Forgery Risks</option>
            <option value="Low">Low Risk</option>
            <option value="Medium">Medium Risk</option>
            <option value="High">High Risk</option>
          </select>
        </div>
      </div>

      {/* ── Document Cards Grid ── */}
      {filteredDocs.length === 0 ? (
        <div className="bg-white border border-slate-200 p-12 text-center rounded-2xl flex flex-col items-center justify-center space-y-3">
          <FileText className="h-10 w-10 text-slate-350 text-slate-400" />
          <div>
            <p className="text-sm font-bold text-slate-800">No Documents Found</p>
            <p className="text-xs text-slate-400 leading-normal max-w-sm mt-0.5">
              No files are pending or uploaded matching your current filter criteria.
            </p>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-5">
          {filteredDocs.map((doc) => (
            <div
              key={doc.id}
              className={`bg-white border border-slate-200 rounded-2xl shadow-sm hover:shadow-md transition duration-200 flex flex-col overflow-hidden ${
                doc.riskLevel === "High" ? "border-rose-300 ring-1 ring-rose-200/55" : ""
              }`}
            >
              {/* Card Header */}
              <div className="p-4 border-b border-slate-100 space-y-1">
                <div className="flex justify-between items-start">
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">
                      Citizen: {doc.citizen}
                    </span>
                    <h3 className="text-sm font-black text-slate-900 tracking-tight">{doc.name}</h3>
                  </div>
                  <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${
                    doc.status === "verified"
                      ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                      : doc.status === "rejected"
                      ? "bg-rose-50 text-rose-700 border-rose-200"
                      : "bg-amber-50 text-amber-700 border-amber-200"
                  }`}>
                    {doc.status}
                  </span>
                </div>
                <div className="flex flex-wrap gap-2 text-[9px] text-slate-400 font-semibold pt-1">
                  <span>Source: {doc.source}</span>
                  <span>•</span>
                  <span>Uploaded: {doc.date}</span>
                </div>
              </div>

              {/* Document Mock Preview */}
              <div className="bg-slate-900/90 text-slate-300 p-4 h-32 flex flex-col justify-between font-mono relative overflow-hidden select-none">
                <div className="absolute top-0 right-0 w-24 h-24 bg-indigo-500/10 rounded-full blur-2xl pointer-events-none"></div>
                <div className="flex justify-between items-start">
                  <div className="flex items-center gap-1.5">
                    <FileDigit className="h-4 w-4 text-indigo-400" />
                    <span className="text-[9px] text-slate-400 uppercase tracking-widest font-bold">Secure PDF OCR</span>
                  </div>
                  <span className="text-[8px] px-1.5 py-0.5 bg-slate-800 rounded font-semibold text-indigo-300">
                    Confidence: {doc.aiConfidence}%
                  </span>
                </div>

                <div className="text-[10px] text-slate-400 truncate max-w-full">
                  &gt; Decrypting document keys...
                  <br />
                  &gt; Parsing certificates and bounds...
                </div>

                <div className="flex justify-between items-end border-t border-slate-800 pt-2 text-[9px]">
                  <span className="text-[8px] text-slate-500">TAMPER SEAL #38210A</span>
                  <span className="text-emerald-400 font-bold flex items-center gap-0.5">
                    <ShieldCheck className="h-3 w-3" />
                    AUTHENTIC
                  </span>
                </div>
              </div>

              {/* OCR Scanned Outputs */}
              <div className="p-4 space-y-3 bg-slate-50 flex-1 text-xs">
                <div className="flex justify-between items-center">
                  <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider">OCR Verification Metadata</span>
                  <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${ocrStatusStyles[doc.ocrStatus] || ""}`}>
                    OCR {doc.ocrStatus}
                  </span>
                </div>

                <div className="space-y-1.5 bg-white border border-slate-150 p-2.5 rounded-xl text-[11px] font-medium leading-relaxed shadow-sm">
                  {Object.entries(doc.ocrData).map(([key, val]) => (
                    <div key={key} className="flex justify-between">
                      <span className="text-slate-450 text-slate-500">{key}:</span>
                      <span className="font-bold text-slate-800 text-right">{val}</span>
                    </div>
                  ))}
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div className="bg-white border border-slate-150 p-2 rounded-xl text-[10px] space-y-0.5 flex flex-col justify-between">
                    <span className="text-slate-400 block uppercase font-bold text-[8px]">Forgery Check</span>
                    <span className={`font-bold inline-flex items-center gap-1 mt-0.5 ${riskStyles[doc.riskLevel]}`}>
                      {doc.riskLevel} Risk
                    </span>
                  </div>
                  <div className="bg-white border border-slate-150 p-2 rounded-xl text-[10px] space-y-0.5 flex flex-col justify-between">
                    <span className="text-slate-400 block uppercase font-bold text-[8px]">Digital Signature</span>
                    <span className="text-emerald-700 font-bold inline-flex items-center gap-0.5 mt-0.5">
                      <Lock className="h-3 w-3 shrink-0" />
                      Certified
                    </span>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="p-4 border-t border-slate-100 flex gap-2 bg-white">
                <button
                  onClick={() => updateDocumentStatus(doc.id, "verified")}
                  disabled={doc.status === "verified"}
                  className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-100 disabled:text-slate-400 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1"
                >
                  <CheckCircle className="h-3.5 w-3.5" />
                  <span>Verify</span>
                </button>
                <button
                  onClick={() => updateDocumentStatus(doc.id, "rejected")}
                  disabled={doc.status === "rejected"}
                  className="flex-1 py-2 bg-rose-600 hover:bg-rose-700 disabled:bg-slate-100 disabled:text-slate-400 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1"
                >
                  <XCircle className="h-3.5 w-3.5" />
                  <span>Reject</span>
                </button>
                <button
                  onClick={() => alert(`Request re-upload triggered for ${doc.name} - citizen notified.`)}
                  className="px-2 py-2 border border-slate-200 hover:bg-slate-50 text-slate-655 text-slate-600 rounded-xl text-xs font-bold transition flex items-center justify-center"
                  title="Request Reupload"
                >
                  <RefreshCw className="h-3.5 w-3.5" />
                </button>
              </div>

            </div>
          ))}
        </div>
      )}
    </div>
  );
}
