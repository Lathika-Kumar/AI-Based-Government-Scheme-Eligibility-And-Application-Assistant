import React, { useState } from "react";
import { useApp } from "@context/AppContext";
import { useAuth } from "@context/AuthContext";
import { CONFIG } from "@config/env";
import { usePageMeta } from "@utils/usePageMeta";
import { useToast } from "@components/ui/ToastNotification";
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
  const { schemes, grievances, submitGrievance } = useApp();
  const { showToast } = useToast();
  usePageMeta("Help & Grievances", "File grievances, track support tickets, and view FAQs");

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
      q: "How do I check scheme status?",
      a: "Navigate to Application Tracker in your dashboard to view active status and history."
    },
    {
      q: "What if my Aadhaar or document verification fails?",
      a: "Upload an updated clear PDF or JPEG copy in Document Vault, or submit a verification ticket below."
    },
    {
      q: "How long does grievance resolution take?",
      a: "Grievance tickets are processed within 3-5 working days by designated department officers."
    },
    {
      q: "Can I cancel a submitted application?",
      a: "Once submitted, applications are processed by respective authorities. Contact support desk for assistance."
    }
  ];

  const categories = [
    { value: "Payment Delayed", label: "Payment Delayed" },
    { value: "Eligibility Engine Dispute", label: "Eligibility Engine Dispute" },
    { value: "Document Verification Block", label: "Document Verification Block" },
    { value: "Official Link Broken", label: "Official Link Broken" },
    { value: "Application Status Discrepancy", label: "Application Status Discrepancy" },
    { value: "General Support Desk", label: "General Support Desk" }
  ];

  const getCategoryLabel = (catValue) => {
    const found = categories.find((c) => c.value === catValue);
    return found ? found.label : catValue;
  };

  const getStatusText = (status) => {
    return status;
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
    showToast("success", "Grievance Filed", `Ticket Reference: ${gId}`);
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
        <h1 className="text-xl font-bold text-gray-900">Help & Grievance Portal</h1>
        <p className="text-gray-500 text-xs mt-1">File official grievances, track support tickets, and view FAQs.</p>
      </div>

      <div className="grid lg:grid-cols-3 gap-6">
        {/* Left/Middle Column - FAQs & Grievance Form */}
        <div className="lg:col-span-2 space-y-6">

          {/* FAQ Search */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm space-y-4">
            <h2 className="text-sm font-bold text-gray-800 uppercase tracking-wider flex items-center gap-1.5">
              <HelpCircle className="h-4.5 w-4.5 text-government-blue" />
              Frequently Asked Questions
            </h2>
            <input
              type="text"
              placeholder="Search FAQs..."
              value={faqSearch}
              onChange={(e) => setFaqSearch(e.target.value)}
              className="w-full px-3.5 py-2 border border-gray-200 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl text-xs transition"
            />

            <div className="space-y-2">
              {filteredFaqs.length === 0 ? (
                <p className="text-gray-400 text-xs py-4 text-center">No matching questions found.</p>
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
              File a Grievance Ticket
            </h2>

            {submittedId ? (
              <div className="bg-india-green/10 border border-india-green/20 p-6 rounded-2xl text-center space-y-4 animate-in fade-in zoom-in-95 duration-200">
                <CheckCircle className="h-10 w-10 text-india-green mx-auto" />
                <div className="space-y-1">
                  <h3 className="font-bold text-india-green-dark text-sm">Grievance Ticket Submitted</h3>
                  <p className="text-india-green-dark/90 text-xs">
                    Your grievance has been filed under ID: <strong className="font-mono text-gray-900 text-xs bg-india-green/10 px-1.5 py-0.5 rounded">{submittedId}</strong>.
                  </p>
                </div>
                <p className="text-[11px] text-gray-500 max-w-sm mx-auto leading-relaxed">
                  Your ticket is queued for resolution by designated department officers.
                </p>
                <button
                  type="button"
                  onClick={() => setSubmittedId(null)}
                  className="inline-flex items-center space-x-1 border border-india-green/30 bg-white hover:bg-india-green/10 text-india-green-dark py-1.5 px-4 rounded-xl text-xs font-semibold shadow-sm transition"
                >
                  File Another Ticket
                </button>
              </div>
            ) : (
              <form onSubmit={handleFormSubmit} className="space-y-4 text-xs">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="grievanceEmail" className="block font-semibold text-gray-600 mb-1.5">Email Address</label>
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
                    <label htmlFor="grievancePhone" className="block font-semibold text-gray-600 mb-1.5">Mobile Number</label>
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
                    <label htmlFor="grievanceScheme" className="block font-semibold text-gray-600 mb-1.5">Related Scheme</label>
                    <select
                      id="grievanceScheme"
                      value={formScheme}
                      onChange={(e) => setFormScheme(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    >
                      <option value="General Query">General Query</option>
                      {schemes.map((s) => (
                        <option key={s.id} value={s.name}>
                          {s.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="grievanceCategory" className="block font-semibold text-gray-600 mb-1.5">Issue Category</label>
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
                  <label htmlFor="grievanceDesc" className="block font-semibold text-gray-600 mb-1.5">Description of Grievance</label>
                  <textarea
                    id="grievanceDesc"
                    required
                    rows="4"
                    placeholder="Provide details regarding payment delays, document verification blocks, or system disputes..."
                    value={formDesc}
                    onChange={(e) => setFormDesc(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                  />
                </div>

                <div>
                  <label htmlFor="grievanceNote" className="block font-semibold text-gray-600 mb-1.5">Additional Notes (Optional)</label>
                  <input
                    id="grievanceNote"
                    type="text"
                    placeholder="Any extra context..."
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
                    <span>Submit Grievance Ticket</span>
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
            <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider">Support Hotlines</h3>
            <div className="space-y-3 text-xs leading-normal text-gray-500">
              <div className="flex items-start gap-2.5">
                <Phone className="h-4.5 w-4.5 text-government-blue shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-700">Toll Free Helpline</p>
                  <p className="font-medium text-gray-600">{CONFIG.HELP_LINE}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <Mail className="h-4.5 w-4.5 text-government-blue shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-700">Support Email</p>
                  <p className="font-medium text-gray-600">{CONFIG.SUPPORT_EMAIL}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <Clock className="h-4.5 w-4.5 text-government-blue shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-700">Operating Hours</p>
                  <p className="font-medium text-gray-600">Mon - Sat, 9:00 AM - 6:00 PM IST</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <div className="bg-gray-100 p-2 rounded-lg text-gray-700 shrink-0">
                  <svg className="h-4.5 w-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                </div>
                <div>
                  <p className="font-bold text-gray-700">National Portal HQ</p>
                  <p className="font-medium text-gray-600">e-Governance Directorate, CGO Complex, Lodhi Road, New Delhi - 110003</p>
                </div>
              </div>
            </div>
          </div>

          {/* Active Grievances Table */}
          <div className="bg-white border border-gray-200 p-5 rounded-2xl shadow-sm space-y-4">
            <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider">Your Grievance Tickets</h3>

            {userGrievances.length === 0 ? (
              <p className="text-gray-400 text-xs text-center py-4">No grievances filed yet.</p>
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
