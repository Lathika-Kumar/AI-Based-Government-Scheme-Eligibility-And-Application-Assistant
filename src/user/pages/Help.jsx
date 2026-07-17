import React, { useState } from "react";
import { useApp } from "@context/AppContext";
import { useAuth } from "@context/AuthContext";
import { CONFIG } from "@config/env";
import {
  HelpCircle,
  Mail,
  Phone,
  Clock,
  CheckCircle,
  ChevronDown,
  ChevronUp,
  Send,
  MessageSquare
} from "lucide-react";

export default function Help() {
  const { user } = useAuth();
  const { schemes, grievances, submitGrievance, t, language } = useApp();

  const [faqSearch, setFaqSearch] = useState("");
  const [expandedFaq, setExpandedFaq] = useState(null);

  // Form states
  const [formEmail, setFormEmail] = useState(user?.email || "citizen@schemebridge.in");
  const [formPhone, setFormPhone] = useState("9876543210");
  const [formScheme, setFormScheme] = useState(schemes[0]?.name || "General Query");
  const [formCategory, setFormCategory] = useState("Payment Delayed");
  const [formDesc, setFormDesc] = useState("");
  const [formNote, setFormNote] = useState("");

  const [submittedId, setSubmittedId] = useState(null);

  const faqCategories = [
    {
      q: t("help_cit_faq_q1"),
      a: t("help_cit_faq_a1")
    },
    {
      q: t("help_cit_faq_q2"),
      a: t("help_cit_faq_a2")
    },
    {
      q: t("help_cit_faq_q3"),
      a: t("help_cit_faq_a3")
    },
    {
      q: t("help_cit_faq_q4"),
      a: t("help_cit_faq_a4")
    }
  ];

  const categories = [
    { value: "Payment Delayed", label: t("help_cat_payment") },
    { value: "Eligibility Engine Dispute", label: t("help_cat_dispute") },
    { value: "Document Verification Block", label: t("help_cat_doc_block") },
    { value: "Official Link Broken", label: t("help_cat_link_broken") },
    { value: "Application Status Discrepancy", label: t("help_cat_discrepancy") },
    { value: "General Support Desk", label: t("help_cat_general") }
  ];

  const getCategoryLabel = (catValue) => {
    const found = categories.find((c) => c.value === catValue);
    return found ? found.label : catValue;
  };

  const getStatusText = (status) => {
    const statusMap = {
      en: {
        "Received": "Received",
        "In Review": "In Review",
        "Resolved": "Resolved",
        "Closed": "Closed"
      },
      hi: {
        "Received": "प्राप्त",
        "In Review": "समीक्षा में",
        "Resolved": "समाधान किया गया",
        "Closed": "बंद"
      },
      ta: {
        "Received": "பெறப்பட்டது",
        "In Review": "மதிப்பாய்வில்",
        "Resolved": "தீர்க்கப்பட்டது",
        "Closed": "மூடப்பட்டது"
      },
      te: {
        "Received": "స్వీకరించబడింది",
        "In Review": "సమీక్షలో ఉంది",
        "Resolved": "పరిష్కరించబడింది",
        "Closed": "మూసివేయబడింది"
      },
      kn: {
        "Received": "ಸ್ವೀಕರಿಸಲಾಗಿದೆ",
        "In Review": "ಪರಿಶೀಲನೆಯಲ್ಲಿದೆ",
        "Resolved": "ಪರಿಹರಿಸಲಾಗಿದೆ",
        "Closed": "ಮುಚ್ಚಲಾಗಿದೆ"
      }
    };
    const lang = language || "en";
    const dict = statusMap[lang] || statusMap["en"];
    return dict[status] || status;
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    if (!formDesc.trim()) {
      return;
    }

    const gId = submitGrievance({
      email: formEmail,
      phone: formPhone,
      relatedScheme: formScheme,
      category: formCategory,
      description: formDesc,
      supportingNote: formNote
    });

    setSubmittedId(gId);
    setFormDesc("");
    setFormNote("");
  };

  const filteredFaqs = faqCategories.filter(
    (faq) =>
      faq.q.toLowerCase().includes(faqSearch.toLowerCase()) ||
      faq.a.toLowerCase().includes(faqSearch.toLowerCase())
  );

  // Filter grievances filed by this user
  const userGrievances = grievances.filter(
    (g) => g.citizenEmail === formEmail || g.citizenName === (user?.name || "Rajesh Patel")
  );

  const getStatusBadge = (status) => {
    switch (status) {
      case "Received":
        return "bg-government-blue/10 text-government-blue border-government-blue/20";
      case "In Review":
        return "bg-saffron/10 text-saffron-dark border-saffron/20";
      case "Resolved":
        return "bg-india-green/10 text-india-green border-india-green/20";
      case "Closed":
        return "bg-gray-100 text-gray-600 border-gray-200";
      default:
        return "bg-gray-100 text-gray-700 border-gray-200";
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="bg-white border border-gray-200 p-6 rounded-2xl shadow-sm">
        <h1 className="text-xl font-bold text-gray-900">{t("help_title")}</h1>
        <p className="text-gray-500 text-xs mt-1">{t("help_desc")}</p>
      </div>

      <div className="grid lg:grid-cols-3 gap-6">
        {/* Left/Middle Column - FAQs & Grievance Form */}
        <div className="lg:col-span-2 space-y-6">

          {/* FAQ Search */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm space-y-4">
            <h2 className="text-sm font-bold text-gray-800 uppercase tracking-wider flex items-center gap-1.5">
              <HelpCircle className="h-4.5 w-4.5 text-government-blue" />
              {t("help_faq_header")}
            </h2>
            <input
              type="text"
              placeholder={t("help_faq_search_placeholder")}
              value={faqSearch}
              onChange={(e) => setFaqSearch(e.target.value)}
              className="w-full px-3.5 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl text-xs transition"
            />

            <div className="space-y-2">
              {filteredFaqs.length === 0 ? (
                <p className="text-gray-400 text-xs py-4 text-center">{t("help_faq_no_match")}</p>
              ) : (
                filteredFaqs.map((faq, idx) => (
                  <div key={idx} className="border border-gray-200 rounded-xl overflow-hidden">
                    <button
                      onClick={() => setExpandedFaq(expandedFaq === idx ? null : idx)}
                      className="w-full flex items-start justify-between gap-3 p-3.5 text-left hover:bg-gray-50 transition text-xs font-semibold text-gray-800"
                    >
                      <span>{faq.q}</span>
                      {expandedFaq === idx ? (
                        <ChevronUp className="h-4 w-4 text-gray-400 shrink-0" />
                      ) : (
                        <ChevronDown className="h-4 w-4 text-gray-400 shrink-0" />
                      )}
                    </button>
                    {expandedFaq === idx && (
                      <div className="px-4 pb-3.5 text-[11px] text-gray-500 leading-relaxed border-t border-gray-100 pt-2.5 bg-gray-50/50">
                        {faq.a}
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Grievance Submission Form */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm space-y-4">
            <h2 className="text-sm font-bold text-gray-800 uppercase tracking-wider flex items-center gap-1.5">
              <MessageSquare className="h-4.5 w-4.5 text-government-blue" />
              {t("help_grievance_header")}
            </h2>

            {submittedId ? (
              <div className="bg-india-green/10 border border-india-green/20 p-6 rounded-2xl text-center space-y-4 animate-in fade-in zoom-in-95 duration-200">
                <CheckCircle className="h-10 w-10 text-india-green mx-auto" />
                <div className="space-y-1">
                  <h3 className="font-bold text-india-green-dark text-sm">{t("help_grievance_success_title")}</h3>
                  <p className="text-india-green-dark/90 text-xs">
                    {t("help_grievance_success_ticket")} <strong className="font-mono text-gray-900 text-xs bg-india-green/10 px-1.5 py-0.5 rounded">{submittedId}</strong>.
                  </p>
                </div>
                <p className="text-[11px] text-gray-500 max-w-sm mx-auto leading-relaxed">
                  {t("help_grievance_success_forward")}
                </p>
                <button
                  type="button"
                  onClick={() => setSubmittedId(null)}
                  className="inline-flex items-center space-x-1 border border-india-green/30 bg-white hover:bg-india-green/10 text-india-green-dark py-1.5 px-4 rounded-xl text-xs font-semibold shadow-sm transition"
                >
                  {t("help_file_another_ticket")}
                </button>
              </div>
            ) : (
              <form onSubmit={handleFormSubmit} className="space-y-4 text-xs">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="grievanceEmail" className="block font-semibold text-gray-600 mb-1.5">{t("help_label_email")}</label>
                    <input
                      id="grievanceEmail"
                      type="email"
                      required
                      value={formEmail}
                      onChange={(e) => setFormEmail(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    />
                  </div>
                  <div>
                    <label htmlFor="grievancePhone" className="block font-semibold text-gray-600 mb-1.5">{t("help_label_phone")}</label>
                    <input
                      id="grievancePhone"
                      type="text"
                      required
                      value={formPhone}
                      onChange={(e) => setFormPhone(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="grievanceScheme" className="block font-semibold text-gray-600 mb-1.5">{t("help_label_scheme")}</label>
                    <select
                      id="grievanceScheme"
                      value={formScheme}
                      onChange={(e) => setFormScheme(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    >
                      <option value="General Query">{t("help_option_general_query")}</option>
                      {schemes.map((s) => (
                        <option key={s.id} value={s.name}>
                          {s.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="grievanceCategory" className="block font-semibold text-gray-600 mb-1.5">{t("help_label_category")}</label>
                    <select
                      id="grievanceCategory"
                      value={formCategory}
                      onChange={(e) => setFormCategory(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    >
                      {categories.map((c) => (
                        <option key={c.value} value={c.value}>
                          {c.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label htmlFor="grievanceDesc" className="block font-semibold text-gray-600 mb-1.5">{t("help_label_desc")}</label>
                  <textarea
                    id="grievanceDesc"
                    required
                    rows="4"
                    placeholder={t("help_placeholder_desc")}
                    value={formDesc}
                    onChange={(e) => setFormDesc(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                  />
                </div>

                <div>
                  <label htmlFor="grievanceNote" className="block font-semibold text-gray-600 mb-1.5">{t("help_label_note")}</label>
                  <input
                    id="grievanceNote"
                    type="text"
                    placeholder={t("help_placeholder_note")}
                    value={formNote}
                    onChange={(e) => setFormNote(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                  />
                </div>

                <div className="flex justify-end pt-2">
                  <button
                    type="submit"
                    className="inline-flex items-center space-x-1.5 bg-government-blue hover:bg-government-blue-dark text-white py-2 px-5 rounded-xl font-bold shadow transition focus:outline-none focus:ring-2 focus:ring-government-blue focus:ring-offset-2"
                  >
                    <Send className="h-3.5 w-3.5" />
                    <span>{t("help_btn_submit")}</span>
                  </button>
                </div>
              </form>
            )}
          </div>

        </div>

        {/* Right Sidebar - Support contacts & Raised grievances list */}
        <div className="space-y-6">
          {/* Help Desk Contacts */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm space-y-4">
            <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider">{t("help_sidebar_hotlines")}</h3>
            <div className="space-y-3 text-xs leading-normal text-gray-500">
              <div className="flex items-start gap-2.5">
                <Phone className="h-4.5 w-4.5 text-government-blue shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-700">{t("help_sidebar_tollfree")}</p>
                  <p className="font-medium text-gray-600">{CONFIG.HELP_LINE}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <Mail className="h-4.5 w-4.5 text-government-blue shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-700">{t("help_sidebar_email")}</p>
                  <p className="font-medium text-gray-600">{CONFIG.SUPPORT_EMAIL}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <Clock className="h-4.5 w-4.5 text-government-blue shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-700">{t("help_sidebar_hours")}</p>
                  <p className="font-medium text-gray-600">{t("help_sidebar_hours_val")}</p>
                </div>
              </div>
            </div>
          </div>

          {/* Active Grievances Table */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm space-y-4">
            <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider">{t("help_sidebar_grievances")}</h3>

            {userGrievances.length === 0 ? (
              <p className="text-gray-400 text-xs text-center py-4">{t("help_sidebar_no_grievances")}</p>
            ) : (
              <div className="space-y-3">
                {userGrievances.map((g) => (
                  <div key={g.id} className="border border-gray-200 rounded-xl p-3.5 space-y-2 hover:border-gray-300 transition">
                    <div className="flex justify-between items-center text-[10px]">
                      <span className="font-mono font-bold text-gray-900 bg-gray-100 px-1.5 py-0.2 rounded">{g.id}</span>
                      <span className="text-gray-400 font-medium">{g.date}</span>
                    </div>
                    <div>
                      <p className="text-[11px] font-bold text-gray-800 truncate">{getCategoryLabel(g.category)}</p>
                      <p className="text-[10px] text-gray-400 mt-0.5 truncate">{g.relatedScheme}</p>
                    </div>
                    <div className="flex items-center justify-between pt-1 border-t border-gray-100">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full font-extrabold border text-[9px] ${getStatusBadge(g.status)}`}>
                        {getStatusText(g.status)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
