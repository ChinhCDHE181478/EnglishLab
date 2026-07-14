import { useEffect, useMemo, useState } from 'react';

// Client-side pagination helper shared across admin/learner list screens so long lists
// always have page controls instead of dumping every row at once.
export function usePagination(items, pageSize = 10, resetKey) {
  const list = Array.isArray(items) ? items : [];
  const [page, setPage] = useState(1);
  const totalPages = Math.max(1, Math.ceil(list.length / pageSize));

  // Jump back to the first page whenever the active filter/search changes.
  useEffect(() => {
    setPage(1);
  }, [resetKey]);

  // Keep the current page in range when items are removed/reloaded.
  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [page, totalPages]);

  const pageItems = useMemo(
    () => list.slice((page - 1) * pageSize, page * pageSize),
    [list, page, pageSize],
  );

  return { page, setPage, totalPages, pageItems, totalItems: list.length };
}

export default function Pagination({ page, totalPages, onChange, totalItems, pageSize = 10, className = '' }) {
  if (totalPages <= 1) return null;

  const from = (page - 1) * pageSize + 1;
  const to = Math.min(page * pageSize, totalItems ?? page * pageSize);

  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;
    if (totalPages <= maxVisible) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);
      const start = Math.max(2, page - 1);
      const end = Math.min(totalPages - 1, page + 1);

      if (start > 2) {
        pages.push('ellipsis-start');
      }

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (end < totalPages - 1) {
        pages.push('ellipsis-end');
      }

      pages.push(totalPages);
    }
    return pages;
  };

  const pageNumbers = getPageNumbers();

  return (
    <div className={`flex flex-col items-center gap-3 w-full sm:flex-row sm:justify-between ${className}`}>
      {/* Items Range Info */}
      {typeof totalItems === 'number' ? (
        <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
          Hiển thị {from}-{to} trong {totalItems}
        </span>
      ) : (
        <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
          Trang {page} / {totalPages}
        </span>
      )}

      {/* Pages Controls */}
      <div className="flex items-center gap-1.5">
        <button
          type="button"
          disabled={page <= 1}
          onClick={() => onChange(page - 1)}
          className="h-9 rounded-xl border border-slate-200 bg-white px-3 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff4f5] disabled:cursor-not-allowed disabled:opacity-40 select-none active:scale-95"
        >
          Trước
        </button>

        {pageNumbers.map((p, idx) => {
          if (p === 'ellipsis-start' || p === 'ellipsis-end') {
            return (
              <span key={`ellipsis-${idx}`} className="w-9 h-9 flex items-center justify-center text-slate-400 font-bold text-xs">
                ...
              </span>
            );
          }
          const isCurrent = page === p;
          return (
            <button
              key={`page-${p}`}
              type="button"
              onClick={() => onChange(p)}
              className={`w-9 h-9 flex items-center justify-center rounded-xl text-xs font-extrabold transition select-none active:scale-95 ${
                isCurrent
                  ? 'bg-[#730014] text-white border border-[#730014] shadow-md shadow-[#730014]/15'
                  : 'bg-white text-slate-700 border border-slate-200 hover:bg-[#fff4f5] hover:text-[#730014] hover:border-[#dfbfbd]'
              }`}
            >
              {p}
            </button>
          );
        })}

        <button
          type="button"
          disabled={page >= totalPages}
          onClick={() => onChange(page + 1)}
          className="h-9 rounded-xl border border-slate-200 bg-white px-3 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff4f5] disabled:cursor-not-allowed disabled:opacity-40 select-none active:scale-95"
        >
          Sau
        </button>
      </div>
    </div>
  );
}
