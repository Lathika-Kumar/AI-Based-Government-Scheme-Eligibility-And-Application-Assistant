import React, { useState } from "react";
import { Briefcase, IndianRupee, Users, MapPin, ArrowRight, ArrowLeft, AlertCircle, Sparkles } from "lucide-react";
import { useApp } from "@context/AppContext";

const OCCUPATIONS = [
  "Farmer / Agricultural Worker",
  "Student",
  "Salaried Employee",
  "Self-Employed / Business",
  "Daily Wage Labour",
  "Unemployed",
  "Homemaker",
  "Retired",
  "Other",
];

const CASTES = [
  { value: "General",  label: "General"                    },
  { value: "OBC",      label: "OBC — Other Backward Class" },
  { value: "SC",       label: "SC — Scheduled Caste"       },
  { value: "ST",       label: "ST — Scheduled Tribe"       },
];

const STATES = [
  "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
  "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand",
  "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
  "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
  "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
  "Uttar Pradesh", "Uttarakhand", "West Bengal",
  // Union Territories
  "Andaman and Nicobar Islands", "Chandigarh",
  "Dadra and Nagar Haveli and Daman and Diu", "Delhi (NCT)",
  "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry",
];

export default function Step2Eligibility({ initialData, onNext, onBack }) {
  const { t } = useApp();
  const [occupation, setOccupation] = useState(initialData?.occupation || "");

  // Prefill logic
  const [annualIncome, setAnnualIncome] = useState(() => {
    if (initialData?.annualIncome) {
return initialData.annualIncome;
}
    const prefill = localStorage.getItem("schemebridge_prefill_profile");
    if (prefill) {
      try {
        const parsed = JSON.parse(prefill);
        if (parsed.annualIncome) {
return parsed.annualIncome;
}
      } catch (e) {}
    }
    return "";
  });

  const [caste, setCaste] = useState(() => {
    if (initialData?.caste) {
return initialData.caste;
}
    const prefill = localStorage.getItem("schemebridge_prefill_profile");
    if (prefill) {
      try {
        const parsed = JSON.parse(prefill);
        if (parsed.caste) {
return parsed.caste;
}
      } catch (e) {}
    }
    return "";
  });

  const [state, setState] = useState(() => {
    if (initialData?.state) {
return initialData.state;
}
    const prefill = localStorage.getItem("schemebridge_prefill_profile");
    if (prefill) {
      try {
        const parsed = JSON.parse(prefill);
        if (parsed.state) {
return parsed.state;
}
      } catch (e) {}
    }
    return "";
  });

  const [error, setError] = useState("");

  const handleContinue = () => {
    if (!occupation)             {
 setError("Please select your occupation.");             return;
}
    if (!annualIncome || Number(annualIncome) < 0) {
 setError("Please enter a valid annual income.");        return;
}
    if (!caste)                  {
 setError("Please select your social category.");        return;
}
    if (!state)                  {
 setError("Please select your state of residence.");     return;
}
    setError("");
    onNext({
      occupation,
      annualIncome: Number(annualIncome),
      caste,
      state,
    });
  };

  const inputClass = "w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition";

  return (
    <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200">
      {/* Header */}
      <div className="bg-slate-900 px-8 py-6">
        <div className="flex items-center gap-3">
          <div className="bg-indigo-600/20 border border-indigo-500/30 p-2 rounded-xl text-indigo-400">
            <Briefcase className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-extrabold text-white">{t("ob_eligibility_attributes_title") || "Step 2 — Eligibility Attributes"}</h2>
            <p className="text-slate-400 text-xs mt-0.5 font-medium">{t("ob_eligibility_attributes_desc") || "Income thresholds and geographical parameters"}</p>
          </div>
        </div>
      </div>

      <div className="px-8 py-7 space-y-5">
        {error && (
          <div className="flex items-center gap-2.5 bg-rose-50 border border-rose-200 text-rose-700 text-xs p-3.5 rounded-xl">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />
            <span className="font-semibold">{error}</span>
          </div>
        )}

        {/* Prefill helper */}
        {localStorage.getItem("schemebridge_prefill_profile") && (
          <div className="flex items-center gap-2 bg-indigo-50 border border-indigo-200 text-indigo-700 p-3 rounded-xl text-xs font-semibold">
            <Sparkles className="h-3.5 w-3.5 text-indigo-600 shrink-0" />
            <span>{t("ob_prefill_loaded") || "Socio-demographics pre-loaded from calculator preview."}</span>
          </div>
        )}

        {/* Occupation */}
        <div>
          <label htmlFor="ob-occupation" className="block text-[10px] font-extrabold text-slate-600 mb-1.5 uppercase tracking-wide">
            <Briefcase className="inline h-3.5 w-3.5 mr-1.5 text-slate-400 -mt-0.5" />
            Occupation <span className="text-rose-500">*</span>
          </label>
          <select
            id="ob-occupation"
            value={occupation}
            onChange={(e) => {
 setOccupation(e.target.value); setError("");
}}
            className={inputClass}
          >
            <option value="">— Select your occupation —</option>
            {OCCUPATIONS.map((o) => (
              <option key={o} value={o}>{o}</option>
            ))}
          </select>
        </div>

        {/* Annual Income */}
        <div>
          <label htmlFor="ob-income" className="block text-[10px] font-extrabold text-slate-600 mb-1.5 uppercase tracking-wide">
            <IndianRupee className="inline h-3.5 w-3.5 mr-1 text-slate-400 -mt-0.5" />
            Annual Household Income (₹) <span className="text-rose-500">*</span>
          </label>
          <div className="relative">
            <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500 font-bold text-sm">₹</span>
            <input
              id="ob-income"
              type="number"
              placeholder="e.g. 180000"
              value={annualIncome}
              min={0}
              onChange={(e) => {
 setAnnualIncome(e.target.value); setError("");
}}
              className="w-full pl-8 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition"
            />
          </div>
          {annualIncome && (
            <p className="text-[10px] text-indigo-600 font-semibold mt-1">
              {t("ob_format_annual_income", { amount: Number(annualIncome).toLocaleString("en-IN") }) || `Formatted: ₹${Number(annualIncome).toLocaleString("en-IN")} / year`}
            </p>
          )}
          <p className="text-[10px] text-slate-400 mt-1">
            {t("ob_income_combined_hint") || "Combined pre-tax income from all household members across all sources."}
          </p>
        </div>

        {/* Social Category */}
        <div>
          <label className="block text-[10px] font-extrabold text-slate-600 mb-2 uppercase tracking-wide">
            <Users className="inline h-3.5 w-3.5 mr-1 text-slate-400 -mt-0.5" />
            Social Category <span className="text-rose-500">*</span>
          </label>
          <div className="grid grid-cols-2 gap-2">
            {CASTES.map((c) => (
              <button
                key={c.value}
                type="button"
                id={`ob-caste-${c.value.toLowerCase()}`}
                onClick={() => {
 setCaste(c.value); setError("");
}}
                className={`py-2.5 px-3 rounded-xl border-2 text-xs font-bold text-left transition duration-150
                  ${caste === c.value
                    ? "bg-indigo-600 border-indigo-600 text-white shadow-md shadow-indigo-600/10"
                    : "bg-white border-slate-200 text-slate-700 hover:border-indigo-300"
                  }`}
              >
                {c.label}
              </button>
            ))}
          </div>
        </div>

        {/* State */}
        <div>
          <label htmlFor="ob-state" className="block text-[10px] font-extrabold text-slate-600 mb-1.5 uppercase tracking-wide">
            <MapPin className="inline h-3.5 w-3.5 mr-1 text-slate-400 -mt-0.5" />
            State of Residence / UT <span className="text-rose-500">*</span>
          </label>
          <select
            id="ob-state"
            value={state}
            onChange={(e) => {
 setState(e.target.value); setError("");
}}
            className={inputClass}
          >
            <option value="">{t("ob_select_state_ut") || "— Select your state / UT —"}</option>
            <optgroup label={t("ob_states_group") || "States"}>
              {STATES.slice(0, 28).map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </optgroup>
            <optgroup label={t("ob_uts_group") || "Union Territories"}>
              {STATES.slice(28).map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </optgroup>
          </select>
        </div>

        {/* Note */}
        <div className="bg-indigo-50/50 border border-indigo-100 rounded-xl px-4 py-3 text-[10px] leading-relaxed text-indigo-800">
          {t("ob_eligibility_realtime_notice") || "ℹ️ These criteria are evaluated in real-time by the matching engine. You can update these attributes later from the Profile configurations."}
        </div>

        {/* Navigation */}
        <div className="flex gap-3 pt-2">
          <button
            id="ob-step2-back"
            onClick={onBack}
            className="flex items-center gap-2 px-5 py-2.5 border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-xl text-xs font-bold transition duration-150"
          >
            <ArrowLeft className="h-4 w-4" /> {t("ob_back") || "Back"}
          </button>
          <button
            id="ob-step2-next"
            onClick={handleContinue}
            className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-2.5 rounded-xl text-sm font-extrabold flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition duration-150"
          >
            <span>{t("ob_continue_docs") || "Continue to Documents Setup"}</span>
            <ArrowRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
