/**
 * @file FilterSidebar.jsx
 * @description Reusable collapsible filter sidebar panel.
 * Used in Recommendations and Documents for filtering by category, status, etc.
 */

import React, { useState } from "react";
import { ChevronDown, ChevronUp, SlidersHorizontal, X } from "lucide-react";

/**
 * FilterSidebar component.
 *
 * @param {object} props
 * @param {Array<FilterGroup>} props.filters - Filter group definitions
 * @param {Record<string, string[]>} props.selected - Currently selected values per filter key
 * @param {(key: string, value: string) => void} props.onChange - Toggle handler
 * @param {() => void} [props.onClear] - Clear all filters handler
 * @param {string} [props.className=""] - Extra wrapper classes
 * @param {boolean} [props.collapsible=true] - Allow sidebar to be collapsed on mobile
 */

/**
 * @typedef {object} FilterGroup
 * @property {string} key - Unique filter key (matches selected object key)
 * @property {string} label - Display label for the filter group
 * @property {Array<{ value: string, label: string, count?: number }>} options - Options
 * @property {boolean} [defaultOpen=true] - Default open/closed state
 */

export default function FilterSidebar({
  filters = [],
  selected = {},
  onChange,
  onClear,
  className = "",
  collapsible = true,
}) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [groupOpen, setGroupOpen] = useState(
    Object.fromEntries(filters.map((f) => [f.key, f.defaultOpen !== false]))
  );

  const totalSelected = Object.values(selected).flat().length;

  const toggleGroup = (key) =>
    setGroupOpen((prev) => ({ ...prev, [key]: !prev[key] }));

  const content = (
    <div className="space-y-4">
      {filters.map((group) => (
        <div key={group.key}>
          {/* Group header */}
          <button
            onClick={() => toggleGroup(group.key)}
            className="flex w-full items-center justify-between text-left py-1 group"
          >
            <span className="text-[10px] font-bold text-slate-600 uppercase tracking-widest">
              {group.label}
            </span>
            {groupOpen[group.key] ? (
              <ChevronUp className="h-3.5 w-3.5 text-slate-400 group-hover:text-slate-600 transition" />
            ) : (
              <ChevronDown className="h-3.5 w-3.5 text-slate-400 group-hover:text-slate-600 transition" />
            )}
          </button>

          {/* Options list */}
          {groupOpen[group.key] && (
            <div className="mt-2 space-y-1.5 pl-0.5">
              {group.options.map((opt) => {
                const isSelected = (selected[group.key] ?? []).includes(opt.value);
                return (
                  <button
                    key={opt.value}
                    onClick={() => onChange(group.key, opt.value)}
                    className={`flex items-center justify-between w-full text-left px-2.5 py-1.5 rounded-lg text-xs transition ${
                      isSelected
                        ? "bg-indigo-50 text-indigo-800 font-semibold border border-indigo-200"
                        : "text-slate-600 hover:bg-slate-100 border border-transparent"
                    }`}
                  >
                    <span className="truncate">{opt.label}</span>
                    <div className="flex items-center gap-1.5 shrink-0 ml-2">
                      {opt.count !== undefined && (
                        <span className="text-[9px] text-slate-400 font-bold">
                          {opt.count}
                        </span>
                      )}
                      {isSelected && (
                        <span className="h-1.5 w-1.5 rounded-full bg-indigo-600" />
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          )}

          <div className="border-b border-slate-100 mt-3" />
        </div>
      ))}

      {/* Clear all */}
      {totalSelected > 0 && onClear && (
        <button
          onClick={onClear}
          className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-rose-600 font-semibold transition"
        >
          <X className="h-3.5 w-3.5" />
          Clear all filters ({totalSelected})
        </button>
      )}
    </div>
  );

  if (!collapsible) {
    return (
      <div className={`bg-white border border-slate-200 rounded-2xl p-4 shadow-xs ${className}`}>
        <div className="flex items-center gap-2 mb-4 border-b border-slate-100 pb-3">
          <SlidersHorizontal className="h-4 w-4 text-slate-500" />
          <span className="text-xs font-bold text-slate-800 uppercase tracking-widest">Filters</span>
          {totalSelected > 0 && (
            <span className="ml-auto text-[9px] bg-indigo-600 text-white rounded-full px-1.5 py-0.5 font-black">
              {totalSelected}
            </span>
          )}
        </div>
        {content}
      </div>
    );
  }

  return (
    <div className={className}>
      {/* Mobile toggle button */}
      <button
        onClick={() => setSidebarOpen((o) => !o)}
        className="flex w-full items-center justify-between gap-2 px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-700 shadow-xs sm:hidden mb-2"
      >
        <div className="flex items-center gap-2">
          <SlidersHorizontal className="h-4 w-4 text-slate-500" />
          Filters
          {totalSelected > 0 && (
            <span className="text-[9px] bg-indigo-600 text-white rounded-full px-1.5 py-0.5 font-black">
              {totalSelected}
            </span>
          )}
        </div>
        {sidebarOpen ? (
          <ChevronUp className="h-4 w-4" />
        ) : (
          <ChevronDown className="h-4 w-4" />
        )}
      </button>

      {/* Panel */}
      <div
        className={`bg-white border border-slate-200 rounded-2xl p-4 shadow-xs sm:block ${
          sidebarOpen ? "block" : "hidden"
        }`}
      >
        <div className="flex items-center gap-2 mb-4 border-b border-slate-100 pb-3">
          <SlidersHorizontal className="h-4 w-4 text-slate-500" />
          <span className="text-xs font-bold text-slate-800 uppercase tracking-widest">Filters</span>
          {totalSelected > 0 && (
            <span className="ml-auto text-[9px] bg-indigo-600 text-white rounded-full px-1.5 py-0.5 font-black">
              {totalSelected}
            </span>
          )}
        </div>
        {content}
      </div>
    </div>
  );
}
