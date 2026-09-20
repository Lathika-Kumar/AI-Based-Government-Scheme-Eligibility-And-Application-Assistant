import React, { useState, useEffect, useMemo } from "react";
import { useApp } from "@context/AppContext";
import { useToast } from "@components/ui/ToastNotification";
import * as feedbackService from "@services/feedbackService";
import {
  ThumbsUp,
  Star,
  Search,
  Filter,
  MessageSquare,
  CheckCircle2,
  Clock,
  User,
  Mail,
  Calendar,
  AlertCircle
} from "lucide-react";

export default function FeedbackManagementCenter() {
  const { showToast } = useToast();

  const [feedbackList, setFeedbackList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [ratingFilter, setRatingFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const loadFeedback = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await feedbackService.getAdminFeedback({ page: 0, size: 100 });
      if (res && !res.error && res.data) {
        const items = Array.isArray(res.data) ? res.data : (res.data.content || []);
        setFeedbackList(items);
        setError(null);
      } else {
        const errMsg = res?.message || "Unable to load citizen feedback from server.";
        setError(errMsg);
        setFeedbackList([]);
      }
    } catch (err) {
      console.error("Failed to fetch admin feedback from backend:", err);
      setError(err?.message || "Network error loading feedback.");
      setFeedbackList([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFeedback();
  }, []);

  const handleUpdateStatus = async (fbId, newStatus) => {
    try {
      await feedbackService.updateFeedbackStatus(fbId, newStatus);
      showToast("success", "Status Updated", `Feedback status changed to ${newStatus}`);
      setFeedbackList((prev) =>
        prev.map((f) => (f.id === fbId || f.feedbackNumber === fbId ? { ...f, status: newStatus } : f))
      );
    } catch (err) {
      showToast("info", "Status Updated", `Feedback status changed locally to ${newStatus}`);
      setFeedbackList((prev) =>
        prev.map((f) => (f.id === fbId || f.feedbackNumber === fbId ? { ...f, status: newStatus } : f))
      );
    }
  };

  const filteredList = useMemo(() => {
    return feedbackList.filter((item) => {
      const matchesSearch =
        !searchQuery ||
        (item.comment && item.comment.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (item.citizenName && item.citizenName.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (item.citizenEmail && item.citizenEmail.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (item.relatedScheme && item.relatedScheme.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (item.feedbackNumber && item.feedbackNumber.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesType = typeFilter === "ALL" || item.type === typeFilter;
      const matchesRating = ratingFilter === "ALL" || Number(item.rating) === Number(ratingFilter);
      const matchesStatus = statusFilter === "ALL" || (item.status && item.status.toUpperCase() === statusFilter.toUpperCase());

      return matchesSearch && matchesType && matchesRating && matchesStatus;
    });
  }, [feedbackList, searchQuery, typeFilter, ratingFilter, statusFilter]);

  const stats = useMemo(() => {
    const total = feedbackList.length;
    const ratedItems = feedbackList.filter((f) => f.rating !== null && f.rating !== undefined && !isNaN(f.rating));
    const avgRating = ratedItems.length > 0
      ? (ratedItems.reduce((acc, f) => acc + Number(f.rating), 0) / ratedItems.length).toFixed(1)
      : "0.0";
    const fiveStars = feedbackList.filter((f) => Number(f.rating) === 5).length;
    const bugs = feedbackList.filter((f) => f.type === "Bug Report").length;
    return { total, avgRating, fiveStars, bugs };
  }, [feedbackList]);

  const getStatusBadge = (status) => {
    const s = String(status || "RECEIVED").toUpperCase();
    if (s === "RESOLVED" || s === "ACKNOWLEDGED") {
      return "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800";
    }
    if (s === "UNDER_REVIEW" || s === "REVIEWED") {
      return "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300 border-amber-200 dark:border-amber-800";
    }
    return "bg-indigo-100 text-indigo-800 dark:bg-indigo-900/40 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800";
  };

  return (
    <div className="space-y-6">
      {/* Metric Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Total Feedback</span>
            <MessageSquare className="h-4.5 w-4.5 text-indigo-600" />
          </div>
          <p className="text-2xl font-black text-slate-900 dark:text-white mt-2">{stats.total}</p>
        </div>

        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Avg Rating</span>
            <Star className="h-4.5 w-4.5 text-amber-500 fill-amber-500" />
          </div>
          <p className="text-2xl font-black text-slate-900 dark:text-white mt-2">{stats.avgRating} <span className="text-xs text-slate-400">/ 5.0</span></p>
        </div>

        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">5-Star Ratings</span>
            <ThumbsUp className="h-4.5 w-4.5 text-emerald-600" />
          </div>
          <p className="text-2xl font-black text-emerald-600 dark:text-emerald-400 mt-2">{stats.fiveStars}</p>
        </div>

        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 rounded-2xl shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Bug Reports</span>
            <AlertCircle className="h-4.5 w-4.5 text-rose-600" />
          </div>
          <p className="text-2xl font-black text-rose-600 dark:text-rose-400 mt-2">{stats.bugs}</p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-4 rounded-2xl shadow-sm space-y-3">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search feedback, citizen name, email, scheme, or ref ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ALL">All Feedback Types</option>
              <option value="Portal Rating">Portal Rating</option>
              <option value="Scheme Suggestion">Scheme Suggestion</option>
              <option value="Bug Report">Bug Report</option>
              <option value="General Feedback">General Feedback</option>
            </select>

            <select
              value={ratingFilter}
              onChange={(e) => setRatingFilter(e.target.value)}
              className="px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ALL">All Ratings</option>
              <option value="5">5 Stars</option>
              <option value="4">4 Stars</option>
              <option value="3">3 Stars</option>
              <option value="2">2 Stars</option>
              <option value="1">1 Star</option>
            </select>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-2 border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ALL">All Statuses</option>
              <option value="RECEIVED">Received</option>
              <option value="REVIEWED">Reviewed</option>
              <option value="ACKNOWLEDGED">Acknowledged</option>
            </select>
          </div>
        </div>
      </div>

      {/* Feedback Feed */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <h2 className="text-sm font-bold text-slate-800 dark:text-slate-100 flex items-center gap-2">
            <ThumbsUp className="h-4 w-4 text-indigo-600" />
            Citizen Feedback Submissions ({filteredList.length})
          </h2>
          <button
            onClick={loadFeedback}
            className="text-xs text-indigo-600 dark:text-indigo-400 font-semibold hover:underline"
          >
            Refresh
          </button>
        </div>

        {loading ? (
          <div className="p-8 text-center text-slate-400 text-xs font-semibold">
            Loading feedback records...
          </div>
        ) : error ? (
          <div className="p-12 text-center text-rose-500 space-y-3">
            <AlertCircle className="h-8 w-8 mx-auto text-rose-500" />
            <p className="text-sm font-bold text-slate-800 dark:text-slate-200">Failed to Load Feedback</p>
            <p className="text-xs text-rose-500">{error}</p>
            <button
              onClick={loadFeedback}
              className="mt-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold hover:bg-indigo-700 transition"
            >
              Retry
            </button>
          </div>
        ) : filteredList.length === 0 ? (
          <div className="p-12 text-center text-slate-400 space-y-2">
            <MessageSquare className="h-8 w-8 mx-auto text-slate-300 dark:text-slate-600" />
            <p className="text-xs font-bold">No feedback entries found matching your filter criteria.</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {filteredList.map((item) => (
              <div key={item.id || item.feedbackNumber} className="p-5 hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="flex items-center gap-2.5 flex-wrap">
                    <span className="font-mono text-xs font-black text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60 px-2 py-0.5 rounded border border-indigo-200 dark:border-indigo-900">
                      {item.feedbackNumber || item.id}
                    </span>
                    <span className="text-xs font-bold text-slate-700 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded border border-slate-200 dark:border-slate-700">
                      {item.type}
                    </span>
                    {item.relatedScheme && (
                      <span className="text-xs text-slate-500 dark:text-slate-400">
                        • Re: <strong className="text-slate-700 dark:text-slate-300">{item.relatedScheme}</strong>
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-3">
                    {/* Stars */}
                    <div className="flex items-center gap-0.5" title={`${item.rating ?? 0} out of 5 stars`}>
                      {[1, 2, 3, 4, 5].map((s) => (
                        <Star
                          key={s}
                          className={`h-3.5 w-3.5 ${
                            s <= Number(item.rating || 0)
                              ? "text-amber-500 fill-amber-500"
                              : "text-slate-300 dark:text-slate-700"
                          }`}
                        />
                      ))}
                    </div>

                    <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full border ${getStatusBadge(item.status)}`}>
                      {item.status || "RECEIVED"}
                    </span>
                  </div>
                </div>

                {/* Comment Body */}
                <p className="text-xs text-slate-700 dark:text-slate-200 font-medium leading-relaxed bg-slate-50 dark:bg-slate-800/50 p-3.5 rounded-xl border border-slate-200/60 dark:border-slate-700/60">
                  "{item.comment}"
                </p>

                {/* Citizen Details & Actions */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-[11px] text-slate-500 dark:text-slate-400 pt-1">
                  <div className="flex items-center gap-4 flex-wrap">
                    <span className="flex items-center gap-1">
                      <User className="h-3 w-3 text-slate-400" />
                      <strong>{item.citizenName || "Anonymous Citizen"}</strong>
                    </span>
                    {item.citizenEmail && (
                      <span className="flex items-center gap-1">
                        <Mail className="h-3 w-3 text-slate-400" />
                        <span>{item.citizenEmail}</span>
                      </span>
                    )}
                    <span className="flex items-center gap-1">
                      <Calendar className="h-3 w-3 text-slate-400" />
                      <span>{item.createdAt ? new Date(item.createdAt).toLocaleString() : (item.date || "Today")}</span>
                    </span>
                  </div>

                  {/* Status Action Buttons */}
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleUpdateStatus(item.id || item.feedbackNumber, "REVIEWED")}
                      className="px-2.5 py-1 bg-amber-50 hover:bg-amber-100 text-amber-800 dark:bg-amber-950 dark:hover:bg-amber-900 dark:text-amber-300 rounded-lg text-[10px] font-bold border border-amber-200 dark:border-amber-800 transition"
                    >
                      Mark Reviewed
                    </button>
                    <button
                      onClick={() => handleUpdateStatus(item.id || item.feedbackNumber, "ACKNOWLEDGED")}
                      className="px-2.5 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:hover:bg-emerald-900 dark:text-emerald-300 rounded-lg text-[10px] font-bold border border-emerald-200 dark:border-emerald-800 transition"
                    >
                      Acknowledge
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
