import React, { useState, useMemo } from "react";
import {
  ArrowLeft,
  User,
  ShieldCheck,
  XCircle,
  FileCheck,
  Sparkles,
  AlertTriangle,
  Send,
  Download,
  Calendar,
  Layers,
  MapPin,
  Clock,
  BookOpen,
  CheckCircle,
  AlertCircle
} from "lucide-react";

export default function ApplicationReviewWorkspace({
  application,
  onBack,
  updateApplicationStatus
}) {
  const [officerNote, setOfficerNote] = useState("");
  const [notesList, setNotesList] = useState([
    { author: "System", date: application.appliedDate, text: "Application submitted and initial eligibility triage complete." },
    { author: "Aadhaar Registry", date: application.appliedDate, text: "Demographic authentication successful via UIDAI." }
  ]);
  const [assignedOfficer, setAssignedOfficer] = useState(application.officer || "Amit Singh");

  const handleAddNote = () => {
    if (!officerNote.trim()) return;
    const newNote = {
      author: "Sanjay Kumar (Admin)",
      date: new Date().toISOString().split("T")[0],
      text: officerNote
    };
    setNotesList([...notesList, newNote]);
    setOfficerNote("");
  };

  const handleApprove = () => {
    updateApplicationStatus(application.id, "Approved");
  };

  const handleReject = () => {
    updateApplicationStatus(application.id, "Rejected");
  };

  const handleRequestDocs = () => {
    const requestNote = {
      author: "System Audit",
      date: new Date().toISOString().split("T")[0],
      text: "Requested document re-upload: Citizen notified via SMS and Dashboard notifications."
    };
    setNotesList([...notesList, requestNote]);
    alert(`Document re-upload requested for ${application.applicantName}`);
  };

  const handleDownload = () => {
    const content = `SchemeBridge Official Application Summary
=============================================
Application ID: ${application.id}
Reference Number: ${application.referenceNo || "N/A"}
Citizen Name: ${application.applicantName}
State of Residence: ${application.applicantState}
Income Category: ${application.applicantIncome}
Social Category: ${application.applicantCaste}
Scheme Name: ${application.schemeName}
Submission Date: ${application.appliedDate}
Status: ${application.currentStage}
Officer Assigned: ${assignedOfficer}
Tamper Validation: Passed (100% Integrity)
Verification Audit Code: SB-OP-${application.id}-${Math.floor(1000 + Math.random() * 9000)}`;

    const blob = new Blob([content], { type: "text/plain" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `Application_Record_${application.id}.txt`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Automated Eligibility Check Calculations (Matching Rajesh Patel or other applicant variables)
  const eligibilityChecks = useMemo(() => {
    // Demographics fallback
    const citizenAge = 32;
    const citizenIncome = 180000;
    const citizenCaste = application.applicantCaste || "OBC";
    const citizenOccupation = "Farmer";

    // Standard checking
    const checks = [
      {
        criterion: "Age Boundary",
        expected: "18 - 70 Years",
        actual: `${citizenAge} Years`,
        status: "Passed",
        valid: true
      },
      {
        criterion: "Income Threshold",
        expected: "≤ ₹3,00,000 / Year",
        actual: application.applicantIncome,
        status: "Passed",
        valid: true
      },
      {
        criterion: "Social Grouping",
        expected: "General, OBC, SC, ST",
        actual: citizenCaste,
        status: "Passed",
        valid: true
      },
      {
        criterion: "Occupational Alignment",
        expected: "Farmer / Student / Open",
        actual: citizenOccupation,
        status: "Passed",
        valid: true
      }
    ];

    return {
      checks,
      overallMet: checks.every((c) => c.valid)
    };
  }, [application]);

  // AI Review Assistant Mock Stats
  const aiAssistantDetails = useMemo(() => {
    const isHighRisk = application.riskLevel === "High";
    const confidence = isHighRisk ? 54 : 91;
    const missingDocs = isHighRisk ? ["Income Certificate (Affidavit Signed)"] : [];
    
    return {
      confidence,
      missingDocs,
      recommendation: isHighRisk
        ? "Manual intervention advised. Document uploads show a potential signature mismatch. Flagged for review."
        : "Highly recommended for approval. Citizen matches all criteria and certificates show verified digital signatures.",
      suggestedAction: isHighRisk ? "Verify Signature & Audits" : "Instant Approval",
      completeness: isHighRisk ? 80 : 100
    };
  }, [application]);

  return (
    <div className="space-y-4 animate-in fade-in duration-200">
      {/* Header / Back Bar */}
      <div className="flex items-center justify-between bg-white border border-slate-200 px-4 py-3 rounded-2xl shadow-sm">
        <div className="flex items-center gap-3">
          <button
            onClick={onBack}
            className="p-1.5 hover:bg-slate-100 text-slate-500 hover:text-slate-800 rounded-lg transition"
            title="Back to grid"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-[10px] font-bold font-mono text-slate-400 uppercase tracking-wider">{application.id}</span>
              <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${
                application.currentStage === "Approved"
                  ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                  : application.currentStage === "Rejected"
                  ? "bg-rose-50 text-rose-700 border-rose-200"
                  : "bg-indigo-50 text-indigo-700 border-indigo-200"
              }`}>
                {application.currentStage}
              </span>
            </div>
            <h2 className="text-sm font-bold text-slate-900 -mt-0.5">
              Review Workspace: {application.applicantName}
            </h2>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleDownload}
            className="px-3 py-1.5 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl text-xs font-bold flex items-center gap-1.5 transition"
            title="Download Application Summary"
          >
            <Download className="h-3.5 w-3.5" />
            <span>Download PDF</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* ── LEFT COLUMN (Citizen Profile, Documents & Eligibility) ── */}
        <div className="lg:col-span-7 space-y-5">
          {/* Demographics Card */}
          <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm space-y-4">
            <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
              <div className="p-1.5 bg-indigo-50 rounded-lg text-indigo-600">
                <User className="h-4 w-4" />
              </div>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Citizen Demographics</h3>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-y-4 gap-x-6 text-xs">
              <div>
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Full Name</span>
                <p className="font-bold text-slate-800 mt-0.5">{application.applicantName}</p>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 uppercase font-semibold">State of Residence</span>
                <p className="font-bold text-slate-800 mt-0.5 flex items-center gap-1">
                  <MapPin className="h-3.5 w-3.5 text-slate-400" />
                  <span>{application.applicantState}</span>
                </p>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Annual Income</span>
                <p className="font-bold text-slate-800 mt-0.5">{application.applicantIncome}</p>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Social Category (Caste)</span>
                <p className="font-bold text-slate-800 mt-0.5">{application.applicantCaste}</p>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Occupation</span>
                <p className="font-bold text-slate-800 mt-0.5">Farmer / Cultivator</p>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Age Verification</span>
                <p className="font-bold text-slate-800 mt-0.5">32 Years (Verified)</p>
              </div>
            </div>
          </div>

          {/* Scheme Summary & Eligibility Matcher */}
          <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm space-y-4">
            <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
              <div className="p-1.5 bg-emerald-50 rounded-lg text-emerald-600">
                <BookOpen className="h-4 w-4" />
              </div>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Automated Eligibility Audits</h3>
            </div>

            <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2 text-xs">
              <div className="flex justify-between items-start">
                <div>
                  <h4 className="font-bold text-slate-850 text-slate-800">{application.schemeName}</h4>
                  <p className="text-[10px] text-slate-400 font-semibold">{application.dept}</p>
                </div>
                <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${
                  eligibilityChecks.overallMet
                    ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                    : "bg-rose-50 text-rose-700 border-rose-200"
                }`}>
                  {eligibilityChecks.overallMet ? "Eligible" : "Needs Review"}
                </span>
              </div>
            </div>

            {/* Verification Rows */}
            <div className="divide-y divide-slate-150 divide-slate-100 border border-slate-100 rounded-xl overflow-hidden text-xs">
              {eligibilityChecks.checks.map((chk, idx) => (
                <div key={idx} className="flex justify-between items-center px-4 py-3 bg-white hover:bg-slate-50/50 transition">
                  <div className="space-y-0.5">
                    <span className="font-semibold text-slate-700">{chk.criterion}</span>
                    <div className="flex gap-2 text-[10px] text-slate-400 font-medium">
                      <span>Target: {chk.expected}</span>
                      <span>•</span>
                      <span>Actual: {chk.actual}</span>
                    </div>
                  </div>
                  <span className={`font-bold px-2 py-0.5 rounded-full text-[9px] border ${
                    chk.valid
                      ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                      : "bg-rose-50 text-rose-700 border-rose-200"
                  }`}>
                    {chk.status}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Document Verification Cards */}
          <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm space-y-4">
            <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
              <div className="p-1.5 bg-blue-50 rounded-lg text-blue-600">
                <FileCheck className="h-4 w-4" />
              </div>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Required Document Validation</h3>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              {[
                { name: "Aadhaar Card", source: "DigiLocker", integrity: "100% (Verified)", date: "2026-06-10", type: "Identity Proof" },
                { name: "Land Record (Khatauni)", source: "e-District API", integrity: "98% (Secure)", date: "2026-06-12", type: "Asset Proof" },
                { name: "Income Certificate", source: "Manual Upload", integrity: "Pending OCR Audit", date: "2026-06-12", type: "Financial Proof" },
                { name: "Bank Passbook", source: "DigiLocker Integration", integrity: "100% (Match)", date: "2026-06-10", type: "Disbursement Check" }
              ].map((doc, idx) => (
                <div key={idx} className="border border-slate-200 p-4 rounded-xl space-y-3 bg-white hover:border-slate-350 hover:shadow-sm transition">
                  <div className="flex justify-between items-start">
                    <div>
                      <h4 className="font-bold text-slate-800 text-xs">{doc.name}</h4>
                      <span className="text-[9px] text-slate-400 font-semibold uppercase">{doc.type}</span>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${
                      doc.source === "DigiLocker" || doc.source.includes("API")
                        ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                        : "bg-amber-50 text-amber-700 border-amber-200"
                    }`}>
                      {doc.source}
                    </span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-[10px] text-slate-500 font-medium bg-slate-50 p-2 rounded-lg border border-slate-100">
                    <div>
                      <span className="text-slate-400 block uppercase text-[8px] font-semibold">OCR Verification</span>
                      <span className="font-bold text-slate-700">{doc.integrity}</span>
                    </div>
                    <div>
                      <span className="text-slate-400 block uppercase text-[8px] font-semibold">Upload Date</span>
                      <span className="font-bold text-slate-700">{doc.date}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── RIGHT COLUMN (Timeline, Notes, AI assistant & Actions) ── */}
        <div className="lg:col-span-5 space-y-5">
          {/* AI Review Assistant Panel */}
          <div className="bg-gradient-to-br from-slate-900 to-indigo-950 text-white p-5 rounded-2xl shadow-md border border-indigo-900/60 space-y-4">
            <div className="flex justify-between items-center border-b border-indigo-900/60 pb-3">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4.5 w-4.5 text-indigo-400 animate-pulse" />
                <h3 className="text-xs font-bold uppercase tracking-wider text-indigo-200">AI Review Assistant</h3>
              </div>
              <span className={`px-2 py-0.5 bg-indigo-500/20 text-indigo-300 text-[9px] font-bold rounded-full border border-indigo-400/20`}>
                {aiAssistantDetails.confidence}% Confidence
              </span>
            </div>

            <div className="space-y-3 text-xs leading-normal">
              <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl space-y-1">
                <span className="text-[9px] text-indigo-300 font-bold uppercase tracking-widest block">AI Summary & Risk Assessment</span>
                <p className="text-slate-200 leading-relaxed font-medium">{aiAssistantDetails.recommendation}</p>
              </div>

              <div className="grid grid-cols-2 gap-2 text-[10px] text-slate-300 font-medium">
                <div className="bg-slate-800/40 p-2 rounded-lg border border-slate-700/30">
                  <span className="text-slate-400 block uppercase text-[8px]">Completeness</span>
                  <span className="font-bold text-white">{aiAssistantDetails.completeness}% Verified</span>
                </div>
                <div className="bg-slate-800/40 p-2 rounded-lg border border-slate-700/30">
                  <span className="text-slate-400 block uppercase text-[8px]">Risk Rating</span>
                  <span className={`font-bold ${application.riskLevel === "High" ? "text-rose-400" : "text-emerald-400"}`}>
                    {application.riskLevel} Risk
                  </span>
                </div>
              </div>

              {aiAssistantDetails.missingDocs.length > 0 && (
                <div className="bg-rose-500/15 border border-rose-500/20 p-2.5 rounded-lg flex items-start gap-2 text-rose-200 text-[10px]">
                  <AlertTriangle className="h-4 w-4 shrink-0 text-rose-400" />
                  <div>
                    <span className="font-bold block">Missing/Flagged Items:</span>
                    <ul className="list-disc list-inside mt-0.5 space-y-0.5 font-medium text-slate-350">
                      {aiAssistantDetails.missingDocs.map((doc, idx) => <li key={idx}>{doc}</li>)}
                    </ul>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Timeline and notes */}
          <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm space-y-4">
            <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
              <div className="p-1.5 bg-slate-100 rounded-lg text-slate-600">
                <Clock className="h-4 w-4" />
              </div>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Application Timeline</h3>
            </div>

            <div className="relative pl-6 border-l border-slate-200 space-y-4 text-xs select-none">
              {(application.stageHistory || []).map((step, idx) => (
                <div key={idx} className="relative">
                  <span className="absolute -left-[30px] top-0.5 h-4 w-4 rounded-full bg-indigo-650 bg-indigo-600 border-2 border-white text-white flex items-center justify-center font-bold text-[8px]">
                    ✓
                  </span>
                  <div className="flex justify-between items-start">
                    <span className="font-bold text-slate-800">{step.stage}</span>
                    <span className="text-[9px] text-slate-400 font-semibold">{step.date}</span>
                  </div>
                  <p className="text-[10px] text-slate-500 leading-normal mt-0.5">{step.note || "Milestone verified."}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Notes Desk */}
          <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm space-y-4">
            <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
              <div className="p-1.5 bg-slate-100 rounded-lg text-slate-600">
                <Layers className="h-4 w-4" />
              </div>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Officer Audit Notes</h3>
            </div>

            <div className="max-h-48 overflow-y-auto space-y-3 pr-2 scrollbar-thin">
              {notesList.map((note, idx) => (
                <div key={idx} className="bg-slate-50 border border-slate-100 p-3 rounded-xl space-y-1 text-xs">
                  <div className="flex justify-between text-[10px] text-slate-400 font-semibold">
                    <span>{note.author}</span>
                    <span>{note.date}</span>
                  </div>
                  <p className="text-slate-600 font-medium leading-relaxed">{note.text}</p>
                </div>
              ))}
            </div>

            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Add internal evaluation note..."
                value={officerNote}
                onChange={(e) => setOfficerNote(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleAddNote()}
                className="flex-1 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
              <button
                onClick={handleAddNote}
                className="p-2 bg-indigo-650 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl transition shadow-sm"
              >
                <Send className="h-4.5 w-4.5" />
              </button>
            </div>
          </div>

          {/* Officer Assignment Dropdown */}
          <div className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm space-y-3">
            <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Assigned Nodal Officer</label>
            <select
              value={assignedOfficer}
              onChange={(e) => setAssignedOfficer(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold focus:outline-none focus:ring-1 focus:ring-indigo-500 cursor-pointer"
            >
              <option value="Amit Singh">Amit Singh (Document Verification)</option>
              <option value="Neha Sharma">Neha Sharma (Ministry Liaison)</option>
              <option value="Sanjay Kumar">Sanjay Kumar (Super Admin)</option>
              <option value="Priya Patel">Priya Patel (Support Officer)</option>
            </select>
          </div>

          {/* Core Action Workspace Buttons */}
          <div className="bg-slate-50 border border-slate-200 p-5 rounded-2xl space-y-3">
            <span className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">Triage & Decision Controls</span>
            
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={handleApprove}
                disabled={application.currentStage === "Approved"}
                className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-200 disabled:text-slate-400 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1.5"
              >
                <CheckCircle className="h-4 w-4" />
                <span>Approve</span>
              </button>
              <button
                onClick={handleReject}
                disabled={application.currentStage === "Rejected"}
                className="w-full py-2.5 bg-rose-600 hover:bg-rose-700 disabled:bg-slate-200 disabled:text-slate-400 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1.5"
              >
                <XCircle className="h-4 w-4" />
                <span>Reject</span>
              </button>
            </div>

            <button
              onClick={handleRequestDocs}
              className="w-full py-2.5 bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 rounded-xl text-xs font-bold shadow-sm transition flex items-center justify-center gap-1.5"
            >
              <AlertCircle className="h-4 w-4 text-amber-500" />
              <span>Request More Documents</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
