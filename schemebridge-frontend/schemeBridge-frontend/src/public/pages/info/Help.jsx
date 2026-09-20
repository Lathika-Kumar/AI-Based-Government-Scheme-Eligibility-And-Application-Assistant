import React, { useState } from "react";
import { HelpCircle, Phone, Mail, MapPin, ChevronDown, ChevronUp } from "lucide-react";

export default function Help() {
  const [openFaq, setOpenFaq] = useState(null);

  const faqs = [
    {
      q: "How does SchemeBridge match me with schemes?",
      a: "SchemeBridge evaluates your socio-economic details (age, income, state, caste) locally in your browser against central and state rule engines."
    },
    {
      q: "Is my uploaded document data secure?",
      a: "Yes, all document metadata and personal details are stored exclusively inside your browser's localStorage sandbox model."
    },
    {
      q: "What should I do if an application stage is delayed?",
      a: "You can file a formal grievance via the Help & Grievances desk in your citizen dashboard or contact the department helpline."
    },
    {
      q: "Are the scheme details official?",
      a: "Scheme parameters are synchronized with official public guidelines from Government of India portals."
    }
  ];

  return (
    <div className="max-w-4xl mx-auto px-4 py-12 space-y-10">
      {/* Header */}
      <div className="border-b border-slate-200 pb-6 text-center sm:text-left">
        <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Help & Support Center</h1>
        <p className="text-slate-500 text-sm mt-2 max-w-2xl leading-relaxed">
          Find answers to frequently asked questions, contact the support desk, or locate government helpline information.
        </p>
      </div>

      {/* Grid: Contacts & FAQ */}
      <div className="grid md:grid-cols-3 gap-8">
        {/* Support channels */}
        <div className="space-y-5 md:col-span-1">
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-widest">Support Channels</h2>

          <div className="space-y-4">
            {/* Phone */}
            <div className="flex gap-3.5 p-4 border border-slate-200 rounded-xl bg-white shadow-xs">
              <div className="bg-slate-100 p-2 rounded-lg text-slate-700 shrink-0">
                <Phone className="h-4.5 w-4.5" />
              </div>
              <div className="space-y-0.5">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Toll Free Helpline</span>
                <span className="text-xs font-bold text-slate-800">1800-11-1555</span>
              </div>
            </div>

            {/* Email */}
            <div className="flex gap-3.5 p-4 border border-slate-200 rounded-xl bg-white shadow-xs">
              <div className="bg-slate-100 p-2 rounded-lg text-slate-700 shrink-0">
                <Mail className="h-4.5 w-4.5" />
              </div>
              <div className="space-y-0.5">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Public Support Email</span>
                <span className="text-xs font-bold text-slate-850 text-indigo-700 underline truncate block">support@schemebridge.gov.in</span>
              </div>
            </div>

            {/* Location */}
            <div className="flex gap-3.5 p-4 border border-slate-200 rounded-xl bg-white shadow-xs">
              <div className="bg-slate-100 p-2 rounded-lg text-slate-700 shrink-0">
                <MapPin className="h-4.5 w-4.5" />
              </div>
              <div className="space-y-0.5">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">National Portal HQ</span>
                <span className="text-xs text-slate-600 leading-normal block">
                  e-Governance Directorate, CGO Complex, Lodhi Road, New Delhi - 110003
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* FAQs */}
        <div className="md:col-span-2 space-y-4">
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-widest flex items-center gap-1.5">
            <HelpCircle className="h-5 w-5 text-indigo-650" />
            Frequently Asked Questions
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
