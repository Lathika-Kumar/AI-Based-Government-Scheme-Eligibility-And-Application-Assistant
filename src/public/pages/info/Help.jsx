import React, { useState } from "react";
import { HelpCircle, Phone, Mail, MapPin, ChevronDown, ChevronUp } from "lucide-react";
import { useApp } from "@context/AppContext";

export default function Help() {
  const [openFaq, setOpenFaq] = useState(null);
  const { t } = useApp();

  const faqs = [
    {
      q: t("help_faq_q1"),
      a: t("help_faq_a1")
    },
    {
      q: t("help_faq_q2"),
      a: t("help_faq_a2")
    },
    {
      q: t("help_faq_q3"),
      a: t("help_faq_a3")
    },
    {
      q: t("help_faq_q4"),
      a: t("help_faq_a4")
    }
  ];

  return (
    <div className="max-w-4xl mx-auto px-4 py-12 space-y-10">
      {/* Header */}
      <div className="border-b border-slate-200 pb-6 text-center sm:text-left">
        <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">{t("help_public_title")}</h1>
        <p className="text-slate-500 text-sm mt-2 max-w-2xl leading-relaxed">
          {t("help_public_desc")}
        </p>
      </div>

      {/* Grid: Contacts & FAQ */}
      <div className="grid md:grid-cols-3 gap-8">
        {/* Support channels */}
        <div className="space-y-5 md:col-span-1">
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-widest">{t("help_channels_title")}</h2>

          <div className="space-y-4">
            {/* Phone */}
            <div className="flex gap-3.5 p-4 border border-slate-200 rounded-xl bg-white shadow-xs">
              <div className="bg-slate-100 p-2 rounded-lg text-slate-700 shrink-0">
                <Phone className="h-4.5 w-4.5" />
              </div>
              <div className="space-y-0.5">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">{t("help_toll_free")}</span>
                <span className="text-xs font-bold text-slate-800">1800-180-1947</span>
              </div>
            </div>

            {/* Email */}
            <div className="flex gap-3.5 p-4 border border-slate-200 rounded-xl bg-white shadow-xs">
              <div className="bg-slate-100 p-2 rounded-lg text-slate-700 shrink-0">
                <Mail className="h-4.5 w-4.5" />
              </div>
              <div className="space-y-0.5">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">{t("help_email_desk")}</span>
                <span className="text-xs font-bold text-slate-850 text-indigo-700 underline truncate block">support@schemebridge.gov.in</span>
              </div>
            </div>

            {/* Location */}
            <div className="flex gap-3.5 p-4 border border-slate-200 rounded-xl bg-white shadow-xs">
              <div className="bg-slate-100 p-2 rounded-lg text-slate-700 shrink-0">
                <MapPin className="h-4.5 w-4.5" />
              </div>
              <div className="space-y-0.5">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">{t("help_hq_title")}</span>
                <span className="text-xs text-slate-600 leading-normal block">
                  {t("help_hq_desc")}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* FAQs */}
        <div className="md:col-span-2 space-y-4">
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-widest flex items-center gap-1.5">
            <HelpCircle className="h-5 w-5 text-indigo-650" />
            {t("help_faqs_title")}
          </h2>

          <div className="border border-slate-200 rounded-2xl bg-white divide-y divide-slate-200 overflow-hidden shadow-xs">
            {faqs.map((faq, index) => {
              const isOpen = openFaq === index;
              return (
                <div key={index} className="transition duration-150">
                  <button
                    onClick={() => setOpenFaq(isOpen ? null : index)}
                    className="w-full px-5 py-4 flex items-center justify-between text-left hover:bg-slate-50 transition"
                  >
                    <span className="text-xs font-bold text-slate-900 pr-4">{faq.q}</span>
                    {isOpen ? <ChevronUp className="h-4 w-4 text-slate-500 shrink-0" /> : <ChevronDown className="h-4 w-4 text-slate-500 shrink-0" />}
                  </button>
                  {isOpen && (
                    <div className="px-5 pb-4.5 pt-0 text-xs text-slate-600 leading-relaxed bg-slate-50 border-t border-slate-100 py-3">
                      {faq.a}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
