import React, { useState, useEffect, useCallback, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { usePageMeta } from "@utils/usePageMeta";
import schemeService from "@services/schemeService";
import { Search, SlidersHorizontal, X, ArrowRight, AlertCircle, RefreshCw, ChevronLeft, ChevronRight, Building2, Tag } from "lucide-react";

const SCHEME_LEVELS = ["", "CENTRAL", "STATE", "UT"];
const BENEFICIARY_TYPES = ["", "INDIVIDUAL", "FAMILY", "FARMER", "STUDENT", "WOMAN", "SENIOR_CITIZEN", "PERSON_WITH_DISABILITY", "BPL", "SC", "ST", "OBC", "MINORITY"];
const SCHEME_TYPES = ["", "DIRECT_BENEFIT", "SCHOLARSHIP", "SUBSIDY", "INSURANCE", "PENSION", "LOAN", "GRANT", "INFRASTRUCTURE"];

function getTitle(scheme) {
  if (!scheme.title) return scheme.schemeCode;
  return scheme.title.en || scheme.title.hi || Object.values(scheme.title)[0] || scheme.schemeCode;
}

function getDesc(scheme) {
  if (!scheme.shortDescription) return "";
  return scheme.shortDescription.en || scheme.shortDescription.hi || Object.values(scheme.shortDescription)[0] || "";
}

export default function SchemeSearch() {
  usePageMeta("Search Schemes", "Search and filter government welfare schemes");
  const navigate = useNavigate();

  const [q, setQ]           = useState("");
  const [schemeLevel, setSchemeLevel]       = useState("");
  const [beneficiaryType, setBeneficiaryType] = useState("");
  const [schemeType, setSchemeType]         = useState("");
  const [page, setPage]     = useState(0);
  const [size]              = useState(12);
  const [sort, setSort]     = useState("schemeCode");
  const [direction, setDirection] = useState("asc");
  const [showFilters, setShowFilters] = useState(false);

  const [results, setResults]   = useState(null);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState(null);

  const searchTimeoutRef = useRef(null);

  const doSearch = useCallback(async (params) => {
    setLoading(true);
    setError(null);
    const result = await schemeService.searchSchemes(params);
    if (result.error) {
      setError(result.message || "Unable to search schemes. Please try again.");
      setResults(null);
    } else {
      setResults(result.data);
    }
    setLoading(false);
  }, []);

  // Search on mount and when params change
  useEffect(() => {
    clearTimeout(searchTimeoutRef.current);
    searchTimeoutRef.current = setTimeout(() => {
      doSearch({ q, schemeLevel, beneficiaryType, schemeType, page, size, sort, direction });
    }, 300);
    return () => clearTimeout(searchTimeoutRef.current);
  }, [q, schemeLevel, beneficiaryType, schemeType, page, size, sort, direction, doSearch]);

  const resetFilters = () => {
    setQ(""); setSchemeLevel(""); setBeneficiaryType(""); setSchemeType("");
    setPage(0); setSort("schemeCode"); setDirection("asc");
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="bg-gradient-to-r from-government-blue via-government-blue-light to-government-blue text-white p-6 rounded-xl shadow-lg">
        <h1 className="text-2xl font-bold tracking-tight">Search Government Schemes</h1>
        <p className="text-sm text-white/90 mt-1">
          Discover Central and State government welfare schemes. Use search and filters to find schemes relevant to you.
        </p>
      </div>

      {/* Search Bar */}
      <div className="flex gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 dark:text-slate-400" />
          <input
            type="text"
            value={q}
            onChange={(e) => { setQ(e.target.value); setPage(0); }}
            placeholder="Search by scheme name, ministry, department, tags..."
            className="w-full pl-10 pr-10 py-3 bg-white dark:bg-slate-800 border border-gray-300 dark:border-slate-700 rounded-xl shadow-sm text-sm text-gray-800 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400 transition"
          />
          {q && (
            <button onClick={() => setQ("")} className="absolute inset-y-0 right-3.5 flex items-center text-gray-400 dark:text-slate-400 hover:text-gray-600 dark:hover:text-slate-200">
              <X className="h-4 w-4" />
            </button>
          )}
        </div>
        <button
          onClick={() => setShowFilters(!showFilters)}
          className="flex items-center gap-2 bg-white dark:bg-slate-800 border border-gray-300 dark:border-slate-700 text-gray-700 dark:text-slate-200 font-semibold px-4 py-3 rounded-xl shadow-sm text-sm hover:bg-gray-50 dark:hover:bg-slate-700 transition"
        >
          <SlidersHorizontal className="h-4 w-4" />
          Filters
          {(schemeLevel || beneficiaryType || schemeType) && (
            <span className="bg-government-blue dark:bg-indigo-600 text-white text-xs rounded-full px-1.5 py-0.5">
              {[schemeLevel, beneficiaryType, schemeType].filter(Boolean).length}
            </span>
          )}
        </button>
      </div>

      {/* Filter Panel */}
      {showFilters && (
        <div className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl p-5 shadow-sm grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1.5">Scheme Level</label>
            <select value={schemeLevel} onChange={(e) => { setSchemeLevel(e.target.value); setPage(0); }}
              className="w-full text-sm border border-gray-300 dark:border-slate-700 rounded-lg px-3 py-2 bg-gray-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
              {SCHEME_LEVELS.map(l => <option key={l} value={l}>{l || "All Levels"}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1.5">Beneficiary Type</label>
            <select value={beneficiaryType} onChange={(e) => { setBeneficiaryType(e.target.value); setPage(0); }}
              className="w-full text-sm border border-gray-300 dark:border-slate-700 rounded-lg px-3 py-2 bg-gray-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
              {BENEFICIARY_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, " ") || "All Types"}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1.5">Scheme Type</label>
            <select value={schemeType} onChange={(e) => { setSchemeType(e.target.value); setPage(0); }}
              className="w-full text-sm border border-gray-300 dark:border-slate-700 rounded-lg px-3 py-2 bg-gray-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
              {SCHEME_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, " ") || "All Types"}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-gray-500 dark:text-slate-400 uppercase tracking-wider mb-1.5">Sort By</label>
            <div className="flex gap-2">
              <select value={sort} onChange={(e) => setSort(e.target.value)}
                className="flex-1 text-sm border border-gray-300 dark:border-slate-700 rounded-lg px-3 py-2 bg-gray-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
                <option value="schemeCode">Scheme Code</option>
                <option value="createdAt">Newest</option>
              </select>
              <select value={direction} onChange={(e) => setDirection(e.target.value)}
                className="text-sm border border-gray-300 dark:border-slate-700 rounded-lg px-3 py-2 bg-gray-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400">
                <option value="asc">↑ Asc</option>
                <option value="desc">↓ Desc</option>
              </select>
            </div>
          </div>
          <div className="sm:col-span-2 lg:col-span-4 flex justify-end">
            <button onClick={resetFilters} className="text-sm text-government-blue dark:text-indigo-400 font-semibold hover:underline flex items-center gap-1">
              <RefreshCw className="h-3.5 w-3.5" /> Reset All Filters
            </button>
          </div>
        </div>
      )}

      {/* Results Summary */}
      {results && !loading && (
        <div className="text-xs text-gray-500 dark:text-slate-400 font-semibold px-1">
          Showing {results.content?.length ?? 0} of {results.totalElements ?? 0} schemes
          {results.totalPages > 1 && ` · Page ${results.page + 1} of ${results.totalPages}`}
        </div>
      )}

      {/* Error State */}
      {error && !loading && (
        <div className="flex items-center gap-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 p-4 rounded-xl text-sm">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <div>
            <p className="font-semibold">Unable to load schemes</p>
            <p className="text-xs mt-0.5">{error}</p>
          </div>
          <button onClick={() => doSearch({ q, schemeLevel, beneficiaryType, schemeType, page, size, sort, direction })}
            className="ml-auto text-xs font-bold text-red-700 dark:text-red-300 hover:underline flex items-center gap-1">
            <RefreshCw className="h-3.5 w-3.5" /> Retry
          </button>
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1,2,3,4,5,6].map(i => (
            <div key={i} className="bg-gray-100 dark:bg-slate-800 rounded-xl animate-pulse h-48" />
          ))}
        </div>
      )}

      {/* Scheme Cards */}
      {!loading && results && results.content?.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {results.content.map((scheme) => (
            <div key={scheme.id} className="bg-white dark:bg-slate-900 border border-gray-200 dark:border-slate-800 rounded-xl shadow-sm hover:shadow-md transition-shadow overflow-hidden flex flex-col">
              <div className="p-5 flex-1 flex flex-col gap-3">
                {/* Header badges */}
                <div className="flex flex-wrap gap-1.5">
                  <span className="text-xs bg-government-blue/10 text-government-blue dark:bg-indigo-950/60 dark:text-indigo-300 px-2 py-0.5 rounded font-semibold">
                    {scheme.schemeLevel || "CENTRAL"}
                  </span>
                  {scheme.beneficiaryType && (
                    <span className="text-xs bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-slate-300 px-2 py-0.5 rounded font-medium">
                      {scheme.beneficiaryType.replace(/_/g, " ")}
                    </span>
                  )}
                </div>

                {/* Title */}
                <div>
                  <h2 className="text-sm font-bold text-gray-900 dark:text-slate-100 leading-snug">{getTitle(scheme)}</h2>
                  {scheme.ministry && (
                    <p className="text-xs text-gray-500 dark:text-slate-400 mt-0.5 flex items-center gap-1">
                      <Building2 className="h-3 w-3" />
                      {scheme.ministry}
                    </p>
                  )}
                  <p className="text-xs text-gray-400 dark:text-slate-500 mt-0.5">{scheme.schemeCode}</p>
                </div>

                {/* Description */}
                {getDesc(scheme) && (
                  <p className="text-xs text-gray-600 dark:text-slate-300 leading-relaxed line-clamp-2">{getDesc(scheme)}</p>
                )}

                {/* Tags */}
                {scheme.tags?.length > 0 && (
                  <div className="flex flex-wrap gap-1">
                    {scheme.tags.slice(0, 3).map(tag => (
                      <span key={tag} className="text-xs bg-saffron/10 text-saffron-dark dark:bg-amber-950/50 dark:text-amber-300 px-2 py-0.5 rounded-full flex items-center gap-1">
                        <Tag className="h-2.5 w-2.5" />{tag}
                      </span>
                    ))}
                    {scheme.tags.length > 3 && (
                      <span className="text-xs text-gray-400 dark:text-slate-500">+{scheme.tags.length - 3} more</span>
                    )}
                  </div>
                )}
              </div>

              {/* Action */}
              <div className="border-t border-gray-100 dark:border-slate-800 p-4">
                <Link
                  to={`/scheme/${scheme.schemeCode}`}
                  className="w-full inline-flex items-center justify-center gap-1.5 bg-government-blue dark:bg-indigo-600 hover:bg-government-blue-dark dark:hover:bg-indigo-700 text-white py-2 rounded-lg text-sm font-semibold transition"
                >
                  View Details <ArrowRight className="h-4 w-4" />
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Empty State */}
      {!loading && results && results.content?.length === 0 && (
        <div className="text-center py-16 text-gray-500 dark:text-slate-400">
          <Search className="h-12 w-12 mx-auto mb-3 opacity-30" />
          <p className="font-semibold text-gray-700 dark:text-slate-300">No schemes found</p>
          <p className="text-sm mt-1">Try changing your search terms or clearing filters.</p>
          <button onClick={resetFilters} className="mt-4 text-government-blue dark:text-indigo-400 font-semibold hover:underline text-sm">
            Clear All Filters
          </button>
        </div>
      )}

      {/* Pagination */}
      {results && results.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="flex items-center gap-1 px-4 py-2 text-sm font-semibold border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <ChevronLeft className="h-4 w-4" /> Previous
          </button>
          <span className="text-sm text-gray-600 dark:text-slate-400 font-medium">
            Page {page + 1} of {results.totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(results.totalPages - 1, p + 1))}
            disabled={page >= results.totalPages - 1}
            className="flex items-center gap-1 px-4 py-2 text-sm font-semibold border border-gray-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            Next <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  );
}
