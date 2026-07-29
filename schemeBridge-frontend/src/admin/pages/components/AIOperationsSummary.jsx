import React from "react";
import { Sparkles, Activity, Clock, TrendingUp } from "lucide-react";

export default function AIOperationsSummary({ summaryData }) {
  const {
    pendingReviews = 0,
    pendingDocuments = 0,
    nearingSLA = 0,
    aiRecommendation = "No current recommendations.",
    confidence = 97.4,
    processingPerformance = 98.4,
    avgProcessingTime = "2.3 Days",
    lastUpdated = "Just Now",
    systemStatus = "Healthy"
  } = summaryData || {};

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return "Good Morning";
    if (hour < 17) return "Good Afternoon";
    return "Good Evening";
  };

  const getStatusColor = (score) => {
    if (score >= 95) return "text-emerald-400";
    if (score >= 90) return "text-amber-400";
    return "text-rose-400";
  };

  const getStatusLabel = (score) => {
    if (score >= 95) return "Excellent";
    if (score >= 90) return "Good";
    return "Needs Attention";
  };

  return (
    <div className="bg-gradient-to-br from-indigo-950 via-slate-900 to-indigo-950 rounded-2xl p-6 text-white shadow-lg border border-indigo-900/40 relative overflow-hidden">
      {/* Decorative elements */}
      <div className="absolute top-0 right-0 w-80 h-80 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20" />
      <div className="absolute bottom-0 left-0 w-64 h-64 bg-emerald-500/5 rounded-full blur-3xl pointer-events-none -ml-16 -mb-16" />

      <div className="relative z-10">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-indigo-800/50 pb-4 mb-6 gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-indigo-500/20 border border-indigo-400/30 rounded-xl text-indigo-300">
              <Sparkles className="h-5 w-5 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-black tracking-widest uppercase text-indigo-300">
                  Operational Intelligence
                </span>
                <span className="px-2 py-0.5 bg-indigo-500/30 text-indigo-200 text-[8px] font-black rounded-full uppercase tracking-wider">
                  Gemini Engine V2
                </span>
              </div>
              <h2 className="text-base font-black tracking-tight mt-0.5">AI Operations Summary</h2>
            </div>
          </div>
          
          {/* Live Status Indicator */}
          <div className="flex items-center gap-2">
            <div className={`w-2 h-2 rounded-full bg-emerald-400 animate-pulse`} />
            <span className="text-[10px] font-bold text-indigo-300">
              STATUS: {systemStatus.toUpperCase()}
            </span>
          </div>
        </div>

        {/* Greeting and Summary */}
        <div className="mb-6 p-4 bg-white/5 rounded-xl border border-white/5">
          <p className="text-sm font-semibold text-slate-200 leading-relaxed">
            {getGreeting()}. {pendingReviews} applications require review. {pendingDocuments} documents require verification. {nearingSLA} applications are nearing processing deadline.
          </p>
        </div>

        {/* Grid of Metrics */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
          <div className="space-y-1.5 p-3.5 bg-white/5 rounded-xl border border-white/5 flex flex-col justify-between hover:bg-white/10 transition">
            <span className="text-[9px] font-black text-indigo-300 uppercase tracking-wider block">
              Average processing
            </span>
            <p className="text-xs text-slate-200 leading-normal font-semibold">
              Currently averaging {avgProcessingTime} per case lifecycle.
            </p>
          </div>

          <div className="space-y-1.5 p-3.5 bg-white/5 rounded-xl border border-white/5 flex flex-col justify-between hover:bg-white/10 transition">
            <span className="text-[9px] font-black text-indigo-300 uppercase tracking-wider block">
              Pending Reviews
            </span>
            <p className="text-xs text-slate-200 leading-normal font-semibold">
              {pendingReviews} submissions need final signature prior to DBT cycle.
            </p>
          </div>

          <div className="space-y-1.5 p-3.5 bg-white/5 rounded-xl border border-white/5 flex flex-col justify-between hover:bg-white/10 transition">
            <span className="text-[9px] font-black text-indigo-300 uppercase tracking-wider block">
              Document Workload
            </span>
            <p className="text-xs text-slate-200 leading-normal font-semibold">
              {pendingDocuments} files in queue requiring credential validation.
            </p>
          </div>

          <div className="space-y-1.5 p-3.5 bg-white/5 rounded-xl border border-white/5 flex flex-col justify-between hover:bg-white/10 transition">
            <span className="text-[9px] font-black text-indigo-300 uppercase tracking-wider block">
              Deadlines Approaching
            </span>
            <p className="text-xs text-slate-200 leading-normal font-semibold">
              {nearingSLA} applications nearing target SLA threshold limit.
            </p>
          </div>

          <div className="space-y-1.5 p-3.5 bg-indigo-500/10 rounded-xl border border-indigo-500/30 flex flex-col justify-between hover:bg-indigo-500/20 transition">
            <span className="text-[9px] font-black text-indigo-300 uppercase tracking-wider block">
              AI Recommendation
            </span>
            <p className="text-xs text-slate-200 leading-normal font-semibold">
              {aiRecommendation}
            </p>
          </div>
        </div>

        {/* AI Metrics Row */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {/* Confidence Score */}
          <div className="bg-white/5 rounded-xl border border-white/5 p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-indigo-500/20 rounded-lg">
                <Activity className="h-4 w-4 text-indigo-300" />
              </div>
              <div>
                <p className="text-[9px] font-black text-indigo-300 uppercase tracking-wider">
                  AI Confidence
                </p>
                <p className="text-2xl font-black text-white mt-1">
                  {confidence.toFixed(1)}%
                </p>
              </div>
            </div>
            <div className={`text-right`}>
              <span className={`text-xs font-bold ${getStatusColor(confidence)}`}>
                {getStatusLabel(confidence)}
              </span>
            </div>
          </div>

          {/* Processing Performance */}
          <div className="bg-white/5 rounded-xl border border-white/5 p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-emerald-500/20 rounded-lg">
                <TrendingUp className="h-4 w-4 text-emerald-300" />
              </div>
              <div>
                <p className="text-[9px] font-black text-indigo-300 uppercase tracking-wider">
                  Processing Performance
                </p>
                <p className="text-2xl font-black text-white mt-1">
                  {processingPerformance.toFixed(1)}%
                </p>
              </div>
            </div>
            <div className={`text-right`}>
              <span className={`text-xs font-bold ${getStatusColor(processingPerformance)}`}>
                {getStatusLabel(processingPerformance)}
              </span>
            </div>
          </div>

          {/* Last Updated */}
          <div className="bg-white/5 rounded-xl border border-white/5 p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-slate-500/20 rounded-lg">
                <Clock className="h-4 w-4 text-slate-300" />
              </div>
              <div>
                <p className="text-[9px] font-black text-indigo-300 uppercase tracking-wider">
                  Last Updated
                </p>
                <p className="text-sm font-bold text-white mt-1">
                  {lastUpdated}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

