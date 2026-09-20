/**
 * @file SearchBar.jsx
 * @description Reusable controlled search input with icon and clear button.
 */

import React, { useRef } from "react";
import { Search, X } from "lucide-react";

/**
 * SearchBar component.
 *
 * @param {object} props
 * @param {string} props.value - Controlled input value
 * @param {(value: string) => void} props.onChange - Change handler
 * @param {string} [props.placeholder="Search…"] - Input placeholder
 * @param {string} [props.id] - Input element ID
 * @param {string} [props.className=""] - Extra wrapper classes
 * @param {boolean} [props.autoFocus=false] - Auto-focus the input on mount
 * @param {() => void} [props.onClear] - Optional explicit clear handler
 */
export default function SearchBar({
  value,
  onChange,
  placeholder = "Search…",
  id,
  className = "",
  autoFocus = false,
  onClear,
}) {
  const inputRef = useRef(null);

  const handleClear = () => {
    onChange("");
    onClear?.();
    inputRef.current?.focus();
  };

  return (
    <div className={`relative flex items-center ${className}`}>
      <Search className="absolute left-3 h-4 w-4 text-slate-400 pointer-events-none" />
      <input
        ref={inputRef}
        id={id}
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoFocus={autoFocus}
        className="w-full bg-white border border-slate-200 rounded-xl pl-9 pr-8 py-2 text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 transition"
        aria-label={placeholder}
      />
      {value && (
        <button
          type="button"
          onClick={handleClear}
          className="absolute right-2.5 text-slate-400 hover:text-slate-600 transition"
          aria-label="Clear search"
          tabIndex={-1}
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
}
