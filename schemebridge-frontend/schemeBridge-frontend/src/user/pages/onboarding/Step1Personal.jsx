import React, { useState } from "react";
import { User, Calendar, ArrowRight, AlertCircle, Sparkles } from "lucide-react";
import { IS_FLOW_TEST_MODE } from "@utils/flowTestMode";
import { safeGetItem } from "@utils/storage";

const GENDERS = ["Male", "Female", "Other", "Prefer not to say"];

export default function Step1Personal({ initialData, onNext }) {
  const [name, setName] = useState(() => {
    return initialData?.name || "";
  });

  const [age, setAge] = useState(() => {
    if (initialData?.age) {
      return initialData.age;
    }
    // Attempt prefill from landing calculator
    const prefill = safeGetItem("schemebridge_prefill_profile", null);
    if (prefill && prefill.age) {
      return prefill.age;
    }
    return "";
  });

  const [gender, setGender] = useState(() => {
    return initialData?.gender || "";
  });

  const [error, setError] = useState("");

  const handleContinue = () => {
    // TEMPORARY TEST MODE: Guard client validation for manual testing
    if (!IS_FLOW_TEST_MODE) {
      if (!name.trim()) {
        setError("Full name is required."); return;
      }
      if (!age || Number(age) < 1 || Number(age) > 110) {
        setError("Please enter a valid age between 1 and 110.");
        return;
      }
      if (!gender) {
        setError("Please select a gender option."); return;
      }
    }
    setError("");
    onNext({ name: (name || "").trim(), age: Number(age) || 25, gender: gender || "Male" });
  };


  const getGenderLabel = (g) => {
    return g;
  };

  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl overflow-hidden border border-slate-200 dark:border-slate-800">
      {/* Header */}
      <div className="bg-slate-900 px-8 py-6">
        <div className="flex items-center gap-3">
          <div className="bg-indigo-600/20 border border-indigo-500/30 p-2 rounded-xl text-indigo-400">
            <User className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-extrabold text-white">Step 1 — Personal Information</h2>
            <p className="text-slate-400 text-xs mt-0.5">Basic demographics to authenticate your profile</p>
          </div>
        </div>
      </div>

      <div className="px-8 py-7 space-y-5">
        {error && (
          <div className="flex items-center gap-2.5 bg-rose-50 dark:bg-rose-950/50 border border-rose-200 dark:border-rose-900 text-rose-700 dark:text-rose-300 text-xs p-3.5 rounded-xl">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 dark:text-rose-400" />
            <span className="font-semibold">{error}</span>
          </div>
        )}

        {/* Prefill helper notification */}
        {safeGetItem("schemebridge_prefill_profile", null) && (
          <div className="flex items-center gap-2 bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 p-3 rounded-xl text-xs font-semibold">
            <Sparkles className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400 shrink-0" />
            <span>Age pre-loaded from eligibility calculator search.</span>
          </div>
        )}

        {/* Full Name */}
        <div>
          <label htmlFor="ob-name" className="block text-[10px] font-extrabold text-slate-600 dark:text-slate-300 mb-1.5 uppercase tracking-wide">
            Full Name <span className="text-rose-500">*</span>
          </label>
          <div className="relative">
            <User className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              id="ob-name"
              type="text"
              placeholder="e.g. Rajesh Patel"
              value={name}
              onChange={(e) => {
                setName(e.target.value); setError("");
              }}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white dark:focus:bg-slate-800 transition"
            />
          </div>
        </div>

        {/* Age */}
        <div>
          <label htmlFor="ob-age" className="block text-[10px] font-extrabold text-slate-600 dark:text-slate-300 mb-1.5 uppercase tracking-wide">
            Age of Applicant <span className="text-rose-500">*</span>
          </label>
          <div className="relative">
            <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              id="ob-age"
              type="number"
              placeholder="e.g. 32"
              value={age}
              min={1}
              max={110}
              onChange={(e) => {
                setAge(e.target.value); setError("");
              }}
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white dark:focus:bg-slate-800 transition"
            />
          </div>
        </div>

        {/* Gender */}
        <div>
          <label className="block text-[10px] font-extrabold text-slate-600 dark:text-slate-300 mb-2 uppercase tracking-wide">
            Gender <span className="text-rose-500">*</span>
          </label>
          <div className="grid grid-cols-2 gap-2">
            {GENDERS.map((g) => (
              <button
                key={g}
                type="button"
                id={`ob-gender-${g.toLowerCase().replace(/ /g, "-")}`}
                onClick={() => {
                  setGender(g); setError("");
                }}
                className={`py-2.5 px-4 rounded-xl border-2 text-xs font-bold transition duration-150 text-center
                  ${gender === g
                    ? "border-indigo-600 bg-indigo-600 text-white shadow-md shadow-indigo-600/10"
                    : "bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 hover:border-indigo-300 dark:hover:border-indigo-500"
                  }`}
              >
                {getGenderLabel(g)}
              </button>
            ))}
          </div>
        </div>

        {/* Info note */}
        <div className="bg-indigo-50/50 dark:bg-indigo-950/40 border border-indigo-100 dark:border-indigo-900/50 rounded-xl px-4 py-3 text-[10px] leading-relaxed text-indigo-800 dark:text-indigo-300">
          💡 Demographic coordinates are crucial for filtering schemes with mandatory age gates. Please check against government ID records.
        </div>

        <button
          id="ob-step1-next"
          onClick={handleContinue}
          className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-3.5 rounded-xl text-sm font-extrabold flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition"
        >
          <span>Continue to Eligibility Details</span>
          <ArrowRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
