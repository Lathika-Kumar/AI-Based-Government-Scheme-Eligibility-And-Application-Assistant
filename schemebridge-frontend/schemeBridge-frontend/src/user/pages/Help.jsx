import React, { useState } from "react";
import { useApp } from "@context/AppContext";
import { useAuth } from "@context/AuthContext";
import { CONFIG } from "@config/env";
import { usePageMeta } from "@utils/usePageMeta";
import { useToast } from "@components/ui/ToastNotification";
import * as grievanceService from "@services/grievanceService";
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

  const [expandedGrievanceId, setExpandedGrievanceId] = useState(null);
  const [citizenReplies, setCitizenReplies] = useState({});
  const [submittingReply, setSubmittingReply] = useState(false);

  const handleSendCitizenReply = async (grievanceId) => {
    const text = (citizenReplies[grievanceId] || "").trim();
    if (!text) return;
    setSubmittingReply(true);
    try {
      const res = await grievanceService.replyToGrievance(grievanceId, text);
      if (!res.error) {
        showToast("success", "Reply Sent", "Your reply has been submitted to the grievance desk.");
        setCitizenReplies((prev) => ({ ...prev, [grievanceId]: "" }));
      } else {
        showToast("error", "Reply Failed", res.message || "Failed to submit reply.");
      }
    } catch (err) {
      const errMsg = err?.response?.data?.message || err?.message || "Failed to submit reply.";
      showToast("error", "Reply Failed", errMsg);
    } finally {
      setSubmittingReply(false);
    }
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
    const s = String(status || "").toUpperCase();
    if (s === "RESOLVED" || s === "CLOSED") {
      return "bg-india-green/10 text-india-green border-india-green/20";
    }
    if (s === "IN_PROGRESS" || s === "IN REVIEW" || s === "WAITING_FOR_CITIZEN") {
      return "bg-saffron/10 text-saffron-dark border-saffron/20";
    }
    if (s === "REJECTED") {
      return "bg-red-100 text-red-700 border-red-200";
    }
    return "bg-government-blue/10 text-government-blue border-government-blue/20";
  };

  const getStatusText = (status) => {
    const s = String(status || "").toUpperCase();
    if (s === "RESOLVED") return "Resolved";
    if (s === "CLOSED") return "Closed";
    if (s === "IN_PROGRESS" || s === "IN REVIEW") return "In Review";
    if (s === "WAITING_FOR_CITIZEN") return "Awaiting Citizen";
    if (s === "REJECTED") return "Rejected";
    return status || "Received";
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 p-6 rounded-2xl shadow-sm">
        <h1 className="text-xl font-bold text-gray-900 dark:text-white">Help & Grievance Portal</h1>
        <p className="text-gray-500 dark:text-slate-400 text-xs mt-1">File official grievances, track support tickets, and view FAQs.</p>
      </div>

      <div className="grid lg:grid-cols-3 gap-6">
        {/* Left/Middle Column - FAQs & Grievance Form */}
        <div className="lg:col-span-2 space-y-6">

          {/* FAQ Search */}
          <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm space-y-4">
            <h2 className="text-sm font-bold text-gray-800 dark:text-slate-200 uppercase tracking-wider flex items-center gap-1.5">
              <HelpCircle className="h-4.5 w-4.5 text-government-blue dark:text-blue-400" />
              Frequently Asked Questions
            </h2>
            <input
              type="text"
              placeholder="Search FAQs..."
              value={faqSearch}
              onChange={(e) => setFaqSearch(e.target.value)}
              className="w-full px-3.5 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:bg-white dark:focus:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-government-blue rounded-xl text-xs transition"
            />

            <div className="space-y-2">
              {filteredFaqs.length === 0 ? (
                <p className="text-gray-400 dark:text-slate-500 text-xs py-4 text-center">No matching questions found.</p>
              ) : (
                filteredFaqs.map((faq, idx) => (
                  <div key={idx} className="border border-gray-200 dark:border-slate-700 rounded-xl overflow-hidden bg-white dark:bg-slate-800/50">
                    <button
                      onClick={() => setExpandedFaq(expandedFaq === idx ? null : idx)}
                      className="w-full flex items-start justify-between gap-3 p-3.5 text-left hover:bg-gray-50 dark:hover:bg-slate-800 transition text-xs font-semibold text-gray-800 dark:text-slate-200"
                    >
                      <span>{faq.q}</span>
                      {expandedFaq === idx ? (
                        <ChevronUp className="h-4 w-4 text-gray-400 dark:text-slate-400 shrink-0" />
                      ) : (
                        <ChevronDown className="h-4 w-4 text-gray-400 dark:text-slate-400 shrink-0" />
                      )}
                    </button>
                    {expandedFaq === idx && (
                      <div className="px-4 pb-3.5 text-[11px] text-gray-600 dark:text-slate-300 leading-relaxed border-t border-gray-100 dark:border-slate-750 pt-2.5 bg-gray-50/50 dark:bg-slate-900/40">
                        {faq.a}
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Grievance Submission Form */}
          <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm space-y-4">
            <h2 className="text-sm font-bold text-gray-800 dark:text-slate-200 uppercase tracking-wider flex items-center gap-1.5">
              <MessageSquare className="h-4.5 w-4.5 text-government-blue dark:text-blue-400" />
              File a Grievance Ticket
            </h2>

            {submittedId ? (
              <div className="bg-india-green/10 border border-india-green/20 p-6 rounded-2xl text-center space-y-4 animate-in fade-in zoom-in-95 duration-200">
                <CheckCircle className="h-10 w-10 text-india-green mx-auto" />
                <div className="space-y-1">
                  <h3 className="font-bold text-india-green-dark dark:text-emerald-400 text-sm">Grievance Ticket Submitted</h3>
                  <p className="text-india-green-dark/90 dark:text-emerald-300 text-xs">
                    Your grievance has been filed under ID: <strong className="font-mono text-gray-900 dark:text-white text-xs bg-india-green/10 dark:bg-emerald-950/60 px-1.5 py-0.5 rounded">{submittedId}</strong>.
                  </p>
                </div>
                <p className="text-[11px] text-gray-500 dark:text-slate-400 max-w-sm mx-auto leading-relaxed">
                  Your ticket is queued for resolution by designated department officers.
                </p>
                <button
                  type="button"
                  onClick={() => setSubmittedId(null)}
                  className="inline-flex items-center space-x-1 border border-india-green/30 bg-white dark:bg-slate-800 hover:bg-india-green/10 dark:hover:bg-slate-700 text-india-green-dark dark:text-emerald-300 py-1.5 px-4 rounded-xl text-xs font-semibold shadow-sm transition"
                >
                  File Another Ticket
                </button>
              </div>
            ) : (
              <form onSubmit={handleFormSubmit} className="space-y-4 text-xs">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="grievanceEmail" className="block font-semibold text-gray-600 dark:text-slate-300 mb-1.5">Email Address</label>
                    <input
                      id="grievanceEmail"
                      type="email"
                      required
                      value={formEmail}
                      onChange={(e) => setFormEmail(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-900 dark:text-slate-100 rounded-lg focus:bg-white dark:focus:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    />
                  </div>
                  <div>
                    <label htmlFor="grievancePhone" className="block font-semibold text-gray-600 dark:text-slate-300 mb-1.5">Mobile Number</label>
                    <input
                      id="grievancePhone"
                      type="text"
                      required
                      value={formPhone}
                      onChange={(e) => setFormPhone(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-900 dark:text-slate-100 rounded-lg focus:bg-white dark:focus:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="grievanceScheme" className="block font-semibold text-gray-600 dark:text-slate-300 mb-1.5">Related Scheme</label>
                    <select
                      id="grievanceScheme"
                      value={formScheme}
                      onChange={(e) => setFormScheme(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-900 dark:text-slate-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    >
                      <option value="General Query" className="bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100">General Query</option>
                      {schemes.map((s) => (
                        <option key={s.id} value={s.name} className="bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100">
                          {s.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="grievanceCategory" className="block font-semibold text-gray-600 dark:text-slate-300 mb-1.5">Issue Category</label>
                    <select
                      id="grievanceCategory"
                      value={formCategory}
                      onChange={(e) => setFormCategory(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-900 dark:text-slate-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    >
                      {categories.map((c) => (
                        <option key={c.value} value={c.value} className="bg-white dark:bg-slate-800 text-gray-900 dark:text-slate-100">
                          {c.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label htmlFor="grievanceDesc" className="block font-semibold text-gray-600 dark:text-slate-300 mb-1.5">Description of Grievance</label>
                  <textarea
                    id="grievanceDesc"
                    required
                    rows="4"
                    placeholder="Provide details regarding payment delays, document verification blocks, or system disputes..."
                    value={formDesc}
                    onChange={(e) => setFormDesc(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:bg-white dark:focus:bg-slate-900 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                  />
                </div>

                <div>
                  <label htmlFor="grievanceNote" className="block font-semibold text-gray-600 dark:text-slate-300 mb-1.5">Additional Notes (Optional)</label>
                  <input
                    id="grievanceNote"
                    type="text"
                    placeholder="Any extra context..."
                    value={formNote}
                    onChange={(e) => setFormNote(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:bg-white dark:focus:bg-slate-900 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
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
          <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm space-y-4">
            <h3 className="font-bold text-gray-800 dark:text-slate-200 text-xs uppercase tracking-wider">Support Hotlines</h3>
            <div className="space-y-3 text-xs leading-normal text-gray-500 dark:text-slate-400">
              <div className="flex items-start gap-2.5">
                <Phone className="h-4.5 w-4.5 text-government-blue dark:text-blue-400 shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-700 dark:text-slate-300">Toll Free Helpline</p>
                  <p className="font-medium text-gray-600 dark:text-slate-400">{CONFIG.HELP_LINE}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <Mail className="h-4.5 w-4.5 text-government-blue dark:text-blue-400 shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-700 dark:text-slate-300">Support Email</p>
                  <p className="font-medium text-gray-600 dark:text-slate-400">{CONFIG.SUPPORT_EMAIL}</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <Clock className="h-4.5 w-4.5 text-government-blue dark:text-blue-400 shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-700 dark:text-slate-300">Operating Hours</p>
                  <p className="font-medium text-gray-600 dark:text-slate-400">Mon - Sat, 9:00 AM - 6:00 PM IST</p>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <div className="bg-gray-100 dark:bg-slate-800 p-2 rounded-lg text-gray-700 dark:text-slate-300 shrink-0">
                  <svg className="h-4.5 w-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                </div>
                <div>
                  <p className="font-bold text-gray-700 dark:text-slate-300">National Portal HQ</p>
                  <p className="font-medium text-gray-600 dark:text-slate-400">e-Governance Directorate, CGO Complex, Lodhi Road, New Delhi - 110003</p>
                </div>
              </div>
            </div>
          </div>

          {/* Active Grievances Table */}
          <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm space-y-4">
            <h3 className="font-bold text-gray-800 dark:text-slate-200 text-xs uppercase tracking-wider">Your Grievance Tickets</h3>

            {userGrievances.length === 0 ? (
              <p className="text-gray-400 dark:text-slate-500 text-xs text-center py-4">No grievances filed yet.</p>
            ) : (
              <div className="space-y-3">
                {userGrievances.map((g) => {
                  const isExpanded = expandedGrievanceId === g.id;
                  const isTerminal = ["RESOLVED", "CLOSED", "REJECTED"].includes(String(g.status || "").toUpperCase());
                  return (
                    <div
                      key={g.id}
                      className="border border-gray-200 dark:border-slate-700 rounded-xl p-3.5 space-y-2 hover:border-gray-300 dark:hover:border-slate-600 transition bg-white dark:bg-slate-850"
                    >
                      <div
                        onClick={() => setExpandedGrievanceId(isExpanded ? null : g.id)}
                        className="cursor-pointer space-y-2"
                      >
                        <div className="flex justify-between items-center text-[10px]">
                          <span className="font-mono font-bold text-gray-900 dark:text-slate-100 bg-gray-100 dark:bg-slate-800 px-1.5 py-0.5 rounded">
                            {g.grievanceNumber || g.id}
                          </span>
                          <span className="text-gray-400 dark:text-slate-500 font-medium">
                            {g.createdAt ? new Date(g.createdAt).toLocaleDateString("en-IN") : (g.date || "")}
                          </span>
                        </div>
                        <div>
                          <p className="text-[11px] font-bold text-gray-800 dark:text-slate-200 truncate">
                            {getCategoryLabel(g.category)}
                          </p>
                          <p className="text-[10px] text-gray-400 dark:text-slate-500 mt-0.5 truncate">
                            {g.relatedScheme || g.subject || "General"}
                          </p>
                        </div>
                        <div className="flex items-center justify-between pt-1 border-t border-gray-100 dark:border-slate-800">
                          <span className={`inline-flex items-center px-2 py-0.5 rounded-full font-extrabold border text-[9px] ${getStatusBadge(g.status)}`}>
                            {getStatusText(g.status)}
                          </span>
                          <span className="text-[10px] text-government-blue dark:text-indigo-400 font-semibold flex items-center gap-0.5">
                            {isExpanded ? (
                              <>Hide Details <ChevronUp className="h-3 w-3" /></>
                            ) : (
                              <>View Details <ChevronDown className="h-3 w-3" /></>
                            )}
                          </span>
                        </div>
                      </div>

                      {/* Expanded Section */}
                      {isExpanded && (
                        <div className="pt-2 border-t border-gray-100 dark:border-slate-800 space-y-2.5 text-xs">
                          {g.description && (
                            <div className="space-y-1">
                              <span className="text-[10px] font-bold text-gray-500 dark:text-slate-400 uppercase tracking-wider block">
                                Your Complaint
                              </span>
                              <p className="p-2.5 bg-gray-50 dark:bg-slate-800 rounded-lg text-gray-700 dark:text-slate-300 leading-relaxed text-[11px]">
                                {g.description}
                              </p>
                            </div>
                          )}

                          {/* Official Response */}
                          {g.resolution ? (
                            <div className="p-2.5 bg-indigo-50/70 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-800/60 rounded-lg space-y-1">
                              <span className="text-[10px] font-bold text-indigo-700 dark:text-indigo-300 uppercase tracking-wider block">
                                Official Resolution / Officer Response
                              </span>
                              <p className="text-[11px] text-slate-800 dark:text-slate-200 leading-relaxed font-medium">
                                {g.resolution}
                              </p>
                              {g.resolvedAt && (
                                <span className="text-[9px] text-slate-400 dark:text-slate-500 block pt-0.5">
                                  Resolved on {new Date(g.resolvedAt).toLocaleString("en-IN")}
                                </span>
                              )}
                            </div>
                          ) : null}

                          {/* Terminal state indicator or Reply Form */}
                          {isTerminal ? (
                            <div className="p-2.5 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/60 rounded-lg text-xs text-emerald-800 dark:text-emerald-300 flex items-center gap-1.5">
                              <CheckCircle className="h-4 w-4 shrink-0 text-emerald-600 dark:text-emerald-400" />
                              <span className="font-semibold text-[11px]">
                                This grievance has been resolved. Further replies are closed.
                              </span>
                            </div>
                          ) : (
                            <div className="space-y-1.5 pt-1">
                              <label className="text-[10px] font-bold text-gray-600 dark:text-slate-300 uppercase tracking-wider block">
                                Send Reply to Officer
                              </label>
                              <div className="flex gap-1.5">
                                <input
                                  type="text"
                                  placeholder="Type your reply here..."
                                  value={citizenReplies[g.id] || ""}
                                  onChange={(e) => setCitizenReplies((prev) => ({ ...prev, [g.id]: e.target.value }))}
                                  className="flex-1 px-2.5 py-1.5 border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-800 text-gray-900 dark:text-slate-100 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-government-blue"
                                />
                                <button
                                  type="button"
                                  disabled={submittingReply}
                                  onClick={() => handleSendCitizenReply(g.id)}
                                  className="px-3 py-1.5 bg-government-blue hover:bg-government-blue-dark text-white rounded-lg font-bold text-xs flex items-center gap-1 shrink-0 transition"
                                >
                                  <Send className="h-3 w-3" />
                                  Send
                                </button>
                              </div>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
