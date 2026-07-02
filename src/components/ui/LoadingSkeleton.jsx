/**
 * @file LoadingSkeleton.jsx
 * @description Animated loading skeleton placeholder component.
 * Used across Dashboard, Recommendations, Documents, Tracker, and Profile
 * to indicate async data loading states.
 */

import React from "react";

// ── Base pulse animation ──────────────────────────────────────────────────────

function SkeletonBlock({ className = "" }) {
  return (
    <div
      className={`animate-pulse bg-slate-200 rounded-xl ${className}`}
      aria-hidden="true"
    />
  );
}

// ── Variant Components ────────────────────────────────────────────────────────

/** Skeleton for a KPI stat card (Dashboard, Admin) */
function StatCardSkeleton() {
  return (
    <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs">
      <div className="flex items-center justify-between mb-3">
        <SkeletonBlock className="h-2.5 w-20" />
        <SkeletonBlock className="h-8 w-8 rounded-xl" />
      </div>
      <SkeletonBlock className="h-8 w-14 mb-1.5" />
      <SkeletonBlock className="h-2 w-24" />
    </div>
  );
}

/** Skeleton for a scheme / recommendation card */
function CardSkeleton() {
  return (
    <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 space-y-2">
          <SkeletonBlock className="h-3 w-3/4" />
          <SkeletonBlock className="h-2.5 w-1/2" />
        </div>
        <SkeletonBlock className="h-6 w-16 rounded-full" />
      </div>
      <SkeletonBlock className="h-2 w-full" />
      <SkeletonBlock className="h-2 w-5/6" />
      <div className="flex gap-2 pt-1">
        <SkeletonBlock className="h-8 flex-1 rounded-xl" />
        <SkeletonBlock className="h-8 w-8 rounded-xl" />
      </div>
    </div>
  );
}

/** Skeleton for a list row (notifications, applications, audit logs) */
function ListRowSkeleton() {
  return (
    <div className="flex items-start gap-3 p-3 border border-slate-100 rounded-xl">
      <SkeletonBlock className="h-9 w-9 rounded-xl shrink-0" />
      <div className="flex-1 space-y-2 py-0.5">
        <SkeletonBlock className="h-3 w-2/3" />
        <SkeletonBlock className="h-2.5 w-full" />
        <SkeletonBlock className="h-2.5 w-4/5" />
      </div>
      <SkeletonBlock className="h-5 w-12 rounded-full shrink-0" />
    </div>
  );
}

/** Skeleton for a document card in the vault grid */
function DocumentCardSkeleton() {
  return (
    <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs space-y-3">
      <div className="flex items-center gap-3">
        <SkeletonBlock className="h-10 w-10 rounded-xl shrink-0" />
        <div className="flex-1 space-y-1.5">
          <SkeletonBlock className="h-3 w-3/4" />
          <SkeletonBlock className="h-2.5 w-1/2" />
        </div>
        <SkeletonBlock className="h-5 w-16 rounded-full shrink-0" />
      </div>
      <SkeletonBlock className="h-px w-full" />
      <div className="grid grid-cols-2 gap-2">
        <SkeletonBlock className="h-7 rounded-lg" />
        <SkeletonBlock className="h-7 rounded-lg" />
      </div>
    </div>
  );
}

/** Skeleton for a timeline item (Tracker) */
function TimelineSkeleton() {
  return (
    <div className="border border-slate-200 rounded-2xl p-5 shadow-xs space-y-4">
      <div className="flex items-center justify-between">
        <div className="space-y-1.5">
          <SkeletonBlock className="h-3.5 w-48" />
          <SkeletonBlock className="h-2.5 w-32" />
        </div>
        <SkeletonBlock className="h-6 w-20 rounded-full" />
      </div>
      <div className="space-y-3 pl-4 border-l border-slate-100">
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex gap-3 items-center">
            <SkeletonBlock className="h-5 w-5 rounded-full shrink-0 -ml-[10.5px]" />
            <SkeletonBlock className="h-2.5 flex-1" />
          </div>
        ))}
      </div>
    </div>
  );
}

/** Skeleton for a form section (Profile) */
function FormSectionSkeleton() {
  return (
    <div className="bg-white border border-slate-200 rounded-2xl p-6 space-y-4">
      <div className="flex items-center gap-2 border-b border-slate-100 pb-4">
        <SkeletonBlock className="h-5 w-5 rounded" />
        <SkeletonBlock className="h-3.5 w-32" />
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="space-y-1.5">
            <SkeletonBlock className="h-2.5 w-20" />
            <SkeletonBlock className="h-10 w-full rounded-xl" />
          </div>
        ))}
      </div>
    </div>
  );
}

/** Skeleton for a text paragraph block */
function TextSkeleton({ lines = 3 }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: lines }, (_, i) => (
        <SkeletonBlock
          key={i}
          className={`h-2.5 ${i === lines - 1 ? "w-2/3" : "w-full"}`}
        />
      ))}
    </div>
  );
}

/** Skeleton for the AI brief panel (Dashboard) */
function AIBriefSkeleton() {
  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4">
      <div className="flex items-center justify-between border-b border-white/10 pb-3">
        <SkeletonBlock className="h-3 w-40 bg-slate-700" />
        <SkeletonBlock className="h-5 w-20 rounded-full bg-slate-700" />
      </div>
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex gap-2">
            <SkeletonBlock className="h-2 w-2 rounded-full shrink-0 mt-1 bg-slate-700" />
            <SkeletonBlock className={`h-2.5 bg-slate-700 ${i === 3 ? "w-1/2" : "w-full"}`} />
          </div>
        ))}
      </div>
      <div className="flex gap-2 pt-2 border-t border-white/5">
        <SkeletonBlock className="h-8 w-32 rounded-xl bg-slate-700" />
        <SkeletonBlock className="h-8 w-28 rounded-xl bg-slate-700" />
      </div>
    </div>
  );
}

// ── Main export ───────────────────────────────────────────────────────────────

/**
 * LoadingSkeleton — renders the appropriate skeleton variant.
 *
 * @param {{ variant: string, count?: number, className?: string }} props
 * @param {string} props.variant - One of: "stat-card", "card", "list-row", "document-card", "timeline", "form-section", "text", "ai-brief"
 * @param {number} [props.count=1] - Number of skeleton items to render
 * @param {string} [props.className=""] - Wrapper className
 */
export default function LoadingSkeleton({ variant = "card", count = 1, className = "" }) {
  const variants = {
    "stat-card": StatCardSkeleton,
    card: CardSkeleton,
    "list-row": ListRowSkeleton,
    "document-card": DocumentCardSkeleton,
    timeline: TimelineSkeleton,
    "form-section": FormSectionSkeleton,
    text: TextSkeleton,
    "ai-brief": AIBriefSkeleton,
  };

  const Component = variants[variant] ?? CardSkeleton;

  return (
    <div className={className} aria-busy="true" aria-label="Loading content…">
      {Array.from({ length: count }, (_, i) => (
        <Component key={i} />
      ))}
    </div>
  );
}

// Named exports for direct use
export {
  StatCardSkeleton,
  CardSkeleton,
  ListRowSkeleton,
  DocumentCardSkeleton,
  TimelineSkeleton,
  FormSectionSkeleton,
  TextSkeleton,
  AIBriefSkeleton,
};
