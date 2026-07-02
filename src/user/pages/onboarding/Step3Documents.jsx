import React, { useState } from "react";
import { useApp } from "@context/AppContext";
import DigiLockerModal from "@components/DigiLockerModal";
import {
  FileText, Upload, Shield, Trash2, CheckCircle2, Plus,
  ArrowLeft, ArrowRight, AlertCircle,
} from "lucide-react";

const DOC_CATEGORIES = [
  "Identity Proof", "Financial Proof", "Category Proof",
  "Domicile Proof", "Property Proof", "Academic Proof", "Other",
];

const STATUS_OPTIONS = [
  { value: "verified",  label: "✓ Verified",  cls: "bg-emerald-100 text-emerald-700 border-emerald-200" },
  { value: "uploaded",  label: "⬆ Uploaded",  cls: "bg-blue-100 text-blue-700 border-blue-200"         },
  { value: "pending",   label: "⏳ Pending",   cls: "bg-amber-100 text-amber-700 border-amber-200"      },
];

const EMPTY_FORM = {
  name: "", issuer: "", category: "", status: "uploaded", expiryDate: "",
};

export default function Step3Documents({ onComplete, onBack }) {
  const { documents, addDocument, t } = useApp();
  const [showForm,    setShowForm]    = useState(false);
  const [showDL,      setShowDL]      = useState(false);
  const [form,        setForm]        = useState(EMPTY_FORM);
  const [formError,   setFormError]   = useState("");

  /* ── Manual upload ── */
  const handleFormChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setFormError("");
  };

  const handleAddDoc = () => {
    if (!form.name.trim()) {
 setFormError("Document name is required.");   return;
}
    if (!form.category)    {
 setFormError("Please select a category.");     return;
}
    setFormError("");
    addDocument(
      form.name.trim(),
      form.category,
      form.issuer.trim() || "Self-Uploaded",
      form.expiryDate   || "No Expiration",
      form.status
    );
    setForm(EMPTY_FORM);
    setShowForm(false);
  };

  /* ── DigiLocker import ── */
  const handleDigiLockerImport = (docs) => {
    docs.forEach((doc) =>
      addDocument(doc.name, doc.type, doc.issuer, doc.expiryDate, doc.status)
    );
    setShowDL(false);
  };

  /* ── Finish ── */
  const handleFinish = () => {
    onComplete({});
  };

  const inputClass =
    "w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition";

  return (
    <>
      {showDL && (
        <DigiLockerModal
          onImport={handleDigiLockerImport}
          onClose={() => setShowDL(false)}
        />
      )}

      <div className="bg-white rounded-3xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="bg-slate-900 px-8 py-6">
          <div className="flex items-center gap-3">
            <div className="bg-indigo-600/20 border border-indigo-500/30 p-2 rounded-xl">
              <FileText className="h-5 w-5 text-indigo-400" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">{t("ob_step3_title") || "Step 3 — Document Setup"}</h2>
              <p className="text-slate-400 text-xs mt-0.5">
                {t("ob_step3_desc") || "Upload your documents or connect DigiLocker"}
              </p>
            </div>
          </div>
        </div>

        <div className="px-8 py-7 space-y-5">
          {/* Two action buttons */}
          <div className="grid grid-cols-2 gap-3">
            {/* DigiLocker */}
            <button
              id="open-digilocker"
              onClick={() => setShowDL(true)}
              className="flex flex-col items-center gap-2 p-4 border-2 border-emerald-200 bg-emerald-50 hover:bg-emerald-100 hover:border-emerald-400 rounded-2xl transition group"
            >
              <div className="bg-emerald-600 p-2.5 rounded-xl group-hover:scale-105 transition">
                <Shield className="h-5 w-5 text-white" />
              </div>
              <span className="text-xs font-bold text-emerald-800">{t("ob_connect_digilocker") || "Connect DigiLocker"}</span>
              <span className="text-[10px] text-emerald-600 text-center leading-tight">
                {t("ob_digilocker_hint") || "Import verified government certificates instantly"}
              </span>
            </button>

            {/* Manual upload */}
            <button
              id="open-manual-upload"
              onClick={() => setShowForm(true)}
              className="flex flex-col items-center gap-2 p-4 border-2 border-indigo-200 bg-indigo-50 hover:bg-indigo-100 hover:border-indigo-400 rounded-2xl transition group"
            >
              <div className="bg-indigo-600 p-2.5 rounded-xl group-hover:scale-105 transition">
                <Upload className="h-5 w-5 text-white" />
              </div>
              <span className="text-xs font-bold text-indigo-800">{t("ob_upload_manually") || "Upload Manually"}</span>
              <span className="text-[10px] text-indigo-600 text-center leading-tight">
                {t("ob_upload_manually_hint") || "Add document details one by one"}
              </span>
            </button>
          </div>

          {/* Manual upload form */}
          {showForm && (
            <div className="bg-slate-50 border border-slate-200 rounded-2xl p-5 space-y-4">
              <div className="flex items-center justify-between">
                <p className="text-sm font-bold text-slate-700 flex items-center gap-2">
                  <Plus className="h-4 w-4 text-indigo-500" /> {t("ob_add_document") || "Add Document"}
                </p>
                <button
                  onClick={() => {
 setShowForm(false); setFormError(""); setForm(EMPTY_FORM);
}}
                  className="text-slate-400 hover:text-slate-600 text-xs font-semibold"
                >
                  {t("gen_cancel") || "Cancel"}
                </button>
              </div>

              {formError && (
                <div className="flex items-center gap-2 bg-rose-50 border border-rose-200 text-rose-700 text-xs p-2.5 rounded-xl">
                  <AlertCircle className="h-3.5 w-3.5 shrink-0" /> {formError}
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <div className="col-span-2">
                  <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wide mb-1">
                    {t("ob_doc_name_label") || "Document Name *"}
                  </label>
                  <input
                    id="doc-name"
                    type="text"
                    placeholder="e.g. Aadhaar Card"
                    value={form.name}
                    onChange={(e) => handleFormChange("name", e.target.value)}
                    className={inputClass}
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wide mb-1">
                    {t("ob_doc_issuer_label") || "Issuer"}
                  </label>
                  <input
                    id="doc-issuer"
                    type="text"
                    placeholder="e.g. UIDAI"
                    value={form.issuer}
                    onChange={(e) => handleFormChange("issuer", e.target.value)}
                    className={inputClass}
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wide mb-1">
                    {t("ob_doc_expiry_label") || "Expiry Date"}
                  </label>
                  <input
                    id="doc-expiry"
                    type="text"
                    placeholder="YYYY-MM-DD or 'None'"
                    value={form.expiryDate}
                    onChange={(e) => handleFormChange("expiryDate", e.target.value)}
                    className={inputClass}
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wide mb-1">
                    {t("ob_doc_category_label") || "Category *"}
                  </label>
                  <select
                    id="doc-category"
                    value={form.category}
                    onChange={(e) => handleFormChange("category", e.target.value)}
                    className={inputClass}
                  >
                    <option value="">— Select —</option>
                    {DOC_CATEGORIES.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wide mb-1">
                    {t("ob_doc_status_label") || "Status"}
                  </label>
                  <select
                    id="doc-status"
                    value={form.status}
                    onChange={(e) => handleFormChange("status", e.target.value)}
                    className={inputClass}
                  >
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s.value} value={s.value}>{s.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              <button
                id="doc-add-submit"
                onClick={handleAddDoc}
                className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-2.5 rounded-xl text-sm font-semibold transition"
              >
                {t("ob_doc_add_btn") || "Add Document"}
              </button>
            </div>
          )}

          {/* Document list */}
          {documents.length > 0 ? (
            <div className="space-y-2">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
                {documents.length} {documents.length !== 1 ? (t("ob_docs_in_vault_plural", { n: documents.length }) || `Documents in Vault`) : (t("ob_docs_in_vault", { n: documents.length }) || `Document in Vault`)}
              </p>
              <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                {documents.map((doc) => (
                  <div
                    key={doc.id}
                    className="flex items-center gap-3 bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5"
                  >
                    <FileText className="h-4 w-4 text-indigo-400 shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-semibold text-slate-800 truncate">{doc.name}</p>
                      <p className="text-[10px] text-slate-400 truncate">{doc.type}</p>
                    </div>
                    <span className={`text-[9px] font-bold px-2 py-0.5 rounded-full border ${
                      STATUS_OPTIONS.find(s => s.value === doc.status)?.cls ||
                      "bg-slate-100 text-slate-500 border-slate-200"
                    }`}>
                      {doc.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="text-center py-4 text-slate-400 text-xs">
              <FileText className="h-8 w-8 mx-auto mb-2 opacity-30" />
              {t("ob_no_docs_yet") || "No documents added yet. Use DigiLocker or upload manually above."}
            </div>
          )}

          {/* Skip note */}
          <p className="text-center text-[11px] text-slate-400">
            {t("ob_skip_hint") || "You can also skip this step and add documents later from the"}{" "}
            <strong>{t("ob_doc_vault_link") || "Document Vault"}</strong> {t("gen_page") || "page"}.
          </p>

          {/* Navigation */}
          <div className="flex gap-3">
            <button
              id="ob-step3-back"
              onClick={onBack}
              className="flex items-center gap-2 px-5 py-2.5 border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-xl text-xs font-bold transition duration-155"
            >
              <ArrowLeft className="h-4 w-4" /> {t("ob_back") || "Back"}
            </button>
            <button
              id="ob-step3-finish"
              onClick={handleFinish}
              className="flex-1 bg-indigo-650 bg-indigo-600 hover:bg-indigo-700 text-white py-2.5 rounded-xl text-xs font-extrabold flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition duration-155"
            >
              <CheckCircle2 className="h-4 w-4" />
              {documents.length > 0
                ? (t("ob_run_ai_btn", { n: documents.length, s: documents.length !== 1 ? "s" : "" }) || `Run AI Analysis (${documents.length} Certificate${documents.length !== 1 ? "s" : ""})`)
                : (t("ob_skip_run_ai_btn") || "Skip & Run AI Analysis")}
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
