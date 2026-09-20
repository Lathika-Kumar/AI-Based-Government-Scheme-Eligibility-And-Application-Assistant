import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const Pagination = ({
  currentPage = 1,
  totalPages = 1,
  onPageChange,
  itemsPerPage = 10,
  onItemsPerPageChange,
  totalItems = 0,
}) => {
  const total = Math.max(0, Number(totalItems) || 0);
  const safeItemsPerPage = Math.min(50, Math.max(1, Number(itemsPerPage) || 10));
  const calculatedTotalPages = total > 0 ? Math.ceil(total / safeItemsPerPage) : 1;
  const safeTotalPages = Math.max(1, totalPages ? Math.min(Number(totalPages), calculatedTotalPages) : calculatedTotalPages);
  const safeCurrentPage = Math.min(Math.max(1, Number(currentPage) || 1), safeTotalPages);

  const pageNumbers = [];
  
  // Generate page numbers to display
  if (safeTotalPages <= 5) {
    for (let i = 1; i <= safeTotalPages; i++) {
      pageNumbers.push(i);
    }
  } else {
    if (safeCurrentPage <= 3) {
      pageNumbers.push(1, 2, 3, 4, '...', safeTotalPages);
    } else if (safeCurrentPage >= safeTotalPages - 2) {
      pageNumbers.push(1, '...', safeTotalPages - 3, safeTotalPages - 2, safeTotalPages - 1, safeTotalPages);
    } else {
      pageNumbers.push(1, '...', safeCurrentPage - 1, safeCurrentPage, safeCurrentPage + 1, '...', safeTotalPages);
    }
  }

  const startIndex = total === 0 ? 0 : Math.min((safeCurrentPage - 1) * safeItemsPerPage + 1, total);
  const rawEndIndex = total === 0 ? 0 : Math.min(safeCurrentPage * safeItemsPerPage, total);
  const endIndex = Math.max(startIndex, rawEndIndex);

  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 py-4 border-t border-gray-200 dark:border-slate-800 mt-6">
      <div className="text-sm text-gray-600 dark:text-slate-400">
        Showing <span className="font-semibold text-gray-900 dark:text-slate-100">{startIndex}</span> to{' '}
        <span className="font-semibold text-gray-900 dark:text-slate-100">{endIndex}</span> of{' '}
        <span className="font-semibold text-gray-900 dark:text-slate-100">{total}</span> results
      </div>

      <div className="flex items-center gap-2">
        <div className="flex items-center gap-2">
          <label htmlFor="itemsPerPage" className="text-sm text-gray-600 dark:text-slate-400">
            Per page:
          </label>
          <select
            id="itemsPerPage"
            value={safeItemsPerPage}
            onChange={(e) => {
              const newSize = Math.min(50, Math.max(1, Number(e.target.value) || 10));
              if (onItemsPerPageChange) {
                onItemsPerPageChange(newSize);
              }
              if (onPageChange) {
                onPageChange(1);
              }
            }}
            className="border border-gray-300 dark:border-slate-700 rounded-lg px-2 py-1 text-sm bg-white dark:bg-slate-800 text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-government-blue dark:focus:ring-indigo-400 transition cursor-pointer"
          >
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={30}>30</option>
            <option value={40}>40</option>
            <option value={50}>50</option>
          </select>
        </div>

        <div className="flex items-center gap-1">
          <button
            onClick={() => onPageChange && onPageChange(safeCurrentPage - 1)}
            disabled={safeCurrentPage <= 1}
            aria-label="Previous page"
            className="p-2 rounded-lg border border-gray-300 dark:border-slate-700 text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          
          {pageNumbers.map((page, index) => (
            <React.Fragment key={index}>
              {page === '...' ? (
                <span className="px-2 py-1 text-gray-500 dark:text-slate-400">...</span>
              ) : (
                <button
                  onClick={() => onPageChange && onPageChange(page)}
                  aria-label={`Page ${page}`}
                  aria-current={page === safeCurrentPage ? 'page' : undefined}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                    page === safeCurrentPage
                      ? 'bg-government-blue text-white shadow-sm'
                      : 'text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-700 border border-gray-200 dark:border-slate-700'
                  }`}
                >
                  {page}
                </button>
              )}
            </React.Fragment>
          ))}

          <button
            onClick={() => onPageChange && onPageChange(safeCurrentPage + 1)}
            disabled={safeCurrentPage >= safeTotalPages}
            aria-label="Next page"
            className="p-2 rounded-lg border border-gray-300 dark:border-slate-700 text-gray-700 dark:text-slate-200 hover:bg-gray-100 dark:hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default Pagination;