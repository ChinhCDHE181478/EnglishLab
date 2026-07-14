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

export default function Pagination({
  page,
  totalPages,
  onChange,
  totalItems,
  pageSize = 10,
  className = '',
  alwaysVisible = false,
  compact = false,
}) {
  if (totalPages <= 1 && !alwaysVisible) return null;

  const safePage = Math.min(Math.max(Number(page) || 1, 1), Math.max(totalPages, 1));
  const from = typeof totalItems === 'number' && totalItems > 0 ? (safePage - 1) * pageSize + 1 : 0;
  const to = typeof totalItems === 'number' ? Math.min(safePage * pageSize, totalItems) : safePage * pageSize;
  const pageNumbers = getPageNumbers(safePage, Math.max(totalPages, 1), compact ? 3 : 5);

  return (
    <div className={`flex w-full flex-col items-center gap-3 sm:flex-row sm:justify-between ${className}`}>
      {typeof totalItems === 'number' ? (
        <span className={`${compact ? 'text-[11px]' : 'text-xs'} font-bold uppercase tracking-wider text-slate-500`}>
          Hiển thị {from}-{to} trong {totalItems}
        </span>
      ) : (
        <span className={`${compact ? 'text-[11px]' : 'text-xs'} font-bold uppercase tracking-wider text-slate-500`}>
          Trang {safePage} / {Math.max(totalPages, 1)}
        </span>
      )}

      <div className={`flex items-center ${compact ? 'gap-1' : 'gap-1.5'}`}>
        <button
          className={`${compact ? 'h-8 px-2 text-[11px]' : 'h-9 px-3 text-xs'} rounded-xl border border-slate-200 bg-white font-extrabold text-[#730014] transition hover:bg-[#fff4f5] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40`}
          disabled={safePage <= 1}
          onClick={() => onChange(safePage - 1)}
          type="button"
        >
          Trước
        </button>

        {pageNumbers.map((item, index) => {
          if (item === 'ellipsis') {
            return (
              <span
                className={`${compact ? 'h-8 w-6 text-[11px]' : 'h-9 w-9 text-xs'} flex items-center justify-center font-bold text-slate-400`}
                key={`ellipsis-${index}`}
              >
                ...
              </span>
            );
          }

          const isCurrent = safePage === item;
          return (
            <button
              className={`${compact ? 'h-8 w-8 text-[11px]' : 'h-9 w-9 text-xs'} flex items-center justify-center rounded-xl border font-extrabold transition active:scale-95 ${
                isCurrent
                  ? 'border-[#730014] bg-[#730014] text-white shadow-md shadow-[#730014]/15'
                  : 'border-slate-200 bg-white text-slate-700 hover:border-[#dfbfbd] hover:bg-[#fff4f5] hover:text-[#730014]'
              }`}
              key={`page-${item}`}
              onClick={() => onChange(item)}
              type="button"
            >
              {item}
            </button>
          );
        })}

        <button
          className={`${compact ? 'h-8 px-2 text-[11px]' : 'h-9 px-3 text-xs'} rounded-xl border border-slate-200 bg-white font-extrabold text-[#730014] transition hover:bg-[#fff4f5] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40`}
          disabled={safePage >= totalPages}
          onClick={() => onChange(safePage + 1)}
          type="button"
        >
          Sau
        </button>
      </div>
    </div>
  );
}

function getPageNumbers(page, totalPages, maxVisible) {
  if (totalPages <= maxVisible) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const pages = [1];
  const sideCount = maxVisible <= 3 ? 0 : 1;
  const start = Math.max(2, page - sideCount);
  const end = Math.min(totalPages - 1, page + sideCount);

  if (start > 2) pages.push('ellipsis');
  for (let current = start; current <= end; current += 1) {
    pages.push(current);
  }
  if (end < totalPages - 1) pages.push('ellipsis');
  pages.push(totalPages);

  return pages;
}
