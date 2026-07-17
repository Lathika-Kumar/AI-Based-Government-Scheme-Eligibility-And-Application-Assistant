import React, { useState, useMemo } from "react";
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
  Cpu,
  Eye,
  Download,
  CheckSquare,
  Square,
  Clock,
  Filter,
  Trash2
} from "lucide-react";

export default function DocumentVerificationCenter({ documents, updateDocumentStatus }) {
  const [search, setSearch] = useState("");
  const [ocrFilter, setOcrFilter] = useState("all");
  const [riskFilter, setRiskFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [selectedDoc, setSelectedDoc] = useState(null);
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
  const [selectedDocIds, setSelectedDocIds] = useState(new Set());
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [selectedRejectReason, setSelectedRejectReason] = useState("");

  // Reject reason templates
  const rejectReasons = [
    "Document is blurry or illegible",
    "Document is expired",
    "Name mismatch with citizen profile",
    "Document is tampered or forged",
    "Required fields missing",
    "Signature mismatch",
    "Document not issued by authorized authority"
  ];

  // Enriching document mock objects with high-fidelity visual variables
  const enrichedDocs = useMemo(() => {
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
      let verificationHistory = [
        { date: doc.date, action: "Uploaded", by: "System" }
      ];

      if (doc.name.includes("Income")) {
        ocrStatus = "Warning";
        aiConfidence = 82;
        riskLevel = "Medium";
        ocrData = {
          "Tax assessment year": "2025-2026",
          "Declared Income": "₹1,80,000",
          "Tax Registry Match": "Mismatch (ITR shows ₹1,95,000)"
        };
        verificationHistory.push({ date: "2026-07-02", action: "OCR Completed", by: "AI System" });
      } else if (doc.name.includes("Land") || doc.status === "uploaded") {
        ocrStatus = "Success";
        aiConfidence = 94;
        riskLevel = "Low";
        ocrData = {
          "Khasra Number": "104/A",
          "Hectares Owned": "1.82",
          "Registry Signature": "Valid Digital Certificate"
        };
        verificationHistory.push({ date: "2026-07-02", action: "OCR Completed", by: "AI System" });
      } else if (doc.name.includes("Domicile")) {
        ocrStatus = "Failed";
        aiConfidence = 45;
        riskLevel = "High";
        ocrData = {
          "Certificate Number": "DOM/GJ/88210",
          "Issuer Authority": "Tehsildar Office",
          "Expiry Validation": "Expired on 2024-05-10"
        };
        verificationHistory.push({ date: "2026-07-01", action: "OCR Completed", by: "AI System" });
      } else {
        ocrData = {
          "Aadhaar Number": "xxxx-xxxx-4012",
          "Name Authenticated": "Match (Rajesh Patel)",
          "DOB Registry Match": "Match (15-08-1994)"
        };
        verificationHistory.push({ date: doc.date, action: "OCR Completed", by: "AI System" });
      }

      if (doc.status === "verified") {
        verificationHistory.push({ date: "2026-07-02", action: "Verified", by: "Admin" });
      } else if (doc.status === "rejected") {
        verificationHistory.push({ date: "2026-07-01", action: "Rejected", by: "Officer" });
      }

      return {
        ...doc,
        citizen,
        ocrStatus,
        aiConfidence,
        riskLevel,
        ocrData,
        verificationHistory
      };
    });
  }, [documents]);

  const filteredDocs = useMemo(() => {
    return enrichedDocs.filter((doc) => {
      const matchesSearch =
        doc.name.toLowerCase().includes(search.toLowerCase()) ||
        doc.citizen.toLowerCase().includes(search.toLowerCase());
      const matchesOcr = ocrFilter === "all" || doc.ocrStatus === ocrFilter;
      const matchesRisk = riskFilter === "all" || doc.riskLevel === riskFilter;
      const matchesStatus = statusFilter === "all" || doc.status === statusFilter;
      return matchesSearch && matchesOcr && matchesRisk && matchesStatus;
    });
  }, [enrichedDocs, search, ocrFilter, riskFilter, statusFilter]);

  const ocrStatusStyles = {
    Success: "bg-emerald-50 text-emerald-700 border-emerald-200",
    Warning: "bg-amber-50 text-amber-700 border-amber-200",
    Failed: "bg-red-50 text-red-700 border-red-200"
  };

  const riskStyles = {
    Low: "bg-emerald-50 text-emerald-700 border-emerald-200",
    Medium: "bg-amber-50 text-amber-700 border-amber-200",
    High: "bg-red-50 text-red-700 border-red-200"
  };

  // Toggle select/deselect document
  const toggleSelectDoc = (docId) => {
    setSelectedDocIds((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(docId)) newSet.delete(docId);
      else newSet.add(docId);
      return newSet;
    });
  };

  // Bulk verify selected docs
  const bulkVerify = () => {
    selectedDocIds.forEach(id => {
      updateDocumentStatus(id, "verified");
    });
    setSelectedDocIds(new Set());
  };

  // Bulk reject with reason
  const bulkReject = () => {
    if (selectedRejectReason) {
      selectedDocIds.forEach(id => {
        updateDocumentStatus(id, "rejected");
      });
      setSelectedDocIds(new Set());
      setIsRejectModalOpen(false);
      setSelectedRejectReason("");
    }
  };

  // Verify single doc
  const verifyDoc = (doc) => {
    updateDocumentStatus(doc.id, "verified");
    if (isPreviewOpen && selectedDoc?.id === doc.id) {
      setSelectedDoc({ ...doc, status: "verified" });
    }
  };

  // Reject single doc with reason
  const rejectDoc = (doc) => {
    setSelectedDoc(doc);
    setIsRejectModalOpen(true);
  };

  // Confirm reject single
  const confirmRejectSingle = () => {
    if (selectedDoc && selectedRejectReason) {
      updateDocumentStatus(selectedDoc.id, "rejected");
      setIsRejectModalOpen(false);
      setSelectedDoc({ ...selectedDoc, status: "rejected" });
      setSelectedRejectReason("");
    }
  };

  return (
    <div className="space-y-4">
      {/* ── Search & Filter Controls + Bulk Actions ── */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm space-y-4">
        {/* Top row: search & filters */}
        <div className="flex flex-col lg:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search documents by name or citizen..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 hover:bg-slate-100/50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-600 text-slate-700 transition"
            />
          </div>
          <div className="flex flex-wrap gap-2">
            <div className="relative">
              <Filter className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="pl-10 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-600 cursor-pointer"
              >
                <option value="all">All Statuses</option>
                <option value="uploaded">Pending</option>
                <option value="verified">Verified</option>
                <option value="rejected">Rejected</option>
              </select>
            </div>
            <select
              value={ocrFilter}
              onChange={(e) => setOcrFilter(e.target.value)}
              className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-600 cursor-pointer"
            >
              <option value="all">All OCR</option>
              <option value="Success">Success</option>
              <option value="Warning">Warning</option>
              <option value="Failed">Failed</option>
            </select>
            <select
              value={riskFilter}
              onChange={(e) => setRiskFilter(e.target.value)}
              className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-2 focus:ring-indigo-600 cursor-pointer"
            >
              <option value="all">All Risks</option>
              <option value="Low">Low Risk</option>
              <option value="Medium">Medium Risk</option>
              <option value="High">High Risk</option>
            </select>
          </div>
        </div>

        {/* Bulk actions row */}
        {selectedDocIds.size > 0 && (
          <div className="flex flex-wrap items-center gap-3 pt-3 border-t border-slate-100">
            <span className="text-xs font-bold text-slate-600">
              {selectedDocIds.size} document{selectedDocIds.size > 1 ? "s" : ""} selected
            </span>
            <button
              onClick={bulkVerify}
              className="flex items-center gap-2 px-3 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-sm transition"
            >
              <CheckCircle className="h-3.5 w-3.5" />
              Bulk Verify
            </button>
            <button
              onClick={() => setIsRejectModalOpen(true)}
              className="flex items-center gap-2 px-3 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold shadow-sm transition"
            >
              <XCircle className="h-3.5 w-3.5" />
              Bulk Reject
            </button>
            <button
              onClick={() => setSelectedDocIds(new Set())}
              className="flex items-center gap-2 px-3 py-2 border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-xl text-xs font-bold transition"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Clear Selection
            </button>
          </div>
        )}
      </div>

      {/* ── Document Cards Grid ── */}
      {filteredDocs.length === 0 ? (
        <div className="bg-white border border-slate-200 p-12 text-center rounded-2xl flex flex-col items-center justify-center space-y-3">
          <FileText className="h-10 w-10 text-slate-400" />
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
              className={`bg-white border border-slate-200 rounded-2xl shadow-sm hover:shadow-md transition duration-200 flex flex-col overflow-hidden relative ${
                doc.riskLevel === "High" ? "border-red-300 ring-1 ring-red-200/55" : ""
              }`}
            >
              {/* Selection checkbox */}
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  toggleSelectDoc(doc.id);
                }}
                className="absolute top-4 left-4 p-1.5 bg-white rounded-full border border-slate-200 hover:bg-slate-50 transition z-10"
              >
                {selectedDocIds.has(doc.id) ? (
                  <CheckSquare className="h-4 w-4 text-indigo-600" />
                ) : (
                  <Square className="h-4 w-4 text-slate-400" />
                )}
              </button>

              {/* Card Header */}
              <div className="p-4 border-b border-slate-100 space-y-1">
                <div className="flex justify-between items-start">
                  <div className="ml-8">
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">
                      Citizen: {doc.citizen}
                    </span>
                    <h3 className="text-sm font-black text-slate-900 tracking-tight">{doc.name}</h3>
                  </div>
                  <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${
                    doc.status === "verified"
                      ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                      : doc.status === "rejected"
                      ? "bg-red-50 text-red-700 border-red-200"
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
              <div 
                className="bg-slate-900/90 text-slate-300 p-4 h-32 flex flex-col justify-between font-mono relative overflow-hidden select-none cursor-pointer"
                onClick={() => {
                  setSelectedDoc(doc);
                  setIsPreviewOpen(true);
                }}
              >
                <div className="absolute top-0 right-0 w-24 h-24 bg-indigo-600/10 rounded-full blur-2xl pointer-events-none"></div>
                <div className="flex justify-between items-start">
                  <div className="flex items-center gap-1.5">
                    <FileDigit className="h-4 w-4 text-indigo-400" />
                    <span className="text-[9px] text-slate-400 uppercase tracking-widest font-bold">Secure PDF OCR</span>
                  </div>
                  <span className="text-[8px] px-1.5 py-0.5 bg-slate-800 rounded font-semibold text-indigo-400">
                    Confidence: {doc.aiConfidence}%
                  </span>
                </div>
                <div className="text-[10px] text-slate-400 truncate max-w-full">
                  &gt; Decrypting document keys...<br />&gt; Parsing certificates and bounds...
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
                <div className="space-y-1.5 bg-white border border-slate-200 p-2.5 rounded-xl text-[11px] font-medium leading-relaxed shadow-sm">
                  {Object.entries(doc.ocrData).map(([key, val]) => (
                    <div key={key} className="flex justify-between">
                      <span className="text-slate-500">{key}:</span>
                      <span className="font-bold text-slate-800 text-right">{val}</span>
                    </div>
                  ))}
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div className="bg-white border border-slate-200 p-2 rounded-xl text-[10px] space-y-0.5 flex flex-col justify-between">
                    <span className="text-slate-400 block uppercase font-bold text-[8px]">Risk Level</span>
                    <span className={`font-bold inline-flex items-center gap-1 mt-0.5 ${riskStyles[doc.riskLevel]}`}>
                      {doc.riskLevel} Risk
                    </span>
                  </div>
                  <div className="bg-white border border-slate-200 p-2 rounded-xl text-[10px] space-y-0.5 flex flex-col justify-between">
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
                  onClick={() => verifyDoc(doc)}
                  disabled={doc.status === "verified"}
                  className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-100 disabled:text-slate-400 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1"
                >
                  <CheckCircle className="h-3.5 w-3.5" />
                  <span>Verify</span>
                </button>
                <button
                  onClick={() => rejectDoc(doc)}
                  disabled={doc.status === "rejected"}
                  className="flex-1 py-2 bg-red-600 hover:bg-red-700 disabled:bg-slate-100 disabled:text-slate-400 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1"
                >
                  <XCircle className="h-3.5 w-3.5" />
                  <span>Reject</span>
                </button>
                <button
                  onClick={() => alert(`Request re-upload triggered for ${doc.name} - citizen notified.`)}
                  className="px-2 py-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl text-xs font-bold transition flex items-center justify-center"
                  title="Request Reupload"
                >
                  <RefreshCw className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── Document Preview Modal ── */}
      {isPreviewOpen && selectedDoc && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl max-w-3xl w-[95%] max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h3 className="text-lg font-bold text-slate-900">{selectedDoc.name}</h3>
                <p className="text-xs text-slate-500">{selectedDoc.citizen}</p>
              </div>
              <button
                onClick={() => setIsPreviewOpen(false)}
                className="p-2 hover:bg-slate-100 rounded-lg transition"
              >
                <XCircle className="h-5 w-5 text-slate-500" />
              </button>
            </div>

            <div className="p-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Left: Preview & AI Validation */}
              <div className="space-y-5">
                {/* Preview */}
                <div className="border border-slate-200 rounded-2xl overflow-hidden">
                  <div className="bg-slate-900/90 text-slate-300 p-6 h-64 flex flex-col justify-between font-mono relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-32 h-32 bg-indigo-600/10 rounded-full blur-2xl pointer-events-none"></div>
                    <div className="flex justify-between items-start">
                      <div className="flex items-center gap-1.5">
                        <FileDigit className="h-5 w-5 text-indigo-400" />
                        <span className="text-[10px] text-slate-400 uppercase tracking-widest font-bold">Secure Preview</span>
                      </div>
                      <span className="text-[9px] px-2 py-1 bg-slate-800 rounded font-semibold text-indigo-400">PDF</span>
                    </div>
                    <div className="text-[11px] text-slate-400">
                      &gt; Document Content Preview<br />
                      &gt; Showing page 1 of 1<br />
                      &gt; Document type: {selectedDoc.type}
                    </div>
                    <div className="flex justify-between items-end border-t border-slate-800 pt-3 text-[10px]">
                      <span className="text-[8px] text-slate-500">DOCUMENT ID: DOC-{selectedDoc.id}</span>
                      <Download className="h-4 w-4 text-indigo-400 cursor-pointer" />
                    </div>
                  </div>
                </div>

                {/* AI Validation Panel */}
                <div className="bg-slate-50 border border-slate-200 p-4 rounded-2xl">
                  <h4 className="text-xs font-bold text-slate-800 mb-3 flex items-center gap-2">
                    <Cpu className="h-4 w-4 text-indigo-600" />
                    AI Validation Panel
                  </h4>
                  <div className="space-y-3">
                    <div className="bg-white border border-slate-200 p-3 rounded-xl">
                      <div className="flex justify-between items-center mb-1">
                        <span className="text-[10px] text-slate-500 uppercase font-bold tracking-wider">Confidence</span>
                        <span className="text-xs font-bold text-slate-800">{selectedDoc.aiConfidence}%</span>
                      </div>
                      <div className="w-full bg-slate-100 rounded-full h-2">
                        <div
                          className="bg-indigo-600 h-2 rounded-full"
                          style={{ width: `${selectedDoc.aiConfidence}%` }}
                        ></div>
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="bg-white border border-slate-200 p-3 rounded-xl text-center">
                        <div className={`inline-flex items-center gap-1 text-xs font-bold mb-1 ${
                          selectedDoc.ocrStatus === "Success" ? "text-emerald-700" :
                          selectedDoc.ocrStatus === "Warning" ? "text-amber-700" : "text-red-700"
                        }`}>
                          {selectedDoc.ocrStatus === "Success" ? <CheckCircle className="h-3.5 w-3.5" /> :
                            selectedDoc.ocrStatus === "Warning" ? <AlertTriangle className="h-3.5 w-3.5" /> :
                            <XCircle className="h-3.5 w-3.5" />}
                          OCR {selectedDoc.ocrStatus}
                        </div>
                      </div>
                      <div className="bg-white border border-slate-200 p-3 rounded-xl text-center">
                        <div className={`inline-flex items-center gap-1 text-xs font-bold mb-1 ${
                          selectedDoc.riskLevel === "Low" ? "text-emerald-700" :
                          selectedDoc.riskLevel === "Medium" ? "text-amber-700" : "text-red-700"
                        }`}>
                          {selectedDoc.riskLevel === "Low" ? <ShieldCheck className="h-3.5 w-3.5" /> :
                            selectedDoc.riskLevel === "Medium" ? <AlertTriangle className="h-3.5 w-3.5" /> :
                            <XCircle className="h-3.5 w-3.5" />}
                          {selectedDoc.riskLevel} Risk
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Right: OCR Data & Verification Timeline & History */}
              <div className="space-y-5">
                {/* OCR Data */}
                <div className="border border-slate-200 p-4 rounded-2xl">
                  <h4 className="text-xs font-bold text-slate-800 mb-3">OCR Extracted Data</h4>
                  <div className="space-y-2">
                    {Object.entries(selectedDoc.ocrData).map(([key, val]) => (
                      <div key={key} className="flex justify-between text-xs bg-slate-50 p-2 rounded-lg border border-slate-100">
                        <span className="text-slate-500">{key}</span>
                        <span className="font-bold text-slate-800">{val}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Verification Timeline */}
                <div className="border border-slate-200 p-4 rounded-2xl">
                  <h4 className="text-xs font-bold text-slate-800 mb-3 flex items-center gap-2">
                    <Clock className="h-4 w-4 text-indigo-600" />
                    Verification Timeline
                  </h4>
                  <div className="space-y-3">
                    {selectedDoc.verificationHistory.map((item, idx) => (
                      <div key={idx} className="flex gap-3">
                        <div className="flex flex-col items-center">
                          <div className="w-2 h-2 rounded-full bg-indigo-600 mt-1" />
                          {idx < selectedDoc.verificationHistory.length - 1 && <div className="w-0.5 h-full bg-slate-200" />}
                        </div>
                        <div className="pb-3">
                          <div className="flex justify-between items-start">
                            <p className="text-xs font-bold text-slate-800">{item.action}</p>
                            <span className="text-[9px] text-slate-400">{item.date}</span>
                          </div>
                          <p className="text-[10px] text-slate-500 mt-0.5">By: {item.by}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex gap-3 pt-2">
                  <button
                    onClick={() => verifyDoc(selectedDoc)}
                    disabled={selectedDoc.status === "verified"}
                    className="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-100 disabled:text-slate-400 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1"
                  >
                    <CheckCircle className="h-4 w-4" />
                    Verify Document
                  </button>
                  <button
                    onClick={() => rejectDoc(selectedDoc)}
                    disabled={selectedDoc.status === "rejected"}
                    className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 disabled:bg-slate-100 disabled:text-slate-400 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1"
                  >
                    <XCircle className="h-4 w-4" />
                    Reject Document
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Reject Reason Modal */}
      {isRejectModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4">
            <div className="p-6 border-b border-slate-100 flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-900">Reject Document</h3>
              <button
                onClick={() => {
                  setIsRejectModalOpen(false);
                  setSelectedRejectReason("");
                }}
                className="p-2 hover:bg-slate-100 rounded-lg transition"
              >
                <XCircle className="h-5 w-5 text-slate-500" />
              </button>
            </div>
            <div className="p-6">
              <p className="text-xs text-slate-600 mb-4">Select a reason for rejection:</p>
              <div className="space-y-2 mb-6">
                {rejectReasons.map((reason, idx) => (
                  <button
                    key={idx}
                    onClick={() => setSelectedRejectReason(reason)}
                    className={
                      selectedRejectReason === reason
                        ? "w-full text-left px-3 py-2.5 rounded-xl border border-indigo-600 bg-indigo-50 text-indigo-700 text-xs font-semibold transition"
                        : "w-full text-left px-3 py-2.5 rounded-xl border border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 text-xs font-semibold transition"
                    }
                  >
                    {reason}
                  </button>
                ))}
              </div>
              <div className="flex gap-3">
                <button
                  onClick={() => {
                    setIsRejectModalOpen(false);
                    setSelectedRejectReason("");
                  }}
                  className="flex-1 py-2 border border-slate-200 text-slate-700 hover:bg-slate-50 rounded-xl text-xs font-bold shadow-sm transition"
                >
                  Cancel
                </button>
                <button
                  onClick={selectedDocIds.size > 0 ? bulkReject : confirmRejectSingle}
                  disabled={!selectedRejectReason}
                  className="flex-1 py-2 bg-red-600 hover:bg-red-700 disabled:bg-slate-100 disabled:text-slate-400 text-white rounded-xl text-xs font-bold shadow-sm transition"
                >
                  Confirm Reject
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
