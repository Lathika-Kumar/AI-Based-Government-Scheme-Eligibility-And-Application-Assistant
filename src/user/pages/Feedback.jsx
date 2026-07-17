import React, { useState } from "react";
import { useApp } from "@context/AppContext";
import { useAuth } from "@context/AuthContext";
import { Star, Send, MessageSquare, CheckCircle, AlertCircle, ThumbsUp } from "lucide-react";

export default function Feedback() {
  const { user } = useAuth();
  const { schemes, feedback, submitFeedback, profile } = useApp();

  const [formType, setFormType] = useState("Portal Rating");
  const [formRating, setFormRating] = useState(0);
  const [formComment, setFormComment] = useState("");
  const [formRelatedScheme, setFormRelatedScheme] = useState("");
  const [submittedId, setSubmittedId] = useState(null);

  const types = [
    { value: "Portal Rating", label: "Rate the Portal" },
    { value: "Scheme Suggestion", label: "Suggest a Scheme" },
    { value: "Bug Report", label: "Report an Issue" },
    { value: "General Feedback", label: "General Comments" }
  ];

  const userFeedback = feedback.filter(
    (f) => f.citizenEmail === user?.email || f.citizenName === (profile?.name || user?.name)
  );

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!formComment.trim()) return;

    const fbId = submitFeedback({
      type: formType,
      rating: formRating,
      comment: formComment,
      relatedScheme: formRelatedScheme || null
    });

    setSubmittedId(fbId);
    setFormRating(0);
    setFormComment("");
    setFormRelatedScheme("");
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "Received": return "bg-government-blue/10 text-government-blue border-government-blue/20";
      case "Under Review": return "bg-saffron/10 text-saffron-dark border-saffron/20";
      case "Acknowledged": return "bg-saffron/10 text-saffron-dark border-saffron/20";
      case "Resolved": return "bg-india-green/10 text-india-green border-india-green/20";
      default: return "bg-gray-100 text-gray-700 border-gray-200";
    }
  };

  const StarRating = ({ current, onChange }) => {
    return (
      <div className="flex gap-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            onClick={() => onChange(star)}
            className="p-1 focus:outline-none"
          >
            <Star
              className={`w-6 h-6 ${
                star <= current
                  ? "fill-saffron-dark text-saffron-dark"
                  : "text-gray-300"
              }`}
            />
          </button>
        ))}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="bg-white border border-gray-200 p-6 rounded-2xl shadow-sm">
        <div className="flex items-center gap-2 mb-2">
          <ThumbsUp className="h-6 w-6 text-government-blue" />
          <h1 className="text-xl font-bold text-gray-900">Your Feedback</h1>
        </div>
        <p className="text-gray-500 text-sm">Help us improve SchemeBridge with your valuable feedback and suggestions.</p>
      </div>

      <div className="grid lg:grid-cols-3 gap-6">
        {/* Left Column: Feedback Form */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white border border-gray-200 p-6 rounded-2xl shadow-sm">
            <h2 className="text-sm font-bold text-gray-800 uppercase tracking-wider flex items-center gap-1.5 mb-4">
              <MessageSquare className="h-4.5 w-4.5 text-government-blue" />
              Submit Feedback
            </h2>

            {submittedId ? (
              <div className="bg-india-green/10 border border-india-green/20 p-6 rounded-2xl text-center space-y-4 animate-in fade-in zoom-in-95 duration-200">
                <CheckCircle className="h-10 w-10 text-india-green mx-auto" />
                <div className="space-y-1">
                  <h3 className="font-bold text-india-green-dark text-sm">Thank you for your feedback!</h3>
                  <p className="text-india-green-dark/90 text-xs">
                    Your feedback reference: <strong className="font-mono text-gray-900 text-xs bg-india-green/10 px-1.5 py-0.5 rounded">{submittedId}</strong>
                  </p>
                </div>
                <p className="text-[11px] text-gray-500 max-w-sm mx-auto leading-relaxed">
                  We appreciate you taking the time to help us improve. Our team will review your feedback shortly.
                </p>
                <button
                  type="button"
                  onClick={() => setSubmittedId(null)}
                  className="inline-flex items-center space-x-1 border border-india-green/30 bg-white hover:bg-india-green/10 text-india-green-dark py-1.5 px-4 rounded-xl text-xs font-semibold shadow-sm transition"
                >
                  Submit Another Feedback
                </button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4 text-xs">
                {/* Feedback Type */}
                <div>
                  <label className="block font-semibold text-gray-600 mb-1.5">Feedback Type</label>
                  <select
                    value={formType}
                    onChange={(e) => setFormType(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 bg-gray-50 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                  >
                    {types.map((t) => (
                      <option key={t.value} value={t.value}>{t.label}</option>
                    ))}
                  </select>
                </div>

                {/* Rating */}
                {(formType === "Portal Rating" || formType === "Scheme Suggestion") && (
                  <div>
                    <label className="block font-semibold text-gray-600 mb-1.5">Rating</label>
                    <StarRating current={formRating} onChange={setFormRating} />
                  </div>
                )}

                {/* Related Scheme (for Scheme Suggestions) */}
                {formType === "Scheme Suggestion" && (
                  <div>
                    <label className="block font-semibold text-gray-600 mb-1.5">Related Scheme (Optional)</label>
                    <select
                      value={formRelatedScheme}
                      onChange={(e) => setFormRelatedScheme(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-200 bg-gray-50 rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                    >
                      <option value="">General suggestion</option>
                      {schemes.map((s) => (
                        <option key={s.id} value={s.name}>{s.name}</option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Comment */}
                <div>
                  <label className="block font-semibold text-gray-600 mb-1.5">Your Comments</label>
                  <textarea
                    required
                    rows="5"
                    placeholder="Please share your thoughts, suggestions, or report issues in detail..."
                    value={formComment}
                    onChange={(e) => setFormComment(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 bg-gray-50 focus:bg-white rounded-lg focus:outline-none focus:ring-2 focus:ring-government-blue transition"
                  />
                </div>

                <div className="flex justify-end pt-2">
                  <button
                    type="submit"
                    className="inline-flex items-center space-x-1.5 bg-government-blue hover:bg-government-blue-dark text-white py-2 px-5 rounded-xl font-bold shadow transition focus:outline-none focus:ring-2 focus:ring-government-blue focus:ring-offset-2"
                  >
                    <Send className="h-3.5 w-3.5" />
                    <span>Submit Feedback</span>
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>

        {/* Right Column: Past Feedback */}
        <div className="space-y-6">
          <div className="bg-white border border-gray-200 p-6 rounded-2xl shadow-sm">
            <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider mb-4">Your Past Feedback</h3>

            {userFeedback.length === 0 ? (
              <div className="text-center py-6 text-gray-400 text-xs">
                <AlertCircle className="h-8 w-8 mx-auto mb-2 opacity-50" />
                <p>No feedback submitted yet.</p>
              </div>
            ) : (
              <div className="space-y-3 max-h-[500px] overflow-y-auto">
                {userFeedback.map((f) => (
                  <div key={f.id} className="border border-gray-200 rounded-xl p-3.5 space-y-2 hover:border-gray-300 transition">
                    <div className="flex justify-between items-center text-[10px]">
                      <span className="font-mono font-bold text-gray-900 bg-gray-100 px-1.5 py-0.5 rounded">{f.id}</span>
                      <span className="text-gray-400 font-medium">{f.date}</span>
                    </div>
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <p className="text-[11px] font-bold text-gray-800">{f.type}</p>
                        {f.rating > 0 && (
                          <div className="flex gap-0.5">
                            {[1, 2, 3, 4, 5].map((star) => (
                              <Star
                                key={star}
                                className={`w-3 h-3 ${star <= f.rating ? "fill-saffron-dark text-saffron-dark" : "text-gray-200"}`}
                              />
                            ))}
                          </div>
                        )}
                      </div>
                      <p className="text-[11px] text-gray-600 leading-relaxed">{f.comment}</p>
                      {f.relatedScheme && (
                        <p className="text-[10px] text-gray-400 mt-1">Re: {f.relatedScheme}</p>
                      )}
                    </div>
                    <div className="flex items-center justify-between pt-1 border-t border-gray-100">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full font-extrabold border text-[9px] ${getStatusBadge(f.status)}`}>
                        {f.status}
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
