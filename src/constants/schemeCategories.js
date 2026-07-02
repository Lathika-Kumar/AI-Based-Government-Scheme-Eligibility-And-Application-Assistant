/**
 * @file schemeCategories.js
 * @description Government scheme category constants and display metadata.
 * Backend integration: fetch from /api/schemes/categories for dynamic category management.
 */

/** Canonical scheme category identifiers */
export const SCHEME_CATEGORIES = Object.freeze({
  AGRICULTURE: "Agriculture & Farming",
  HOUSING: "Housing & Urban Development",
  EDUCATION: "Education & Scholarships",
  HEALTH: "Health & Medical",
  PENSION: "Pension & Social Security",
  WOMEN: "Women & Child Development",
  EMPLOYMENT: "Employment & Skill Development",
  FINANCE: "Financial Inclusion & Banking",
  ENERGY: "Energy & Environment",
  RURAL: "Rural Development",
  MSME: "MSME & Entrepreneurship",
  TRIBAL: "Tribal Welfare",
});

/** Display metadata for each category (color classes, description) */
export const CATEGORY_META = Object.freeze({
  [SCHEME_CATEGORIES.AGRICULTURE]: {
    color: "text-green-700",
    bg: "bg-green-50",
    border: "border-green-200",
    badge: "bg-green-100 text-green-800",
    emoji: "🌾",
  },
  [SCHEME_CATEGORIES.HOUSING]: {
    color: "text-blue-700",
    bg: "bg-blue-50",
    border: "border-blue-200",
    badge: "bg-blue-100 text-blue-800",
    emoji: "🏠",
  },
  [SCHEME_CATEGORIES.EDUCATION]: {
    color: "text-indigo-700",
    bg: "bg-indigo-50",
    border: "border-indigo-200",
    badge: "bg-indigo-100 text-indigo-800",
    emoji: "📚",
  },
  [SCHEME_CATEGORIES.HEALTH]: {
    color: "text-rose-700",
    bg: "bg-rose-50",
    border: "border-rose-200",
    badge: "bg-rose-100 text-rose-800",
    emoji: "🏥",
  },
  [SCHEME_CATEGORIES.PENSION]: {
    color: "text-violet-700",
    bg: "bg-violet-50",
    border: "border-violet-200",
    badge: "bg-violet-100 text-violet-800",
    emoji: "🛡️",
  },
  [SCHEME_CATEGORIES.WOMEN]: {
    color: "text-pink-700",
    bg: "bg-pink-50",
    border: "border-pink-200",
    badge: "bg-pink-100 text-pink-800",
    emoji: "👩",
  },
  [SCHEME_CATEGORIES.EMPLOYMENT]: {
    color: "text-amber-700",
    bg: "bg-amber-50",
    border: "border-amber-200",
    badge: "bg-amber-100 text-amber-800",
    emoji: "💼",
  },
  [SCHEME_CATEGORIES.FINANCE]: {
    color: "text-emerald-700",
    bg: "bg-emerald-50",
    border: "border-emerald-200",
    badge: "bg-emerald-100 text-emerald-800",
    emoji: "🏦",
  },
  [SCHEME_CATEGORIES.ENERGY]: {
    color: "text-teal-700",
    bg: "bg-teal-50",
    border: "border-teal-200",
    badge: "bg-teal-100 text-teal-800",
    emoji: "⚡",
  },
  [SCHEME_CATEGORIES.RURAL]: {
    color: "text-lime-700",
    bg: "bg-lime-50",
    border: "border-lime-200",
    badge: "bg-lime-100 text-lime-800",
    emoji: "🌿",
  },
  [SCHEME_CATEGORIES.MSME]: {
    color: "text-orange-700",
    bg: "bg-orange-50",
    border: "border-orange-200",
    badge: "bg-orange-100 text-orange-800",
    emoji: "🏭",
  },
  [SCHEME_CATEGORIES.TRIBAL]: {
    color: "text-cyan-700",
    bg: "bg-cyan-50",
    border: "border-cyan-200",
    badge: "bg-cyan-100 text-cyan-800",
    emoji: "🌍",
  },
});

/**
 * Returns metadata for a given category.
 * Falls back to a neutral default if not found.
 * @param {string} category
 */
export function getCategoryMeta(category) {
  return (
    CATEGORY_META[category] ?? {
      color: "text-slate-700",
      bg: "bg-slate-50",
      border: "border-slate-200",
      badge: "bg-slate-100 text-slate-800",
      emoji: "📋",
    }
  );
}

/** Flat array of all category strings (for filter lists, dropdowns) */
export const CATEGORY_LIST = Object.values(SCHEME_CATEGORIES);
